/*
 * Inventário — ClipboardService.
 * Clipboard interno de uma intenção; guardar CUT não altera o disco (OP-08).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - ClipboardService.ClipboardService(): Cria clipboard vazio, sem integração com o sistema operacional.
 * - ClipboardService.put(NativeFile file, UUID localId, boolean cut): Substitui a intenção anterior sem executar trabalho físico.
 * - ClipboardService.getFile(): Informa referência guardada ou vazio.
 * - ClipboardService.getLocalId(): Informa identidade guardada, ou vazio para arquivo nativo.
 * - ClipboardService.isCut(): Informa se a intenção atual é CUT; consultar o arquivo antes.
 * - ClipboardService.clear(): Limpa a intenção depois de CUT concluído, sem afetar arquivos.
 *
 * Consulte: doc/arquitetura-e-padroes.md — ARQ-06/ARQ-07; doc/requisitos-e-regras.md — OP-09/ERR-01.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package service;

import model.NativeFile;
import java.util.Optional;
import java.util.UUID;

/**
 * Clipboard interno de uma intenção; guardar CUT não altera o disco (OP-08).
 */
public final class ClipboardService {
    /**
     * Referência guardada na intenção do clipboard.
     */
    private NativeFile file;
    /**
     * UUID correspondente, ou nulo para arquivo somente nativo.
     */
    private UUID localId;
    /**
     * Verdadeiro para recortar; falso para copiar.
     */
    private boolean cut;

    /**
     * Cria clipboard vazio, sem integração com o sistema operacional.
     */
    public ClipboardService() { }
    /**
     * Substitui a intenção anterior sem executar trabalho físico.
     *
     * @param file referência nativa, não nula
     * @param localId UUID se cadastrado, ou nulo
     * @param cut verdadeiro para CUT, falso para COPY
     */
    public void put(NativeFile file, UUID localId, boolean cut) { this.file = java.util.Objects.requireNonNull(file); this.localId = localId; this.cut = cut; }
    /**
     * Informa referência guardada ou vazio.
     *
     * @return referência guardada ou vazio
     */
    public Optional<NativeFile> getFile() { return Optional.ofNullable(file); }
    /**
     * Informa identidade guardada, ou vazio para arquivo nativo.
     *
     * @return identidade guardada, ou vazio para arquivo nativo
     */
    public Optional<UUID> getLocalId() { return Optional.ofNullable(localId); }
    /**
     * Informa se a intenção atual é CUT; consultar o arquivo antes.
     *
     * @return se a intenção atual é CUT; consultar o arquivo antes
     */
    public boolean isCut() { return cut; }
    /**
     * Limpa a intenção depois de CUT concluído, sem afetar arquivos.
     */
    public void clear() { file = null; localId = null; cut = false; }
}
