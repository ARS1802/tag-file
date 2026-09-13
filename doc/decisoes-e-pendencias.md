# Decisões e pendências

[Índice](README.md) · [Arquitetura](arquitetura-e-padroes.md) · [Rastreabilidade](rastreabilidade.md)

A especificação atual descreve o projeto que a equipe deverá construir. **[P-01](decisoes-e-pendencias.md#p-01) a [P-13](decisoes-e-pendencias.md#p-13) continuam abertas. [DEC-01](decisoes-e-pendencias.md#dec-01) é uma decisão confirmada e encerrada**, independente dessas pendências. O histórico abaixo permite compreender as mudanças, sem reapresentá-las como alternativas atuais.

Uma parte aprovada que será implementada depois não é uma decisão em aberto. Pendência significa contrato ou comportamento ainda não escolhido. Exemplos, diagramas e materiais de apoio não encerram pendências por sua simples existência.

## Histórico de revisões

| Definição antiga ou alternativa | Situação final a preservar |
|---|---|
| Missão centrada em inspecionar uma implementação e documentar o que já existe | Corrigida explicitamente: a documentação precede a implementação e orienta os estudantes sobre o que construir e como as partes deverão colaborar. Código não é pré-condição da missão. |
| Apenas etiquetar sem modificar arquivos | Substituída pela inclusão de operações físicas. |
| Entidade `File` | Renomeada para LocalFile; depois distinguida de NativeFile. |
| Caminho como identidade suficiente | UUID identifica; caminho muda e é único. |
| Tipos genéricos IMAGE/AUDIO/VIDEO | Extensões reais, múltiplas e específicas. |
| Uma extensão por Tag | Zero, uma ou várias. |
| Contador persistido de disponíveis | Removido; consultar quando necessário. |
| Excluir imediatamente ao retirar a última Tag normal | Substituído pela sentinela e limpeza na próxima inicialização; alcance de remoções explícitas em [P-02](decisoes-e-pendencias.md#p-02). |
| Limpeza dentro de todo refreshAll | Incompatível com a regra final; somente na inicialização. |
| LocalFile para todo arquivo local exibido | Rejeitado. |
| Clipboard integrado ao sistema | Fora do escopo; comentário futuro. |
| FileService | Nome substituído por NativeFileService. |
| LocalFileService para sincronização | Substituído por LocalFileManager. |
| Camada GoF Command e classes como `MoveFileCommand` | Retiradas por simplificação; Controllers chamam diretamente os Managers/Services pertinentes. Decisão confirmada e encerrada em [DEC-01](decisoes-e-pendencias.md#dec-01), não uma pendência nem uma melhoria futura. |
| Factory para NativeFile e NativeDirectory | Rejeitada. |
| Controller único para dois exploradores | Rejeitado; dois Controllers. |
| Controller registra diretamente listeners dos botões | Rejeitado; Screen faz a ligação. |
| Callbacks extras entre Controller e Screen | Não consolidados; a proposta final discutida foi as Screens observarem os eventos. |
| Controllers como Observers versus Screens | Núcleo de publicação definido; formalização concreta em [P-04](decisoes-e-pendencias.md#p-04), sem impor simultaneamente as duas estruturas. |
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

<a id="dec-01"></a>

## DEC-01 — Retirada de Command para simplificação do projeto

**Estado: decisão confirmada e encerrada.**

**Definição substituída:** o projeto havia adotado uma camada do padrão GoF Command, com contrato `Command.execute()` e classes por operação, como `MoveFileCommand`, `CopyFileCommand`, `RenameFileCommand` e `DeleteFileCommand`. Essa estrutura pertence ao histórico e não à arquitetura vigente.

**Decisão final:** retirar essa camada e executar as operações por chamadas dos Controllers aos Managers/Services responsáveis, conforme [ARQ-03](arquitetura-e-padroes.md#arq-03). O `LocalFileManager` coordena o trabalho relacionado aos registros; o `NativeFileService` manipula os arquivos físicos; os DAOs cuidam da persistência. Os Controllers continuam responsáveis pela publicação de eventos por `ExplorerEventService`. Não criar um novo Manager por antigo comando nem apenas renomear as classes retiradas, pois isso não atende à simplificação escolhida.

**Motivo:** a equipe de estudantes preferiu reduzir a quantidade de classes e a complexidade da implementação, considerando o prazo disponível, mesmo deixando de utilizar esse padrão GoF. Não acrescentar outro padrão ou outra camada para compensar a retirada.

**Alcance:** a decisão elimina a camada Command, não as funcionalidades de mover, copiar, recortar, colar, renomear ou excluir. Preserva as regras de identidade, Tags, seleção, filtros e falhas, com as pendências funcionais já registradas. Observer, as responsabilidades GRASP, os DAOs, CRUD e a Factory permanecem conforme suas definições atuais. A ausência de Undo/Redo continua sendo uma restrição do projeto; não usar essa possibilidade para sugerir a volta de Command.

**Relação com a arquitetura vigente:** [DEC-01](decisoes-e-pendencias.md#dec-01) está encerrada e orienta [ARQ-03](arquitetura-e-padroes.md#arq-03) e as responsabilidades de [ARQ-05](arquitetura-e-padroes.md#arq-05) a [ARQ-08](arquitetura-e-padroes.md#arq-08). Referências a Command e às suas classes neste registro têm função exclusivamente histórica. Não apresentá-lo como padrão atual, alternativa ainda em avaliação, item pendente de confirmação, tarefa de implementação, comentário de reintrodução ou melhoria futura. Esta decisão não depende da resolução de [P-01](decisoes-e-pendencias.md#p-01) a [P-13](decisoes-e-pendencias.md#p-13) e não exige nova aprovação.

**Materiais antigos de apoio, se existirem:** uma referência a Command em esboço, documentação, exemplo ou diagrama é incompatível com [DEC-01](decisoes-e-pendencias.md#dec-01) e não reabre a escolha. Não exigir a procura de classes antigas nem presumir que tenham sido implementadas. Preserve essa retirada na documentação de referência; a missão não autoriza criar ou corrigir código.

**Correção de nomenclatura:** `NativeFileCommand` foi um erro de digitação esclarecido pelo solicitante. Nunca foi uma classe aprovada do projeto; não registrá-la como participante antigo, atual ou alternativa descartada, nem deduzir desse termo a criação de `NativeFileManager`.

## Perguntas que afetam diretamente os fluxos

A produção ou exportação do prompt não respondeu às três primeiras perguntas. As decisões confirmadas descritas em cada registro continuam válidas; somente os efeitos indicados permanecem abertos.

<a id="p-01"></a>

## P-01 — Identidade na cópia/substituição e cópia sem Tags

**Conflito:** o efeito físico da sobrescrita é claro, mas os exemplos de identidade passaram a utilizar A, B e C com papéis diferentes. Foram expressas as orientações de prevalecer o `LocalFile`/UUID/Tags do arquivo copiado e de apenas sobrescrever o caminho de `LocalFileB`. Uma resposta posterior do assistente alterou o registro da origem como se a cópia fosse uma movimentação; essa resposta não é uma resolução confiável da decisão.

**Parte confirmada:** copiar mantém a origem física, há pergunta sobre herdar Tags e “Substituir” sobrescreve o arquivo do destino.

**Pergunta de decisão:** considerando origem O registrada em `/Documentos/prova.pdf` e destino D registrado em `/Backup/prova.pdf`, qual UUID fica associado a cada caminho após copiar com substituição e qual registro é excluído? Se o “LocalFile do arquivo copiado” for um registro da cópia já criado, ele apenas assume o caminho final, sem gerar outro UUID?

**Segunda pergunta:** quando não herda Tags, a cópia é apenas NativeFile ou recebe LocalFile temporário em `Etiqueta Ausente`?

**Dependências:** [OP-04](requisitos-e-regras.md#op-04), [OP-06](requisitos-e-regras.md#op-06), [UC-08](casos-de-uso.md#uc-08), modelo de identidade, coordenação da operação de cópia pelos Managers/Services, clipboard, persistência e critérios de aceite da cópia. O diagrama não deverá escolher uma dessas alternativas como resultado aprovado.

<a id="p-02"></a>

## P-02 — Modalidade 2 de exclusão versus adiamento para a inicialização

**Conflito:** a modalidade de retirar todas as etiquetas dos arquivos da Tag foi definida como remoção dos LocalFile e associações. Posteriormente foi definido, no contexto de reorganização, que os registros sem Tags normais permanecem durante a sessão em `Etiqueta Ausente`.

**Pergunta de decisão:** o adiamento vale somente para retirar a última Tag no fluxo comum, enquanto a modalidade 2 continua excluindo os registros imediatamente?

**Segunda pergunta:** nessa modalidade 2, a Tag selecionada também é excluída ou permanece vazia?

**Parte confirmada:** não apagar arquivos físicos na modalidade 2 e não apagar globalmente as outras Tags. Modalidade 3 inclui exclusão física e dos registros; a ordem/recuperação não é garantida.

**Dependências:** [CIC-01](requisitos-e-regras.md#cic-01), [DEL-02](requisitos-e-regras.md#del-02), remoção explícita do Tag-File em [OP-02](requisitos-e-regras.md#op-02), [UC-11](casos-de-uso.md#uc-11)/[UC-12](casos-de-uso.md#uc-12), cleanup, diagramas de ciclo de vida e aceite. Não combinar remoção imediata e sentinela para o mesmo resultado como se fossem equivalentes.

<a id="p-03"></a>

## P-03 — Efeito de “Não perguntar novamente nesta sessão”

**Lacuna:** o prazo da preferência está definido, mas não seu efeito automático nos próximos arquivos.

**Pergunta de decisão:** a opção apenas silencia futuras sugestões sem associar automaticamente Tags predefinidas, ou repete para os próximos arquivos a escolha feita no diálogo?

**Parte confirmada:** ao reiniciar, a pergunta volta; não há preferência permanente e não há classificação automática obrigatória aprovada para toda mudança de extensão.

**Dependências:** [TAG-04](requisitos-e-regras.md#tag-04), criação de LocalFile, [UC-04](casos-de-uso.md#uc-04)/[UC-05](casos-de-uso.md#uc-05) e testes de sugestão. Facilidade de programação não escolhe a política pendente.

## Demais decisões ainda necessárias

| ID | Assunto | Tratamento documental |
|---|---|---|
| <a id="p-04"></a>[P-04](decisoes-e-pendencias.md#p-04) | Relação Panels/Screens e implementação de ExplorerListener pelas Screens | Preservar `TagExplorerPanel` e `LocalFileExplorerPanel`; explicar a proposta final de Screens observadoras; não impor renomeação nem duplicação. |
| <a id="p-05"></a>[P-05](decisoes-e-pendencias.md#p-05) | Retorno/assinaturas da consulta de arquivos por Tags e eventos para NativeFile sem cadastro | Diferenciar consulta de Tag de consulta de arquivo; não criar LocalFile artificial para notificar; registrar contratos abertos. |
| <a id="p-06"></a>[P-06](decisoes-e-pendencias.md#p-06) | DDL físico, tipo UUID SQL, nulabilidade, limites, índices, collation, nome singular/plural de extensões e cascatas | Documentar regras lógicas confirmadas e explicitar as escolhas físicas ainda abertas para a futura implementação; exemplos SQL são ilustrativos, não um DDL homologado. |
| <a id="p-07"></a>[P-07](decisoes-e-pendencias.md#p-07) | Identificação técnica e editabilidade de Etiqueta Ausente e preparação das predefinidas | Não identificar a sentinela apenas por nome sem explicar o problema de duplicatas; não inventar coluna/enum/UUID fixo como aprovado. |
| <a id="p-08"></a>[P-08](decisoes-e-pendencias.md#p-08) | Nomes de conflito, caminhos equivalentes, links, arquivos sem extensão, extensões compostas e colisão ao relocalizar | Manter a política em aberto; exemplos simples não são algoritmos gerais. |
| <a id="p-09"></a>[P-09](decisoes-e-pendencias.md#p-09) | Comparação de nomes repetidos, validação de vazio, cor/defaults e formato hexadecimal | Separar sugestões de decisões e indicar quais validações ainda precisam ser escolhidas; não preencher a lacuna por suposição. |
| <a id="p-10"></a>[P-10](decisoes-e-pendencias.md#p-10) | Ausência de metadados, conversão temporal e atualização de lastFileTaggedAt | Não inventar datas de substituição, triggers ou efeitos para ações repetidas. |
| <a id="p-11"></a>[P-11](decisoes-e-pendencias.md#p-11) | Versões de servidor/driver, instaladores, verificação/reaplicação de schema e encerramento | Registrar o ambiente de referência, as responsabilidades previstas e as escolhas ainda necessárias; não exigir artefatos prontos nem instalar ou executar para decidir. |
| <a id="p-12"></a>[P-12](decisoes-e-pendencias.md#p-12) | Seleção múltipla geral, comportamento de lotes com falhas, clipboard após colagem, prevenção interna de reentrância e arranjo lado a lado | Documentar o fluxo confirmado; não acrescentar mecanismo, fila ou comportamento automático. |
| <a id="p-13"></a>[P-13](decisoes-e-pendencias.md#p-13) | Consulta sem Tags selecionadas, estados vazios da UI e ordenação padrão | Deixar visível a ausência de definição. |

Esses pontos não impedem produzir documentos úteis e completos quanto à cobertura. Eles impedem apenas afirmar que todas as decisões de implementação estão fechadas.

## Propostas que não devem virar requisitos por inferência

| Tema | Estado e efeito |
|---|---|
| Nome de Tag | Ignorar caixa e espaços nas extremidades foi proposta. Comparação, vazio e formato de cor permanecem em [P-09](decisoes-e-pendencias.md#p-09). |
| Extensões | Coleção vazia foi proposta de API; a semântica de ausência de restrição está aprovada. Os catálogos iniciais são exemplos, não listas finais. |
| Interface | SwingWorker, classes visuais como TagBadge e o componente de Drop não são obrigatoriedades adicionais. [P-04](decisoes-e-pendencias.md#p-04) trata a composição Screens/Panels. |
| Consultas | Filtros auxiliares de nome, disponibilidade ou etiquetados foram sugestões. Ordenação/desempate permanece parcialmente futuro em [EXP-05](interface-e-fluxos.md#exp-05)/[P-13](decisoes-e-pendencias.md#p-13). |
| Persistência | CHAR(36), BINARY(16), BIGINT, PK composta específica, cascatas, tamanhos e índices não formam um DDL homologado. [P-06](decisoes-e-pendencias.md#p-06) mantém as decisões físicas abertas. |
| Ambiente | pkexec, detecção por PATH, comandos de instalação, versões e reaplicação completa de schema não foram fechados. [P-11](decisoes-e-pendencias.md#p-11) concentra essas escolhas. |
| Padrões extras | Creator, Strategy, Singleton e fábricas GoF não se tornam obrigações pela menção em explicações. [ARQ-09](arquitetura-e-padroes.md#arq-09) determina a classificação pertinente. |

## Efeito das pendências na construção

| Pendência | Partes a revisar conjuntamente quando houver resposta |
|---|---|
| [P-01](decisoes-e-pendencias.md#p-01) | [OP-04](requisitos-e-regras.md#op-04)/[OP-06](requisitos-e-regras.md#op-06), [UC-08](casos-de-uso.md#uc-08), [DOM-02](modelo-de-dominio.md#dom-02), coordenação de cópia em [ARQ-03](arquitetura-e-padroes.md#arq-03)/[ARQ-07](arquitetura-e-padroes.md#arq-07), clipboard, persistência e [ACE-20](criterios-de-aceite.md#ace-20). Origem física permanece e herança de Tags é perguntada. |
| [P-02](decisoes-e-pendencias.md#p-02) | [CIC-01](requisitos-e-regras.md#cic-01), [DEL-02](requisitos-e-regras.md#del-02), [OP-02](requisitos-e-regras.md#op-02), [UC-11](casos-de-uso.md#uc-11)/[UC-12](casos-de-uso.md#uc-12), ciclo de vida, limpeza e [ACE-19](criterios-de-aceite.md#ace-19). A modalidade 2 preserva o disco e não apaga globalmente outras Tags. |
| [P-03](decisoes-e-pendencias.md#p-03) | [TAG-04](requisitos-e-regras.md#tag-04), criação de LocalFile em [UC-04](casos-de-uso.md#uc-04)/[UC-05](casos-de-uso.md#uc-05) e sugestão da interface. O prazo de supressão é somente a sessão; não há preferência permanente. |
| [P-04](decisoes-e-pendencias.md#p-04)/[P-05](decisoes-e-pendencias.md#p-05) | [ARQ-05](arquitetura-e-padroes.md#arq-05)/[ARQ-06](arquitetura-e-padroes.md#arq-06), Screens, eventos e consultas. Controllers publicam pelo serviço e consultas de Tags continuam retornando Tags. |
| [P-06](decisoes-e-pendencias.md#p-06)/[P-07](decisoes-e-pendencias.md#p-07) | DOM, SQL, predefinidas e [CIC-02](requisitos-e-regras.md#cic-02). Identidade, unicidade e proteção estão definidas; mecanismos físicos e identificação especial não estão. |
| [P-08](decisoes-e-pendencias.md#p-08)/[P-09](decisoes-e-pendencias.md#p-09)/[P-10](decisoes-e-pendencias.md#p-10) | Caminhos, nomes, cores, extensões especiais, conversões e metadados. Regras simples confirmadas não homologam algoritmos gerais. |
| [P-11](decisoes-e-pendencias.md#p-11) | [AMB-01](instalacao-e-execucao.md#amb-01) a [AMB-07](instalacao-e-execucao.md#amb-07) e [SQL-06](banco-de-dados.md#sql-06). Estrutura relativa, porta e responsabilidades estão definidas; instaladores, versões e recuperação precisam de decisão. |
| [P-12](decisoes-e-pendencias.md#p-12)/[P-13](decisoes-e-pendencias.md#p-13) | Clipboard, lotes, Loading, lado a lado, estados vazios e ordenação. Os fluxos principais continuam documentáveis sem escolher esses detalhes. |

## Limitações conscientes

A lista integral está em [visão geral](visao-geral.md#restricoes). Sem transações explícitas não há garantia de atomicidade entre tabelas nem entre disco e SQL. Reconexão não assegura repetição, reversão ou conclusão; Loading bloqueia interações conflitantes, mas não define serialização interna. A conta administrativa e a senha pública são referências didáticas da instância exclusiva local, não configuração genérica de produção.

As melhorias futuras permitidas devem ser apenas comentadas nos pontos pertinentes: eventos estruturados, clipboard do sistema, estudo de transações e Undo/Redo. Essa última possibilidade não altera [DEC-01](decisoes-e-pendencias.md#dec-01). A documentação não acrescenta ferramentas, estruturas operacionais ou padrões compensatórios.
