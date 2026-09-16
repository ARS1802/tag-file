/*
 * Inventário — Main.
 * Entrada didática: exemplos reais em recursos temporários próprios; consulte doc/implementacao.md.
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - Main.Main(): Impede instanciação da entrada estática.
 * - Main.main(String[] args): Seleciona demonstração ou administração explícita; modo gráfico permanece aberto.
 * - Main.demoLocal(): Executa os exemplos de domínio, disco e coordenação que dispensam MySQL.
 * - Main.demoObserver(NativeDirectory directory, NativeFile file): Verifica os três callbacks, a EDT e o ciclo de inscrição.
 * - ExplorerListener (anônima).onTagsChanged(List<Tag> tags): Recebe Tags e verifica a entrega do aviso na EDT.
 * - ExplorerListener (anônima).onFilesChanged(List<LocalFile> files): Registra o recebimento do resultado de arquivos.
 * - ExplorerListener (anônima).onDirectoryChanged(NativeDirectory current, List<NativeDirectory> directories, List<NativeFile> files, Map<Path, LocalFile> registered): Verifica a entrega de nativos sem cadastro artificial.
 * - Main.demoActionGate(): Verifica rejeição, reentrância e término real após cancelamento do futuro.
 * - Main.demoEnvironmentRefusal(Path temporary): Simula processos ausentes/cancelados; não afirma instalar MySQL com estes dublês.
 * - Main.demoDatabase(): Executa exemplos integrados com JDBC e recursos temporários próprios.
 * - Main.demoPersistence(DemoSession s): Verifica CRUD genérico, reconstrução e propriedade da conexão.
 * - Main.demoClassification(DemoSession s): Verifica associação, pesquisa, indisponibilidade e ciclo da sentinela.
 * - Main.demoDecisions(DemoSession s): Confere as decisões P-01/02/03 e diálogos de classificação usando banco real.
 * - Main.demoEditingAndRenaming(DemoSession s, DemoInteraction ui): Verifica confirmações e efeitos de editar restrições e renomear.
 * - Main.demoCopyAndDeletion(DemoSession s, DemoInteraction ui): Verifica cópia com substituição e as três modalidades de exclusão.
 * - Main.demoSuggestions(DemoSession s, DemoInteraction ui): Verifica sugestões compatíveis e supressão limitada à sessão.
 * - Main.demoRelocationAndBatch(DemoSession s, DemoInteraction ui): Verifica relocalização, remoção explícita e interrupção de um lote na primeira falha.
 * - Main.demoClipboardAndFailures(DemoSession s): Verifica CUT entre exploradores e falha SQL posterior ao movimento.
 * - Main.demoControllerGate(DemoSession s): Verifica a admissão comum das entradas dos dois Controllers.
 * - Main.prepareEnvironment(boolean allowInstall): Prepara explicitamente instância e schema, sem limpar cadastros.
 * - Main.demoUi(boolean automated): Exibe Panels reais; somente o modo de verificação fecha automaticamente a janela própria.
 * - Main.verifyUi(DemoSession s, DemoWindow window, Tag tag): Usa componentes Swing reais para verificar Observer, atalhos, Drop e diálogo modal.
 * - Main.verifyFailureDialog(DemoSession s, DemoWindow window): Provoca falha SQL real após mover e inspeciona o pop-up e sua expansão técnica.
 * - Main.descendants(Container root): Percorre componentes da própria janela para verificações gráficas.
 * - Main.findButton(Container root, String text): Localiza um botão pelo rótulo na apresentação testada.
 * - DemoWindow.DemoWindow(DemoSession session, DatabaseManager environment): Monta a janela e os dois Panels na EDT, com Loading compartilhado.
 * - WindowAdapter (anônima).windowClosing(WindowEvent event): Solicita encerramento respeitando a ação ainda em andamento.
 * - DemoWindow.dispose(): Remove observadores e fecha apenas as janelas desta demonstração; chamar na EDT.
 * - Main.check(boolean condition, String description): Confere uma previsão e registra evidência somente se ela for satisfeita.
 * - Main.expectFailure(CompletableFuture<?> future, Class<? extends Throwable> expected): Confere o tipo da causa excepcional, sem usar assert.
 * - Main.awaitFree(ActionGate gate): Aguarda com prazo o término real do trabalho cujo futuro foi cancelado.
 * - Main.deleteTemporaryTree(Path root): Remove apenas a árvore temporária de propriedade da demonstração.
 * - DemoInteraction.DemoInteraction(): Roteiro inicialmente vazio: qualquer diálogo não previsto faz o teste falhar.
 * - DemoInteraction.choose(String message, String[] options): Consome uma decisão previamente especificada pelo cenário.
 * - DemoInteraction.text(String message, String initial): Consome uma entrada de texto previamente especificada pelo cenário.
 * - DemoInteraction.files(boolean multiple, Set<String> extensions): Fornece a seleção explícita do cenário, sem varrer pastas.
 * - DemoSession.DemoSession(Interaction interaction): Abre JDBC e compõe os colaboradores reais usados pela demonstração.
 * - DemoSession.file(String name): Cria conteúdo de teste dentro da pasta temporária própria.
 * - DemoSession.newTag(String name, Set<String> extensions): Persiste uma Tag e registra seu UUID para a limpeza da demonstração.
 * - DemoSession.close(): Remove recursos próprios e fecha a conexão mesmo quando a limpeza falhar.
 *
 * Consulte: doc/criterios-de-aceite.md — ACE-01 a ACE-25.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

import controller.*;
import filter.*;
import manager.LocalFileManager;
import model.*;
import persistence.*;
import service.*;
import ui.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Entrada didática: exemplos reais em recursos temporários próprios; consulte doc/implementacao.md.
 */
