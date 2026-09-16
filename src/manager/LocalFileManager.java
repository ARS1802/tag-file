/*
 * Inventário — LocalFileManager.
 * Coordena regras, disco e SQL; não administra servidor nem componentes Swing (ARQ-07).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - LocalFileManager.LocalFileManager(NativeFileService nativeService, EntityFactory factory, LocalFileDAO files, TagDAO tags, LocalFileTagDAO links): Recebe colaboradores da mesma sessão, usados sob o controle global de ações.
 * - LocalFileManager.initialize(): Limpa uma única vez por sessão e sincroniza os restantes; reconexão não chama isto.
 * - LocalFileManager.refreshAll(): Sincroniza cada cadastro exatamente uma vez, sem limpeza de órfãos/sentinela.
 * - LocalFileManager.getRefreshCount(): Informa quantidade de passagens globais, para verificar SYN-03.
 * - LocalFileManager.refresh(LocalFile file, boolean required): Atualiza somente se metadados mudaram, preservando UUID e relações.
 * - LocalFileManager.removeTag(UUID file, UUID tag): Reorganiza uma associação; última Tag normal é substituída pela sentinela.
 * - LocalFileManager.associate(List<Path> selected, UUID tagId, Interaction interaction): Reúne a seleção confirmada sem reaplicar filtros; para na primeira falha/cancelamento.
 * - LocalFileManager.associateOne(Path path, UUID tagId, Interaction interaction): Valida extensão antes de cadastrar; oferece as três alternativas de EXT-02.
 * - LocalFileManager.chooseSuggestions(NativeFile file, Interaction interaction): Oferece todas as predefinidas compatíveis; nenhuma é aplicada sem escolha.
 * - LocalFileManager.createTag(Interaction interaction): Coleta uma Tag e confirma nomes duplicados antes de persistir.
 * - LocalFileManager.parseExtensions(String text): Interpreta uma lista de extensões digitada em formulário.
 * - LocalFileManager.confirmName(Tag tag, Interaction interaction): Valida dados e confirma nome repetido, comparando sem caixa/espaços externos.
 * - LocalFileManager.editTag(UUID id, Interaction interaction): Edita etiqueta considerando o conjunto final de restrições, com lista de afetados.
 * - LocalFileManager.resolveAvailable(NativeFile nativeFile, Interaction interaction): Resolve indisponibilidade com localização confirmada, remoção explícita ou cancelamento.
 * - LocalFileManager.transfer(NativeFile source, Path proposed, boolean copy, Interaction interaction): Move ou copia após confirmar herança, conflitos e possíveis classificações perdidas.
 * - LocalFileManager.resolveConflict(NativeFile source, Path proposed, boolean copy, Interaction interaction): Resolve conflito físico ou somente SQL; manter ambos busca nome livre nos dois meios.
 * - LocalFileManager.rename(NativeFile source, String name, Interaction interaction): Renomeia sem converter conteúdo, oferecendo as três alternativas de OP-05.
 * - LocalFileManager.paste(ClipboardService clipboard, NativeDirectory directory, Interaction interaction): Cola a intenção comum; COPY permanece, CUT é limpo somente após sucesso completo.
 * - LocalFileManager.deleteTag(UUID id, Interaction interaction): Exclui uma Tag segundo modalidade explicitamente escolhida e confirmação dos alvos.
 * - LocalFileManager.deleteFile(NativeFile source, Interaction interaction): Exclui permanentemente um arquivo selecionado com confirmação de suas Tags.
 *
 * Consulte: doc/requisitos-e-regras.md — CIC-01 a CIC-03, OP-01 a OP-09, DEL-01 a DEL-04.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package manager;

import filter.*;
import model.*;
import persistence.*;
import service.*;
import ui.Interaction;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CancellationException;

/**
 * Coordena regras, disco e SQL; não administra servidor nem componentes Swing (ARQ-07).
 */
