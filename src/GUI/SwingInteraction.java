/*
 * Inventário — SwingInteraction.
 * Diálogos Swing na EDT; pode ser chamada pela thread de trabalho sem bloquear a UI.
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - SwingInteraction.SwingInteraction(Component parent): Injeta os colaboradores necessários a SwingInteraction.
 * - SwingInteraction.choose(String message, String[] options): Exibe alternativas na EDT; o loop modal mantém eventos dos diálogos responsivos.
 * - SwingInteraction.text(String message, String initial): Obtém texto por diálogo na EDT, preservando o cancelamento.
 * - SwingInteraction.color(String message, String initial): Abre um seletor de cor nativo em vez de pedir o código hexadecimal digitado.
 * - SwingInteraction.files(boolean multiple, Set<String> extensions): Usa JFileChooser e filtro visual; o Manager ainda valida Drop e extensões compostas.
 * - SwingInteraction.showFailure(Throwable failure): Apresenta falha real com área técnica expansível; senhas de parâmetros são ocultadas.
 * - SwingInteraction.redact(String text): Oculta valores de senha em mensagens técnicas antes de apresentá-las.
 * - SwingInteraction.onEdt(Callable<T> task): Executa diálogo na EDT e aguarda o retorno sem criar outra ação funcional.
 * - SwingInteraction.bind(JComponent component, String stroke, String name, Runnable action): Liga um atalho à mesma entrada usada pelo botão; não contorna o Controller.
 * - AbstractAction (anônima).actionPerformed(ActionEvent event): Encaminha o atalho à mesma entrada funcional usada pelo botão.
 * - SwingInteraction.observe(CompletableFuture<?> future): Trata somente rejeições imediatas; outros erros são apresentados dentro da ação.
 *
 * Consulte: doc/interface-e-fluxos.md — UI-01 a UI-03; doc/arquitetura-e-padroes.md — ARQ-06.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package GUI;

import service.ActionGate;
import persistence.DatabaseManager;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Diálogos Swing na EDT; pode ser chamada pela thread de trabalho sem bloquear a UI.
 */
public final class SwingInteraction implements Interaction {
    /**
     * Componente proprietário dos diálogos, possivelmente nulo.
     */
    private final Component parent;
    /**
     * Injeta os colaboradores necessários a SwingInteraction.
     *
     * @param parent componente proprietário; nulo permite diálogos independentes
     */
    public SwingInteraction(Component parent) { this.parent = parent; }

