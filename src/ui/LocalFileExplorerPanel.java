/*
 * Inventário — LocalFileExplorerPanel.
 * Apresenta disco enriquecido por Map; ausência de cadastro mantém a linha visível (EXP-03).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - LocalFileExplorerPanel.LocalFileExplorerPanel(LocalFileExplorerController controller, ExplorerEventService events, ActionGate gate, NativeDirectory directory): Constrói componentes e registra Observer na EDT; não acessa banco no construtor.
 * - DefaultListCellRenderer (anônima).getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused): Prepara o rótulo visual sem acessar disco ou SQL.
 * - LocalFileExplorerPanel.filter(): Captura critérios de UI e encaminha uma única ação de filtro/Refresh.
 * - LocalFileExplorerPanel.copy(boolean cut): Encaminha a intenção de copiar ou recortar o arquivo selecionado.
 * - LocalFileExplorerPanel.withFile(Consumer<NativeFile> action): Encaminha a ação somente quando existe arquivo selecionado.
 * - LocalFileExplorerPanel.addButton(JPanel panel, String text, Runnable action): Liga um botão à entrada correspondente do Controller.
 * - LocalFileExplorerPanel.onTagsChanged(List<Tag> tags): Aplica a fotografia de Tags sem iniciar outra consulta ou Refresh.
 * - LocalFileExplorerPanel.onFilesChanged(List<LocalFile> matches): Atualiza a apresentação usando o aviso recebido, sem repetir Refresh.
 * - LocalFileExplorerPanel.onDirectoryChanged(NativeDirectory directory, List<NativeDirectory> subdirectories, List<NativeFile> nativeFiles, Map<Path, LocalFile> registered): Atualiza somente componentes, preservando nativos sem correspondência.
 * - LocalFileExplorerPanel.dispose(): Remove a inscrição ao descartar a janela.
 * - LocalFileExplorerPanel.getFileList(): Informa lista física para verificações da demonstração.
 *
 * Consulte: doc/interface-e-fluxos.md — UI-01 a UI-03; doc/arquitetura-e-padroes.md — ARQ-06.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package ui;

import controller.LocalFileExplorerController;
import filter.NativeFileFilter;
import manager.LocalFileManager;
import model.*;
import service.*;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * Apresenta disco enriquecido por Map; ausência de cadastro mantém a linha visível (EXP-03).
 */
