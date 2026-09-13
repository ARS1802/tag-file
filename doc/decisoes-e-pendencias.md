# Decisões, pendências e diferenças do repositório

[Índice](../README.md) · [Rastreabilidade](rastreabilidade.md) · [Critérios de aceite](criterios-de-aceite.md)

> **P-01 a P-13 permanecem abertas.** A exportação do prompt e esta entrega documental não responderam P-01, P-02 ou P-03. Uma ausência de implementação não resolve nenhuma delas. A especificação determina o comportamento aprovado; o código só comprova o estado observado.

## Como interpretar o estado

Decisões confirmadas e consequências derivadas orientam os documentos principais. Propostas são identificadas como tal; versões substituídas aparecem apenas no histórico abaixo. As lacunas da implementação são descritas como **não encontradas no repositório inspecionado**, sem presumir a existência de classes ou scripts externos. Não há gravidade ou prioridade atribuída às diferenças.

## Histórico das decisões substituídas

| Definição antiga ou alternativa | Situação final a preservar |
|---|---|
| Apenas etiquetar sem modificar arquivos | Substituída pela inclusão de operações físicas. |
| Entidade `File` | Renomeada para LocalFile; depois distinguida de NativeFile. |
| Caminho como identidade suficiente | UUID identifica; caminho muda e é único. |
| Tipos genéricos IMAGE/AUDIO/VIDEO | Extensões reais, múltiplas e específicas. |
| Uma extensão por Tag | Zero, uma ou várias. |
| Contador persistido de disponíveis | Removido; consultar quando necessário. |
| Excluir imediatamente ao retirar a última Tag normal | Substituído pela sentinela e limpeza na próxima inicialização; alcance de remoções explícitas em P-02. |
| Limpeza dentro de todo refreshAll | Incompatível com a regra final; somente na inicialização. |
| LocalFile para todo arquivo local exibido | Rejeitado. |
| Clipboard integrado ao sistema | Fora do escopo; comentário futuro. |
| FileService | Nome substituído por NativeFileService. |
| LocalFileService para sincronização | Substituído por LocalFileManager. |
| Factory para NativeFile e NativeDirectory | Rejeitada. |
| Controller único para dois exploradores | Rejeitado; dois Controllers. |
| Controller registra diretamente listeners dos botões | Rejeitado; Screen faz a ligação. |
| Callbacks extras entre Controller e Screen | Não consolidados; a proposta final discutida foi as Screens observarem os eventos. |
| Controllers como Observers versus Screens | Núcleo de publicação definido; formalização concreta em P-04, sem impor simultaneamente as duas estruturas. |
| ExplorerEvent e enum obrigatórios | Rejeitados nesta versão; comentário de evolução. |
| Undo/Redo | Fora do escopo; comentário futuro. |
| Forçar join-table a CrudDAO | Rejeitado; DAO especializado. |
| Filtros nomeados somente por visão | Substituídos por NativeFileFilter, LocalFileFilter e TagFilter. |
| SQL separado por arquivo da pasta | Consulta em lote com Map foi aprovada. |
| Transações explícitas | Rejeitadas por decisão de escopo. |
| MySQL iniciado apenas externamente | Aplicativo deve verificar/preparar/iniciar sua instância. |
| Conta limitada para DAOs | Substituída por root na instância didática. |
| Porta de exemplo 3307 | Substituída por 3333. |
| Maven | Rejeitado. |
| Pool de conexões | Rejeitado. |
| schema_history | Rejeitado. |
| datadir obrigatoriamente na raiz de database | Corrigido: subdiretório permitido, referência runtime/data. |
| Inicializar fora e transferir dados | Descartado. |
| Refresh independente por explorador | Não é a definição final; botão único e atualização compartilhada. |

Sugestões de Strategy, Singleton, Abstract Factory e outras camadas não se tornam requisitos por terem sido mencionadas em explicações.

## Propostas e definições ainda não homologadas