public final class LocalFileManager {
    /**
     * Serviço que lê e opera o disco, sem SQL.
     */
    private final NativeFileService nativeService;
    /**
     * Fábrica simples para entidades novas.
     */
    private final EntityFactory factory;
    /**
     * DAO de cadastros compartilhando a conexão da sessão.
     */
    private final LocalFileDAO files;
    /**
     * DAO de etiquetas e suas extensões.
     */
    private final TagDAO tags;
    /**
     * DAO de associações na mesma conexão.
     */
    private final LocalFileTagDAO links;
    /**
     * Impede repetir a limpeza de inicialização na sessão.
     */
    private boolean initialized;
    /**
     * Preferência temporária; silencia sugestões sem classificar automaticamente.
     */
    private boolean suggestionsMuted;
    /**
     * Número de sincronizações globais iniciadas na sessão.
     */
    private int refreshCount;

    /**
     * Recebe colaboradores da mesma sessão, usados sob o controle global de ações.
     *
     * @param nativeService operações físicas
     * @param factory criação de entidades
     * @param files persistência de cadastros
     * @param tags persistência de etiquetas
     * @param links persistência de relações
     */
    public LocalFileManager(NativeFileService nativeService, EntityFactory factory, LocalFileDAO files, TagDAO tags, LocalFileTagDAO links) {
        this.nativeService = nativeService; this.factory = factory; this.files = files; this.tags = tags; this.links = links;
    }

    /**
     * Limpa uma única vez por sessão e sincroniza os restantes; reconexão não chama isto.
     *
     * @return número de cadastros removidos, preservando disco e sentinela
     * @throws SQLException se limpeza ou sincronização SQL falhar
     * @throws IOException se metadados não puderem ser consultados
     * @throws IllegalStateException se inicialização já foi tentada nesta sessão
     */
    public int initialize() throws SQLException, IOException {
        if (initialized) throw new IllegalStateException("Inicialização já executada nesta sessão");
        initialized = true;
        int removed = files.cleanupUnclassified(); refreshAll(); return removed;
    }

    /**
     * Sincroniza cada cadastro exatamente uma vez, sem limpeza de órfãos/sentinela.
     *
     * @throws SQLException se uma escrita falhar; anteriores permanecem
     * @throws IOException se leitura física falhar
     */
    public void refreshAll() throws SQLException, IOException {
        refreshCount++;
        for (LocalFile file : files.find(new LocalFileFilter())) refresh(file, false);
    }

    /**
     * Informa quantidade de passagens globais, para verificar SYN-03.
     *
     * @return quantidade de passagens globais, para verificar SYN-03
     */
    public int getRefreshCount() { return refreshCount; }

    /**
     * Atualiza somente se metadados mudaram, preservando UUID e relações.
     *
     * @param file fotografia anterior
     * @param required exige existência física para uso
     * @return fotografia atualizada
     * @throws IOException se arquivo exigido estiver ausente ou leitura falhar
     * @throws SQLException se persistência falhar
     */
    public LocalFile refresh(LocalFile file, boolean required) throws IOException, SQLException {
        LocalFile current = nativeService.readMetadata(file, required);
        if (current.isAvailable() != file.isAvailable() || !Objects.equals(current.getSize(), file.getSize())
                || !Objects.equals(current.getCreatedAt(), file.getCreatedAt()) || !Objects.equals(current.getModifiedAt(), file.getModifiedAt())
                || !Objects.equals(current.getLastAccessedAt(), file.getLastAccessedAt())) files.update(current);
        return current;
    }

