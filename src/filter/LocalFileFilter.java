/*
 * Inventário — LocalFileFilter.
 * Pesquisa cadastros por Tags AND/OR e critérios físicos (P-05).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - LocalFileFilter.LocalFileFilter(): Constrói consulta de todos os cadastros.
 * - LocalFileFilter.LocalFileFilter(Collection<UUID> tagIds, boolean matchAll, NativeFileFilter physical): Configura pesquisa sem NOT e sem duplicar identidade.
 * - LocalFileFilter.getTagIds(): Informa identidades imutáveis.
 * - LocalFileFilter.isMatchAll(): Informa verdadeiro para exigir todas as Tags.
 * - LocalFileFilter.matchesPhysical(LocalFile file): Informa se atende extensão/tamanho/datas; SQL aplica Tags.
 *
 * Consulte: doc/interface-e-fluxos.md — EXP-02 a EXP-04.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package filter;

import model.LocalFile;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Pesquisa cadastros por Tags AND/OR e critérios físicos (P-05).
 */
public final class LocalFileFilter extends Filter {
    /**
     * UUIDs selecionados para AND/OR.
     */
    private final Set<UUID> tagIds;
    /**
     * Exige todas as Tags quando verdadeiro.
     */
    private final boolean matchAll;
    /**
     * Critérios físicos aplicados aos cadastros consultados.
     */
    private final NativeFileFilter physical;

    /**
     * Constrói consulta de todos os cadastros.
     */
    public LocalFileFilter() { this(Set.of(), true, new NativeFileFilter()); }
    /**
     * Configura pesquisa sem NOT e sem duplicar identidade.
     *
     * @param tagIds identidades selecionadas, copiadas; vazio não restringe
     * @param matchAll verdadeiro para AND, falso para OR
     * @param physical critérios físicos não nulos
     */
    public LocalFileFilter(Collection<UUID> tagIds, boolean matchAll, NativeFileFilter physical) {
        super(physical.getFrom(), physical.getTo()); this.tagIds = Set.copyOf(tagIds); this.matchAll = matchAll; this.physical = physical;
    }
    /**
     * Informa identidades imutáveis.
     *
     * @return identidades imutáveis
     */
    public Set<UUID> getTagIds() { return tagIds; }
    /**
     * Informa verdadeiro para exigir todas as Tags.
     *
     * @return verdadeiro para exigir todas as Tags
     */
    public boolean isMatchAll() { return matchAll; }
    /**
     * Informa se atende extensão/tamanho/datas; SQL aplica Tags.
     *
     * @param file cadastro reconstruído
     * @return se atende extensão/tamanho/datas; SQL aplica Tags
     */
    public boolean matchesPhysical(LocalFile file) { return physical.matches(file); }
}
