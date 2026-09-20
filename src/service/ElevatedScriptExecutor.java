/*
 * Inventário — ElevatedScriptExecutor.
 * ElevatedScriptExecutor(): impede instanciação da utilidade.
 * execute(Path, String...): solicita elevação nativa e acompanha o resultado do script.
 * command(Path, Path, String[]): prepara argumentos de pkexec ou UAC.
 * windowsCommand(Path, Path, String[], String, String): prepara a solicitação de UAC e o script elevado.
 * powershellLiteral(String): representa um argumento literal no PowerShell.
 * encodedCommand(String): codifica o comando para o parâmetro -EncodedCommand.
 * checkResult(int, Path): distingue sucesso, cancelamento e falha.
 */

package service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
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
        // Inicia apenas o script elevado; a aplicação continua com a conta atual.
        ProcessBuilder builder = new ProcessBuilder(command).directory(Path.of(".").toAbsolutePath().toFile());
        if (System.getProperty("os.name").startsWith("Windows")) {
            // O PowerShell elevado grava o log; o processo que solicita UAC usa o console.
            builder.inheritIO();
        } else {
            builder.redirectErrorStream(true).redirectOutput(log.toFile());
        }
        Process process = builder.start();
        try {
            if (!process.waitFor(15, TimeUnit.MINUTES)) {
                process.destroy();
                throw new IOException("Tempo excedido na autorização ou execução do script. Consulte: " + log);
            }
            checkResult(process.exitValue(), log);
        } catch (InterruptedException interrupted) {
            process.destroy();
            Thread.currentThread().interrupt();
            throw interrupted;
        }
        System.out.println("Script concluído: " + absoluteScript + "\nLog: " + log);
    }

    /**
     * Prepara a chamada nativa sem concatenar argumentos em comandos de shell no Linux.
     * No Windows, valores são literais e o comando completo usa UTF-16LE em Base64.
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
     * Usa UAC para executar um PowerShell separado, com argumentos literais e log próprio.
     * @param script script PowerShell
     * @param log destino da saída
     * @param arguments argumentos do script
     * @param javaHome JDK usado pela aplicação
     * @param systemRoot diretório do Windows
     * @return chamada que solicita a elevação, aguarda e devolve o código do script
     */
    private static List<String> windowsCommand(Path script, Path log, String[] arguments,
            String javaHome, String systemRoot) {
        String powershell = Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe").toString();
        // Mantém caminhos com espaços, apóstrofos e cifrões como dados literais.
        StringBuilder invocation = new StringBuilder("& ").append(powershellLiteral(powershell))
                .append(" -NoProfile -NonInteractive -ExecutionPolicy Bypass -File ")
                .append(powershellLiteral(script.toString()));
        for (String argument : arguments) invocation.append(' ').append(powershellLiteral(argument));
        String elevated = "$ErrorActionPreference='Stop'; $env:TAG_FILE_JDK=" + powershellLiteral(javaHome)
                + "; try { " + invocation + " *> " + powershellLiteral(log.toString())
                + "; if ($null -eq $LASTEXITCODE) { exit 0 }; exit $LASTEXITCODE } "
                + "catch { $_ | Out-File -LiteralPath " + powershellLiteral(log.toString()) + " -Append; exit 1 }";
        String request = "$ErrorActionPreference='Stop'; try { $process=Start-Process -FilePath "
                + powershellLiteral(powershell)
                + " -ArgumentList '-NoProfile','-NonInteractive','-EncodedCommand','" + encodedCommand(elevated)
                + "' -Verb RunAs -Wait -PassThru; exit $process.ExitCode } "
                + "catch { $cause=$_.Exception; while ($null -ne $cause) { "
                + "if ($cause -is [System.ComponentModel.Win32Exception] -and $cause.NativeErrorCode -eq 1223) { exit 126 }; "
                + "$cause=$cause.InnerException }; Write-Error $_ -ErrorAction Continue; exit 127 }";
        return List.of(powershell, "-NoProfile", "-NonInteractive", "-EncodedCommand", encodedCommand(request));
    }

    /**
     * Delimita um valor como texto literal no PowerShell.
     * @param value texto não interpretado como código
     * @return literal entre apóstrofos, com apóstrofos internos duplicados
     */
    private static String powershellLiteral(String value) { return "'" + value.replace("'", "''") + "'"; }

    /**
     * Prepara o formato aceito pelo PowerShell, sem escapes de linha de comando adicionais.
     * @param command código formado somente com valores literais escapados
     * @return código em Base64 de UTF-16LE
     */
    private static String encodedCommand(String command) {
        return Base64.getEncoder().encodeToString(command.getBytes(StandardCharsets.UTF_16LE));
    }

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