    /**
     * Reorganiza uma associação; última Tag normal é substituída pela sentinela.
     *
     * @param file identidade do cadastro
     * @param tag etiqueta normal a retirar
     * @throws SQLException se etapas SQL falharem, possivelmente com efeitos parciais
     * @throws IllegalArgumentException se tentar retirar diretamente a sentinela
     */
    public void removeTag(UUID file, UUID tag) throws SQLException {
        if (Tag.MISSING_ID.equals(tag)) throw new IllegalArgumentException("A sentinela é removida ao adicionar uma Tag normal");
        LocalFile current = files.findById(file).orElseThrow(() -> new IllegalArgumentException("Cadastro não encontrado"));
        if (current.getTags().stream().noneMatch(t -> !t.isMissing() && !t.getId().equals(tag))) links.associate(file, Tag.MISSING_ID);
        links.dissociate(file, tag);
    }

    /**
     * Reúne a seleção confirmada sem reaplicar filtros; para na primeira falha/cancelamento.
     *
     * @param selected caminhos confirmados, copiados pelo Controller
     * @param tagId Tag identificada pelo UUID
     * @param interaction fronteira de confirmações da UI
     * @return cadastros resultantes na ordem da seleção
     * @throws Exception se validação, disco, SQL ou decisão falhar; anteriores são informados
     */
    public List<LocalFile> associate(List<Path> selected, UUID tagId, Interaction interaction) throws Exception {
        List<LocalFile> done = new ArrayList<>();
        try {
            for (Path path : selected) done.add(associateOne(path, tagId, interaction));
            return List.copyOf(done);
        } catch (Exception e) {
            if (done.isEmpty()) throw e;
            throw new OperationFailure("Associações concluídas para " + done.stream().map(f -> f.getNativeFile().toString()).toList(), "Restante da seleção interrompido", e);
        }
    }

    /**
     * Valida extensão antes de cadastrar; oferece as três alternativas de EXT-02.
     *
     * @param path arquivo confirmado
     * @param tagId identidade da etiqueta
     * @param interaction respostas do usuário
     * @return cadastro relido com relações persistidas
     * @throws Exception se cancelado, indisponível ou escrita falhar
     */
    private LocalFile associateOne(Path path, UUID tagId, Interaction interaction) throws Exception {
        NativeFile nativeFile = new NativeFile(path); nativeService.requireAvailable(nativeFile);
        Tag tag = tags.findById(tagId).orElseThrow(() -> new IllegalArgumentException("Tag não encontrada"));
        if (tag.isMissing()) throw new IllegalArgumentException("Etiqueta Ausente é aplicada pelo ciclo de organização");
        if (!tag.accepts(nativeFile)) {
            int decision = interaction.choose("A Tag " + tag + " não aceita " + nativeFile.getExtension(), "Criar nova etiqueta", "Adicionar extensão à etiqueta", "Cancelar");
            if (decision == 0) tag = createTag(interaction);
            else if (decision == 1) {
                Set<String> extensions = new HashSet<>(tag.getExtensions()); extensions.add(nativeFile.getExtension());
                tag = new Tag(tag.getId(), tag.getName(), tag.getColor(), extensions, tag.getCreatedAt(), tag.getLastFileTaggedAt(), tag.isPredefined()); tags.update(tag);
            } else throw new CancellationException("Associação cancelada antes de cadastrar");
            if (!tag.accepts(nativeFile)) throw new IllegalArgumentException("A nova Tag também é incompatível com " + nativeFile);
        }
        Optional<LocalFile> known = files.findByPath(path);
        LocalFile file = known.isPresent() ? refresh(known.get(), true) : factory.createLocalFile(path);
        List<Tag> suggested = known.isEmpty() ? chooseSuggestions(nativeFile, interaction) : List.of();
        boolean created = false;
        try {
            if (known.isEmpty()) { files.create(file); created = true; }
            links.associate(file.getId(), tag.getId());
            links.dissociate(file.getId(), Tag.MISSING_ID);
            for (Tag suggestion : suggested) links.associate(file.getId(), suggestion.getId());
            return files.findById(file.getId()).orElseThrow();
        } catch (SQLException e) {
            throw new OperationFailure(created ? "Cadastro criado: " + file.getId() : "Cadastro existente localizado: " + file.getId(), "Persistência das associações; podem existir vínculos parciais", e);
        }
    }

