/*
 * Inventário — TagExplorerController.
 * Entradas da visão por Tags; toda entrada funcional passa pela admissão comum (ARQ-05).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - TagExplorerController.locate(NativeFile file): Confere disponibilidade e permite relocalizar sem abrir a aplicação associada.
 * - TagExplorerController.TagExplorerController(ExplorerContext context): Injeta os colaboradores necessários a TagExplorerController.
 * - TagExplorerController.search(LocalFileFilter filter): Atualiza metadados uma vez e aplica a consulta por Tags.
 * - TagExplorerController.refresh(): Admite a ação de Refresh global, sem limpeza de inicialização pelo controle compartilhado.
 * - TagExplorerController.associate(List<Path> selected, UUID tagId): Associa a seleção imutável recebida, inclusive Drop e chamadas automáticas.
 * - TagExplorerController.selectAndAssociate(UUID tagId): Abre seletor filtrado dentro da ação e associa somente os escolhidos.
 * - TagExplorerController.createTag(): Cria uma etiqueta e permite seleção múltipla inicial na mesma ação.
 * - TagExplorerController.editTag(UUID id): Admite a ação de edição confirmada da etiqueta pelo controle compartilhado.
 * - TagExplorerController.removeTag(UUID file, UUID tag): Admite a ação de reorganização do vínculo e eventual sentinela pelo controle compartilhado.
 * - TagExplorerController.clipboard(NativeFile file, boolean cut): Guarda COPY/CUT sem mover; a identidade guardada permite reler caminhos atualizados.
 * - TagExplorerController.filterTags(TagFilter filter): Admite a ação de consulta de Tags após Refresh global pelo controle compartilhado.
 * - TagExplorerController.countAvailable(UUID id): Admite a ação de contagem de arquivos disponíveis pelo controle compartilhado.
 * - TagExplorerController.paste(NativeDirectory directory): Admite a ação de colagem pelo clipboard compartilhado pelo controle compartilhado.
 * - TagExplorerController.deleteTag(UUID id): Admite a ação de exclusão de Tag na modalidade confirmada pelo controle compartilhado.
 * - TagExplorerController.open(NativeFile file): Admite a ação de abertura, com verificação de disponibilidade pelo controle compartilhado.
 * - TagExplorerController.rename(NativeFile file): Admite a ação de renomeação com confirmação de efeitos pelo controle compartilhado.
 * - TagExplorerController.deleteFile(NativeFile file): Admite a ação de exclusão física confirmada pelo controle compartilhado.
 *
 * Consulte: doc/arquitetura-e-padroes.md — ARQ-03/ARQ-05; doc/requisitos-e-regras.md — OP-09.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package controller;

import filter.*;
import model.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Entradas da visão por Tags; toda entrada funcional passa pela admissão comum (ARQ-05).
 */
public final class TagExplorerController {
    /**
     * Colaboradores e estado da sessão compartilhada.
     */
    private final ExplorerContext context;

    /**
     * Confere disponibilidade e permite relocalizar sem abrir a aplicação associada.
     *
     * @param file referência selecionada, possivelmente indisponível
     * @return referência disponível, ou futuro excepcional se cancelar/falhar
     */
    public CompletableFuture<NativeFile> locate(NativeFile file) {
        return context.execute(() -> context.manager.resolveAvailable(file, context.interaction));
    }
    /**
     * Injeta os colaboradores necessários a TagExplorerController.
     *
     * @param context colaboradores e controle compartilhados pelos exploradores
     */
    public TagExplorerController(ExplorerContext context) { this.context = context; }

    /**
     * Atualiza metadados uma vez e aplica a consulta por Tags.
     *
     * @param filter seleção de Tags e critérios físicos
     * @return arquivos resultantes; falhas e rejeição ficam no futuro
     */
    public CompletableFuture<List<LocalFile>> search(LocalFileFilter filter) {
        return context.execute(() -> { context.manager.refreshAll(); context.localFilter = filter; return context.files.find(filter); });
    }
    /**
     * Admite a ação de Refresh global, sem limpeza de inicialização pelo controle compartilhado.
     *
     * @return futuro da atualização global única, sem limpar sentinela
     */
    public CompletableFuture<Void> refresh() { return context.execute(() -> { context.manager.refreshAll(); return null; }); }

    /**
     * Associa a seleção imutável recebida, inclusive Drop e chamadas automáticas.
     *
     * @param selected arquivos efetivamente confirmados
     * @param tagId UUID escolhido, nunca somente o nome
     * @return cadastros após associação
     */
    public CompletableFuture<List<LocalFile>> associate(List<Path> selected, UUID tagId) {
        List<Path> snapshot = List.copyOf(selected);
        return context.execute(() -> context.manager.associate(snapshot, tagId, context.interaction));
    }

    /**
     * Abre seletor filtrado dentro da ação e associa somente os escolhidos.
     *
     * @param tagId etiqueta alvo
     * @return cadastros confirmados; vazio ao cancelar a seleção
     */
    public CompletableFuture<List<LocalFile>> selectAndAssociate(UUID tagId) {
        return context.execute(() -> {
            Tag tag = context.tags.findById(tagId).orElseThrow();
            return context.manager.associate(context.interaction.files(true, tag.getExtensions()), tagId, context.interaction);
        });
    }