public final class LocalFileExplorerPanel extends JPanel implements ExplorerListener {
    /**
     * Versão de serialização exigida pelo tipo herdado; não implementa persistência da aplicação.
     */
    private static final long serialVersionUID = 1L;
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
     * Lista visual de arquivos físicos, cadastrados ou não.
     */
    private final JList<NativeFile> files = new JList<>(new DefaultListModel<>());
    /**
     * Lista visual de subpastas navegáveis.
     */
    private final JComboBox<NativeDirectory> directories = new JComboBox<>();
    /**
     * Campo de caminho da pasta escolhida.
     */
    private final JTextField path = new JTextField(35);
    /**
     * Entrada de extensões do filtro físico.
     */
    private final JTextField extensions = new JTextField(8);
    /**
     * Entrada do tamanho mínimo em bytes.
     */
    private final JTextField min = new JTextField(6);
    /**
     * Entrada do tamanho máximo em bytes.
     */
    private final JTextField max = new JTextField(6);
    /**
     * Entrada da modificação mínima UTC.
     */
    private final JTextField from = new JTextField(16);
    /**
     * Entrada da modificação máxima UTC.
     */
    private final JTextField to = new JTextField(16);
    /**
     * Mapa de caminhos para cadastros reconstruídos; ausências continuam visíveis.
     */
    private Map<Path, LocalFile> registered = Map.of();
    /**
     * Pasta atual usada para navegação e colagem.
     */
    private NativeDirectory directory;

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
        JPanel navigation = new JPanel(new GridLayout(0, 2, 4, 4)); navigation.add(new JLabel("Pasta")); navigation.add(path);
        addButton(navigation, "Ir", () -> dialogs.observe(controller.navigate(new NativeDirectory(Path.of(path.getText())))));
        addButton(navigation, "Subir", () -> { Path parent = this.directory.getPath().getParent(); if (parent != null) dialogs.observe(controller.navigate(new NativeDirectory(parent))); });
        navigation.add(directories); addButton(navigation, "Entrar", () -> { NativeDirectory chosen = (NativeDirectory) directories.getSelectedItem(); if (chosen != null) dialogs.observe(controller.navigate(chosen)); });
        add(navigation, BorderLayout.NORTH); add(new JScrollPane(files), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(0, 1));
        JPanel criteria = new JPanel(new GridLayout(0, 4, 4, 4));
        criteria.add(new JLabel("Extensões")); criteria.add(extensions); criteria.add(new JLabel("Bytes mínimos")); criteria.add(min);
        criteria.add(new JLabel("Bytes máximos")); criteria.add(max); criteria.add(new JLabel("Modificação desde (UTC)")); criteria.add(from);
        criteria.add(new JLabel("Modificação até (UTC)")); criteria.add(to); from.setToolTipText("AAAA-MM-DDTHH:MM; vazio sem limite"); to.setToolTipText(from.getToolTipText());
        addButton(criteria, "Filtrar", this::filter); bottom.add(criteria);
        JPanel actions = new JPanel(new GridLayout(0, 3, 4, 4));
        addButton(actions, "Abrir", () -> withFile(f -> dialogs.observe(controller.open(f))));
        addButton(actions, "Copiar", () -> copy(false)); addButton(actions, "Recortar", () -> copy(true));
        addButton(actions, "Colar", () -> dialogs.observe(controller.paste(this.directory)));
        addButton(actions, "Mover para…", () -> withFile(f -> dialogs.observe(controller.chooseMove(f))));
        addButton(actions, "Renomear", () -> withFile(f -> dialogs.observe(controller.rename(f))));
        addButton(actions, "Apagar arquivo…", () -> withFile(f -> dialogs.observe(controller.delete(f)))); bottom.add(actions); add(bottom, BorderLayout.SOUTH);
        SwingInteraction.bind(this, "ctrl C", "copy", () -> copy(false)); SwingInteraction.bind(this, "ctrl X", "cut", () -> copy(true));
        SwingInteraction.bind(this, "ctrl V", "paste", () -> dialogs.observe(controller.paste(this.directory)));
        SwingInteraction.bind(this, "F2", "rename", () -> withFile(f -> dialogs.observe(controller.rename(f))));
        files.setTransferHandler(new FileTransferHandler(gate, null)); if (!GraphicsEnvironment.isHeadless()) files.setDragEnabled(true);
        files.setCellRenderer(new DefaultListCellRenderer() {
            /**
             * Prepara o rótulo visual sem acessar disco ou SQL.
             *
             * @param list lista
             * @param value NativeFile
             * @param index posição
             * @param selected seleção
             * @param focused foco
             * @return nome e etiquetas, ou região vazia se sem cadastro
             */
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof NativeFile file) {
                    LocalFile local = registered.get(file.getPath());
                    label.setText(file.getName() + "  " + (local == null ? "" : local.getTags().stream().map(Tag::getName).sorted().toList()));
                }
                return label;
            }
        });
        events.subscribe(this);
    }

    /**
     * Captura critérios de UI e encaminha uma única ação de filtro/Refresh.
     */
    private void filter() {
        try {
            dialogs.observe(controller.filter(new NativeFileFilter(LocalFileManager.parseExtensions(extensions.getText()), min.getText().isBlank() ? null : Long.valueOf(min.getText()),
                    max.getText().isBlank() ? null : Long.valueOf(max.getText()), from.getText().isBlank() ? null : LocalDateTime.parse(from.getText()), to.getText().isBlank() ? null : LocalDateTime.parse(to.getText()))));
        } catch (RuntimeException e) { dialogs.showFailure(e); }
    }
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
     */
    private static void addButton(JPanel panel, String text, Runnable action) { JButton button = new JButton(text); button.addActionListener(e -> action.run()); panel.add(button); }
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
        directories.removeAllItems(); subdirectories.forEach(directories::addItem);
        Path selected = files.getSelectedValue() == null ? null : files.getSelectedValue().getPath();
        DefaultListModel<NativeFile> model = (DefaultListModel<NativeFile>) files.getModel(); model.clear(); model.addAll(nativeFiles);
        if (selected != null) for (int i = 0; i < model.size(); i++) if (model.get(i).getPath().equals(selected)) files.setSelectedIndex(i);
        files.setToolTipText(nativeFiles.isEmpty() ? "Pasta sem arquivos que correspondam aos critérios" : null);
    }
    /**
     * Remove a inscrição ao descartar a janela.
     */
    public void dispose() { events.unsubscribe(this); }
    /**
     * Informa lista física para verificações da demonstração.
     *
     * @return lista física para verificações da demonstração
     */
    public JList<NativeFile> getFileList() { return files; }
}
