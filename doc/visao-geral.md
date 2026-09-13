# Visão geral do Tag-File

[Índice](README.md) · [Requisitos](requisitos-e-regras.md) · [Orientação de construção](arquitetura-e-padroes.md#orientacao-construcao)

**A documentação precede a implementação.** O objetivo é dar à equipe uma referência para compreender, discutir e construir o projeto. As funcionalidades abaixo são requisitos aprovados; as decisões abertas são indicadas por [P-01](decisoes-e-pendencias.md#p-01) a [P-13](decisoes-e-pendencias.md#p-13). A falta esperada de código não é uma pendência de modelagem.

<a id="obj-01"></a>

## OBJ-01 — Problema e finalidade

**Estado: confirmada.**

O Tag-File será uma aplicação desktop para organizar e consultar arquivos locais por etiquetas. Um mesmo arquivo pode receber classificações diferentes e ser encontrado por essas classificações, sem depender apenas da hierarquia física de pastas.

Exemplo do próprio domínio:

```text
prova.pdf
Tags: Faculdade, PDF, Importante
```

O usuário pode encontrá-lo pela etiqueta `Faculdade`, ainda que o arquivo esteja fisicamente em outra pasta. As etiquetas não são novas cópias do arquivo nem obrigam a armazenar seu conteúdo no MySQL.

O aplicativo deverá oferecer uma visão dos arquivos classificados e uma visão do sistema de arquivos real. Essas visões deverão ser interoperáveis.

<a id="obj-02"></a>

## OBJ-02 — Contexto e tecnologias

**Estado: confirmada.**

O projeto é acadêmico, de Ciência da Computação, voltado a uma disciplina de Design Patterns. As tecnologias escolhidas são **Java, Swing e MySQL**. A equipe é composta por estudantes com pouco tempo para implementação e aprendizado de infraestrutura adicional.

A documentação precisa permitir que a equipe compreenda o que deverá construir, por que cada parte existe e como as responsabilidades colaboram. Ela também será a referência para explicar o projeto ao professor antes e durante o desenvolvimento. As explicações usam o próprio Tag-File e não acrescentam padrões apenas para aumentar a quantidade de nomes citados.

GoF, GRASP, DAO e CRUD têm papéis distintos. Nem toda classe é um padrão de projeto. Uma Factory simples não se torna Factory Method ou Abstract Factory apenas por ter “Factory” no nome.

Não foi fornecida uma rubrica formal com quantidade mínima de padrões, pontuação, data exata de entrega ou diagramas obrigatórios. Esses critérios não são acrescentados à documentação.

<a id="obj-03"></a>

## OBJ-03 — Evolução do escopo

**Estado: confirmada.**

A ideia inicial de apenas registrar etiquetas, sem modificar arquivos físicos, foi substituída pela inclusão de operações de mover, copiar, recortar, colar, renomear e excluir arquivos. Também foi incluída a administração de uma instância local MySQL pelo próprio aplicativo, com scripts e configurações em `database`.

A navegação por pastas foi incluída. Isso não equivale a aprovar operações recursivas sobre pastas, etiquetar diretórios, copiar ou excluir árvores de diretórios ou importar automaticamente todo o conteúdo de subpastas.

Não há requisito de armazenar o conteúdo dos arquivos no MySQL, criar uma biblioteca que renomeie internamente todos os arquivos, sincronizar com nuvem, gerenciar arquivos remotos ou adicionar contas de usuários do aplicativo.

<a id="restricoes"></a>

## Restrições e limitações conhecidas

| Tema | Restrição aprovada |
|---|---|
| Maven | Não usar. |
| Pool de conexões | Fora do escopo. |
| Controle explícito de transações | Não será implementado nesta versão. |
| Undo/Redo | Não implementar; apenas comentar uma evolução futura. |
| Clipboard do sistema operacional | Não integrar; apenas comentar possibilidade futura. |
| Monitoramento contínuo de arquivos | Não implementar. |
| Classe `ExplorerEvent` | Não criar nesta versão; evolução apenas comentada. |
| `schema_history` | Não criar. |
| Factory para objetos Native | Não criar. |
| Contador persistido de arquivos disponíveis em Tag | Não manter. |
| Operador NOT na pesquisa por Tags | Não incluir. |
| Sofisticação extra | Não adicionar bibliotecas, camadas ou infraestrutura por convenção profissional. |

Essas restrições não autorizam afirmar garantias inexistentes. Falhas parciais, fragilidade de configuração administrativa e limitações de reconexão devem ser descritas.

A colaboração vigente das operações é a de [ARQ-03](arquitetura-e-padroes.md#arq-03), consolidada por [DEC-01](decisoes-e-pendencias.md#dec-01). O escopo funcional de mover, copiar, recortar, colar, renomear e excluir permanece aprovado. A simplificação da arquitetura não introduz novas camadas ou padrões compensatórios.

## Glossário para começar

| Termo | Significado no Tag-File |
|---|---|
| Arquivo físico | Conteúdo no sistema de arquivos; não é armazenado no MySQL por este projeto. |
| NativeFile / NativeDirectory | Representações de arquivo e diretório por Path. Não implicam cadastro SQL. |
| LocalFile | Registro conhecido pelo aplicativo, com UUID, metadados, referência nativa e Tags. |
| Tag / etiqueta | Classificação com UUID próprio, nome, cor e possíveis restrições de extensão. |
| UUID | Identificador do registro; não muda quando o arquivo é movido normalmente. |
| Associação / join-table | Vínculo entre IDs de arquivo e Tag persistido em LOCAL_FILE_TAG. |
| Sentinela | Papel de Etiqueta Ausente ao manter um registro acessível durante a reorganização. A associação é temporária; a Tag permanece. |
| Disponibilidade | Estado conhecido no caminho salvo. É independente de ter ou não Tags. |
| Filtro / seleção / operação | Critérios da busca / elementos escolhidos / ação sobre os elementos confirmados. |
| Refresh | Sincronização de disponibilidade/metadados, distinta da limpeza exclusiva da inicialização. |
| DAO / CRUD | DAO concentra acesso a dados; CRUD nomeia criar, consultar, atualizar e excluir. |
| GoF / GRASP | GoF reúne padrões de projeto; GRASP orienta a distribuição de responsabilidades. A aplicação concreta está em [ARQ-09](arquitetura-e-padroes.md#arq-09). |

## Exemplo que conecta o problema ao modelo

`prova.pdf` poderá ser encontrado por `Faculdade`, `PDF` ou `Importante`, sem três cópias físicas. Na visão `Arquivos Local`, um arquivo poderá aparecer sem registro; ao associar a primeira Tag, a equipe deverá reutilizar ou criar o LocalFile necessário. Mover o arquivo manterá UUID e Tags e atualizará o caminho. A cópia preservará a origem física; sua identidade em substituição e o cadastro da cópia sem Tags dependem de [P-01](decisoes-e-pendencias.md#p-01).

O projeto evoluiu de simples etiquetação para operações físicas e administração de uma instância local MySQL. As revisões relevantes estão em [decisões](decisoes-e-pendencias.md). O contexto do JDK 24 e do driver manual está em [AMB-06](instalacao-e-execucao.md#amb-06); não se presume uma migração anterior de bibliotecas ou versões não decididas.

## Sequência de estudo

1. Identidade versus localização e composição entre objetos ([DOM-01](modelo-de-dominio.md#dom-01) a [DOM-04](modelo-de-dominio.md#dom-04)).
2. Relações muitos-para-muitos e diferença entre associação, registro e disco ([SQL-01](banco-de-dados.md#sql-01) a [SQL-05](banco-de-dados.md#sql-05)).
3. Distribuição de responsabilidades e Observer ([ARQ-01](arquitetura-e-padroes.md#arq-01) a [ARQ-09](arquitetura-e-padroes.md#arq-09)).
4. Gatilhos de sincronização, falhas parciais e critérios de aceite (SYN, ERR, ACE).

Essa sequência é editorial, não uma rubrica, cronograma ou ampliação do escopo. Os percursos práticos estão em [orientação para a implementação futura](arquitetura-e-padroes.md#orientacao-construcao).
