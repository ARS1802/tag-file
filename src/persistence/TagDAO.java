/*
 * Inventário — TagDAO.
 * Persiste Tags e extensões; não retorna arquivos nem fecha a conexão (SQL-04).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - TagDAO.TagDAO(DatabaseConnection database): Injeta os colaboradores necessários a TagDAO.
 * - TagDAO.create(Tag tag): Insere Tag e extensões em etapas autocommit; falha pode deixar Tag incompleta.
 * - TagDAO.findById(UUID id): Carrega todas as extensões em uma única consulta conjunta.
 * - TagDAO.find(TagFilter filter): Consulta Tags, inclusive vazias; indisponibilidade não define vazio.
 * - TagDAO.read(String condition, List<String> values): Lê Tags por um fragmento interno; dados externos são parâmetros preparados.
 * - TagDAO.update(Tag tag): Atualiza nome/cor/restrições; Manager confirma incompatibilidades antes da chamada.
 * - TagDAO.writeExtensions(Tag tag): Insere cada restrição com chave composta única; não usa INSERT IGNORE.
 * - TagDAO.delete(UUID id): Remove Tag e relações por cascata; Manager cuida da sentinela dos cadastros.
 * - TagDAO.countAvailable(UUID id): Conta associados disponíveis sem persistir contador na Tag.
 *
 * Consulte: doc/banco-de-dados.md — SQL-01 a SQL-06; doc/instalacao-e-execucao.md — AMB-01 a AMB-07.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package persistence;

import filter.TagFilter;
import model.Tag;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Persiste Tags e extensões; não retorna arquivos nem fecha a conexão (SQL-04).
 */
public final class TagDAO implements CrudDAO<Tag, TagFilter> {
    /**
     * Proprietário da conexão JDBC compartilhada.
     */
    private final DatabaseConnection database;
    /**
     * Injeta os colaboradores necessários a TagDAO.
     *
     * @param database conexão compartilhada aberta pelo proprietário
     */
    public TagDAO(DatabaseConnection database) { this.database = Objects.requireNonNull(database); }

    /**
     * Insere Tag e extensões em etapas autocommit; falha pode deixar Tag incompleta.
     *
     * @param tag entidade nova com UUID próprio
     * @throws SQLException se identidade duplicada ou escrita falhar
     */
    @Override public void create(Tag tag) throws SQLException {
        try (PreparedStatement s = database.getConnection().prepareStatement("INSERT INTO TAG(id,name,color,created_at,last_file_tagged_at,predefined) VALUES(?,?,?,?,?,?)")) {
            s.setString(1, tag.getId().toString()); s.setString(2, tag.getName()); s.setString(3, tag.getColor());
            s.setObject(4, tag.getCreatedAt()); s.setObject(5, tag.getLastFileTaggedAt()); s.setBoolean(6, tag.isPredefined()); s.executeUpdate();
        }
        writeExtensions(tag);
    }

    /**
     * Carrega todas as extensões em uma única consulta conjunta.
     *
     * @param id UUID buscado
     * @return Tag reconstruída ou vazio
     * @throws SQLException se leitura falhar
     */
    @Override public Optional<Tag> findById(UUID id) throws SQLException {
        return read("WHERE t.id=?", List.of(id.toString())).stream().findFirst();
    }

    /**
     * Consulta Tags, inclusive vazias; indisponibilidade não define vazio.
     *
     * @param filter critérios de criação e associações
     * @return Tags ordenadas por nome e UUID
     * @throws SQLException se SQL falhar
     */
    @Override public List<Tag> find(TagFilter filter) throws SQLException {
        String condition = filter.isEmptyOnly() ? "WHERE NOT EXISTS (SELECT 1 FROM LOCAL_FILE_TAG ft WHERE ft.tag_id=t.id)" : "";
        return read(condition, List.of()).stream().filter(t -> filter.acceptsDate(t.getCreatedAt())).toList();
    }

