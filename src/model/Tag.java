/*
 * Inventário — Tag.
 * Etiqueta imutável identificada pelo UUID; reconstrução preserva datas (DOM-04).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - Tag.Tag(UUID id, String name, String color, Collection<String> extensions, LocalDateTime createdAt, LocalDateTime lastFileTaggedAt, boolean predefined): Reconstrói uma etiqueta com identidade e datas fornecidas, sem gerar novos valores.
 * - Tag.normalizeExtensions(Collection<String> values): Remove duplicações e padroniza extensões simples ou compostas.
 * - Tag.accepts(NativeFile file): Verifica compatibilidade sem ler disco; extensões compostas usam o sufixo completo.
 * - Tag.getId(): Informa identidade estável da Tag.
 * - Tag.getName(): Informa nome exibido; não é chave de identidade.
 * - Tag.getColor(): Informa cor hexadecimal.
 * - Tag.getExtensions(): Informa conjunto imutável de extensões normalizadas.
 * - Tag.getCreatedAt(): Informa criação da etiqueta, distinta das datas físicas.
 * - Tag.getLastFileTaggedAt(): Informa última associação nova, ou nulo.
 * - Tag.isPredefined(): Informa se exige confirmação reforçada para exclusão.
 * - Tag.isMissing(): Informa se o UUID identifica a sentinela protegida.
 * - Tag.equals(Object other): Informa igualdade pelo UUID.
 * - Tag.hashCode(): Informa código coerente com a identidade.
 * - Tag.toString(): Informa nome e UUID para distinguir nomes repetidos.
 *
 * Consulte: doc/modelo-de-dominio.md — DOM-01 a DOM-04; doc/arquitetura-e-padroes.md — ARQ-02.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Etiqueta imutável identificada pelo UUID; reconstrução preserva datas (DOM-04).
 */
public final class Tag {
    /**
     * Identidade reservada à sentinela; nome igual não concede privilégios.
     */
    public static final UUID MISSING_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    /**
     * Identidade estável em UUID.
     */
    private final UUID id;
    /**
     * Nome apresentado ao usuário, sem ser chave de identidade.
     */
    private final String name;
    /**
     * Cor escolhida em hexadecimal #RRGGBB.
     */
    private final String color;
    /**
     * Extensões aceitas; conjunto vazio significa ausência de restrição.
     */
    private final Set<String> extensions;
    /**
     * Instante UTC de criação da etiqueta, preservado na reconstrução.
     */
    private final LocalDateTime createdAt;
    /**
     * Instante UTC da última associação nova, ou nulo.
     */
    private final LocalDateTime lastFileTaggedAt;
    /**
     * Marca de etiqueta predefinida que exige confirmação reforçada.
     */
    private final boolean predefined;

    /**
     * Reconstrói uma etiqueta com identidade e datas fornecidas, sem gerar novos valores.
     *
     * @param id identidade não nula
     * @param name nome não nulo; após aparar espaços externos, deve ter de 1 a 100 caracteres
     * @param color cor não nula no formato {@code #RRGGBB}, normalizada em maiúsculas
     * @param extensions restrições sem elementos nulos; conjunto vazio aceita qualquer arquivo
     * @param createdAt criação da Tag, não nula
     * @param lastFileTaggedAt última associação nova, ou nulo se nunca associada
     * @param predefined indica uma etiqueta preparada pelo sistema
     * @throws NullPointerException se um argumento obrigatório for nulo
     * @throws IllegalArgumentException se nome, cor ou extensão tiver formato inválido
     */
    public Tag(UUID id, String name, String color, Collection<String> extensions,
               LocalDateTime createdAt, LocalDateTime lastFileTaggedAt, boolean predefined) {
        this.id = Objects.requireNonNull(id);
        String cleanName = Objects.requireNonNull(name).strip();
        String cleanColor = Objects.requireNonNull(color).toUpperCase(Locale.ROOT);
        if (cleanName.isEmpty() || cleanName.length() > 100 || !cleanColor.matches("#[0-9A-F]{6}"))
            throw new IllegalArgumentException("Use nome de 1 a 100 caracteres e cor #RRGGBB");
        this.name = cleanName;
        this.color = cleanColor;
        this.extensions = normalizeExtensions(extensions);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.lastFileTaggedAt = lastFileTaggedAt;
        this.predefined = predefined;
    }

