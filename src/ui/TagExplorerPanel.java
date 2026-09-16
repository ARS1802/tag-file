/*
 * Inventário — TagExplorerPanel.
 * Screen e Panel em um único objeto: mostra Tags, pesquisa e encaminha ações (P-04).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - TagExplorerPanel.TagExplorerPanel(TagExplorerController controller, ExplorerEventService events, ActionGate gate, NativeDirectory directory): Monta e inscreve a apresentação na EDT, sem consultar SQL no construtor.
 * - DefaultListCellRenderer (anônima).getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused): Prepara o rótulo visual sem acessar disco ou SQL.
 * - TagExplorerPanel.search(): Encaminha Tags selecionadas; nenhum filtro é reaplicado para mudar alvos de operações.
 * - TagExplorerPanel.copy(boolean cut): Encaminha a intenção de copiar ou recortar o arquivo selecionado.
 * - TagExplorerPanel.withTag(Consumer<Tag> action): Encaminha a ação somente quando existe Tag selecionada.
 * - TagExplorerPanel.withFile(Consumer<LocalFile> action): Encaminha a ação somente quando existe arquivo selecionado.
 * - TagExplorerPanel.addButton(JPanel panel, String text, Runnable action): Liga um botão à entrada correspondente do Controller.
 * - TagExplorerPanel.onTagsChanged(List<Tag> values): Aplica a fotografia de Tags sem iniciar outra consulta ou Refresh.
 * - TagExplorerPanel.onFilesChanged(List<LocalFile> values): Atualiza a apresentação usando o aviso recebido, sem repetir Refresh.
 * - TagExplorerPanel.onDirectoryChanged(NativeDirectory directory, List<NativeDirectory> directories, List<NativeFile> files, Map<Path, LocalFile> registered): Atualiza a pasta de destino recebida da outra apresentação.
 * - TagExplorerPanel.dispose(): Remove inscrição quando a janela descarta o painel.
 * - TagExplorerPanel.getTagList(): Informa lista de etiquetas, para simular Drop e conferir a UI na demonstração.
 * - TagExplorerPanel.getFileList(): Informa lista de resultados, para conferir atualizações na demonstração.
 *
 * Consulte: doc/interface-e-fluxos.md — UI-01 a UI-03; doc/arquitetura-e-padroes.md — ARQ-06.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package ui;

import controller.TagExplorerController;
import filter.*;
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
 * Screen e Panel em um único objeto: mostra Tags, pesquisa e encaminha ações (P-04).
 */
public final class TagExplorerPanel extends JPanel implements ExplorerListener {
    /**
     * Versão de serialização exigida pelo tipo herdado; não implementa persistência da aplicação.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Coordenador que recebe solicitações desta apresentação.
     */
    private final TagExplorerController controller;
    /**
     * Serviço compartilhado de inscrições e avisos.
     */
    private final ExplorerEventService events;
    /**
     * Apresentação de diálogos e rejeições na EDT.
     */
    private final SwingInteraction dialogs;
    /**
     * Lista visual de Tags identificadas pelo UUID.
     */
    private final JList<Tag> tags = new JList<>(new DefaultListModel<>());
    /**
     * Lista visual de cadastros resultantes da pesquisa.
     */
    private final JList<LocalFile> files = new JList<>(new DefaultListModel<>());
    /**
     * Seleção visual entre AND e OR.
     */
    private final JComboBox<String> mode = new JComboBox<>(new String[]{"AND — todas", "OR — alguma"});
    /**
     * Pasta atual usada para navegação e colagem.
     */
    private NativeDirectory directory;

