# Diagramas do Tag-File

Comece pelos quatro diagramas principais. Os fluxos maiores possuem detalhes separados para permitir acompanhar uma ação por vez. Cada arquivo `.mmd` é editável e tem uma versão `.svg` para abrir e ampliar no navegador.

Os desenhos representam a **especificação planejada**, conforme [doc/README.md:L7-L9](../README.md#doc-01). As fontes e os trechos que sustentam cada desenho estão nos comentários `%%` de seu arquivo Mermaid. As relações descrevem responsabilidades documentadas; mensagens de sequência não estabelecem assinaturas Java definitivas.

| Tipo solicitado | Visualização | Fonte Mermaid | O que observar |
|---|---|---|---|
| DER — entidades | [Abrir DER](svg/01-der.svg) | [01-der.mmd](01-der.mmd) | Cardinalidade entre cadastro e etiqueta; referência ao arquivo físico. |
| MER — tabelas e atributos | [Abrir MER](svg/02-mer.svg) | [02-mer.mmd](02-mer.mmd) | Quatro estruturas relacionais, todos os dados documentados e referências entre elas. |
| Fluxo de usuário | [Abrir visão geral](svg/03-fluxo-usuario.svg) | [03-fluxo-usuario.mmd](03-fluxo-usuario.mmd) | Pontos de entrada para navegação, classificação, operações físicas e atualização. |
| Fluxo de classes | [Abrir relações centrais](svg/04-fluxo-classes.svg) | [04-fluxo-classes.mmd](04-fluxo-classes.mmd) | Quem chama quem; as sequências abaixo detalham o comportamento no tempo. |

## Leitura dos modelos de dados

A nomenclatura segue a separação solicitada: **DER** apresenta os conceitos e **MER** detalha o modelo relacional. O DER mostra `0..N` nos dois lados da classificação. A regra da última etiqueta é uma regra adicional de ciclo de vida: durante a reorganização, o cadastro recebe Etiqueta Ausente, e cadastros sem vínculos são tratados defensivamente na inicialização. [doc/modelo-de-dominio.md:L43-L50](../modelo-de-dominio.md#dom-01), [doc/requisitos-e-regras.md:L89-L124](../requisitos-e-regras.md#cic-01).

No MER, `PK` identifica a entidade, `FK` referencia outra entidade e `UK` indica unicidade lógica. `UUID`, `TEXTO`, `NUMERO` e `LOGICO` expressam o tipo da informação; **não são uma escolha de DDL MySQL**. `DATETIME` é o tipo temporal já definido. Os nomes de campos que não foram fixados nos documentos são rótulos de leitura. [doc/banco-de-dados.md:L59-L88](../banco-de-dados.md#sql-02).

| Estrutura | Atributos apresentados | Fonte |
|---|---|---|
| `LOCAL_FILE` | `id`, `path`, `name`, `extension`, `size`, `available`, `createdAt`, `modifiedAt`, `lastAccessedAt` | [doc/modelo-de-dominio.md:L70-L84](../modelo-de-dominio.md#dom-03) |
| `TAG` | `id`, `name`, `color`, `createdAt`, `lastFileTaggedAt` | [doc/modelo-de-dominio.md:L88-L100](../modelo-de-dominio.md#dom-04) |
| `LOCAL_FILE_TAG` | `localFileId`, `tagId` — referências aos dois cadastros associados | [doc/banco-de-dados.md:L34-L42](../banco-de-dados.md#sql-01) |
| `EXTENSOES_DA_TAG` | `tagId`, `extension` — o par é único; cada atributo isolado pode repetir | [doc/banco-de-dados.md:L44-L69](../banco-de-dados.md#sql-02) |

As informações do domínio também estão cobertas quando não viram uma coluna escalar: `nativeFile` fornece a referência de caminho representada por `path`; a coleção de Tags é representada por `LOCAL_FILE_TAG`; a coleção de extensões fica em `EXTENSOES_DA_TAG`. A quantidade de disponíveis é **calculada por consulta**, aparece como informação derivada no DER e não é uma coluna de `TAG`. NativeFile e NativeDirectory não exigem tabelas próprias. [doc/modelo-de-dominio.md:L13-L19](../modelo-de-dominio.md#dom-01), [doc/modelo-de-dominio.md:L70-L100](../modelo-de-dominio.md#dom-03), [doc/banco-de-dados.md:L118-L124](../banco-de-dados.md#sql-06).

**[NEEDS INVESTIGATION] P-06/P-07:** `name` e `extension` constam no inventário porque são informações necessárias, mas persistir cada uma em coluna própria ainda depende de decisão. Também estão abertos os tipos físicos, nulabilidade, índices, chaves das tabelas de vínculo, cascatas, nome definitivo da tabela de extensões e identificação técnica da sentinela. Os diagramas não acrescentam um campo de sistema nem escolhem uma chave composta. [doc/decisoes-e-pendencias.md:L80-L98](../decisoes-e-pendencias.md#p-06).

## Percursos do usuário

Retângulos duplos indicam um percurso detalhado em outro arquivo. Losangos indicam escolhas ou condições; verde marca resultados, vermelho destaca falhas ou exclusões físicas e amarelo também sinaliza decisões pendentes. As divisões organizam a documentação, sem definir posições de botões ou um novo menu da aplicação. A composição visual permanece nos limites de [P-04](../decisoes-e-pendencias.md#p-04) e [P-12](../decisoes-e-pendencias.md#p-12).

| Percurso | Mermaid | Imagem | Cobertura e fonte |
|---|---|---|---|
| Preparar e abrir | [01-inicializacao](fluxo-usuario/01-inicializacao.mmd) | [SVG](svg/fluxo-usuario/01-inicializacao.svg) | [UC-01](../casos-de-uso.md#uc-01): primeira preparação, dados existentes, autorização e limpeza. |
| Navegar e pesquisar | [02-navegacao](fluxo-usuario/02-navegacao.mmd) | [SVG](svg/fluxo-usuario/02-navegacao.svg) | [UC-02](../casos-de-uso.md#uc-02), [UC-03](../casos-de-uso.md#uc-03): arquivos locais, Tags, AND/OR e filtros. |
| Criar etiqueta | [03-criacao-tag](fluxo-usuario/03-criacao-tag.mmd) | [SVG](svg/fluxo-usuario/03-criacao-tag.svg) | [UC-04](../casos-de-uso.md#uc-04): nome repetido, extensões e seleção inicial opcional. |
| Associar arquivo | [04-associacao](fluxo-usuario/04-associacao.mmd) | [SVG](svg/fluxo-usuario/04-associacao.svg) | [UC-05](../casos-de-uso.md#uc-05): Drop/seleção, incompatibilidade, sentinela e sugestão. |
| Reorganizar | [05-reorganizacao](fluxo-usuario/05-reorganizacao.mmd) | [SVG](svg/fluxo-usuario/05-reorganizacao.svg) | [UC-06](../casos-de-uso.md#uc-06), [UC-07](../casos-de-uso.md#uc-07): desassociação, edição e última Tag normal. |
| Copiar, recortar, mover e colar | [06-clipboard](fluxo-usuario/06-clipboard.mmd) | [SVG](svg/fluxo-usuario/06-clipboard.svg) | [UC-08](../casos-de-uso.md#uc-08), [UC-09](../casos-de-uso.md#uc-09), [OP-03](../requisitos-e-regras.md#op-03): intenção, destino e preservação de identidade. |
| Renomear | [07-renomeacao](fluxo-usuario/07-renomeacao.mmd) | [SVG](svg/fluxo-usuario/07-renomeacao.svg) | [UC-10](../casos-de-uso.md#uc-10): nova extensão, Tags incompatíveis e cancelamento. |
| Disponibilidade e conflitos | [08-disponibilidade-e-conflitos](fluxo-usuario/08-disponibilidade-e-conflitos.mmd) | [SVG](svg/fluxo-usuario/08-disponibilidade-e-conflitos.svg) | [UC-11](../casos-de-uso.md#uc-11), [OP-06](../requisitos-e-regras.md#op-06): localizar, remover referência, substituir ou manter ambos. |
| Excluir e encontrar vazias | [09-exclusao](fluxo-usuario/09-exclusao.mmd) | [SVG](svg/fluxo-usuario/09-exclusao.svg) | [UC-12](../casos-de-uso.md#uc-12), [UC-13](../casos-de-uso.md#uc-13): três consequências e proteção de Etiqueta Ausente. |
| Refresh compartilhado | [10-refresh](fluxo-usuario/10-refresh.mmd) | [SVG](svg/fluxo-usuario/10-refresh.svg) | [UC-14](../casos-de-uso.md#uc-14): uma atualização por solicitação, sem limpeza de cadastros. |
| Falhas | [11-falhas](fluxo-usuario/11-falhas.mmd) | [SVG](svg/fluxo-usuario/11-falhas.svg) | [UC-15](../casos-de-uso.md#uc-15): resultado parcial, detalhes e saída de Loading. |

O percurso de falhas se aplica a todas as etapas falíveis, mesmo quando o desenho de detalhe apresenta somente o caminho nominal. Cancelar impede a ação ainda não confirmada; não desfaz uma etapa concluída. A ordem completa de confirmações e escritas da cópia continua aberta, assim como o resultado final da modalidade 2 de exclusão. [doc/requisitos-e-regras.md:L194-L196](../requisitos-e-regras.md#op-07), [doc/decisoes-e-pendencias.md:L32-L48](../decisoes-e-pendencias.md#p-01).

## Colaboração entre classes

O diagrama principal apresenta as dependências mais relevantes; ele não pretende ser uma lista de todos os atributos e métodos Java. As sequências abaixo mostram chamadas ao longo do tempo. Os participantes “Screen”, “Controller da visão”, “objetos ExplorerListener”, “DAOs” e “Inicialização da aplicação” são papéis ou grupos explicitamente indicados, não novas classes obrigatórias.

| Detalhe | Mermaid | Imagem | Fonte principal |
|---|---|---|---|
| Associar arquivo e avisar a interface | [01-associacao](fluxo-classes/01-associacao.mmd) | [SVG](svg/fluxo-classes/01-associacao.svg) | [doc/arquitetura-e-padroes.md:L176-L180](../arquitetura-e-padroes.md#percurso-associacao) |
| Recortar em uma visão e colar na outra | [02-recortar-colar](fluxo-classes/02-recortar-colar.mmd) | [SVG](svg/fluxo-classes/02-recortar-colar.svg) | [doc/arquitetura-e-padroes.md:L184-L190](../arquitetura-e-padroes.md#percurso-movimentacao) |
| Refresh e Observer | [03-refresh-observer](fluxo-classes/03-refresh-observer.mmd) | [SVG](svg/fluxo-classes/03-refresh-observer.svg) | [doc/arquitetura-e-padroes.md:L194-L204](../arquitetura-e-padroes.md#percurso-refresh) |
| Preparação do banco e da sessão | [04-ambiente](fluxo-classes/04-ambiente.mmd) | [SVG](svg/fluxo-classes/04-ambiente.svg) | [doc/instalacao-e-execucao.md:L111-L168](../instalacao-e-execucao.md#amb-04) |
| Factory, composição, DAO e filtros | [05-contratos-e-criacao](fluxo-classes/05-contratos-e-criacao.mmd) | [SVG](svg/fluxo-classes/05-contratos-e-criacao.svg) | [doc/arquitetura-e-padroes.md:L101-L138](../arquitetura-e-padroes.md#arq-02), [doc/interface-e-fluxos.md:L63-L96](../interface-e-fluxos.md#exp-02) |

`refreshAll`, `cleanupOrphans`, `onFileChanged`, `onTagChanged` e `onFileTagsChanged` são nomes de referência da documentação, e os parênteses no desenho não fixam métodos sem parâmetros. A proposta de Screens observadoras e os dados enviados nos avisos dependem de P-04/P-05. As chamadas diretas entre Controllers e colaboradores seguem a decisão encerrada DEC-01. [doc/arquitetura-e-padroes.md:L52-L56](../arquitetura-e-padroes.md#arq-06), [doc/arquitetura-e-padroes.md:L95-L97](../arquitetura-e-padroes.md#arq-07), [doc/decisoes-e-pendencias.md:L22-L28](../decisoes-e-pendencias.md#dec-01).

## Verificação da entrega

Os 20 arquivos Mermaid foram renderizados com sucesso e suas prévias foram inspecionadas visualmente. As 20 imagens SVG correspondem às fontes finais. Foram conferidos os 82 links locais deste índice, os intervalos das citações nos desenhos, a presença dos 15 casos de uso e o inventário de 18 campos apresentados no MER, com as informações relacionais e derivadas explicadas acima.
