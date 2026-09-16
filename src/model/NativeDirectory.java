/*
 * Inventário — NativeDirectory.
 * Representa uma pasta de navegação ou destino; pastas não recebem Tags (DOM-01).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - NativeDirectory.NativeDirectory(Path path): Normaliza a referência sem criar a pasta.
 * - NativeDirectory.getPath(): Informa caminho absoluto normalizado da pasta.
 * - NativeDirectory.toString(): Informa caminho usado na apresentação.
 *
 * Consulte: doc/modelo-de-dominio.md — DOM-01 a DOM-04; doc/arquitetura-e-padroes.md — ARQ-02.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Representa uma pasta de navegação ou destino; pastas não recebem Tags (DOM-01).
 */
public final class NativeDirectory {
    /**
     * Referência independente da existência da pasta.
     */
    private final Path path;

    /**
     * Normaliza a referência sem criar a pasta.
     *
     * @param path caminho não nulo
     * @throws NullPointerException se nulo
     */
    public NativeDirectory(Path path) { this.path = Objects.requireNonNull(path).toAbsolutePath().normalize(); }

    /**
     * Informa caminho absoluto normalizado da pasta.
     *
     * @return caminho absoluto normalizado da pasta
     */
    public Path getPath() { return path; }

    /**
     * Informa caminho usado na apresentação.
     *
     * @return caminho usado na apresentação
     */
    @Override public String toString() { return path.toString(); }
}
