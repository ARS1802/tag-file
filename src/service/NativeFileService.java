/*
 * Inventário — NativeFileService.
 * Único serviço de disco; não contém SQL, diálogos ou publicação de eventos (ARQ-07).
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - NativeFileService.NativeFileService(): Cria serviço sem estado ou recursos persistentes.
 * - NativeFileService.listFiles(NativeDirectory directory, NativeFileFilter filter): Lista apenas filhos diretos regulares; links não são operados nesta versão.
 * - NativeFileService.listDirectories(NativeDirectory directory): Lista subpastas para navegação, sem varrer recursivamente.
 * - NativeFileService.readMetadata(LocalFile previous, boolean required): Lê metadados preservando UUID e Tags; ausência retém últimos dados conhecidos.
 * - NativeFileService.physicalTime(FileTime time): Converte data física conhecida para o contrato UTC, sem usar o relógio atual.
 * - NativeFileService.requireAvailable(NativeFile file): Verifica imediatamente um arquivo antes de utilizá-lo.
 * - NativeFileService.move(NativeFile source, Path target, boolean replace): Move fisicamente após as confirmações feitas pelo chamador.
 * - NativeFileService.copy(NativeFile source, Path target, boolean replace): Copia conteúdo mantendo a origem; não cria cadastro ou associações.
 * - NativeFileService.validateTarget(NativeFile source, Path target): Impede sobrescrever a própria origem, pastas ou links.
 * - NativeFileService.delete(NativeFile file): Exclui permanentemente somente um arquivo regular; não usa lixeira.
 * - NativeFileService.open(NativeFile file): Abre o arquivo na aplicação associada pelo sistema operacional.
 *
 * Consulte: doc/arquitetura-e-padroes.md — ARQ-06/ARQ-07; doc/requisitos-e-regras.md — OP-09/ERR-01.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package service;

import filter.NativeFileFilter;
import model.*;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Único serviço de disco; não contém SQL, diálogos ou publicação de eventos (ARQ-07).
 */
public final class NativeFileService {
    /**
     * Cria serviço sem estado ou recursos persistentes.
     */
    public NativeFileService() { }

