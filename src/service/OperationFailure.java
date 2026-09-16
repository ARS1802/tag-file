/*
 * Inventário — OperationFailure.
 * Falha com efeitos já concluídos; causa SQL/IO permanece acessível (ERR-01).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - OperationFailure.OperationFailure(String completed, String failed, Throwable cause): Registra resultado parcial sem alegar rollback.
 * - OperationFailure.getCompleted(): Informa descrição dos efeitos confirmados.
 * - OperationFailure.getFailed(): Informa descrição da etapa malsucedida.
 *
 * Consulte: doc/arquitetura-e-padroes.md — ARQ-06/ARQ-07; doc/requisitos-e-regras.md — OP-09/ERR-01.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package service;

/**
 * Falha com efeitos já concluídos; causa SQL/IO permanece acessível (ERR-01).
 */
public final class OperationFailure extends Exception {
    /**
     * Versão de serialização exigida pelo tipo herdado; não implementa persistência da aplicação.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Efeitos cuja conclusão é conhecida.
     */
    private final String completed;
    /**
     * Etapa que não concluiu.
     */
    private final String failed;
    /**
     * Registra resultado parcial sem alegar rollback.
     *
     * @param completed efeitos confirmados antes da falha
     * @param failed etapa que não concluiu
     * @param cause causa original com SQLState/código quando disponível
     */
    public OperationFailure(String completed, String failed, Throwable cause) {
        super("Concluído: " + completed + "\nFalhou: " + failed, cause); this.completed = completed; this.failed = failed;
    }
    /**
     * Informa descrição dos efeitos confirmados.
     *
     * @return descrição dos efeitos confirmados
     */
    public String getCompleted() { return completed; }
    /**
     * Informa descrição da etapa malsucedida.
     *
     * @return descrição da etapa malsucedida
     */
    public String getFailed() { return failed; }
}
