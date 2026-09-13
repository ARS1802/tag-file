# Documentação de projeto do Tag-File

**Esta documentação vem antes da implementação.** Ela orienta estudantes sobre o que construir, por que cada parte existe, como as responsabilidades deverão colaborar e quais decisões faltam. Classes, tabelas e scripts descritos são elementos planejados, não arquivos cuja existência seja necessária para entender o projeto.

A base é a especificação consolidada lida integralmente em `/home/arthur/Projetos/tag-file/prompts/prompt-documentacao-tag-file.md`. A raiz de trabalho é `/home/arthur/IdeaProjects/tag-file`, e esta entrega está em sua pasta `doc`. O conteúdo técnico é autossuficiente: o caminho externo registra a proveniência, sem exigir consulta à conversa original.

A arquitetura vigente usa chamadas diretas dos Controllers aos Managers/Services pertinentes ([ARQ-03](arquitetura-e-padroes.md#arq-03) e [DEC-01](decisoes-e-pendencias.md#dec-01)). **[P-01](decisoes-e-pendencias.md#p-01) a [P-13](decisoes-e-pendencias.md#p-13) permanecem abertas**; sua existência não impede documentar os comportamentos já aprovados.

<a id="doc-01"></a>

## DOC-01 — Missão e limites

A missão produz somente documentação dentro de `doc`. Não cria classes Java, testes executáveis, configurações operacionais, dependências ou schemas. Procedimentos de instalação, inicialização, SQL e uso da aplicação são planejamento, sem execução nesta entrega.

A ausência esperada de implementação não é defeito, divergência ou pré-condição documental. A leitura inicial da pasta e das instruções serve para identificar o destino e preservar o trabalho existente. Nenhuma configuração ou mudança preparada no Git foi alterada pela missão.

## Legenda de estados

| Estado | Significado |
|---|---|
| Confirmada | Decisão aprovada para o projeto; não significa implementada. |
| Delegada | Escolha resolvida dentro de autorização expressa, sem implicar execução. |
| Derivada | Consequência necessária de decisões confirmadas. |
| Proposta | Exemplo ou alternativa sem aprovação inequívoca. |
| Pendente | Contrato, valor ou comportamento ainda aberto. |
| Substituída | Definição antiga, mantida somente no histórico pertinente. |
| Fora do escopo / não definido | Restrição da versão ou ausência de requisito; não preencher por convenção. |
| Limitação conhecida | Escolha consciente, como ausência de transações explícitas. |
| Exemplo ilustrativo / não executado | Material explicativo, sem resultado de funcionamento. |

O presente dos documentos é normativo: “Tag aceita” descreve o que deverá aceitar. Identificadores como [DOM-01](modelo-de-dominio.md#dom-01), [UC-05](casos-de-uso.md#uc-05) e [ACE-02](criterios-de-aceite.md#ace-02) relacionam partes da especificação; não são números de mensagens.

<a id="doc-02"></a>

## DOC-02 — Índice e ordem de leitura

| Ordem | Documento | Finalidade | Grupos principais |
|---|---|---|---|
| 1 | [visao-geral.md](visao-geral.md) | Problema, escopo, restrições e glossário | OBJ |
| 2 | [requisitos-e-regras.md](requisitos-e-regras.md) | Regras normativas, condições e efeitos | TAG, EXT, CIC, OP, DEL, SYN, ERR |
| 3 | [casos-de-uso.md](casos-de-uso.md) | Percursos [UC-01](casos-de-uso.md#uc-01) a [UC-15](casos-de-uso.md#uc-15) | UC |
| 4 | [modelo-de-dominio.md](modelo-de-dominio.md) | Representações, atributos, identidade e ciclo de vida | DOM |
| 5 | [banco-de-dados.md](banco-de-dados.md) | Modelo lógico, dicionário e escolhas físicas abertas | SQL |
| 6 | [arquitetura-e-padroes.md](arquitetura-e-padroes.md) | Responsabilidades, colaborações e orientação de construção | ARQ |
| 7 | [interface-e-fluxos.md](interface-e-fluxos.md) | Exploradores, consultas, componentes e diálogos | EXP, UI |
| 8 | [instalacao-e-execucao.md](instalacao-e-execucao.md) | Ambiente e procedimentos operacionais futuros | AMB |
| 9 | [criterios-de-aceite.md](criterios-de-aceite.md) | Condições e resultados para verificação futura | ACE |
| 10 | [decisoes-e-pendencias.md](decisoes-e-pendencias.md) | [DEC-01](decisoes-e-pendencias.md#dec-01), revisões, propostas e [P-01](decisoes-e-pendencias.md#p-01) a [P-13](decisoes-e-pendencias.md#p-13) | DEC, P |
| 11 | [rastreabilidade.md](rastreabilidade.md) | Correspondência de regras, participantes, fluxos e aceite | Matriz |
| 12 | [relatorio-entrega.md](relatorio-entrega.md) | Arquivos produzidos, cobertura e validação documental | Entrega |

Para começar: leia a visão geral, as regras e os casos de uso; consulte domínio e banco para compreender os dados; então siga a [orientação para a implementação futura](arquitetura-e-padroes.md#orientacao-construcao). Ela decompõe os grupos aprovados, explica entradas/resultados e percorre associação, recorte/colagem e Refresh. Interface e ambiente completam o desenho, e os critérios de aceite mostram o resultado que deverá ser verificado depois.

A matriz explica a correspondência com os 41 temas do inventário. Cada regra tem um documento principal; exemplos em outras páginas fazem referência à norma. Não há cronograma, atribuição de pessoas ou novas ferramentas nessa sequência editorial.

<a id="doc-03"></a>

## DOC-03 — Produção e conferência documental

A produção partiu da leitura integral das 1.998 linhas da fonte atual e da identificação mínima da pasta/instruções aplicáveis. A versão atual substitui a orientação anterior centrada em auditoria de implementação. As classes não são procuradas para decidir regras ou fechar pendências.

A rastreabilidade relaciona estado, comportamento planejado, elementos aprovados, fluxo, aceite, seção principal e decisão em aberto. Exemplos e diagramas são conferidos contra a especificação, com atenção a identidade, sentinela, operações, consultas, eventos e ambiente.

A skill OpenAI Docs foi consultada conforme solicitação. Sua orientação para tarefas genéricas de software permite executar esta missão diretamente; a fonte normativa do Tag-File permanece o prompt local, sem inserir requisitos de produtos OpenAI.

<a id="doc-04"></a>

## DOC-04 — Entrega e uso pela equipe

O [relatório de entrega](relatorio-entrega.md) lista os arquivos efetivamente escritos e as verificações documentais realizadas. Os 24 cenários de aceite são previstos para validação futura e não foram executados. Cobertura documental não significa aplicação pronta ou todas as decisões resolvidas.

Para uma dúvida específica, parta do UC correspondente e siga seus links de origem. Para dividir responsabilidades de construção, consulte a orientação da arquitetura. Para decidir contratos ainda abertos, use as perguntas de [P-01](decisoes-e-pendencias.md#p-01) a [P-13](decisoes-e-pendencias.md#p-13) sem converter exemplos em aprovação.