    /**
     * Oferece todas as predefinidas compatíveis; nenhuma é aplicada sem escolha.
     *
     * @param file arquivo de um cadastro novo
     * @param interaction diálogo de sugestão
     * @return zero ou uma Tag adicional escolhida
     * @throws SQLException se consulta de predefinidas falhar
     */
    private List<Tag> chooseSuggestions(NativeFile file, Interaction interaction) throws SQLException {
        if (suggestionsMuted) return List.of();
        List<Tag> compatible = tags.find(new TagFilter()).stream().filter(t -> t.isPredefined() && !t.isMissing() && t.accepts(file)).toList();
        if (compatible.isEmpty()) return List.of();
        List<String> choices = new ArrayList<>(compatible.stream().map(Tag::getName).toList());
        choices.add("Não adicionar"); choices.add("Não sugerir novamente nesta sessão (sem classificação automática)");
        int decision = interaction.choose("Adicionar também uma etiqueta predefinida a " + file.getName() + "?", choices.toArray(String[]::new));
        if (decision == compatible.size() + 1) suggestionsMuted = true;
        return decision >= 0 && decision < compatible.size() ? List.of(compatible.get(decision)) : List.of();
    }

    /**
     * Coleta uma Tag e confirma nomes duplicados antes de persistir.
     *
     * @param interaction interface de entrada; cancelamento não grava a Tag
     * @return entidade criada
     * @throws Exception se cancelar, validação ou persistência falhar
     */
    public Tag createTag(Interaction interaction) throws Exception {
        String name = interaction.text("Nome da etiqueta", "");
        if (name == null) throw new CancellationException("Criação cancelada");
        String color = interaction.text("Cor hexadecimal #RRGGBB", "#2864B4");
        if (color == null) throw new CancellationException("Criação cancelada");
        String extensions = interaction.text("Extensões separadas por vírgula; vazio aceita todas; <sem> aceita arquivo sem extensão", "");
        if (extensions == null) throw new CancellationException("Criação cancelada");
        Tag tag = factory.createTag(name.strip(), color.toUpperCase(Locale.ROOT), parseExtensions(extensions));
        confirmName(tag, interaction);
        tags.create(tag); return tag;
    }

    /**
     * Interpreta uma lista de extensões digitada em formulário.
     *
     * @param text texto separado por vírgula, vazio libera todas
     * @return conjunto normalizado; marcador {@code <sem>} vira extensão vazia
     * @throws IllegalArgumentException se extensão inválida
     */
    public static Set<String> parseExtensions(String text) {
        return text.isBlank() ? Set.of() : Tag.normalizeExtensions(Arrays.stream(text.split(",", -1)).map(String::strip).map(e -> e.equals("<sem>") ? "" : e).toList());
    }

