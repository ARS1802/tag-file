/*
 * Inventário — LocalFileExplorerController.
 * Coordena a visão física sem cadastrar arquivos apenas porque foram listados (EXP-03).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - LocalFileExplorerController.LocalFileExplorerController(ExplorerContext context): Injeta os colaboradores necessários a LocalFileExplorerController.
 * - LocalFileExplorerController.navigate(NativeDirectory directory): Navega sem Refresh global; publicação acrescenta o mapa consultado em lote.
 * - LocalFileExplorerController.filter(NativeFileFilter filter): Admite a ação de filtragem física após Refresh global pelo controle compartilhado.
 * - LocalFileExplorerController.refresh(): Admite a ação de Refresh global, sem limpeza de inicialização pelo controle compartilhado.
 * - LocalFileExplorerController.clipboard(NativeFile file, boolean cut): Guarda a intenção no mesmo clipboard usado pela outra visão.
 * - LocalFileExplorerController.open(NativeFile file): Admite a ação de abertura, com verificação de disponibilidade pelo controle compartilhado.
 * - LocalFileExplorerController.paste(NativeDirectory directory): Admite a ação de colagem pelo clipboard compartilhado pelo controle compartilhado.
 * - LocalFileExplorerController.move(NativeFile file, NativeDirectory destination): Admite a ação de movimentação para a pasta confirmada pelo controle compartilhado.
 * - LocalFileExplorerController.chooseMove(NativeFile file): Admite a ação de escolha de destino e movimentação pelo controle compartilhado.
 * - LocalFileExplorerController.rename(NativeFile file): Admite a ação de renomeação com confirmação de efeitos pelo controle compartilhado.
 * - LocalFileExplorerController.delete(NativeFile file): Admite a ação de exclusão física confirmada pelo controle compartilhado.
 *
 * Consulte: doc/arquitetura-e-padroes.md — ARQ-03/ARQ-05; doc/requisitos-e-regras.md — OP-09.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package controller;

import filter.NativeFileFilter;
import model.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Coordena a visão física sem cadastrar arquivos apenas porque foram listados (EXP-03).
 */
public final class LocalFileExplorerController {
    /**
     * Colaboradores e estado da sessão compartilhada.
     */
    private final ExplorerContext context;
    /**
     * Injeta os colaboradores necessários a LocalFileExplorerController.
     *
     * @param context a mesma sessão usada pelo Controller por Tags
     */
    public LocalFileExplorerController(ExplorerContext context) { this.context = context; }

    /**
     * Navega sem Refresh global; publicação acrescenta o mapa consultado em lote.
     *
     * @param directory pasta selecionada
     * @return arquivos físicos que satisfazem o filtro atual
     */
    public CompletableFuture<List<NativeFile>> navigate(NativeDirectory directory) {
        return context.execute(() -> { List<NativeFile> listed = context.nativeService.listFiles(directory, context.nativeFilter); context.directory = directory; return listed; });
    }
    /**
     * Admite a ação de filtragem física após Refresh global pelo controle compartilhado.
     *
     * @param filter critérios físicos
     * @return arquivos filtrados após uma sincronização global
     */
    public CompletableFuture<List<NativeFile>> filter(NativeFileFilter filter) {
        return context.execute(() -> { context.manager.refreshAll(); context.nativeFilter = filter; return context.nativeService.listFiles(context.directory, filter); });
    }
    /**
     * Admite a ação de Refresh global, sem limpeza de inicialização pelo controle compartilhado.
     *
     * @return conclusão de um Refresh global sem limpeza
     */
    public CompletableFuture<Void> refresh() { return context.execute(() -> { context.manager.refreshAll(); return null; }); }

    /**
     * Guarda a intenção no mesmo clipboard usado pela outra visão.
     *
     * @param file referência física selecionada
     * @param cut verdadeiro para recortar
     * @return conclusão sem operações físicas
     */
    public CompletableFuture<Void> clipboard(NativeFile file, boolean cut) {
        return context.execute(() -> { var known = context.files.findByPath(file.getPath()); context.clipboard.put(file, known.map(LocalFile::getId).orElse(null), cut); return null; });
    }

    /**
     * Admite a ação de abertura, com verificação de disponibilidade pelo controle compartilhado.
     *
     * @param file arquivo selecionado
     * @return conclusão da abertura ou decisão sobre indisponibilidade
     */
    public CompletableFuture<Void> open(NativeFile file) {
        return context.execute(() -> { context.nativeService.open(context.manager.resolveAvailable(file, context.interaction)); return null; });
    }

    /**
     * Admite a ação de colagem pelo clipboard compartilhado pelo controle compartilhado.
     *
     * @param directory pasta de destino
     * @return arquivo movido/copiado após as confirmações
     */
    public CompletableFuture<NativeFile> paste(NativeDirectory directory) { return context.execute(() -> context.manager.paste(context.clipboard, directory, context.interaction)); }
    /**
     * Admite a ação de movimentação para a pasta confirmada pelo controle compartilhado.
     *
     * @param file arquivo selecionado
     * @param destination pasta confirmada
     * @return referência movida, preservando cadastro quando houver
     */
    public CompletableFuture<NativeFile> move(NativeFile file, NativeDirectory destination) { return context.execute(() -> context.manager.transfer(file, destination.getPath().resolve(file.getName()), false, context.interaction)); }
    /**
     * Admite a ação de escolha de destino e movimentação pelo controle compartilhado.
     *
     * @param file arquivo selecionado
     * @return resultado após escolher destino dentro da ação admitida
     */
    public CompletableFuture<NativeFile> chooseMove(NativeFile file) {
        return context.execute(() -> {
            String path = context.interaction.text("Pasta de destino", context.directory.toString());
            if (path == null) throw new java.util.concurrent.CancellationException();
            return context.manager.transfer(file, java.nio.file.Path.of(path).resolve(file.getName()), false, context.interaction);
        });
    }
    /**
     * Admite a ação de renomeação com confirmação de efeitos pelo controle compartilhado.
     *
     * @param file arquivo selecionado
     * @return referência renomeada ou cancelamento no futuro
     */
    public CompletableFuture<NativeFile> rename(NativeFile file) {
        return context.execute(() -> { String name = context.interaction.text("Novo nome; mudar extensão não converte conteúdo", file.getName()); if (name == null) throw new java.util.concurrent.CancellationException(); return context.manager.rename(file, name, context.interaction); });
    }
    /**
     * Admite a ação de exclusão física confirmada pelo controle compartilhado.
     *
     * @param file arquivo selecionado
     * @return conclusão da exclusão permanente após confirmação
     */
    public CompletableFuture<Void> delete(NativeFile file) { return context.execute(() -> { context.manager.deleteFile(file, context.interaction); return null; }); }
}
