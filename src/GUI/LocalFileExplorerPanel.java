/*
 * Inventário — LocalFileExplorerPanel.
 * Apresenta disco enriquecido por Map; ausência de cadastro mantém a linha visível (EXP-03).
 * Navegação em árvore expande via NativeFileService.listDirectories (ARQ-07, sem SQL/eventos); só entra na pasta via Controller.navigate ao selecionar.
 * A raiz da árvore é fixa (raiz do disco da pasta inicial), montada uma única vez; navegar só revela e seleciona o caminho atual, sem reconstruir a árvore.
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - LocalFileExplorerPanel.LocalFileExplorerPanel(LocalFileExplorerController controller, ExplorerEventService events, ActionGate gate, NativeDirectory directory): Constrói componentes e registra Observer na EDT; não acessa banco no construtor.
 * - DefaultListCellRenderer (anônima).getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused): Prepara o rótulo visual sem acessar disco ou SQL.
 * - LocalFileExplorerPanel.createNode(NativeDirectory value): Monta nó da árvore com um filho-placeholder, sem consultar disco ainda.
 * - LocalFileExplorerPanel.ensureLoaded(DefaultMutableTreeNode node): Substitui o placeholder pelas subpastas reais na primeira vez que o nó é usado, sem repetir depois.
 * - LocalFileExplorerPanel.revealDirectory(NativeDirectory target): Desce pela árvore fixa carregando sob demanda até selecionar a pasta atual.
 * - LocalFileExplorerPanel.promptBytes(String title, Long current): Pede um tamanho em bytes por pop-up com JSpinner, sem aceitar texto livre.
 * - LocalFileExplorerPanel.promptDateTime(String title, LocalDateTime current): Pede uma data/hora UTC por pop-up com JSpinner, sem aceitar texto livre.
 * - LocalFileExplorerPanel.labelBytes(JButton button, String prefix, Long value): Atualiza o rótulo do botão de bytes com o valor atual.
 * - LocalFileExplorerPanel.labelDate(JButton button, String prefix, LocalDateTime value): Atualiza o rótulo do botão de data com o valor atual.
 * - TreeWillExpandListener (anônima).treeWillExpand(TreeExpansionEvent event): Carrega subpastas reais via NativeFileService somente na primeira expansão do nó.
 * - TreeWillExpandListener (anônima).treeWillCollapse(TreeExpansionEvent event): Não faz nada; estrutura já carregada permanece em memória.
 * - LocalFileExplorerPanel.DirNode.DirNode(NativeDirectory directory): Envolve a pasta para rotular o nó pelo nome, não pelo caminho inteiro.
 * - LocalFileExplorerPanel.DirNode.toString(): Informa o nome da pasta para o rótulo da árvore.
 * - LocalFileExplorerPanel.filter(): Captura critérios de UI e encaminha uma única ação de filtro/Refresh.
 * - LocalFileExplorerPanel.copy(boolean cut): Encaminha a intenção de copiar ou recortar o arquivo selecionado.
 * - LocalFileExplorerPanel.withFile(Consumer<NativeFile> action): Encaminha a ação somente quando existe arquivo selecionado.
 * - LocalFileExplorerPanel.addButton(JPanel panel, String text, Runnable action): Liga um botão à entrada correspondente do Controller.
 * - LocalFileExplorerPanel.onTagsChanged(List<Tag> tags): Aplica a fotografia de Tags sem iniciar outra consulta ou Refresh.
 * - LocalFileExplorerPanel.onFilesChanged(List<LocalFile> matches): Atualiza a apresentação usando o aviso recebido, sem repetir Refresh.
 * - LocalFileExplorerPanel.onDirectoryChanged(NativeDirectory directory, List<NativeDirectory> subdirectories, List<NativeFile> nativeFiles, Map<Path, LocalFile> registered): Atualiza componentes, revela a pasta atual na árvore fixa e a contagem de arquivos exibidos.
 * - LocalFileExplorerPanel.dispose(): Remove a inscrição ao descartar a janela.
 * - LocalFileExplorerPanel.getFileList(): Informa lista visual de arquivos físicos.
 *
 * Consulte: doc/interface-e-fluxos.md — UI-01 a UI-03; doc/arquitetura-e-padroes.md — ARQ-06/ARQ-07.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package GUI;

import controller.LocalFileExplorerController;
import filter.NativeFileFilter;
import manager.LocalFileManager;
import model.*;
import service.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Apresenta disco enriquecido por Map; ausência de cadastro mantém a linha visível (EXP-03).
 */
