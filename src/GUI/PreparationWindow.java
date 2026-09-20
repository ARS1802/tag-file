package GUI;

import service.PreparationProgress;
import service.ActionGate;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;

/** Janela de inicialização responsiva, anterior à janela principal do explorador. */
public final class PreparationWindow implements AutoCloseable {
    /** Janela modeless; o trabalho continua fora da EDT. */
    private final JDialog dialog = new JDialog((java.awt.Frame) null, "Preparando o Tag-File", false);
    /** Descrição da etapa. */
    private final JLabel status = new JLabel();
    /** Porcentagem real da transferência ou animação indeterminada. */
    private final JProgressBar bar = new JProgressBar(0, 100);
    /** Último log, selecionável e copiável. */
    private final JTextField logPath = new JTextField(52);
    /** Animação controlada pela EDT, independente dos processos externos. */
    private final Timer timer;
    /** Estado corrente da preparação. */
    private PreparationProgress state = new PreparationProgress("Verificando ambiente", -1, null);
    /** Contador dos pontos animados. */
    private int dots;
    /** Impede atualizações depois de liberar a janela. */
    private boolean closed;

    /**
     * Cria os componentes na EDT.
     * @param logDirectory pasta persistente dos diagnósticos
     */
    private PreparationWindow(Path logDirectory) {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        panel.add(status, BorderLayout.NORTH);
        panel.add(bar, BorderLayout.CENTER);
        JPanel details = new JPanel(new BorderLayout(4, 4));
        logPath.setEditable(false);
        logPath.setText(logDirectory.toString());
        details.add(logPath, BorderLayout.NORTH);
        details.add(SwingInteraction.logActions(() -> state.log() == null ? logDirectory : state.log()), BorderLayout.SOUTH);
        panel.add(details, BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        timer = new Timer(400, event -> refresh());
        refresh(); timer.start();
        dialog.pack(); dialog.setLocationRelativeTo(null); dialog.setVisible(true);
    }

    /**
     * Abre a janela antes das operações longas.
     * @param logDirectory pasta dos logs
     * @return janela pronta para receber progresso
     * @throws Exception se a criação Swing falhar
     */
    public static PreparationWindow open(Path logDirectory) throws Exception {
        AtomicReference<PreparationWindow> result = new AtomicReference<>();
        ActionGate.onEdt(() -> result.set(new PreparationWindow(logDirectory)));
        return result.get();
    }

    /**
     * Agenda uma atualização curta; pode ser chamada pela thread de preparação.
     * @param progress etapa e porcentagem atual
     */
    public void update(PreparationProgress progress) {
        SwingUtilities.invokeLater(() -> {
            if (closed) return;
            state = progress; dots = 0;
            if (progress.log() != null) logPath.setText(progress.log().toString());
            refresh();
        });
    }

    /** Atualiza o texto e a barra sem atribuir porcentagem fictícia às esperas. */
    private void refresh() {
        boolean waiting = state.percent() < 0;
        status.setText(state.message() + (waiting ? ".".repeat(dots++ % 3 + 1) : ""));
        bar.setIndeterminate(waiting);
        bar.setStringPainted(!waiting);
        if (!waiting) { bar.setValue(state.percent()); bar.setString(state.percent() + "%"); }
    }

    /** Para a animação e libera a janela na EDT, inclusive após falha/cancelamento. */
    @Override public void close() {
        SwingUtilities.invokeLater(() -> { closed = true; timer.stop(); dialog.dispose(); });
    }
}
