import persistence.DatabaseConnection;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Comparator;

/** Verifica primeira execucao sem banco nem alteracao de arquivos do projeto. */
public final class DatabaseConfigurationCheck {
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
        System.out.println("PASS: " + message);
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("tag-file config [A] '");
        Path file = root.resolve("database.properties");
        Path example = root.resolve("database.properties.example");
        try {
            try { new DatabaseConnection(file); throw new AssertionError("Ausencia ignorada"); }
            catch (IOException expected) {
                check(expected.getMessage().contains("Configuração JDBC não encontrada"), "Mensagem explica arquivo ausente, nao apenas caminho");
                check(expected.getCause() instanceof NoSuchFileException, "Causa original preservada");
            }
            Files.copy(Path.of("database/config/database.properties.example"), example);
            DatabaseConnection connection = DatabaseConnection.prepareLocalConfiguration(file);
            check(Files.mismatch(file, example) == -1, "Primeira execucao cria configuracao a partir do modelo");
            String customized = Files.readString(file).replace("TagFile123!", "senha-personalizada-de-teste");
            Files.writeString(file, customized);
            DatabaseConnection.prepareLocalConfiguration(file);
            check(Files.readString(file).equals(customized), "Configuracao existente preservada");
            Files.writeString(file, "db.password=segredo-que-nao-deve-aparecer\n");
            try { DatabaseConnection.prepareLocalConfiguration(file); throw new AssertionError("Configuracao invalida aceita"); }
            catch (IOException expected) {
                check(expected.getMessage().contains("Configuração JDBC inválida"), "Configuracao invalida identificada");
                check(!expected.getMessage().contains("segredo-que-nao-deve-aparecer"), "Mensagem nao revela credenciais");
            }
            check(Files.readString(file).contains("segredo-que-nao-deve-aparecer"), "Arquivo invalido nao sobrescrito");
            Files.delete(file); Files.delete(example);
            try { DatabaseConnection.prepareLocalConfiguration(file); throw new AssertionError("Modelo ausente ignorado"); }
            catch (IOException expected) {
                check(expected.getMessage().contains("database.properties.example"), "Modelo ausente identificado pelo caminho");
            }
            check(!Files.exists(file), "Sem modelo nao cria configuracao incompleta");
            Files.copy(Path.of("database/config/database.properties.example"), example);
            connection = DatabaseConnection.prepareLocalConfiguration(file);
            if (args.length > 0 && args[0].equals("driver-present")) {
                connection.verifyDriver();
                check(true, "Driver JDBC encontrado sem abrir conexao SQL");
            } else {
                try { connection.verifyDriver(); throw new AssertionError("Driver ausente ignorado"); }
                catch (SQLException expected) {
                    check(expected.getMessage().contains("classpath"), "Driver ausente tem orientacao sobre classpath");
                }
            }
            connection.close();
            System.out.println("DATABASE-CONFIGURATION: " + checks + " verificacoes passaram.");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
