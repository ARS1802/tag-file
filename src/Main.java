/*
 * Inventário — Main.
 * Main(): impede instanciação da entrada estática.
 * main(String[]): inicia a aplicação e apresenta falhas de inicialização.
 * executeScriptWithElevation(Path, String...): delega a autorização ao sistema operacional.
 */

import GUI.SwingInteraction;
import application.Application;
import service.ElevatedScriptExecutor;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;

/** Ponto de entrada da aplicação Tag-File. */
public final class Main {
    /** Impede instanciação da entrada estática. */
    private Main() { }

    /**
     * Inicia a interface e os serviços necessários à sessão.
     *
     * @param args vazio para abrir a aplicação; --build para compilar com elevação
     * @throws Exception se a inicialização falhar
     */
    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("O Tag-File precisa de um ambiente gráfico para iniciar.");
        }

        // Apresenta confirmações e falhas antes de a janela principal existir.
        SwingInteraction dialogs = new SwingInteraction(null);
        try {
            if (args.length == 1 && args[0].equals("--build")) {
                String extension = System.getProperty("os.name").startsWith("Windows") ? ".ps1" : ".sh";
                executeScriptWithElevation(Path.of("scripts/build" + extension));
                return;
            }
            if (args.length != 0) throw new IllegalArgumentException("Use Main ou Main --build.");
            // Prepara os serviços e mantém os recursos da aplicação durante a sessão.
            Application application = new Application(Path.of("."), dialogs, Main::executeScriptWithElevation);
            application.start();
        } catch (CancellationException cancelled) {
            // Cancelar a preparação encerra a inicialização normalmente.
        } catch (Exception failure) {
            dialogs.showFailure(failure);
            throw failure;
        }
    }

    /**
     * Executa um script com autorização nativa, mantendo o Java sem elevação.
     * A senha é solicitada pelo sistema e nunca recebida pela aplicação.
     *
     * @param script caminho do script confiável do projeto
     * @param arguments argumentos separados, sem interpretação como código de shell
     * @throws IOException se a autorização ou o script falhar
     * @throws InterruptedException se a espera for interrompida
     * @throws CancellationException se o usuário cancelar a autorização
     */
    public static void executeScriptWithElevation(Path script, String... arguments)
            throws IOException, InterruptedException {
        ElevatedScriptExecutor.execute(script, arguments);
    }
}
