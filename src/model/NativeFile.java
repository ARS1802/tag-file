/*
 * Inventário — NativeFile.
 * Referência imutável a um caminho, independente da existência física (DOM-01).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - NativeFile.NativeFile(Path path): Representa um caminho sem acessar o disco.
 * - NativeFile.getPath(): Informa caminho absoluto normalizado; a existência não é garantida.
 * - NativeFile.getName(): Informa último componente do caminho, ou o caminho inteiro para uma raiz.
 * - NativeFile.getExtension(): Obtém o último sufixo; nomes ocultos sem outro ponto não têm extensão.
 * - NativeFile.toString(): Informa caminho legível, sem consultar disponibilidade.
 *
 * Consulte: doc/modelo-de-dominio.md — DOM-01 a DOM-04; doc/arquitetura-e-padroes.md — ARQ-02.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package model;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Referência imutável a um caminho, independente da existência física (DOM-01).
 */
public final class NativeFile {
    /**
     * Caminho absoluto normalizado; não resolve links simbólicos.
     */
    private final Path path;

    /**
     * Representa um caminho sem acessar o disco.
     *
     * @param path caminho relativo ou absoluto, não nulo
     * @throws NullPointerException se o caminho for nulo
     */
    public NativeFile(Path path) {
        this.path = Objects.requireNonNull(path, "Caminho necessário").toAbsolutePath().normalize();
    }

    /**
     * Informa caminho absoluto normalizado; a existência não é garantida.
     *
     * @return caminho absoluto normalizado; a existência não é garantida
     */
    public Path getPath() { return path; }

    /**
     * Informa último componente do caminho, ou o caminho inteiro para uma raiz.
     *
     * @return último componente do caminho, ou o caminho inteiro para uma raiz
     */
    public String getName() { return path.getFileName() == null ? path.toString() : path.getFileName().toString(); }

    /**
     * Obtém o último sufixo; nomes ocultos sem outro ponto não têm extensão.
     *
     * @return extensão minúscula com ponto, ou texto vazio se ausente
     */
    public String getExtension() {
        String name = getName();
        int dot = name.lastIndexOf('.');
        return dot <= 0 || dot == name.length() - 1 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    /**
     * Informa caminho legível, sem consultar disponibilidade.
     *
     * @return caminho legível, sem consultar disponibilidade
     */
    @Override public String toString() { return path.toString(); }
}
