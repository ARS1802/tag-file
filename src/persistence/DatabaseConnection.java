/*
 * Inventário — DatabaseConnection.
 * Proprietário da única conexão JDBC; DAOs fecham apenas statements/resultados (AMB-05).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - DatabaseConnection.DatabaseConnection(Path configuration): Lê configuração local UTF-8 sem registrar credenciais.
 * - DatabaseConnection.open(): Abre explicitamente a conexão; não repete escritas nem inicia limpeza de domínio.
 * - DatabaseConnection.getConnection(): Empresta a conexão aberta; o chamador não assume sua propriedade.
 * - DatabaseConnection.close(): Fecha a conexão compartilhada no ponto responsável pelo seu ciclo de vida.
 *
 * Consulte: doc/banco-de-dados.md — SQL-01 a SQL-06; doc/instalacao-e-execucao.md — AMB-01 a AMB-07.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package persistence;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;

/**
 * Proprietário da única conexão JDBC; DAOs fecham apenas statements/resultados (AMB-05).
 */
public final class DatabaseConnection implements AutoCloseable {
    /**
     * Configuração JDBC, incluindo credenciais que não são registradas em logs.
     */
    private final Properties settings;
    /**
     * Conexão JDBC cujo ciclo de vida pertence a este objeto.
     */
    private Connection connection;

    /**
     * Lê configuração local UTF-8 sem registrar credenciais.
     *
     * @param configuration arquivo .properties existente
     * @throws IOException se configuração ausente ou inválida
     */
    public DatabaseConnection(Path configuration) throws IOException {
        settings = new Properties();
        try (Reader reader = Files.newBufferedReader(configuration, StandardCharsets.UTF_8)) { settings.load(reader); }
        if (!settings.getProperty("db.url", "").startsWith("jdbc:mysql://127.0.0.1:3333/tag_file?")
                || settings.getProperty("db.user") == null || settings.getProperty("db.password") == null)
            throw new IOException("Configuração deve identificar a instância exclusiva 127.0.0.1:3333/tag_file e conter usuário/senha");
    }

    /**
     * Abre explicitamente a conexão; não repete escritas nem inicia limpeza de domínio.
     *
     * @throws SQLException se driver/servidor/autenticação falhar ou conexão existente estiver inválida
     */
    public void open() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            if (!connection.isValid(3)) throw new SQLException("Conexão inválida; reconecte explicitamente", "08006");
            return;
        }
        Properties credentials = new Properties();
        credentials.setProperty("user", settings.getProperty("db.user"));
        credentials.setProperty("password", settings.getProperty("db.password"));
        connection = DriverManager.getConnection(settings.getProperty("db.url"), credentials);
    }

    /**
     * Empresta a conexão aberta; o chamador não assume sua propriedade.
     *
     * @return conexão compartilhada, sem pool
     * @throws SQLException se ainda não aberta ou já fechada
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) throw new SQLException("Abra a conexão antes da consulta", "08003");
        return connection;
    }

    /**
     * Fecha a conexão compartilhada no ponto responsável pelo seu ciclo de vida.
     *
     * @throws SQLException se fechamento JDBC falhar; só o proprietário encerra a conexão
     */
    @Override public void close() throws SQLException {
        if (connection != null) connection.close();
    }
}
