# Visão geral do Tag-File

[Índice](../README.md) · [Regras](requisitos-e-regras.md) · [Rastreabilidade](rastreabilidade.md)

> **Situação:** especificação planejada para um projeto acadêmico. A inspeção em 13/09/2026 encontrou apenas o programa inicial da IDE em [src/Main.java](../src/Main.java), que imprime uma saudação e os números de 1 a 5. Os fluxos do Tag-File descritos aqui ainda não têm implementação correspondente no repositório inspecionado.

<a id="obj-01"></a>

## OBJ-01 — Problema e finalidade

**Estado: confirmada.**

O Tag-File é uma aplicação desktop para organizar e consultar arquivos locais por etiquetas. Um mesmo arquivo pode receber classificações diferentes e ser encontrado por essas classificações, sem depender apenas da hierarquia física de pastas.

Exemplo do próprio domínio:

```text
prova.pdf
Tags: Faculdade, PDF, Importante
```

O usuário pode encontrá-lo pela etiqueta `Faculdade`, ainda que o arquivo esteja fisicamente em outra pasta. As etiquetas não são novas cópias do arquivo nem obrigam a armazenar seu conteúdo no MySQL.

O aplicativo oferece uma visão dos arquivos classificados e uma visão do sistema de arquivos real. Essas visões são interoperáveis.

<a id="obj-02"></a>

## OBJ-02 — Contexto e tecnologias

**Estado: confirmada.**

O projeto é acadêmico, de Ciência da Computação, voltado a uma disciplina de Design Patterns. As tecnologias escolhidas são **Java, Swing e MySQL**. A equipe é composta por estudantes com pouco tempo para implementação e aprendizado de infraestrutura adicional.

A documentação permite que a equipe compreenda o projeto e diferencie o que pretende construir do que já construiu. Os conceitos são explicados com exemplos do Tag-File, sem introduzir padrões apenas para aumentar a quantidade de nomes citados.

GoF, GRASP, DAO e CRUD têm papéis distintos. Nem toda classe é um padrão de projeto. Uma Factory simples não se torna Factory Method ou Abstract Factory apenas por ter “Factory” no nome.

Não foi fornecida uma rubrica formal com quantidade mínima de padrões, pontuação, data exata de entrega ou diagramas obrigatórios. Esta documentação não acrescenta esses critérios.

<a id="obj-03"></a>

## OBJ-03 — Evolução do escopo

**Estado: confirmada.**

A ideia inicial de apenas registrar etiquetas, sem modificar arquivos físicos, foi substituída pela inclusão de operações de mover, copiar, recortar, colar, renomear e excluir arquivos. Também foi incluída a administração de uma instância local MySQL pelo próprio aplicativo, com scripts e configurações em `database`.

A navegação por pastas foi incluída. Isso não equivale a aprovar operações recursivas sobre pastas, etiquetar diretórios, copiar ou excluir árvores de diretórios ou importar automaticamente todo o conteúdo de subpastas.

Não há requisito de armazenar o conteúdo dos arquivos no MySQL, criar uma biblioteca que renomeie internamente todos os arquivos, sincronizar com nuvem, gerenciar arquivos remotos ou adicionar contas de usuários do aplicativo.

## Limites conscientes da versão

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

## Glossário de leitura

| Termo | Significado no projeto |
|---|---|
| Arquivo físico | Conteúdo presente no sistema de arquivos, fora do banco. |
| `NativeFile` / `NativeDirectory` | Representações de um arquivo e de uma pasta por `Path`. Não implicam cadastro no SQL. |
| `LocalFile` | Registro conhecido pelo Tag-File; possui UUID, metadados, referência nativa e associações. |
| `Tag` / etiqueta | Classificação por identidade própria. O nome pode repetir mediante confirmação. |
| UUID | Identificador do registro. Um movimento muda o caminho, preservando o UUID. |
| Associação / join-table | Vínculo entre IDs de arquivo e Tag, persistido em `LOCAL_FILE_TAG`. |
| Sentinela | Papel de `Etiqueta Ausente`: classificação temporária durante reorganização; a Tag em si permanece. |
| Disponível | Arquivo conhecido como encontrado no caminho registrado. Não é sinônimo de classificado. |
| Tag vazia | Tag sem associações. Ter zero arquivos disponíveis não basta para estar vazia. |
| Filtro | Critérios da consulta; a seleção é o conjunto escolhido pelo usuário e o Command representa a ação. |
| Refresh | Atualiza disponibilidade e metadados; não faz a limpeza de registros sem classificação. |
| DAO / CRUD | DAO concentra acesso aos dados; CRUD designa criar, consultar, atualizar e excluir. |
| GoF / GRASP | GoF reúne padrões de projeto; GRASP orienta a distribuição de responsabilidades. A aplicação concreta está em [arquitetura](arquitetura-e-padroes.md#arq-09). |

As definições normativas e os atributos estão em [DOM-01 a DOM-04](modelo-de-dominio.md#dom-01). Operações que retiram uma associação, removem um registro ou apagam um arquivo físico têm consequências diferentes; veja [DEL-01 a DEL-04](requisitos-e-regras.md#del-01).

## Exemplo que orienta a leitura

`prova.pdf` pode receber `Faculdade`, `PDF` e `Importante` sem gerar três cópias físicas. Ao mover esse arquivo pelo aplicativo, seu UUID e as Tags permanecem, e o caminho muda. Se outra ferramenta mover o arquivo, o próximo Refresh poderá detectar indisponibilidade; a relocalização depende do usuário. Na cópia, a origem física permanece, mas a identidade após substituição e o cadastro de cópias sem Tags continuam em [P-01](decisoes-e-pendencias.md#p-01).

## Evolução do projeto e escopo de estudo

O histórico relevante para esta documentação é o das decisões consolidadas: a ideia de só etiquetar passou a incluir operações físicas e administração de uma instância local MySQL. Não há evidência de uma migração de ferramentas, atualização de bibliotecas ou implementação anterior a 2025 neste repositório. A configuração observada de JDK 24 é registrada em [AMB-06](instalacao-e-execucao.md#amb-06), sem transformá-la em recomendação de atualização.

Para aprofundar, a equipe pode estudar identidade versus localização, composição de objetos, relações muitos-para-muitos, Command/Observer e limites de consistência entre disco e banco. Isso não amplia o escopo da versão nem cria critérios adicionais atribuídos ao professor.