public final class LocalFileExplorerPanel extends JPanel implements ExplorerListener {
    /**
     * Versão de serialização exigida pelo tipo herdado; não implementa persistência da aplicação.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Marcador de nó ainda não expandido; nunca é exibido, só habilita a seta de expansão.
     */
    private static final Object PLACEHOLDER = new Object();
    /**
     * Coordenador que recebe solicitações desta apresentação.
     */
    private final LocalFileExplorerController controller;
    /**
     * Serviço compartilhado de inscrições e avisos.
     */
    private final ExplorerEventService events;
    /**
     * Apresentação de diálogos e rejeições na EDT.
     */
    private final SwingInteraction dialogs;
    /**
     * Leitura de disco pura, sem SQL nem eventos (ARQ-07); usada só para expandir a árvore.
     */
    private final NativeFileService disk = new NativeFileService();
    /**
     * Lista visual de arquivos físicos, cadastrados ou não.
     */
    private final JList<NativeFile> files = new JList<>(new DefaultListModel<>());
    /**
     * Árvore de navegação; expande via disco puro, entra na pasta via Controller ao selecionar.
     */
    private final JTree tree = new JTree(new DefaultMutableTreeNode());
    /**
     * Campo de caminho da pasta escolhida.
     */
    private final JTextField path = new JTextField(35);
    /**
     * Entrada de extensões do filtro físico.
     */
    private final JTextField extensions = new JTextField(8);
    /**
     * Abre o pop-up de tamanho mínimo; rótulo mostra o valor atual.
     */
    private final JButton minButton = new JButton();
    /**
     * Abre o pop-up de tamanho máximo; rótulo mostra o valor atual.
     */
    private final JButton maxButton = new JButton();
    /**
     * Abre o pop-up de modificação mínima UTC; rótulo mostra o valor atual.
     */
    private final JButton fromButton = new JButton();
    /**
     * Abre o pop-up de modificação máxima UTC; rótulo mostra o valor atual.
     */
    private final JButton toButton = new JButton();
    /**
     * Tamanho mínimo em bytes escolhido no pop-up; nulo significa sem limite.
     */
    private Long minValue;
    /**
     * Tamanho máximo em bytes escolhido no pop-up; nulo significa sem limite.
     */
    private Long maxValue;
    /**
     * Modificação mínima UTC escolhida no pop-up; nula significa sem limite.
     */
    private LocalDateTime fromValue;
    /**
     * Modificação máxima UTC escolhida no pop-up; nula significa sem limite.
     */
    private LocalDateTime toValue;
    /**
     * Mapa de caminhos para cadastros reconstruídos; ausências continuam visíveis.
     */
    private Map<Path, LocalFile> registered = Map.of();
    /**
     * Pasta atual usada para navegação e colagem.
     */
    private NativeDirectory directory;
    /**
     * Contagem de arquivos exibidos, atualizada a cada aviso de pasta.
     */
    private final JLabel status = new JLabel(" ");

