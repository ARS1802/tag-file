import persistence.DatabaseManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;

/** Verifica decisao de instalar e propagacao de falhas com scripts de teste. */
public final class DatabasePreparationCheck {
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
        System.out.println("PASS: " + message);
    }
    public static void main(String[] args) throws Exception {
        // No Linux, o runner fornece powershell.exe como shim para pwsh.
        // Inicializa o backend nativo real antes de simular a selecao de scripts Windows.
        String originalOs = System.getProperty("os.name");
        new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin",
                originalOs.startsWith("Windows") ? "java.exe" : "java").toString(), "-version")
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor();
        Path root = Files.createTempDirectory("tag-file database [A] '");
        try {
            System.setProperty("os.name", "Windows test selection");
            Path scripts = Files.createDirectories(root.resolve("database/scripts/windows"));
            write(scripts.resolve("check.ps1"), "exit ([int](Get-Content -LiteralPath (Join-Path $PSScriptRoot 'code')))\n");
            write(scripts.resolve("install.ps1"), "param([switch]$Authorized)\nif (!$Authorized) { exit 9 }\n"
                    + "Write-Output \"TAG_FILE_PROGRESS`tSTAGE`tInstalando teste\"\n"
                    + "$limit = (Get-Date).AddSeconds(5)\n"
                    + "while (!(Test-Path -LiteralPath (Join-Path $PSScriptRoot 'release'))) { if ((Get-Date) -gt $limit) { exit 11 }; Start-Sleep -Milliseconds 20 }\n"
                    + "Set-Content -LiteralPath (Join-Path $PSScriptRoot 'installed') -Value yes\nexit 0\n");
            for (String action : new String[]{"initialize", "start"}) {
                write(scripts.resolve(action + ".ps1"), "Set-Content -LiteralPath (Join-Path $PSScriptRoot '"
                        + action + "-executed') -Value yes\nexit 0\n");
            }
            DatabaseManager manager = new DatabaseManager(root, (script, argument) -> {
                throw new AssertionError("Windows nao deve solicitar elevacao");
            });
            java.util.concurrent.atomic.AtomicBoolean liveProgress = new java.util.concurrent.atomic.AtomicBoolean();
            manager.setProgressListener(progress -> {
                if (progress.message().equals("Instalando teste")) {
                    check(!Files.exists(scripts.resolve("installed")), "Progresso recebido antes de o script terminar");
                    check(Files.exists(progress.log()), "Log existe durante a execucao");
                    liveProgress.set(true);
                    try { Files.writeString(scripts.resolve("release"), "continue"); }
                    catch (IOException e) { throw new java.io.UncheckedIOException(e); }
                }
            });
            Files.writeString(scripts.resolve("code"), "4");
            try { manager.prepare(false); throw new AssertionError("Consentimento ignorado"); }
            catch (DatabaseManager.InstallationRequiredException expected) { checks++; }
            check(!Files.exists(scripts.resolve("installed")), "Recusa nao instala");
            check(!Files.exists(scripts.resolve("initialize-executed")), "Recusa nao inicializa dados");
            manager.prepare(true);
            check(liveProgress.get(), "Observador recebe progresso ao vivo");
            check(Files.exists(scripts.resolve("installed")), "Windows instala sem executor elevado");
            check(Files.exists(scripts.resolve("start-executed")), "Preparacao continua apos instalacao");
            for (int code : new int[]{5, 6}) {
                Files.delete(scripts.resolve("installed"));
                Files.delete(scripts.resolve("initialize-executed"));
                Files.delete(scripts.resolve("start-executed"));
                Files.writeString(scripts.resolve("code"), Integer.toString(code));
                try { manager.prepare(true); throw new AssertionError("Falha ignorada"); }
                catch (DatabaseManager.InstallationRequiredException wrong) { throw new AssertionError("Falha confundida com ausencia", wrong); }
                catch (IOException expected) { check(expected.getMessage().contains("Log:"), "Falha inclui log: " + code); }
                check(!Files.exists(scripts.resolve("installed")), "Falha existente nao reinstala: " + code);
                check(!Files.exists(scripts.resolve("start-executed")), "Falha nao inicia servidor: " + code);
                // Recria apenas marcadores para a limpeza da proxima iteracao.
                for (String marker : new String[]{"installed", "initialize-executed", "start-executed"}) Files.writeString(scripts.resolve(marker), "fixture");
            }
            Files.delete(scripts.resolve("installed"));
            Files.writeString(scripts.resolve("code"), "0");
            manager.prepare(false);
            check(!Files.exists(scripts.resolve("installed")), "Instalacao existente dispensa consentimento e download");
            try (var logs = Files.list(root.resolve("database/runtime/logs"))) {
                for (Path log : logs.toList()) check(Files.readString(log).matches("(?s).*SHA-256: [a-f0-9]{64}.*"), "Log identifica script executado");
            }
            System.out.println("DATABASE-PREPARATION: " + checks + " verificacoes; scripts Windows reais de teste, sem MySQL.");
        } finally {
            System.setProperty("os.name", originalOs);
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
    private static void write(Path file, String body) throws IOException {
        Files.writeString(file, "\ufeff" + body, StandardCharsets.UTF_8);
    }
}
