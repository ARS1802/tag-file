/*
 * Inventário — ExplorerEventService.
 * Publica fotografias na EDT para observadores inscritos; não consulta disco/SQL (ARQ-06).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - ExplorerEventService.ExplorerEventService(): Cria serviço sem observadores; não é Singleton.
 * - ExplorerEventService.subscribe(ExplorerListener listener): Inscreve um observador de forma idempotente.
 * - ExplorerEventService.unsubscribe(ExplorerListener listener): Remove um observador sem alterar os demais inscritos.
 * - ExplorerEventService.publish(List<Tag> tags, List<LocalFile> matches, NativeDirectory directory, List<NativeDirectory> directories, List<NativeFile> nativeFiles, Map<Path, LocalFile> registered): Entrega dados imutáveis e aguarda atualização visual dentro da ação corrente.
 *
 * Consulte: doc/arquitetura-e-padroes.md — ARQ-06/ARQ-07; doc/requisitos-e-regras.md — OP-09/ERR-01.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package service;

import model.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Publica fotografias na EDT para observadores inscritos; não consulta disco/SQL (ARQ-06).
 */
public final class ExplorerEventService {
    /**
     * Observadores inscritos, sem duplicações.
     */
    private final Set<ExplorerListener> listeners = new CopyOnWriteArraySet<>();
    /**
     * Cria serviço sem observadores; não é Singleton.
     */
    public ExplorerEventService() { }
    /**
     * Inscreve um observador de forma idempotente.
     *
     * @param listener observador não nulo; inscrição repetida é idempotente
     */
    public void subscribe(ExplorerListener listener) { listeners.add(Objects.requireNonNull(listener)); }
    /**
     * Remove um observador sem alterar os demais inscritos.
     *
     * @param listener observador a remover ao descartar o painel
     */
    public void unsubscribe(ExplorerListener listener) { listeners.remove(listener); }
    /**
     * Entrega dados imutáveis e aguarda atualização visual dentro da ação corrente.
     *
     * @param tags etiquetas consultadas
     * @param matches arquivos resultantes da busca
     * @param directory pasta local atual
     * @param directories subpastas
     * @param nativeFiles arquivos locais, mesmo sem cadastro
     * @param registered mapa de correspondências
     * @throws Exception se interrupção ou callback impedir a entrega; falhas não viram sucesso
     */
    public void publish(List<Tag> tags, List<LocalFile> matches, NativeDirectory directory,
                        List<NativeDirectory> directories, List<NativeFile> nativeFiles, Map<Path, LocalFile> registered) throws Exception {
        List<Tag> immutableTags = List.copyOf(tags); List<LocalFile> immutableMatches = List.copyOf(matches);
        List<NativeDirectory> immutableDirectories = List.copyOf(directories); List<NativeFile> immutableFiles = List.copyOf(nativeFiles);
        Map<Path, LocalFile> immutableMap = Map.copyOf(registered);
        ActionGate.onEdt(() -> {
            RuntimeException failure = null;
            for (ExplorerListener listener : listeners) {
                try { listener.onTagsChanged(immutableTags); listener.onFilesChanged(immutableMatches); listener.onDirectoryChanged(directory, immutableDirectories, immutableFiles, immutableMap); }
                catch (RuntimeException e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
            }
            if (failure != null) throw failure;
        });
    }
}
