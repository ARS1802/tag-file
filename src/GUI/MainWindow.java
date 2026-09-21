/*
 * Inventário — MainWindow.
 * MainWindow(...): compõe os exploradores, as ações globais e o indicador de trabalho; aplica fonte 13pt a todo o Look and Feel antes de montar os Panels.
 * show(): exibe a janela após a carga inicial dos dados.
 * updateLayout(boolean): alterna entre abas e painéis lado a lado.
 * requestClose(): solicita o encerramento quando não existe outra ação em andamento.
 * dispose(): remove as inscrições e descarta os componentes Swing.
 * applyFontDefaults(): define fonte 13pt nos componentes Swing padrão e desliga o beep de feedback do Windows, uma única vez, antes de qualquer componente ser criado.
 * fadeIn(Window): anima a opacidade de 0 a 1 para a janela de progresso não aparecer abruptamente.
 */

package GUI;

import controller.LocalFileExplorerController;
import controller.TagExplorerController;
import model.NativeDirectory;
import service.ActionGate;
import service.ExplorerEventService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/** Janela principal com os exploradores por etiquetas e por diretórios. */
public final class MainWindow {
    /** Janela que contém toda a interface da aplicação. */
    private final JFrame frame = new JFrame("Tag-File");
    /** Painel de etiquetas e resultados de pesquisa. */
    private final TagExplorerPanel tags;
    /** Painel de navegação pelos arquivos físicos. */
    private final LocalFileExplorerPanel local;
    /** Área que recebe as abas ou a divisão horizontal. */
    private final JPanel views = new JPanel(new BorderLayout());
    /** Alternativa de navegação por abas. */
    private final JTabbedPane tabs = new JTabbedPane();
    /** Ação global de sincronização. */
    private final JButton refresh = new JButton("Refresh");
    /** Escolha de disposição dos exploradores. */
    private final JCheckBox sideBySide = new JCheckBox("Mostrar lado a lado");
    /** Indicador visível enquanto uma operação está em andamento. */
    private final JDialog loading;
    /** Diálogos associados à janela principal. */
    private final SwingInteraction dialogs;
    /** Controle de operações compartilhado com os controladores. */
    private final ActionGate gate;
    /** Encerramento dos recursos pertencentes à sessão. */
    private final AutoCloseable session;
    /** Impede que a remontagem das abas solicite outra operação. */
    private boolean changingLayout;

    /**
     * Monta a interface sem exibi-la antes de receber os dados iniciais.
     *
     * @param tagController ações do explorador por etiquetas
     * @param localController ações do explorador físico
     * @param events avisos compartilhados entre os painéis
     * @param gate controle global de operações
     * @param directory pasta inicial de navegação
     * @param session recursos a liberar ao fechar a janela
     * @throws IllegalStateException se chamado fora da EDT
     */
    public MainWindow(TagExplorerController tagController, LocalFileExplorerController localController,
                      ExplorerEventService events, ActionGate gate, NativeDirectory directory, AutoCloseable session) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Crie a janela na EDT");
        applyFontDefaults();
        this.gate = gate;
        this.session = session;
        // Apresenta os cadastros organizados pelas etiquetas.
        tags = new TagExplorerPanel(tagController, events, gate, directory);
        // Apresenta os arquivos e diretórios da máquina.
        local = new LocalFileExplorerPanel(localController, events, gate, directory);
        // Mantém as mensagens desta janela associadas a ela.
        dialogs = new SwingInteraction(frame);

        // Organiza a barra de ações acima dos exploradores.
        JPanel content = new JPanel(new BorderLayout());
        // Agrupa as ações que afetam a janela inteira.
        JPanel toolbar = new JPanel();
        refresh.addActionListener(event -> dialogs.observe(localController.refresh()));
        toolbar.add(refresh);
        toolbar.add(sideBySide);
        updateLayout(false);
        tabs.addChangeListener(event -> {
            if (!changingLayout) dialogs.observe(localController.refresh());
        });
        sideBySide.addActionListener(event -> {
            if (gate.isBusy()) {
                sideBySide.setSelected(!sideBySide.isSelected());
                return;
            }
            boolean split = sideBySide.isSelected();
            gate.submit(() -> {
                ActionGate.onEdt(() -> updateLayout(split));
                return null;
            }).exceptionally(error -> { dialogs.showFailure(error); return null; });
        });
        content.add(toolbar, BorderLayout.NORTH);
        content.add(views, BorderLayout.CENTER);
        frame.setContentPane(content);

