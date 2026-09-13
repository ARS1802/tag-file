# Rastreabilidade e cobertura

[Índice](../README.md) · [Decisões e divergências](decisoes-e-pendencias.md) · [Relatório](relatorio-entrega.md)

A matriz liga a especificação ao documento principal e à evidência disponível. **Planejado/aprovado não significa implementado.** Os IDs originais foram preservados. Não existem resultados de execução da aplicação ou do banco nesta entrega.

<a id="evidencias"></a>

## Evidências observadas

| ID de evidência | Caminho real e trecho identificável | Alcance e limite |
|---|---|---|
| E-01 | Inventário de `/home/arthur/IdeaProjects/tag-file`: `src/Main.java`, `tag-file.iml`, `.gitignore`, `.idea/.gitignore`, `.idea/misc.xml`, `.idea/modules.xml`, `.idea/vcs.xml`. | Esses são os sete arquivos já presentes no índice do Git. A inspeção da árvore de fontes e arquivos do projeto não encontrou outras classes Java, SQL, testes, `database`, `lib`, JAR ou documentação anterior. Arquivos internos de `.git` e estado local ignorado da IDE não constituem implementação do domínio. |
| E-02 | [src/Main.java](../src/Main.java), `Main.main`, `System.out.printf("Hello and welcome!")` e `for (int i = 1; i <= 5; i++)`. | Programa inicial da IDE. Não contém modelos, UI Swing, Commands, Services, DAOs ou inicialização MySQL. Foi lido, não executado. |
| E-03 | [.idea/misc.xml](../.idea/misc.xml), `ProjectRootManager`: `languageLevel="JDK_24"`, `project-jdk-name="corretto-24"`. | Comprova configuração da IDE para JDK 24, não instalação/validade do SDK no computador nem compilação. |
| E-04 | [tag-file.iml](../tag-file.iml), `NewModuleRootManager`: `sourceFolder` em `src`, `orderEntry` com `inheritedJdk` e `sourceFolder`. [.idea/modules.xml](../.idea/modules.xml) referencia esse módulo. | Não há biblioteca Connector/J declarada no módulo. Não há Maven ou outro gerenciador de dependências encontrado. |
| E-05 | [.gitignore](../.gitignore) e [.idea/.gitignore](../.idea/.gitignore), regras de artefatos/estado de IDE; [.idea/vcs.xml](../.idea/vcs.xml), mapeamento Git do projeto. | Não há política específica para a futura configuração e dados de `database`. Nenhum desses arquivos foi modificado pela missão. |
| E-06 | [Índice documental](../README.md) e [relatório](relatorio-entrega.md). | Evidência da missão DOC-01 a DOC-04; não é evidência de implementação dos requisitos funcionais. |

Não foram usados números de linha inferidos. As ausências referem-se ao repositório e inventário acima, não a outros projetos, serviços ou instalações da máquina.

<a id="inventario"></a>

## Inventário completo de temas

Os 41 temas do inventário da fonte foram mantidos. A organização editorial concentra regras de comportamento em `requisitos-e-regras.md`, interação/consulta em `interface-e-fluxos.md` e o fluxo correspondente em `casos-de-uso.md`. Isso explica mudanças de destino principal em relação à organização inicialmente proposta. Os arquivos complementares usam referências ao principal.

