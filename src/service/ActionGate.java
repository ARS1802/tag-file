/*
 * Inventário — ActionGate.
 * Admissão global sem fila; só a tarefa proprietária libera o controle (DEC-02/P-12).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - ActionGate.ActionGate(): Cria controle livre que deve ser compartilhado pelos dois Controllers.
 * - ActionGate.isBusy(): Informa se uma tarefa ainda trabalha, inclusive em diálogos ou atualização visual.
 * - ActionGate.setBusyListener(Consumer<Boolean> listener): Define a atualização visual do estado ocupado.
 * - ActionGate.submit(Callable<T> task): Admite uma solicitação por comparação atômica antes de criar qualquer thread.
 * - ActionGate.onEdt(Runnable update): Executa e aguarda uma atualização Swing; não cria uma ação independente.
 *
 * Consulte: doc/arquitetura-e-padroes.md — ARQ-06/ARQ-07; doc/requisitos-e-regras.md — OP-09/ERR-01.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package service;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/**
 * Admissão global sem fila; só a tarefa proprietária libera o controle (DEC-02/P-12).
 */
public final class ActionGate {
    /**
     * Posse atômica da ação corrente.
     */
    private final AtomicBoolean busy = new AtomicBoolean();
    /**
     * Callback curto de Loading, entregue na EDT.
     */
    private volatile Consumer<Boolean> busyListener = value -> { };

    /**
     * Cria controle livre que deve ser compartilhado pelos dois Controllers.
     */
    public ActionGate() { }
    /**
     * Informa se uma tarefa ainda trabalha, inclusive em diálogos ou atualização visual.
     *
     * @return se uma tarefa ainda trabalha, inclusive em diálogos ou atualização visual
     */
    public boolean isBusy() { return busy.get(); }
    /**
     * Define a atualização visual do estado ocupado.
     *
     * @param listener atualização visual do Loading, executada na EDT; não inicia nova ação
     */
    public void setBusyListener(Consumer<Boolean> listener) { busyListener = Objects.requireNonNull(listener); }

    /**
     * Admite uma solicitação por comparação atômica antes de criar qualquer thread.
     * Solicitações ocupadas/reentrantes recebem futuro excepcional, sem fila.
     * Cancelar o futuro não interrompe a tarefa e não libera sua posse antecipadamente.
     *
     * @param <T> tipo do resultado
     * @param task todas as etapas da solicitação, inclusive confirmações e apresentação
     * @return futuro concluído após liberação; falha preserva a causa original
     */
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        Objects.requireNonNull(task);
        if (!busy.compareAndSet(false, true)) return CompletableFuture.failedFuture(new RejectedExecutionException("Há uma ação em andamento; solicitação descartada"));
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            new Thread(() -> {
                T value = null; Throwable failure = null;
                try { onEdt(() -> busyListener.accept(true)); value = task.call(); }
                catch (Throwable e) { failure = e; }
                finally {
                    try { onEdt(() -> busyListener.accept(false)); }
                    catch (Throwable e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
                    finally { busy.set(false); }
                }
                if (failure == null) future.complete(value); else future.completeExceptionally(failure);
            }, "tag-file-action").start();
        } catch (RuntimeException | Error e) { busy.set(false); future.completeExceptionally(e); }
        return future;
    }

    /**
     * Executa e aguarda uma atualização Swing; não cria uma ação independente.
     *
     * @param update trecho curto de UI
     * @throws Exception se thread for interrompida ou callback falhar
     */
    public static void onEdt(Runnable update) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) update.run(); else SwingUtilities.invokeAndWait(update);
    }
}
