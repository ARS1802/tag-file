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

            // Usa o preambulo real do instalador, mas para antes de qualquer download.
            Path installDirectory = Files.createDirectories(root.resolve("database/scripts/windows"));
            Path installer = installDirectory.resolve("install.ps1");
            String installSource = Files.readString(Path.of("database/scripts/windows/install.ps1"));
            String guard = installSource.lines().limit(3).collect(java.util.stream.Collectors.joining("\n"));
            Files.copy(Path.of("database/scripts/windows/common.ps1"), installDirectory.resolve("common.ps1"));
            Files.writeString(installer, guard + "\n[Console]::WriteLine('AUTORIZACAO RECEBIDA')\n"
                    + "[Console]::WriteLine(('PROGRESSO: ' + $ProgressPreference))\n"
                    + "Write-Progress -Activity 'PROGRESSO TESTE' -Status 'Teste'\nexit 0\n");
            for (String[] forwarded : new String[][]{ {"-Authorized"}, {"-authorized"}, {},
                    {"-Authorized:$false"}, {"-Authorized; Write-Output INJETADO"} }) {
                boolean authorized = forwarded.length == 1 && forwarded[0].equalsIgnoreCase("-Authorized");
                @SuppressWarnings("unchecked")
                List<String> installCommand = (List<String>) method.invoke(null, installer, log, forwarded, "test", systemRoot);
                String installRequest = new String(Base64.getDecoder().decode(installCommand.getLast()), StandardCharsets.UTF_16LE);
                var installPayload = Pattern.compile("'-EncodedCommand','([A-Za-z0-9+/=]+)'").matcher(installRequest);
                check(installPayload.find(), "Payload do instalador encontrado");
                Process installProcess = new ProcessBuilder(shell.toString(), "-NoProfile", "-NonInteractive",
                        "-EncodedCommand", installPayload.group(1)).inheritIO().start();
                if (!installProcess.waitFor(30, TimeUnit.SECONDS)) {
                    installProcess.destroyForcibly(); throw new AssertionError("Timeout no teste de autorizacao");
                }
                String installLog = Files.readString(log);
                check(installProcess.exitValue() == (authorized ? 0 : 1),
                        authorized ? "Switch Authorized realmente recebido pelo instalador" : "Ausencia/argumento falso nao autoriza");
                check(installLog.contains("AUTORIZACAO RECEBIDA") == authorized, "Guarda de autorizacao preservada");
                if (authorized) {
                    check(installLog.contains("PROGRESSO: SilentlyContinue"), "Progresso suprimido antes de carregar modulos");
                    check(!installLog.contains("CLIXML"), "Log sem serializacao de progresso");
                } else {
                    check(installLog.contains("Instalação não autorizada"), "Mensagem de recusa preserva acentos");
                    check(!installLog.contains("\nINJETADO"), "Argumento nao executa codigo PowerShell");
                }
            }
            try (var scripts = Files.list(Path.of("database/scripts/windows"))) {
                for (Path source : scripts.filter(p -> p.toString().endsWith(".ps1")).sorted().toList()) {
                    byte[] bytes = Files.readAllBytes(source);
                    check(bytes.length >= 3 && bytes[0] == (byte) 0xef && bytes[1] == (byte) 0xbb
                            && bytes[2] == (byte) 0xbf, "UTF-8 identificado no Windows PowerShell: " + source.getFileName());
                }
            }
            System.out.println("ELEVATED-CAPTURE: " + checks + " verificacoes passaram; UAC real nao executado.");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
