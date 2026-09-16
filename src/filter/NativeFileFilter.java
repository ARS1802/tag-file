/*
 * Inventário — NativeFileFilter.
 * Critérios físicos de extensão, bytes e modificação, com limites inclusivos (EXP-03).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - NativeFileFilter.NativeFileFilter(): Constrói listagem sem restrições.
 * - NativeFileFilter.NativeFileFilter(Collection<String> extensions, Long minSize, Long maxSize, LocalDateTime from, LocalDateTime to): Configura critérios físicos combinados por AND.
 * - NativeFileFilter.matches(LocalFile file): Avalia os critérios sobre metadados lidos pelo serviço.
 *
 * Consulte: doc/interface-e-fluxos.md — EXP-02 a EXP-04.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package filter;

import model.LocalFile;
import model.Tag;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;

/**
 * Critérios físicos de extensão, bytes e modificação, com limites inclusivos (EXP-03).
 */
public final class NativeFileFilter extends Filter {
    /**
     * Extensões aceitas; conjunto vazio significa ausência de restrição.
     */
    private final Set<String> extensions;
    /**
     * Limite inferior de bytes, ou nulo.
     */
    private final Long minSize;
    /**
     * Limite superior de bytes, ou nulo.
     */
    private final Long maxSize;

    /**
     * Constrói listagem sem restrições.
     */
    public NativeFileFilter() { this(Set.of(), null, null, null, null); }
    /**
     * Configura critérios físicos combinados por AND.
     *
     * @param extensions extensões aceitas; vazio libera todas
     * @param minSize mínimo em bytes, ou nulo
     * @param maxSize máximo em bytes, ou nulo
     * @param from modificação mínima UTC, ou nulo
     * @param to modificação máxima UTC, ou nulo
     * @throws IllegalArgumentException se limites forem negativos/invertidos ou extensões inválidas
     */
    public NativeFileFilter(Collection<String> extensions, Long minSize, Long maxSize, LocalDateTime from, LocalDateTime to) {
        super(from, to);
        if (minSize != null && minSize < 0 || maxSize != null && maxSize < 0 || minSize != null && maxSize != null && minSize > maxSize)
            throw new IllegalArgumentException("Intervalo de tamanho inválido");
        this.extensions = Tag.normalizeExtensions(extensions); this.minSize = minSize; this.maxSize = maxSize;
    }
    /**
     * Avalia os critérios sobre metadados lidos pelo serviço.
     *
     * @param file fotografia de metadados não nula
     * @return se satisfaz todos os critérios; não exige cadastro persistido
     */
    public boolean matches(LocalFile file) {
        String name = file.getNativeFile().getName().toLowerCase(java.util.Locale.ROOT);
        boolean ext = extensions.isEmpty() || extensions.stream().anyMatch(e -> e.isEmpty() ? file.getNativeFile().getExtension().isEmpty() : name.length() > e.length() && name.endsWith(e));
        Long size = file.getSize();
        return ext && (minSize == null || size != null && size >= minSize) && (maxSize == null || size != null && size <= maxSize) && acceptsDate(file.getModifiedAt());
    }
}
