/*
 * Inventário — FileTransferHandler.
 * Exporta arquivos para Drag e recebe Drop sobre uma Tag identificada (UI-02/ACE-25).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - FileTransferHandler.FileTransferHandler(ActionGate gate, BiConsumer<List<Path>, UUID> association): Configura exportação de arquivos ou recepção sobre Tag.
 * - FileTransferHandler.getSourceActions(JComponent component): Indica transferência de referências, sem movimentação física durante Drag.
 * - FileTransferHandler.createTransferable(JComponent component): Exporta referências selecionadas no formato de lista de arquivos do sistema.
 * - FileTransferHandler.transfer(List<Path> paths): Cria dados de transferência sem mover/classificar arquivos.
 * - Transferable (anônima).getTransferDataFlavors(): Informa o formato de transferência de lista de arquivos.
 * - Transferable (anônima).isDataFlavorSupported(DataFlavor flavor): Verifica se o formato solicitado é lista de arquivos.
 * - Transferable (anônima).getTransferData(DataFlavor flavor): Entrega a lista imutável de arquivos no formato suportado.
 * - FileTransferHandler.canImport(TransferSupport support): Verifica formato, Tag alvo e estado livre antes de aceitar Drop.
 * - FileTransferHandler.importData(TransferSupport support): Usa a Tag sob o Drop; Controller revalida a admissão mesmo após canImport.
 *
 * Consulte: doc/interface-e-fluxos.md — UI-01 a UI-03; doc/arquitetura-e-padroes.md — ARQ-06.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package GUI;

import model.*;
import service.ActionGate;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.*;

/**
 * Exporta arquivos para Drag e recebe Drop sobre uma Tag identificada (UI-02/ACE-25).
 */
public final class FileTransferHandler extends TransferHandler {
    /**
     * Versão de serialização exigida pelo tipo herdado; não implementa persistência da aplicação.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Controle global de admissão sem fila.
     */
    private final ActionGate gate;
    /**
     * Encaminhamento do Drop ao Controller, ou nulo no exportador.
     */
    private final BiConsumer<List<Path>, UUID> association;

    /**
     * Configura exportação de arquivos ou recepção sobre Tag.
     *
     * @param gate controle compartilhado, inclusive para Drop durante uma ação
     * @param association associação encaminhada ao Controller, ou nulo para exportar somente
     */
    public FileTransferHandler(ActionGate gate, BiConsumer<List<Path>, UUID> association) { this.gate = gate; this.association = association; }
    /**
     * Indica transferência de referências, sem movimentação física durante Drag.
     *
     * @param component lista de arquivos
     * @return COPY para transferência de referências, sem mover durante Drag
     */
    @Override public int getSourceActions(JComponent component) { return COPY; }

    /**
     * Exporta referências selecionadas no formato de lista de arquivos do sistema.
     *
     * @param component JList com NativeFile ou LocalFile
     * @return dados da seleção; nulo se ocupado ou sem arquivos
     */
    @Override protected Transferable createTransferable(JComponent component) {
        if (gate.isBusy() || !(component instanceof JList<?> list)) return null;
        List<File> paths = new ArrayList<>();
        for (Object item : list.getSelectedValuesList()) {
            if (item instanceof NativeFile nativeFile) paths.add(nativeFile.getPath().toFile());
            if (item instanceof LocalFile localFile) paths.add(localFile.getNativeFile().getPath().toFile());
        }
        return paths.isEmpty() ? null : transfer(paths.stream().map(File::toPath).toList());
    }

    /**
     * Cria dados de transferência sem mover/classificar arquivos.
     *
     * @param paths arquivos selecionados
     * @return objeto compatível com javaFileListFlavor
     */
    public static Transferable transfer(List<Path> paths) {
        List<File> files = paths.stream().map(Path::toFile).toList();
        return new Transferable() {
            /**
             * Informa o formato de transferência de lista de arquivos.
             *
             * @return único formato suportado
             */
            @Override public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.javaFileListFlavor}; }
            /**
             * Verifica se o formato solicitado é lista de arquivos.
             *
             * @param flavor formato solicitado
             * @return se é lista de arquivos
             */
            @Override public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.javaFileListFlavor.equals(flavor); }
            /**
             * Entrega a lista imutável de arquivos no formato suportado.
             *
             * @param flavor formato solicitado
             * @return arquivos imutáveis
             * @throws UnsupportedFlavorException se formato diferente
             */
            @Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor); return files;
            }
        };
    }

    /**
     * Verifica formato, Tag alvo e estado livre antes de aceitar Drop.
     *
     * @param support contexto de Drop
     * @return se formato/alvo permitem associação e aplicação está livre
     */
    @Override public boolean canImport(TransferSupport support) {
        if (association == null || gate.isBusy() || !(support.getComponent() instanceof JList<?> list) || !support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false;
        int index = support.isDrop() ? ((JList.DropLocation) support.getDropLocation()).getIndex() : list.getSelectedIndex();
        return index >= 0 && index < list.getModel().getSize() && list.getModel().getElementAt(index) instanceof Tag;
    }

    /**
     * Usa a Tag sob o Drop; Controller revalida a admissão mesmo após canImport.
     *
     * @param support arquivos soltos sobre uma etiqueta
     * @return se associação foi encaminhada; falso se formato/alvo/ocupação impediram
     */
    @Override public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;
        JList<?> list = (JList<?>) support.getComponent();
        int index = support.isDrop() ? ((JList.DropLocation) support.getDropLocation()).getIndex() : list.getSelectedIndex();
        Tag tag = (Tag) list.getModel().getElementAt(index);
        try {
            Object data = support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (!(data instanceof List<?> entries) || entries.stream().anyMatch(e -> !(e instanceof File))) return false;
            association.accept(entries.stream().map(e -> ((File) e).toPath()).toList(), tag.getId()); return true;
        } catch (UnsupportedFlavorException | IOException e) { new SwingInteraction(list).showFailure(e); return false; }
    }
}
