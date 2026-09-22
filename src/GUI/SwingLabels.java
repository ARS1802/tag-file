package GUI;

import model.LocalFile;
import model.NativeFile;
import model.Tag;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

/** Monta rótulos HTML curtos para as listas Swing sem alterar as entidades exibidas. */
final class SwingLabels {
    private SwingLabels() { }

    /** Exibe cadastro, disponibilidade, Tags coloridas e, opcionalmente, caminho completo. */
    static String localFile(LocalFile file, boolean includePath) {
        StringBuilder text = new StringBuilder("<html>").append(escape(file.getNativeFile().getName()));
        if (!file.isAvailable()) text.append(" <i>[indisponível]</i>");
        appendTags(text, file.getTags());
        if (includePath) text.append(" &nbsp;—&nbsp; ").append(escape(file.getNativeFile().toString()));
        return text.append("</html>").toString();
    }

    /** Exibe arquivo físico e as Tags do cadastro correspondente, quando ele existir. */
    static String nativeFile(NativeFile file, LocalFile registered) {
        StringBuilder text = new StringBuilder("<html>").append(escape(file.getName()));
        if (registered != null) appendTags(text, registered.getTags());
        return text.append("</html>").toString();
    }

    /** Acrescenta cada Tag como ponto colorido seguido pelo nome, mantendo ordenação estável. */
    private static void appendTags(StringBuilder text, Collection<Tag> tags) {
        String badges = tags.stream()
                .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Tag::getId))
                .map(tag -> "<font color='" + tag.getColor() + "'>&#9679;</font>&nbsp;" + escape(tag.getName()))
                .collect(Collectors.joining("&nbsp;&nbsp;"));
        if (!badges.isEmpty()) text.append(" &nbsp;[").append(badges).append(']');
    }

    /** Impede que nomes e caminhos sejam interpretados como marcação HTML. */
    private static String escape(String value) {
        return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
