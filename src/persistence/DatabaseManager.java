/*
 * Inventário — DatabaseManager.
 * Administra scripts e schema da instância exclusiva, separado da conexão (AMB-01/07).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - DatabaseManager.DatabaseManager(Path root): Define a raiz que contém configuração, scripts e runtime.
 * - DatabaseManager.DatabaseManager(Path root, Installer installer): Injeta a execução autorizada da instalação.
 * - Installer.install(Path script, String authorizationArgument): Executa o instalador após autorização nativa.
 * - DatabaseManager.inspect(): Inspeciona configuração, driver e executáveis; não instala/inicia/encerra servidor.
 * - DatabaseManager.prepare(boolean installationAuthorized): Verifica componentes; instalar requer autorização explícita do chamador/UI.
 * - DatabaseManager.runScript(String action, boolean authorized): Executa apenas scripts distribuídos; credenciais nunca vão nos argumentos.
 * - DatabaseManager.prepareSchema(DatabaseConnection database): Confirma identidade da instância antes de aplicar DDL; não usa transações explícitas.
 * - DatabaseManager.verifyInstance(DatabaseConnection database): Verifica porta e diretório reais do servidor, sem confiar somente no endereço JDBC.
 * - DatabaseManager.executeSql(DatabaseConnection database, Path script): Aplica SQL confiável do projeto, separado por ponto e vírgula; não aceita entrada do usuário.
 * - DatabaseManager.validateSchema(DatabaseConnection database): Confere colunas obrigatórias e o índice único do caminho; incompatibilidade exige revisão.
 * - DatabaseManager.seedTags(DatabaseConnection database): Prepara cinco identidades fixas uma vez; exclusões posteriores não são revertidas.
 * - DatabaseManager.stop(): Encerra somente a instância identificada; fechar JDBC antes desta chamada.
 *
 * Consulte: doc/banco-de-dados.md — SQL-01 a SQL-06; doc/instalacao-e-execucao.md — AMB-01 a AMB-07.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package persistence;

import model.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Administra scripts e schema da instância exclusiva, separado da conexão (AMB-01/07).
 */
public final class DatabaseManager {
    /**
     * Raiz absoluta do projeto para configuração, scripts e dados exclusivos.
     */
    private final Path root;
    /** Instalação com autorização nativa; null mantém a execução administrativa direta. */
    private final Installer installer;

    /** Fronteira usada pela entrada da aplicação para solicitar autorização ao sistema. */
    @FunctionalInterface
    public interface Installer {
        /**
         * Executa somente o instalador fornecido pelo projeto.
         * @param script caminho absoluto do instalador
         * @param authorizationArgument argumento explícito de autorização do script
         * @throws IOException se autorização ou instalação falhar
         * @throws InterruptedException se a espera for interrompida
         */
        void install(Path script, String authorizationArgument) throws IOException, InterruptedException;
    }
    /**
     * Define a raiz que contém configuração, scripts e runtime.
     *
     * @param root raiz existente do projeto
     * @throws IllegalArgumentException se scripts não estiverem presentes
     */
    public DatabaseManager(Path root) {
        this(root, null);
    }

    /**
     * Define a raiz e o executor que solicita autorização para instalar.
     * @param root raiz existente do projeto
     * @param installer executor nativo; null para administração direta já autorizada
     * @throws IllegalArgumentException se scripts não estiverem presentes
     */
    public DatabaseManager(Path root, Installer installer) {
        this.root = root.toAbsolutePath().normalize();
        this.installer = installer;
        if (!Files.isDirectory(this.root.resolve("database/scripts"))) throw new IllegalArgumentException("Execute a partir da raiz do Tag-File");
    }

