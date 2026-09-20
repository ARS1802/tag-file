import service.PreparationProgress;
import service.ScriptProgressMonitor;
import GUI.PreparationWindow;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;

/** Verifica progresso incremental e animacao Swing sem servidor ou acesso a rede. */
public final class PreparationProgressCheck {
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
        System.out.println("PASS: " + message);
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("tag-file-progress-");
        Path log = Files.createFile(root.resolve("progress.log"));
        Path transfer = Files.write(root.resolve("package.zip"), new byte[25]);
        List<PreparationProgress> events = new ArrayList<>();
        try {
            try (ScriptProgressMonitor monitor = new ScriptProgressMonitor(log, events::add)) {
                append(log, "diagnostico comum\nTAG_FILE_PROGRESS\tSTAGE\tVerificando acentos: ç");
                monitor.poll(); check(events.isEmpty(), "Linha incompleta nao gera estado parcial");
                append(log, "\n"); monitor.poll();
                check(events.getLast().message().endsWith("ç") && events.getLast().percent() == -1, "Etapa UTF-8 indeterminada");
                append(log, "TAG_FILE_PROGRESS\tTRANSFER\tBaixando MySQL\t100\t" + transfer + "\n");
                monitor.poll(); check(events.getLast().percent() == 25, "Percentual corresponde aos bytes escritos");
                Files.write(transfer, new byte[75]); monitor.poll();
                check(events.getLast().percent() == 75, "Progresso muda enquanto arquivo cresce");
                append(log, "TAG_FILE_PROGRESS\tTRANSFER\tInválido\tzero\tfile\n"); monitor.poll();
                check(events.getLast().percent() == 75, "Evento invalido nao apaga progresso");
                append(log, "X".repeat(10000) + "\nTAG_FILE_PROGRESS\tSTAGE\tExtraindo pacote\n"); monitor.poll();
                check(events.getLast().percent() == -1, "Etapa seguinte usa espera sem percentual ficticio");
            }
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                System.out.println("SKIP: janela Swing requer ambiente grafico");
            } else {
                PreparationWindow window = PreparationWindow.open(root);
                try {
                    window.update(new PreparationProgress("Baixando MySQL", 37, log));
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            var barField = PreparationWindow.class.getDeclaredField("bar"); barField.setAccessible(true);
                            JProgressBar bar = (JProgressBar) barField.get(window);
                            check(bar.getValue() == 37 && !bar.isIndeterminate(), "Barra mostra percentual real na EDT");
                            var pathField = PreparationWindow.class.getDeclaredField("logPath"); pathField.setAccessible(true);
                            check(((JTextField) pathField.get(window)).getText().equals(log.toString()), "Caminho do log disponivel na janela");
                        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                    });
                    window.update(new PreparationProgress("Aguardando banco", -1, log));
                    CountDownLatch animated = new CountDownLatch(1);
                    SwingUtilities.invokeAndWait(() -> {
                        javax.swing.Timer observer = new javax.swing.Timer(900, event -> {
                            try {
                                var labelField = PreparationWindow.class.getDeclaredField("status"); labelField.setAccessible(true);
                                String text = ((JLabel) labelField.get(window)).getText();
                                check(text.startsWith("Aguardando banco.") && text.length() > "Aguardando banco.".length(), "Pontos animam durante espera");
                            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                            animated.countDown();
                        });
                        observer.setRepeats(false); observer.start();
                    });
                    check(animated.await(3, TimeUnit.SECONDS), "EDT permanece responsiva durante preparacao");
                } finally { window.close(); }
                SwingUtilities.invokeAndWait(() -> {
                    for (java.awt.Window w : java.awt.Window.getWindows()) {
                        if (w.getType() != java.awt.Window.Type.POPUP) check(!w.isShowing(), "Janela liberada ao terminar");
                    }
                });
            }
            System.out.println("PREPARATION-PROGRESS: " + checks + " verificacoes passaram.");
        } finally {
            Files.deleteIfExists(log); Files.deleteIfExists(transfer); Files.deleteIfExists(root);
        }
    }
    private static void append(Path log, String text) throws Exception {
        Files.writeString(log, text, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }
}
