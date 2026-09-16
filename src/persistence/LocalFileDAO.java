/*
 * Inventário — LocalFileDAO.
 * Cadastros e pesquisa de arquivos por Tags; reconstrução conserva UUID/datas (SQL-02).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - LocalFileDAO.LocalFileDAO(DatabaseConnection database, LocalFileTagDAO links): Injeta os colaboradores necessários a LocalFileDAO.
 * - LocalFileDAO.create(LocalFile file): Grava metadados; associações são gravadas separadamente pelo DAO de vínculos.
 * - LocalFileDAO.findById(UUID id): Reconstrói o cadastro com UUID, datas e relações persistidos.
 * - LocalFileDAO.findByPath(Path path): Busca normalizada sem criar cadastro.
 * - LocalFileDAO.find(LocalFileFilter filter): Consulta AND/OR por subconsulta agrupada: cada UUID de arquivo aparece uma vez.
 * - LocalFileDAO.findByPaths(Collection<Path> paths): Consulta correspondências em lote, sem eliminar nativos não cadastrados da UI.
 * - LocalFileDAO.read(String condition, List<?> values): Reconstrói linhas antes de carregar relações, fechando o ResultSet intermediário.
 * - LocalFileDAO.update(LocalFile file): Atualiza caminho e metadados preservando identidade e associações.
 * - LocalFileDAO.bind(PreparedStatement s, LocalFile file): Preenche parâmetros na ordem comum a criação e atualização.
 * - LocalFileDAO.delete(UUID id): Remove cadastro e vínculos por cascata, preservando disco e Tags.
 * - LocalFileDAO.cleanupUnclassified(): Limpa somente órfãos ou registros exclusivos da sentinela; chamar na inicialização.
 *
 * Consulte: doc/banco-de-dados.md — SQL-01 a SQL-06; doc/instalacao-e-execucao.md — AMB-01 a AMB-07.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package persistence;

import filter.LocalFileFilter;
import model.*;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Cadastros e pesquisa de arquivos por Tags; reconstrução conserva UUID/datas (SQL-02).
 */
public final class LocalFileDAO implements CrudDAO<LocalFile, LocalFileFilter> {
    /**
     * Proprietário da conexão JDBC compartilhada.
     */
    private final DatabaseConnection database;
    /**
     * DAO de associações na mesma conexão.
     */
    private final LocalFileTagDAO links;
    /**
     * Injeta os colaboradores necessários a LocalFileDAO.
     *
     * @param database conexão compartilhada
     * @param links relações usando a mesma conexão
     */
    public LocalFileDAO(DatabaseConnection database, LocalFileTagDAO links) { this.database = database; this.links = links; }

    /**
     * Grava metadados; associações são gravadas separadamente pelo DAO de vínculos.
     *
     * @param file entidade nova
     * @throws SQLException se UUID/caminho duplicado ou escrita falhar
     */
    @Override public void create(LocalFile file) throws SQLException {
        try (PreparedStatement s = database.getConnection().prepareStatement("INSERT INTO LOCAL_FILE(path,available,size_bytes,created_at,modified_at,last_accessed_at,id) VALUES(?,?,?,?,?,?,?)")) {
            bind(s, file); s.executeUpdate();
        }
    }

    /**
     * Reconstrói o cadastro com UUID, datas e relações persistidos.
     *
     * @param id identidade buscada
     * @return cadastro ou vazio
     * @throws SQLException se consulta falhar
     */
    @Override public Optional<LocalFile> findById(UUID id) throws SQLException { return read("WHERE f.id=?", List.of(id.toString())).stream().findFirst(); }

    /**
     * Busca normalizada sem criar cadastro.
     *
     * @param path caminho relativo ou absoluto
     * @return cadastro correspondente ou vazio
     * @throws SQLException se consulta falhar
     */
    public Optional<LocalFile> findByPath(Path path) throws SQLException { return read("WHERE f.path=?", List.of(new NativeFile(path).getPath().toString())).stream().findFirst(); }

    /**
     * Consulta AND/OR por subconsulta agrupada: cada UUID de arquivo aparece uma vez.
     *
     * @param filter Tags selecionadas e critérios físicos; vazio aceita todos
     * @return cadastros ordenados por caminho
     * @throws SQLException se consulta falhar
     */
    @Override public List<LocalFile> find(LocalFileFilter filter) throws SQLException {
        Set<UUID> ids = filter.getTagIds();
        String condition = ""; List<Object> values = new ArrayList<>();
        if (!ids.isEmpty()) {
            condition = "WHERE f.id IN (SELECT file_id FROM LOCAL_FILE_TAG WHERE tag_id IN (" + String.join(",", Collections.nCopies(ids.size(), "?")) + ") GROUP BY file_id";
            for (UUID id : ids) values.add(id.toString());
            if (filter.isMatchAll()) { condition += " HAVING COUNT(DISTINCT tag_id)=?"; values.add(ids.size()); }
            condition += ")";
        }
        return read(condition, values).stream().filter(filter::matchesPhysical).toList();
    }