    /**
     * Monta e inscreve a apresentação na EDT, sem consultar SQL no construtor.
     *
     * @param controller coordenador da visão por Tags
     * @param events serviço de Observer compartilhado
     * @param gate controle global usado por Drop e atalhos
     * @param directory pasta inicial para colagem
     * @throws IllegalStateException se chamado fora da EDT
     */
    public TagExplorerPanel(TagExplorerController controller, ExplorerEventService events, ActionGate gate, NativeDirectory directory) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Crie o painel na EDT");
        this.controller = controller; this.events = events; this.directory = directory; this.dialogs = new SwingInteraction(this);
        setLayout(new BorderLayout(6, 6));
        tags.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        files.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel tagActions = new JPanel(new GridLayout(0, 3, 4, 4));
        addButton(tagActions, "Nova etiqueta", () -> dialogs.observe(controller.createTag()));
        addButton(tagActions, "Editar", () -> withTag(t -> dialogs.observe(controller.editTag(t.getId()))));
        addButton(tagActions, "Excluir etiqueta…", () -> withTag(t -> dialogs.observe(controller.deleteTag(t.getId()))));
        addButton(tagActions, "Associar arquivos…", () -> withTag(t -> dialogs.observe(controller.selectAndAssociate(t.getId()))));
        addButton(tagActions, "Disponíveis", () -> withTag(t -> dialogs.observe(controller.countAvailable(t.getId()))));
        JCheckBox empty = new JCheckBox("Somente etiquetas vazias"); empty.addActionListener(e -> dialogs.observe(controller.filterTags(new TagFilter(empty.isSelected(), null, null)))); tagActions.add(empty);
        add(tagActions, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tags), new JScrollPane(files)); split.setResizeWeight(.32); add(split, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(0, 3, 4, 4)); actions.add(mode);
        addButton(actions, "Pesquisar", this::search);
        addButton(actions, "Abrir", () -> withFile(f -> dialogs.observe(controller.open(f.getNativeFile()))));
        addButton(actions, "Localizar", () -> withFile(f -> dialogs.observe(controller.locate(f.getNativeFile()))));
        addButton(actions, "Copiar", () -> copy(false)); addButton(actions, "Recortar", () -> copy(true));
        addButton(actions, "Colar na pasta local", () -> dialogs.observe(controller.paste(directory)));
        addButton(actions, "Renomear", () -> withFile(f -> dialogs.observe(controller.rename(f.getNativeFile()))));
        addButton(actions, "Apagar arquivo…", () -> withFile(f -> dialogs.observe(controller.deleteFile(f.getNativeFile()))));
        addButton(actions, "Retirar etiqueta", () -> withFile(f -> withTag(t -> dialogs.observe(controller.removeTag(f.getId(), t.getId())))));
        add(actions, BorderLayout.SOUTH);
        SwingInteraction.bind(this, "ctrl C", "copy", () -> copy(false)); SwingInteraction.bind(this, "ctrl X", "cut", () -> copy(true));
        SwingInteraction.bind(this, "ctrl V", "paste", () -> dialogs.observe(controller.paste(directory)));
        SwingInteraction.bind(this, "F2", "rename", () -> withFile(f -> dialogs.observe(controller.rename(f.getNativeFile()))));
        tags.setTransferHandler(new FileTransferHandler(gate, (paths, id) -> dialogs.observe(controller.associate(paths, id)))); tags.setDropMode(DropMode.ON);
        files.setTransferHandler(new FileTransferHandler(gate, null)); if (!GraphicsEnvironment.isHeadless()) files.setDragEnabled(true);
        tags.setCellRenderer(new DefaultListCellRenderer() {
            /**
             * Prepara o rótulo visual sem acessar disco ou SQL.
             *
             * @param list lista
             * @param value Tag
             * @param index posição
             * @param selected seleção
             * @param focused foco
             * @return rótulo com cor, nome e UUID
             */
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof Tag tag && !selected) label.setForeground(Color.decode(tag.getColor())); return label;
            }
        });
        events.subscribe(this);
    }

    /**
     * Encaminha Tags selecionadas; nenhum filtro é reaplicado para mudar alvos de operações.
     */
    private void search() {
        dialogs.observe(controller.search(new LocalFileFilter(tags.getSelectedValuesList().stream().map(Tag::getId).toList(), mode.getSelectedIndex() == 0, new NativeFileFilter())));
    }
    /**
     * Encaminha a intenção de copiar ou recortar o arquivo selecionado.
     *
     * @param cut indica CUT em vez de COPY; seleção é capturada antes de encaminhar
     */
    private void copy(boolean cut) { withFile(f -> dialogs.observe(controller.clipboard(f.getNativeFile(), cut))); }
    /**
     * Encaminha a ação somente quando existe Tag selecionada.
     *
     * @param action ação que recebe a Tag selecionada, quando houver
     */
    private void withTag(Consumer<Tag> action) { Tag tag = tags.getSelectedValue(); if (tag != null) action.accept(tag); }
    /**
     * Encaminha a ação somente quando existe arquivo selecionado.
     *
     * @param action ação que recebe o cadastro selecionado, quando houver
     */
    private void withFile(Consumer<LocalFile> action) { LocalFile file = files.getSelectedValue(); if (file != null) action.accept(file); }
    /**
     * Liga um botão à entrada correspondente do Controller.
     *
     * @param panel barra de ações
     * @param text rótulo
     * @param action entrada de Controller correspondente
     */
    private static void addButton(JPanel panel, String text, Runnable action) { JButton button = new JButton(text); button.addActionListener(e -> action.run()); panel.add(button); }

    /**
     * Aplica a fotografia de Tags sem iniciar outra consulta ou Refresh.
     *
     * @param values Tags consultadas; preserva a seleção pelos UUIDs, sem consultar novamente
     */
    @Override public void onTagsChanged(List<Tag> values) {
        Set<UUID> selected = new HashSet<>(tags.getSelectedValuesList().stream().map(Tag::getId).toList());
        DefaultListModel<Tag> model = (DefaultListModel<Tag>) tags.getModel(); model.clear(); model.addAll(values);
        for (int i = 0; i < model.size(); i++) if (selected.contains(model.get(i).getId())) tags.addSelectionInterval(i, i);
        tags.setToolTipText(values.isEmpty() ? "Nenhuma etiqueta corresponde ao filtro" : "Solte arquivos sobre uma etiqueta para associar");
    }
    /**
     * Atualiza a apresentação usando o aviso recebido, sem repetir Refresh.
     *
     * @param values arquivos consultados; nenhum callback dispara Refresh
     */
    @Override public void onFilesChanged(List<LocalFile> values) {
        LocalFile selected = files.getSelectedValue(); DefaultListModel<LocalFile> model = (DefaultListModel<LocalFile>) files.getModel(); model.clear(); model.addAll(values);
        if (selected != null) files.setSelectedValue(selected, true); files.setToolTipText(values.isEmpty() ? "Nenhum arquivo corresponde à pesquisa" : null);
    }
    /**
     * Atualiza a pasta de destino recebida da outra apresentação.
     *
     * @param directory pasta local
     * @param directories subpastas
     * @param files nativos
     * @param registered mapa; esta visão usa somente a pasta como destino
     */
    @Override public void onDirectoryChanged(NativeDirectory directory, List<NativeDirectory> directories, List<NativeFile> files, Map<Path, LocalFile> registered) { this.directory = directory; }
    /**
     * Remove inscrição quando a janela descarta o painel.
     */
    public void dispose() { events.unsubscribe(this); }
    /**
     * Informa lista de etiquetas, para simular Drop e conferir a UI na demonstração.
     *
     * @return lista de etiquetas, para simular Drop e conferir a UI na demonstração
     */
    public JList<Tag> getTagList() { return tags; }
    /**
     * Informa lista de resultados, para conferir atualizações na demonstração.
     *
     * @return lista de resultados, para conferir atualizações na demonstração
     */
    public JList<LocalFile> getFileList() { return files; }
}