    /**
     * Remove duplicações e padroniza extensões simples ou compostas.
     *
     * @param values coleção não nula; extensões com ou sem ponto inicial e até 64 caracteres normalizados;
     *               espaços externos são aparados e texto vazio é permitido
     * @return conjunto imutável; texto vazio representa arquivo sem extensão
     * @throws NullPointerException se coleção ou elemento for nulo
     * @throws IllegalArgumentException se uma extensão exceder 64 caracteres ou contiver
     *                                  separadores, espaços internos, símbolos inválidos ou apenas ponto
     */
    public static Set<String> normalizeExtensions(Collection<String> values) {
        Set<String> result = new TreeSet<>();
        for (String value : Objects.requireNonNull(values)) {
            String ext = Objects.requireNonNull(value).strip().toLowerCase(Locale.ROOT);
            if (!ext.isEmpty() && !ext.startsWith(".")) ext = "." + ext;
            if (ext.length() > 64 || (!ext.isEmpty() && !ext.matches("\\.[\\p{L}\\p{N}_+-]+(?:\\.[\\p{L}\\p{N}_+-]+)*")))
                throw new IllegalArgumentException("Extensão inválida: " + value);
            result.add(ext);
        }
        return Set.copyOf(result);
    }

    /**
     * Verifica compatibilidade sem ler disco; extensões compostas usam o sufixo completo.
     *
     * @param file referência não nula
     * @return verdadeiro para Tag livre ou sufixo permitido
     * @throws NullPointerException se a referência for nula
     */
    public boolean accepts(NativeFile file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return extensions.isEmpty() || extensions.stream().anyMatch(ext -> ext.isEmpty()
                ? file.getExtension().isEmpty() : name.length() > ext.length() && name.endsWith(ext));
    }

    /**
     * Informa identidade estável da Tag.
     *
     * @return identidade estável da Tag
     */
    public UUID getId() { return id; }
    /**
     * Informa nome exibido; não é chave de identidade.
     *
     * @return nome exibido; não é chave de identidade
     */
    public String getName() { return name; }
    /**
     * Informa cor hexadecimal.
     *
     * @return cor hexadecimal
     */
    public String getColor() { return color; }
    /**
     * Informa conjunto imutável de extensões normalizadas.
     *
     * @return conjunto imutável de extensões normalizadas
     */
    public Set<String> getExtensions() { return extensions; }
    /**
     * Informa criação da etiqueta, distinta das datas físicas.
     *
     * @return criação da etiqueta, distinta das datas físicas
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /**
     * Informa última associação nova, ou nulo.
     *
     * @return última associação nova, ou nulo
     */
    public LocalDateTime getLastFileTaggedAt() { return lastFileTaggedAt; }
    /**
     * Informa se exige confirmação reforçada para exclusão.
     *
     * @return se exige confirmação reforçada para exclusão
     */
    public boolean isPredefined() { return predefined; }
    /**
     * Informa se o UUID identifica a sentinela protegida.
     *
     * @return se o UUID identifica a sentinela protegida
     */
    public boolean isMissing() { return id.equals(MISSING_ID); }
    /**
     * Informa igualdade pelo UUID.
     *
     * @param other objeto comparado, inclusive nulo
     * @return igualdade pelo UUID
     */
    @Override public boolean equals(Object other) { return other instanceof Tag tag && id.equals(tag.id); }
    /**
     * Informa código coerente com a identidade.
     *
     * @return código coerente com a identidade
     */
    @Override public int hashCode() { return id.hashCode(); }
    /**
     * Informa nome e UUID para distinguir nomes repetidos.
     *
     * @return nome e UUID para distinguir nomes repetidos
     */
    @Override public String toString() { return name + " [" + id + "]"; }
}
