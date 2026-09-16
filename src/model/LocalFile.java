/*
 * Inventário — LocalFile.
 * Fotografia imutável de um cadastro; contém NativeFile, sem herdar dele (DOM-02/03).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - LocalFile.LocalFile(UUID id, NativeFile nativeFile, boolean available, Long size, LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime lastAccessedAt, Collection<Tag> tags): Reconstrói dados conhecidos sem consultar disco nem inventar datas.
 * - LocalFile.getId(): Informa UUID do cadastro.
 * - LocalFile.getNativeFile(): Informa referência física contida no cadastro.
 * - LocalFile.isAvailable(): Informa disponibilidade da última sincronização.
 * - LocalFile.getSize(): Informa bytes conhecidos, ou nulo.
 * - LocalFile.getCreatedAt(): Informa criação física, ou nulo se o provedor não a informar.
 * - LocalFile.getModifiedAt(): Informa modificação física, ou nulo.
 * - LocalFile.getLastAccessedAt(): Informa acesso físico, ou nulo.
 * - LocalFile.getTags(): Informa associações imutáveis dessa leitura.
 * - LocalFile.equals(Object other): Informa igualdade pelo UUID.
 * - LocalFile.hashCode(): Informa código baseado no UUID.
 * - LocalFile.toString(): Informa nome, disponibilidade, Tags e caminho para apresentação.
 *
 * Consulte: doc/modelo-de-dominio.md — DOM-01 a DOM-04; doc/arquitetura-e-padroes.md — ARQ-02.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Fotografia imutável de um cadastro; contém NativeFile, sem herdar dele (DOM-02/03).
 */
public final class LocalFile {
    /**
     * Identidade estável em UUID.
     */
    private final UUID id;
    /**
     * Referência física contida no cadastro.
     */
    private final NativeFile nativeFile;
    /**
     * Disponibilidade conhecida na última sincronização.
     */
    private final boolean available;
    /**
     * Tamanho físico em bytes, ou nulo quando desconhecido.
     */
    private final Long size;
    /**
     * Data física de criação em UTC, ou nulo quando o provedor não a informa.
     */
    private final LocalDateTime createdAt;
    /**
     * Última modificação física conhecida, em UTC.
     */
    private final LocalDateTime modifiedAt;
    /**
     * Último acesso físico conhecido, em UTC.
     */
    private final LocalDateTime lastAccessedAt;
    /**
     * Fotografia imutável das etiquetas associadas ao cadastro nesta leitura.
     */
    private final Set<Tag> tags;

    /**
     * Reconstrói dados conhecidos sem consultar disco nem inventar datas.
     *
     * @param id UUID não nulo, preservado em leituras e movimentos
     * @param nativeFile referência não nula
     * @param available disponibilidade observada na última leitura
     * @param size bytes conhecidos, ou nulo; nunca negativo
     * @param createdAt criação física conhecida, ou nulo
     * @param modifiedAt modificação física conhecida, ou nulo
     * @param lastAccessedAt acesso físico conhecido, ou nulo
     * @param tags relações não nulas, copiadas defensivamente
     * @throws NullPointerException se identidade, referência ou relações forem nulas
     * @throws IllegalArgumentException se o tamanho for negativo
     */
    public LocalFile(UUID id, NativeFile nativeFile, boolean available, Long size,
                     LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime lastAccessedAt,
                     Collection<Tag> tags) {
        this.id = Objects.requireNonNull(id);
        this.nativeFile = Objects.requireNonNull(nativeFile);
        if (size != null && size < 0) throw new IllegalArgumentException("Tamanho negativo");
        this.available = available;
        this.size = size;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.lastAccessedAt = lastAccessedAt;
        this.tags = Set.copyOf(tags);
    }

    /**
     * Informa UUID do cadastro.
     *
     * @return UUID do cadastro
     */
    public UUID getId() { return id; }
    /**
     * Informa referência física contida no cadastro.
     *
     * @return referência física contida no cadastro
     */
    public NativeFile getNativeFile() { return nativeFile; }
    /**
     * Informa disponibilidade da última sincronização.
     *
     * @return disponibilidade da última sincronização
     */
    public boolean isAvailable() { return available; }
    /**
     * Informa bytes conhecidos, ou nulo.
     *
     * @return bytes conhecidos, ou nulo
     */
    public Long getSize() { return size; }
    /**
     * Informa criação física, ou nulo se o provedor não a informar.
     *
     * @return criação física, ou nulo se o provedor não a informar
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /**
     * Informa modificação física, ou nulo.
     *
     * @return modificação física, ou nulo
     */
    public LocalDateTime getModifiedAt() { return modifiedAt; }
    /**
     * Informa acesso físico, ou nulo.
     *
     * @return acesso físico, ou nulo
     */
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    /**
     * Informa associações imutáveis dessa leitura.
     *
     * @return associações imutáveis dessa leitura
     */
    public Set<Tag> getTags() { return tags; }
    /**
     * Informa igualdade pelo UUID.
     *
     * @param other objeto comparado, inclusive nulo
     * @return igualdade pelo UUID
     */
    @Override public boolean equals(Object other) { return other instanceof LocalFile file && id.equals(file.id); }
    /**
     * Informa código baseado no UUID.
     *
     * @return código baseado no UUID
     */
    @Override public int hashCode() { return id.hashCode(); }
    /**
     * Informa nome, disponibilidade, Tags e caminho para apresentação.
     *
     * @return nome, disponibilidade, Tags e caminho para apresentação
     */
    @Override public String toString() {
        return nativeFile.getName() + (available ? "" : " [indisponível]") + "  "
                + tags.stream().map(Tag::getName).sorted().toList() + "  — " + nativeFile;
    }
}