    /**
     * Inspeciona configuração, driver e executáveis; não instala/inicia/encerra servidor.
     *
     * @return diagnóstico sem URL autenticada nem senha
     * @throws IOException se scripts/configuração falharem
     * @throws InterruptedException se espera for interrompida
     */
    public String inspect() throws IOException, InterruptedException {
        new DatabaseConnection(root.resolve("database/config/database.properties"));
        String driver;
        try { Class.forName("com.mysql.cj.jdbc.Driver"); driver = "Connector/J no classpath"; }
        catch (ClassNotFoundException e) { driver = "Connector/J ausente do classpath"; }
        return "Instância exclusiva: 127.0.0.1:3333/tag_file\nDados: " + root.resolve("database/runtime/data") + "\n" + driver + "\n" + runScript("check", false);
    }

    /**
     * Verifica componentes; instalar requer autorização explícita do chamador/UI.
     *
     * @param installationAuthorized escolha afirmativa do usuário se houver ausências
     * @throws IOException se recusado, instalação cancelada ou preparação falhar
     * @throws InterruptedException se processo for interrompido
     */
    public void prepare(boolean installationAuthorized) throws IOException, InterruptedException {
        try { runScript("check", false); }
        catch (IOException missing) {
            if (!installationAuthorized) throw new IOException("Componentes ausentes; instalação não autorizada. Nada foi instalado.", missing);
            runScript("install", true);
        }
        runScript("initialize", false); runScript("start", false);
    }

