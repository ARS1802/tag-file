# Decisões e pendências

[Índice](README.md) · [Arquitetura](arquitetura-e-padroes.md) · [Ordem de construção](arquitetura-e-padroes.md#orientacao-construcao)

Esta página reúne as decisões encerradas DEC-01 e DEC-02 e as escolhas ainda abertas em P-01 a P-13. As regras definidas continuam válidas enquanto a equipe resolve os pontos pendentes.

## Como usar esta página

Os grupos P-01 a P-13 continuam abertos. Eles misturam assuntos com impactos diferentes; use a coluna de momento para saber quando consultá-los, sem precisar resolver tudo antes de estudar o projeto.

| Tipo de decisão | Grupos principais | Momento de tratar |
|---|---|---|
| Resultado de uma ação do usuário | P-01, P-02, P-03, P-13 | Antes de concluir a funcionalidade afetada e seu critério de aceite. |
| Contrato entre componentes | P-04, P-05 e partes de P-12 | Antes de integrar UI, Controllers, consultas e avisos. |
| Dados e regras de validação | P-06 a P-10 | Antes de fixar schema e validações que dependem dessas escolhas. |
| Preparação e execução | P-11 e partes de P-12 | Antes de integrar os scripts e o mecanismo de execução de uma ação por vez. |

Uma pendência pode afetar mais de uma área. Nomear uma tabela e decidir como reconhecer caminhos equivalentes são escolhas de pesos diferentes; os grupos permitem localizar as partes afetadas.

<a id="dec-01"></a>

## Decisão encerrada: operações por chamadas diretas

**DEC-01 está definida e encerrada.** A camada GoF Command foi retirada para simplificar a quantidade de classes e a implementação. Classes por ação, como MoveFileCommand, CopyFileCommand, RenameFileCommand e DeleteFileCommand, não fazem parte da arquitetura atual.

Os Controllers chamam diretamente os Managers/Services responsáveis. LocalFileManager coordena registros, NativeFileService manipula arquivos físicos, DAOs persistem dados e Controllers publicam alterações por ExplorerEventService.

As funcionalidades de mover, copiar, recortar, colar, renomear e excluir permanecem. A retirada não será compensada com um Manager por antigo comando nem com outro padrão adicional. A possibilidade de comentar Undo/Redo futuro não reintroduz Command.

<a id="dec-02"></a>

## Decisão encerrada: uma ação por vez

**DEC-02 está definida e encerrada.** A aplicação executa apenas uma ação por vez, com limite compartilhado pelos dois exploradores. Novas solicitações durante uma ação são bloqueadas, inclusive as automáticas e reentrantes. Não há fila de ações nem sequências de operações armazenadas para execução posterior.

Uma ação pode conter várias etapas e a seleção múltipla já prevista na associação inicial. A UI permanece responsiva e permite os diálogos da ação atual. Ao encerrar por sucesso, falha ou cancelamento, a aplicação libera a execução de uma nova ação, sem disparar automaticamente solicitações bloqueadas.

O comportamento está em [OP-09](requisitos-e-regras.md#op-09) e será verificado por [ACE-25](criterios-de-aceite.md#ace-25). O mecanismo técnico permanece em P-12; a exclusividade global já é requisito. Essa escolha mantém os objetivos atuais, sem reintroduzir Command ou Undo/Redo nesta versão.

<a id="p-01"></a>

## P-01 — Identidade na cópia e substituição

**Definido:** copiar mantém a origem física, cria uma cópia e pergunta se ela deve herdar Tags. Substituir sobrescreve o arquivo do destino.

**A decidir:** ao copiar de um caminho O cadastrado para um caminho D também cadastrado, quais UUIDs e associações ficam em O e D? Qual cadastro, se houver, é removido? Se a cópia já tiver cadastro, como ele assume o destino? Também falta decidir se uma cópia sem Tags fica apenas nativa ou recebe LocalFile temporário em Etiqueta Ausente.

**Impacto:** cópia, conflitos, clipboard, identidade, SQL e [ACE-20](criterios-de-aceite.md#ace-20). A sequência completa de confirmações e execução precisa respeitar essas decisões; o exemplo de cópia não resolve a ordem por si só.

<a id="p-02"></a>

## P-02 — Remoção explícita e reorganização temporária

**Definido:** retirar a última Tag normal durante a reorganização mantém o cadastro em Etiqueta Ausente até a limpeza da próxima inicialização. A modalidade 2 de exclusão preserva arquivos físicos e não apaga globalmente outras Tags.

**A decidir:** a modalidade 2 continua removendo imediatamente os LocalFile e vínculos, como originalmente definida, ou também segue o adiamento? A Tag selecionada é excluída ou permanece vazia? A remoção explícita do Tag-File, oferecida para um arquivo indisponível, deve ser coerente com essa escolha.

**Impacto:** [CIC-01](requisitos-e-regras.md#cic-01), [DEL-02](requisitos-e-regras.md#del-02), [UC-11](casos-de-uso.md#uc-11), [UC-12](casos-de-uso.md#uc-12) e [ACE-19](criterios-de-aceite.md#ace-19). Não são equivalentes manter temporariamente o cadastro e removê-lo imediatamente.

<a id="p-03"></a>

## P-03 — Silenciar sugestões na sessão

**Definido:** um novo LocalFile oferece predefinida compatível. A opção Não perguntar novamente nesta sessão vale até reiniciar; não é preferência permanente.

**A decidir:** silenciar apenas impede novas sugestões sem classificação automática, ou repete para os próximos arquivos a escolha feita no diálogo? Qual o critério quando várias predefinidas são compatíveis?

**Impacto:** criação de LocalFile, [TAG-04](requisitos-e-regras.md#tag-04), [UC-04](casos-de-uso.md#uc-04) e [UC-05](casos-de-uso.md#uc-05). Não há classificação automática geral definida.

<a id="p-04"></a>

## P-04 — Screens, Panels e observadores

**Definido:** TagExplorerPanel e LocalFileExplorerPanel são nomes das partes visuais. Screens montam a UI, registram os listeners de seus componentes e encaminham ações aos Controllers. O serviço de eventos notifica por ExplorerListener.

**A decidir:** como os Panels se relacionam com as Screens e como formalizar a proposta de Screens implementarem ExplorerListener. A integração precisa combinar o ciclo de inscrição dos objetos visuais.

**Impacto:** composição Swing e [Observer](arquitetura-e-padroes.md#arq-06). A explicação com Screens observadoras é didática e acompanha a proposta existente; não exige duplicar classes visuais nem impõe simultaneamente Controllers e Screens como observadores.

<a id="p-05"></a>

## P-05 — Consultas de arquivos e dados dos avisos

**Definido:** TagDAO retorna Tags conforme CrudDAO<Tag, TagFilter>. Pesquisar arquivos por Tags tem como resultado arquivos. Avisos precisam atender alterações de NativeFile sem cadastro; não se cria LocalFile artificial para notificar.

**A decidir:** assinaturas e retornos da consulta de arquivos por Tags; assinatura e normalização da consulta em lote, cujo resultado Map<Path, LocalFile> já está definido; distribuição dos critérios; dados e assinaturas dos eventos para arquivos cadastrados e não cadastrados.

**Impacto:** filtros, DAOs, Controllers, Map da visão local e ExplorerListener. Um exemplo com parâmetro LocalFile, NativeFile ou sem parâmetros não fecha o contrato final.

<a id="p-06"></a>

## P-06 — Modelo físico do banco

**Definido:** entidades, relações, identidade por UUID, caminho único e alterável, extensões únicas por Tag e tipos temporais LocalDateTime/DATETIME.

**A decidir:** DDL, representação SQL do UUID, tipos e limites de campos, nulabilidade, índices, collation, normalização do caminho na persistência, armazenamento separado de nome/extensão, nome singular/plural da tabela de extensões e mecanismos de cascata.

**Impacto:** [banco de dados](banco-de-dados.md), DAOs e objetos reconstruídos. CHAR(36), BINARY(16), BIGINT e chaves compostas concretas são propostas de implementação, não schema final.

<a id="p-07"></a>

## P-07 — Sentinela e predefinidas

**Definido:** Etiqueta Ausente é protegida; nomes de Tags podem repetir. Predefinidas comuns podem ser removidas com confirmação reforçada e não são recriadas automaticamente a cada abertura.

**A decidir:** como identificar tecnicamente a sentinela, quais campos especiais podem ser editados e como preparar/reconhecer predefinidas. UUID fixo, coluna de sistema ou enum não foram escolhidos.

**Impacto:** carga inicial, proteção, ciclo de vida e limpeza. Usar apenas o nome não distingue a Tag de sistema de uma Tag de mesmo nome.

<a id="p-08"></a>

## P-08 — Caminhos, conflitos e extensões especiais

**Definido:** caminho cadastrado é único; mover normalmente preserva identidade; conflitos oferecem as alternativas aplicáveis; extensões são normalizadas em minúsculas com ponto.

**A decidir:** equivalência e normalização de caminhos entre plataformas, links simbólicos, colisão ao relocalizar, conflito apenas no SQL, nomes para manter ambos e alternativas disponíveis por operação. Também falta tratar arquivos sem extensão e extensões compostas.

**Impacto:** navegação, Map, unicidade SQL, associação, movimentação, cópia e renomeação. prova (1).pdf é apenas exemplo de nome.

<a id="p-09"></a>

## P-09 — Nomes e cores

**Definido:** nomes de Tag podem repetir mediante confirmação; cor é escolhida pelo usuário e representada em hexadecimal.

**A decidir:** comparação de nomes com caixa/espaços, aceitação de vazio, comprimentos, valor padrão e obrigatoriedade da escolha da cor, além do formato hexadecimal completo.

**Impacto:** formulários, validações, Factory/construtores e schema. Ignorar caixa e espaços nas extremidades continua uma proposta.

<a id="p-10"></a>

## P-10 — Metadados e datas

**Definido:** LocalFile representa tamanho, disponibilidade e datas físicas; Tag tem criação e lastFileTaggedAt. Os tipos temporais definidos são LocalDateTime/DATETIME.

**A decidir:** datas não disponíveis, precisão, conversão temporal/fuso, UPDATE sem alteração e efeitos em lastFileTaggedAt para associação repetida, sentinela, cópia de Tags e exclusões.

**Impacto:** leitura do disco, sincronização, DAOs e filtros. Não há datas substitutas ou gatilhos de atualização definidos para esses casos.

<a id="p-11"></a>

## P-11 — Ambiente, schema e encerramento

**Definido:** instância MySQL local administrada pelo aplicativo, parâmetros de referência, scripts PowerShell/Bash, ProcessBuilder, JDK 24 e Connector/J manual. Sem schema_history.

**A decidir:** versões de servidor/driver, instaladores, detecção de componentes e instância, autenticação administrativa no Linux, tempos de espera, verificação/reaplicação de schema, instalação ou schema incompletos e encerramento detalhado. A API de abertura do arquivo na aplicação associada também não foi escolhida.

**Impacto:** [instalação e execução](instalacao-e-execucao.md), DatabaseManager, DatabaseConnection, scripts e verificações entre plataformas. Não há comandos definitivos nem versões preenchidas por convenção.

<a id="p-12"></a>

## P-12 — Lotes, clipboard e execução da UI

**Definido:** seleção múltipla no fluxo inicial de associação, clipboard interno COPY/CUT compartilhado, intenção de visões lado a lado e apenas uma ação em andamento em toda a aplicação, conforme [DEC-02](#dec-02)/[OP-09](requisitos-e-regras.md#op-09). Loading acompanha a ação; novas ações são bloqueadas nos dois exploradores, inclusive chamadas automáticas e reentrantes, sem fila ou sequências armazenadas.

**A decidir:** seleção múltipla geral, ordem/continuidade de lotes com falha, representação do clipboard e seu estado após colagem, repetição de CUT, histórico/persistência do clipboard e arranjo lado a lado. Também falta escolher o mecanismo compartilhado de controle da ação em andamento e de execução em segundo plano; SwingWorker permanece alternativa de implementação. Histórico do clipboard não autoriza uma fila de ações.

**Impacto:** UI, Controllers, conexão compartilhada e operações físicas. O mecanismo deve garantir OP-09 para todas as entradas; bloquear apenas cliques não impede outras solicitações. Manter a UI responsiva não autoriza executar outra ação em paralelo.

<a id="p-13"></a>

## P-13 — Busca vazia e ordenação

**Definido:** AND exige todas as Tags, OR exige alguma, sem NOT e sem duplicar identidade nos resultados.

**A decidir:** resultado sem Tags selecionadas, estados vazios da UI, ordenação padrão, direção, precedência e desempates.

**Impacto:** pesquisa por Tags, apresentação e aceite. Nome, disponibilidade e filtros de etiquetados continuam sugestões; não são adicionados como funcionalidades obrigatórias.

## Limites que não são pendências

A lista completa está na [visão geral](visao-geral.md#restricoes). Sem transações explícitas, não há garantia de atomicidade entre tabelas ou entre disco e SQL. Reconexão não garante repetição, reversão ou conclusão. As credenciais didáticas e o acesso administrativo pertencem à instância local exclusiva do projeto.

Possíveis evoluções de eventos estruturados, clipboard do sistema, transações e Undo/Redo serão apenas comentadas nos pontos pertinentes.
