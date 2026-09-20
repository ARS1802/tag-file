# Melhorias de UI

Registro das alterações feitas na camada `ui` (e nos pontos de `manager`/`Interaction` que ela
atravessa) fora do escopo original documentado em `decisoes-implementacao.md`. Nenhuma decisão
de produto (P-01 a P-13) foi revertida; estas são refinamentos de apresentação sobre contratos
já existentes.

| ID | Alteração e motivo | Arquivos |
|---|---|---|
| UI-M1 | Botões que dependem de uma Tag ou de um arquivo selecionado ficam desabilitados até haver seleção, em vez de aceitar clique e não fazer nada. Listener ignora `getValueIsAdjusting()` para não recalcular a cada passo de um arraste de seleção. | `TagExplorerPanel`, `LocalFileExplorerPanel` |
| UI-M2 | Lista de Tags mostra só o nome; o UUID (mantido por `Tag.toString()` para distinguir nomes repetidos, conforme Javadoc original) passou para o tooltip por célula, via subclasse de `JList` que sobrescreve `getToolTipText(MouseEvent)` — `JList` não expõe tooltip por célula automaticamente como `JTree`/`JTable`. | `TagExplorerPanel` |
| UI-M3 | Seleção de cor da Tag passou de campo de texto (`#RRGGBB` digitado) para um `JColorChooser`. Acrescentado `Interaction.color(String, String)` como método `default` que delega para `text(...)`, preservando o comportamento do `Interaction` usado por `--demo-local` sem precisar alterá-lo. Só `SwingInteraction` sobrescreve com o seletor nativo. `LocalFileManager.createTag`/`editTag` passaram a chamar `color(...)` em vez de `text(...)`. | `Interaction`, `SwingInteraction`, `LocalFileManager` |
| UI-M4 | Navegação por `JComboBox` de subpastas + botão "Entrar" substituída por uma árvore (`JTree`) de raiz fixa no disco da pasta inicial, montada uma única vez. Expansão de nós usa `NativeFileService.listDirectories` diretamente (ARQ-07: sem SQL, sem publicação de eventos) — não passa pelo `Controller`. Selecionar um nó chama `controller.navigate(...)`, o mesmo fluxo que "Entrar" já usava, preservando P-12/apresentação (`ExplorerContext` continua sendo a única fonte de estado de navegação; o `Panel` não consulta DAOs). A cada `onDirectoryChanged`, `revealDirectory(...)` desce pela árvore já carregada e seleciona a pasta atual, sem reconstruir a árvore nem perder nós já expandidos. | `LocalFileExplorerPanel` |
| UI-M5 | Barra de status (`X arquivo(s)`) no rodapé de `LocalFileExplorerPanel`, no mesmo padrão que `TagExplorerPanel` já usava. Tooltips explicando formato em "Extensões" (lista separada por vírgula, `<sem>` para arquivo sem extensão) e "Bytes mínimos/máximos" (inteiro). | `LocalFileExplorerPanel` |


## Não alterado

Nenhum arquivo de `controller`, `persistence`, `model`, `filter` ou `service` foi modificado.
Nenhuma assinatura pública de construtor mudou; `Main.java`/`DemoWindow` continuam construindo
os Panels exatamente como documentado.
