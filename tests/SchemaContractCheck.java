import persistence.DatabaseConnection;
import persistence.DatabaseManager;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.time.LocalDateTime;
import model.*;
import persistence.TagDAO;
import persistence.LocalFileDAO;
import persistence.LocalFileTagDAO;

/** Usa somente o MySQL descartável iniciado por schema-contract.py. */
public final class SchemaContractCheck {
    private static int checks;
    private static final List<String> failures = new ArrayList<>();
    private static Connection connection;
    private static DatabaseConnection database;
    private static DatabaseManager manager;
    private static Path fixture;
    private static String ddl;

    public static void main(String[] args) throws Exception {
        Path repository = Path.of(args[0]);
        fixture = Path.of(args[1]);
        int port = Integer.parseInt(args[2]);
        int mode = Integer.parseInt(args[3]);
        if (port == 3333) throw new AssertionError("Não usar a porta da aplicação");
        try (Connection opened = DriverManager.getConnection("jdbc:mysql://127.0.0.1:" + port
                + "/?sslMode=DISABLED&allowPublicKeyRetrieval=true", "root", "")) {
            connection = opened;
            // Confirma a identidade antes de criar qualquer schema de teste.
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT @@datadir,@@lower_case_table_names")) {
                rows.next();
                if (!Path.of(rows.getString(1)).normalize().equals(fixture.resolve("data"))
                        || rows.getInt(2) != mode) throw new AssertionError("Servidor não pertence ao teste");
            }
            ddl = Files.readString(repository.resolve("database/schema/001-create.sql"))
                    .replaceAll("(?m)^\\s*--.*$", "");
            reset();
            Files.createDirectories(fixture.resolve("database/scripts"));
            Path configuration = fixture.resolve("database.properties");
            Files.copy(repository.resolve("database/config/database.properties.example"), configuration);
            database = new DatabaseConnection(configuration);
            // Injeta só no teste: a produção continua restrita à porta 3333 e ao datadir do projeto.
            Field field = DatabaseConnection.class.getDeclaredField("connection");
            field.setAccessible(true);
            field.set(database, connection);
            for (String table : new String[]{"TAG_EXTENSION", "LOCAL_FILE_TAG"}) {
                try (ResultSet rows = connection.getMetaData().getImportedKeys("tag_file", null, table)) {
                    while (rows.next()) System.out.println("JDBC: " + rows.getString("FKTABLE_NAME")
                            + "." + rows.getString("FKCOLUMN_NAME") + " -> " + rows.getString("PKTABLE_NAME")
                            + "." + rows.getString("PKCOLUMN_NAME") + "; DELETE_RULE=" + rows.getShort("DELETE_RULE"));
                }
            }
            manager = new DatabaseManager(fixture);
            check("schema original e validação repetida preservam estrutura", () -> {
                List<String> before = structure();
                validate(manager, database);
                validate(manager, database);
                require(before.equals(structure()), "DDL mudou durante a validação");
            });
            if (args.length < 5 || !args[4].equals("--probe")) runCases(mode);
            System.out.println("SCHEMA-CONTRACT: modo " + mode + "; " + checks + " passaram; " + failures.size() + " falharam");
            if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
        }
    }

    private static void runCases(int mode) throws Exception {
        check("DAOs, cascatas e preservação de arquivos/Tags/capitalização", () -> {
            reset();
            Path file = fixture.resolve("Arquivo.txt");
            Files.writeString(file, "conteúdo preservado");
            TagDAO tags = new TagDAO(database);
            LocalFileTagDAO links = new LocalFileTagDAO(database, tags);
            LocalFileDAO files = new LocalFileDAO(database, links);
            Tag first = new Tag(UUID.randomUUID(), "MiXeD", "#ABCDEF", Set.of("txt"), LocalDateTime.now(), null, false);
            Tag second = new Tag(UUID.randomUUID(), "Outra", "#012345", Set.of("txt"), LocalDateTime.now(), null, false);
            tags.create(first); tags.create(second);
            UUID fileId = UUID.randomUUID();
            files.create(new LocalFile(fileId, new NativeFile(file), true, 1L, null, null, null, Set.of()));
            UUID lowerId = UUID.randomUUID();
            files.create(new LocalFile(lowerId, new NativeFile(fixture.resolve("arquivo.txt")), true, 1L, null, null, null, Set.of()));
            links.associate(fileId, first.getId()); links.associate(fileId, second.getId());
            validate(manager, database);
            require(files.findById(fileId).orElseThrow().getNativeFile().getPath().equals(file), "Caminho foi normalizado indevidamente");
            require(files.findById(lowerId).isPresent(), "Caminhos com capitalização diferente foram mesclados");
            require(tags.findById(first.getId()).orElseThrow().getName().equals("MiXeD"), "Nome da Tag foi alterado");
            tags.delete(first.getId());
            require(files.findById(fileId).orElseThrow().getTags().size() == 1, "Exclusão de Tag não preservou o outro vínculo");
            require(count("TAG_EXTENSION") == 1, "Cascata das extensões falhou");
            files.delete(fileId);
            require(count("LOCAL_FILE_TAG") == 0 && tags.findById(second.getId()).isPresent(), "Cascata de arquivo removeu Tag ou deixou vínculos");
            require(Files.readString(file).equals("conteúdo preservado"), "Arquivo físico foi alterado");
        });
        incompatible("FK ausente", () -> dropKey("LOCAL_FILE_TAG", "file_id"), true);
        incompatible("RESTRICT no lugar de CASCADE", () -> {
            dropKey("LOCAL_FILE_TAG", "file_id");
            sql("ALTER TABLE LOCAL_FILE_TAG ADD CONSTRAINT changed_fk FOREIGN KEY(file_id) REFERENCES LOCAL_FILE(id) ON DELETE RESTRICT");
        }, true);
        incompatible("destino incorreto", () -> {
            dropKey("LOCAL_FILE_TAG", "file_id");
            sql("ALTER TABLE LOCAL_FILE_TAG ADD CONSTRAINT changed_fk FOREIGN KEY(file_id) REFERENCES TAG(id) ON DELETE CASCADE");
        }, true);
        incompatible("coluna de origem incorreta", () -> {
            dropKey("LOCAL_FILE_TAG", "file_id");
            sql("ALTER TABLE LOCAL_FILE_TAG ADD other_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin, ADD CONSTRAINT changed_fk FOREIGN KEY(other_id) REFERENCES LOCAL_FILE(id) ON DELETE CASCADE");
        }, true);
        incompatible("FK extra sem CASCADE", () -> sql("ALTER TABLE LOCAL_FILE_TAG ADD CONSTRAINT extra_fk FOREIGN KEY(file_id) REFERENCES TAG(id) ON DELETE RESTRICT"), true);
        incompatible("FK extra em tabela principal", () -> sql("ALTER TABLE TAG ADD CONSTRAINT extra_fk FOREIGN KEY(id) REFERENCES LOCAL_FILE(id) ON DELETE CASCADE"), true);
        incompatible("FK duplicada", () -> sql("ALTER TABLE LOCAL_FILE_TAG ADD CONSTRAINT duplicate_fk FOREIGN KEY(file_id) REFERENCES LOCAL_FILE(id) ON DELETE CASCADE"), true);
        incompatible("catálogo de destino incorreto", () -> {
            sql("CREATE DATABASE other_schema");
            sql("CREATE TABLE other_schema.TAG LIKE TAG");
            dropKey("LOCAL_FILE_TAG", "tag_id");
            sql("ALTER TABLE LOCAL_FILE_TAG ADD CONSTRAINT changed_fk FOREIGN KEY(tag_id) REFERENCES other_schema.TAG(id) ON DELETE CASCADE");
        }, true);
        incompatible("FK composta adicional", () -> {
            sql("ALTER TABLE LOCAL_FILE_TAG ADD extension VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin");
            sql("ALTER TABLE LOCAL_FILE_TAG ADD CONSTRAINT composite_fk FOREIGN KEY(tag_id,extension) REFERENCES TAG_EXTENSION(tag_id,extension) ON DELETE CASCADE");
        }, true);
        incompatible("path com tamanho incorreto", () -> sql("ALTER TABLE LOCAL_FILE MODIFY path VARCHAR(699) NOT NULL"), false);
        incompatible("path com collation incorreta", () -> sql("ALTER TABLE LOCAL_FILE MODIFY path VARCHAR(700) COLLATE utf8mb4_0900_ai_ci NOT NULL"), false);
        incompatible("precisão temporal incorreta", () -> sql("ALTER TABLE LOCAL_FILE MODIFY created_at DATETIME(3) NULL"), false);
        incompatible("índice único ausente", () -> sql("ALTER TABLE LOCAL_FILE DROP INDEX unique_path"), false);
        if (mode == 0) incompatible("TAG e tag continuam tabelas distintas no Linux", () -> {
            sql("CREATE TABLE tag LIKE TAG");
            dropKey("LOCAL_FILE_TAG", "tag_id");
            sql("ALTER TABLE LOCAL_FILE_TAG ADD CONSTRAINT changed_fk FOREIGN KEY(tag_id) REFERENCES tag(id) ON DELETE CASCADE");
        }, true);
        check("falha ao gravar log preserva diagnóstico SQL", () -> {
            reset(); dropKey("LOCAL_FILE_TAG", "file_id");
            Path blocked = fixture.resolve("blocked-log");
            Files.createDirectories(blocked.resolve("database/scripts"));
            Files.writeString(blocked.resolve("database/runtime"), "impede criar diretório de logs");
            SQLException failure = rejection(new DatabaseManager(blocked));
            require(failure.getSuppressed().length > 0, "Falha de gravação do log não foi preservada");
            require(failure.getCause().getMessage().contains("Esperado"), "Diagnóstico técnico perdido");
        });
    }

    private static void incompatible(String name, Action mutation, boolean diagnostic) throws Exception {
        check(name, () -> {
            reset(); mutation.run();
            List<String> before = structure();
            SQLException failure = rejection(manager);
            require("42000".equals(failure.getSQLState()), "SQLState não identifica contrato inválido");
            require(before.equals(structure()), "Validador tentou reparar o schema automaticamente");
            if (diagnostic) {
                Method getter = failure.getClass().getMethod("getLog");
                Path log = (Path) getter.invoke(failure);
                String report = Files.readString(log);
                for (String expected : new String[]{"lower_case_table_names=", "MySQL:", "JDBC:", "Esperado", "Encontrado", "Ausente", "Inesperado"})
                    require(report.contains(expected), "Diagnóstico sem " + expected);
                require(failure.getMessage().contains(log.toString()), "Popup não indica log");
                require(failure.getCause().getMessage().contains("Encontrado"), "Detalhes técnicos não disponíveis");
            }
        });
    }

    private static SQLException rejection(DatabaseManager target) throws Exception {
        try { validate(target, database); }
        catch (SQLException expected) { return expected; }
        throw new AssertionError("Schema incompatível foi aceito");
    }

    private static void reset() throws Exception {
        connection.setCatalog("mysql");
        sql("DROP DATABASE IF EXISTS tag_file");
        sql("DROP DATABASE IF EXISTS other_schema");
        sql("CREATE DATABASE tag_file CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin");
        connection.setCatalog("tag_file");
        for (String part : ddl.split(";")) if (!part.isBlank()) sql(part);
    }

    private static void dropKey(String table, String column) throws Exception {
        String name = null;
        try (ResultSet rows = connection.getMetaData().getImportedKeys("tag_file", null, table)) {
            while (rows.next()) if (rows.getString("FKCOLUMN_NAME").equals(column)) name = rows.getString("FK_NAME");
        }
        if (name == null) throw new AssertionError("FK de teste não encontrada");
        sql("ALTER TABLE " + table + " DROP FOREIGN KEY " + quote(name));
    }

    private static List<String> structure() throws Exception {
        List<String> tables = new ArrayList<>(), result = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SHOW TABLES")) {
            while (rows.next()) tables.add(rows.getString(1));
        }
        for (String table : tables) {
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SHOW CREATE TABLE " + quote(table))) {
                rows.next(); result.add(rows.getString(2));
            }
        }
        return result;
    }

    private static long count(String table) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + quote(table))) {
            rows.next(); return rows.getLong(1);
        }
    }

    private static String quote(String name) { return "`" + name.replace("`", "``") + "`"; }
    private static void sql(String query) throws Exception {
        try (Statement statement = connection.createStatement()) { statement.execute(query); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void check(String name, Action test) {
        try { test.run(); checks++; System.out.println("PASS: " + name); }
        catch (Exception | AssertionError failure) {
            failures.add(name + ": " + failure);
            System.out.println("FAIL: " + failures.getLast());
        }
    }

    private static void validate(DatabaseManager manager, DatabaseConnection database) throws Exception {
        Method method = DatabaseManager.class.getDeclaredMethod("validateSchema", DatabaseConnection.class);
        method.setAccessible(true);
        try { method.invoke(manager, database); }
        catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Exception cause) throw cause;
            throw failure;
        }
    }
}
