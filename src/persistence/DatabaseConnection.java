/*
 * Inventário — DatabaseConnection.
 * Proprietário da única conexão JDBC; DAOs fecham apenas statements/resultados (AMB-05).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - DatabaseConnection.DatabaseConnection(Path configuration): Lê configuração local UTF-8 sem registrar credenciais.
 * - prepareLocalConfiguration(Path): cria configuração ausente a partir do modelo validado.
 * - verifyDriver(): verifica o Connector/J sem abrir conexão SQL.
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
import java.nio.file.*;
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
        try (Reader reader = Files.newBufferedReader(configuration, StandardCharsets.UTF_8)) {
            settings.load(reader);
        } catch (NoSuchFileException missing) {
            throw new IOException("Configuração JDBC não encontrada: " + configuration
                    + "\nO arquivo define como o Tag-File se conecta ao MySQL. Confira os arquivos de configuração do projeto.", missing);
        } catch (IOException unreadable) {
            throw new IOException("Não foi possível ler a configuração JDBC: " + configuration
                    + "\nVerifique se é um arquivo e se sua conta tem permissão de leitura.", unreadable);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Configuração JDBC inválida: " + configuration
                    + "\nConfira o formato do arquivo .properties e sua codificação UTF-8.", invalid);
        }
        if (!settings.getProperty("db.url", "").startsWith("jdbc:mysql://127.0.0.1:3333/tag_file?")
                || settings.getProperty("db.user") == null || settings.getProperty("db.password") == null)
            throw new IOException("Configuração JDBC inválida: " + configuration
                    + "\nDefina db.url para a instância exclusiva 127.0.0.1:3333/tag_file e inclua db.user e db.password. Consulte database.properties.example.");
    }

    /**
     * Prepara apenas a configuração ausente, usando o modelo distribuído com o projeto.
     * Não abre conexão nem sobrescreve configurações existentes, mesmo inválidas.
     * @param configuration caminho do arquivo local .properties
     * @return conexão configurada, ainda fechada
     * @throws IOException se o modelo ou a configuração não puderem ser lidos/criados
     */
    public static DatabaseConnection prepareLocalConfiguration(Path configuration) throws IOException {
        if (Files.notExists(configuration, LinkOption.NOFOLLOW_LINKS)) {
            Path example = configuration.resolveSibling(configuration.getFileName() + ".example");
            // Valida o modelo antes de criar qualquer arquivo local.
            new DatabaseConnection(example);
            try {
                Files.copy(example, configuration);
            } catch (FileAlreadyExistsException concurrentCreation) {
                // Outra execução criou o arquivo; valida o conteúdo existente abaixo.
            } catch (IOException creationFailure) {
                throw new IOException("Não foi possível criar a configuração JDBC: " + configuration
                        + "\nVerifique a permissão de escrita na pasta database/config. O modelo é " + example + ".", creationFailure);
            }
        }
        return new DatabaseConnection(configuration);
    }

    /**
     * Confere o driver antes de instalar ou iniciar o servidor, sem conexão de rede.
     * @throws SQLException se o Connector/J estiver ausente, incompatível ou inválido
     */
    public void verifyDriver() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DriverManager.getDriver(settings.getProperty("db.url"));
        } catch (ClassNotFoundException | SQLException unavailable) {
            throw new SQLException("Driver JDBC do MySQL ausente do classpath."
                    + "\nCopie mysql-connector-j-9.7.0.jar para a pasta lib do projeto e inclua-o nas dependências de execução."
                    + "\nNo IntelliJ: Project Structure > Modules > Dependencies. Confira o módulo usado pela configuração Run."
                    + "\nO JAR não é incluído pelo Git. Consulte lib/README.md.", "08001", unavailable);
        } catch (LinkageError incompatible) {
            throw new SQLException("Não foi possível carregar o driver JDBC do MySQL."
                    + "\nConfira a integridade do Connector/J na pasta lib e sua compatibilidade com o JDK.", "08001", incompatible);
        }
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
        verifyDriver();
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
