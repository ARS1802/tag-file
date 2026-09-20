package service;

import java.nio.file.Path;

/** Estado da preparação; a porcentagem pertence à transferência atual, não ao tempo total.
 * @param message etapa apresentada ao usuário
 * @param percent porcentagem entre 0 e 100, ou -1 quando o total é desconhecido
 * @param log arquivo de diagnóstico da etapa, ou null antes de executar scripts
 */
public record PreparationProgress(String message, int percent, Path log) { }
