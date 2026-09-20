package service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Lê eventos completos do log enquanto o script executa; demais linhas continuam no log. */
public final class ScriptProgressMonitor implements AutoCloseable {
    /** Log aberto para leitura incremental, sem carregar todo o histórico. */
    private final RandomAccessFile input;
    /** Caminho mostrado na interface. */
    private final Path log;
    /** Destino dos estados; a UI encaminha suas atualizações à EDT. */
    private final Consumer<PreparationProgress> listener;
    /** Bytes da linha atual; preserva caracteres UTF-8 divididos entre leituras. */
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    /** Descarta apenas linhas excessivamente grandes do protocolo. */
    private boolean oversized;
    /** Descrição da etapa atual. */
    private String message;
    /** Arquivo que está recebendo os bytes, quando houver transferência. */
    private Path transfer;
    /** Total conhecido da transferência. */
    private long total;
    /** Último estado entregue, evitando eventos repetidos. */
    private PreparationProgress previous;

    /**
     * Abre a leitura de um log já criado pelo executor.
     * @param log arquivo UTF-8
     * @param listener destino das atualizações
     * @throws IOException se o log não puder ser aberto
     */
    public ScriptProgressMonitor(Path log, Consumer<PreparationProgress> listener) throws IOException {
        this.log = log;
        this.listener = listener;
        input = new RandomAccessFile(log.toFile(), "r");
    }

    /**
     * Consome somente os bytes disponíveis, sem esperar pelo término do processo.
     * @throws IOException se falhar a leitura do log
     */
    public void poll() throws IOException {
        byte[] bytes = new byte[8192];
        // Limita cada consulta para que saída contínua não impeça o timeout do executor.
        long remaining = input.length() - input.getFilePointer();
        while (remaining > 0) {
            int count = input.read(bytes, 0, (int) Math.min(bytes.length, remaining));
            if (count < 0) break;
            remaining -= count;
            for (int i = 0; i < count; i++) {
                if (bytes[i] == '\n') {
                    if (!oversized) accept(pending.toString(StandardCharsets.UTF_8).stripTrailing());
                    pending.reset(); oversized = false;
                } else if (pending.size() < 8192) pending.write(bytes[i]);
                else oversized = true;
            }
        }
        publish();
    }

    /**
     * Reconhece somente os eventos emitidos pelos scripts do projeto.
     * @param line linha completa, sem terminador
     */
    private void accept(String line) {
        String[] parts = line.split("\t", 5);
        if (parts.length == 3 && parts[0].equals("TAG_FILE_PROGRESS") && parts[1].equals("STAGE")) {
            message = parts[2]; transfer = null;
        } else if (parts.length == 5 && parts[0].equals("TAG_FILE_PROGRESS") && parts[1].equals("TRANSFER")) {
            try {
                long size = Long.parseLong(parts[3]);
                Path path = Path.of(parts[4]);
                if (size <= 0) return;
                message = parts[2]; total = size; transfer = path;
            } catch (IllegalArgumentException invalidEvent) { /* Mantém a etapa anterior. */ }
        }
    }

    /** Emite a etapa atual e mede bytes somente quando o tamanho total é conhecido. */
    private void publish() {
        if (message == null) return;
        int percent = -1;
        if (transfer != null) {
            try { percent = (int) Math.min(100, Files.size(transfer) * 100.0 / total); }
            catch (IOException unavailable) { percent = 0; }
        }
        PreparationProgress next = new PreparationProgress(message, percent, log);
        if (!next.equals(previous)) { listener.accept(next); previous = next; }
    }

    /** Fecha somente o leitor, preservando o arquivo de log.
     * @throws IOException se falhar o fechamento da leitura
     */
    @Override public void close() throws IOException { input.close(); }
}