    /**
     * Exibe alternativas na EDT; o loop modal mantém eventos dos diálogos responsivos.
     *
     * @param message consequências apresentadas
     * @param options alternativas explícitas
     * @return índice ou -1 ao fechar
     */
    @Override public int choose(String message, String... options) {
        return onEdt(() -> JOptionPane.showOptionDialog(parent, message, "Tag-File", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options.length == 0 ? null : options[0]));
    }
    /**
     * Obtém texto por diálogo na EDT, preservando o cancelamento.
     *
     * @param message propósito da entrada
     * @param initial valor inicial
     * @return texto ou nulo ao cancelar
     */
    @Override public String text(String message, String initial) { return onEdt(() -> (String) JOptionPane.showInputDialog(parent, message, "Tag-File", JOptionPane.QUESTION_MESSAGE, null, null, initial)); }
    /**
     * Abre um seletor de cor nativo em vez de pedir o código hexadecimal digitado.
     *
     * @param message propósito da entrada
     * @param initial cor inicial em #RRGGBB; valor inválido cai para o padrão da aplicação
     * @return cor escolhida em #RRGGBB, ou nulo ao cancelar
     */
    @Override public String color(String message, String initial) {
        return onEdt(() -> {
            Color start;
            try { start = Color.decode(initial); } catch (RuntimeException e) { start = Color.decode("#2864B4"); }
            Color chosen = JColorChooser.showDialog(parent, message, start);
            return chosen == null ? null : String.format("#%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue());
        });
    }
    /**
     * Usa JFileChooser e filtro visual; o Manager ainda valida Drop e extensões compostas.
     *
     * @param multiple se seleção múltipla é permitida
     * @param extensions extensões normalizadas para o filtro, vazio libera tudo
     * @return caminhos escolhidos, ou vazio
     */
    @Override public List<Path> files(boolean multiple, Set<String> extensions) {
        return onEdt(() -> {
            JFileChooser chooser = new JFileChooser(); chooser.setMultiSelectionEnabled(multiple); chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            String[] simple = extensions.stream().filter(e -> !e.isEmpty() && e.indexOf('.', 1) < 0).map(e -> e.substring(1)).toArray(String[]::new);
            if (simple.length > 0) chooser.setFileFilter(new FileNameExtensionFilter("Extensões da etiqueta", simple));
            if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return List.of();
            return multiple ? Arrays.stream(chooser.getSelectedFiles()).map(java.io.File::toPath).toList() : List.of(chooser.getSelectedFile().toPath());
        });
    }

    /**
     * Apresenta falha real com área técnica expansível; senhas de parâmetros são ocultadas.
     *
     * @param failure erro original, preservado para diagnóstico
     */
    public void showFailure(Throwable failure) {
        onEdt(() -> {
            Throwable actual = failure;
            while (actual instanceof CompletionException || actual instanceof ExecutionException) actual = actual.getCause();
            if (actual instanceof CancellationException) return null;
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            JTextArea message = new JTextArea(actual.getMessage() == null ? "Não foi possível concluir a solicitação." : redact(actual.getMessage()));
            message.setEditable(false); message.setLineWrap(true); message.setWrapStyleWord(true); message.setColumns(65); panel.add(message, BorderLayout.NORTH);
            StringBuilder detail = new StringBuilder();
            for (Throwable cause = actual; cause != null; cause = cause.getCause()) {
                detail.append(cause.getClass().getName()).append(": ").append(redact(String.valueOf(cause.getMessage()))).append('\n');
                if (cause instanceof SQLException sql) detail.append("SQLState: ").append(sql.getSQLState()).append("; código MySQL: ").append(sql.getErrorCode()).append('\n');
                for (Throwable suppressed : cause.getSuppressed()) detail.append("Falha adicional: ").append(redact(suppressed.toString())).append('\n');
            }
            JTextArea technical = new JTextArea(detail.toString(), 12, 65); technical.setEditable(false);
            JScrollPane scroll = new JScrollPane(technical); scroll.setVisible(false); panel.add(scroll, BorderLayout.CENTER);
            JCheckBox expanded = new JCheckBox("Mostrar detalhes técnicos");
            JPanel actions = new JPanel(new BorderLayout());
            actions.add(expanded, BorderLayout.NORTH);
            for (Throwable cause = actual; cause != null; cause = cause.getCause()) {
                if (cause instanceof DatabaseManager.ScriptFailure scriptFailure) {
                    actions.add(logActions(scriptFailure.getLog()), BorderLayout.SOUTH); break;
                }
                if (cause instanceof DatabaseManager.SchemaFailure schemaFailure && schemaFailure.getLog() != null) {
                    actions.add(logActions(schemaFailure.getLog()), BorderLayout.SOUTH); break;
                }
            }
            panel.add(actions, BorderLayout.SOUTH);
            expanded.addActionListener(e -> { scroll.setVisible(expanded.isSelected()); Window window = SwingUtilities.getWindowAncestor(panel); if (window != null) window.pack(); });
            JOptionPane.showMessageDialog(parent, panel, "Resultado da operação", JOptionPane.ERROR_MESSAGE); return null;
        });
    }

    /**
     * Oferece acesso ao diagnóstico sem depender da exibição de pastas ignoradas no IDE.
     * @param path arquivo de log ou diretório de logs
     * @return botões para abrir a pasta e copiar o caminho
     */
    public static JPanel logActions(Path path) { return logActions(() -> path); }

    /**
     * Oferece acesso ao log corrente quando a etapa muda durante a preparação.
     * @param path fornecedor do arquivo ou pasta, consultado na EDT
     * @return botões de acesso ao diagnóstico
     */
    public static JPanel logActions(Supplier<Path> path) {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton copy = new JButton("Copiar caminho do log");
        copy.addActionListener(event -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(path.get().toString()), null));
        JButton open = new JButton("Abrir pasta de logs");
        open.addActionListener(event -> {
            Path selected = path.get();
            new Thread(() -> {
                try {
                    Path directory = Files.isDirectory(selected) ? selected : selected.getParent();
                    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        throw new IllegalStateException("Abertura de pastas indisponível; copie o caminho.");
                    }
                    Desktop.getDesktop().open(directory.toFile());
                } catch (Exception failure) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(buttons,
                            "Não foi possível abrir a pasta. Copie o caminho.\n" + failure.getMessage()));
                }
            }, "tag-file-open-log").start();
        });
        buttons.add(open); buttons.add(copy);
        return buttons;
    }

    /**
     * Oculta valores de senha em mensagens técnicas antes de apresentá-las.
     *
     * @param text diagnóstico técnico
     * @return texto sem valores usuais de senha em mensagens JDBC
     */
    private static String redact(String text) { return text.replaceAll("(?i)(password|pwd|senha)\\s*[=:]\\s*[^&\\s;]+", "$1=<oculta>").replace("TagFile123!", "<senha didática>"); }

    /**
     * Executa diálogo na EDT e aguarda o retorno sem criar outra ação funcional.
     *
     * @param <T> tipo da resposta
     * @param task operação curta de UI, possivelmente modal
     * @return resposta do diálogo
     * @throws IllegalStateException se execução na EDT falhar
     */
    private static <T> T onEdt(Callable<T> task) {
        FutureTask<T> future = new FutureTask<>(task);
        try { ActionGate.onEdt(future); return future.get(); }
        catch (Exception e) { throw new IllegalStateException("Falha no diálogo", e); }
    }

    /**
     * Liga um atalho à mesma entrada usada pelo botão; não contorna o Controller.
     *
     * @param component área que recebe o atalho
     * @param stroke expressão KeyStroke, como {@code ctrl X}
     * @param name identificação no ActionMap
     * @param action solicitação funcional a encaminhar
     */
    public static void bind(JComponent component, String stroke, String name, Runnable action) {
        component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(stroke), name);
        component.getActionMap().put(name, new AbstractAction() {
            /**
             * Encaminha o atalho à mesma entrada funcional usada pelo botão.
             *
             * @param event evento do atalho; executa a mesma solicitação do botão
             */
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    /**
     * Trata somente rejeições imediatas; outros erros são apresentados dentro da ação.
     *
     * @param future resultado da entrada do Controller
     */
    public void observe(CompletableFuture<?> future) {
        future.whenComplete((result, error) -> {
            Throwable cause = error instanceof CompletionException ? error.getCause() : error;
            if (cause instanceof RejectedExecutionException) SwingUtilities.invokeLater(() -> {
                if (parent instanceof JComponent component) component.setToolTipText(cause.getMessage());
                Toolkit.getDefaultToolkit().beep();
            });
        });
    }
}
