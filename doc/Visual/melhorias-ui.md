# Melhorias de UI

Registro das alterações feitas na camada de apresentação (atualmente `src/GUI`) e nos pontos de `Interaction` que ela
atravessa) fora do escopo original documentado em `decisoes-implementacao.md`. Nenhuma decisão
de produto (P-01 a P-13) foi revertida; estas são refinamentos de apresentação sobre contratos
já existentes.

| ID | Alteração e motivo | Arquivos |
|---|---|---|
| UI-M1 | Botões que dependem de uma Tag ou de um arquivo selecionado ficam desabilitados até haver seleção, em vez de aceitar clique e não fazer nada. Listener ignora `getValueIsAdjusting()` para não recalcular a cada passo de um arraste de seleção. | `TagExplorerPanel`, `LocalFileExplorerPanel` |
| UI-M2 | Lista de Tags mostra só o nome; o UUID (mantido por `Tag.toString()` para distinguir nomes repetidos, conforme Javadoc original) passou para o tooltip por célula, via subclasse de `JList` que sobrescreve `getToolTipText(MouseEvent)` — `JList` não expõe tooltip por célula automaticamente como `JTree`/`JTable`. | `TagExplorerPanel` |
| UI-M3 | Acrescentado `Interaction.color(String, String)` como método `default` que delega para `text(...)`; `SwingInteraction` implementa o seletor `JColorChooser`. A integração ao fluxo ainda está pendente: `LocalFileManager.createTag`/`editTag` continuam chamando `text(...)` para a cor. | `Interaction`, `SwingInteraction` |
| UI-M4 | Navegação por `JComboBox` de subpastas + botão "Entrar" substituída por uma árvore (`JTree`) de raiz fixa no disco da pasta inicial, montada uma única vez. Expansão de nós usa `NativeFileService.listDirectories` diretamente (ARQ-07: sem SQL, sem publicação de eventos) — não passa pelo `Controller`. Selecionar um nó chama `controller.navigate(...)`, o mesmo fluxo que "Entrar" já usava, preservando P-12/apresentação (`ExplorerContext` continua sendo a única fonte de estado de navegação; o `Panel` não consulta DAOs). A cada `onDirectoryChanged`, `revealDirectory(...)` desce pela árvore já carregada e seleciona a pasta atual, sem reconstruir a árvore nem perder nós já expandidos. | `LocalFileExplorerPanel` |
| UI-M5 | Barra de status (`X arquivo(s)`) no rodapé de `LocalFileExplorerPanel`, no mesmo padrão que `TagExplorerPanel` já usava. Tooltips explicando formato em "Extensões" (lista separada por vírgula, `<sem>` para arquivo sem extensão) e "Bytes mínimos/máximos" (inteiro). | `LocalFileExplorerPanel` |


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
