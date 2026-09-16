/*
 * Inventário — ExplorerListener.
 * Observer de fotografias já consultadas; callbacks não repetem Refresh (ARQ-06/P-05).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - ExplorerListener.onTagsChanged(List<Tag> tags): Recebe a fotografia de etiquetas para apresentação.
 * - ExplorerListener.onFilesChanged(List<LocalFile> files): Recebe o resultado da pesquisa de arquivos para apresentação.
 * - ExplorerListener.onDirectoryChanged(NativeDirectory directory, List<NativeDirectory> directories, List<NativeFile> files, Map<Path, LocalFile> registered): Apresenta uma listagem real, inclusive arquivos sem cadastro.
 *
 * Consulte: doc/arquitetura-e-padroes.md — ARQ-06/ARQ-07; doc/requisitos-e-regras.md — OP-09/ERR-01.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package service;

import model.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Observer de fotografias já consultadas; callbacks não repetem Refresh (ARQ-06/P-05).
 */
public interface ExplorerListener {
    /**
     * Recebe a fotografia de etiquetas para apresentação.
     *
     * @param tags Tags atuais, com identidade; executar somente apresentação, na EDT
     */
    void onTagsChanged(List<Tag> tags);
    /**
     * Recebe o resultado da pesquisa de arquivos para apresentação.
     *
     * @param files resultado atual da pesquisa por Tags; não iniciar nova ação
     */
    void onFilesChanged(List<LocalFile> files);
    /**
     * Apresenta uma listagem real, inclusive arquivos sem cadastro.
     *
     * @param directory pasta exibida
     * @param directories subpastas navegáveis
     * @param files nativos que passaram pelo filtro físico
     * @param registered correspondências em lote; ausência mantém o nativo visível
     */
    void onDirectoryChanged(NativeDirectory directory, List<NativeDirectory> directories, List<NativeFile> files, Map<Path, LocalFile> registered);
}
