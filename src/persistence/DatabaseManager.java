/*
 * Inventário — DatabaseManager.
 * Administra scripts e schema da instância exclusiva, separado da conexão (AMB-01/07).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - DatabaseManager.DatabaseManager(Path root): Define a raiz que contém configuração, scripts e runtime.
 * - DatabaseManager.DatabaseManager(Path root, Installer installer): Injeta a execução autorizada da instalação.
 * - Installer.install(Path script, String authorizationArgument): Executa o instalador Linux após autorização nativa.
 * - DatabaseManager.inspect(): Inspeciona configuração, driver e executáveis; não instala/inicia/encerra servidor.
 * - DatabaseManager.prepare(boolean installationAuthorized): Verifica componentes; instalar requer autorização explícita do chamador/UI.
 * - DatabaseManager.runScript(String action, boolean authorized): Executa apenas scripts distribuídos; credenciais nunca vão nos argumentos.
 * - DatabaseManager.prepareSchema(DatabaseConnection database): Confirma identidade da instância antes de aplicar DDL; não usa transações explícitas.
 * - DatabaseManager.verifyInstance(DatabaseConnection database): Verifica porta e diretório reais do servidor, sem confiar somente no endereço JDBC.
 * - DatabaseManager.executeSql(DatabaseConnection database, Path script): Aplica SQL confiável do projeto, separado por ponto e vírgula; não aceita entrada do usuário.
 * - DatabaseManager.validateSchema(DatabaseConnection database): Confere contratos e compara nomes conforme a política do servidor, preservando o schema.
 * - DatabaseManager.schemaIdentifier(String identifier, boolean foldTableNames): Adapta somente nomes de tabelas e catálogos para comparação.
 * - DatabaseManager.schemaRelation(ResultSet rows, boolean foldTableNames): Representa vínculos JDBC com catálogo, colunas, ordem e regra de exclusão.
 * - DatabaseManager.schemaFailure(DatabaseMetaData metadata, int caseMode, List expected, List actual, List reported): Registra as diferenças sem modificar o banco.
 * - DatabaseManager.seedTags(DatabaseConnection database): Prepara cinco identidades fixas uma vez; exclusões posteriores não são revertidas.
 * - DatabaseManager.stop(): Encerra somente a instância identificada; fechar JDBC antes desta chamada.
 *
 * Consulte: doc/banco-de-dados.md — SQL-01 a SQL-06; doc/instalacao-e-execucao.md — AMB-01 a AMB-07.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package persistence;

import model.*;
import service.PreparationProgress;
import service.ScriptProgressMonitor;
import java.util.function.Consumer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    /** Executor elevado para Linux; a instalação portátil Windows usa a conta atual. */
    private final Installer installer;
    /** Atualizações curtas da preparação, sem executar trabalho gráfico nesta thread. */
    private Consumer<PreparationProgress> progressListener = progress -> { };

    /**
     * Define o destino do progresso antes de preparar a instância.
     * @param listener observador que encaminha atualizações à interface
     */
    public void setProgressListener(Consumer<PreparationProgress> listener) {
        progressListener = Objects.requireNonNull(listener);
    }

    /** Fronteira de elevação usada pela instalação Linux. */
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
     * @param installer executor nativo Linux; Windows sempre usa a conta atual
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
        catch (ScriptFailure missing) {
            if (missing.exitCode != 4) throw missing;
            if (!installationAuthorized) throw new InstallationRequiredException(missing);
            runScript("install", true);
        }
        runScript("initialize", false); runScript("start", false);
    }

    /** Sinaliza apenas componentes ausentes, permitindo solicitar consentimento na UI. */
    public static final class InstallationRequiredException extends IOException {
        /**
         * Preserva a causa da ausência dos componentes.
         * @param cause diagnóstico da verificação dos componentes
         */
        private InstallationRequiredException(Throwable cause) {
            super("Componentes ausentes; instalação não autorizada. Nada foi instalado.", cause);
        }
    }

    /** Preserva o código do script para distinguir ausência de falhas de execução. */
    public static final class ScriptFailure extends IOException {
        /** Código retornado pelo processo PowerShell ou Bash. */
        private final int exitCode;
        /** Arquivo persistente com diagnóstico completo. */
        private final Path log;
        /** Retorna o log sem extrair caminhos da mensagem de erro.
         * @return arquivo de diagnóstico
         */
        public Path getLog() { return log; }
        /**
         * Associa o diagnóstico ao código de saída do processo.
         * @param exitCode código retornado pelo script
         * @param message etapa, log e diagnóstico do processo
         * @param log arquivo persistente do processo
         */
        private ScriptFailure(int exitCode, String message, Path log) {
            super(message);
            this.exitCode = exitCode;
            this.log = log;
        }
    }

    /** Incompatibilidade de vínculos com detalhes SQL e, quando possível, log persistente. */
    public static final class SchemaFailure extends SQLException {
        /** Caminho do relatório, ou null quando sua gravação falhar. */
        private final Path log;

        /**
         * Preserva o diagnóstico mesmo sem permissão para gravar o log.
         * @param detail metadados esperados e encontrados, sem credenciais
         * @param log relatório persistente, ou null se indisponível
         */
        private SchemaFailure(String detail, Path log) {
            super("Schema incompatível: os vínculos entre tabelas ou suas regras de exclusão diferem do esperado."
                    + "\nO Tag-File não alterou esses vínculos. Consulte os detalhes técnicos antes de modificar o banco."
                    + (log == null ? "\nNão foi possível gravar o log." : "\nLog: " + log),
                    "42000", new SQLException(detail, "42000"));
            this.log = log;
        }

        /**
         * Disponibiliza o relatório à interface sem extrair caminhos da mensagem.
         * @return arquivo de diagnóstico, ou null quando não pôde ser gravado
         */
        public Path getLog() { return log; }
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
        String stage = switch (action) {
            case "check" -> "Verificando componentes do MySQL";
            case "install" -> "Preparando instalação do MySQL";
            case "initialize" -> "Preparando dados do banco";
            case "start" -> "Iniciando servidor MySQL";
            default -> "Encerrando servidor MySQL";
        };
        progressListener.accept(new PreparationProgress(stage, -1, null));
        if (action.equals("install") && !windows && installer != null) {
            if (!authorized) throw new IOException("Instalação não autorizada");
            installer.install(script, "--authorized");
            return "Instalação autorizada concluída";
        }
        List<String> command = new ArrayList<>(windows ? List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", script.toString()) : List.of("bash", script.toString()));
        if (authorized) command.add(windows ? "-Authorized" : "--authorized");
        Files.createDirectories(root.resolve("database/runtime/logs"));
        Path log = Files.createTempFile(root.resolve("database/runtime/logs"), action + "-", ".log");
        // Identifica a cópia executada, mesmo se o PowerShell falhar antes de carregar o script.
        String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(script))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        Files.writeString(log, "Script: " + script + "\nSHA-256: " + hash + "\n", StandardCharsets.UTF_8);
        System.out.println("Preparando MySQL: " + action + "\nScript: " + script + "\nSHA-256: " + hash + "\nLog: " + log);
        progressListener.accept(new PreparationProgress(stage, -1, log));
        Process process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile())).start();
        try (ScriptProgressMonitor monitor = new ScriptProgressMonitor(log, progressListener)) {
            long deadline = System.nanoTime() + Duration.ofMinutes(action.equals("install") ? 15 : 2).toNanos();
            while (!process.waitFor(150, TimeUnit.MILLISECONDS)) {
                monitor.poll();
                if (System.nanoTime() >= deadline) {
                    process.destroy(); throw new ScriptFailure(-1, "Tempo excedido no script " + action + "; consulte " + log, log);
                }
            }
            monitor.poll();
        } catch (InterruptedException e) { process.destroy(); Thread.currentThread().interrupt(); throw e; }
        catch (IOException e) { process.destroy(); throw e; }
        String output = Files.readString(log, StandardCharsets.UTF_8);
        if (process.exitValue() != 0) throw new ScriptFailure(process.exitValue(), "Script " + action + ": código de saída " + process.exitValue() + "\nLog: " + log + "\n" + output, log);
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
     * Confere colunas, chaves, cascatas, índice e caminhos conforme a política de nomes do servidor.
     * A validação não modifica tabelas nem dados; incompatibilidades reais exigem revisão.
     *
     * @param database conexão compartilhada
     * @throws SQLException se schema não atender os contratos utilizados pelos DAOs
     */
    private void validateSchema(DatabaseConnection database) throws SQLException {
        int lowerCaseTableNames;
        try (Statement statement = database.getConnection().createStatement();
             ResultSet rows = statement.executeQuery("SELECT @@lower_case_table_names")) {
            rows.next();
            lowerCaseTableNames = rows.getInt(1);
        }
        // Segue o servidor, não o sistema operacional que executa o cliente Java.
        boolean foldTableNames = lowerCaseTableNames != 0;
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
        // A lista também detecta duplicatas; não descarta vínculos com regra diferente de CASCADE.
        List<String> relations = new ArrayList<>(), reported = new ArrayList<>();
        for (String table : new TreeSet<>(primaryKeys.keySet())) {
            try (ResultSet rows = metadata.getImportedKeys("tag_file", null, table)) {
                while (rows.next()) {
                    relations.add(schemaRelation(rows, foldTableNames));
                    reported.add(rows.getString("FK_NAME") + ": " + schemaRelation(rows, false));
                }
            }
        }
        String catalog = schemaIdentifier("tag_file", foldTableNames) + ".";
        List<String> expectedRelations = new ArrayList<>();
        for (String pair : List.of("TAG_EXTENSION.tag_id>TAG.id", "LOCAL_FILE_TAG.tag_id>TAG.id", "LOCAL_FILE_TAG.file_id>LOCAL_FILE.id")) {
            String[] ends = pair.split(">");
            expectedRelations.add(catalog + ends[0] + " -> " + catalog + ends[1] + " [KEY_SEQ=1; ON DELETE=CASCADE]");
        }
        Collections.sort(relations); Collections.sort(expectedRelations);
        if (!relations.equals(expectedRelations)) throw schemaFailure(metadata, lowerCaseTableNames, expectedRelations, relations, reported);
        try (Statement statement = database.getConnection().createStatement(); ResultSet rows = statement.executeQuery("SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,DATETIME_PRECISION,CHARACTER_MAXIMUM_LENGTH,COLLATION_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE()")) {
            while (rows.next()) {
                String column = rows.getString("COLUMN_NAME");
                if (Set.of("created_at", "modified_at", "last_accessed_at", "last_file_tagged_at").contains(column)
                        && (!rows.getString("DATA_TYPE").equals("datetime") || rows.getInt("DATETIME_PRECISION") != 6)) throw new SQLException("Schema incompatível: data precisa ser DATETIME(6): " + column, "42000");
                if (schemaIdentifier(rows.getString("TABLE_NAME"), foldTableNames).equals("LOCAL_FILE") && column.equals("path")
                        && (rows.getLong("CHARACTER_MAXIMUM_LENGTH") != 700 || !"utf8mb4_0900_bin".equals(rows.getString("COLLATION_NAME")))) throw new SQLException("Schema incompatível: representação de caminho", "42000");
            }
        }
    }

    /**
     * Compara identificadores conforme o servidor; nunca normaliza caminhos de arquivos.
     * @param identifier nome de tabela ou catálogo retornado pelo JDBC
     * @param foldTableNames indica comparação sem distinguir maiúsculas no servidor
     * @return identificador para comparação, preservado no modo sensível a maiúsculas
     */
    private static String schemaIdentifier(String identifier, boolean foldTableNames) {
        if (identifier == null) return "<ausente>";
        return foldTableNames ? identifier.toUpperCase(Locale.ROOT) : identifier;
    }

    /**
     * Representa cada coluna de uma FK, incluindo catálogo, ordem e regra de exclusão.
     * @param rows linha corrente de getImportedKeys
     * @param foldTableNames política de comparação de tabelas e catálogos
     * @return relação comparável sem depender do nome dado à constraint
     * @throws SQLException se metadados não puderem ser lidos
     */
    private static String schemaRelation(ResultSet rows, boolean foldTableNames) throws SQLException {
        short rule = rows.getShort("DELETE_RULE");
        String deletion = switch (rule) {
            case DatabaseMetaData.importedKeyCascade -> "CASCADE";
            case DatabaseMetaData.importedKeyRestrict -> "RESTRICT";
            case DatabaseMetaData.importedKeyNoAction -> "NO ACTION";
            case DatabaseMetaData.importedKeySetNull -> "SET NULL";
            case DatabaseMetaData.importedKeySetDefault -> "SET DEFAULT";
            default -> "DESCONHECIDA(" + rule + ")";
        };
        return schemaIdentifier(rows.getString("FKTABLE_CAT"), foldTableNames) + "."
                + schemaIdentifier(rows.getString("FKTABLE_NAME"), foldTableNames) + "." + rows.getString("FKCOLUMN_NAME")
                + " -> " + schemaIdentifier(rows.getString("PKTABLE_CAT"), foldTableNames) + "."
                + schemaIdentifier(rows.getString("PKTABLE_NAME"), foldTableNames) + "." + rows.getString("PKCOLUMN_NAME")
                + " [KEY_SEQ=" + rows.getShort("KEY_SEQ") + "; ON DELETE=" + deletion + "]";
    }

    /**
     * Registra diferenças sem alterar o schema nem ocultar erros de gravação do relatório.
     * @param metadata versões do servidor e do driver
     * @param caseMode lower_case_table_names lido do servidor
     * @param expected relações exigidas pelos DAOs
     * @param actual relações encontradas, normalizadas só para comparação
     * @param reported nomes originais retornados pelo JDBC, incluindo constraints
     * @return falha SQL com detalhes e eventual caminho do log
     * @throws SQLException se as versões dos componentes não puderem ser consultadas
     */
    private SchemaFailure schemaFailure(DatabaseMetaData metadata, int caseMode, List<String> expected,
                                        List<String> actual, List<String> reported) throws SQLException {
        List<String> missing = new ArrayList<>(expected), unexpected = new ArrayList<>(actual);
        // Remove uma ocorrência por vez para manter visíveis constraints duplicadas.
        for (String relation : actual) missing.remove(relation);
        for (String relation : expected) unexpected.remove(relation);
        String detail = "MySQL: " + metadata.getDatabaseProductVersion() + "\nJDBC: " + metadata.getDriverVersion()
                + "\nlower_case_table_names=" + caseMode + "\nCatálogo: tag_file"
                + "\nEsperado:\n" + String.join("\n", expected)
                + "\nEncontrado (JDBC):\n" + String.join("\n", reported)
                + "\nAusente: " + missing + "\nInesperado: " + unexpected + "\n";
        Path log = null;
        IOException logFailure = null;
        try {
            Path directory = Files.createDirectories(root.resolve("database/runtime/logs"));
            Path candidate = Files.createTempFile(directory, "schema-", ".log");
            Files.writeString(candidate, detail, StandardCharsets.UTF_8);
            log = candidate;
        } catch (IOException failure) { logFailure = failure; }
        SchemaFailure failure = new SchemaFailure(detail, log);
        if (logFailure != null) failure.addSuppressed(logFailure);
        return failure;
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
