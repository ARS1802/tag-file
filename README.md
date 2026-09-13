# Documentação do Tag-File

O Tag-File é o projeto de um aplicativo desktop Java/Swing/MySQL para organizar arquivos locais por etiquetas. Esta documentação descreve a especificação consolidada e o estado observado no repositório em **13/09/2026**.

**A parte documental possível está concluída. O aplicativo descrito ainda não está implementado no repositório inspecionado:** há somente o programa inicial em [src/Main.java](src/Main.java) e as configurações da IDE. Nenhum cenário funcional foi executado. **P-01 a P-13 continuam abertas**, inclusive as decisões de identidade na cópia, modalidade 2 de exclusão e efeito de silenciar sugestões.

<a id="doc-01"></a>

## DOC-01 — Escopo e proveniência

A missão autorizou leitura do repositório e criação/atualização exclusiva de documentação nesta pasta `doc`. Não autorizou alterações na implementação, configurações operacionais, scripts, dependências ou dados; nem compilação, execução da aplicação, testes funcionais, instalação de ferramentas, SQL, serviços, commits ou publicação.

A fonte normativa foi lida integralmente: `/home/arthur/Projetos/tag-file/prompts/prompt-documentacao-tag-file.md`, com 1.866 linhas. A fonte é uma consolidação das decisões de modelagem, não uma transcrição integral da conversa nem prova de implementação ou de testes. Os IDs são referências de rastreabilidade, não números de mensagens. Não foi necessário material externo ou histórico inacessível para produzir os documentos.

A raiz real do projeto inspecionado é `/home/arthur/IdeaProjects/tag-file`; `/home/arthur/Projetos/tag-file` contém o prompt. Os documentos foram criados em `/home/arthur/IdeaProjects/tag-file/doc`. O conteúdo técnico é autossuficiente nesta pasta; o caminho externo é registrado apenas para proveniência.

## Legenda de estados

| Estado | Como ler |
|---|---|
| Confirmada | Decisão expressa consolidada; descreve o comportamento aprovado, não prova sua implementação. |
| Delegada | Escolha resolvida dentro de autorização expressa. Não permite presumir aprovação de outras alternativas. |
| Derivada | Consequência necessária de decisões confirmadas, identificada como tal. |
| Proposta | Exemplo ou alternativa sem aprovação inequívoca. |
| Pendente | Pergunta ou contrato que continua aberto. |
| Substituída | Decisão antiga, mantida somente no histórico. |
| Fora do escopo / não definido | Não constitui requisito atual; a documentação explica a restrição ou lacuna. |
| Observado | Informação comprovada por leitura de arquivo/símbolo, separada da aprovação. |
| Não encontrado / não verificável | Ausência no inventário inspecionado ou falta de evidência/definição suficiente. |
| Limitação conhecida | Escolha consciente da versão, como ausência de transações explícitas e pool. |

<a id="doc-02"></a>

## DOC-02 — Índice e ordem de leitura

Leia na ordem abaixo para conhecer o problema, os fluxos, os modelos, a arquitetura e os limites. As regras têm um único documento principal; as outras páginas apresentam exemplos, consequências e links. A matriz registra a correspondência com todos os temas do inventário original.

| Ordem | Documento | Finalidade | Identificadores principais |
|---|---|---|---|
| 1 | [visao-geral.md](doc/visao-geral.md) | Objetivo, escopo acadêmico, restrições e glossário | OBJ |
| 2 | [requisitos-e-regras.md](doc/requisitos-e-regras.md) | Tags, extensões, ciclo de vida, operações, exclusões, sincronização e falhas | TAG, EXT, CIC, OP, DEL, SYN, ERR |
| 3 | [casos-de-uso.md](doc/casos-de-uso.md) | UC-01 a UC-15: gatilhos, sequência, alternativas e efeitos | UC |
| 4 | [modelo-de-dominio.md](doc/modelo-de-dominio.md) | Identidade, composição, atributos e ciclo de vida | DOM |
| 5 | [banco-de-dados.md](doc/banco-de-dados.md) | Modelo relacional, dicionário lógico, DAOs e limites do DDL | SQL |
| 6 | [arquitetura-e-padroes.md](doc/arquitetura-e-padroes.md) | Responsabilidades, Command, Observer, Factory simples e GRASP | ARQ |
| 7 | [interface-e-fluxos.md](doc/interface-e-fluxos.md) | Exploradores, consultas, filtros, apresentação e diálogos | EXP, UI |
| 8 | [instalacao-e-execucao.md](doc/instalacao-e-execucao.md) | Ambiente previsto, configuração observada, scripts de referência e dependências | AMB |
| 9 | [criterios-de-aceite.md](doc/criterios-de-aceite.md) | ACE-01 a ACE-24, todos não executados | ACE |
| 10 | [decisoes-e-pendencias.md](doc/decisoes-e-pendencias.md) | Histórico, propostas, P-01 a P-13 e DIV-001 a DIV-004 | P, DIV |
| 11 | [rastreabilidade.md](doc/rastreabilidade.md) | Inventário integral de temas e comparação por identificador | Matriz |
| 12 | [relatorio-entrega.md](doc/relatorio-entrega.md) | Arquivos criados, inspeção, auditoria e limites da validação | Entrega |

Para estudar uma operação específica, comece pelos [casos de uso](doc/casos-de-uso.md) e siga seus links. Para avaliar o que já existe, consulte [rastreabilidade](doc/rastreabilidade.md#evidencias). Para decidir os contratos restantes, consulte [pendências](doc/decisoes-e-pendencias.md#p-01).

<a id="doc-03"></a>

## DOC-03 — Método e limites da inspeção

Foram verificadas as instruções aplicáveis, os dois caminhos informados, o estado inicial do Git e o inventário do repositório. Não foram encontrados arquivos `AGENTS.md` locais nos diretórios ancestrais verificados nem na árvore do projeto. As orientações fornecidas na conversa foram consideradas. A documentação foi criada sem substituir documentação anterior: a pasta `doc` não existia.

A inspeção leu o único fonte Java, o módulo, as configurações pertinentes da IDE e os arquivos de exclusão do Git. Nomes previstos na especificação não foram tratados como classes existentes. Os sete arquivos que já estavam preparados no índice do Git foram preservados, sem commit, descarte ou mudança de estágio pela missão.

A comparação diferencia regras confirmadas, propostas, pendências e ausência de implementação. As validações são exclusivamente documentais: links, âncoras, inventário de IDs, tabelas, exemplos, revisão textual de diagramas e preservação dos arquivos anteriores. Consulte os resultados efetivos e os limites no [relatório](doc/relatorio-entrega.md).

<a id="doc-04"></a>

## DOC-04 — Entrega e estado final

O [relatório de entrega](doc/relatorio-entrega.md) relaciona todos os arquivos criados, sua finalidade, a cobertura e os resultados da auditoria. Há 15 casos de uso, 24 cenários futuros de aceite, 13 pendências abertas e quatro registros de lacunas da implementação. Cobertura documental não equivale a sistema implementado, modelo sem pendências ou testes aprovados.