    /**
     * Valida dados e confirma nome repetido, comparando sem caixa/espaços externos.
     *
     * @param tag proposta de criação/edição
     * @param interaction confirmação de duplicidade
     * @throws SQLException se consulta falhar
     * @throws IllegalArgumentException se nome/cor inválidos
     * @throws CancellationException se duplicidade não for confirmada
     */
    private void confirmName(Tag tag, Interaction interaction) throws SQLException {
        if (tag.getName().isBlank() || tag.getName().length() > 100 || !tag.getColor().matches("#[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Use nome de 1 a 100 caracteres e cor #RRGGBB");
        boolean duplicate = tags.find(new TagFilter()).stream().anyMatch(t -> !t.equals(tag) && t.getName().strip().equalsIgnoreCase(tag.getName().strip()));
        if (duplicate && interaction.choose("Já existe uma etiqueta chamada " + tag.getName() + ". Criar/manter outra identidade?", "Confirmar", "Cancelar") != 0) throw new CancellationException("Nome repetido não confirmado");
    }

    /**
     * Edita etiqueta considerando o conjunto final de restrições, com lista de afetados.
     *
     * @param id etiqueta escolhida
     * @param interaction dados e confirmações
     * @return etiqueta atualizada
     * @throws Exception se cancelar ou disco/SQL/validação falhar, sem reversão automática
     */
    public Tag editTag(UUID id, Interaction interaction) throws Exception {
        Tag old = tags.findById(id).orElseThrow();
        if (old.isMissing()) throw new IllegalArgumentException("Etiqueta Ausente é protegida contra edição");
        String name = interaction.text("Nome", old.getName());
        if (name == null) throw new CancellationException();
        String color = interaction.text("Cor #RRGGBB", old.getColor());
        if (color == null) throw new CancellationException();
        String ext = interaction.text("Extensões; vazio aceita todas; <sem> representa ausência", String.join(",", old.getExtensions().stream().map(e -> e.isEmpty() ? "<sem>" : e).toList()));
        if (ext == null) throw new CancellationException();
        Tag edited = new Tag(id, name.strip(), color.toUpperCase(Locale.ROOT), parseExtensions(ext), old.getCreatedAt(), old.getLastFileTaggedAt(), old.isPredefined());
        confirmName(edited, interaction);
        List<LocalFile> incompatible = files.find(new LocalFileFilter(Set.of(id), true, new NativeFileFilter())).stream().filter(f -> !edited.accepts(f.getNativeFile())).toList();
        if (!incompatible.isEmpty() && interaction.choose("Estes arquivos perderão esta etiqueta (disco e demais etiquetas preservados):\n" + incompatible, "Confirmar", "Cancelar") != 0) throw new CancellationException();
        List<UUID> removed = new ArrayList<>();
        try {
            for (LocalFile f : incompatible) { removeTag(f.getId(), id); removed.add(f.getId()); }
            tags.update(edited); return edited;
        } catch (SQLException e) { throw new OperationFailure("Associações retiradas: " + removed, "Finalizar edição da Tag; dados podem estar parciais", e); }
    }

    /**
     * Resolve indisponibilidade com localização confirmada, remoção explícita ou cancelamento.
     *
     * @param nativeFile caminho usado pela solicitação
     * @param interaction escolhas do usuário
     * @return referência disponível; relocalização preserva UUID/Tags
     * @throws Exception se cancelar, colisão, disco ou SQL falhar
     */
    public NativeFile resolveAvailable(NativeFile nativeFile, Interaction interaction) throws Exception {
        Optional<LocalFile> registered = files.findByPath(nativeFile.getPath());
        if (registered.isEmpty()) { nativeService.requireAvailable(nativeFile); return nativeFile; }
        LocalFile file = refresh(registered.get(), false);
        if (file.isAvailable()) return nativeFile;
        int choice = interaction.choose("Arquivo indisponível: " + nativeFile, "Localizar", "Remover do Tag-File", "Cancelar");
        if (choice == 0) {
            List<Path> selected = interaction.files(false, Set.of());
            if (selected.isEmpty()) throw new CancellationException();
            Path target = new NativeFile(selected.getFirst()).getPath();
            if (files.findByPath(target).filter(f -> !f.equals(file)).isPresent()) throw new IOException("Caminho já cadastrado; relocalização não mescla identidades");
            NativeFile found = new NativeFile(target); nativeService.requireAvailable(found);
            if (file.getTags().stream().anyMatch(t -> !t.accepts(found))) throw new IOException("Extensão incompatível; ajuste as etiquetas antes de relocalizar");
            LocalFile relocated = new LocalFile(file.getId(), found, false, file.getSize(), file.getCreatedAt(), file.getModifiedAt(), file.getLastAccessedAt(), file.getTags());
            files.update(nativeService.readMetadata(relocated, true)); return found;
        }
        if (choice == 1) {
            if (interaction.choose("Retirar imediatamente o cadastro e TODOS os seus vínculos? O disco será preservado.\n" + file, "Remover cadastro", "Cancelar") == 0) files.delete(file.getId());
        }
        throw new CancellationException("Uso do arquivo cancelado");
    }

    /**
     * Move ou copia após confirmar herança, conflitos e possíveis classificações perdidas.
     *
     * @param source referência original, resolvida antes do uso
     * @param proposed destino proposto em pasta existente
     * @param copy verdadeiro copia; falso move preservando UUID/Tags
     * @param interaction confirmações da operação admitida
     * @return referência física final, inclusive nome escolhido para manter ambos
     * @throws Exception se cancelar, disco/SQL falhar; falhas após disco descrevem efeito parcial
     */
    public NativeFile transfer(NativeFile source, Path proposed, boolean copy, Interaction interaction) throws Exception {
        source = resolveAvailable(source, interaction);
        Optional<LocalFile> registered = files.findByPath(source.getPath());
        boolean inherit = false;
        if (copy) {
            int answer = interaction.choose("A cópia deve receber as etiquetas do original? Sem herança, ficará apenas no disco.", "Herdar etiquetas", "Copiar sem etiquetas", "Cancelar");
            if (answer < 0 || answer == 2) throw new CancellationException("Cópia cancelada");
            inherit = answer == 0;
        }
        Map.Entry<Path, Boolean> plan = resolveConflict(source, proposed, copy, interaction);
        Path target = plan.getKey();
        Optional<LocalFile> destination = files.findByPath(target);
        List<Tag> suggestions = copy && inherit && registered.isPresent() && !registered.get().getTags().isEmpty()
                ? chooseSuggestions(new NativeFile(target), interaction) : List.of();
        NativeFile result = copy ? nativeService.copy(source, target, plan.getValue()) : nativeService.move(source, target, plan.getValue());
        try {
            if (destination.isPresent() && (registered.isEmpty() || !destination.get().equals(registered.get()))) files.delete(destination.get().getId());
            if (copy) {
                if (inherit && registered.isPresent() && !registered.get().getTags().isEmpty()) {
                    LocalFile duplicate = factory.createLocalFile(target); files.create(duplicate);
                    Set<Tag> inherited = new HashSet<>(registered.get().getTags()); inherited.addAll(suggestions);
                    if (inherited.stream().anyMatch(t -> !t.isMissing())) inherited.removeIf(Tag::isMissing);
                    for (Tag tag : inherited) links.associate(duplicate.getId(), tag.getId());
                }
            } else if (registered.isPresent()) {
                LocalFile old = registered.get();
                LocalFile moved = new LocalFile(old.getId(), result, false, old.getSize(), old.getCreatedAt(), old.getModifiedAt(), old.getLastAccessedAt(), old.getTags());
                files.update(nativeService.readMetadata(moved, true));
            }
            return result;
        } catch (Exception e) {
            throw new OperationFailure((copy ? "Arquivo copiado para " : "Arquivo movido para ") + target + "; " + (copy ? "origem preservada" : "origem removida"), "Sincronizar cadastros e vínculos no MySQL; etapas SQL anteriores podem ter concluído", e);
        }
    }

    /**
     * Resolve conflito físico ou somente SQL; manter ambos busca nome livre nos dois meios.
     *
     * @param source origem para impedir substituir o próprio arquivo
     * @param proposed destino proposto
     * @param copy permite copiar na mesma pasta usando outro nome
     * @param interaction escolha explícita antes de sobrescrever
     * @return par de caminho e autorização explícita de substituição, sem realizar modificações
     * @throws Exception se cancelado, mesmo destino de movimento ou consulta falhar
     */
    private Map.Entry<Path, Boolean> resolveConflict(NativeFile source, Path proposed, boolean copy, Interaction interaction) throws Exception {
        Path target = new NativeFile(proposed).getPath();
        if (target.equals(source.getPath()) && !copy) throw new CancellationException("Origem e destino iguais; nada movido");
        Optional<LocalFile> destination = files.findByPath(target);
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS) && destination.isEmpty()) return Map.entry(target, false);
        boolean same = target.equals(source.getPath()) || Files.exists(target) && Files.isSameFile(source.getPath(), target);
        int answer = interaction.choose("Destino já existe: " + target + "\n" + destination.map(f -> "Classificação do destino: " + f.getTags()).orElse("")
                + "\nSubstituir descarta o conteúdo e o cadastro anteriores do destino.", same ? "Manter os dois" : "Substituir", same ? "Cancelar" : "Manter os dois", "Cancelar");
        if (answer < 0 || answer == 2 || same && answer != 0) throw new CancellationException("Conflito cancelado");
        if (!same && answer == 0) return Map.entry(target, true);
        NativeFile named = new NativeFile(target); String extension = named.getExtension();
        String base = named.getName().substring(0, named.getName().length() - extension.length());
        for (int suffix = 1; suffix < Integer.MAX_VALUE; suffix++) {
            Path candidate = target.resolveSibling(base + " (" + suffix + ")" + extension);
            if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS) && files.findByPath(candidate).isEmpty()) return Map.entry(candidate, false);
        }
        throw new IOException("Não foi encontrado nome livre");
    }

    /**
     * Renomeia sem converter conteúdo, oferecendo as três alternativas de OP-05.
     *
     * @param source arquivo escolhido
     * @param name novo nome simples, sem componentes de pasta
     * @param interaction confirmações de compatibilidade e conflito
     * @return referência física final
     * @throws Exception se cancelar, nome inválido ou disco/SQL falhar
     */
    public NativeFile rename(NativeFile source, String name, Interaction interaction) throws Exception {
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\")) throw new IllegalArgumentException("Informe apenas o novo nome do arquivo");
        source = resolveAvailable(source, interaction);
        Optional<LocalFile> registered = files.findByPath(source.getPath());
        NativeFile proposed = new NativeFile(source.getPath().resolveSibling(name));
        List<Tag> incompatible = registered.map(f -> f.getTags().stream().filter(t -> !t.accepts(proposed)).toList()).orElse(List.of());
        int decision = -1;
        if (!incompatible.isEmpty()) {
            decision = interaction.choose("Renomear não converte o conteúdo. Etiquetas incompatíveis: " + incompatible, "Remover etiquetas incompatíveis", "Adicionar nova extensão às etiquetas", "Cancelar");
            if (decision < 0 || decision == 2) throw new CancellationException("Renomeação cancelada");
        }
        NativeFile result = transfer(source, proposed.getPath(), false, interaction);
        try {
            if (registered.isPresent()) for (Tag tag : incompatible) {
                if (decision == 0) removeTag(registered.get().getId(), tag.getId());
                else {
                    Set<String> allowed = new HashSet<>(tag.getExtensions()); allowed.add(result.getExtension());
                    tags.update(new Tag(tag.getId(), tag.getName(), tag.getColor(), allowed, tag.getCreatedAt(), tag.getLastFileTaggedAt(), tag.isPredefined()));
                }
            }
            return result;
        } catch (Exception e) { throw new OperationFailure("Arquivo renomeado e caminho cadastrado: " + result, "Adequar associações/extensões das Tags", e); }
    }

    /**
     * Cola a intenção comum; COPY permanece, CUT é limpo somente após sucesso completo.
     *
     * @param clipboard intenção compartilhada
     * @param directory destino selecionado
     * @param interaction confirmações aplicáveis
     * @return referência final da cópia ou movimento
     * @throws Exception se clipboard vazio, cancelamento ou falha; não repete automaticamente
     */
    public NativeFile paste(ClipboardService clipboard, NativeDirectory directory, Interaction interaction) throws Exception {
        NativeFile source = clipboard.getFile().orElseThrow(() -> new IllegalStateException("Clipboard vazio"));
        if (clipboard.getLocalId().isPresent()) source = files.findById(clipboard.getLocalId().get()).map(LocalFile::getNativeFile).orElse(source);
        NativeFile result = transfer(source, directory.getPath().resolve(source.getName()), !clipboard.isCut(), interaction);
        if (clipboard.isCut()) clipboard.clear(); return result;
    }

    /**
     * Exclui uma Tag segundo modalidade explicitamente escolhida e confirmação dos alvos.
     *
     * @param id etiqueta selecionada por UUID
     * @param interaction escolhas e confirmações, incluindo outras Tags afetadas
     * @throws Exception se protegida, cancelada ou uma etapa falhar; lotes param na primeira falha
     */
    public void deleteTag(UUID id, Interaction interaction) throws Exception {
        Tag tag = tags.findById(id).orElseThrow();
        if (tag.isMissing()) throw new IllegalArgumentException("Etiqueta Ausente não pode ser excluída");
        List<LocalFile> selected = List.copyOf(files.find(new LocalFileFilter(Set.of(id), true, new NativeFileFilter())));
        int mode = interaction.choose("Excluir " + tag + "\nArquivos atingidos: " + selected,
                "1 — Apenas esta etiqueta; preservar cadastros e disco",
                "2 — Remover cadastros e todos os seus vínculos; preservar disco",
                "3 — Apagar permanentemente arquivos, cadastros e esta etiqueta", "Cancelar");
        if (mode < 0 || mode > 2) throw new CancellationException("Exclusão cancelada");
        if (tag.isPredefined() && interaction.choose("Esta etiqueta é predefinida e não será recriada automaticamente: " + tag, "Confirmar exclusão", "Cancelar") != 0) throw new CancellationException();
        Set<Tag> affected = new HashSet<>();
        if (mode > 0) for (LocalFile f : selected) for (Tag t : f.getTags()) if (!t.getId().equals(id)) affected.add(t);
        if (!affected.isEmpty() && interaction.choose("Outras etiquetas perderão vínculos com os arquivos selecionados, mas continuarão existindo:\n" + affected, "Confirmar", "Cancelar") != 0) throw new CancellationException();
        List<String> completed = new ArrayList<>();
        try {
            for (LocalFile f : selected) {
                if (mode == 0) removeTag(f.getId(), id);
                else {
                    if (mode == 2) {
                        NativeFile resolved = resolveAvailable(f.getNativeFile(), interaction);
                        nativeService.delete(resolved); completed.add("Conteúdo físico apagado: " + resolved);
                    }
                    files.delete(f.getId());
                }
                completed.add("Cadastro processado: " + f.getId());
            }
            tags.delete(id);
        } catch (Exception e) { throw new OperationFailure(completed.isEmpty() ? "Nenhum alvo anterior concluído" : String.join("\n", completed), "Exclusão interrompida; etapas do alvo atual podem estar parciais", e); }
    }

    /**
     * Exclui permanentemente um arquivo selecionado com confirmação de suas Tags.
     *
     * @param source arquivo nativo ou cadastrado
     * @param interaction confirmação antes de apagar conteúdo
     * @throws Exception se cancelado ou disco/SQL falhar, informando efeitos parciais
     */
    public void deleteFile(NativeFile source, Interaction interaction) throws Exception {
        source = resolveAvailable(source, interaction);
        Optional<LocalFile> file = files.findByPath(source.getPath());
        if (interaction.choose("Apagar PERMANENTEMENTE " + source + "?\nClassificações perdidas: " + file.map(LocalFile::getTags).orElse(Set.of()), "Apagar arquivo", "Cancelar") != 0) throw new CancellationException();
        nativeService.delete(source);
        try { if (file.isPresent()) files.delete(file.get().getId()); }
        catch (SQLException e) { throw new OperationFailure("Arquivo físico apagado: " + source, "Remover cadastro/vínculos", e); }
    }
}
