/*
 * Inventário — ElevatedScriptExecutor.
 * ElevatedScriptExecutor(): impede instanciação da utilidade.
 * execute(Path, String...): solicita elevação nativa e acompanha o resultado do script.
 * command(Path, Path, String[]): prepara argumentos de pkexec ou UAC.
 * windowsCommand(Path, Path, String[], String, String): prepara a chamada ao launcher nativo e o script elevado.
 * windowsTask(Path): informa o arquivo temporário executado pelo PowerShell elevado.
 * powershellLiteral(String): representa um argumento literal no PowerShell.
 * checkResult(int, Path): distingue sucesso, cancelamento e falha.
 */

package service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/** Executa scripts confiáveis com pkexec no Linux ou UAC no Windows. */
public final class ElevatedScriptExecutor {
    /** Impede instanciação da utilidade. */
    private ElevatedScriptExecutor() { }

    /**
     * Solicita a autorização ao sistema, sem receber nem armazenar a senha.
     * Aguarda fora da EDT; cancelamento impede a continuação da inicialização.
     *
     * @param script arquivo confiável a executar
     * @param arguments argumentos literais do script
     * @throws IOException se o script não existir, a plataforma não for suportada ou a execução falhar
     * @throws InterruptedException se a espera for interrompida; o processo recebe pedido de término
     * @throws CancellationException se a autorização for cancelada
     * @throws IllegalStateException se chamado na EDT
     */
    public static void execute(Path script, String... arguments) throws IOException, InterruptedException {
        if (SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Execute a preparação fora da EDT");
        Path absoluteScript = script.toRealPath();
        if (!Files.isRegularFile(absoluteScript)) throw new IOException("Script inválido: " + absoluteScript);
        Path log = Files.createTempFile("tag-file-elevated-", ".log");
        List<String> command = command(absoluteScript, log, arguments);
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        System.out.println("Solicitando autorização do sistema para: " + absoluteScript + "\nLog: " + log);
        // Inicia apenas o script elevado; a aplicação continua com a conta atual.
        ProcessBuilder builder = new ProcessBuilder(command).directory(Path.of(".").toAbsolutePath().toFile());
        if (windows) {
            // O Windows abre o console elevado; o solicitante informa falhas no console do Java.
            builder.inheritIO();
        } else {
            builder.redirectErrorStream(true).redirectOutput(log.toFile());
        }
        Process process = null;
        try {
            process = builder.start();
            if (windows) {
                // Start-Process -Wait acompanha o processo elevado e seus descendentes.
                process.waitFor();
            } else if (!process.waitFor(15, TimeUnit.MINUTES)) {
                process.destroy();
                throw new IOException("Tempo excedido na autorização ou execução do script. Consulte: " + log);
            }
            checkResult(process.exitValue(), log);
        } catch (InterruptedException interrupted) {
            if (process != null) process.destroy();
            Thread.currentThread().interrupt();
            throw interrupted;
        } finally {
            // Se a espera for interrompida, o Windows ainda pode estar usando este arquivo.
            if (windows && (process == null || !process.isAlive()) && !Thread.currentThread().isInterrupted()) {
                Files.deleteIfExists(windowsTask(log));
            }
        }
        System.out.println("Script concluído: " + absoluteScript + "\nLog: " + log);
    }

    /**
     * Prepara a chamada nativa sem concatenar argumentos em comandos de shell no Linux.
     * No Windows, usa -File com um launcher PowerShell e um script temporário legível.
     *
     * @param script script absoluto existente
     * @param log destino da saída do script
     * @param arguments argumentos literais
     * @return comando com cada argumento separado
     * @throws IOException se a plataforma ou a extensão não forem suportadas
     */
    private static List<String> command(Path script, Path log, String[] arguments) throws IOException {
        String os = System.getProperty("os.name");
        String javaHome = System.getProperty("java.home");
        if (os.startsWith("Linux")) {
            if (!script.toString().endsWith(".sh")) throw new IOException("No Linux, informe um script .sh");
            if (!Files.isExecutable(Path.of("/usr/bin/pkexec"))) throw new IOException("pkexec não está instalado");
            // pkexec remove variáveis do ambiente; transmite somente o JDK selecionado.
            List<String> command = new ArrayList<>(List.of("/usr/bin/pkexec", "--disable-internal-agent",
                    "--keep-cwd", "/usr/bin/env", "TAG_FILE_JDK=" + javaHome,
                    "/bin/bash", script.toString()));
            command.addAll(List.of(arguments));
            return command;
        }
        if (!os.startsWith("Windows")) throw new IOException("Elevação disponível somente no Linux e Windows");
        if (!script.toString().endsWith(".ps1")) throw new IOException("No Windows, informe um script .ps1");
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null) throw new IOException("Diretório do Windows não identificado");
        return windowsCommand(script, log, arguments, javaHome, systemRoot);
    }

