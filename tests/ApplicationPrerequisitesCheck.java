import application.Application;
import GUI.SwingInteraction;
import persistence.DatabaseManager;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Comparator;
import javax.swing.SwingUtilities;

/** Executa a entrada da aplicacao em pasta temporaria; nunca inicia MySQL. */
public final class ApplicationPrerequisitesCheck {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("tag-file-prerequisites-");
        int checks = 0;
        try {
            Path scripts = Files.createDirectories(root.resolve("database/scripts"));
            Path config = Files.createDirectories(root.resolve("database/config"));
            for (String platform : new String[]{"linux", "windows"}) Files.createDirectories(scripts.resolve(platform));
            // Se houver chamada prematura, o script falha e identifica a ordem incorreta.
            Files.writeString(scripts.resolve("linux/check.sh"), "exit 71\n");
            Files.writeString(scripts.resolve("windows/check.ps1"), "exit 71\n");
            try { new Application(root, new SwingInteraction(null), null).start(); throw new AssertionError("Modelo ausente aceito"); }
            catch (IOException expected) {
                if (!expected.getMessage().contains("Configuração JDBC não encontrada")) throw expected;
                checks++;
            }
            if (Files.exists(root.resolve("database/runtime/logs"))) throw new AssertionError("Script executado antes de validar configuracao");
            checks++;
            Files.copy(Path.of("database/config/database.properties.example"), config.resolve("database.properties.example"));
            boolean driverPresent = args.length > 0 && args[0].equals("driver-present");
            try { new Application(root, new SwingInteraction(null), null).start(); throw new AssertionError("Fixture deveria interromper startup"); }
            catch (SQLException expected) {
                if (driverPresent || !expected.getMessage().contains("classpath")) throw expected;
                if (Files.exists(root.resolve("database/runtime/logs"))) throw new AssertionError("Driver ausente iniciou scripts");
                checks++;
            } catch (DatabaseManager.ScriptFailure expected) {
                if (!driverPresent || !expected.getMessage().contains("71")) throw expected;
                checks++;
            }
            if (!Files.exists(config.resolve("database.properties"))) throw new AssertionError("Configuracao inicial nao criada");
            checks++;
            SwingUtilities.invokeAndWait(() -> {
                for (java.awt.Window window : java.awt.Window.getWindows()) {
                    if (window.isShowing()) throw new AssertionError("Janela de preparacao permaneceu aberta apos erro");
                }
            });
            checks++;
            System.out.println("APPLICATION-PREREQUISITES: " + checks + " verificacoes; ordem de startup, sem MySQL.");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
