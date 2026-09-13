# Requisitos e regras

[Índice](README.md) · [Rastreabilidade](rastreabilidade.md) · [Decisões e pendências](decisoes-e-pendencias.md)

Esta é a referência principal dos grupos TAG, EXT, CIC, OP, DEL, SYN e ERR. **O presente é normativo:** descreve o comportamento que a equipe deverá implementar. Gatilhos, condições e efeitos abaixo definem o projeto; os exemplos são ilustrativos, sem execução. As pendências são decisões abertas, não a falta esperada de código.

Arquivo físico, registro e associação são conceitos distintos de [DOM-01](modelo-de-dominio.md#dom-01). UUID identifica o registro; caminho indica sua localização ([DOM-02](modelo-de-dominio.md#dom-02)). Os fluxos completos estão em [casos de uso](casos-de-uso.md).

<a id="tag-01"></a>

## TAG-01 — Criação, edição e nomes repetidos

**Estado: confirmada.**

O usuário pode criar Tags próprias e configurar nome, cor hexadecimal e extensões permitidas. Pode editar a classificação, respeitando os efeitos das alterações de extensão descritos em [EXT-03](requisitos-e-regras.md#ext-03).

O nome de uma Tag **não é UNIQUE**. Ao detectar um nome já existente, a UI deve pedir confirmação e permitir criar mesmo assim. A nova Tag possui outra identidade, mesmo com o mesmo nome.

A comparação que ignora maiúsculas/minúsculas e espaços nas extremidades foi uma proposta, não uma regra final homologada. Essa normalização não é requisito confirmado.

**Consequência derivada:** ações e relações devem identificar a Tag por UUID, não apenas pelo texto do nome. Isso também afeta Tags predefinidas e a Tag de sistema.

<a id="tag-02"></a>

## TAG-02 — Tags predefinidas

**Estado: confirmada.**

Os nomes aprovados são:

```text
Imagens
PDF
Audios
Videos
Etiqueta Ausente
```

As quatro primeiras podem ser apagadas mediante confirmação reforçada. `Etiqueta Ausente` não pode ser apagada pelo usuário.

Exemplos de extensões iniciais foram discutidos:

```text
Imagens → .jpg, .jpeg, .png, .gif, .webp
PDF     → .pdf
Audios  → .mp3, .wav, .flac, .aac
Videos  → .mp4, .mkv, .avi, .mov
```

Esses conjuntos foram exemplos; não há um catálogo completo fechado para todas as Tags. Não há aprovação para ampliar essa lista por inferência.

A criação inicial das Tags predefinidas não é autorização para recriar a cada abertura aquelas que o usuário excluiu. A política técnica de identificação e preparação das predefinidas permanece parcialmente aberta.

<a id="tag-03"></a>

## TAG-03 — Tags vazias e contagem de disponíveis

**Estado: confirmada.**

Tags podem existir vazias. Deve haver uma funcionalidade para encontrá-las e permitir sua exclusão.

A quantidade de arquivos associados com `available = true` será consultada no banco quando necessária. A Tag não terá contador persistido de disponíveis.

**Distinção derivada:** uma Tag com associações apenas a arquivos indisponíveis não é uma Tag vazia; ela tem zero arquivos disponíveis. A procura por Tags vazias deve considerar associações, não somente a contagem dos disponíveis.

A proteção de `Etiqueta Ausente` continua valendo quando ela está vazia e também na funcionalidade de localizar/excluir Tags vazias.

<a id="ext-01"></a>

## EXT-01 — Extensões específicas, múltiplas e normalizadas

**Estado: confirmada.**

Uma Tag aceita zero, uma ou várias extensões. Pode aceitar extensões específicas, como `.cdr`, sem depender de uma enumeração fechada de categorias.

Ausência de extensões configuradas significa ausência de restrição: a Tag aceita qualquer extensão. Coleção vazia foi proposta como representação Java; `null` também apareceu originalmente. A regra semântica está confirmada, a API de nulabilidade não está completamente fechada.

Normalize extensões para minúsculas e com ponto inicial:

```text
PDF   → .pdf
.PDF  → .pdf
jpeg  → .jpeg
CDR   → .cdr
```

A mesma extensão não pode ser registrada duas vezes para a mesma Tag. Tags diferentes podem aceitar a mesma extensão.

`FileType` foi usado no início como nome do conceito. O significado final é extensão real, não categoria `IMAGE`, `AUDIO` ou `VIDEO`. Não existe decisão inequívoca exigindo uma classe ou enum `FileType` no código final.

<a id="ext-02"></a>

## EXT-02 — Incompatibilidade ao associar

**Estado: confirmada.**

Condição: o usuário tenta associar um arquivo a uma Tag restrita que não aceita sua extensão.

A interface informa a extensão e a Tag afetada e oferece:

```text
Criar nova etiqueta
Adicionar a extensão à etiqueta atual
Cancelar
```

A associação incompatível não ocorre silenciosamente. Cancelar não autoriza a nova associação nem a ampliação da restrição.

Exemplo:

```text
foto.png → tentativa de associar à Tag PDF
Tag PDF aceita apenas .pdf

O usuário pode criar outra Tag, permitir .png em PDF ou cancelar.
```

O aviso é amigável. A incompatibilidade não é tratada como falha irrecuperável do aplicativo.

O filtro do seletor de arquivos ajuda na seleção, mas a regra também vale para Drag and Drop e não pode depender exclusivamente da aparência do seletor.

<a id="ext-03"></a>

## EXT-03 — Edição de extensões já utilizadas

**Estado: confirmada.**

Quando a edição das extensões tornar arquivos associados incompatíveis, a UI lista os arquivos afetados e pede confirmação antes de retirar a extensão e as associações incompatíveis.

Exemplo:

```text
Tag Faculdade aceita .pdf e .docx.
Arquivos: prova.pdf, trabalho.docx.
Usuário altera a restrição para aceitar somente .docx.
```

Se confirmar, `prova.pdf` perde apenas sua associação com `Faculdade`. Outras Tags permanecem. Se perder a última Tag normal, aplica-se [CIC-01](requisitos-e-regras.md#cic-01). O arquivo físico não é apagado por essa edição.

Se não confirmar, as associações incompatíveis não são retiradas silenciosamente.

**Consequências derivadas:**

- A compatibilidade é avaliada contra o conjunto final de extensões.
- Remover a última extensão configurada deixa a Tag sem restrição; não significa rejeitar todos os arquivos.
- Passar de ausência de restrição para uma lista específica pode tornar arquivos incompatíveis. O estado final precisa obedecer à mesma regra, embora não tenha sido desenhado um diálogo separado para essa transição.

Não invente tratamento por arquivo em lote ou botões adicionais não aprovados.

<a id="tag-04"></a>

## TAG-04 — Sugestão de Tag predefinida

**Estado: parcialmente confirmada; [P-03](decisoes-e-pendencias.md#p-03) permanece aberta.**

Quando um `LocalFile` é criado, deve ser apresentada a possibilidade de adicionar uma Tag predefinida compatível com a extensão.

```text
prova.pdf é registrado em Faculdade.
O programa pode sugerir a associação adicional com PDF.
```

A interface possui uma opção equivalente a **“Não perguntar novamente nesta sessão”**. A supressão termina na próxima inicialização do aplicativo; não é uma preferência permanente persistida.

Não foi aprovada aplicação obrigatória automática nem reclassificação automática em toda alteração de extensão.

**Pendente:** ao silenciar, o programa deixa de adicionar sugestões ou repete a resposta dada para os próximos arquivos? A resolução depende de [P-03](decisoes-e-pendencias.md#p-03); também não há critério definido para várias predefinidas compatíveis.

<a id="cic-01"></a>

## CIC-01 — Primeira classificação e reorganização durante a sessão

**Estado: confirmada.**

Exibir um arquivo em `Arquivos Local` não cria registro SQL. Ao associar a primeira Tag, o programa passa a precisar de um `LocalFile`; se o caminho já estiver registrado, reutiliza o registro.

Quando o usuário remove a última Tag normal durante a sessão, o `LocalFile` não é destruído imediatamente. Recebe automaticamente a Tag protegida `Etiqueta Ausente`.

```text
prova.pdf: Faculdade, PDF
remove Faculdade → PDF
remove PDF → Etiqueta Ausente
```

Isso mantém o arquivo acessível em `Arquivos por Tag` para continuar a reorganização, sem obrigar o usuário a procurá-lo novamente na pasta física.

Quando uma Tag normal é adicionada, `Etiqueta Ausente` é removida automaticamente da associação:

```text
Etiqueta Ausente + nova Tag Importante
→ resultado: Importante
```

A Tag de sistema permanece cadastrada. O que é temporário é a associação com ela.

<a id="cic-02"></a>

## CIC-02 — Limpeza exclusiva da inicialização

**Estado: confirmada.**

Na abertura do programa, remover do banco:

1. Os `LocalFile` cuja única Tag seja `Etiqueta Ausente`.
2. Defensivamente, os `LocalFile` sem qualquer associação em `LOCAL_FILE_TAG`.

Remover também suas associações, conforme o efeito definido. **Não apagar os arquivos físicos.** A Tag `Etiqueta Ausente` permanece e fica vazia depois da limpeza.

Arquivos indisponíveis não são excluídos apenas por estarem indisponíveis. Indisponibilidade e falta de classificação são condições diferentes.

A identificação técnica de `Etiqueta Ausente` não pode depender ingenuamente do nome, pois nomes repetidos são permitidos. O mecanismo concreto está em [P-07](decisoes-e-pendencias.md#p-07).

<a id="cic-03"></a>

## CIC-03 — Limites da limpeza

**Estado: derivada das decisões de sessão e de Refresh.**

Não executam a limpeza de inicialização:

```text
Refresh
Troca de aba
Aplicação de filtros
Navegação de pasta
Reconexão com MySQL
Notificação do Observer
```

Reconectar não é reiniciar a sessão de organização do usuário.

O alcance do adiamento em relação à remoção explícita de todos os registros da modalidade [DEL-02](requisitos-e-regras.md#del-02) permanece em [P-02](decisoes-e-pendencias.md#p-02). Uma regra geral não resolve essa diferença de alcance.

<a id="op-01"></a>

## OP-01 — Seleção, cadastro e associação inicial

**Estado: confirmada.**

Formas aprovadas:

- Seleção individual de arquivo.
- Drag and Drop de arquivos sobre a representação de uma Tag.
- Seleção de arquivos dentro de uma pasta durante a criação de Tag.

`JFileChooser` e `FileNameExtensionFilter` são componentes oficialmente adotados.

Ao criar uma Tag com restrição de extensões, o usuário pode abrir uma pasta e selecionar apenas os arquivos desejados compatíveis. Não é obrigado a associar todos os arquivos encontrados.

Exemplo aprovado:

```text
Nova Tag: Faculdade
Extensão: .pdf
Pasta: ~/Jogos/Megaman

Arquivos PDF visíveis:
Megaman-Manual.pdf
prova.pdf

Seleção do usuário:
prova.pdf
```

Para cada arquivo confirmado, verificar se já existe `LocalFile` para o caminho. Se existir, reutilizar o UUID e criar a associação necessária. Caso contrário, criar `LocalFile` pela Factory e persistir a associação.

Não importar automaticamente subpastas, não tratar diretórios como arquivos etiquetáveis e não cadastrar todos os arquivos pela mera navegação.

Seleção múltipla foi aceita nesse fluxo de associação inicial. Não existe especificação completa de seleção múltipla para toda operação de arquivo. Novas formas de seleção em lote não devem ser inventadas.

<a id="op-02"></a>

## OP-02 — Utilização e recuperação de referência indisponível

**Estado: confirmada no fluxo principal.**

Antes de utilizar um arquivo registrado em uma operação, verificar sua disponibilidade. Se não for encontrado no caminho registrado, oferecer:

```text
Localizar
Remover do Tag-File
Cancelar
```

Ao localizar novamente, o usuário indica o caminho correspondente; o mesmo registro passa a apontar para esse endereço, preservando UUID e associações.

Não foi definido o resultado quando o caminho escolhido já pertence a outro `LocalFile`. A unicidade revela o conflito, mas não autoriza mesclar registros automaticamente.

Remover do Tag-File não equivale a apagar o arquivo físico. Seu alcance imediato deve permanecer coerente com [P-02](decisoes-e-pendencias.md#p-02).

Abrir o arquivo em uma aplicação associada foi usado como fluxo de utilização. A API Java concreta para fazê-lo não foi fechada. Não invente uma ferramenta ou integração adicional.

<a id="op-03"></a>

## OP-03 — Mover, recortar e colar

**Estado: confirmada.**

Mover um arquivo registrado preserva `LocalFile`, UUID e Tags; altera sua localização depois da operação física.

Recortar não move imediatamente. Registra uma intenção pendente no `ClipboardService`. Colar em uma pasta executa a movimentação correspondente.

Fluxo confirmado pelo solicitante:

```text
Arquivos por Tag
→ selecionar Faculdade
→ selecionar prova.pdf
→ Recortar

Arquivos Local
→ navegar até ~/Documentos/Faculdade
→ Colar
```

Resultado: arquivo físico no destino, mesmo UUID e Tags no registro; a visão local mostra as Tags junto do arquivo.

Um arquivo sem `LocalFile` também pode ser manipulado na visão local, sem criar registro SQL apenas para permitir a operação.

Conflitos seguem [OP-06](requisitos-e-regras.md#op-06); falhas, [ERR-01](requisitos-e-regras.md#err-01). Não afirme que a atualização do disco e do banco é atômica.

<a id="op-04"></a>

## OP-04 — Copiar

**Estado: função confirmada; detalhes de identidade e cópia sem Tags em [P-01](decisoes-e-pendencias.md#p-01).**

Copiar produz outro arquivo físico, mantendo a origem. A UI deve perguntar se a cópia receberá as Tags do original. Não foram aprovados “sempre copiar Tags” nem “nunca copiar Tags”.

Quando dois arquivos em caminhos distintos estiverem simultaneamente registrados, o modelo precisa distinguir esses registros. Essa necessidade não resolve sozinha a controvérsia de UUID surgida na substituição.

Não se sabe inequivocamente se uma cópia sem Tags deve permanecer apenas `NativeFile` ou receber um `LocalFile` temporário em `Etiqueta Ausente`. A primeira opção foi sugerida em uma consolidação anterior, mas não foi explicitamente aprovada. Preserve [P-01](decisoes-e-pendencias.md#p-01).

Não transforme copiar em mover a origem, não atribua silenciosamente um único caminho a dois arquivos registrados e não decida a identidade sobrevivente da substituição sem a resolução correspondente.

<a id="op-05"></a>

## OP-05 — Renomear e mudar extensão

**Estado: confirmada.**

Renomear altera nome e localização representada; para um arquivo cadastrado, a operação normal preserva sua identidade.

Quando a extensão mudar e alguma Tag se tornar incompatível, oferecer antes de concluir:

```text
Remover as Tags incompatíveis
Adicionar a nova extensão às Tags incompatíveis
Cancelar
```

As Tags compatíveis permanecem. Se a retirada das incompatíveis deixar o arquivo sem Tag normal, [CIC-01](requisitos-e-regras.md#cic-01) se aplica. Não retire associações silenciosamente.

O recurso é renomeação, não conversão do conteúdo para outro formato. Não documente conversão de arquivo como funcionalidade implícita.

<a id="op-06"></a>

## OP-06 — Conflitos de nome ou caminho

**Estado: parcialmente confirmada.**

Oferecer, conforme aplicável:

```text
Substituir
Manter os dois
Cancelar
```

- **Substituir:** o arquivo copiado sobrescreve fisicamente o existente no destino. O resultado exato de UUID e associações em caso de cópia está em [P-01](decisoes-e-pendencias.md#p-01).
- **Manter os dois:** preservar os dois arquivos com caminhos distintos. `prova (1).pdf` foi exemplo, não algoritmo aprovado.
- **Cancelar:** não autoriza a operação conflitante ainda não executada.

Não foi fechada uma matriz de disponibilidade dessas opções para cada operação, especialmente renomeação. O tratamento de colisão só no SQL, sem arquivo físico existente, também não foi definido.

<a id="op-07"></a>

## OP-07 — Cancelamento versus falha parcial

**Estado: consequência derivada com limites confirmados.**

Cancelar antes da confirmação impede aquela ação. Isso não é um mecanismo de reversão de etapas já concluídas.

O projeto não possui Undo/Redo, transações explícitas ou restauração garantida. Quando uma etapa termina e a seguinte falha, descrever o resultado parcial conforme [ERR-01](requisitos-e-regras.md#err-01). Não prometer “tudo desfeito”: essa garantia não foi aprovada para o projeto.

<a id="op-08"></a>

## OP-08 — Clipboard interno

**Estado: confirmada.**

`ClipboardService` é compartilhado pelos exploradores e mantém o estado necessário para COPY/CUT/PASTE dentro do Tag-File. A integração com o clipboard do sistema operacional não será implementada; apenas uma explicação de possível evolução deve existir em comentários.

`COPY` e `CUT` foram os estados apresentados. A colagem consulta o estado para escolher a ação.

A representação interna final não foi fechada: surgiram `List<LocalFile>`, depois `List<NativeFile>` e uma sugestão de contexto adicional. Não crie uma nova classe de contexto como decisão aprovada. O requisito é suportar arquivos com e sem registro, mantendo corretamente a relação com `LocalFile` quando ela existir.

Não foram definidos histórico do clipboard, persistência entre execuções, comportamento após colagem ou repetição de uma colagem de CUT.

<a id="del-01"></a>

## DEL-01 — Deletar apenas a etiqueta

**Estado: confirmada.**

Excluir a Tag selecionada e remover somente suas associações. Preservar os arquivos físicos e as outras Tags dos arquivos.

```text
Antes: prova.pdf → Faculdade, PDF, Importante
Excluir apenas Faculdade
Depois: prova.pdf → PDF, Importante
```

Se a Tag excluída era a última Tag normal, a regra de reorganização atual conduz a `Etiqueta Ausente`, em vez de apagar automaticamente o registro na sessão.

<a id="del-02"></a>

## DEL-02 — Deletar todas as etiquetas dos arquivos que possuem a selecionada

**Estado: efeito originalmente confirmado; relação com regra posterior em [P-02](decisoes-e-pendencias.md#p-02).**

A decisão explícita original determinou remover os `LocalFile` atingidos e todas as suas linhas na join-table, mantendo os arquivos físicos.

```text
Seleção: Tag Faculdade
prova.pdf possui Faculdade, PDF, Importante

Efeito originalmente definido:
LocalFile de prova.pdf → removido
Todas as associações desse registro → removidas
Arquivo físico → permanece
```

Não apagar globalmente as Tags `PDF` e `Importante`, nem retirar etiquetas de arquivos fora do conjunto atingido.

[P-02](decisoes-e-pendencias.md#p-02) precisa esclarecer se a remoção continua imediata depois da introdução de `Etiqueta Ausente` e se a Tag selecionada também é excluída ou fica vazia.

<a id="del-03"></a>

## DEL-03 — Deletar permanentemente os arquivos da etiqueta

**Estado: confirmada quanto ao resultado final.**

Excluir permanentemente os arquivos físicos associados, seus registros `LocalFile`, todas as associações desses registros e a Tag selecionada.

Outras Tags continuam existindo, mas deixam de conter os registros excluídos. Não se apagam essas Tags globalmente apenas por terem classificado o mesmo arquivo.

Não foi especificada a ordem exata das etapas, política por lote ou recuperação de falha entre disco e banco. Não garantir restauração, envio para lixeira ou reversibilidade.

<a id="del-04"></a>

## DEL-04 — Confirmações e clareza

**Estado: confirmada.**

A UI deve diferenciar as três consequências. Antes de excluir registros ou arquivos que possuem outras Tags, deve haver confirmação adicional listando quais são as outras etiquetas afetadas.

As Tags predefinidas comuns têm confirmação reforçada. `Etiqueta Ausente` é protegida.

Não reduza as três modalidades a botões indistinguíveis chamados apenas “Excluir”. Os textos de exemplos não precisam ser copiados literalmente, mas precisam explicar separadamente retirada de associação, remoção do registro e exclusão física.

<a id="syn-01"></a>

## SYN-01 — Quando atualizar e com qual alcance

**Estado: confirmada.**

| Gatilho | Alcance |
|---|---|
| Inicialização | Todos os `LocalFile`, além da limpeza de inicialização separada. |
| Utilização de arquivo em operação | Verificar o arquivo utilizado. |
| Troca de aba | Refresh completo de todos os `LocalFile`. |
| Antes de aplicar filtros | Refresh completo de todos os `LocalFile`. |
| Botão Refresh | Refresh completo de todos os `LocalFile`. |

Não realizar sincronização completa automática a cada pasta navegada nem monitoramento contínuo.

Listar uma pasta para mostrá-la e consultar correspondências no Map não se confunde com Refresh global. Não impedir navegação normal sob a interpretação equivocada de que nenhuma leitura pode ocorrer ao entrar em pasta.

<a id="syn-02"></a>

## SYN-02 — Metadados sincronizados

**Estado: consolidado no fluxo discutido.**

Sincronizar disponibilidade, tamanho, criação, modificação e último acesso do arquivo. `LocalFileManager` coordena a leitura nativa e a persistência.

Não foi decidido executar `UPDATE` quando nenhum campo mudou, nem o que gravar quando determinada data não puder ser lida. Preservar [P-10](decisoes-e-pendencias.md#p-10) e não inventar datas substitutas ou valores zero.

<a id="syn-03"></a>

## SYN-03 — Refresh único, sem limpeza nem duplicação

**Estado: confirmada no botão; demais restrições derivadas.**

Existe um único botão Refresh compartilhado pelos exploradores. Uma solicitação de atualização global não deve ser duplicada porque duas Screens recebem uma notificação.

O Observer atualiza a apresentação; suas notificações não devem produzir um ciclo de novo Refresh global.

`refreshAll()` não executa a limpeza por ausência de Tags.

<a id="err-01"></a>

## ERR-01 — Falhas e resultados parciais

**Estado: confirmada.**

Exibir pop-up com:

```text
Mensagem compreensível
O que foi concluído
O que falhou
Área expansível com detalhes técnicos
```

Detalhes podem incluir mensagem da exceção, SQLState, código MySQL e código de saída de processo quando existirem. Diferencie erro de compilação de erro durante execução. Não invente um código de saída onde não há processo externo.

Exemplo:

```text
Concluído: o arquivo foi movido para a pasta de destino.
Falhou: o novo caminho não foi salvo no MySQL.
Detalhes técnicos: mensagem e identificadores disponíveis.
```

Não afirmar recuperação automática, repetição garantida, rollback de arquivo ou restauração. A política de continuar/parar um lote após falha não foi definida.

Não exponha credenciais reais nos exemplos de diagnóstico. A credencial didática do projeto está definida separadamente; isso não autoriza copiar segredos do repositório para os documentos.

## Leitura conjunta das operações

As Screens encaminharão as interações aos Controllers. [ARQ-03](arquitetura-e-padroes.md#arq-03) distribui a execução direta entre os Managers/Services já aprovados; LocalFileManager coordena o registro e NativeFileService atua no disco. Os DAOs persistem, e o Controller publica a atualização pertinente. O exemplo de associação, a colagem entre visões e o Refresh estão explicados em [orientação de construção](arquitetura-e-padroes.md#orientacao-construcao).

As limitações da versão estão em [visão geral](visao-geral.md#restricoes). Copiar/substituir permanece limitado por [P-01](decisoes-e-pendencias.md#p-01); a modalidade 2 e a remoção explícita por [P-02](decisoes-e-pendencias.md#p-02); silenciar sugestões por [P-03](decisoes-e-pendencias.md#p-03). Nenhuma sequência deve transformar essas alternativas em resultados fechados.
