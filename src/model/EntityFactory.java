/*
 * Inventário — EntityFactory.
 * Fábrica simples para entidades novas; DAOs reconstroem diretamente (ARQ-02).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - EntityFactory.EntityFactory(NativeFileService nativeService): Injeta o serviço que obtém metadados para a criação de cadastros.
 * - EntityFactory.createTag(String name, String color, Collection<String> extensions): Cria etiqueta comum com UUID novo e instante UTC com precisão de microssegundos.
 * - EntityFactory.createLocalFile(Path path): Cria cadastro novo lendo o arquivo, sem gravar banco ou classificar automaticamente.
 * - EntityFactory.now(): Informa instante UTC sem nanos excedentes ao DATETIME(6).
 *
 * Consulte: doc/modelo-de-dominio.md — DOM-01 a DOM-04; doc/arquitetura-e-padroes.md — ARQ-02.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package model;

import service.NativeFileService;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Fábrica simples para entidades novas; DAOs reconstroem diretamente (ARQ-02).
 */
public final class EntityFactory {
    /**
     * Serviço que lê e opera o disco, sem SQL.
     */
    private final NativeFileService nativeService;

    /**
     * Injeta o serviço que obtém metadados para a criação de cadastros.
     *
     * @param nativeService leitor responsável pelos metadados físicos, não nulo
     */
    public EntityFactory(NativeFileService nativeService) { this.nativeService = java.util.Objects.requireNonNull(nativeService); }

    /**
     * Cria etiqueta comum com UUID novo e instante UTC com precisão de microssegundos.
     *
     * @param name nome não nulo de 1 a 100 caracteres após aparar espaços externos
     * @param color cor não nula no formato {@code #RRGGBB}
     * @param extensions restrições não nulas; vazio permite todas
     * @return nova Tag ainda não persistida
     * @throws IllegalArgumentException se nome, cor ou extensão for inválido
     * @throws NullPointerException se um argumento ou elemento de extensão for nulo
     */
    public Tag createTag(String name, String color, Collection<String> extensions) {
        return new Tag(UUID.randomUUID(), name, color, extensions, now(), null, false);
    }

    /**
     * Cria cadastro novo lendo o arquivo, sem gravar banco ou classificar automaticamente.
     *
     * @param path caminho de arquivo regular
     * @return entidade com UUID novo e metadados físicos
     * @throws IOException se arquivo ausente, link simbólico ou leitura falhar
     */
    public LocalFile createLocalFile(Path path) throws IOException {
        LocalFile empty = new LocalFile(UUID.randomUUID(), new NativeFile(path), false, null, null, null, null, List.of());
        return nativeService.readMetadata(empty, true);
    }

    /**
     * Informa instante UTC sem nanos excedentes ao DATETIME(6).
     *
     * @return instante UTC sem nanos excedentes ao DATETIME(6)
     */
    public static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS); }
}