| Tema | Estado a preservar | Onde consultar |
|---|---|---|
| Factory simples | Uma Factory para `LocalFile` e `Tag` foi aprovada; o nome `EntityFactory` e APIs aparecem em exemplos. Factory Method/Abstract Factory não estão aprovados. | [ARQ-02](arquitetura-e-padroes.md#arq-02) |
| Extensões e filtros | Coleção vazia como API, catálogo inicial completo, filtros de nome/disponibilidade/etiquetados e intervalos exatos não estão fechados. | [EXT-01](requisitos-e-regras.md#ext-01), [EXP-02](interface-e-fluxos.md#exp-02) |
| Interface | `SwingWorker`, componentes como `TagBadge` e classes extras de diálogo são possibilidades. Screens observadoras e composição com Panels estão em P-04. | [UI-01](interface-e-fluxos.md#ui-01), [UI-03](interface-e-fluxos.md#ui-03) |
| SQL | `CHAR(36)`, `BINARY(16)`, `BIGINT`, chaves compostas específicas, cascatas, comprimentos e índices não compõem um DDL homologado. | [SQL-02](banco-de-dados.md#sql-02), [SQL-03](banco-de-dados.md#sql-03) |
| Nomes e caminhos | Ignorar caixa/espaços em nome de Tag, gerar `prova (1).pdf` e tratar caminhos equivalentes não são algoritmos finais aprovados. | P-08, P-09 |
| Ordenação | Tamanho para desempate é possibilidade futura. Não há ordem padrão, direção, precedência, `Comparable` ou `Comparator` aprovados. | [EXP-05](interface-e-fluxos.md#exp-05), P-13 |
| Ambiente | Versões de MySQL/Connector/J, `pkexec`, comandos de instalação e estratégia completa de reaplicação de schema não estão definidos. | [AMB-01](instalacao-e-execucao.md#amb-01), P-11 |
| Padrões extras | Creator, Strategy, Singleton e novas camadas não são exigências por terem aparecido em explicações. | [ARQ-09](arquitetura-e-padroes.md#arq-09) |

## Pendências de comportamento com perguntas completas

<a id="p-01"></a>

## P-01 — Identidade na cópia/substituição e cópia sem Tags

**Conflito:** o efeito físico da sobrescrita é claro, mas os exemplos de identidade passaram a utilizar A, B e C com papéis diferentes. Foram expressas as orientações de prevalecer o `LocalFile`/UUID/Tags do arquivo copiado e de apenas sobrescrever o caminho de `LocalFileB`. Uma resposta posterior do assistente alterou o registro da origem como se a cópia fosse uma movimentação; essa resposta não é uma resolução confiável da decisão.

**Parte confirmada:** copiar mantém a origem física, há pergunta sobre herdar Tags e “Substituir” sobrescreve o arquivo do destino.

**Pergunta a registrar:** considerando origem O registrada em `/Documentos/prova.pdf` e destino D registrado em `/Backup/prova.pdf`, qual UUID fica associado a cada caminho após copiar com substituição e qual registro é excluído? Se o “LocalFile do arquivo copiado” for um registro da cópia já criado, ele apenas assume o caminho final, sem gerar outro UUID?

**Segunda pergunta:** quando não herda Tags, a cópia é apenas NativeFile ou recebe LocalFile temporário em `Etiqueta Ausente`?

**Dependências:** OP-04, OP-06, UC-08, modelo de identidade, descrição de CopyFileCommand, clipboard, persistência e critérios de aceite da cópia. Diagramas não devem escolher apenas uma das alternativas como resultado aprovado.

<a id="p-02"></a>

## P-02 — Modalidade 2 de exclusão versus adiamento para a inicialização

**Conflito:** a modalidade de retirar todas as etiquetas dos arquivos da Tag foi definida como remoção dos LocalFile e associações. Posteriormente foi definido, no contexto de reorganização, que os registros sem Tags normais permanecem durante a sessão em `Etiqueta Ausente`.

**Pergunta a registrar:** o adiamento vale somente para retirar a última Tag no fluxo comum, enquanto a modalidade 2 continua excluindo os registros imediatamente?

**Segunda pergunta:** nessa modalidade 2, a Tag selecionada também é excluída ou permanece vazia?

**Parte confirmada:** não apagar arquivos físicos na modalidade 2 e não apagar globalmente as outras Tags. Modalidade 3 inclui exclusão física e dos registros; a ordem/recuperação não é garantida.

**Dependências:** CIC-01, DEL-02, remoção explícita do Tag-File em OP-02, UC-11/UC-12, cleanup, diagramas de ciclo de vida e aceite. Não combinar remoção imediata e sentinela para o mesmo resultado como se fossem equivalentes.

<a id="p-03"></a>

## P-03 — Efeito de “Não perguntar novamente nesta sessão”

**Lacuna:** o prazo da preferência está definido, mas não seu efeito automático nos próximos arquivos.

**Pergunta a registrar:** a opção apenas silencia futuras sugestões sem associar automaticamente Tags predefinidas, ou repete para os próximos arquivos a escolha feita no diálogo?

**Parte confirmada:** ao reiniciar, a pergunta volta; não há preferência permanente e não há classificação automática obrigatória aprovada para toda mudança de extensão.

**Dependências:** TAG-04, criação de LocalFile, UC-04/UC-05 e testes de sugestão. Não selecionar uma política por ser mais confortável de programar.

## Demais pendências

| ID | Assunto | Tratamento documental |
|---|---|---|
| <a id="p-04"></a>P-04 | Relação Panels/Screens e implementação de ExplorerListener pelas Screens | Preservar `TagExplorerPanel` e `LocalFileExplorerPanel`; explicar a proposta final de Screens observadoras; não impor renomeação nem duplicação. |
| <a id="p-05"></a>P-05 | Retorno/assinaturas da consulta de arquivos por Tags e eventos para NativeFile sem cadastro | Diferenciar consulta de Tag de consulta de arquivo; não criar LocalFile artificial para notificar; registrar contratos abertos. |
| <a id="p-06"></a>P-06 | DDL físico, tipo UUID SQL, nulabilidade, limites, índices, collation, nome singular/plural de extensões e cascatas | Documentar regras lógicas confirmadas e o SQL realmente observado separadamente; não homologar um DDL novo. |
| <a id="p-07"></a>P-07 | Identificação técnica e editabilidade de Etiqueta Ausente e preparação das predefinidas | Não identificar a sentinela apenas por nome sem explicar o problema de duplicatas; não inventar coluna/enum/UUID fixo como aprovado. |
| <a id="p-08"></a>P-08 | Nomes de conflito, caminhos equivalentes, links, arquivos sem extensão, extensões compostas e colisão ao relocalizar | Manter a política em aberto; exemplos simples não são algoritmos gerais. |
| <a id="p-09"></a>P-09 | Comparação de nomes repetidos, validação de vazio, cor/defaults e formato hexadecimal | Separar sugestões de decisões; inspecionar a implementação sem promovê-la a requisito. |
| <a id="p-10"></a>P-10 | Ausência de metadados, conversão temporal e atualização de lastFileTaggedAt | Não inventar datas de substituição, triggers ou efeitos para ações repetidas. |
| <a id="p-11"></a>P-11 | Versões de servidor/driver, instaladores, verificação/reaplicação de schema e encerramento | Registrar o que existe e o que falta; não instalar ou executar para descobrir. |
| <a id="p-12"></a>P-12 | Seleção múltipla geral, comportamento de lotes com falhas, clipboard após colagem, prevenção interna de reentrância e arranjo lado a lado | Documentar o fluxo confirmado; não acrescentar mecanismo, fila ou comportamento automático. |
| <a id="p-13"></a>P-13 | Consulta sem Tags selecionadas, estados vazios da UI e ordenação padrão | Deixar visível a ausência de definição. |

Esses pontos não impedem produzir documentos úteis e completos quanto à cobertura. Eles impedem apenas afirmar que todas as decisões de implementação estão fechadas.

## Onde as respostas terão efeito

| Pendência | Documentos e regras a revisar conjuntamente quando houver decisão expressa |
|---|---|
| P-01 | [OP-04/OP-06](requisitos-e-regras.md#op-04), [UC-08](casos-de-uso.md#uc-08), [identidade](modelo-de-dominio.md#dom-02), [Command](arquitetura-e-padroes.md#arq-03), [persistência](banco-de-dados.md#sql-02), [ACE-20](criterios-de-aceite.md#ace-20) e clipboard. |
| P-02 | [CIC-01](requisitos-e-regras.md#cic-01), [DEL-02](requisitos-e-regras.md#del-02), [OP-02](requisitos-e-regras.md#op-02), [UC-11/UC-12](casos-de-uso.md#uc-11), ciclo de vida, cleanup e aceite da modalidade 2. |
| P-03 | [TAG-04](requisitos-e-regras.md#tag-04), [UC-04/UC-05](casos-de-uso.md#uc-04), diálogos e verificação de sugestões. |
| P-04, P-05 | [Arquitetura](arquitetura-e-padroes.md#arq-06), [interface](interface-e-fluxos.md#exp-01) e consultas; eventos devem comportar arquivos sem cadastro. |
| P-06, P-07 | [Banco](banco-de-dados.md#sql-01), [modelo](modelo-de-dominio.md#dom-04), predefinidas e limpeza da inicialização. |
| P-08, P-09, P-10 | [Modelo](modelo-de-dominio.md#dom-03), [regras](requisitos-e-regras.md#ext-01), persistência e casos que validam nomes, caminhos e metadados. |
| P-11 | [Instalação e execução](instalacao-e-execucao.md#amb-01), [SQL-06](banco-de-dados.md#sql-06) e encerramento. |
| P-12, P-13 | [Interface](interface-e-fluxos.md#exp-01), [casos de uso](casos-de-uso.md#uc-14), clipboard, lotes, ordenação e resultados vazios. |

## Limitações conhecidas

A lista integral de restrições está em [visão geral](visao-geral.md). Sem transações explícitas, não há garantia de atomicidade entre tabelas nem entre disco e MySQL. `autoReconnect=true` não garante repetição, reversão ou conclusão de escritas; reconectar também não reinicia a sessão de organização. Loading bloqueia interações conflitantes, mas seu mecanismo interno não está definido. A conta administrativa e a senha pública pertencem à instância didática local; não definem segurança de produção nem autorizam alterar instâncias alheias.

Undo/Redo, clipboard do sistema, monitoramento contínuo, `ExplorerEvent`, pool, Maven, `schema_history`, Factory Native e NOT estão fora da versão, conforme as restrições específicas. Os comentários de evolução previstos para Command, Observer, clipboard e transações ainda não têm classes correspondentes no código em que possam ser observados; a missão não os insere em Java.

## Diferenças entre planejamento e implementação

A inspeção cobriu os arquivos listados em [evidências](rastreabilidade.md#evidencias). Os registros abaixo são lacunas de implementação, não correções efetuadas nem decisões novas. O JDK 24 observado está alinhado ao contexto da especificação e não é uma divergência.

<a id="div-001"></a>

### DIV-001 — Domínio e operações sem implementação correspondente

- **Regras:** OBJ-01, DOM-01 a DOM-04, TAG-01 a TAG-04, EXT-01 a EXT-03, CIC-01 a CIC-03, OP-01 a OP-08 e DEL-01 a DEL-04.
- **Previsto:** entidades, classificação e operações físicas conforme aprovação parcial ou integral de cada regra; P-01/P-02/P-03 continuam abertas.
- **Observado:** [src/Main.java](../src/Main.java), símbolo `Main.main`, contém `System.out.printf("Hello and welcome!")` e laço de 1 a 5. O inventário de `src` não contém outras classes.
- **Consequência:** não há evidência de comportamento do domínio ou execução dos casos de uso.
- **Explicação:** [modelo de domínio](modelo-de-dominio.md), [regras](requisitos-e-regras.md), [casos de uso](casos-de-uso.md).
- **Decisão necessária:** resolver as pendências relacionadas para fechar esses contratos; a implementação fica fora desta missão.

<a id="div-002"></a>

### DIV-002 — Exploradores e colaborações arquiteturais ausentes

- **Regras:** EXP-01 a EXP-05, UI-01 a UI-03, SYN-01 a SYN-03, ERR-01 e ARQ-01 a ARQ-09.
- **Previsto:** UI Swing com dois exploradores, divisão de responsabilidades, Commands e núcleo Observer; ligações concretas e eventos ainda dependem de P-04/P-05.
- **Observado:** [src/Main.java](../src/Main.java), `Main.main`, é um programa de console sem componentes Swing, Controllers, DAOs ou Services. O inventário não encontra outros fontes nem comentários arquiteturais nos participantes previstos.
- **Consequência:** diagramas descrevem o projeto planejado; não são engenharia reversa de classes existentes. Refresh, Loading, erros e notificação não foram verificados em execução.
- **Explicação:** [arquitetura e padrões](arquitetura-e-padroes.md), [interface](interface-e-fluxos.md), [regras](requisitos-e-regras.md).
- **Decisão necessária:** contratos de P-04/P-05/P-12/P-13 permanecem abertos. Não há autorização documental para criar os participantes.

<a id="div-003"></a>

### DIV-003 — Persistência e esquema não encontrados

- **Regras:** SQL-01 a SQL-06, ARQ-04, ARQ-08 e AMB-05.
- **Previsto:** modelo lógico MySQL, DAOs, scripts de criação/alteração sem histórico e conexão compartilhada.
- **Observado:** [tag-file.iml](../tag-file.iml), componente `NewModuleRootManager`, declara apenas raiz de fontes e JDK herdado; [src/Main.java](../src/Main.java) não usa JDBC. O inventário não contém arquivos SQL, DAOs, `DatabaseConnection` ou diretório `database`.
- **Consequência:** não existe DDL observado para comparar tipos, cascatas, índices ou estratégias de atualização; nenhuma conexão foi validada.
- **Explicação:** [banco de dados](banco-de-dados.md), [arquitetura](arquitetura-e-padroes.md#arq-08).
- **Decisão necessária:** P-06 e P-11; ausência de código não homologa DDL novo nem resolve a estratégia de schema.

<a id="div-004"></a>

### DIV-004 — Preparação operacional e driver não presentes

- **Regras:** AMB-01 a AMB-07, em particular AMB-02, AMB-03 e AMB-06.
- **Previsto:** administração de instância local, scripts por plataforma, dados em `database/runtime/data`, porta 3333 e driver JDBC manual de referência.
- **Observado:** [.idea/misc.xml](../.idea/misc.xml), `ProjectRootManager`, configura `JDK_24` e `corretto-24`; [tag-file.iml](../tag-file.iml) não declara biblioteca Connector/J. O inventário não contém `lib`, JAR, scripts administrativos ou `database`. [.gitignore](../.gitignore) não possui regras específicas para dados/configurações dessa estrutura futura.
- **Consequência:** JDK configurado está alinhado, mas a aplicação planejada não dispõe de procedimento executável completo no repositório. Versões e scripts não são verificáveis; a política específica de versionamento operacional também não está pronta.
- **Explicação:** [instalação e execução](instalacao-e-execucao.md).
- **Decisão necessária:** P-11 e detalhes operacionais existentes em P-12. Não trocar JDK, porta, conta ou ferramentas; nenhuma configuração foi alterada.