    /**
     * Constrói componentes e registra Observer na EDT; não acessa banco no construtor.
     *
     * @param controller coordenador de navegação/operações
     * @param events observadores compartilhados
     * @param gate controle global para atalhos e Drag
     * @param directory pasta inicial
     * @throws IllegalStateException se chamado fora da EDT
     */
    public LocalFileExplorerPanel(LocalFileExplorerController controller, ExplorerEventService events, ActionGate gate, NativeDirectory directory) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Crie o painel na EDT");
        this.controller = controller; this.events = events; this.directory = directory; this.dialogs = new SwingInteraction(this);
        setLayout(new BorderLayout(6, 6)); files.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); path.setText(directory.toString());
        JPanel navigation = new JPanel(new BorderLayout(6, 0));
        navigation.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        navigation.add(new JLabel("Pasta:"), BorderLayout.WEST);
        navigation.add(path, BorderLayout.CENTER);
        JPanel navigationActions = new JPanel(new GridLayout(1, 2, 4, 0));
        JButton goButton = addButton(navigationActions, "Ir", () -> dialogs.observe(controller.navigate(new NativeDirectory(Path.of(path.getText())))));
        goButton.setToolTipText("Ir para o caminho digitado");
        JButton upButton = addButton(navigationActions, "Subir", () -> { Path parent = this.directory.getPath().getParent(); if (parent != null) dialogs.observe(controller.navigate(new NativeDirectory(parent))); });
        upButton.setToolTipText("Subir para a pasta pai");
        navigation.add(navigationActions, BorderLayout.EAST);
        path.addActionListener(event -> goButton.doClick());
        add(navigation, BorderLayout.NORTH);

        tree.setRootVisible(true); tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        Path root = directory.getPath().getRoot();
        tree.setModel(new DefaultTreeModel(createNode(new NativeDirectory(root == null ? directory.getPath() : root))));
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override public void treeWillExpand(TreeExpansionEvent event) {
                ensureLoaded((DefaultMutableTreeNode) event.getPath().getLastPathComponent());
            }
            @Override public void treeWillCollapse(TreeExpansionEvent event) { }
        });
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof DirNode selected && !selected.directory.getPath().equals(this.directory.getPath()))
                dialogs.observe(controller.navigate(selected.directory));
        });
        revealDirectory(directory);

        JScrollPane treeScroll = new JScrollPane(tree); treeScroll.setPreferredSize(new Dimension(200, 0));
        JSplitPane browser = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, new JScrollPane(files));
        browser.setResizeWeight(.12); browser.setDividerLocation(200); add(browser, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        JPanel criteria = new JPanel(new GridLayout(0, 4, 4, 4));
        criteria.setBorder(BorderFactory.createTitledBorder("Filtros"));
        criteria.add(new JLabel("Extensões")); criteria.add(extensions); criteria.add(minButton); criteria.add(maxButton);
        criteria.add(fromButton); criteria.add(toButton); addButton(criteria, "Filtrar", this::filter);
        extensions.setToolTipText("Separadas por vírgula, sem ponto (ex.: txt,pdf); <sem> aceita arquivo sem extensão; vazio aceita todas");
        minButton.addActionListener(e -> { minValue = promptBytes("Bytes mínimos", minValue); labelBytes(minButton, "Bytes mínimos", minValue); });
        maxButton.addActionListener(e -> { maxValue = promptBytes("Bytes máximos", maxValue); labelBytes(maxButton, "Bytes máximos", maxValue); });
        fromButton.addActionListener(e -> { fromValue = promptDateTime("Modificação desde", fromValue); labelDate(fromButton, "Modificação desde", fromValue); });
        toButton.addActionListener(e -> { toValue = promptDateTime("Modificação até", toValue); labelDate(toButton, "Modificação até", toValue); });
        labelBytes(minButton, "Bytes mínimos", null); labelBytes(maxButton, "Bytes máximos", null);
        labelDate(fromButton, "Modificação desde", null); labelDate(toButton, "Modificação até", null);
        criteria.setVisible(false);
        bottom.add(criteria, BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 4, 4, 4));
        actions.setBorder(BorderFactory.createTitledBorder("Ações do arquivo selecionado"));
        JToggleButton filterToggle = new JToggleButton("Mostrar filtros…");
        filterToggle.setToolTipText("Exibir ou ocultar os critérios de filtragem");
        filterToggle.addActionListener(event -> {
            boolean visible = filterToggle.isSelected();
            criteria.setVisible(visible);
            filterToggle.setText(visible ? "Ocultar filtros" : "Mostrar filtros…");
            bottom.revalidate();
            bottom.repaint();
        });
        actions.add(filterToggle);
        JButton openButton = addButton(actions, "Abrir", () -> withFile(f -> dialogs.observe(controller.open(f))));
        JButton copyButton = addButton(actions, "Copiar", () -> copy(false));
        JButton cutButton = addButton(actions, "Recortar", () -> copy(true));
        JButton pasteButton = addButton(actions, "Colar", () -> dialogs.observe(controller.paste(this.directory)));
        JButton moveButton = addButton(actions, "Mover para…", () -> withFile(f -> dialogs.observe(controller.chooseMove(f))));
        JButton renameButton = addButton(actions, "Renomear", () -> withFile(f -> dialogs.observe(controller.rename(f))));
        JButton deleteButton = addButton(actions, "Apagar arquivo…", () -> withFile(f -> dialogs.observe(controller.delete(f))));
        status.setBorder(BorderFactory.createEmptyBorder(0, 4, 2, 4));
        bottom.add(actions, BorderLayout.CENTER);
        bottom.add(status, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        openButton.setToolTipText("Abrir arquivo (Enter ou duplo clique)");
        copyButton.setToolTipText("Copiar arquivo (Ctrl+C)");
        cutButton.setToolTipText("Recortar arquivo (Ctrl+X)");
        pasteButton.setToolTipText("Colar nesta pasta (Ctrl+V)");
        renameButton.setToolTipText("Renomear arquivo (F2)");
        SwingInteraction.bind(this, "ctrl C", "copy", () -> copy(false)); SwingInteraction.bind(this, "ctrl X", "cut", () -> copy(true));
        SwingInteraction.bind(this, "ctrl V", "paste", () -> dialogs.observe(controller.paste(this.directory)));
        SwingInteraction.bind(this, "F2", "rename", () -> withFile(f -> dialogs.observe(controller.rename(f))));
        SwingInteraction.bind(files, "ENTER", "open-selected-file", openButton::doClick);
        files.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                int index = files.locationToIndex(event.getPoint());
                Rectangle bounds = index < 0 ? null : files.getCellBounds(index, index);
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)
                        && bounds != null && bounds.contains(event.getPoint())) openButton.doClick();
            }
        });
        files.setTransferHandler(new FileTransferHandler(gate, null)); if (!GraphicsEnvironment.isHeadless()) files.setDragEnabled(true);

        List<JButton> fileDependent = List.of(openButton, copyButton, cutButton, moveButton, renameButton, deleteButton);
        Runnable updateEnabled = () -> {
            boolean hasFile = files.getSelectedValue() != null;
            fileDependent.forEach(b -> b.setEnabled(hasFile));
        };
        files.addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateEnabled.run(); });
        updateEnabled.run();

        files.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof NativeFile file) {
                    LocalFile local = registered.get(file.getPath());
                    label.setText(SwingLabels.nativeFile(file, local));
                }
                return label;
            }
        });
        events.subscribe(this);
    }

    /**
     * Monta nó da árvore com um filho-placeholder, sem consultar disco ainda.
     *
     * @param value pasta representada pelo nó
     * @return nó pronto para exibir a seta de expansão, sem carregar filhos reais
     */
    private static DefaultMutableTreeNode createNode(NativeDirectory value) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new DirNode(value));
        node.add(new DefaultMutableTreeNode(PLACEHOLDER));
        return node;
    }
    /**
     * Substitui o placeholder pelas subpastas reais na primeira vez que o nó é usado, sem repetir depois.
     *
     * @param node nó cujos filhos devem estar carregados ao final
     */
    private void ensureLoaded(DefaultMutableTreeNode node) {
        if (node.getChildCount() == 1 && ((DefaultMutableTreeNode) node.getChildAt(0)).getUserObject() == PLACEHOLDER && node.getUserObject() instanceof DirNode current) {
            node.removeAllChildren();
            try { for (NativeDirectory sub : disk.listDirectories(current.directory)) node.add(createNode(sub)); }
            catch (IOException e) { dialogs.showFailure(e); }
            ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(node);
        }
    }
    /**
     * Desce pela árvore fixa carregando sob demanda até selecionar a pasta atual.
     *
     * @param target pasta a revelar; sem efeito se estiver fora da raiz fixa da árvore
     */
    private void revealDirectory(NativeDirectory target) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getModel().getRoot();
        ensureLoaded(node);
        for (Path part : target.getPath()) {
            DefaultMutableTreeNode next = null;
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                if (child.getUserObject() instanceof DirNode dir && part.equals(dir.directory.getPath().getFileName())) { next = child; break; }
            }
            if (next == null) return;
            node = next; ensureLoaded(node);
        }
        TreePath treePath = new TreePath(((DefaultTreeModel) tree.getModel()).getPathToRoot(node));
        tree.expandPath(treePath.getParentPath() == null ? treePath : treePath.getParentPath());
        tree.setSelectionPath(treePath); tree.scrollPathToVisible(treePath);
    }
    /**
     * Captura critérios de UI e encaminha uma única ação de filtro/Refresh.
     */
    private void filter() {
        try {
            dialogs.observe(controller.filter(new NativeFileFilter(LocalFileManager.parseExtensions(extensions.getText()), minValue, maxValue, fromValue, toValue)));
        } catch (RuntimeException e) { dialogs.showFailure(e); }
    }
    /**
     * Pede um tamanho em bytes por pop-up com JSpinner, sem aceitar texto livre.
     *
     * @param title assunto mostrado no pop-up
     * @param current valor atual, usado como ponto de partida e devolvido se cancelado
     * @return valor definido, nulo para "sem limite", ou o valor atual se cancelado
     */
    private Long promptBytes(String title, Long current) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(current == null ? 0L : current, 0L, Long.MAX_VALUE, 1L));
        ((JSpinner.NumberEditor) spinner.getEditor()).getTextField().setColumns(14);
        Object[] options = {"Definir", "Sem limite", "Cancelar"};
        int choice = JOptionPane.showOptionDialog(this, spinner, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 0) return (Long) spinner.getValue();
        if (choice == 1) return null;
        return current;
    }
    /**
     * Pede uma data/hora UTC por pop-up com JSpinner, sem aceitar texto livre.
     *
     * @param title assunto mostrado no pop-up
     * @param current valor atual, usado como ponto de partida e devolvido se cancelado
     * @return valor definido, nulo para "sem limite", ou o valor atual se cancelado
     */
    private LocalDateTime promptDateTime(String title, LocalDateTime current) {
        LocalDateTime base = current == null ? LocalDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0) : current;
        SpinnerDateModel model = new SpinnerDateModel();
        model.setValue(Date.from(base.toInstant(ZoneOffset.UTC)));
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd HH:mm");
        editor.getFormat().setTimeZone(TimeZone.getTimeZone("UTC"));
        spinner.setEditor(editor);
        Object[] options = {"Definir", "Sem limite", "Cancelar"};
        int choice = JOptionPane.showOptionDialog(this, spinner, title + " (UTC)", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 0) return LocalDateTime.ofInstant(((Date) spinner.getValue()).toInstant(), ZoneOffset.UTC);
        if (choice == 1) return null;
        return current;
    }
    /**
     * Atualiza o rótulo do botão de bytes com o valor atual.
     *
     * @param button botão a rotular
     * @param prefix nome do critério
     * @param value valor atual, ou nulo para "sem limite"
     */
    private static void labelBytes(JButton button, String prefix, Long value) { button.setText(prefix + ": " + (value == null ? "sem limite" : value + " bytes")); }
    /**
     * Atualiza o rótulo do botão de data com o valor atual.
     *
     * @param button botão a rotular
     * @param prefix nome do critério
     * @param value valor atual, ou nulo para "sem limite"
     */
    private static void labelDate(JButton button, String prefix, LocalDateTime value) { button.setText(prefix + ": " + (value == null ? "sem limite" : value.toString())); }
    /**
     * Encaminha a intenção de copiar ou recortar o arquivo selecionado.
     *
     * @param cut indica se intenção é recortar
     */
    private void copy(boolean cut) { withFile(f -> dialogs.observe(controller.clipboard(f, cut))); }
    /**
     * Encaminha a ação somente quando existe arquivo selecionado.
     *
     * @param action ação a encaminhar caso haja arquivo selecionado
     */
    private void withFile(Consumer<NativeFile> action) { NativeFile file = files.getSelectedValue(); if (file != null) action.accept(file); }
    /**
     * Liga um botão à entrada correspondente do Controller.
     *
     * @param panel barra
     * @param text rótulo
     * @param action solicitação encaminhada
     * @return o botão criado, para controlar habilitação conforme a seleção
     */
    private static JButton addButton(JPanel panel, String text, Runnable action) { JButton button = new JButton(text); button.addActionListener(e -> action.run()); panel.add(button); return button; }
    /**
     * Aplica a fotografia de Tags sem iniciar outra consulta ou Refresh.
     *
     * @param tags fotografia de Tags; nomes são exibidos pelo mapa recebido na mesma publicação
     */
    @Override public void onTagsChanged(List<Tag> tags) { files.repaint(); }
    /**
     * Atualiza a apresentação usando o aviso recebido, sem repetir Refresh.
     *
     * @param matches pesquisa da outra visão; esta lista é física e não é substituída por ela
     */
    @Override public void onFilesChanged(List<LocalFile> matches) { files.repaint(); }
    /**
     * Atualiza somente componentes, preservando nativos sem correspondência.
     *
     * @param directory pasta atual
     * @param subdirectories filhos navegáveis
     * @param nativeFiles arquivos físicos filtrados
     * @param registered mapa obtido em lote
     */
    @Override public void onDirectoryChanged(NativeDirectory directory, List<NativeDirectory> subdirectories, List<NativeFile> nativeFiles, Map<Path, LocalFile> registered) {
        this.directory = directory; this.registered = Map.copyOf(registered); path.setText(directory.toString());
        revealDirectory(directory);
        Path selected = files.getSelectedValue() == null ? null : files.getSelectedValue().getPath();
        DefaultListModel<NativeFile> model = (DefaultListModel<NativeFile>) files.getModel(); model.clear(); model.addAll(nativeFiles);
        if (selected != null) for (int i = 0; i < model.size(); i++) if (model.get(i).getPath().equals(selected)) files.setSelectedIndex(i);
        files.setToolTipText(nativeFiles.isEmpty() ? "Pasta sem arquivos que correspondam aos critérios" : null);
        status.setText(nativeFiles.size() + (nativeFiles.size() == 1 ? " arquivo" : " arquivos"));
    }
    /**
     * Remove a inscrição ao descartar a janela.
     */
    public void dispose() { events.unsubscribe(this); }
    /**
     * Informa lista visual de arquivos físicos.
     *
     * @return lista visual de arquivos físicos
     */
    public JList<NativeFile> getFileList() { return files; }

    /**
     * Envolve a pasta para rotular o nó da árvore pelo nome, não pelo caminho inteiro.
     */
    private static final class DirNode {
        /**
         * Pasta representada por este nó.
         */
        private final NativeDirectory directory;
        /**
         * Envolve a pasta para rotular o nó pelo nome, não pelo caminho inteiro.
         *
         * @param directory pasta representada
         */
        private DirNode(NativeDirectory directory) { this.directory = directory; }
        /**
         * Informa o nome da pasta para o rótulo da árvore.
         *
         * @return nome da última parte do caminho, ou o caminho inteiro na raiz de um disco
         */
        @Override public String toString() {
            Path name = directory.getPath().getFileName();
            return name == null ? directory.toString() : name.toString();
        }
    }
}