public final class Main {
    /**
     * Impede instanciação da entrada estática.
     */
    private Main() { }
    /**
     * Seleciona demonstração ou administração explícita; modo gráfico permanece aberto.
     *
     * @param args modo documentado; vazio equivale a --demo-local
     * @throws Exception se execução ou alguma verificação falhar
     */
    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "--demo-local" : args[0]) {
            case "--demo-local" -> demoLocal();
            case "--demo-db" -> demoDatabase();
            case "--demo-ui" -> demoUi(false);
            case "--demo-ui-check" -> demoUi(true);
            case "--demo-env" -> System.out.println(new DatabaseManager(Path.of(".")).inspect());
            case "--env-prepare" -> prepareEnvironment(Arrays.asList(args).contains("--allow-install"));
            case "--env-stop" -> new DatabaseManager(Path.of(".")).stop();
            default -> throw new IllegalArgumentException("Use --demo-local, --demo-db, --demo-ui, --demo-ui-check, --demo-env, --env-prepare [--allow-install] ou --env-stop");
        }
    }

    /**
     * Executa os exemplos de domínio, disco e coordenação que dispensam MySQL.
     *
     * @throws Exception se domínio, disco, Observer ou exclusividade não atenderem às previsões
     */
    private static void demoLocal() throws Exception {
        Path root = Files.createTempDirectory("tag-file-local-");
        try {
            NativeFile absent = new NativeFile(root.resolve("inexistente.pdf"));
            check(!Files.exists(absent.getPath()), "NativeFile representa caminho ausente: " + absent);
            NativeDirectory directory = new NativeDirectory(root); NativeFileService service = new NativeFileService(); EntityFactory factory = new EntityFactory(service);
            Path pdf = Files.writeString(root.resolve("prova.PDF"), "arquivo temporário");
            Path cdr = Files.writeString(root.resolve("desenho.cdr"), "conteúdo de teste"); Files.writeString(root.resolve("foto.png"), "teste");
            Tag restricted = factory.createTag("Faculdade", "#2864B4", List.of("PDF", ".PDF", "CDR"));
            Tag free = factory.createTag("Importante", "#B42864", Set.of());
            check(restricted.getExtensions().equals(Set.of(".pdf", ".cdr")), "ACE-04/05: extensões específicas normalizadas sem duplicação");
            check(restricted.accepts(new NativeFile(pdf)) && restricted.accepts(new NativeFile(cdr)) && !restricted.accepts(new NativeFile(root.resolve("foto.png"))), "Compatibilidade restrita");
            check(free.accepts(absent) && free.accepts(new NativeFile(root.resolve("sem-extensao"))), "ACE-06: Tag livre aceita qualquer extensão");
            LocalFile local = factory.createLocalFile(pdf);
            check(local.getSize() == Files.size(pdf) && local.isAvailable(), "Factory cria composição com UUID=" + local.getId() + "; bytes=" + local.getSize() + "; criação física=" + local.getCreatedAt());
            Filter criterion = new NativeFileFilter(Set.of("PDF"), 1L, null, null, null);
            check(service.listFiles(directory, (NativeFileFilter) criterion).size() == 1, "Filter abstrato referenciando NativeFileFilter real");
            ClipboardService clipboard = new ClipboardService(); clipboard.put(new NativeFile(pdf), local.getId(), true);
            check(clipboard.isCut() && clipboard.getLocalId().orElseThrow().equals(local.getId()) && Files.exists(pdf), "CUT apenas guarda intenção");
            NativeFile copy = service.copy(new NativeFile(pdf), root.resolve("copia.pdf"), false);
            check(Files.exists(pdf) && Files.exists(copy.getPath()), "COPY físico preserva origem");
            NativeFile moved = service.move(copy, root.resolve("renomeado.pdf"), false);
            check(!Files.exists(copy.getPath()) && Files.exists(moved.getPath()), "Movimento/renomeação física"); service.delete(moved);
            check(!Files.exists(moved.getPath()), "Exclusão física somente do arquivo próprio");
            demoObserver(directory, new NativeFile(pdf)); demoActionGate(); demoEnvironmentRefusal(root);
        } finally { deleteTemporaryTree(root); }
        System.out.println("DEMO-LOCAL: PASSOU");
    }

    /**
     * Verifica os três callbacks, a EDT e o ciclo de inscrição.
     *
     * @param directory pasta temporária
     * @param file nativo sem SQL
     * @throws Exception se entrega na EDT ou inscrição falhar
     */
    private static void demoObserver(NativeDirectory directory, NativeFile file) throws Exception {
        ExplorerEventService events = new ExplorerEventService(); AtomicInteger count = new AtomicInteger();
        ExplorerListener observer = new ExplorerListener() {
            /**
             * Recebe Tags e verifica a entrega do aviso na EDT.
             *
             * @param tags fotografia de Tags
             */
            @Override public void onTagsChanged(List<Tag> tags) { check(SwingUtilities.isEventDispatchThread(), "Observer na EDT"); count.incrementAndGet(); }
            /**
             * Registra o recebimento do resultado de arquivos.
             *
             * @param files resultado de pesquisa
             */
            @Override public void onFilesChanged(List<LocalFile> files) { count.incrementAndGet(); }
            /**
             * Verifica a entrega de nativos sem cadastro artificial.
             *
             * @param current pasta
             * @param directories subpastas
             * @param files nativos
             * @param registered correspondências SQL
             */
            @Override public void onDirectoryChanged(NativeDirectory current, List<NativeDirectory> directories, List<NativeFile> files, Map<Path, LocalFile> registered) {
                check(files.size() == 1 && registered.isEmpty(), "Aviso nativo sem cadastro artificial"); count.incrementAndGet();
            }
        };
        events.subscribe(observer); events.subscribe(observer);
        events.publish(List.of(), List.of(), directory, List.of(), List.of(file), Map.of()); check(count.get() == 3, "Inscrição idempotente; três callbacks");
        events.unsubscribe(observer); events.publish(List.of(), List.of(), directory, List.of(), List.of(file), Map.of()); check(count.get() == 3, "Remoção do observador funciona");
    }

    /**
     * Verifica rejeição, reentrância e término real após cancelamento do futuro.
     *
     * @throws Exception se rejeição, liberação ou reentrância violarem DEC-02
     */
    private static void demoActionGate() throws Exception {
        ActionGate gate = new ActionGate(); CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1); AtomicInteger executed = new AtomicInteger();
        CompletableFuture<Integer> first = gate.submit(() -> {
            started.countDown(); expectFailure(gate.submit(() -> 99), RejectedExecutionException.class);
            if (!release.await(10, TimeUnit.SECONDS)) throw new IOException("Timeout demonstrativo"); return executed.incrementAndGet();
        });
        check(started.await(10, TimeUnit.SECONDS), "Ação controlada começou");
        expectFailure(gate.submit(() -> executed.addAndGet(100)), RejectedExecutionException.class);
        check(gate.isBusy() && executed.get() == 0, "Rejeição não libera proprietária");
        first.cancel(true); check(gate.isBusy(), "Cancelar futuro não libera trabalho ainda em andamento"); release.countDown(); awaitFree(gate);
        check(executed.get() == 1, "Sem execução posterior da ação rejeitada");
        expectFailure(gate.submit(() -> { throw new IOException("falha controlada"); }), IOException.class); check(!gate.isBusy(), "Liberação após falha real");
        expectFailure(gate.submit(() -> { throw new CancellationException(); }), CancellationException.class);
        check(!gate.isBusy() && gate.submit(() -> 42).get() == 42, "Liberação após cancelamento real e sucesso posterior");
    }

    /**
     * Simula processos ausentes/cancelados; não afirma instalar MySQL com estes dublês.
     *
     * @param temporary raiz temporária própria
     * @throws Exception se recusa for tratada como sucesso ou instalação ocorrer sem autorização
     */
    private static void demoEnvironmentRefusal(Path temporary) throws Exception {
        if (System.getProperty("os.name").startsWith("Windows")) { System.out.println("Recusa administrativa simulada: NÃO EXECUTADO neste roteiro Bash"); return; }
        Path fixture = Files.createDirectory(temporary.resolve("ambiente-simulado")); Path scripts = Files.createDirectories(fixture.resolve("database/scripts/linux"));
        Files.writeString(scripts.resolve("check.sh"), "#!/bin/bash\nexit 4\n");
        Files.writeString(scripts.resolve("install.sh"), "#!/bin/bash\nprintf 'instalacao cancelada pelo usuario\\n'\nexit 17\n");
        DatabaseManager manager = new DatabaseManager(fixture);
        try { manager.prepare(false); throw new IllegalStateException("Ausência virou sucesso"); }
        catch (IOException e) { check(e.getMessage().contains("não autorizada"), "ACE-24: recusa explícita impede instalação (processos simulados)"); }
        try { manager.prepare(true); throw new IllegalStateException("Cancelamento virou sucesso"); }
        catch (IOException e) { check(e.getMessage().contains("17"), "ACE-24: saída de instalação cancelada é propagada (processo simulado)"); }
    }

    /**
     * Executa exemplos integrados com JDBC e recursos temporários próprios.
     *
     * @throws Exception se JDBC ou integração das regras falhar
     */
    private static void demoDatabase() throws Exception {
        try (DemoSession s = new DemoSession(new DemoInteraction())) {
            demoPersistence(s); demoClassification(s); demoDecisions(s); demoClipboardAndFailures(s); demoControllerGate(s);
        }
        System.out.println("DEMO-DB: PASSOU");
    }

    /**
     * Verifica CRUD genérico, reconstrução e propriedade da conexão.
     *
     * @param s sessão demonstrativa
     * @throws Exception se identidade, datas ou conexão divergirem
     */
    private static void demoPersistence(DemoSession s) throws Exception {
        CrudDAO<Tag, TagFilter> generic = s.tags; Tag tag = s.newTag("CRUD", Set.of("PDF", ".PDF", "cdr")); Tag read = generic.findById(tag.getId()).orElseThrow();
        check(read.getId().equals(tag.getId()) && read.getCreatedAt().equals(tag.getCreatedAt()) && read.getExtensions().equals(Set.of(".pdf", ".cdr")), "TagDAO/CrudDAO preservam UUID, datas, extensões");
        Path path = s.file("isolado.pdf"), other = s.file("sem-cadastro.pdf"); LocalFile original = s.factory.createLocalFile(path); s.files.create(original);
        CrudDAO<LocalFile, LocalFileFilter> files = s.files; LocalFile loaded = files.findById(original.getId()).orElseThrow();
        check(loaded.getId().equals(original.getId()) && Objects.equals(loaded.getCreatedAt(), original.getCreatedAt()) && loaded.getModifiedAt().equals(original.getModifiedAt()), "Reconstrução de LocalFile não inventa UUID/datas");
        s.links.associate(original.getId(), tag.getId()); var time = s.tags.findById(tag.getId()).orElseThrow().getLastFileTaggedAt();
        check(!s.links.associate(original.getId(), tag.getId()) && s.tags.findById(tag.getId()).orElseThrow().getLastFileTaggedAt().equals(time), "Associação repetida não muda lastFileTaggedAt");
        check(s.links.findTags(Set.of(original.getId())).get(original.getId()).contains(tag), "LocalFileTagDAO consulta vínculo"); s.links.dissociate(original.getId(), tag.getId());
        check(s.files.findById(original.getId()).isPresent() && generic.findById(tag.getId()).isPresent(), "Retirar vínculo preserva entidades");
        Files.writeString(path, "novo conteúdo de tamanho diferente"); files.update(s.nativeService.readMetadata(loaded, true));
        check(files.findById(original.getId()).orElseThrow().getSize().equals(Files.size(path)), "Atualização de metadados gravada");
        Map<Path, LocalFile> batch = s.files.findByPaths(List.of(path, other)); check(batch.size() == 1 && batch.containsKey(path), "Map em lote distingue nativo sem cadastro");
        check(!s.database.getConnection().isClosed() && generic.find(new TagFilter()).contains(tag), "Conexão compartilhada aberta; TagFilter retorna Tags");
        if (s.files.find(new LocalFileFilter()).stream().allMatch(f -> f.getNativeFile().getPath().startsWith(s.root))) {
            s.links.associate(original.getId(), Tag.MISSING_ID); LocalFile orphan = s.factory.createLocalFile(other); s.files.create(orphan);
            check(s.manager.initialize() == 2 && Files.exists(path) && Files.exists(other) && s.tags.findById(Tag.MISSING_ID).isPresent(), "ACE-12: inicialização limpa sentinela/órfão e preserva disco");
        } else System.out.println("ACE-12: NÃO EXECUTADO — existem cadastros externos à demonstração");
    }

    /**
     * Verifica associação, pesquisa, indisponibilidade e ciclo da sentinela.
     *
     * @param s sessão demonstrativa
     * @throws Exception se associação, pesquisa ou ciclo falhar
     */
    private static void demoClassification(DemoSession s) throws Exception {
        Path a = s.file("prova.txt"), b = s.file("trabalho.txt"), nativeOnly = s.file("nativo.txt");
        Tag first = s.newTag("Faculdade", Set.of("txt")), second = s.newTag("Importante", Set.of()); s.primaryTag = first;
        LocalFile one = s.tagController.associate(List.of(a), first.getId()).get().getFirst();
        LocalFile again = s.tagController.associate(List.of(a, b), second.getId()).get().getFirst();
        check(one.getId().equals(again.getId()), "ACE-02: segunda associação reutiliza UUID");
        List<LocalFile> and = s.tagController.search(new LocalFileFilter(Set.of(first.getId(), second.getId()), true, new NativeFileFilter())).get();
        List<LocalFile> or = s.tagController.search(new LocalFileFilter(Set.of(first.getId(), second.getId()), false, new NativeFileFilter())).get();
        check(and.size() == 1 && or.size() == 2 && or.stream().map(LocalFile::getId).distinct().count() == 2, "ACE-15: AND/OR sem duplicação");
        check(s.localController.navigate(new NativeDirectory(s.root)).get().stream().anyMatch(f -> f.getPath().equals(nativeOnly)) && s.files.findByPath(nativeOnly).isEmpty(), "ACE-01/16: navegar mantém nativo sem cadastro");
        s.tagController.removeTag(one.getId(), first.getId()).get(); s.tagController.removeTag(one.getId(), second.getId()).get();
        check(s.files.findById(one.getId()).orElseThrow().getTags().stream().allMatch(Tag::isMissing), "ACE-10: última Tag retirada aplica sentinela");
        int before = s.manager.getRefreshCount(); s.localController.refresh().get();
        check(s.manager.getRefreshCount() == before + 1 && s.files.findById(one.getId()).isPresent(), "ACE-13/21: Refresh único sem limpeza");
        s.tagController.associate(List.of(a), first.getId()).get();
        check(s.files.findById(one.getId()).orElseThrow().getTags().stream().noneMatch(Tag::isMissing), "ACE-11: Tag normal retira vínculo da sentinela");
        expectFailure(s.tagController.deleteTag(Tag.MISSING_ID), IllegalArgumentException.class); check(s.tags.findById(Tag.MISSING_ID).isPresent(), "ACE-14: sentinela protegida");
        s.primaryFile = s.files.findById(one.getId()).orElseThrow();
        Path missing = s.file("ausente.xyz"); Tag unavailableTag = s.newTag("Indisponível", Set.of());
        LocalFile unavailable = s.tagController.associate(List.of(missing), unavailableTag.getId()).get().getFirst(); Files.delete(missing); s.localController.refresh().get();
        check(s.tags.countAvailable(unavailableTag.getId()) == 0 && s.tags.find(new TagFilter(true, null, null)).stream().noneMatch(t -> t.equals(unavailableTag)) && s.files.findById(unavailable.getId()).isPresent(), "Zero disponíveis não significa Tag vazia; indisponibilidade preserva cadastro");
    }

    /**
     * Confere as decisões P-01/02/03 e diálogos de classificação usando banco real.
     *
     * @param s sessão com respostas explicitamente previstas
     * @throws Exception se algum efeito/confirmacão divergir das regras
     */
    private static void demoDecisions(DemoSession s) throws Exception {
        DemoInteraction ui = (DemoInteraction) s.interaction;
        Tag restricted = s.newTag("Restrita", Set.of("abc")); Path incompatible = s.file("incompativel.xyz");
        ui.choices.add(2); expectFailure(s.tagController.associate(List.of(incompatible), restricted.getId()), CancellationException.class);
        check(s.files.findByPath(incompatible).isEmpty() && s.tags.findById(restricted.getId()).orElseThrow().getExtensions().equals(Set.of(".abc")), "ACE-07: cancelar incompatibilidade não cadastra nem amplia Tag");
        ui.choices.add(1); s.tagController.associate(List.of(incompatible), restricted.getId()).get();
        check(s.tags.findById(restricted.getId()).orElseThrow().getExtensions().contains(".xyz"), "ACE-07: extensão ampliada somente após escolha");
        Path newType = s.file("outra.zzz"); ui.choices.add(0);
        String newName = "demo-nova-" + UUID.randomUUID(); ui.texts.addAll(List.of(newName, "#123456", "zzz"));
        LocalFile newly = s.tagController.associate(List.of(newType), restricted.getId()).get().getFirst();
        Tag created = newly.getTags().stream().filter(t -> t.getName().equals(newName)).findFirst().orElseThrow(); s.ownedTags.add(created.getId());
        check(newly.getTags().contains(created) && !newly.getTags().contains(restricted), "ACE-07: alternativa criar nova etiqueta associa apenas a escolhida");
        int count = s.tags.find(new TagFilter()).size();
        ui.texts.addAll(List.of(created.getName(), created.getColor(), "zzz")); ui.choices.add(1); expectFailure(s.tagController.createTag(), CancellationException.class);
        check(s.tags.find(new TagFilter()).size() == count, "ACE-09: duplicidade cancelada não cria Tag");
        ui.texts.addAll(List.of(created.getName(), created.getColor(), "zzz")); ui.choices.addAll(List.of(0, 1)); Tag duplicate = s.tagController.createTag().get(); s.ownedTags.add(duplicate.getId());
        check(!duplicate.getId().equals(created.getId()), "ACE-09: nome repetido confirmado tem UUID próprio");
        demoEditingAndRenaming(s, ui);
        demoCopyAndDeletion(s, ui);
        demoRelocationAndBatch(s, ui);
        demoSuggestions(s, ui);
        s.database.close();
        try { s.tags.find(new TagFilter()); throw new IllegalStateException("SQL fechado virou resultado vazio"); }
        catch (SQLException e) { check("08003".equals(e.getSQLState()), "Falha JDBC preserva SQLState; não vira coleção vazia"); }
        finally { s.database.open(); }
        check(ui.choices.isEmpty() && ui.texts.isEmpty(), "Todos os diálogos previstos foram realmente consumidos");
    }

    /**
     * Verifica confirmações e efeitos de editar restrições e renomear.
     *
     * @param s sessão
     * @param ui roteiro de respostas
     * @throws Exception se edição ou renomeação violar confirmação/identidade
     */
    private static void demoEditingAndRenaming(DemoSession s, DemoInteraction ui) throws Exception {
        Tag editable = s.newTag("Editar", Set.of("aaa", "bbb")); Path a = s.file("edicao.aaa"), b = s.file("edicao.bbb");
        LocalFile fa = s.tagController.associate(List.of(a, b), editable.getId()).get().getFirst();
        ui.texts.addAll(List.of(editable.getName(), editable.getColor(), "bbb")); ui.choices.add(1); expectFailure(s.tagController.editTag(editable.getId()), CancellationException.class);
        check(s.files.findById(fa.getId()).orElseThrow().getTags().contains(editable), "ACE-08: cancelar edição preserva vínculos");
        ui.texts.addAll(List.of(editable.getName(), editable.getColor(), "bbb")); ui.choices.add(0); s.tagController.editTag(editable.getId()).get();
        check(s.files.findById(fa.getId()).orElseThrow().getTags().stream().allMatch(Tag::isMissing) && Files.exists(a), "ACE-08: confirmar retira só incompatíveis e aplica sentinela");
        ui.texts.addAll(List.of(editable.getName(), editable.getColor(), "")); s.tagController.editTag(editable.getId()).get();
        check(s.tags.findById(editable.getId()).orElseThrow().getExtensions().isEmpty(), "Retirar última extensão torna a Tag livre");
        Tag renameTag = s.newTag("Renomear", Set.of("qqq")); Path path = s.file("renomear.qqq");
        LocalFile file = s.tagController.associate(List.of(path), renameTag.getId()).get().getFirst(); String content = Files.readString(path);
        ui.texts.add("renomear.rrr"); ui.choices.add(2); expectFailure(s.localController.rename(new NativeFile(path)), CancellationException.class);
        check(Files.exists(path), "ACE-18: cancelar mudança de extensão preserva disco");
        ui.texts.add("renomear.rrr"); ui.choices.add(0); NativeFile renamed = s.localController.rename(new NativeFile(path)).get();
        check(s.files.findByPath(renamed.getPath()).orElseThrow().getId().equals(file.getId()) && s.files.findById(file.getId()).orElseThrow().getTags().stream().allMatch(Tag::isMissing), "ACE-18: retirar incompatíveis preserva UUID e aplica sentinela");
        ui.texts.add("renomear.qqq"); NativeFile back = s.localController.rename(renamed).get(); s.tagController.associate(List.of(back.getPath()), renameTag.getId()).get();
        ui.texts.add("renomear.rrr"); ui.choices.add(1); NativeFile extended = s.localController.rename(back).get();
        check(s.tags.findById(renameTag.getId()).orElseThrow().getExtensions().contains(".rrr") && Files.readString(extended.getPath()).equals(content), "ACE-18: permitir nova extensão mantém conteúdo e Tags");
        Path homonym = Files.createDirectory(s.root.resolve("homonimos")).resolve(extended.getName()); Files.writeString(homonym, "conteúdo diferente");
        LocalFile other = s.tagController.associate(List.of(homonym), renameTag.getId()).get().getFirst();
        check(!other.getId().equals(file.getId()), "ACE-03: mesmo nome em outra pasta tem identidade distinta");
    }

    /**
     * Verifica cópia com substituição e as três modalidades de exclusão.
     *
     * @param s sessão
     * @param ui confirmações
     * @throws Exception se cópia/substituição ou três exclusões divergirem de P-01/P-02
     */
    private static void demoCopyAndDeletion(DemoSession s, DemoInteraction ui) throws Exception {
        Tag sourceTag = s.newTag("OrigemCopia", Set.of("cpy")), destinationTag = s.newTag("DestinoCopia", Set.of());
        Path original = s.file("copia.cpy"), destination = Files.createDirectory(s.root.resolve("copias")).resolve("copia.cpy"); Files.writeString(destination, "destino antigo");
        LocalFile source = s.tagController.associate(List.of(original), sourceTag.getId()).get().getFirst();
        LocalFile oldTarget = s.tagController.associate(List.of(destination), destinationTag.getId()).get().getFirst();
        s.tagController.clipboard(new NativeFile(original), false).get(); ui.choices.addAll(List.of(0, 0));
        NativeFile copy = s.localController.paste(new NativeDirectory(destination.getParent())).get(); LocalFile copied = s.files.findByPath(copy.getPath()).orElseThrow();
        check(!copied.getId().equals(source.getId()) && !copied.getId().equals(oldTarget.getId()) && s.files.findById(oldTarget.getId()).isEmpty()
                && copied.getTags().equals(source.getTags()) && Files.exists(original), "ACE-20/P-01: substituir cria UUID novo, descarta cadastro anterior e herda só Tags confirmadas");
        ui.choices.addAll(List.of(1, 0)); s.localController.paste(new NativeDirectory(destination.getParent())).get();
        check(s.files.findByPath(destination).isEmpty() && s.files.findById(copied.getId()).isEmpty() && Files.exists(original) && Files.exists(destination), "P-01: cópia sem herança fica apenas nativa, inclusive ao substituir");
        ui.choices.addAll(List.of(0, 1)); NativeFile both = s.localController.paste(new NativeDirectory(destination.getParent())).get();
        check(!both.getPath().equals(destination) && Files.exists(destination) && Files.exists(both.getPath()), "Conflito: manter ambos conserva dois caminhos");
        ui.choices.addAll(List.of(0, 2)); expectFailure(s.localController.paste(new NativeDirectory(destination.getParent())), CancellationException.class);
        check(Files.exists(destination), "Conflito cancelado preserva destino");
        for (int mode = 0; mode < 3; mode++) {
            Tag selected = s.newTag("Exclusao" + mode, Set.of()), other = s.newTag("Outra" + mode, Set.of()); Path path = s.file("exclusao-" + mode + ".dat");
            LocalFile file = s.tagController.associate(List.of(path), selected.getId()).get().getFirst(); s.tagController.associate(List.of(path), other.getId()).get();
            ui.choices.add(mode); if (mode > 0) ui.choices.add(0); s.tagController.deleteTag(selected.getId()).get();
            check(s.tags.findById(selected.getId()).isEmpty() && s.tags.findById(other.getId()).isPresent() && Files.exists(path) == (mode != 2)
                    && s.files.findById(file.getId()).isPresent() == (mode == 0), "ACE-19: modalidade " + (mode + 1) + " tem efeitos próprios e preserva outras Tags");
        }
        Tag predefined = new Tag(UUID.randomUUID(), "demo-predef-" + UUID.randomUUID(), "#123456", Set.of(), EntityFactory.now(), null, true);
        s.ownedTags.add(predefined.getId()); s.tags.create(predefined); ui.choices.addAll(List.of(0, 1)); expectFailure(s.tagController.deleteTag(predefined.getId()), CancellationException.class);
        check(s.tags.findById(predefined.getId()).isPresent(), "Predefinida exige confirmação reforçada");
        ui.choices.addAll(List.of(0, 0)); s.tagController.deleteTag(predefined.getId()).get();
    }

    /**
     * Verifica sugestões compatíveis e supressão limitada à sessão.
     *
     * @param s sessão
     * @param ui roteiro
     * @throws Exception se sugestão/supressão da sessão divergir de P-03
     */
    private static void demoSuggestions(DemoSession s, DemoInteraction ui) throws Exception {
        Tag normal = s.newTag("Sugestoes", Set.of("sug"));
        for (String label : List.of("A", "B")) {
            Tag predefined = new Tag(UUID.randomUUID(), "demo-sug-" + label + "-" + UUID.randomUUID(), "#123456", Set.of("sug"), EntityFactory.now(), null, true);
            s.ownedTags.add(predefined.getId()); s.tags.create(predefined);
        }
        ui.choices.add(0); LocalFile chosen = s.tagController.associate(List.of(s.file("sugerido.sug")), normal.getId()).get().getFirst();
        check(chosen.getTags().size() == 2 && ui.lastOptions.size() == 4, "P-03: todas as predefinidas compatíveis são oferecidas; só a escolhida é aplicada");
        ui.choices.add(3); LocalFile muted = s.tagController.associate(List.of(s.file("silenciar.sug")), normal.getId()).get().getFirst();
        LocalFile next = s.tagController.associate(List.of(s.file("sem-pergunta.sug")), normal.getId()).get().getFirst();
        check(muted.getTags().equals(Set.of(normal)) && next.getTags().equals(Set.of(normal)), "P-03: silenciar não classifica automaticamente");
        LocalFileManager nextSession = new LocalFileManager(s.nativeService, s.factory, s.files, s.tags, s.links);
        ui.choices.add(1); LocalFile askedAgain = nextSession.associate(List.of(s.file("nova-sessao.sug")), normal.getId(), ui).getFirst();
        check(askedAgain.getTags().size() == 2, "P-03: nova sessão volta a oferecer sugestões");
    }

    /**
     * Verifica relocalização, remoção explícita e interrupção de um lote na primeira falha.
     *
     * @param s sessão de teste
     * @param ui respostas previstas; caminhos são arquivos próprios
     * @throws Exception se UUID, vínculos, proteção de colisão ou lote divergirem
     */
    private static void demoRelocationAndBatch(DemoSession s, DemoInteraction ui) throws Exception {
        Tag tag = s.newTag("Relocalizar", Set.of("loc")); Path oldPath = s.file("antigo.loc");
        LocalFile old = s.tagController.associate(List.of(oldPath), tag.getId()).get().getFirst();
        Path found = s.root.resolve("encontrado.loc"); Files.move(oldPath, found); ui.selected = List.of(found); ui.choices.add(0);
        NativeFile relocated = s.tagController.locate(new NativeFile(oldPath)).get();
        check(relocated.getPath().equals(found) && s.files.findByPath(found).orElseThrow().getId().equals(old.getId()) && s.files.findById(old.getId()).orElseThrow().getTags().equals(old.getTags()), "Relocalizar preserva UUID e Tags sem mover novamente");
        Path otherPath = s.file("colisao.loc"); LocalFile other = s.tagController.associate(List.of(otherPath), tag.getId()).get().getFirst();
        Files.delete(found); ui.selected = List.of(otherPath); ui.choices.add(0); expectFailure(s.tagController.locate(relocated), IOException.class);
        check(s.files.findById(old.getId()).isPresent() && s.files.findById(other.getId()).isPresent(), "Colisão na relocalização não mescla nem remove cadastros");
        ui.choices.addAll(List.of(1, 0)); expectFailure(s.tagController.locate(relocated), CancellationException.class);
        check(s.files.findById(old.getId()).isEmpty() && Files.exists(otherPath), "P-02: remoção explícita retira cadastro imediatamente e preserva disco");
        Path first = s.file("lote-primeiro.loc"), last = s.file("lote-ultimo.loc"), absent = s.root.resolve("lote-ausente.loc");
        expectFailure(s.tagController.associate(List.of(first, absent, last), tag.getId()), OperationFailure.class);
        check(s.files.findByPath(first).isPresent() && s.files.findByPath(last).isEmpty(), "Lote para na primeira falha; concluídos preservados e posteriores não executados");
        check(s.clipboard.getFile().isPresent() && !s.clipboard.isCut(), "COPY permanece no clipboard após colagens");
    }

    /**
     * Verifica CUT entre exploradores e falha SQL posterior ao movimento.
     *
     * @param s sessão demonstrativa
     * @throws Exception se CUT ou falha parcial divergirem
     */
    private static void demoClipboardAndFailures(DemoSession s) throws Exception {
        NativeFile original = s.primaryFile.getNativeFile(); Path destination = Files.createDirectory(s.root.resolve("destino"));
        s.tagController.clipboard(original, true).get(); check(Files.exists(original.getPath()), "CUT entre visões mantém origem até colar");
        NativeFile moved = s.localController.paste(new NativeDirectory(destination)).get(); LocalFile loaded = s.files.findByPath(moved.getPath()).orElseThrow();
        check(loaded.getId().equals(s.primaryFile.getId()) && loaded.getTags().equals(s.primaryFile.getTags()) && !Files.exists(original.getPath()) && s.clipboard.getFile().isEmpty(), "ACE-17: colagem mantém UUID/Tags e limpa CUT");
        s.primaryFile = loaded;
        // Injeta erro SQL após o disco por uma trigger temporária restrita ao UUID próprio.
        String trigger = "demo_fail_" + loaded.getId().toString().replace("-", "");
        try (Statement statement = s.database.getConnection().createStatement()) {
            statement.execute("CREATE TRIGGER " + trigger + " BEFORE UPDATE ON LOCAL_FILE FOR EACH ROW BEGIN IF NEW.id='" + loaded.getId() + "' AND NEW.path<>OLD.path THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='falha controlada da demonstração'; END IF; END");
        }
        Path next = Files.createDirectory(s.root.resolve("falha-parcial"));
        try {
            expectFailure(s.localController.move(moved, new NativeDirectory(next)), OperationFailure.class);
            check(Files.exists(next.resolve(moved.getName())) && !Files.exists(moved.getPath()) && s.files.findById(loaded.getId()).orElseThrow().getNativeFile().getPath().equals(moved.getPath()), "ACE-22: disco moveu e SQL falhou, sem rollback fictício");
        } finally { try (Statement statement = s.database.getConnection().createStatement()) { statement.execute("DROP TRIGGER " + trigger); } }
        check(!s.gate.isBusy(), "Falha parcial libera controle somente ao terminar");
    }

    /**
     * Verifica a admissão comum das entradas dos dois Controllers.
     *
     * @param s sessão demonstrativa
     * @throws Exception se entradas ocupadas executarem/enfileirarem
     */
    private static void demoControllerGate(DemoSession s) throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1); int before = s.manager.getRefreshCount();
        CompletableFuture<Void> active = s.gate.submit(() -> { started.countDown(); if (!release.await(10, TimeUnit.SECONDS)) throw new IOException("Timeout"); return null; });
        check(started.await(10, TimeUnit.SECONDS), "Controle comum ocupado");
        expectFailure(s.tagController.refresh(), RejectedExecutionException.class); expectFailure(s.localController.navigate(new NativeDirectory(s.root)), RejectedExecutionException.class);
        expectFailure(s.tagController.associate(List.of(s.root.resolve("nativo.txt")), s.primaryTag.getId()), RejectedExecutionException.class);
        check(s.gate.isBusy(), "Chamadas rejeitadas não liberam a ação"); release.countDown(); active.get();
        check(s.manager.getRefreshCount() == before, "ACE-25: Controllers/automáticas rejeitados sem fila");
    }

    /**
     * Prepara explicitamente instância e schema, sem limpar cadastros.
     *
     * @param allowInstall autorização explícita da CLI
     * @throws Exception se instalação recusada ou alguma etapa falhar
     */
    private static void prepareEnvironment(boolean allowInstall) throws Exception {
        DatabaseManager manager = new DatabaseManager(Path.of(".")); manager.prepare(allowInstall);
        try (DatabaseConnection db = new DatabaseConnection(Path.of("database/config/database.properties"))) { db.open(); manager.prepareSchema(db); }
        System.out.println("AMBIENTE: instância exclusiva e schema conferidos");
    }

    /**
     * Exibe Panels reais; somente o modo de verificação fecha automaticamente a janela própria.
     *
     * @param automated executa verificações gráficas quando verdadeiro
     * @throws Exception se ambiente gráfico, banco ou UI falhar
     */
    private static void demoUi(boolean automated) throws Exception {
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("Ambiente gráfico necessário");
        SwingInteraction dialogs = new SwingInteraction(null); DatabaseManager environment = new DatabaseManager(Path.of("."));
        try { environment.prepare(false); }
        catch (IOException e) {
            if (!e.getMessage().contains("instalação não autorizada") || automated || dialogs.choose("Faltam componentes. Autorizar instalação portátil do MySQL no projeto?", "Autorizar", "Cancelar") != 0) throw e;
            environment.prepare(true);
        }
        DemoSession session = new DemoSession(dialogs);
        if (session.files.find(new LocalFileFilter()).isEmpty()) session.manager.initialize();
        Tag tag = session.newTag("Demonstração", Set.of("txt")); Path sample = session.file("classificado.txt"); session.file("sem-cadastro.txt");
        session.tagController.associate(List.of(sample), tag.getId()).get(); DemoWindow[] reference = new DemoWindow[1];
        ActionGate.onEdt(() -> reference[0] = new DemoWindow(session, environment));
        session.localController.refresh().get();
        if (automated) {
            try { verifyUi(session, reference[0], tag); }
            finally { session.gate.setBusyListener(busy -> { }); ActionGate.onEdt(reference[0]::dispose); session.close(); }
            System.out.println("DEMO-UI-CHECK: PASSOU");
        } else System.out.println("DEMO-UI: janela interativa aberta; arquivos temporários em " + session.root);
    }

    /**
     * Usa componentes Swing reais para verificar Observer, atalhos, Drop e diálogo modal.
     *
     * @param s sessão gráfica
     * @param window janela própria da demonstração
     * @param tag etiqueta de teste
     * @throws Exception se expectativa, captura ou evento falhar
     */
    private static void verifyUi(DemoSession s, DemoWindow window, Tag tag) throws Exception {
        ActionGate.onEdt(() -> {
            check(window.tags.getFileList().getModel().getSize() >= 1 && window.local.getFileList().getModel().getSize() == 2, "Panels recebem cadastro e nativo pelo Observer");
            window.tags.getTagList().setSelectedValue(tag, true); window.local.getFileList().setSelectedIndex(0);
        });
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1); AtomicInteger beats = new AtomicInteger();
        javax.swing.Timer heartbeat = new javax.swing.Timer(20, e -> beats.incrementAndGet()); ActionGate.onEdt(heartbeat::start);
        CompletableFuture<Void> work = s.gate.submit(() -> { started.countDown(); if (!release.await(10, TimeUnit.SECONDS)) throw new IOException("Timeout UI"); return null; });
        check(started.await(10, TimeUnit.SECONDS), "Ação gráfica em andamento");
        ActionGate.onEdt(() -> {
            findButton(window.local, "Recortar").doClick();
            window.local.getActionMap().get("cut").actionPerformed(new ActionEvent(window.local, ActionEvent.ACTION_PERFORMED, "cut"));
            var support = new TransferHandler.TransferSupport(window.tags.getTagList(), FileTransferHandler.transfer(List.of(s.root.resolve("sem-cadastro.txt"))));
            check(!window.tags.getTagList().getTransferHandler().importData(support), "ACE-25: Drop real rejeitado enquanto ocupado");
        });
        expectFailure(s.tagController.refresh(), RejectedExecutionException.class); expectFailure(s.localController.refresh(), RejectedExecutionException.class);
        Thread.sleep(150); check(beats.get() > 0 && s.clipboard.getFile().isEmpty(), "EDT responsiva; atalho não alterou clipboard");
        release.countDown(); work.get(); ActionGate.onEdt(heartbeat::stop);
        check(s.files.findByPath(s.root.resolve("sem-cadastro.txt")).isEmpty(), "Drop rejeitado não executou depois");
        javax.swing.Timer responder = new javax.swing.Timer(100, e -> {
            for (Window candidate : Window.getWindows()) if (candidate instanceof JDialog dialog && dialog.isVisible() && dialog.getTitle().equals("Tag-File")) dialog.dispose();
        });
        ActionGate.onEdt(responder::start);
        try {
            expectFailure(s.gate.submit(() -> { s.interaction.choose("Cancelar ação demonstrativa", "Cancelar"); throw new CancellationException("Diálogo cancelado"); }), CancellationException.class);
        } finally { ActionGate.onEdt(responder::stop); }
        check(!s.gate.isBusy(), "Diálogo utilizável; liberação após cancelamento real");
        verifyFailureDialog(s, window);
        ActionGate.onEdt(() -> {
            descendants(window.frame).stream().filter(c -> c instanceof JCheckBox box && box.getText().equals("Mostrar lado a lado"))
                    .map(c -> (JCheckBox) c).findFirst().orElseThrow().doClick();
        });
        awaitFree(s.gate);
        Rectangle[] bounds = new Rectangle[1]; ActionGate.onEdt(() -> { check(!window.loading.isVisible(), "Loading termina junto com a ação"); bounds[0] = window.frame.getBounds(); });
        Robot robot = new Robot(); robot.waitForIdle(); robot.delay(150);
        Files.createDirectories(Path.of("out")); ImageIO.write(robot.createScreenCapture(bounds[0]), "png", Path.of("out/ui-check.png").toFile());
    }

    /**
     * Provoca falha SQL real após mover e inspeciona o pop-up e sua expansão técnica.
     *
     * @param s sessão gráfica
     * @param window janela da demonstração
     * @throws Exception se mensagem, detalhes ou liberação não corresponderem ao resultado real
     */
    private static void verifyFailureDialog(DemoSession s, DemoWindow window) throws Exception {
        LocalFile file = s.files.findByPath(s.root.resolve("classificado.txt")).orElseThrow();
        String trigger = "demo_ui_" + file.getId().toString().replace("-", "");
        try (Statement statement = s.database.getConnection().createStatement()) {
            statement.execute("CREATE TRIGGER " + trigger + " BEFORE UPDATE ON LOCAL_FILE FOR EACH ROW BEGIN IF NEW.id='" + file.getId() + "' AND NEW.path<>OLD.path THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='falha grafica controlada'; END IF; END");
        }
        AtomicInteger verified = new AtomicInteger();
        javax.swing.Timer inspector = new javax.swing.Timer(100, e -> {
            for (Window candidate : Window.getWindows()) if (candidate instanceof JDialog dialog && dialog.isVisible() && dialog.getTitle().equals("Resultado da operação")) {
                List<Component> components = descendants(dialog); String messages = components.stream().filter(c -> c instanceof JTextArea).map(c -> ((JTextArea) c).getText()).reduce("", (a, b) -> a + "\n" + b);
                boolean busy = s.gate.isBusy();
                for (Component c : components) if (c instanceof JCheckBox checkBox && checkBox.getText().contains("detalhes")) checkBox.doClick();
                boolean expanded = components.stream().anyMatch(c -> c instanceof JScrollPane pane && pane.isVisible());
                if (messages.contains("Concluído:") && messages.contains("Falhou:") && messages.contains("45000") && busy && expanded) verified.incrementAndGet();
                dialog.dispose();
            }
        });
        ActionGate.onEdt(inspector::start);
        try {
            Path destination = Files.createDirectory(s.root.resolve("falha-ui"));
            expectFailure(s.localController.move(file.getNativeFile(), new NativeDirectory(destination)), OperationFailure.class);
            check(verified.get() == 1 && Files.exists(destination.resolve("classificado.txt")) && !s.gate.isBusy(), "ACE-22: pop-up real separa concluído/falhou, expande SQLState e encerra Loading");
        } finally {
            ActionGate.onEdt(inspector::stop);
            try (Statement statement = s.database.getConnection().createStatement()) { statement.execute("DROP TRIGGER " + trigger); }
        }
    }

    /**
     * Percorre componentes da própria janela para verificações gráficas.
     *
     * @param root componente inicial
     * @return componentes descendentes para verificar os diálogos da própria demo
     */
    private static List<Component> descendants(Container root) {
        List<Component> result = new ArrayList<>();
        for (Component child : root.getComponents()) { result.add(child); if (child instanceof Container container) result.addAll(descendants(container)); }
        return result;
    }
    /**
     * Localiza um botão pelo rótulo na apresentação testada.
     *
     * @param root painel
     * @param text rótulo exato
     * @return botão demonstrado
     * @throws NoSuchElementException se o botão não existir
     */
    private static JButton findButton(Container root, String text) {
        return descendants(root).stream().filter(c -> c instanceof JButton b && b.getText().equals(text)).map(c -> (JButton) c).findFirst().orElseThrow();
    }

    /**
     * Janela demonstrativa que compõe os dois Panels em abas ou lado a lado.
     */
    private static final class DemoWindow {
        /**
         * Janela pertencente à demonstração.
         */
        private final JFrame frame = new JFrame("Tag-File — arquivos temporários de demonstração");
        /**
         * Painel por Tags da demonstração.
         */
        private final TagExplorerPanel tags;
        /**
         * Painel físico da demonstração.
         */
        private final LocalFileExplorerPanel local;
        /**
         * Diálogo modeless que acompanha o trabalho real.
         */
        private final JDialog loading;
        /**
         * Monta a janela e os dois Panels na EDT, com Loading compartilhado.
         *
         * @param session sessão compartilhada
         * @param environment proprietário da instância; construtor executado na EDT
         */
        private DemoWindow(DemoSession session, DatabaseManager environment) {
            tags = new TagExplorerPanel(session.tagController, session.events, session.gate, new NativeDirectory(session.root));
            local = new LocalFileExplorerPanel(session.localController, session.events, session.gate, new NativeDirectory(session.root));
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); SwingInteraction dialogs = new SwingInteraction(frame);
            JPanel content = new JPanel(new BorderLayout()), views = new JPanel(new BorderLayout()), toolbar = new JPanel();
            JTabbedPane tabs = new JTabbedPane(); tabs.addTab("Arquivos por Tag", tags); tabs.addTab("Arquivos Local", local);
            tabs.addChangeListener(e -> dialogs.observe(session.localController.refresh())); views.add(tabs, BorderLayout.CENTER);
            JButton refresh = new JButton("Refresh"); refresh.addActionListener(e -> dialogs.observe(session.localController.refresh())); toolbar.add(refresh);
            JCheckBox sideBySide = new JCheckBox("Mostrar lado a lado"); toolbar.add(sideBySide);
            sideBySide.addActionListener(e -> {
                if (session.gate.isBusy()) { sideBySide.setSelected(!sideBySide.isSelected()); return; }
                session.gate.submit(() -> {
                    ActionGate.onEdt(() -> {
                        views.removeAll();
                        if (sideBySide.isSelected()) { JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tags, local); split.setResizeWeight(.5); views.add(split, BorderLayout.CENTER); }
                        else { tabs.removeAll(); tabs.addTab("Arquivos por Tag", tags); tabs.addTab("Arquivos Local", local); views.add(tabs, BorderLayout.CENTER); }
                        views.revalidate(); views.repaint();
                    }); return null;
                });
            });
            content.add(views, BorderLayout.CENTER); content.add(toolbar, BorderLayout.NORTH); frame.setContentPane(content);
            loading = new JDialog(frame, "Loading", false); loading.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            JProgressBar progress = new JProgressBar(); progress.setIndeterminate(true); loading.add(progress); loading.setSize(260, 75);
            session.gate.setBusyListener(busy -> { loading.setLocationRelativeTo(frame); loading.setVisible(busy); refresh.setEnabled(!busy); });
            frame.addWindowListener(new WindowAdapter() {
                /**
                 * Solicita encerramento respeitando a ação ainda em andamento.
                 *
                 * @param event fechamento solicitado; não encerra recursos enquanto existe trabalho em andamento
                 */
                @Override public void windowClosing(WindowEvent event) {
                    if (session.gate.isBusy()) return;
                    session.gate.submit(() -> { ActionGate.onEdt(DemoWindow.this::dispose); session.close(); environment.stop(); return null; })
                            .exceptionally(error -> { dialogs.showFailure(error); return null; });
                }
            });
            frame.setSize(1250, 680); frame.setLocationRelativeTo(null); frame.setVisible(true);
        }
        /**
         * Remove observadores e fecha apenas as janelas desta demonstração; chamar na EDT.
         */
        private void dispose() { tags.dispose(); local.dispose(); loading.dispose(); frame.dispose(); }
    }

    /**
     * Confere uma previsão e registra evidência somente se ela for satisfeita.
     *
     * @param condition condição observada
     * @param description cenário
     * @throws IllegalStateException se falhar
     */
    private static void check(boolean condition, String description) { if (!condition) throw new IllegalStateException("FALHOU: " + description); System.out.println("PASSOU: " + description); }
    /**
     * Confere o tipo da causa excepcional, sem usar assert.
     *
     * @param future ação
     * @param expected causa prevista
     * @throws Exception se sucesso indevido, timeout ou outra causa
     */
    private static void expectFailure(CompletableFuture<?> future, Class<? extends Throwable> expected) throws Exception {
        try { future.get(10, TimeUnit.SECONDS); throw new IllegalStateException("Esperada falha: " + expected.getSimpleName()); }
        catch (ExecutionException e) { if (!expected.isInstance(e.getCause())) throw e; }
        catch (CancellationException e) { if (!expected.isInstance(e)) throw e; }
    }
    /**
     * Aguarda com prazo o término real do trabalho cujo futuro foi cancelado.
     *
     * @param gate controle de tarefa cujo futuro pode estar cancelado
     * @throws Exception se não terminar em dez segundos
     */
    private static void awaitFree(ActionGate gate) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10); while (gate.isBusy() && System.nanoTime() < end) Thread.sleep(5);
        check(!gate.isBusy(), "Liberação após término real");
    }
    /**
     * Remove apenas a árvore temporária de propriedade da demonstração.
     *
     * @param root pasta retornada por createTempDirectory
     * @throws IOException se limpeza dos recursos próprios falhar
     */
    private static void deleteTemporaryTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
    }

    /**
     * Roteiro de diálogos demonstrativos; persistência continua sendo MySQL real.
     */
    private static final class DemoInteraction implements Interaction {
        /**
         * Respostas explícitas previstas para os diálogos da demonstração.
         */
        private final Deque<Integer> choices = new ArrayDeque<>();
        /**
         * Entradas de texto previstas para a demonstração.
         */
        private final Deque<String> texts = new ArrayDeque<>();
        /**
         * Arquivos confirmados no seletor simulado.
         */
        private List<Path> selected = List.of();
        /**
         * Alternativas efetivamente apresentadas pelo último diálogo.
         */
        private List<String> lastOptions = List.of();
        /**
         * Roteiro inicialmente vazio: qualquer diálogo não previsto faz o teste falhar.
         */
        private DemoInteraction() { }
        /**
         * Consome uma decisão previamente especificada pelo cenário.
         *
         * @param message consequências
         * @param options alternativas
         * @return resposta prevista
         * @throws IllegalStateException se diálogo inesperado
         */
        @Override public int choose(String message, String... options) { lastOptions = List.of(options); if (choices.isEmpty()) throw new IllegalStateException("Diálogo não previsto: " + message); return choices.removeFirst(); }
        /**
         * Consome uma entrada de texto previamente especificada pelo cenário.
         *
         * @param message finalidade
         * @param initial padrão
         * @return texto previsto
         * @throws IllegalStateException se entrada inesperada
         */
        @Override public String text(String message, String initial) { if (texts.isEmpty()) throw new IllegalStateException("Entrada não prevista: " + message); return texts.removeFirst(); }
        /**
         * Fornece a seleção explícita do cenário, sem varrer pastas.
         *
         * @param multiple seleção múltipla
         * @param extensions filtro
         * @return seleção confirmada do roteiro
         */
        @Override public List<Path> files(boolean multiple, Set<String> extensions) { return List.copyOf(selected); }
    }

    /**
     * Monta colaboradores reais e possui os recursos temporários de uma demonstração.
     */
    private static final class DemoSession implements AutoCloseable {
        /**
         * Pasta temporária de propriedade exclusiva desta demonstração.
         */
        private final Path root;
        /**
         * Proprietário da conexão JDBC compartilhada.
         */
        private final DatabaseConnection database;
        /**
         * Serviço que lê e opera o disco, sem SQL.
         */
        private final NativeFileService nativeService = new NativeFileService();
        /**
         * Fábrica simples para entidades novas.
         */
        private final EntityFactory factory = new EntityFactory(nativeService);
        /**
         * DAO de etiquetas e suas extensões.
         */
        private final TagDAO tags;
        /**
         * DAO de associações na mesma conexão.
         */
        private final LocalFileTagDAO links;
        /**
         * DAO de cadastros compartilhando a conexão da sessão.
         */
        private final LocalFileDAO files;
        /**
         * Coordenação das regras e das etapas de disco/SQL.
         */
        private final LocalFileManager manager;
        /**
         * Intenção COPY/CUT compartilhada pelas duas visões.
         */
        private final ClipboardService clipboard = new ClipboardService();
        /**
         * Serviço compartilhado de inscrições e avisos.
         */
        private final ExplorerEventService events = new ExplorerEventService();
        /**
         * Controle global de admissão sem fila.
         */
        private final ActionGate gate = new ActionGate();
        /**
         * Fronteira para obter decisões sem conhecer componentes Swing.
         */
        private final Interaction interaction;
        /**
         * Entrada funcional da visão por Tags.
         */
        private final TagExplorerController tagController;
        /**
         * Entrada funcional da visão física.
         */
        private final LocalFileExplorerController localController;
        /**
         * UUIDs criados pela demonstração e autorizados para sua limpeza.
         */
        private final Set<UUID> ownedTags = new HashSet<>();
        /**
         * Tag usada no cenário entre exploradores.
         */
        private Tag primaryTag;
        /**
         * Cadastro usado para verificar identidade no movimento.
         */
        private LocalFile primaryFile;
        /**
         * Abre JDBC e compõe os colaboradores reais usados pela demonstração.
         *
         * @param interaction diálogos Swing ou roteiro previsto
         * @throws Exception se ambiente ou montagem falhar
         */
        private DemoSession(Interaction interaction) throws Exception {
            this.interaction = interaction; database = new DatabaseConnection(Path.of("database/config/database.properties")); database.open();
            try { new DatabaseManager(Path.of(".")).prepareSchema(database); } catch (Exception e) { database.close(); throw e; }
            root = Files.createTempDirectory("tag-file-db-"); tags = new TagDAO(database); links = new LocalFileTagDAO(database, tags); files = new LocalFileDAO(database, links);
            manager = new LocalFileManager(nativeService, factory, files, tags, links);
            ExplorerContext context = new ExplorerContext(manager, nativeService, files, tags, clipboard, interaction, events, gate, new NativeDirectory(root),
                    error -> { if (interaction instanceof SwingInteraction swing) swing.showFailure(error); else System.out.println("Falha demonstrada: " + error.getMessage()); });
            tagController = new TagExplorerController(context); localController = new LocalFileExplorerController(context);
            check(context.getGate() == gate, "Controllers compartilham controle, clipboard e colaboradores");
        }
        /**
         * Cria conteúdo de teste dentro da pasta temporária própria.
         *
         * @param name nome dentro da pasta própria
         * @return caminho criado
         * @throws IOException se escrita falhar
         */
        private Path file(String name) throws IOException { return Files.writeString(root.resolve(name), "conteúdo temporário " + name); }
        /**
         * Persiste uma Tag e registra seu UUID para a limpeza da demonstração.
         *
         * @param name nome demonstrativo
         * @param extensions restrições
         * @return Tag rastreada para limpeza
         * @throws SQLException se SQL falhar
         */
        private Tag newTag(String name, Set<String> extensions) throws SQLException {
            Tag tag = factory.createTag("demo-" + root.getFileName() + "-" + name, "#2864B4", extensions); ownedTags.add(tag.getId()); tags.create(tag); return tag;
        }
        /**
         * Remove recursos próprios e fecha a conexão mesmo quando a limpeza falhar.
         *
         * @throws Exception se limpeza ou fechamento falhar; não esvazia tabelas nem recursos externos
         */
        @Override public void close() throws Exception {
            try {
                database.open(); for (LocalFile f : files.find(new LocalFileFilter())) if (f.getNativeFile().getPath().startsWith(root)) files.delete(f.getId());
                for (UUID id : ownedTags) tags.delete(id);
            } finally { try { database.close(); } finally { deleteTemporaryTree(root); } }
        }
    }
}
