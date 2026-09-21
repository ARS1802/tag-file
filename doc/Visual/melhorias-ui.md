# Melhorias de UI

Registro das alterações feitas na camada de apresentação (atualmente `src/GUI`) e nos pontos de `Interaction` que ela
atravessa, fora do escopo original documentado em `decisoes-implementacao.md`. Nenhuma decisão
de produto (P-01 a P-13) foi revertida; estas são refinamentos de apresentação sobre contratos
já existentes.

| ID | Alteração e motivo | Arquivos |
|---|---|---|
| UI-M1 | Botões que dependem de uma Tag ou de um arquivo selecionado ficam desabilitados até haver seleção, em vez de aceitar clique e não fazer nada. Listener ignora `getValueIsAdjusting()` para não recalcular a cada passo de um arraste de seleção. | `TagExplorerPanel`, `LocalFileExplorerPanel` |
| UI-M2 | Lista de Tags mostra só o nome; o UUID (mantido por `Tag.toString()` para distinguir nomes repetidos, conforme Javadoc original) passou para o tooltip por célula, via subclasse de `JList` que sobrescreve `getToolTipText(MouseEvent)` — `JList` não expõe tooltip por célula automaticamente como `JTree`/`JTable`. | `TagExplorerPanel` |
| UI-M3 | Acrescentado `Interaction.color(String, String)` como método `default` que delega para `text(...)`; `SwingInteraction` implementa o seletor `JColorChooser`. `LocalFileManager.createTag` e `editTag` passaram a usar esse contrato, portanto o usuário escolhe a cor visualmente sem digitar hexadecimal. O valor devolvido continua no formato `#RRGGBB`, preservando modelo, validação e persistência. | `Interaction`, `SwingInteraction`, `LocalFileManager` |
| UI-M4 | Navegação por `JComboBox` de subpastas + botão "Entrar" substituída por uma árvore (`JTree`) de raiz fixa no disco da pasta inicial, montada uma única vez. Expansão de nós usa `NativeFileService.listDirectories` diretamente (ARQ-07: sem SQL, sem publicação de eventos) — não passa pelo `Controller`. Selecionar um nó chama `controller.navigate(...)`, o mesmo fluxo que "Entrar" já usava, preservando P-12/apresentação (`ExplorerContext` continua sendo a única fonte de estado de navegação; o `Panel` não consulta DAOs). A cada `onDirectoryChanged`, `revealDirectory(...)` desce pela árvore já carregada e seleciona a pasta atual, sem reconstruir a árvore nem perder nós já expandidos. | `LocalFileExplorerPanel` |
| UI-M5 | Barra de status (`X arquivo(s)`) no rodapé de `LocalFileExplorerPanel`, no mesmo padrão que `TagExplorerPanel` já usava. Tooltips explicando formato em "Extensões" (lista separada por vírgula, `<sem>` para arquivo sem extensão) e "Bytes mínimos/máximos" (inteiro). | `LocalFileExplorerPanel` |
| UI-M6 | As áreas de etiquetas, filtros e ações receberam agrupamento e títulos visuais. A combinação técnica "AND/OR" foi apresentada como "Todas as etiquetas selecionadas" ou "Qualquer etiqueta selecionada", mantendo os mesmos índices e o mesmo comportamento de pesquisa. | `TagExplorerPanel`, `LocalFileExplorerPanel` |
| UI-M7 | Arquivos podem ser abertos com `Enter` ou duplo clique, sempre acionando o mesmo botão e, por consequência, o mesmo fluxo do Controller. Cliques duplos fora dos limites reais de uma célula são ignorados. Tooltips tornam descobríveis os atalhos `Ctrl+C`, `Ctrl+X`, `Ctrl+V` e `F2`. | `TagExplorerPanel`, `LocalFileExplorerPanel` |
| UI-M8 | O painel Arquivos Local foi compactado para priorizar a árvore e a lista: caminho e botões `Ir`/`Subir` ficam em uma única linha; pressionar `Enter` no caminho navega; filtros ficam recolhidos por padrão e são alternados por `Mostrar filtros…`/`Ocultar filtros`; as oito entradas do bloco de ações ocupam duas linhas com quatro colunas. | `LocalFileExplorerPanel` |


## Refinamentos de usabilidade — 21/09/2026

Os refinamentos UI-M3 e UI-M6 a UI-M8 alteram somente apresentação e formas de acionar
operações já existentes. Não foram modificados o esquema MySQL, as entidades, os DAOs, os
efeitos dos Controllers nem as regras de exclusividade de ações.

## Organização da interface — 20/09/2026

A janela antes interna a `Main`, chamada `DemoWindow`, foi extraída como `GUI.MainWindow`.
Os cinco tipos antes em `src/ui` agora estão em `src/GUI`, com imports atualizados.
Os construtores dos painéis foram preservados. `application.Application` compõe os serviços
e gerencia o ciclo de vida; `Main` contém apenas a entrada e o tratamento de falhas da inicialização.
A execução normal abre a pasta pessoal do usuário e não cria dados de demonstração.
Veja os [arquivos e suas responsabilidades](../implementacao.md#interface-gráfica).

Verificação da reorganização em Linux: os 33 fontes compilaram com JDK 24 e o Javadoc
foi gerado sem erros ou avisos. Um ensaio gráfico com MySQL real em diretório temporário
separado verificou a abertura pelo `Main`, a carga da pasta inicial, a alternância entre abas
e lado a lado, o bloqueio de fechamento durante operações e a liberação das janelas e do banco.
Após reiniciar esse banco, o cadastro e sua Tag continuaram presentes; os arquivos físicos
foram preservados, a limpeza inicial de órfãos ocorreu e nenhum dado de demonstração foi criado.
O ensaio aguardou a remoção do PID antes de reiniciar o MySQL: o script de parada existente
pode retornar antes de o processo terminar completamente. Windows não foi executado.
Logs locais: `out/gui-refactor-build.log` e `out/gui-refactor-lifecycle.log` (não versionados).
