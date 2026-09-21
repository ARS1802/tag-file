# Decisões e pendências

[Índice](README.md) · [Arquitetura](arquitetura-e-padroes.md) · [Ordem de construção](arquitetura-e-padroes.md#orientacao-construcao)

Esta página reúne DEC-01/DEC-02 e os contratos P-01 a P-13, resolvidos para a implementação atual. Veja o [registro de decisões confirmadas e técnicas](decisoes-implementacao.md) e a [verificação executada](verificacao.md).

## Como usar esta página

Os grupos P-01 a P-13 organizam decisões com impactos diferentes. As escolhas desta versão estão registradas abaixo; a tabela indica quando revisar cada contrato em uma evolução futura.

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

O comportamento está em [OP-09](requisitos-e-regras.md#op-09) e foi verificado nos cenários de [ACE-25](verificacao.md). O mecanismo técnico está registrado em P-12; a exclusividade global é requisito. Essa escolha mantém os objetivos atuais, sem reintroduzir Command ou Undo/Redo nesta versão.

<a id="p-01"></a>

## P-01 — Identidade na cópia e substituição

**Definido:** copiar mantém a origem física, cria uma cópia e pergunta se ela deve herdar Tags. Substituir sobrescreve o arquivo do destino.

**Decisão atual:** Confirmado pelo usuário: cópia cadastrada recebe UUID novo; cadastro anterior do destino é removido; origem permanece; herança de Tags só após confirmação. Sem herança, a cópia fica apenas nativa. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** cópia, conflitos, clipboard, identidade, SQL e [ACE-20](criterios-de-aceite.md#ace-20). A sequência completa de confirmações e execução precisa respeitar essas decisões; o exemplo de cópia não resolve a ordem por si só.

<a id="p-02"></a>

## P-02 — Remoção explícita e reorganização temporária

**Definido:** retirar a última Tag normal durante a reorganização mantém o cadastro em Etiqueta Ausente até a limpeza da próxima inicialização. A modalidade 2 de exclusão preserva arquivos físicos e não apaga globalmente outras Tags.

**Decisão atual:** Confirmado pelo usuário: modalidade 2 remove imediatamente cadastros/vínculos e a Tag selecionada. Remover do Tag-File também é imediato. Retirada comum da última Tag continua temporariamente na sentinela. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** [CIC-01](requisitos-e-regras.md#cic-01), [DEL-02](requisitos-e-regras.md#del-02), [UC-11](casos-de-uso.md#uc-11), [UC-12](casos-de-uso.md#uc-12) e [ACE-19](criterios-de-aceite.md#ace-19). Não são equivalentes manter temporariamente o cadastro e removê-lo imediatamente.

<a id="p-03"></a>

## P-03 — Silenciar sugestões na sessão

**Definido:** um novo LocalFile oferece predefinida compatível. A opção Não perguntar novamente nesta sessão vale até reiniciar; não é preferência permanente.

**Decisão atual:** Confirmado pelo usuário: silenciar não repete classificação. Todas as predefinidas compatíveis são oferecidas para escolher uma; supressão termina na próxima sessão. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** criação de LocalFile, [TAG-04](requisitos-e-regras.md#tag-04), [UC-04](casos-de-uso.md#uc-04) e [UC-05](casos-de-uso.md#uc-05). Não há classificação automática geral definida.

<a id="p-04"></a>

## P-04 — Screens, Panels e observadores

**Definido:** TagExplorerPanel e LocalFileExplorerPanel são nomes das partes visuais. Screens montam a UI, registram os listeners de seus componentes e encaminham ações aos Controllers. O serviço de eventos notifica por ExplorerListener.

**Decisão atual:** Panels exercem o papel de Screens, implementam ExplorerListener, inscrevem-se na construção na EDT e removem a inscrição ao descartar a janela. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** composição Swing e [Observer](arquitetura-e-padroes.md#arq-06). A explicação com Screens observadoras é didática e acompanha a proposta existente; não exige duplicar classes visuais nem impõe simultaneamente Controllers e Screens como observadores.

<a id="p-05"></a>

## P-05 — Consultas de arquivos e dados dos avisos

**Definido:** TagDAO retorna Tags conforme CrudDAO<Tag, TagFilter>. Pesquisar arquivos por Tags tem como resultado arquivos. Avisos precisam atender alterações de NativeFile sem cadastro; não se cria LocalFile artificial para notificar.

**Decisão atual:** LocalFileDAO pesquisa arquivos por AND/OR e fornece findByPaths em lote; TagDAO retorna Tags. Eventos entregam fotografias imutáveis já consultadas, incluindo nativos sem cadastro. Ausência é Optional vazio; SQL propaga SQLException. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** filtros, DAOs, Controllers, Map da visão local e ExplorerListener. Um exemplo com parâmetro LocalFile, NativeFile ou sem parâmetros não fecha o contrato final.

<a id="p-06"></a>

## P-06 — Modelo físico do banco

**Definido:** entidades, relações, identidade por UUID, caminho único e alterável, extensões únicas por Tag e tipos temporais LocalDateTime/DATETIME.

**Decisão atual:** CHAR(36) para UUID, VARCHAR(700) binário/único para caminho, BIGINT anulável para bytes e DATETIME(6) anulável para datas físicas. Relações têm chaves compostas e cascata de vínculos. DDL em database/schema. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** [banco de dados](banco-de-dados.md), DAOs e objetos reconstruídos. A escolha final de representação está no SQL executável em `database/schema`.

<a id="p-07"></a>

## P-07 — Sentinela e predefinidas

**Definido:** Etiqueta Ausente é protegida; nomes de Tags podem repetir. Predefinidas comuns podem ser removidas com confirmação reforçada e não são recriadas automaticamente a cada abertura.

**Decisão atual:** Sentinela reconhecida por UUID fixo, protegida contra edição/exclusão. Predefinidas têm identidade estável e marca explícita; carga inicial concluída fica registrada em APP_METADATA, sem recriar etiquetas apagadas. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** carga inicial, proteção, ciclo de vida e limpeza. Usar apenas o nome não distingue a Tag de sistema de uma Tag de mesmo nome.

<a id="p-08"></a>

## P-08 — Caminhos, conflitos e extensões especiais

**Definido:** caminho cadastrado é único; mover normalmente preserva identidade; conflitos oferecem as alternativas aplicáveis; extensões são normalizadas em minúsculas com ponto.

**Decisão atual:** Caminhos absolutos com normalização lexical; sem fusão automática de aliases/conteúdo. Links simbólicos de arquivo são recusados. Relocalização não mescla cadastros. Conflitos consultam disco e SQL; manter ambos usa sufixo numérico. Casos de extensão e limites de caixa estão detalhados no registro. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** navegação, Map, unicidade SQL, associação, movimentação, cópia e renomeação. prova (1).pdf é apenas exemplo de nome.

<a id="p-09"></a>

## P-09 — Nomes e cores

**Definido:** nomes de Tag podem repetir mediante confirmação; cor é escolhida pelo usuário e representada em hexadecimal.

**Decisão atual:** Confirmado pelo usuário: nome aparado de 1 a 100 caracteres, comparação sem caixa/espaços externos e cor #RRGGBB. Nomes repetidos são permitidos com confirmação; a identidade continua sendo UUID. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** formulários, validações, Factory/construtores e schema. A comparação confirmada detecta repetição para pedir confirmação, sem impedir nomes iguais.

<a id="p-10"></a>

## P-10 — Metadados e datas

**Definido:** LocalFile representa tamanho, disponibilidade e datas físicas; Tag tem criação e lastFileTaggedAt. Os tipos temporais definidos são LocalDateTime/DATETIME.

**Decisão atual:** UTC com precisão de microssegundos. Dados físicos desconhecidos são nulos; criação Unix não usa substituto inventado. Indisponibilidade preserva últimos metadados; UPDATE físico só quando muda. Associação nova atualiza lastFileTaggedAt; repetição e retirada não. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** leitura do disco, sincronização, DAOs e filtros. Datas físicas desconhecidas continuam nulas; o Manager e os DAOs aplicam os momentos de atualização definidos acima.

<a id="p-11"></a>

## P-11 — Ambiente, schema e encerramento

**Definido:** instância MySQL local administrada pelo aplicativo, parâmetros de referência, scripts PowerShell/Bash, ProcessBuilder, JDK superior à versão 21 e Connector/J versionado em `lib`. Sem schema_history.

**Decisão atual:** MySQL portátil 8.4.9, Connector/J 9.7.0 e JDK superior à versão 21. Scripts reais em Bash/PowerShell, espera limitada, identificação da instância por dados/porta/marcador/PID, schema não destrutivo e Desktop.OPEN. Windows implementado, não executado neste host. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** [instalação e execução](instalacao-e-execucao.md), DatabaseManager, DatabaseConnection, scripts e verificações entre plataformas. Versões e comandos reproduzíveis estão no [guia da implementação](implementacao.md).

<a id="p-12"></a>

## P-12 — Lotes, clipboard e execução da UI

**Definido:** seleção múltipla no fluxo inicial de associação, clipboard interno COPY/CUT compartilhado, intenção de visões lado a lado e apenas uma ação em andamento em toda a aplicação, conforme [DEC-02](#dec-02)/[OP-09](requisitos-e-regras.md#op-09). Loading acompanha a ação; novas ações são bloqueadas nos dois exploradores, inclusive chamadas automáticas e reentrantes, sem fila ou sequências armazenadas.

**Decisão atual:** Confirmado pelo usuário: clipboard de um arquivo, COPY permanece e CUT limpa após sucesso; associação em lote para na primeira falha. ActionGate admite por comparação atômica antes da thread, sem fila, e mantém posse até terminar inclusive UI. Abas e opção lado a lado compartilham tudo. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** UI, Controllers, conexão compartilhada e operações físicas. O mecanismo deve garantir OP-09 para todas as entradas; bloquear apenas cliques não impede outras solicitações. Manter a UI responsiva não autoriza executar outra ação em paralelo.

<a id="p-13"></a>

## P-13 — Busca vazia e ordenação

**Definido:** AND exige todas as Tags, OR exige alguma, sem NOT e sem duplicar identidade nos resultados.

**Decisão atual:** Confirmado pelo usuário: nenhuma Tag selecionada mostra todos os cadastros; arquivos por nome/caminho e Tags por nome/UUID. Não foram acrescentados NOT, pesquisa textual obrigatória ou filtro de disponibilidade. [Detalhes e justificativas](decisoes-implementacao.md).

**Impacto:** pesquisa por Tags, apresentação e aceite. Nome, disponibilidade e filtros de etiquetados continuam sugestões; não são adicionados como funcionalidades obrigatórias.

## Limites que não são pendências

A lista completa está na [visão geral](visao-geral.md#restricoes). Sem transações explícitas, não há garantia de atomicidade entre tabelas ou entre disco e SQL. Reconexão não garante repetição, reversão ou conclusão. As credenciais didáticas e o acesso administrativo pertencem à instância local exclusiva do projeto.

Possíveis evoluções de eventos estruturados, clipboard do sistema, transações e Undo/Redo serão apenas comentadas nos pontos pertinentes.
