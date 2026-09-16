/*
 * Inventário — CrudDAO.
 * Contrato de persistência: ausência é Optional vazio; erro SQL nunca é ausência (ARQ-04).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - CrudDAO.create(T entity): Persiste uma entidade nova, sem garantia de atomicidade entre várias instruções.
 * - CrudDAO.findById(UUID id): Consulta uma identidade, distinguindo ausência legítima de falha SQL.
 * - CrudDAO.find(F filter): Consulta entidades do tipo correto segundo seus critérios.
 * - CrudDAO.update(T entity): Atualiza uma entidade existente preservando sua identidade.
 * - CrudDAO.delete(UUID id): Exclui a entidade identificada, segundo o contrato da implementação.
 *
 * Consulte: doc/banco-de-dados.md — SQL-01 a SQL-06; doc/instalacao-e-execucao.md — AMB-01 a AMB-07.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package persistence;

import filter.Filter;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistência: ausência é Optional vazio; erro SQL nunca é ausência (ARQ-04).
 *
 * @param <T> entidade persistida
 * @param <F> filtro específico da entidade
 */
public interface CrudDAO<T, F extends Filter> {
    /**
     * Persiste uma entidade nova, sem garantia de atomicidade entre várias instruções.
     *
     * @param entity entidade nova
     * @throws SQLException se gravação falhar, possivelmente após etapas parciais
     */
    void create(T entity) throws SQLException;
    /**
     * Consulta uma identidade, distinguindo ausência legítima de falha SQL.
     *
     * @param id identidade procurada
     * @return entidade ou vazio
     * @throws SQLException se consulta falhar
     */
    Optional<T> findById(UUID id) throws SQLException;
    /**
     * Consulta entidades do tipo correto segundo seus critérios.
     *
     * @param filter critérios da entidade
     * @return resultados sem duplicação, possivelmente vazios
     * @throws SQLException se leitura falhar
     */
    List<T> find(F filter) throws SQLException;
    /**
     * Atualiza uma entidade existente preservando sua identidade.
     *
     * @param entity fotografia com identidade existente
     * @throws SQLException se não existir ou atualização falhar, inclusive parcialmente
     */
    void update(T entity) throws SQLException;
    /**
     * Exclui a entidade identificada, segundo o contrato da implementação.
     *
     * @param id identidade a excluir
     * @return se uma linha foi removida
     * @throws SQLException se exclusão falhar
     */
    boolean delete(UUID id) throws SQLException;
}