| Tema | Estado da especificação | Regras | Pendência | Destino principal nesta entrega |
|---|---|---|---|---|
| Objetivo e contexto acadêmico | Confirmado | [OBJ-01](visao-geral.md#obj-01) a [OBJ-03](visao-geral.md#obj-03) | Não | [visao-geral.md](visao-geral.md) |
| Arquivo físico versus registro | Confirmado | [DOM-01](modelo-de-dominio.md#dom-01) | Não | [modelo-de-dominio.md](modelo-de-dominio.md) |
| UUID e caminho único | Confirmado | [DOM-02](modelo-de-dominio.md#dom-02) | [P-08](decisoes-e-pendencias.md#p-08) para equivalência de caminhos | [modelo-de-dominio.md](modelo-de-dominio.md) |
| Atributos e metadados | Parcial no nível físico | [DOM-03](modelo-de-dominio.md#dom-03), [DOM-04](modelo-de-dominio.md#dom-04) | [P-06](decisoes-e-pendencias.md#p-06), [P-10](decisoes-e-pendencias.md#p-10) | [modelo-de-dominio.md](modelo-de-dominio.md) |
| Tags e nomes repetidos | Confirmado | [TAG-01](requisitos-e-regras.md#tag-01) | [P-09](decisoes-e-pendencias.md#p-09) para comparação/validação | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Predefinidas e proteção | Confirmado | [TAG-02](requisitos-e-regras.md#tag-02) | [P-07](decisoes-e-pendencias.md#p-07) | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Tags vazias e disponíveis | Confirmado | [TAG-03](requisitos-e-regras.md#tag-03) | Não | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Extensões específicas/múltiplas | Confirmado | [EXT-01](requisitos-e-regras.md#ext-01) | [P-08](decisoes-e-pendencias.md#p-08) para casos de nome | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Associação incompatível | Confirmado | [EXT-02](requisitos-e-regras.md#ext-02) | Detalhes de lote não fechados | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Edição de restrições | Confirmado | [EXT-03](requisitos-e-regras.md#ext-03) | [P-12](decisoes-e-pendencias.md#p-12) para apresentação em lote | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Sugestão de predefinidas | Parcial | [TAG-04](requisitos-e-regras.md#tag-04) | [P-03](decisoes-e-pendencias.md#p-03) | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Sentinela e ciclo de vida | Confirmado com alcance aberto | [CIC-01](requisitos-e-regras.md#cic-01) a [CIC-03](requisitos-e-regras.md#cic-03) | [P-02](decisoes-e-pendencias.md#p-02) | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Seleção e associação inicial | Confirmado | [OP-01](requisitos-e-regras.md#op-01) | Seleção geral em lote em [P-12](decisoes-e-pendencias.md#p-12) | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Drag and Drop | Confirmado | [UI-02](interface-e-fluxos.md#ui-02) | Componente concreto não fechado | [interface-e-fluxos.md](interface-e-fluxos.md) |
| Relocalização | Confirmado no fluxo principal | [OP-02](requisitos-e-regras.md#op-02) | [P-02](decisoes-e-pendencias.md#p-02), [P-08](decisoes-e-pendencias.md#p-08) | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Mover, recortar e colar | Confirmado | [OP-03](requisitos-e-regras.md#op-03), [OP-08](requisitos-e-regras.md#op-08) | [P-12](decisoes-e-pendencias.md#p-12) | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Copiar e substituir | Parcial | [OP-04](requisitos-e-regras.md#op-04), [OP-06](requisitos-e-regras.md#op-06) | [P-01](decisoes-e-pendencias.md#p-01) | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Renomear e extensão | Confirmado | [OP-05](requisitos-e-regras.md#op-05) | [P-08](decisoes-e-pendencias.md#p-08), [P-12](decisoes-e-pendencias.md#p-12) | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Cancelamento versus falha | Derivado com limites confirmados | [OP-07](requisitos-e-regras.md#op-07), [ERR-01](requisitos-e-regras.md#err-01) | Política de lote | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Três exclusões | Parcial no alcance da segunda | [DEL-01](requisitos-e-regras.md#del-01) a [DEL-04](requisitos-e-regras.md#del-04) | [P-02](decisoes-e-pendencias.md#p-02) | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Dois exploradores | Confirmado | [EXP-01](interface-e-fluxos.md#exp-01) | [P-04](decisoes-e-pendencias.md#p-04), [P-12](decisoes-e-pendencias.md#p-12) | [interface-e-fluxos.md](interface-e-fluxos.md) |
| Filtros e Map | Confirmado | [EXP-02](interface-e-fluxos.md#exp-02), [EXP-03](interface-e-fluxos.md#exp-03) | [P-05](decisoes-e-pendencias.md#p-05), [P-08](decisoes-e-pendencias.md#p-08) | [interface-e-fluxos.md](interface-e-fluxos.md) |
| AND/OR | Confirmado | [EXP-04](interface-e-fluxos.md#exp-04) | [P-05](decisoes-e-pendencias.md#p-05), [P-13](decisoes-e-pendencias.md#p-13) | [interface-e-fluxos.md](interface-e-fluxos.md) |
| Ordenação/desempate | Parcial/futuro | [EXP-05](interface-e-fluxos.md#exp-05) | [P-13](decisoes-e-pendencias.md#p-13) | [interface-e-fluxos.md](interface-e-fluxos.md) |
| Refresh e metadados | Confirmado no fluxo | [SYN-01](requisitos-e-regras.md#syn-01) a [SYN-03](requisitos-e-regras.md#syn-03) | [P-10](decisoes-e-pendencias.md#p-10) | [requisitos-e-regras.md](requisitos-e-regras.md) |
| Loading e erros | Confirmado na UI | [UI-03](interface-e-fluxos.md#ui-03), [ERR-01](requisitos-e-regras.md#err-01) | Execução interna não fechada | [interface-e-fluxos.md](interface-e-fluxos.md) / [requisitos-e-regras.md](requisitos-e-regras.md) |
| Factory, Command e DAO | Confirmado | [ARQ-02](arquitetura-e-padroes.md#arq-02) a [ARQ-04](arquitetura-e-padroes.md#arq-04) | Assinaturas não homologadas | [arquitetura-e-padroes.md](arquitetura-e-padroes.md) |
| Controllers, Screens e Observer | Núcleo confirmado | [ARQ-05](arquitetura-e-padroes.md#arq-05), [ARQ-06](arquitetura-e-padroes.md#arq-06) | [P-04](decisoes-e-pendencias.md#p-04), [P-05](decisoes-e-pendencias.md#p-05) | [arquitetura-e-padroes.md](arquitetura-e-padroes.md) |
| Services e Managers | Confirmado | [ARQ-07](arquitetura-e-padroes.md#arq-07), [ARQ-08](arquitetura-e-padroes.md#arq-08) | Detalhes operacionais | [arquitetura-e-padroes.md](arquitetura-e-padroes.md) |
| GRASP e classificação de padrões | Exigência confirmada; explicação analítica | [ARQ-09](arquitetura-e-padroes.md#arq-09) | Não inventar padrões | [arquitetura-e-padroes.md](arquitetura-e-padroes.md) |
| Modelo relacional | Conceitual confirmado | [SQL-01](banco-de-dados.md#sql-01) a [SQL-04](banco-de-dados.md#sql-04) | [P-06](decisoes-e-pendencias.md#p-06) | [banco-de-dados.md](banco-de-dados.md) |
| Transações e falhas | Limitação confirmada | [SQL-05](banco-de-dados.md#sql-05), [ERR-01](requisitos-e-regras.md#err-01) | Sem recuperação completa | [banco-de-dados.md](banco-de-dados.md) / [requisitos-e-regras.md](requisitos-e-regras.md) |
| Criação/alteração do schema | Requisito confirmado | [SQL-06](banco-de-dados.md#sql-06) | [P-11](decisoes-e-pendencias.md#p-11) | [banco-de-dados.md](banco-de-dados.md) |
| Windows, Ubuntu e Linux Mint | Confirmado | [AMB-01](instalacao-e-execucao.md#amb-01) | [P-11](decisoes-e-pendencias.md#p-11) | [instalacao-e-execucao.md](instalacao-e-execucao.md) |
| Diretório database e dados | Confirmado na organização | [AMB-02](instalacao-e-execucao.md#amb-02) | Nomes efetivos dos schemas | [instalacao-e-execucao.md](instalacao-e-execucao.md) |
| Porta e credenciais didáticas | Consolidado | [AMB-03](instalacao-e-execucao.md#amb-03) | Verificar diferença no repositório | [instalacao-e-execucao.md](instalacao-e-execucao.md) |
| Conexão compartilhada | Confirmado | [AMB-05](instalacao-e-execucao.md#amb-05) | Validação/serialização interna | [instalacao-e-execucao.md](instalacao-e-execucao.md) |
| JDK, JDBC e driver | Contexto definido | [AMB-06](instalacao-e-execucao.md#amb-06) | [P-11](decisoes-e-pendencias.md#p-11) | [instalacao-e-execucao.md](instalacao-e-execucao.md) |
| Encerramento e preservação | Responsabilidade aprovada | [AMB-07](instalacao-e-execucao.md#amb-07) | [P-11](decisoes-e-pendencias.md#p-11) | [instalacao-e-execucao.md](instalacao-e-execucao.md) |
| Aceite e apresentação | Cenários derivados | [ACE-01](criterios-de-aceite.md#ace-01) a [ACE-24](criterios-de-aceite.md#ace-24) | Nenhuma execução comprovada | [criterios-de-aceite.md](criterios-de-aceite.md) |
| Missão documental | Confirmado | [DOC-01](../README.md#doc-01) a [DOC-04](../README.md#doc-04) | Nenhuma autorização de implementar | [README.md](../README.md) |

<a id="regras"></a>

## Comparação por regra

Cada linha resume a regra; os efeitos completos e as pendências estão no documento principal. A evidência agrupada identifica arquivos/símbolos reais, definidos acima, sem criar uma auditoria fictícia de classes ausentes.

| ID | Comportamento / estado na especificação | Evidência observada | Situação frente ao repositório | Documento principal |
|---|---|---|---|---|
| [OBJ-01](visao-geral.md#obj-01) | Problema e finalidade — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado como aplicativo implementado | [visao-geral.md](visao-geral.md#obj-01) |
| [OBJ-02](visao-geral.md#obj-02) | Contexto e tecnologias — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Parcial: fonte Java presente; Swing/MySQL não encontrados | [visao-geral.md](visao-geral.md#obj-02) |
| [OBJ-03](visao-geral.md#obj-03) | Evolução do escopo — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não verificável como evolução da implementação; histórico da especificação preservado | [visao-geral.md](visao-geral.md#obj-03) |
| [DOC-01](../README.md#doc-01) | Missão exclusivamente documental — Confirmada na especificação | E-06: documentos produzidos, matriz e relatório desta missão. | Atendido documentalmente | [README.md](../README.md#doc-01) |
| [DOM-01](modelo-de-dominio.md#dom-01) | Representações do domínio — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [modelo-de-dominio.md](modelo-de-dominio.md#dom-01) |
| [DOM-02](modelo-de-dominio.md#dom-02) | Identidade versus localização — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [modelo-de-dominio.md](modelo-de-dominio.md#dom-02) |
| [TAG-01](requisitos-e-regras.md#tag-01) | Criação, edição e nomes repetidos — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#tag-01) |
| [TAG-02](requisitos-e-regras.md#tag-02) | Tags predefinidas — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#tag-02) |
| [TAG-03](requisitos-e-regras.md#tag-03) | Tags vazias e contagem de disponíveis — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#tag-03) |
| [EXT-01](requisitos-e-regras.md#ext-01) | Extensões específicas, múltiplas e normalizadas — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#ext-01) |
| [EXT-02](requisitos-e-regras.md#ext-02) | Incompatibilidade ao associar — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#ext-02) |
| [EXT-03](requisitos-e-regras.md#ext-03) | Edição de extensões já utilizadas — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#ext-03) |
| [TAG-04](requisitos-e-regras.md#tag-04) | Sugestão de Tag predefinida — Parcial; P-03 | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#tag-04) |
| [CIC-01](requisitos-e-regras.md#cic-01) | Primeira classificação e reorganização durante a sessão — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#cic-01) |
| [CIC-02](requisitos-e-regras.md#cic-02) | Limpeza exclusiva da inicialização — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#cic-02) |
| [CIC-03](requisitos-e-regras.md#cic-03) | Limites da limpeza — derivada das decisões de sessão e de Refresh | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#cic-03) |
| [OP-01](requisitos-e-regras.md#op-01) | Seleção, cadastro e associação inicial — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#op-01) |
| [OP-02](requisitos-e-regras.md#op-02) | Utilização e recuperação de referência indisponível — Fluxo principal confirmado; P-02/P-08 | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#op-02) |
| [OP-03](requisitos-e-regras.md#op-03) | Mover, recortar e colar — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#op-03) |
| [OP-04](requisitos-e-regras.md#op-04) | Copiar — Função confirmada; identidade e cópia sem Tags em P-01 | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#op-04) |
| [OP-05](requisitos-e-regras.md#op-05) | Renomear e mudar extensão — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#op-05) |
| [OP-06](requisitos-e-regras.md#op-06) | Conflitos de nome ou caminho — Parcial; P-01/P-08 | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#op-06) |
| [OP-07](requisitos-e-regras.md#op-07) | Cancelamento versus falha parcial — consequência derivada com limites confirmados | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#op-07) |
| [OP-08](requisitos-e-regras.md#op-08) | Clipboard interno — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#op-08) |
| [DEL-01](requisitos-e-regras.md#del-01) | Deletar apenas a etiqueta — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#del-01) |
| [DEL-02](requisitos-e-regras.md#del-02) | Deletar todas as etiquetas dos arquivos que possuem a selecionada — Efeito original confirmado; alcance em P-02 | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#del-02) |
| [DEL-03](requisitos-e-regras.md#del-03) | Deletar permanentemente os arquivos da etiqueta — confirmada quanto ao resultado final | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#del-03) |
| [DEL-04](requisitos-e-regras.md#del-04) | Confirmações e clareza — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#del-04) |
| [EXP-01](interface-e-fluxos.md#exp-01) | Duas visões interoperáveis — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [interface-e-fluxos.md](interface-e-fluxos.md#exp-01) |
| [EXP-02](interface-e-fluxos.md#exp-02) | Filtros por tipo de informação — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [interface-e-fluxos.md](interface-e-fluxos.md#exp-02) |
| [EXP-03](interface-e-fluxos.md#exp-03) | Correspondência em lote com Map — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [interface-e-fluxos.md](interface-e-fluxos.md#exp-03) |
| [EXP-04](interface-e-fluxos.md#exp-04) | AND e OR — AND/OR confirmados; API e vazio em P-05/P-13 | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [interface-e-fluxos.md](interface-e-fluxos.md#exp-04) |
| [EXP-05](interface-e-fluxos.md#exp-05) | Ordenação e desempate — Parcial / futuro; P-13 | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [interface-e-fluxos.md](interface-e-fluxos.md#exp-05) |
| [UI-01](interface-e-fluxos.md#ui-01) | Screens conectam componentes aos Controllers — confirmada na divisão de responsabilidades | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [interface-e-fluxos.md](interface-e-fluxos.md#ui-01) |
| [UI-02](interface-e-fluxos.md#ui-02) | Etiquetas visíveis e Drag and Drop — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [interface-e-fluxos.md](interface-e-fluxos.md#ui-02) |
| [SYN-01](requisitos-e-regras.md#syn-01) | Quando atualizar e com qual alcance — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#syn-01) |
| [SYN-02](requisitos-e-regras.md#syn-02) | Metadados sincronizados — consolidado no fluxo discutido | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#syn-02) |
| [SYN-03](requisitos-e-regras.md#syn-03) | Refresh único, sem limpeza nem duplicação — confirmada no botão; demais restrições derivadas | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#syn-03) |
| [UI-03](interface-e-fluxos.md#ui-03) | Loading durante consultas/operações — confirmada na interação | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [interface-e-fluxos.md](interface-e-fluxos.md#ui-03) |
| [ERR-01](requisitos-e-regras.md#err-01) | Falhas e resultados parciais — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [requisitos-e-regras.md](requisitos-e-regras.md#err-01) |
| [DOM-03](modelo-de-dominio.md#dom-03) | LocalFile e metadados — Informações confirmadas; representação parcial, P-06/P-10 | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [modelo-de-dominio.md](modelo-de-dominio.md#dom-03) |
| [DOM-04](modelo-de-dominio.md#dom-04) | Tag — Informações confirmadas; P-07/P-09/P-10 | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [modelo-de-dominio.md](modelo-de-dominio.md#dom-04) |
| [ARQ-01](arquitetura-e-padroes.md#arq-01) | Separação geral — Responsabilidades aprovadas; pacotes não fechados | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [arquitetura-e-padroes.md](arquitetura-e-padroes.md#arq-01) |
| [ARQ-02](arquitetura-e-padroes.md#arq-02) | Factory de entidades — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [arquitetura-e-padroes.md](arquitetura-e-padroes.md#arq-02) |
| [ARQ-03](arquitetura-e-padroes.md#arq-03) | Command — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [arquitetura-e-padroes.md](arquitetura-e-padroes.md#arq-03) |
| [ARQ-04](arquitetura-e-padroes.md#arq-04) | CrudDAO e DAO de associação — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [arquitetura-e-padroes.md](arquitetura-e-padroes.md#arq-04) |
| [ARQ-05](arquitetura-e-padroes.md#arq-05) | Dois Controllers — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [arquitetura-e-padroes.md](arquitetura-e-padroes.md#arq-05) |
| [ARQ-06](arquitetura-e-padroes.md#arq-06) | Observer — Núcleo confirmado; P-04/P-05 | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [arquitetura-e-padroes.md](arquitetura-e-padroes.md#arq-06) |
| [ARQ-07](arquitetura-e-padroes.md#arq-07) | Services e LocalFileManager — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [arquitetura-e-padroes.md](arquitetura-e-padroes.md#arq-07) |
| [ARQ-08](arquitetura-e-padroes.md#arq-08) | DatabaseManager e DatabaseConnection — confirmada | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [arquitetura-e-padroes.md](arquitetura-e-padroes.md#arq-08) |
| [ARQ-09](arquitetura-e-padroes.md#arq-09) | Padrões e justificativas acadêmicas — Categorias e explicações analíticas conforme escopo | E-01/E-02: inventário de fontes; `Main.main` é o template. | Não encontrado | [arquitetura-e-padroes.md](arquitetura-e-padroes.md#arq-09) |
| [SQL-01](banco-de-dados.md#sql-01) | Tabelas e cardinalidades — modelo lógico/conceitual confirmado | E-01/E-04: nenhum SQL/DAO; módulo sem biblioteca adicional. | Não encontrado | [banco-de-dados.md](banco-de-dados.md#sql-01) |
| [SQL-02](banco-de-dados.md#sql-02) | Chaves e unicidade — regras lógicas confirmadas | E-01/E-04: nenhum SQL/DAO; módulo sem biblioteca adicional. | Não encontrado | [banco-de-dados.md](banco-de-dados.md#sql-02) |
| [SQL-03](banco-de-dados.md#sql-03) | Tipos e dicionário físico — Tipos parcialmente definidos; P-06/P-08/P-09/P-10 | E-01/E-04: nenhum SQL/DAO; módulo sem biblioteca adicional. | Não encontrado | [banco-de-dados.md](banco-de-dados.md#sql-03) |
| [SQL-04](banco-de-dados.md#sql-04) | Integridade e atualização de extensões — efeitos confirmados; mecanismo físico não homologado | E-01/E-04: nenhum SQL/DAO; módulo sem biblioteca adicional. | Não encontrado | [banco-de-dados.md](banco-de-dados.md#sql-04) |
| [SQL-05](banco-de-dados.md#sql-05) | Sem transações explícitas — confirmada | E-01/E-04: nenhum SQL/DAO; módulo sem biblioteca adicional. | Não encontrado | [banco-de-dados.md](banco-de-dados.md#sql-05) |
| [SQL-06](banco-de-dados.md#sql-06) | Schema de criação e alteração, sem histórico — requisito confirmado; estratégia detalhada aberta | E-01/E-04: nenhum SQL/DAO; módulo sem biblioteca adicional. | Não encontrado | [banco-de-dados.md](banco-de-dados.md#sql-06) |
| [AMB-01](instalacao-e-execucao.md#amb-01) | Plataformas e autorização para instalar — confirmada | E-01/E-03/E-04: JDK 24 configurado; sem infraestrutura/driver no repositório. | Não encontrado | [instalacao-e-execucao.md](instalacao-e-execucao.md#amb-01) |
| [AMB-02](instalacao-e-execucao.md#amb-02) | Estrutura database e localização dos dados — organização confirmada, com nomes de arquivos de referência | E-01/E-03/E-04: JDK 24 configurado; sem infraestrutura/driver no repositório. | Não encontrado | [instalacao-e-execucao.md](instalacao-e-execucao.md#amb-02) |
| [AMB-03](instalacao-e-execucao.md#amb-03) | Parâmetros públicos de desenvolvimento — Parâmetros públicos consolidados | E-01/E-03/E-04: JDK 24 configurado; sem infraestrutura/driver no repositório. | Não encontrado | [instalacao-e-execucao.md](instalacao-e-execucao.md#amb-03) |
| [AMB-04](instalacao-e-execucao.md#amb-04) | Preparação inicial e abertura subsequente — Fluxo discutido; comandos não homologados | E-01/E-03/E-04: JDK 24 configurado; sem infraestrutura/driver no repositório. | Não encontrado | [instalacao-e-execucao.md](instalacao-e-execucao.md#amb-04) |
| [AMB-05](instalacao-e-execucao.md#amb-05) | Conexão JDBC compartilhada e reconexão — confirmada no modelo simplificado | E-01/E-03/E-04: JDK 24 configurado; sem infraestrutura/driver no repositório. | Não encontrado | [instalacao-e-execucao.md](instalacao-e-execucao.md#amb-05) |
| [AMB-06](instalacao-e-execucao.md#amb-06) | JDK, JDBC e dependências — JDK 24 considerado; Maven rejeitado; driver/versão abertos | E-01/E-03/E-04: JDK 24 configurado; sem infraestrutura/driver no repositório. | Parcial: JDK 24 e ausência de Maven alinhados; Connector/J ausente | [instalacao-e-execucao.md](instalacao-e-execucao.md#amb-06) |
| [AMB-07](instalacao-e-execucao.md#amb-07) | Encerramento e preservação — Responsabilidade aprovada; encerramento em P-11 | E-01/E-03/E-04: JDK 24 configurado; sem infraestrutura/driver no repositório. | Não encontrado | [instalacao-e-execucao.md](instalacao-e-execucao.md#amb-07) |
| [DOC-02](../README.md#doc-02) | Conteúdo autossuficiente e estrutura documental proposta — Confirmada na especificação | E-06: documentos produzidos, matriz e relatório desta missão. | Atendido documentalmente | [README.md](../README.md#doc-02) |
| [DOC-03](../README.md#doc-03) | Procedimento de execução documental — Confirmada na especificação | E-06: documentos produzidos, matriz e relatório desta missão. | Atendido documentalmente | [README.md](../README.md#doc-03) |
| [DOC-04](../README.md#doc-04) | Relatório de entrega — Confirmada na especificação | E-06: documentos produzidos, matriz e relatório desta missão. | Atendido documentalmente | [README.md](../README.md#doc-04) |

<a id="casos"></a>

## Casos de uso

Os fluxos mantêm seus IDs e referências de origem. P-01 afeta UC-08; P-02 afeta UC-11/UC-12 e limites de reorganização; P-03 afeta a sugestão na criação de registros, inclusive UC-04/UC-05. Nenhum fluxo está comprovado pelo programa inicial.

| ID | Fluxo | Origem na especificação | Observação / situação |
|---|---|---|---|
| [UC-01](casos-de-uso.md#uc-01) | Preparar e abrir o Tag-File | [AMB-01](instalacao-e-execucao.md#amb-01) a [AMB-06](instalacao-e-execucao.md#amb-06), [CIC-02](requisitos-e-regras.md#cic-02), [SYN-01](requisitos-e-regras.md#syn-01). | E-01/E-02; não encontrado, não executado |
| [UC-02](casos-de-uso.md#uc-02) | Navegar em Arquivos Local | [DOM-01](modelo-de-dominio.md#dom-01), [EXP-01](interface-e-fluxos.md#exp-01) a [EXP-03](interface-e-fluxos.md#exp-03), [UI-02](interface-e-fluxos.md#ui-02), [SYN-01](requisitos-e-regras.md#syn-01). | E-01/E-02; não encontrado, não executado |
| [UC-03](casos-de-uso.md#uc-03) | Pesquisar em Arquivos por Tag | [EXP-01](interface-e-fluxos.md#exp-01), [EXP-02](interface-e-fluxos.md#exp-02), [EXP-04](interface-e-fluxos.md#exp-04), [SYN-01](requisitos-e-regras.md#syn-01). | E-01/E-02; não encontrado, não executado |
| [UC-04](casos-de-uso.md#uc-04) | Criar uma Tag e associar arquivos iniciais | [TAG-01](requisitos-e-regras.md#tag-01), [TAG-02](requisitos-e-regras.md#tag-02), [EXT-01](requisitos-e-regras.md#ext-01), [OP-01](requisitos-e-regras.md#op-01), [ARQ-02](arquitetura-e-padroes.md#arq-02). | E-01/E-02; não encontrado, não executado |
| [UC-05](casos-de-uso.md#uc-05) | Associar arquivo a Tag existente | [OP-01](requisitos-e-regras.md#op-01), [EXT-02](requisitos-e-regras.md#ext-02), [DOM-02](modelo-de-dominio.md#dom-02), [CIC-01](requisitos-e-regras.md#cic-01), [TAG-04](requisitos-e-regras.md#tag-04). | E-01/E-02; não encontrado, não executado |
| [UC-06](casos-de-uso.md#uc-06) | Remover uma associação durante a reorganização | [CIC-01](requisitos-e-regras.md#cic-01), [DEL-01](requisitos-e-regras.md#del-01) quando aplicável, [UI-02](interface-e-fluxos.md#ui-02). | E-01/E-02; não encontrado, não executado |
| [UC-07](casos-de-uso.md#uc-07) | Editar uma Tag | [TAG-01](requisitos-e-regras.md#tag-01), [EXT-01](requisitos-e-regras.md#ext-01), [EXT-03](requisitos-e-regras.md#ext-03), [DOM-04](modelo-de-dominio.md#dom-04). | E-01/E-02; não encontrado, não executado |
| [UC-08](casos-de-uso.md#uc-08) | Copiar arquivo, com ou sem substituição | [OP-04](requisitos-e-regras.md#op-04), [OP-06](requisitos-e-regras.md#op-06), [OP-08](requisitos-e-regras.md#op-08), [P-01](decisoes-e-pendencias.md#p-01). | E-01/E-02; não encontrado, não executado |
| [UC-09](casos-de-uso.md#uc-09) | Recortar em uma visão e colar na outra | [OP-03](requisitos-e-regras.md#op-03), [OP-08](requisitos-e-regras.md#op-08), [EXP-01](interface-e-fluxos.md#exp-01). | E-01/E-02; não encontrado, não executado |
| [UC-10](casos-de-uso.md#uc-10) | Renomear arquivo | [OP-05](requisitos-e-regras.md#op-05), [EXT-02](requisitos-e-regras.md#ext-02)/[EXT-03](requisitos-e-regras.md#ext-03) por compatibilidade, [CIC-01](requisitos-e-regras.md#cic-01). | E-01/E-02; não encontrado, não executado |
| [UC-11](casos-de-uso.md#uc-11) | Relocalizar ou remover referência indisponível | [OP-02](requisitos-e-regras.md#op-02), [DOM-02](modelo-de-dominio.md#dom-02), [SYN-01](requisitos-e-regras.md#syn-01), [P-02](decisoes-e-pendencias.md#p-02). | E-01/E-02; não encontrado, não executado |
| [UC-12](casos-de-uso.md#uc-12) | Excluir Tag em uma das três modalidades | [DEL-01](requisitos-e-regras.md#del-01) a [DEL-04](requisitos-e-regras.md#del-04), [CIC-01](requisitos-e-regras.md#cic-01), [P-02](decisoes-e-pendencias.md#p-02). | E-01/E-02; não encontrado, não executado |
| [UC-13](casos-de-uso.md#uc-13) | Localizar Tags vazias | [TAG-03](requisitos-e-regras.md#tag-03), [TAG-02](requisitos-e-regras.md#tag-02). | E-01/E-02; não encontrado, não executado |
| [UC-14](casos-de-uso.md#uc-14) | Refresh e filtros | [SYN-01](requisitos-e-regras.md#syn-01) a [SYN-03](requisitos-e-regras.md#syn-03), [UI-03](interface-e-fluxos.md#ui-03), [ARQ-05](arquitetura-e-padroes.md#arq-05) a [ARQ-07](arquitetura-e-padroes.md#arq-07). | E-01/E-02; não encontrado, não executado |
| [UC-15](casos-de-uso.md#uc-15) | Apresentar falha parcial | [ERR-01](requisitos-e-regras.md#err-01), [SQL-05](banco-de-dados.md#sql-05), [AMB-05](instalacao-e-execucao.md#amb-05). | E-01/E-02; não encontrado, não executado |

<a id="aceite"></a>

## Cenários de aceite

As condições e resultados estão em [critérios de aceite](criterios-de-aceite.md). Esta tabela registra rastreabilidade, sem transformar os cenários em testes realizados.

| ID | Origem | Situação |
|---|---|---|
| [ACE-01](criterios-de-aceite.md#ace-01) | [DOM-01](modelo-de-dominio.md#dom-01), [EXP-01](interface-e-fluxos.md#exp-01) | Não executado; não há evidência funcional no repositório |
| [ACE-02](criterios-de-aceite.md#ace-02) | [DOM-02](modelo-de-dominio.md#dom-02), [OP-01](requisitos-e-regras.md#op-01) | Não executado; não há evidência funcional no repositório |
| [ACE-03](criterios-de-aceite.md#ace-03) | [DOM-02](modelo-de-dominio.md#dom-02) | Não executado; não há evidência funcional no repositório |
| [ACE-04](criterios-de-aceite.md#ace-04) | [EXT-01](requisitos-e-regras.md#ext-01) | Não executado; não há evidência funcional no repositório |
| [ACE-05](criterios-de-aceite.md#ace-05) | [EXT-01](requisitos-e-regras.md#ext-01), [SQL-02](banco-de-dados.md#sql-02) | Não executado; não há evidência funcional no repositório |
| [ACE-06](criterios-de-aceite.md#ace-06) | [EXT-01](requisitos-e-regras.md#ext-01) | Não executado; não há evidência funcional no repositório |
| [ACE-07](criterios-de-aceite.md#ace-07) | [EXT-02](requisitos-e-regras.md#ext-02) | Não executado; não há evidência funcional no repositório |
| [ACE-08](criterios-de-aceite.md#ace-08) | [EXT-03](requisitos-e-regras.md#ext-03) | Não executado; não há evidência funcional no repositório |
| [ACE-09](criterios-de-aceite.md#ace-09) | [TAG-01](requisitos-e-regras.md#tag-01) | Não executado; não há evidência funcional no repositório |
| [ACE-10](criterios-de-aceite.md#ace-10) | [CIC-01](requisitos-e-regras.md#cic-01) | Não executado; não há evidência funcional no repositório |
| [ACE-11](criterios-de-aceite.md#ace-11) | [CIC-01](requisitos-e-regras.md#cic-01) | Não executado; não há evidência funcional no repositório |
| [ACE-12](criterios-de-aceite.md#ace-12) | [CIC-02](requisitos-e-regras.md#cic-02) | Não executado; não há evidência funcional no repositório |
| [ACE-13](criterios-de-aceite.md#ace-13) | [CIC-03](requisitos-e-regras.md#cic-03), [SYN-03](requisitos-e-regras.md#syn-03) | Não executado; não há evidência funcional no repositório |
| [ACE-14](criterios-de-aceite.md#ace-14) | [TAG-02](requisitos-e-regras.md#tag-02), [TAG-03](requisitos-e-regras.md#tag-03) | Não executado; não há evidência funcional no repositório |
| [ACE-15](criterios-de-aceite.md#ace-15) | [EXP-04](interface-e-fluxos.md#exp-04) | Não executado; não há evidência funcional no repositório |
| [ACE-16](criterios-de-aceite.md#ace-16) | [EXP-03](interface-e-fluxos.md#exp-03) | Não executado; não há evidência funcional no repositório |
| [ACE-17](criterios-de-aceite.md#ace-17) | [OP-03](requisitos-e-regras.md#op-03) | Não executado; não há evidência funcional no repositório |
| [ACE-18](criterios-de-aceite.md#ace-18) | [OP-05](requisitos-e-regras.md#op-05) | Não executado; não há evidência funcional no repositório |
| [ACE-19](criterios-de-aceite.md#ace-19) | [DEL-01](requisitos-e-regras.md#del-01) a [DEL-04](requisitos-e-regras.md#del-04) | Não executado; não há evidência funcional no repositório |
| [ACE-20](criterios-de-aceite.md#ace-20) | [OP-04](requisitos-e-regras.md#op-04), [P-01](decisoes-e-pendencias.md#p-01) | Não executado; não há evidência funcional no repositório |
| [ACE-21](criterios-de-aceite.md#ace-21) | [SYN-01](requisitos-e-regras.md#syn-01), [SYN-03](requisitos-e-regras.md#syn-03) | Não executado; não há evidência funcional no repositório |
| [ACE-22](criterios-de-aceite.md#ace-22) | [ERR-01](requisitos-e-regras.md#err-01) | Não executado; não há evidência funcional no repositório |
| [ACE-23](criterios-de-aceite.md#ace-23) | [AMB-02](instalacao-e-execucao.md#amb-02), [AMB-03](instalacao-e-execucao.md#amb-03) | Não executado; não há evidência funcional no repositório |
| [ACE-24](criterios-de-aceite.md#ace-24) | [AMB-01](instalacao-e-execucao.md#amb-01) | Não executado; não há evidência funcional no repositório |

## Pendências e exclusões de escopo

[P-01 a P-13](decisoes-e-pendencias.md#p-01) estão explicitadas com perguntas, limites e documentos afetados. [DIV-001 a DIV-004](decisoes-e-pendencias.md#div-001) descrevem lacunas de implementação. As restrições de [visão geral](visao-geral.md) cobrem itens excluídos da versão, incluindo Maven, pool, transações explícitas, Undo/Redo, clipboard externo, monitoramento, NOT e `schema_history`. Não são funcionalidades omitidas da documentação por falta de implementação.

## Proveniência da fonte

Fonte lida: `/home/arthur/Projetos/tag-file/prompts/prompt-documentacao-tag-file.md` (1.866 linhas). SHA-256 do conteúdo lido: `c2054c17c7faefbe44f9123a624edd5a23dc021943c9eff387eb042838f5094f`. O digest serve para identificar a versão documental inspecionada; não certifica regras, código ou testes.
