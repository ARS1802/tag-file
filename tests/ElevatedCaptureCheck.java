import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import service.ElevatedScriptExecutor;

/** Exercita scripts reais; simula somente a fronteira UAC, sem solicitar privilegios. */
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
            var method = ElevatedScriptExecutor.class.getDeclaredMethod("windowsCommand", Path.class,
                    Path.class, String[].class, String.class, String.class);
            method.setAccessible(true);
            Path script = root.resolve("Cópia [A] d'Arthur $.ps1");
            Path log = root.resolve("captura completa.log");
            Path console = root.resolve("console.txt");
            String literal = "texto com ' $ e espaços; sem executar comandos";
            String common = "param([string]$Value)\nWrite-Host $Value\n"
                    + "Write-Host $env:TAG_FILE_JDK\n"
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
                Process process = runTask(shell, command, console);
                if (!process.waitFor(30, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    throw new AssertionError("Captura bloqueou com buffers cheios");
                }
                check(process.exitValue() == expected, "Codigo " + expected + " preservado, mesmo com stderr");
                String output = Files.readString(console, StandardCharsets.UTF_8);
                check(output.contains(literal), "Argumento literal preservado");
                check(output.contains("JDK de teste com espaço"), "JDK transmitido ao filho");
                check(output.contains("PRIMEIRA LINHA") && output.contains("ÚLTIMA LINHA"), "Erro multilinha completo em UTF-8");
                check(output.contains("O".repeat(131072)) && output.contains("E".repeat(131072)), "Ambos os canais chegam ao console sem retencao");
                check(Files.readString(log).contains(literal) && Files.readString(log).contains("ExitCode: " + expected), "Transcricao nativa registra mensagens e resultado");
            }
            Files.writeString(script, "throw 'FALHA ORIGINAL COMPLETA'\n");
            @SuppressWarnings("unchecked")
            List<String> command = (List<String>) method.invoke(null, script, log, new String[0], "test", systemRoot);
            Process process = runTask(shell, command, console);
            if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new AssertionError("Timeout"); }
            check(process.exitValue() == 1, "Excecao PowerShell falha");
            String output = Files.readString(log);
            check(output.contains("FALHA ORIGINAL COMPLETA") && output.contains("ScriptStackTrace"), "Excecao inclui mensagem e contexto na transcricao");

            // Regressao: uma etapa longa precisa mostrar progresso antes de terminar.
            Path release = root.resolve("continue");
            Files.writeString(script, "Write-Host 'PROGRESSO ANTES DO TERMINO'\n"
                    + "while (!(Test-Path -LiteralPath '" + release.toString().replace("'", "''") + "')) { Start-Sleep -Milliseconds 50 }\nexit 0\n");
            process = runTask(shell, command, console);
            boolean visible = false;
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (System.nanoTime() < deadline && process.isAlive()) {
                    if (Files.readString(console).contains("PROGRESSO ANTES DO TERMINO")) { visible = true; break; }
                    Thread.sleep(50);
                }
                check(visible && process.isAlive(), "Progresso visivel enquanto o script ainda trabalha");
            } finally {
                Files.writeString(release, "continue");
                if (!process.waitFor(30, TimeUnit.SECONDS)) process.destroyForcibly();
            }
            check(process.exitValue() == 0, "Script continua normalmente depois do progresso");

            // Executa o launcher real substituindo apenas a chamada que pediria UAC.
            Path harness = root.resolve("native-boundary.ps1");
            Files.writeString(harness, """
                    param($Launcher, $Task, $Log, $Shell, $Mode)
                    $ErrorActionPreference = 'Stop'
                    function Start-Process {
                        param($FilePath, $Verb, $ArgumentList, $WorkingDirectory, $WindowStyle, [switch]$Wait, [switch]$PassThru)
                        if ($Verb -ne 'RunAs' -or !$Wait -or !$PassThru -or $WindowStyle -ne 'Normal') { throw 'Contrato nativo incorreto' }
                        if ($ArgumentList -match 'NonInteractive|EncodedCommand') { throw 'Console elevado deve ser interativo e usar -File' }
                        if ($Mode -eq 'cancel') { throw [ComponentModel.Win32Exception]::new(1223) }
                        if ($Mode -eq 'failure') { throw 'FALHA NATIVA SIMULADA' }
                        Microsoft.PowerShell.Management\\Start-Process -FilePath $Shell -ArgumentList $ArgumentList -WorkingDirectory $WorkingDirectory -NoNewWindow -Wait -PassThru
                    }
                    & $Launcher -ScriptPath $Task -LogPath $Log
                    exit $LASTEXITCODE
                    """);
            for (String mode : new String[]{"success", "script-failure", "cancel", "failure"}) {
                Path executed = root.resolve("executed");
                Files.deleteIfExists(executed);
                Files.writeString(script, "Set-Content -LiteralPath '" + executed.toString().replace("'", "''") + "' -Value 'yes'\nexit "
                        + (mode.equals("script-failure") ? 7 : 0) + "\n");
                Files.writeString(log, "");
                Process request = new ProcessBuilder(shell.toString(), "-NoProfile", "-NonInteractive", "-File", harness.toString(),
                        "-Launcher", command.get(command.indexOf("-File") + 1), "-Task", taskPath(command),
                        "-Log", log.toString(), "-Shell", shell.toString(), "-Mode", mode)
                        .redirectErrorStream(true).redirectOutput(console.toFile()).start();
                if (!request.waitFor(30, TimeUnit.SECONDS)) { request.destroyForcibly(); throw new AssertionError("Timeout no launcher"); }
                int expected = switch (mode) { case "success" -> 0; case "script-failure" -> 7; case "cancel" -> 126; default -> 127; };
                check(request.exitValue() == expected, "Launcher devolve codigo nativo: " + mode
                        + (request.exitValue() == expected ? "" : " / " + Files.readString(console)));
                check(Files.exists(executed) == (expected == 0 || expected == 7), "Execucao respeita o resultado da autorizacao: " + mode);
                if (mode.equals("cancel")) check(Files.readString(log).contains("cancelada"), "Cancelamento fica registrado");
                if (mode.equals("failure")) check(Files.readString(log).contains("FALHA NATIVA SIMULADA"), "Falha do launcher fica registrada");
            }

            // Usa o preambulo real do instalador, mas para antes de qualquer download.
            Path installDirectory = Files.createDirectories(root.resolve("database/scripts/windows"));
            Path installer = installDirectory.resolve("install.ps1");
            String installSource = Files.readString(Path.of("database/scripts/windows/install.ps1"));
            String guard = installSource.lines().limit(3).collect(java.util.stream.Collectors.joining("\n"));
            Files.copy(Path.of("database/scripts/windows/common.ps1"), installDirectory.resolve("common.ps1"));
            Files.writeString(installer, guard + "\nWrite-Host 'AUTORIZACAO RECEBIDA'\n"
                    + "Write-Host ('PROGRESSO: ' + $ProgressPreference)\n"
                    + "Write-Progress -Activity 'PROGRESSO TESTE' -Status 'Teste'\nexit 0\n");
            for (String[] forwarded : new String[][]{ {"-Authorized"}, {"-authorized"}, {},
                    {"-Authorized:$false"}, {"-Authorized; Write-Output INJETADO"} }) {
                boolean authorized = forwarded.length == 1 && forwarded[0].equalsIgnoreCase("-Authorized");
                @SuppressWarnings("unchecked")
                List<String> installCommand = (List<String>) method.invoke(null, installer, log, forwarded, "test", systemRoot);
                Process installProcess = runTask(shell, installCommand, console);
                if (!installProcess.waitFor(30, TimeUnit.SECONDS)) {
                    installProcess.destroyForcibly(); throw new AssertionError("Timeout no teste de autorizacao");
                }
                String installLog = Files.readString(log);
                check(installProcess.exitValue() == (authorized ? 0 : 1),
                        authorized ? "Switch Authorized realmente recebido pelo instalador" : "Ausencia/argumento falso nao autoriza");
                check(installLog.contains("AUTORIZACAO RECEBIDA") == authorized, "Guarda de autorizacao preservada");
                if (authorized) {
                    check(installLog.contains("PROGRESSO: Continue"), "Progresso nativo habilitado no console elevado");
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
            System.out.println("ELEVATED-CAPTURE: " + checks + " verificacoes passaram; fronteira UAC simulada, sem privilegios.");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static String taskPath(List<String> command) {
        int parameter = command.indexOf("-ScriptPath");
        if (parameter < 0) throw new AssertionError("Launcher deve receber o script por -File");
        return command.get(parameter + 1);
    }

    private static Process runTask(Path shell, List<String> command, Path console) throws Exception {
        return new ProcessBuilder(shell.toString(), "-NoProfile", "-NonInteractive", "-File", taskPath(command))
                .redirectErrorStream(true).redirectOutput(console.toFile()).start();
    }
}
