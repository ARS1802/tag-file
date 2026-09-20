/*
 * Inventário — Application.
 * Application(Path, SwingInteraction, Installer): define ambiente, diálogos e instalação autorizada.
 * start(): prepara a persistência, conecta os colaboradores e abre a janela.
 * prepareEnvironment(): prepara o MySQL e solicita autorização se faltar instalação.
 * close(): fecha a conexão, a instância exclusiva do projeto e a janela.
 */

package application;

import GUI.MainWindow;
import GUI.PreparationWindow;
import service.PreparationProgress;
import GUI.SwingInteraction;
import controller.ExplorerContext;
import controller.LocalFileExplorerController;
import controller.TagExplorerController;
import manager.LocalFileManager;
import model.EntityFactory;
import model.NativeDirectory;
import persistence.DatabaseConnection;
import persistence.DatabaseManager;
import persistence.LocalFileDAO;
import persistence.LocalFileTagDAO;
import persistence.TagDAO;
import service.ActionGate;
import service.ClipboardService;
import service.ExplorerEventService;
import service.NativeFileService;
import java.io.IOException;
import java.nio.file.Path;

/** Compõe os objetos compartilhados e controla o ciclo de vida da aplicação. */
public final class Application implements AutoCloseable {
    /** Raiz que contém a configuração e os scripts do projeto. */
    private final Path root;
    /** Apresentação de confirmações e falhas da sessão. */
    private final SwingInteraction dialogs;
    /** Administração da instância exclusiva de MySQL. */
    private final DatabaseManager environment;
    /** Conexão compartilhada pelos DAOs, fechada somente pela aplicação. */
    private DatabaseConnection database;
    /** Janela da sessão, criada e descartada na EDT. */
    private MainWindow window;
    /** Indica que a preparação terminou e a instância pode ser encerrada. */
    private boolean environmentPrepared;

    /**
     * Define os recursos usados na inicialização.
     *
     * @param root raiz do projeto
     * @param dialogs diálogos compartilhados pelas operações
     * @param installer executa a instalação Linux com autorização do sistema
     */
    public Application(Path root, SwingInteraction dialogs, DatabaseManager.Installer installer) {
        this.root = root.toAbsolutePath().normalize();
        this.dialogs = dialogs;
        // Administra apenas o servidor pertencente a este projeto.
        environment = new DatabaseManager(this.root, installer);
    }

    /**
     * Prepara a sessão fora da EDT e monta a interface na EDT.
     * Deve ser chamado uma única vez, pela entrada da aplicação.
     *
     * @throws Exception se ambiente, banco ou interface não puderem iniciar
     */
    public void start() throws Exception {
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Execute a inicialização fora da EDT");
        }
        try (PreparationWindow progress = PreparationWindow.open(root.resolve("database/runtime/logs"))) {
            environment.setProgressListener(progress::update);
            prepareEnvironment();
            progress.update(new PreparationProgress("Conectando ao banco", -1, null));
            // Compartilha uma conexão JDBC entre todos os DAOs.
            database = new DatabaseConnection(root.resolve("database/config/database.properties"));
            database.open();
            progress.update(new PreparationProgress("Verificando estrutura do banco", -1, null));
            environment.prepareSchema(database);

            // Lê e manipula os arquivos físicos.
            NativeFileService nativeService = new NativeFileService();
            // Cria as entidades com identidade e valores iniciais válidos.
            EntityFactory factory = new EntityFactory(nativeService);
            // Persiste as etiquetas e suas restrições de extensão.
            TagDAO tags = new TagDAO(database);
            // Persiste os vínculos entre arquivos e etiquetas.
            LocalFileTagDAO links = new LocalFileTagDAO(database, tags);
            // Persiste os cadastros e reconstrói suas etiquetas.
            LocalFileDAO files = new LocalFileDAO(database, links);
            // Aplica as regras de classificação e sincronização dos cadastros.
            LocalFileManager manager = new LocalFileManager(nativeService, factory, files, tags, links);
            progress.update(new PreparationProgress("Atualizando cadastros", -1, null));
            manager.initialize();

            // Compartilha copiar/recortar entre os dois exploradores.
            ClipboardService clipboard = new ClipboardService();
            // Entrega as atualizações aos painéis inscritos.
            ExplorerEventService events = new ExplorerEventService();
            // Permite somente uma operação por vez na sessão.
            ActionGate gate = new ActionGate();
            // Abre o explorador na pasta pessoal do usuário.
            NativeDirectory directory = new NativeDirectory(Path.of(System.getProperty("user.home")));
            // Reúne as dependências e o estado usados pelos dois controladores.
            ExplorerContext context = new ExplorerContext(manager, nativeService, files, tags,
                    clipboard, dialogs, events, gate, directory, dialogs::showFailure);
            // Recebe as ações da visão por etiquetas.
            TagExplorerController tagController = new TagExplorerController(context);
            // Recebe as ações da visão de arquivos físicos.
            LocalFileExplorerController localController = new LocalFileExplorerController(context);

            // Compõe os painéis e vincula o fechamento à liberação dos recursos.
            ActionGate.onEdt(() -> window = new MainWindow(tagController, localController,
                    events, gate, directory, this));
            progress.update(new PreparationProgress("Abrindo exploradores", -1, null));
            localController.navigate(directory).get();
            ActionGate.onEdt(window::show);
        } catch (Exception failure) {
            try { close(); }
            catch (Exception cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            throw failure;
        } finally { environment.setProgressListener(update -> { }); }
    }

    /**
     * Verifica os componentes e solicita consentimento antes de preparar o banco.
     *
     * @throws IOException se a preparação falhar
     * @throws InterruptedException se a espera pelos scripts for interrompida
     * @throws java.util.concurrent.CancellationException se o usuário cancelar a instalação
     */
    private void prepareEnvironment() throws IOException, InterruptedException {
        try {
            environment.prepare(false);
        } catch (DatabaseManager.InstallationRequiredException missing) {
            if (dialogs.choose("O Tag-File precisa preparar o MySQL na pasta do projeto. Deseja continuar?",
                    "Preparar", "Cancelar") != 0) {
                throw new java.util.concurrent.CancellationException("Preparação cancelada");
            }
            environment.prepare(true);
        }
        environmentPrepared = true;
    }

    /**
     * Fecha JDBC antes do servidor e descarta a interface, preservando os dados.
     * Deve ser chamado fora da EDT, depois de a operação corrente terminar.
     *
     * @throws Exception se algum recurso não puder ser encerrado
     */
    @Override public void close() throws Exception {
        Exception failure = null;
        try {
            if (database != null) database.close();
        } catch (Exception error) { failure = error; }
        try {
            if (environmentPrepared) {
                environment.stop();
                environmentPrepared = false;
            }
        } catch (Exception error) {
            if (failure == null) failure = error; else failure.addSuppressed(error);
        }
        try {
            if (window != null) ActionGate.onEdt(window::dispose);
        } catch (Exception error) {
            if (failure == null) failure = error; else failure.addSuppressed(error);
        }
        if (failure != null) throw failure;
    }
}
