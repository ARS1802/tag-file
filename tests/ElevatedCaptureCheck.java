import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import service.ElevatedScriptExecutor;

/** Exercita o comando pos-UAC real, sem solicitar privilegios. */
public final class ElevatedCaptureCheck {
    private static int checks;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("PASS: " + message);
    }

    public static void main(String[] args) throws Exception {
        Path shell = Path.of(args[0]).toAbsolutePath();
        Path root = Files.createTempDirectory("tag-file-capture-");
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        try {
            String systemRoot = windows ? System.getenv("SystemRoot") : root.resolve("system").toString();
            if (!windows) {
                Path shim = Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
                Files.createDirectories(shim.getParent());
                Files.writeString(shim, "#!/bin/sh\nexec '" + shell.toString().replace("'", "'\\''") + "' \"$@\"\n");
                check(shim.toFile().setExecutable(true), "Adaptador Linux criado somente na fixture");
            }
            var method = ElevatedScriptExecutor.class.getDeclaredMethod("windowsCommand", Path.class,
                    Path.class, String[].class, String.class, String.class);
            method.setAccessible(true);
            Path script = root.resolve("Cópia [A] d'Arthur $.ps1");
            Path log = root.resolve("captura completa.log");
            String literal = "texto com ' $ e espaços; sem executar comandos";
            String common = "param([string]$Value)\n[Console]::WriteLine($Value)\n"
                    + "[Console]::WriteLine($env:TAG_FILE_JDK)\n"
                    + "[Console]::Error.WriteLine('PRIMEIRA LINHA')\n"
                    + "[Console]::Out.WriteLine(('O' * 131072))\n"
                    + "[Console]::Error.WriteLine(('E' * 131072))\n"
                    + "[Console]::Error.WriteLine('ÚLTIMA LINHA')\n";
            for (int expected : new int[]{0, 7}) {
                // BOM garante leitura dos acentos tambem no Windows PowerShell 5.1.
                Files.writeString(script, "\ufeff" + common + "exit " + expected + "\n", StandardCharsets.UTF_8);
                @SuppressWarnings("unchecked")
                List<String> command = (List<String>) method.invoke(null, script, log, new String[]{literal},
                        "JDK de teste com espaço", systemRoot);
                String request = new String(Base64.getDecoder().decode(command.getLast()), StandardCharsets.UTF_16LE);
                check(request.contains("-Verb RunAs -Wait -PassThru") && request.contains("1223"), "UAC e cancelamento mantidos");
                var match = Pattern.compile("'-EncodedCommand','([A-Za-z0-9+/=]+)'").matcher(request);
                check(match.find(), "Payload elevado encontrado");
                Process process = new ProcessBuilder(shell.toString(), "-NoProfile", "-NonInteractive",
                        "-EncodedCommand", match.group(1)).inheritIO().start();
                if (!process.waitFor(30, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new AssertionError("Captura bloqueou com buffers cheios");
                }
                check(process.exitValue() == expected, "Codigo " + expected + " preservado, mesmo com stderr");
                String output = Files.readString(log, StandardCharsets.UTF_8);
                check(output.contains(literal), "Argumento literal preservado");
                check(output.contains("JDK de teste com espaço"), "JDK transmitido ao filho");
                check(output.contains("PRIMEIRA LINHA") && output.contains("ÚLTIMA LINHA"), "Erro multilinha completo em UTF-8");
                check(output.contains("O".repeat(131072)) && output.contains("E".repeat(131072)), "Ambos os canais drenados integralmente");
            }
            Files.writeString(script, "throw 'FALHA ORIGINAL COMPLETA'\n");
            @SuppressWarnings("unchecked")
            List<String> command = (List<String>) method.invoke(null, script, log, new String[0], "test", systemRoot);
            String request = new String(Base64.getDecoder().decode(command.getLast()), StandardCharsets.UTF_16LE);
            var match = Pattern.compile("'-EncodedCommand','([A-Za-z0-9+/=]+)'").matcher(request);
            check(match.find(), "Payload para excecao encontrado");
            Process process = new ProcessBuilder(shell.toString(), "-NoProfile", "-NonInteractive",
                    "-EncodedCommand", match.group(1)).inheritIO().start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new AssertionError("Timeout"); }
            check(process.exitValue() == 1, "Excecao PowerShell falha");
            String output = Files.readString(log);
            check(output.contains("FALHA ORIGINAL COMPLETA") && output.contains("ScriptStackTrace"), "Excecao inclui mensagem e contexto");
            System.out.println("ELEVATED-CAPTURE: " + checks + " verificacoes passaram; UAC real nao executado.");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