    /**
     * Consulta correspondências em lote, sem eliminar nativos não cadastrados da UI.
     *
     * @param paths caminhos que a listagem física confirmou; vazio retorna mapa vazio
     * @return mapa imutável com chaves absolutas normalizadas
     * @throws SQLException se consulta falhar
     */
    public Map<Path, LocalFile> findByPaths(Collection<Path> paths) throws SQLException {
        if (paths.isEmpty()) return Map.of();
        List<Object> normalized = paths.stream().map(p -> (Object) new NativeFile(p).getPath().toString()).distinct().toList();
        Map<Path, LocalFile> map = new LinkedHashMap<>();
        // Lotes limitam parâmetros sem consultar cada arquivo individualmente.
        for (int offset = 0; offset < normalized.size(); offset += 500) {
            List<Object> batch = normalized.subList(offset, Math.min(offset + 500, normalized.size()));
            for (LocalFile file : read("WHERE f.path IN (" + String.join(",", Collections.nCopies(batch.size(), "?")) + ")", batch)) map.put(file.getNativeFile().getPath(), file);
        }
        return Map.copyOf(map);
    }

    /**
     * Reconstrói linhas antes de carregar relações, fechando o ResultSet intermediário.
     *
     * @param condition fragmento interno parametrizado
     * @param values valores recebidos para PreparedStatement
     * @return fotografias completas preservando dados persistidos
     * @throws SQLException se SQL falhar
     */
    private List<LocalFile> read(String condition, List<?> values) throws SQLException {
        List<LocalFile> files = new ArrayList<>();
        try (PreparedStatement s = database.getConnection().prepareStatement("SELECT f.* FROM LOCAL_FILE f " + condition + " ORDER BY f.path,f.id")) {
            for (int i = 0; i < values.size(); i++) s.setObject(i + 1, values.get(i));
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) files.add(new LocalFile(UUID.fromString(r.getString("id")), new NativeFile(Path.of(r.getString("path"))), r.getBoolean("available"),
                        r.getObject("size_bytes", Long.class), r.getObject("created_at", LocalDateTime.class), r.getObject("modified_at", LocalDateTime.class), r.getObject("last_accessed_at", LocalDateTime.class), Set.of()));
            }
        }
        Map<UUID, Set<Tag>> associations = links.findTags(files.stream().map(LocalFile::getId).toList());
        return files.stream().map(f -> new LocalFile(f.getId(), f.getNativeFile(), f.isAvailable(), f.getSize(), f.getCreatedAt(), f.getModifiedAt(), f.getLastAccessedAt(), associations.getOrDefault(f.getId(), Set.of())))
                .sorted(Comparator.comparing((LocalFile f) -> f.getNativeFile().getName()).thenComparing(f -> f.getNativeFile().getPath().toString())).toList();
    }

    /**
     * Atualiza caminho e metadados preservando identidade e associações.
     *
     * @param file cadastro existente com nova fotografia física
     * @throws SQLException se ausente, conflito de caminho ou falha SQL
     */
    @Override public void update(LocalFile file) throws SQLException {
        try (PreparedStatement s = database.getConnection().prepareStatement("UPDATE LOCAL_FILE SET path=?,available=?,size_bytes=?,created_at=?,modified_at=?,last_accessed_at=? WHERE id=?")) {
            bind(s, file); if (s.executeUpdate() == 0) throw new SQLException("Cadastro não encontrado: " + file.getId(), "02000");
        }
    }

    /**
     * Preenche parâmetros na ordem comum a criação e atualização.
     *
     * @param s statement do DAO, fechado pelo chamador
     * @param file valores já validados no domínio
     * @throws SQLException se driver recusar conversão ou parâmetro
     */
    private void bind(PreparedStatement s, LocalFile file) throws SQLException {
        s.setString(1, file.getNativeFile().getPath().toString()); s.setBoolean(2, file.isAvailable()); s.setObject(3, file.getSize());
        s.setObject(4, file.getCreatedAt()); s.setObject(5, file.getModifiedAt()); s.setObject(6, file.getLastAccessedAt()); s.setString(7, file.getId().toString());
    }

    /**
     * Remove cadastro e vínculos por cascata, preservando disco e Tags.
     *
     * @param id identidade a excluir
     * @return se existia registro
     * @throws SQLException se exclusão falhar
     */
    @Override public boolean delete(UUID id) throws SQLException {
        try (PreparedStatement s = database.getConnection().prepareStatement("DELETE FROM LOCAL_FILE WHERE id=?")) { s.setString(1, id.toString()); return s.executeUpdate() != 0; }
    }

    /**
     * Limpa somente órfãos ou registros exclusivos da sentinela; chamar na inicialização.
     *
     * @return número removido; indisponibilidade não interfere
     * @throws SQLException se exclusão falhar
     */
    public int cleanupUnclassified() throws SQLException {
        try (PreparedStatement s = database.getConnection().prepareStatement("DELETE FROM LOCAL_FILE WHERE NOT EXISTS (SELECT 1 FROM LOCAL_FILE_TAG ft WHERE ft.file_id=LOCAL_FILE.id AND ft.tag_id<>?)")) {
            s.setString(1, Tag.MISSING_ID.toString()); return s.executeUpdate();
        }
    }
}