    /**
     * Lista apenas filhos diretos regulares; links não são operados nesta versão.
     *
     * @param directory pasta existente
     * @param filter critérios físicos
     * @return arquivos ordenados por nome e caminho, sem cadastrar no SQL
     * @throws IOException se leitura da pasta ou dos atributos falhar
     */
    public List<NativeFile> listFiles(NativeDirectory directory, NativeFileFilter filter) throws IOException {
        List<NativeFile> files = new ArrayList<>();
        try (var entries = Files.list(directory.getPath())) {
            for (Path path : entries.toList()) {
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    NativeFile file = new NativeFile(path);
                    LocalFile metadata = readMetadata(new LocalFile(new UUID(0, 0), file, false, null, null, null, null, Set.of()), true);
                    if (filter.matches(metadata)) files.add(file);
                }
            }
        }
        files.sort(Comparator.comparing(NativeFile::getName).thenComparing(f -> f.getPath().toString()));
        return List.copyOf(files);
    }

    /**
     * Lista subpastas para navegação, sem varrer recursivamente.
     *
     * @param directory pasta a listar
     * @return referências das subpastas reais, ordenadas
     * @throws IOException se listagem falhar
     */
    public List<NativeDirectory> listDirectories(NativeDirectory directory) throws IOException {
        try (var entries = Files.list(directory.getPath())) {
            return entries.filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)).sorted().map(NativeDirectory::new).toList();
        }
    }

    /**
     * Lê metadados preservando UUID e Tags; ausência retém últimos dados conhecidos.
     * Criação fica nula em Unix, onde esta API não distingue data real de substituta;
     * no provedor Windows/DOS é lida pelo contrato de BasicFileAttributes.
     *
     * @param previous fotografia anterior
     * @param required se verdadeiro, ausência impede a operação
     * @return nova fotografia, sem persistência
     * @throws IOException se required e ausente, link, diretório ou acesso falhar
     */
    public LocalFile readMetadata(LocalFile previous, boolean required) throws IOException {
        Path path = previous.getNativeFile().getPath();
        BasicFileAttributes a;
        try {
            a = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!a.isRegularFile()) throw new IOException("Somente arquivos regulares são aceitos: " + path);
        } catch (NoSuchFileException e) {
            if (required) throw e;
            return new LocalFile(previous.getId(), previous.getNativeFile(), false, previous.getSize(), previous.getCreatedAt(),
                    previous.getModifiedAt(), previous.getLastAccessedAt(), previous.getTags());
        }
        LocalDateTime creation = null;
        if (path.getFileSystem().supportedFileAttributeViews().contains("dos") && System.getProperty("os.name").startsWith("Windows"))
            creation = physicalTime(a.creationTime());
        // Em Unix a API básica não distingue birthtime verdadeiro de mtime substituto.
        return new LocalFile(previous.getId(), previous.getNativeFile(), true, a.size(), creation,
                physicalTime(a.lastModifiedTime()), physicalTime(a.lastAccessTime()), previous.getTags());
    }

    /**
     * Converte data física conhecida para o contrato UTC, sem usar o relógio atual.
     *
     * @param time valor fornecido pelo sistema de arquivos
     * @return data UTC em microssegundos; epoch usado pelo provedor como desconhecido vira nulo
     */
    private static LocalDateTime physicalTime(FileTime time) {
        return time.toMillis() == 0 ? null : LocalDateTime.ofInstant(time.toInstant(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * Verifica imediatamente um arquivo antes de utilizá-lo.
     *
     * @param file referência física
     * @throws IOException se ausente, não regular ou link simbólico
     */
    public void requireAvailable(NativeFile file) throws IOException {
        if (!Files.isRegularFile(file.getPath(), LinkOption.NOFOLLOW_LINKS)) throw new NoSuchFileException(file.toString(), null, "Arquivo regular indisponível (links não aceitos)");
    }

    /**
     * Move fisicamente após as confirmações feitas pelo chamador.
     *
     * @param source arquivo existente
     * @param target destino absoluto ou relativo
     * @param replace permite substituir arquivo regular no destino
     * @return referência no novo caminho
     * @throws IOException se origem inválida, conflito, destino inseguro ou movimentação falhar
     */
    public NativeFile move(NativeFile source, Path target, boolean replace) throws IOException {
        validateTarget(source, target); requireAvailable(source);
        if (replace) Files.move(source.getPath(), target, StandardCopyOption.REPLACE_EXISTING);
        else Files.move(source.getPath(), target);
        return new NativeFile(target);
    }

    /**
     * Copia conteúdo mantendo a origem; não cria cadastro ou associações.
     *
     * @param source arquivo existente
     * @param target caminho de destino
     * @param replace permite substituir arquivo regular
     * @return referência da cópia
     * @throws IOException se cópia ou validação falhar; arquivo parcial pode existir
     */
    public NativeFile copy(NativeFile source, Path target, boolean replace) throws IOException {
        validateTarget(source, target); requireAvailable(source);
        if (replace) Files.copy(source.getPath(), target, StandardCopyOption.REPLACE_EXISTING);
        else Files.copy(source.getPath(), target);
        return new NativeFile(target);
    }

    /**
     * Impede sobrescrever a própria origem, pastas ou links.
     *
     * @param source origem da operação
     * @param target destino proposto
     * @throws IOException se alvo for o mesmo arquivo, pasta, link ou pai inexistente
     */
    private void validateTarget(NativeFile source, Path target) throws IOException {
        Path normalized = target.toAbsolutePath().normalize();
        if (source.getPath().equals(normalized) || Files.exists(normalized) && Files.isSameFile(source.getPath(), normalized))
            throw new IOException("Origem e destino representam o mesmo arquivo");
        if (!Files.isDirectory(normalized.getParent()) || Files.isSymbolicLink(normalized) || Files.isDirectory(normalized))
            throw new IOException("Destino deve ser arquivo em pasta existente, sem link: " + normalized);
    }

    /**
     * Exclui permanentemente somente um arquivo regular; não usa lixeira.
     *
     * @param file arquivo cuja exclusão já foi confirmada
     * @throws IOException se indisponível ou exclusão falhar
     */
    public void delete(NativeFile file) throws IOException { requireAvailable(file); Files.delete(file.getPath()); }

    /**
     * Abre o arquivo na aplicação associada pelo sistema operacional.
     *
     * @param file arquivo existente
     * @throws IOException se Desktop/Open não estiver disponível ou a abertura falhar
     */
    public void open(NativeFile file) throws IOException {
        requireAvailable(file);
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) throw new IOException("Este ambiente não oferece abertura de arquivos");
        Desktop.getDesktop().open(file.getPath().toFile());
    }
}