    /**
     * Executa apenas scripts distribuídos; credenciais nunca vão nos argumentos.
     *
     * @param action check, install, initialize, start ou stop
     * @param authorized confirma instalação, sem autorizar outras instâncias
     * @return saída do processo sem credenciais
     * @throws IOException se script falhar ou exceder o tempo; código de saída é preservado na mensagem
     * @throws InterruptedException se espera for interrompida
     */
    private String runScript(String action, boolean authorized) throws IOException, InterruptedException {
        if (!Set.of("check", "install", "initialize", "start", "stop").contains(action)) throw new IllegalArgumentException("Script desconhecido");
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path script = root.resolve("database/scripts/" + (windows ? "windows/" : "linux/") + action + (windows ? ".ps1" : ".sh"));
        if (action.equals("install") && installer != null) {
            if (!authorized) throw new IOException("Instalação não autorizada");
            installer.install(script, windows ? "-Authorized" : "--authorized");
            return "Instalação autorizada concluída";
        }
        List<String> command = new ArrayList<>(windows ? List.of("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.toString()) : List.of("bash", script.toString()));
        if (authorized) command.add(windows ? "-Authorized" : "--authorized");
        Files.createDirectories(root.resolve("database/runtime/logs"));
        Path log = Files.createTempFile(root.resolve("database/runtime/logs"), action + "-", ".log");
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            if (!process.waitFor(Duration.ofMinutes(action.equals("install") ? 15 : 2).toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroy(); throw new IOException("Tempo excedido no script " + action + "; consulte " + log);
            }
        } catch (InterruptedException e) { process.destroy(); Thread.currentThread().interrupt(); throw e; }
        String output = Files.readString(log, StandardCharsets.UTF_8);
        if (process.exitValue() != 0) throw new IOException("Script " + action + ": código de saída " + process.exitValue() + "\n" + output);
        return output.strip();
    }

    /**
     * Confirma identidade da instância antes de aplicar DDL; não usa transações explícitas.
     * Cria ausências, aplica índice faltante e verifica contratos; não corrige destrutivamente
     * schema incompatível. Predefinidas são preparadas uma vez, retomando etapas incompletas.
     *
     * @param database conexão aberta da instância exclusiva
     * @throws SQLException se instância errada, schema incompatível ou etapa SQL falhar
     * @throws IOException se script SQL não puder ser lido
     */
    public void prepareSchema(DatabaseConnection database) throws SQLException, IOException {
        verifyInstance(database);
        executeSql(database, root.resolve("database/schema/001-create.sql"));
        try (PreparedStatement s = database.getConnection().prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='LOCAL_FILE' AND INDEX_NAME='available_files'"); ResultSet r = s.executeQuery()) {
            r.next(); if (r.getInt(1) == 0) executeSql(database, root.resolve("database/schema/002-index.sql"));
        }
        // Consultas explícitas detectam colunas faltantes; metadados verificam PK/FK/índice em validateSchema.
        validateSchema(database);
        seedTags(database);
    }

    /**
     * Verifica porta e diretório reais do servidor, sem confiar somente no endereço JDBC.
     *
     * @param database conexão aberta
     * @throws SQLException se servidor não pertence ao diretório do projeto
     */
    public void verifyInstance(DatabaseConnection database) throws SQLException {
        try (Statement s = database.getConnection().createStatement(); ResultSet r = s.executeQuery("SELECT @@datadir,@@port")) {
            r.next(); Path actual = Path.of(r.getString(1)).toAbsolutePath().normalize();
            if (r.getInt(2) != 3333 || !actual.equals(root.resolve("database/runtime/data"))) throw new SQLException("Servidor não é a instância exclusiva deste projeto", "08004");
        }
    }

    /**
     * Aplica SQL confiável do projeto, separado por ponto e vírgula; não aceita entrada do usuário.
     *
     * @param database conexão compartilhada
     * @param script arquivo DDL sem procedures nem delimitadores especiais
     * @throws IOException se arquivo não puder ser lido
     * @throws SQLException se qualquer instrução falhar, preservando as anteriores
     */
    private void executeSql(DatabaseConnection database, Path script) throws IOException, SQLException {
        String sql = Files.readString(script).replaceAll("(?m)^\\s*--.*$", "");
        try (Statement s = database.getConnection().createStatement()) { for (String part : sql.split(";")) if (!part.isBlank()) s.execute(part); }
    }

    /**
     * Confere colunas obrigatórias e o índice único do caminho; incompatibilidade exige revisão.
     *
     * @param database conexão compartilhada
     * @throws SQLException se schema não atender os contratos utilizados pelos DAOs
     */
    private void validateSchema(DatabaseConnection database) throws SQLException {
        try (Statement s = database.getConnection().createStatement()) {
            for (String query : List.of("SELECT id,path,available,size_bytes,created_at,modified_at,last_accessed_at FROM LOCAL_FILE LIMIT 0",
                    "SELECT id,name,color,created_at,last_file_tagged_at,predefined FROM TAG LIMIT 0", "SELECT file_id,tag_id FROM LOCAL_FILE_TAG LIMIT 0", "SELECT tag_id,extension FROM TAG_EXTENSION LIMIT 0")) {
                try (ResultSet ignored = s.executeQuery(query)) { ignored.getMetaData(); }
            }
        }
        try (PreparedStatement s = database.getConnection().prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='LOCAL_FILE' AND INDEX_NAME='unique_path' AND NON_UNIQUE=0"); ResultSet r = s.executeQuery()) {
            r.next(); if (r.getInt(1) != 1) throw new SQLException("Schema incompatível: índice único de caminho ausente", "42000");
        }
        DatabaseMetaData metadata = database.getConnection().getMetaData();
        Map<String, Set<String>> primaryKeys = Map.of("TAG", Set.of("id"), "LOCAL_FILE", Set.of("id"), "TAG_EXTENSION", Set.of("tag_id", "extension"), "LOCAL_FILE_TAG", Set.of("file_id", "tag_id"));
        for (var expected : primaryKeys.entrySet()) {
            Set<String> actual = new HashSet<>();
            try (ResultSet rows = metadata.getPrimaryKeys("tag_file", null, expected.getKey())) { while (rows.next()) actual.add(rows.getString("COLUMN_NAME")); }
            if (!actual.equals(expected.getValue())) throw new SQLException("Schema incompatível: chave primária de " + expected.getKey(), "42000");
        }
        Set<String> relations = new HashSet<>();
        for (String table : List.of("TAG_EXTENSION", "LOCAL_FILE_TAG")) {
            try (ResultSet rows = metadata.getImportedKeys("tag_file", null, table)) {
                while (rows.next()) if (rows.getShort("DELETE_RULE") == DatabaseMetaData.importedKeyCascade)
                    relations.add(table + "." + rows.getString("FKCOLUMN_NAME") + ">" + rows.getString("PKTABLE_NAME") + "." + rows.getString("PKCOLUMN_NAME"));
            }
        }
        if (!relations.equals(Set.of("TAG_EXTENSION.tag_id>TAG.id", "LOCAL_FILE_TAG.tag_id>TAG.id", "LOCAL_FILE_TAG.file_id>LOCAL_FILE.id"))) throw new SQLException("Schema incompatível: vínculos/cascatas diferentes do contrato", "42000");
        try (Statement statement = database.getConnection().createStatement(); ResultSet rows = statement.executeQuery("SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,DATETIME_PRECISION,CHARACTER_MAXIMUM_LENGTH,COLLATION_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE()")) {
            while (rows.next()) {
                String column = rows.getString("COLUMN_NAME");
                if (Set.of("created_at", "modified_at", "last_accessed_at", "last_file_tagged_at").contains(column)
                        && (!rows.getString("DATA_TYPE").equals("datetime") || rows.getInt("DATETIME_PRECISION") != 6)) throw new SQLException("Schema incompatível: data precisa ser DATETIME(6): " + column, "42000");
                if (rows.getString("TABLE_NAME").equals("LOCAL_FILE") && column.equals("path")
                        && (rows.getLong("CHARACTER_MAXIMUM_LENGTH") != 700 || !"utf8mb4_0900_bin".equals(rows.getString("COLLATION_NAME")))) throw new SQLException("Schema incompatível: representação de caminho", "42000");
            }
        }
    }

    /**
     * Prepara cinco identidades fixas uma vez; exclusões posteriores não são revertidas.
     *
     * @param database conexão compartilhada
     * @throws SQLException se alguma etapa falhar; marcador só é gravado no final
     */
    private void seedTags(DatabaseConnection database) throws SQLException {
        try (Statement s = database.getConnection().createStatement(); ResultSet r = s.executeQuery("SELECT value FROM APP_METADATA WHERE setting='predefined.complete'")) { if (r.next()) return; }
        TagDAO tags = new TagDAO(database);
        String[] names = {"Etiqueta Ausente", "Imagens", "PDF", "Audios", "Videos"};
        List<Set<String>> extensions = List.of(Set.of(), Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp"), Set.of(".pdf"), Set.of(".mp3", ".wav", ".flac", ".aac"), Set.of(".mp4", ".mkv", ".avi", ".mov"));
        for (int i = 0; i < names.length; i++) {
            UUID id = new UUID(0, i + 1);
            Tag tag = new Tag(id, names[i], i == 0 ? "#808080" : "#2864B4", extensions.get(i), EntityFactory.now(), null, true);
            if (tags.findById(id).isEmpty()) tags.create(tag);
            // Retoma inclusive extensões faltantes de uma carga inicial interrompida.
            try (PreparedStatement s = database.getConnection().prepareStatement("INSERT INTO TAG_EXTENSION(tag_id,extension) VALUES(?,?) ON DUPLICATE KEY UPDATE extension=VALUES(extension)")) {
                for (String extension : tag.getExtensions()) { s.setString(1, id.toString()); s.setString(2, extension); s.executeUpdate(); }
            }
        }
        try (Statement s = database.getConnection().createStatement()) { s.executeUpdate("INSERT INTO APP_METADATA(setting,value) VALUES('predefined.complete','true')"); }
    }

    /**
     * Encerra somente a instância identificada; fechar JDBC antes desta chamada.
     *
     * @throws IOException se identidade não conferir ou parada falhar
     * @throws InterruptedException se espera for interrompida
     */
    public void stop() throws IOException, InterruptedException { runScript("stop", false); }
}
