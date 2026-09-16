/*
 * Inventário — Interaction.
 * Fronteira de decisões: Controllers/Manager pedem respostas, UI apresenta diálogos.
 * Responsabilidade e limites: os contratos detalhados estão no Javadoc junto de cada declaração.
 *
 * Construtores e métodos declarados (inclusive privados e implementações anônimas):
 * - Interaction.choose(String message, String[] options): Solicita escolha explícita antes dos efeitos correspondentes.
 * - Interaction.text(String message, String initial): Solicita uma entrada de texto que pode ser cancelada.
 * - Interaction.files(boolean multiple, Set<String> extensions): Seleciona arquivos reais; escolher pasta no diálogo não importa seu conteúdo.
 *
 * Consulte: doc/interface-e-fluxos.md — UI-01 a UI-03; doc/arquitetura-e-padroes.md — ARQ-06.
 * Consulte: doc/decisoes-implementacao.md — contratos escolhidos para P-01 a P-13.
 */

package ui;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Fronteira de decisões: Controllers/Manager pedem respostas, UI apresenta diálogos.
 */
public interface Interaction {
    /**
     * Solicita escolha explícita antes dos efeitos correspondentes.
     *
     * @param message consequências e alvos confirmados
     * @param options alternativas na ordem de apresentação
     * @return índice escolhido, ou -1 ao fechar/cancelar
     */
    int choose(String message, String... options);
    /**
     * Solicita uma entrada de texto que pode ser cancelada.
     *
     * @param message finalidade da entrada
     * @param initial valor inicial
     * @return texto informado ou nulo ao cancelar
     */
    String text(String message, String initial);
    /**
     * Seleciona arquivos reais; escolher pasta no diálogo não importa seu conteúdo.
     *
     * @param multiple permite seleção múltipla explícita
     * @param extensions restrições visuais; vazio permite qualquer arquivo
     * @return caminhos confirmados, ou lista vazia ao cancelar
     */
    List<Path> files(boolean multiple, Set<String> extensions);
}
