/*
 * Inventário — Filter.
 * Base de intervalos inclusivos; não representa seleção confirmada (EXP-02).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - Filter.Filter(LocalDateTime from, LocalDateTime to): Configura limites opcionais.
 * - Filter.getFrom(): Informa início inclusivo, ou nulo.
 * - Filter.getTo(): Informa fim inclusivo, ou nulo.
 * - Filter.acceptsDate(LocalDateTime value): Compara uma data; dado desconhecido só passa quando não há limite.
 *
 * Consulte: doc/interface-e-fluxos.md — EXP-02 a EXP-04.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package filter;

import java.time.LocalDateTime;

/**
 * Base de intervalos inclusivos; não representa seleção confirmada (EXP-02).
 */
public abstract class Filter {
    /**
     * Limite inicial inclusivo de data.
     */
    private final LocalDateTime from;
    /**
     * Limite final inclusivo de data.
     */
    private final LocalDateTime to;

    /**
     * Configura limites opcionais.
     *
     * @param from início inclusivo em UTC, ou nulo
     * @param to fim inclusivo em UTC, ou nulo
     * @throws IllegalArgumentException se início for posterior ao fim
     */
    protected Filter(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("Intervalo de datas invertido");
        this.from = from; this.to = to;
    }
    /**
     * Informa início inclusivo, ou nulo.
     *
     * @return início inclusivo, ou nulo
     */
    public LocalDateTime getFrom() { return from; }
    /**
     * Informa fim inclusivo, ou nulo.
     *
     * @return fim inclusivo, ou nulo
     */
    public LocalDateTime getTo() { return to; }
    /**
     * Compara uma data; dado desconhecido só passa quando não há limite.
     *
     * @param value data conhecida ou nulo
     * @return se o intervalo é satisfeito
     */
    public boolean acceptsDate(LocalDateTime value) {
        return from == null && to == null || value != null && (from == null || !value.isBefore(from)) && (to == null || !value.isAfter(to));
    }
}
