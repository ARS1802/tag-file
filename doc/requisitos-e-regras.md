# Requisitos e regras do Tag-File

[Índice da documentação](../README.md) · [Casos de uso](casos-de-uso.md) · [Interface](interface-e-fluxos.md)

Este é o documento principal das regras `TAG`, `EXT`, `CIC`, `OP`, `DEL`, `SYN` e `ERR`. Ele descreve o comportamento planejado da especificação consolidada, com o estado de cada decisão. Os fluxos operacionais fazem referência a essas regras em [casos de uso](casos-de-uso.md). A legenda dos estados está no [índice](../README.md).

**Estado observado:** [src/Main.java](../src/Main.java) contém somente o exemplo inicial de console, com uma saudação e laço de 1 a 5. A inspeção não encontrou implementação de Tags, exploradores, operações físicas, persistência ou sincronização. Nenhum resultado abaixo deve ser interpretado como funcionalidade já implementada ou teste aprovado.

O [modelo de domínio](modelo-de-dominio.md#dom-01) distingue arquivo físico (`NativeFile`), diretório (`NativeDirectory`), registro (`LocalFile`) e associação com `Tag`. UUID identifica o registro; caminho é localização alterável e única no banco ([DOM-02](modelo-de-dominio.md#dom-02)). Os exemplos são didáticos e não foram executados.

## Tags e extensões

<a id="tag-01"></a>

### TAG-01 — Criação, edição e nomes repetidos

**Estado: confirmada.**

O usuário pode criar Tags próprias e configurar nome, cor hexadecimal e extensões permitidas. Pode editar a classificação, respeitando os efeitos de alterações de extensão em [EXT-03](#ext-03).

O nome de uma Tag **não é UNIQUE**. Ao detectar nome já existente, a UI deve pedir confirmação e permitir criar mesmo assim. A nova Tag possui outra identidade, mesmo com o mesmo nome.

**Proposta não aprovada:** comparar nomes ignorando maiúsculas/minúsculas e espaços nas extremidades. A regra exata de comparação, validação de nome vazio, formato hexadecimal, cor padrão e obrigação de escolha continuam em [P-09](decisoes-e-pendencias.md#p-09).

**Consequência derivada:** ações e relações identificam a Tag por UUID, não apenas por nome. Isso também vale para predefinidas e para a Tag de sistema.

<a id="tag-02"></a>

### TAG-02 — Tags predefinidas

**Estado: confirmada quanto aos nomes e à proteção.**

Os nomes aprovados são `Imagens`, `PDF`, `Audios`, `Videos` e `Etiqueta Ausente`. As quatro primeiras podem ser apagadas com confirmação reforçada; `Etiqueta Ausente` não pode ser apagada pelo usuário.

**Exemplos de extensões iniciais discutidos, sem catálogo completo homologado:**

| Tag | Extensões de exemplo |
|---|---|
| Imagens | `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp` |
| PDF | `.pdf` |
| Audios | `.mp3`, `.wav`, `.flac`, `.aac` |
| Videos | `.mp4`, `.mkv`, `.avi`, `.mov` |

A criação inicial não autoriza recriar a cada abertura as predefinidas que o usuário excluiu. Identificação técnica, preparação das predefinidas e editabilidade da sentinela continuam em [P-07](decisoes-e-pendencias.md#p-07). Nomes duplicados impedem identificar a sentinela apenas comparando seu texto; coluna especial, enum e UUID fixo não foram aprovados como solução.

<a id="tag-03"></a>

### TAG-03 — Tags vazias e contagem de disponíveis

**Estado: confirmada.**

Tags podem existir vazias. Deve haver funcionalidade para encontrá-las e permitir sua exclusão, conforme [UC-13](casos-de-uso.md#uc-13).

A quantidade de arquivos associados com `available = true` será consultada no banco quando necessária. A Tag não terá contador persistido de disponíveis.

**Distinção derivada:** Tag vazia tem zero associações. Uma Tag com associações somente a arquivos indisponíveis tem zero disponíveis, mas não está vazia. A busca de Tags vazias considera associações, não apenas disponibilidade.

A proteção de `Etiqueta Ausente` vale mesmo vazia e também na funcionalidade de localizar/excluir Tags vazias.

<a id="ext-01"></a>

### EXT-01 — Extensões específicas, múltiplas e normalizadas

**Estado: confirmada na semântica.**

Uma Tag aceita zero, uma ou várias extensões, incluindo específicas como `.cdr`, sem enumeração fechada de categorias. Ausência de extensões configuradas significa **ausência de restrição**: qualquer extensão é aceita.

Coleção vazia foi proposta como representação Java; `null` também apareceu originalmente. A API de nulabilidade não está inteiramente fechada. A normalização confirmada é para minúsculas e com ponto inicial:

```text
PDF   → .pdf
.PDF  → .pdf
jpeg  → .jpeg
CDR   → .cdr
```

A mesma extensão não pode ser registrada duas vezes para a mesma Tag; Tags diferentes podem aceitar a mesma extensão ([SQL-02](banco-de-dados.md#sql-02)).

`FileType` foi um nome inicial. O conceito final é extensão real, não categoria `IMAGE`, `AUDIO` ou `VIDEO`. Não existe decisão inequívoca exigindo uma classe ou enum `FileType`. Arquivos sem extensão, extensões compostas e nomes especiais permanecem em [P-08](decisoes-e-pendencias.md#p-08).

<a id="ext-02"></a>

### EXT-02 — Incompatibilidade ao associar

**Estado: confirmada.**

**Gatilho e condição:** tentativa de associar um arquivo a uma Tag restrita que não aceita sua extensão. A interface informa a extensão e a Tag afetada e oferece:

```text
Criar nova etiqueta
Adicionar a extensão à etiqueta atual
Cancelar
```

Exemplo: `foto.png` é arrastado sobre `PDF`, que aceita apenas `.pdf`. O usuário pode criar outra Tag, permitir `.png` em `PDF` ou cancelar. A associação incompatível não acontece silenciosamente. Cancelar não autoriza a nova associação nem ampliar a restrição.

O aviso é amigável; incompatibilidade não é falha irrecuperável do aplicativo. O filtro do seletor ajuda a escolher arquivos, mas a regra também vale para Drag and Drop e não pode depender exclusivamente da aparência do seletor. Detalhes de tratamento em lote não estão fechados ([P-12](decisoes-e-pendencias.md#p-12)).

<a id="ext-03"></a>

### EXT-03 — Edição de extensões já utilizadas

**Estado: confirmada, com consequências derivadas indicadas abaixo.**

Quando editar extensões tornar arquivos associados incompatíveis, a UI lista os arquivos afetados e pede confirmação antes de retirar a extensão e as associações incompatíveis.

```text
Tag Faculdade aceita .pdf e .docx.
Arquivos: prova.pdf, trabalho.docx.
Usuário altera a restrição para aceitar somente .docx.
```

Se confirmar, `prova.pdf` perde apenas a associação com `Faculdade`; outras Tags permanecem. Se perder a última Tag normal, aplica-se [CIC-01](#cic-01). O arquivo físico não é apagado. Sem confirmação, as associações incompatíveis não são retiradas silenciosamente.

**Consequências derivadas:**

- A compatibilidade é avaliada contra o conjunto final de extensões.
- Remover a última extensão configurada deixa a Tag sem restrição; não rejeita todos os arquivos.
- Passar de ausência de restrição para uma lista específica também pode produzir incompatibilidades. O estado final obedece à mesma regra, embora não haja um diálogo separado homologado para essa transição.

Tratamento por arquivo em lote e botões adicionais não foram aprovados ([P-12](decisoes-e-pendencias.md#p-12)).

<a id="tag-04"></a>

### TAG-04 — Sugestão de Tag predefinida

**Estado: parcialmente confirmada; [P-03](decisoes-e-pendencias.md#p-03) permanece aberta.**

Quando um `LocalFile` é criado, deve ser apresentada a possibilidade de adicionar uma Tag predefinida compatível com a extensão. Por exemplo, ao registrar `prova.pdf` em `Faculdade`, o programa pode sugerir a associação adicional com `PDF`.

A interface possui uma opção equivalente a **“Não perguntar novamente nesta sessão”**. A supressão termina na próxima inicialização; não é uma preferência permanente persistida.

**Pendente:** silenciar apenas impede futuras sugestões sem associar Tags automaticamente, ou repete a resposta dada para os próximos arquivos? [P-03](decisoes-e-pendencias.md#p-03) não foi resolvida pela exportação da especificação. Também não há critério fechado para escolher entre várias predefinidas compatíveis.

Aplicação automática obrigatória e reclassificação automática a cada mudança de extensão não foram aprovadas.

## Ciclo de vida do registro

<a id="cic-01"></a>

### CIC-01 — Primeira classificação e reorganização durante a sessão

**Estado: confirmada no fluxo de reorganização.**

Exibir um arquivo em `Arquivos Local` não cria registro SQL. Associar a primeira Tag passa a exigir um `LocalFile`; se o caminho já estiver registrado, o registro e o UUID são reutilizados.

Quando o usuário retira a última Tag normal durante a sessão, o `LocalFile` permanece e recebe automaticamente a Tag protegida `Etiqueta Ausente`:

```text
prova.pdf: Faculdade, PDF
remove Faculdade → PDF
remove PDF → Etiqueta Ausente
```

Assim o arquivo continua acessível em `Arquivos por Tag` para reorganização, sem obrigar o usuário a procurá-lo novamente na pasta física. Ao adicionar uma Tag normal, a associação com a sentinela é retirada automaticamente:

```text
Etiqueta Ausente + nova Tag Importante
→ resultado: Importante
```

A Tag de sistema permanece cadastrada; somente a associação é temporária. O alcance dessa regra sobre remoções explícitas de registros continua em [P-02](decisoes-e-pendencias.md#p-02), especialmente [DEL-02](#del-02) e [OP-02](#op-02).

<a id="cic-02"></a>

### CIC-02 — Limpeza exclusiva da inicialização

**Estado: confirmada.**

Na abertura do programa, remover do banco:

1. Os `LocalFile` cuja única Tag seja `Etiqueta Ausente`.
2. Defensivamente, os `LocalFile` sem associação em `LOCAL_FILE_TAG`.

Remover também suas associações, conforme o efeito definido. **Os arquivos físicos permanecem.** A Tag `Etiqueta Ausente` permanece cadastrada e fica vazia depois da limpeza.

Indisponibilidade não é falta de classificação: arquivos indisponíveis não são excluídos apenas por estarem indisponíveis. A identificação técnica da sentinela está em [P-07](decisoes-e-pendencias.md#p-07), pois o nome pode repetir. A preparação e abertura estão em [UC-01](casos-de-uso.md#uc-01).

<a id="cic-03"></a>

### CIC-03 — Limites da limpeza

**Estado: derivada das decisões de sessão e de Refresh.**

Não executam a limpeza de inicialização: Refresh, troca de aba, aplicação de filtros, navegação de pasta, reconexão com MySQL e notificação do Observer. Reconectar não reinicia a sessão de organização.

O adiamento em relação à modalidade [DEL-02](#del-02) continua em [P-02](decisoes-e-pendencias.md#p-02). A remoção imediata originalmente definida e a associação temporária à sentinela não são resultados equivalentes nem podem ser combinados como decisão resolvida.

## Operações sobre arquivos

<a id="op-01"></a>

### OP-01 — Seleção, cadastro e associação inicial

**Estado: confirmada.**

Formas aprovadas: seleção individual de arquivo; Drag and Drop de arquivos sobre a representação de uma Tag; seleção de arquivos dentro de uma pasta durante a criação de Tag. `JFileChooser` e `FileNameExtensionFilter` são componentes adotados.

Ao criar Tag com restrição, o usuário pode abrir uma pasta e selecionar somente os arquivos desejados compatíveis. Não é obrigado a associar todos os encontrados:

```text
Nova Tag: Faculdade
Extensão: .pdf
Pasta: ~/Jogos/Megaman

PDFs visíveis: Megaman-Manual.pdf, prova.pdf
Seleção do usuário: prova.pdf
```

Para cada arquivo confirmado, verificar se existe `LocalFile` para o caminho. Se existir, reutilizar UUID e criar a associação necessária. Caso contrário, criar `LocalFile` pela Factory e persistir a associação ([ARQ-02](arquitetura-e-padroes.md#arq-02)).

A mera navegação não cadastra todos os arquivos. Não há importação automática de subpastas nem classificação de diretórios. Seleção múltipla foi aceita nesse fluxo inicial; especificação geral de seleção múltipla para todas as operações permanece em [P-12](decisoes-e-pendencias.md#p-12). Correspondência entre caminhos equivalentes depende de [P-08](decisoes-e-pendencias.md#p-08).

<a id="op-02"></a>

### OP-02 — Utilização e recuperação de referência indisponível

**Estado: confirmada no fluxo principal.**

Antes de utilizar um arquivo registrado em uma operação, verificar sua disponibilidade. Se não encontrado no caminho registrado, oferecer:

```text
Localizar
Remover do Tag-File
Cancelar
```

Ao localizar novamente, o usuário indica o caminho correspondente. O mesmo registro passa a apontar para esse endereço, preservando UUID e associações. Não foi definido o resultado se esse caminho já pertencer a outro `LocalFile`; unicidade não autoriza mesclar registros automaticamente ([P-08](decisoes-e-pendencias.md#p-08)).

Remover do Tag-File não equivale a apagar o arquivo físico. O alcance imediato da remoção precisa ser resolvido em [P-02](decisoes-e-pendencias.md#p-02).

Abrir em aplicação associada foi usado como fluxo de utilização. A API Java concreta para isso não está fechada e nenhuma integração adicional foi aprovada.

<a id="op-03"></a>

### OP-03 — Mover, recortar e colar

**Estado: confirmada.**

Mover arquivo registrado preserva `LocalFile`, UUID e Tags; altera sua localização depois da operação física. Recortar não move imediatamente: registra intenção pendente no `ClipboardService`. Colar em uma pasta executa a movimentação correspondente.

```text
Arquivos por Tag
→ selecionar Faculdade
→ selecionar prova.pdf
→ Recortar

Arquivos Local
→ navegar até ~/Documentos/Faculdade
→ Colar
```

Após sucesso, o arquivo físico está no destino, o registro tem o mesmo UUID e Tags, e a visão local mostra essas Tags junto do arquivo. Um arquivo sem `LocalFile` também pode ser manipulado na visão local, sem criar registro SQL para permitir a operação.

Conflitos seguem [OP-06](#op-06); falhas seguem [ERR-01](#err-01). Atualização de disco e banco não é atômica. Clipboard após colagem e lotes permanecem em [P-12](decisoes-e-pendencias.md#p-12).

<a id="op-04"></a>

### OP-04 — Copiar

**Estado: função confirmada; identidade e cópia sem Tags em [P-01](decisoes-e-pendencias.md#p-01).**

Copiar produz outro arquivo físico e mantém a origem. A UI pergunta se a cópia receberá as Tags do original. Nem “sempre copiar Tags” nem “nunca copiar Tags” foram aprovados.

Se dois arquivos em caminhos distintos estiverem simultaneamente registrados, o modelo deve distingui-los. Isso não resolve a controvérsia de UUID na substituição.

**Pendências no ponto de uso:** quando a cópia não herda Tags, ela permanece somente `NativeFile` ou recebe `LocalFile` temporário em `Etiqueta Ausente`? Na substituição, qual UUID permanece em cada caminho e qual registro é excluído? Se já existir um registro da própria cópia, ele assume o caminho final sem gerar outro UUID? Essas perguntas constam de [P-01](decisoes-e-pendencias.md#p-01).

Copiar não move a origem. A documentação não atribui um único caminho a dois registros nem escolhe uma identidade sobrevivente. A primeira alternativa de cópia sem Tags foi sugerida, mas não explicitamente aprovada. Ver [UC-08](casos-de-uso.md#uc-08).

<a id="op-05"></a>

### OP-05 — Renomear e mudar extensão

**Estado: confirmada.**

Renomear altera nome e localização representada; a operação normal de arquivo cadastrado preserva identidade. Se a extensão mudar e alguma Tag ficar incompatível, oferecer antes de concluir:

```text
Remover as Tags incompatíveis
Adicionar a nova extensão às Tags incompatíveis
Cancelar
```

Tags compatíveis permanecem. Se retirar incompatíveis deixar o arquivo sem Tag normal, aplica-se [CIC-01](#cic-01). Associações não são retiradas silenciosamente.

Renomeação não converte o conteúdo para outro formato. Nomes e extensões especiais estão em [P-08](decisoes-e-pendencias.md#p-08), e o comportamento geral de lotes em [P-12](decisoes-e-pendencias.md#p-12).

<a id="op-06"></a>

### OP-06 — Conflitos de nome ou caminho

**Estado: parcialmente confirmada.**

Oferecer, conforme aplicável:

| Alternativa | Efeito e limite |
|---|---|
| Substituir | O arquivo copiado sobrescreve fisicamente o existente no destino. UUID e associações em cópia permanecem em [P-01](decisoes-e-pendencias.md#p-01). |
| Manter os dois | Preservar os dois arquivos em caminhos distintos. `prova (1).pdf` foi exemplo, não algoritmo homologado. |
| Cancelar | Não autoriza a operação conflitante ainda não executada. |

Não foi fechada uma matriz de disponibilidade dessas opções para cada operação, especialmente renomeação. Também não foi definido conflito somente no SQL, sem arquivo físico existente. Política de nomes, equivalência de caminhos, links e colisão ao relocalizar permanecem em [P-08](decisoes-e-pendencias.md#p-08).

<a id="op-07"></a>

### OP-07 — Cancelamento versus falha parcial

**Estado: consequência derivada com limites confirmados.**

Cancelar antes da confirmação impede aquela ação; não reverte etapas já concluídas. O projeto não possui Undo/Redo, transações explícitas ou restauração garantida. Se uma etapa termina e a seguinte falha, informar o resultado parcial por [ERR-01](#err-01). Não há garantia de “tudo desfeito”.

<a id="op-08"></a>

### OP-08 — Clipboard interno

**Estado: confirmada na finalidade; representação interna aberta.**

`ClipboardService` é compartilhado pelos exploradores e mantém o estado necessário para COPY/CUT/PASTE dentro do Tag-File. `COPY` e `CUT` foram os estados apresentados; a colagem consulta o estado para escolher a ação.

Integração com o clipboard do sistema operacional está fora da versão atual, com possibilidade de evolução apenas comentada. Não há histórico ou persistência entre execuções definidos.

A representação passou por `List<LocalFile>`, `List<NativeFile>` e proposta de contexto adicional. Nenhuma classe nova de contexto está aprovada. O requisito é suportar arquivos com e sem cadastro e manter a relação correta com `LocalFile` quando existir.

Comportamento após colagem e repetição de CUT permanecem em [P-12](decisoes-e-pendencias.md#p-12); identidade da cópia continua em [P-01](decisoes-e-pendencias.md#p-01).

## Três modalidades de exclusão

<a id="del-01"></a>

### DEL-01 — Deletar apenas a etiqueta

**Estado: confirmada.**

Excluir a Tag selecionada e somente suas associações. Preservar arquivos físicos e outras Tags dos arquivos.

```text
Antes: prova.pdf → Faculdade, PDF, Importante
Excluir apenas Faculdade
Depois: prova.pdf → PDF, Importante
```

Se era a última Tag normal, [CIC-01](#cic-01) conduz à associação com `Etiqueta Ausente`, mantendo o registro durante a sessão.

<a id="del-02"></a>

### DEL-02 — Deletar todas as etiquetas dos arquivos que possuem a selecionada

**Estado: efeito originalmente confirmado; relação com regra posterior em [P-02](decisoes-e-pendencias.md#p-02).**

A decisão explícita original determinou remover os `LocalFile` atingidos e todas as suas linhas na join-table, preservando os arquivos físicos.

```text
Seleção: Tag Faculdade
prova.pdf possui Faculdade, PDF, Importante

Efeito originalmente definido:
LocalFile de prova.pdf → removido
Todas as associações desse registro → removidas
Arquivo físico → permanece
```

As Tags `PDF` e `Importante` não são apagadas globalmente, nem se retiram etiquetas de arquivos fora do conjunto atingido.

**Duas decisões abertas em P-02:** essa remoção continua imediata depois da introdução de `Etiqueta Ausente`, ou a sentinela também se aplica aqui? A Tag selecionada é excluída ou permanece vazia? O texto não combina os dois resultados como se fossem equivalentes.

<a id="del-03"></a>

### DEL-03 — Deletar permanentemente os arquivos da etiqueta

**Estado: confirmada quanto ao resultado final.**

Excluir permanentemente os arquivos físicos associados, seus registros `LocalFile`, todas as associações desses registros e a Tag selecionada. Outras Tags continuam existindo, mas deixam de conter os registros excluídos; não são apagadas globalmente por terem classificado o mesmo arquivo.

Ordem exata das etapas, política por lote e recuperação entre disco e banco não foram especificadas ([P-12](decisoes-e-pendencias.md#p-12)). Não há garantia de restauração, envio à lixeira ou reversibilidade.

<a id="del-04"></a>

### DEL-04 — Confirmações e clareza

**Estado: confirmada.**

A UI deve distinguir retirada de associação, remoção de registro e exclusão física. Antes de excluir registros ou arquivos que tenham outras Tags, pedir confirmação adicional listando essas etiquetas afetadas.

Predefinidas comuns exigem confirmação reforçada e `Etiqueta Ausente` é protegida. Não se reduzem as três modalidades a opções indistinguíveis chamadas apenas “Excluir”. A redação visual não precisa copiar literalmente os exemplos, mas deve explicar as consequências separadamente. Ver a tabela de efeitos em [UC-12](casos-de-uso.md#uc-12).

## Atualização, disponibilidade e falhas

<a id="syn-01"></a>

### SYN-01 — Quando atualizar e com qual alcance

**Estado: confirmada.**

| Gatilho | Alcance |
|---|---|
| Inicialização | Todos os `LocalFile`, além da limpeza separada e exclusiva da inicialização. |
| Utilização de arquivo em operação | Verificar o arquivo utilizado. |
| Troca de aba | Refresh completo de todos os `LocalFile`. |
| Antes de aplicar filtros | Refresh completo de todos os `LocalFile`. |
| Botão Refresh | Refresh completo de todos os `LocalFile`. |

Não há sincronização completa automática a cada pasta navegada nem monitoramento contínuo. Listar uma pasta e consultar correspondências em `Map` são leituras normais da navegação, distintas de Refresh global ([EXP-03](interface-e-fluxos.md#exp-03)).

<a id="syn-02"></a>

### SYN-02 — Metadados sincronizados

**Estado: consolidado no fluxo discutido.**

Sincronizar disponibilidade, tamanho, criação, modificação e último acesso físico. `LocalFileManager` coordena a leitura nativa e a persistência ([ARQ-07](arquitetura-e-padroes.md#arq-07)).

Não foi decidido executar `UPDATE` quando nenhum campo muda, nem o valor a gravar se uma data não puder ser lida. Ausência de metadados e conversão temporal permanecem em [P-10](decisoes-e-pendencias.md#p-10), sem datas substitutas ou zeros inventados. Representações de atributos estão em [DOM-03](modelo-de-dominio.md#dom-03); o DDL físico segue [P-06](decisoes-e-pendencias.md#p-06).

<a id="syn-03"></a>

### SYN-03 — Refresh único, sem limpeza nem duplicação

**Estado: confirmada no botão; demais restrições derivadas.**

Existe um único botão Refresh compartilhado pelos exploradores. Uma solicitação global não deve ser duplicada porque duas Screens recebem notificação. O Observer atualiza a apresentação; a notificação não deve criar um ciclo de novo Refresh global.

`refreshAll()` não executa limpeza por ausência de Tags. O [fluxo visual](interface-e-fluxos.md#fluxo-refresh) e [UC-14](casos-de-uso.md#uc-14) apresentam essa separação. A composição concreta das Screens está em [P-04](decisoes-e-pendencias.md#p-04) e a prevenção interna de reentrância em [P-12](decisoes-e-pendencias.md#p-12).

<a id="err-01"></a>

### ERR-01 — Falhas e resultados parciais

**Estado: confirmada.**

Apresentar pop-up com mensagem compreensível, o que foi concluído, o que falhou e área expansível de detalhes técnicos. Quando existirem, detalhes podem incluir mensagem da exceção, SQLState, código MySQL e código de saída de processo. Erro de compilação é distinto de erro durante execução; não há código de saída de processo a inventar quando nenhum processo externo participou.

**Exemplo didático, não executado:**

```text
Concluído: o arquivo foi movido para a pasta de destino.
Falhou: o novo caminho não foi salvo no MySQL.
Detalhes técnicos: mensagem e identificadores disponíveis.
```

Não há recuperação automática, repetição garantida, rollback de arquivo ou restauração. A política de continuar/parar lotes após falha não foi definida ([P-12](decisoes-e-pendencias.md#p-12)). [SQL-05](banco-de-dados.md#sql-05) registra a ausência de transações explícitas; [AMB-05](instalacao-e-execucao.md#amb-05) explica que reconexão não desfaz nem garante repetição de operações.

Diagnósticos não devem expor credenciais reais. A credencial didática pública está definida separadamente em [AMB-03](instalacao-e-execucao.md#amb-03). Falhas na preparação do ambiente e recuperação de schema continuam em [P-11](decisoes-e-pendencias.md#p-11).

## Limites da especificação

As restrições completas estão na [visão geral](visao-geral.md). Esta versão não inclui Maven, pool de conexões, transações explícitas, Undo/Redo, clipboard do sistema, monitoramento contínuo, classe `ExplorerEvent`, `schema_history`, Factory para objetos Native, contador persistido de disponíveis ou operador NOT. Essas escolhas acadêmicas não garantem atomicidade, reconexão perfeita ou recuperação de falhas.

As questões [P-01 a P-13](decisoes-e-pendencias.md) continuam explícitas nos assuntos afetados. Em particular, cópia/substituição, modalidade 2 de exclusão e supressão das sugestões permanecem sem decisão completa.
