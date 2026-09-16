/*
 * Inventário — TagFilter.
 * Critérios de Tags: criação e ausência real de associações (TAG-03/EXP-02).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - TagFilter.TagFilter(): Constrói consulta de todas as Tags, inclusive sentinela.
 * - TagFilter.TagFilter(boolean emptyOnly, LocalDateTime from, LocalDateTime to): Configura consulta; zero disponíveis não significa Tag vazia.
 * - TagFilter.isEmptyOnly(): Informa se exige ausência de todas as associações.
 *
 * Consulte: doc/interface-e-fluxos.md — EXP-02 a EXP-04.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package filter;

import java.time.LocalDateTime;

/**
 * Critérios de Tags: criação e ausência real de associações (TAG-03/EXP-02).
 */
public final class TagFilter extends Filter {
    /**
     * Restringe a consulta a Tags sem associações.
     */
    private final boolean emptyOnly;
    /**
     * Constrói consulta de todas as Tags, inclusive sentinela.
     */
    public TagFilter() { this(false, null, null); }
    /**
     * Configura consulta; zero disponíveis não significa Tag vazia.
     *
     * @param emptyOnly restringe a Tags sem nenhum vínculo
     * @param from criação mínima UTC, ou nulo
     * @param to criação máxima UTC, ou nulo
     * @throws IllegalArgumentException se intervalo invertido
     */
    public TagFilter(boolean emptyOnly, LocalDateTime from, LocalDateTime to) { super(from, to); this.emptyOnly = emptyOnly; }
    /**
     * Informa se exige ausência de todas as associações.
     *
     * @return se exige ausência de todas as associações
     */
    public boolean isEmptyOnly() { return emptyOnly; }
}