    /**
     * Delega UAC a Start-Process; o próprio console elevado executa o script e mostra a saída.
     * @param script script PowerShell
     * @param log destino da saída
     * @param arguments argumentos do script
     * @param javaHome JDK usado pela aplicação
     * @param systemRoot diretório do Windows
     * @return chamada que solicita a elevação, aguarda e devolve o código do script
     * @throws IOException se o launcher estiver ausente ou o script temporário não puder ser gravado
     */
    private static List<String> windowsCommand(Path script, Path log, String[] arguments,
            String javaHome, String systemRoot) throws IOException {
        String powershell = Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe").toString();
        Path launcher = Path.of("scripts/elevate.ps1").toRealPath();
        // Mantém caminhos/valores como dados. O único switch previsto pelo contrato
        // de instalação precisa ser um parâmetro, pois '-Authorized' entre aspas
        // seria um valor posicional e deixaria [switch]$Authorized desativado.
        StringBuilder invocation = new StringBuilder("& ").append(powershellLiteral(script.toString()));
        for (String argument : arguments) {
            invocation.append(' ').append("-Authorized".equalsIgnoreCase(argument)
                    ? "-Authorized" : powershellLiteral(argument));
        }
        String task = "\ufeff$ErrorActionPreference = 'Stop'\n"
                + "[Console]::OutputEncoding = New-Object Text.UTF8Encoding $false\n"
                + "$env:TAG_FILE_JDK = " + powershellLiteral(javaHome) + "\n"
                + "$log = " + powershellLiteral(log.toString()) + "\n"
                + "$global:LASTEXITCODE = 0\n$code = 1\n$transcribing = $false\n"
                + "try {\n    Start-Transcript -LiteralPath $log -Force | Out-Null\n"
                + "    $transcribing = $true\n    Write-Host " + powershellLiteral("Executando: " + script) + "\n"
                + "    " + invocation + "\n    $code = $LASTEXITCODE\n"
                + "} catch {\n    Write-Host ($_ | Format-List * -Force | Out-String -Width 4096)\n    $code = 1\n"
                + "} finally {\n    Write-Host \"ExitCode: $code | Log: $log\"\n"
                + "    if ($transcribing) { Stop-Transcript | Out-Null }\n}\n"
                + "if ($code -ne 0 -and ![Console]::IsInputRedirected) {\n"
                + "    Read-Host 'O script falhou. Pressione Enter para retornar ao Tag-File' | Out-Null\n}\n"
                + "exit $code\n";
        Files.writeString(windowsTask(log), task, StandardCharsets.UTF_8);
        return List.of(powershell, "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-File", launcher.toString(), "-ScriptPath", windowsTask(log).toString(), "-LogPath", log.toString());
    }

    /**
     * Localiza o script temporário correspondente ao log de uma execução.
     * @param log log exclusivo da execução
     * @return caminho do script temporário, preservado quando a espera é interrompida
     */
    private static Path windowsTask(Path log) { return log.resolveSibling(log.getFileName() + ".ps1"); }

    /**
     * Delimita um valor como texto literal no PowerShell.
     * @param value texto não interpretado como código
     * @return literal entre apóstrofos, com apóstrofos internos duplicados
     */
    private static String powershellLiteral(String value) { return "'" + value.replace("'", "''") + "'"; }

    /**
     * Preserva cancelamento como resultado distinto de falha de execução.
     * @param code código retornado pelo processo
     * @param log saída do script para diagnóstico
     * @throws CancellationException se a autorização for cancelada
     * @throws IOException se autorização ou script falhar
     */
    private static void checkResult(int code, Path log) throws IOException {
        if (code == 126) throw new CancellationException("Autorização cancelada pelo usuário");
        if (code == 127) throw new IOException("Autorização indisponível, negada ou falha no executor. Consulte: " + log);
        if (code != 0) throw new IOException("O script encerrou com código " + code + ". Consulte: " + log);
    }
}
