/*
 * Inventário — LocalFileTagDAO.
 * Relações arquivo/Tag: sem CRUD artificial, sem operações físicas (ARQ-04).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - LocalFileTagDAO.LocalFileTagDAO(DatabaseConnection database, TagDAO tags): Injeta os colaboradores necessários a LocalFileTagDAO.
 * - LocalFileTagDAO.associate(UUID file, UUID tag): Acrescenta vínculo idempotente; atualiza data apenas para vínculo novo.
 * - LocalFileTagDAO.dissociate(UUID file, UUID tag): Remove somente o vínculo; Manager completa a regra da última Tag.
 * - LocalFileTagDAO.findTags(Collection<UUID> ids): Carrega relações de uma coleção em lote, incluindo suas extensões.
 *
 * Consulte: doc/banco-de-dados.md — SQL-01 a SQL-06; doc/instalacao-e-execucao.md — AMB-01 a AMB-07.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package persistence;

import model.EntityFactory;
import model.Tag;
import java.sql.*;
import java.util.*;

/**
 * Relações arquivo/Tag: sem CRUD artificial, sem operações físicas (ARQ-04).
 */
public final class LocalFileTagDAO {
    /**
     * Proprietário da conexão JDBC compartilhada.
     */
    private final DatabaseConnection database;
    /**
     * DAO de etiquetas e suas extensões.
     */
    private final TagDAO tags;
    /**
     * Injeta os colaboradores necessários a LocalFileTagDAO.
     *
     * @param database conexão compartilhada
     * @param tags DAO de Tags na mesma conexão
     */
    public LocalFileTagDAO(DatabaseConnection database, TagDAO tags) { this.database = database; this.tags = tags; }

    /**
     * Acrescenta vínculo idempotente; atualiza data apenas para vínculo novo.
     * Não valida compatibilidade nem sentinela: coordenação pertence ao Manager.
     *
     * @param file identidade existente do arquivo
     * @param tag identidade existente da Tag
     * @return se criou uma associação
     * @throws SQLException se consulta, inserção ou atualização da data falhar; vínculo pode já existir
     */
    public boolean associate(UUID file, UUID tag) throws SQLException {
        try (PreparedStatement s = database.getConnection().prepareStatement("SELECT 1 FROM LOCAL_FILE_TAG WHERE file_id=? AND tag_id=?")) {
            s.setString(1, file.toString()); s.setString(2, tag.toString()); try (ResultSet r = s.executeQuery()) { if (r.next()) return false; }
        }
        try (PreparedStatement s = database.getConnection().prepareStatement("INSERT INTO LOCAL_FILE_TAG(file_id,tag_id) VALUES(?,?)")) {
            s.setString(1, file.toString()); s.setString(2, tag.toString()); s.executeUpdate();
        }
        try (PreparedStatement s = database.getConnection().prepareStatement("UPDATE TAG SET last_file_tagged_at=? WHERE id=?")) {
            s.setObject(1, EntityFactory.now()); s.setString(2, tag.toString()); s.executeUpdate();
        }
        return true;
    }

    /**
     * Remove somente o vínculo; Manager completa a regra da última Tag.
     *
     * @param file UUID do arquivo
     * @param tag UUID da Tag
     * @return se o vínculo existia
     * @throws SQLException se escrita falhar
     */
    public boolean dissociate(UUID file, UUID tag) throws SQLException {
        try (PreparedStatement s = database.getConnection().prepareStatement("DELETE FROM LOCAL_FILE_TAG WHERE file_id=? AND tag_id=?")) {
            s.setString(1, file.toString()); s.setString(2, tag.toString()); return s.executeUpdate() != 0;
        }
    }

    /**
     * Carrega relações de uma coleção em lote, incluindo suas extensões.
     *
     * @param ids identidades a consultar; vazio não consulta SQL
     * @return mapa de associações imutáveis; chaves sem vínculos ficam ausentes
     * @throws SQLException se leitura falhar
     */
    public Map<UUID, Set<Tag>> findTags(Collection<UUID> ids) throws SQLException {
        if (ids.isEmpty()) return Map.of();
        Map<UUID, Tag> known = new HashMap<>();
        for (Tag tag : tags.find(new filter.TagFilter())) known.put(tag.getId(), tag);
        Map<UUID, Set<Tag>> result = new HashMap<>();
        String marks = String.join(",", Collections.nCopies(ids.size(), "?"));
        try (PreparedStatement s = database.getConnection().prepareStatement("SELECT file_id,tag_id FROM LOCAL_FILE_TAG WHERE file_id IN (" + marks + ")")) {
            int index = 1; for (UUID id : ids) s.setString(index++, id.toString());
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) result.computeIfAbsent(UUID.fromString(r.getString(1)), key -> new HashSet<>()).add(known.get(UUID.fromString(r.getString(2))));
            }
        }
        result.replaceAll((key, value) -> Set.copyOf(value)); return Map.copyOf(result);
    }
}