    /**
     * Lê Tags por um fragmento interno; dados externos são parâmetros preparados.
     *
     * @param condition SQL constante controlado pelo DAO
     * @param values parâmetros posicionais
     * @return entidades com suas extensões normalizadas
     * @throws SQLException se consulta/reconstrução falhar
     */
    private List<Tag> read(String condition, List<String> values) throws SQLException {
        Map<UUID, Tag> base = new LinkedHashMap<>();
        Map<UUID, Set<String>> extensions = new HashMap<>();
        try (PreparedStatement s = database.getConnection().prepareStatement("SELECT t.*,e.extension FROM TAG t LEFT JOIN TAG_EXTENSION e ON e.tag_id=t.id " + condition + " ORDER BY t.name,t.id,e.extension")) {
            for (int i = 0; i < values.size(); i++) s.setString(i + 1, values.get(i));
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    UUID id = UUID.fromString(r.getString("id"));
                    base.putIfAbsent(id, new Tag(id, r.getString("name"), r.getString("color"), Set.of(),
                            r.getObject("created_at", LocalDateTime.class), r.getObject("last_file_tagged_at", LocalDateTime.class), r.getBoolean("predefined")));
                    Set<String> set = extensions.computeIfAbsent(id, key -> new HashSet<>());
                    if (r.getString("extension") != null) set.add(r.getString("extension"));
                }
            }
        }
        return base.values().stream().map(t -> new Tag(t.getId(), t.getName(), t.getColor(), extensions.get(t.getId()), t.getCreatedAt(), t.getLastFileTaggedAt(), t.isPredefined())).toList();
    }

    /**
     * Atualiza nome/cor/restrições; Manager confirma incompatibilidades antes da chamada.
     * Identidade, criação, condição de predefinida e última associação são preservadas.
     *
     * @param tag estado final validado
     * @throws SQLException se ausente ou alguma etapa SQL falhar
     * @throws IllegalArgumentException se tentar editar a sentinela
     */
    @Override public void update(Tag tag) throws SQLException {
        if (tag.isMissing()) throw new IllegalArgumentException("Etiqueta Ausente não pode ser editada");
        try (PreparedStatement s = database.getConnection().prepareStatement("UPDATE TAG SET name=?,color=? WHERE id=?")) {
            s.setString(1, tag.getName()); s.setString(2, tag.getColor()); s.setString(3, tag.getId().toString());
            if (s.executeUpdate() == 0) throw new SQLException("Tag não encontrada: " + tag.getId(), "02000");
        }
        try (PreparedStatement s = database.getConnection().prepareStatement("DELETE FROM TAG_EXTENSION WHERE tag_id=?")) {
            s.setString(1, tag.getId().toString()); s.executeUpdate();
        }
        writeExtensions(tag);
    }

    /**
     * Insere cada restrição com chave composta única; não usa INSERT IGNORE.
     *
     * @param tag etiqueta cuja linha já existe
     * @throws SQLException se gravação falhar após extensões anteriores
     */
    private void writeExtensions(Tag tag) throws SQLException {
        try (PreparedStatement s = database.getConnection().prepareStatement("INSERT INTO TAG_EXTENSION(tag_id,extension) VALUES(?,?)")) {
            for (String ext : tag.getExtensions()) { s.setString(1, tag.getId().toString()); s.setString(2, ext); s.executeUpdate(); }
        }
    }

    /**
     * Remove Tag e relações por cascata; Manager cuida da sentinela dos cadastros.
     *
     * @param id identidade autorizada para exclusão
     * @return se existia uma Tag
     * @throws SQLException se banco falhar
     * @throws IllegalArgumentException se identidade for a sentinela
     */
    @Override public boolean delete(UUID id) throws SQLException {
        if (Tag.MISSING_ID.equals(id)) throw new IllegalArgumentException("Etiqueta Ausente é protegida");
        try (PreparedStatement s = database.getConnection().prepareStatement("DELETE FROM TAG WHERE id=?")) { s.setString(1, id.toString()); return s.executeUpdate() != 0; }
    }

    /**
     * Conta associados disponíveis sem persistir contador na Tag.
     *
     * @param id identidade da Tag
     * @return total de arquivos com available=true
     * @throws SQLException se consulta falhar
     */
    public long countAvailable(UUID id) throws SQLException {
        try (PreparedStatement s = database.getConnection().prepareStatement("SELECT COUNT(*) FROM LOCAL_FILE_TAG ft JOIN LOCAL_FILE f ON f.id=ft.file_id WHERE ft.tag_id=? AND f.available=TRUE")) {
            s.setString(1, id.toString()); try (ResultSet r = s.executeQuery()) { r.next(); return r.getLong(1); }
        }
    }
}