        // Informa progresso sem bloquear a thread de eventos; sem moldura própria, decorada por nós mesmos.
        loading = new JDialog(frame, false);
        loading.setUndecorated(true);
        loading.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel loadingContent = new JPanel(new BorderLayout(0, 12));
        loadingContent.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x60, 0x60, 0x60), 1),
                BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        JLabel loadingLabel = new JLabel("Carregando…", SwingConstants.CENTER);
        loadingLabel.setFont(loadingLabel.getFont().deriveFont(Font.BOLD, 17f));
        // Representa operações cujo tempo restante é desconhecido.
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setPreferredSize(new Dimension(260, 26));
        loadingContent.add(loadingLabel, BorderLayout.NORTH);
        loadingContent.add(progress, BorderLayout.CENTER);
        loading.setContentPane(loadingContent);
        loading.setSize(340, 130);

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // Encaminha o fechamento para a liberação ordenada dos recursos.
        frame.addWindowListener(new WindowAdapter() {
            /**
             * Solicita o encerramento sem interromper a operação corrente.
             * @param event pedido de fechamento da janela
             */
            @Override public void windowClosing(WindowEvent event) { requestClose(); }
        });
        frame.setSize(1250, 680);
        frame.setLocationRelativeTo(null);
    }

    /** Exibe a janela e conecta o indicador de trabalho; chamar na EDT. */
    public void show() {
        gate.setBusyListener(busy -> {
            loading.setLocationRelativeTo(frame);
            if (busy) {
                loading.setOpacity(0f);
                loading.setVisible(true);
                fadeIn(loading);
            } else {
                loading.setVisible(false);
            }
            refresh.setEnabled(!busy);
            sideBySide.setEnabled(!busy);
        });
        frame.setVisible(true);
    }

    /**
     * Alterna a disposição sem disparar Refresh durante a remontagem.
     *
     * @param split se os painéis devem ficar lado a lado
     */
    private void updateLayout(boolean split) {
        changingLayout = true;
        try {
            views.removeAll();
            tabs.removeAll();
            if (split) {
                // Permite ajustar a largura relativa dos exploradores.
                JSplitPane divider = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tags, local);
                divider.setResizeWeight(.5);
                views.add(divider, BorderLayout.CENTER);
            } else {
                tabs.addTab("Arquivos por Tag", tags);
                tabs.addTab("Arquivos Local", local);
                views.add(tabs, BorderLayout.CENTER);
            }
            views.revalidate();
            views.repaint();
        } finally { changingLayout = false; }
    }

    /** Fecha a sessão fora da EDT, respeitando a exclusividade das operações. */
    private void requestClose() {
        if (gate.isBusy()) return;
        gate.submit(() -> {
            try { session.close(); }
            finally { ActionGate.onEdt(this::dispose); }
            return null;
        }).exceptionally(error -> { dialogs.showFailure(error); return null; });
    }

    /** Remove observadores e descarta a interface; chamar na EDT. */
    public void dispose() {
        gate.setBusyListener(busy -> { });
        tags.dispose();
        local.dispose();
        loading.dispose();
        frame.dispose();
    }

    /**
     * Define fonte 13pt nos componentes Swing padrão, uma única vez, antes de qualquer componente ser criado.
     * Também desliga o beep de feedback de cliques inválidos do Look and Feel.
     * Só afeta apresentação (UIManager); não altera nenhum contrato de domínio ou persistência.
     */
    private static void applyFontDefaults() {
        // Some Look and Feels (ex.: Windows) tocam um beep de feedback em cliques inválidos, como botões desabilitados.
        UIManager.put("AuditoryCues.playList", null);
        Font base = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        for (String key : new String[]{
                "Label.font", "Button.font", "CheckBox.font", "RadioButton.font", "ComboBox.font",
                "List.font", "Tree.font", "TextField.font", "TextArea.font", "FormattedTextField.font",
                "Spinner.font", "TabbedPane.font", "ToolTip.font", "ProgressBar.font", "Panel.font",
                "OptionPane.messageFont", "OptionPane.buttonFont"}) {
            UIManager.put(key, base);
        }
    }

    /**
     * Anima a opacidade de 0 a 1 para a janela de progresso não aparecer abruptamente.
     *
     * @param window janela já visível, com opacidade inicial zero
     */
    private static void fadeIn(Window window) {
        Timer timer = new Timer(15, null);
        timer.addActionListener(event -> {
            float next = window.getOpacity() + 0.12f;
            if (next >= 1f) { window.setOpacity(1f); timer.stop(); }
            else window.setOpacity(next);
        });
        timer.start();
    }
}