    /**
     * Cria uma etiqueta e permite seleção múltipla inicial na mesma ação.
     *
     * @return etiqueta persistida; se associação posterior falhar, a Tag permanece
     */
    public CompletableFuture<Tag> createTag() {
        return context.execute(() -> {
            Tag tag = context.manager.createTag(context.interaction);
            try {
                if (context.interaction.choose("Etiqueta criada. Selecionar arquivos iniciais?", "Selecionar", "Manter vazia") == 0)
                    context.manager.associate(context.interaction.files(true, tag.getExtensions()), tag.getId(), context.interaction);
            } catch (Exception e) { throw new service.OperationFailure("Etiqueta criada: " + tag, "Associação inicial", e); }
            return tag;
        });
    }
    /**
     * Admite a ação de edição confirmada da etiqueta pelo controle compartilhado.
     *
     * @param id Tag a editar
     * @return fotografia atualizada após confirmações
     */
    public CompletableFuture<Tag> editTag(UUID id) { return context.execute(() -> context.manager.editTag(id, context.interaction)); }
    /**
     * Admite a ação de reorganização do vínculo e eventual sentinela pelo controle compartilhado.
     *
     * @param file cadastro selecionado
     * @param tag vínculo escolhido
     * @return conclusão da reorganização
     */
    public CompletableFuture<Void> removeTag(UUID file, UUID tag) { return context.execute(() -> { context.manager.removeTag(file, tag); return null; }); }

    /**
     * Guarda COPY/CUT sem mover; a identidade guardada permite reler caminhos atualizados.
     *
     * @param file referência selecionada
     * @param cut verdadeiro para recortar
     * @return conclusão da atualização do clipboard
     */
    public CompletableFuture<Void> clipboard(NativeFile file, boolean cut) {
        return context.execute(() -> { var known = context.files.findByPath(file.getPath()); context.clipboard.put(file, known.map(LocalFile::getId).orElse(null), cut); return null; });
    }
    /**
     * Admite a ação de consulta de Tags após Refresh global pelo controle compartilhado.
     *
     * @param filter critérios de Tags vazias/datas
     * @return Tags, sem trocar o tipo por arquivos
     */
    public CompletableFuture<List<Tag>> filterTags(TagFilter filter) {
        return context.execute(() -> { context.manager.refreshAll(); context.tagFilter = filter; return context.tags.find(filter); });
    }
    /**
     * Admite a ação de contagem de arquivos disponíveis pelo controle compartilhado.
     *
     * @param id etiqueta escolhida
     * @return contagem calculada no banco, sem contador persistido
     */
    public CompletableFuture<Long> countAvailable(UUID id) {
        return context.execute(() -> { long count = context.tags.countAvailable(id); context.interaction.choose("Arquivos disponíveis: " + count, "Fechar"); return count; });
    }
    /**
     * Admite a ação de colagem pelo clipboard compartilhado pelo controle compartilhado.
     *
     * @param directory pasta confirmada
     * @return resultado da colagem usando clipboard comum
     */
    public CompletableFuture<NativeFile> paste(NativeDirectory directory) { return context.execute(() -> context.manager.paste(context.clipboard, directory, context.interaction)); }
    /**
     * Admite a ação de exclusão de Tag na modalidade confirmada pelo controle compartilhado.
     *
     * @param id etiqueta selecionada
     * @return conclusão da exclusão na modalidade confirmada
     */
    public CompletableFuture<Void> deleteTag(UUID id) { return context.execute(() -> { context.manager.deleteTag(id, context.interaction); return null; }); }
    /**
     * Admite a ação de abertura, com verificação de disponibilidade pelo controle compartilhado.
     *
     * @param file arquivo selecionado
     * @return conclusão da abertura ou decisão sobre indisponibilidade
     */
    public CompletableFuture<Void> open(NativeFile file) { return context.execute(() -> { context.nativeService.open(context.manager.resolveAvailable(file, context.interaction)); return null; }); }
    /**
     * Admite a ação de renomeação com confirmação de efeitos pelo controle compartilhado.
     *
     * @param file arquivo selecionado
     * @return referência renomeada após confirmação
     */
    public CompletableFuture<NativeFile> rename(NativeFile file) {
        return context.execute(() -> { String name = context.interaction.text("Novo nome; mudar extensão não converte conteúdo", file.getName()); if (name == null) throw new java.util.concurrent.CancellationException(); return context.manager.rename(file, name, context.interaction); });
    }
    /**
     * Admite a ação de exclusão física confirmada pelo controle compartilhado.
     *
     * @param file arquivo escolhido
     * @return conclusão da exclusão física confirmada
     */
    public CompletableFuture<Void> deleteFile(NativeFile file) { return context.execute(() -> { context.manager.deleteFile(file, context.interaction); return null; }); }
}
