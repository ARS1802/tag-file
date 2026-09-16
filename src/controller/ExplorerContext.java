/*
 * Inventário — ExplorerContext.
 * Colaboradores e critérios da sessão compartilhada; evita Refresh disparado por Observer.
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - ExplorerContext.ExplorerContext(LocalFileManager manager, NativeFileService nativeService, LocalFileDAO files, TagDAO tags, ClipboardService clipboard, Interaction interaction, ExplorerEventService events, ActionGate gate, NativeDirectory directory, Consumer<Throwable> errors): Reúne referências compartilhadas por injeção; não cria Singletons.
 * - ExplorerContext.execute(Callable<T> task): Admite a ação inteira e publica fotografias sem chamar Refresh implicitamente.
 * - ExplorerContext.publish(): Consulta os modelos das duas visões dentro da mesma ação, sem novas solicitações.
 * - ExplorerContext.getGate(): Informa controle comum, usado pela janela para Loading e encerramento.
 *
 * Consulte: doc/arquitetura-e-padroes.md — ARQ-03/ARQ-05; doc/requisitos-e-regras.md — OP-09.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package controller;

import filter.*;
import manager.LocalFileManager;
import model.*;
import persistence.*;
import service.*;
import ui.Interaction;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Colaboradores e critérios da sessão compartilhada; evita Refresh disparado por Observer.
 */
public final class ExplorerContext {
    /**
     * Coordenação das regras e das etapas de disco/SQL.
     */
    final LocalFileManager manager;
    /**
     * Serviço que lê e opera o disco, sem SQL.
     */
    final NativeFileService nativeService;
    /**
     * DAO de cadastros compartilhando a conexão da sessão.
     */
    final LocalFileDAO files;
    /**
     * DAO de etiquetas e suas extensões.
     */
    final TagDAO tags;
    /**
     * Intenção COPY/CUT compartilhada pelas duas visões.
     */
    final ClipboardService clipboard;
    /**
     * Fronteira para obter decisões sem conhecer componentes Swing.
     */
    final Interaction interaction;
    /**
     * Serviço compartilhado de inscrições e avisos.
     */
    private final ExplorerEventService events;
    /**
     * Controle global de admissão sem fila.
     */
    private final ActionGate gate;
    /**
     * Apresentação da falha dentro da ação corrente, na EDT.
     */
    private final Consumer<Throwable> errors;
    /**
     * Pasta atual usada para navegação e colagem.
     */
    NativeDirectory directory;
    /**
     * Critérios correntes da listagem física.
     */
    NativeFileFilter nativeFilter = new NativeFileFilter();
    /**
     * Critérios correntes da pesquisa de cadastros.
     */
    LocalFileFilter localFilter = new LocalFileFilter();
    /**
     * Critérios correntes da listagem de Tags.
     */
    TagFilter tagFilter = new TagFilter();

    /**
     * Reúne referências compartilhadas por injeção; não cria Singletons.
     *
     * @param manager regras de domínio
     * @param nativeService disco
     * @param files cadastros na conexão da sessão
     * @param tags Tags na mesma conexão
     * @param clipboard intenção COPY/CUT comum
     * @param interaction respostas da UI
     * @param events observadores das duas visões
     * @param gate admissão global sem fila
     * @param directory pasta inicial existente
     * @param errors apresentação da falha na EDT, ainda dentro da ação
     */
    public ExplorerContext(LocalFileManager manager, NativeFileService nativeService, LocalFileDAO files, TagDAO tags,
                           ClipboardService clipboard, Interaction interaction, ExplorerEventService events, ActionGate gate,
                           NativeDirectory directory, Consumer<Throwable> errors) {
        this.manager = manager; this.nativeService = nativeService; this.files = files; this.tags = tags; this.clipboard = clipboard;
        this.interaction = interaction; this.events = events; this.gate = gate; this.directory = directory; this.errors = errors;
    }

    /**
     * Admite a ação inteira e publica fotografias sem chamar Refresh implicitamente.
     * Mesmo após falha tenta refletir efeitos reais; falha visual não esconde a original.
     *
     * @param <T> tipo retornado pelo fluxo
     * @param task etapas do Controller já admitidas como uma única solicitação
     * @return futuro concluído depois de publicação, tratamento e liberação
     */
    <T> CompletableFuture<T> execute(Callable<T> task) {
        return gate.submit(() -> {
            T result;
            try { result = task.call(); }
            catch (Exception failure) {
                try { publish(); } catch (Exception visual) { failure.addSuppressed(visual); }
                if (!(failure instanceof CancellationException)) ActionGate.onEdt(() -> errors.accept(failure));
                throw failure;
            }
            try { publish(); }
            catch (Exception failure) {
                OperationFailure partial = new OperationFailure("Solicitação concluída", "Atualizar apresentação", failure);
                ActionGate.onEdt(() -> errors.accept(partial)); throw partial;
            }
            return result;
        });
    }

    /**
     * Consulta os modelos das duas visões dentro da mesma ação, sem novas solicitações.
     *
     * @throws Exception se listagem, SQL ou entrega na EDT falhar
     */
    private void publish() throws Exception {
        List<NativeFile> nativeFiles = nativeService.listFiles(directory, nativeFilter);
        Map<Path, LocalFile> registered = files.findByPaths(nativeFiles.stream().map(NativeFile::getPath).toList());
        events.publish(tags.find(tagFilter), files.find(localFilter), directory, nativeService.listDirectories(directory), nativeFiles, registered);
    }

    /**
     * Informa controle comum, usado pela janela para Loading e encerramento.
     *
     * @return controle comum, usado pela janela para Loading e encerramento
     */
    public ActionGate getGate() { return gate; }
}
