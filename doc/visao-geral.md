x'# Visão geral do Tag-File

[Índice](README.md) · [Modelo de domínio](modelo-de-dominio.md) · [Requisitos](requisitos-e-regras.md)

<a id="obj-01"></a>

## O que o aplicativo faz

O Tag-File organiza arquivos locais por etiquetas. Um arquivo pode receber várias classificações e ser encontrado por elas, independentemente da pasta em que esteja.

Imagine uma pasta com estes arquivos:

```text
Documentos/
├── prova.pdf
├── trabalho.docx
└── foto.png
```

O usuário cria a etiqueta Faculdade e a associa a prova.pdf e trabalho.docx. Também pode marcar prova.pdf como Importante:

```text
prova.pdf      [Faculdade] [Importante]
trabalho.docx  [Faculdade]
foto.png
```

O conteúdo de cada arquivo continua no disco. O MySQL guarda os cadastros, metadados, etiquetas e associações necessários para recuperar a classificação em outra execução. Associar uma Tag não cria uma cópia do arquivo.

## As duas formas de navegar

| Visão | Ponto de partida | O que aparece |
|---|---|---|
| Arquivos Local | Uma pasta da máquina. | Arquivos físicos, inclusive os ainda não cadastrados, com suas Tags quando existirem. |
| Arquivos por Etiqueta / Arquivos por Tag | As etiquetas selecionadas. | Arquivos cadastrados que atendem à busca por Tags. |

Ao associar Faculdade a prova.pdf na visão local, o usuário passa a poder encontrá-lo pela etiqueta. Os dois exploradores usam as mesmas classificações e compartilham as operações pertinentes.

Mostrar um arquivo na pasta não o cadastra automaticamente. A primeira associação exige procurar o cadastro existente e reutilizá-lo ou criar o necessário. O [modelo de domínio](modelo-de-dominio.md) explica a relação entre o arquivo representado por NativeFile, o cadastro LocalFile e a Tag.

<a id="obj-02"></a>

## Tecnologias e objetivo de estudo

O projeto será usado em disciplinas de Ciência da Computação para praticar POO e padrões de projeto. A equipe está aprendendo esses fundamentos. As tecnologias definidas são **Java, Swing e MySQL**, com **JDK superior à versão 21**.

O aplicativo terá suporte a Windows, Ubuntu e Linux Mint. Observer, DAO, Factory simples e responsabilidades GRASP estão explicados na [arquitetura](arquitetura-e-padroes.md#arq-09). Ainda não há uma rubrica acadêmica com quantidade mínima de padrões ou diagramas obrigatórios.

<a id="obj-03"></a>

## Escopo da versão

| Área | Funcionalidades |
|---|---|
| Navegação | Explorar pastas e arquivos locais. |
| Classificação | Criar e editar Tags, configurar nome/cor/extensões, associar e desassociar arquivos. |
| Consulta | Encontrar arquivos por Tags com AND/OR, filtrar por informações físicas e localizar Tags vazias. |
| Operações físicas | Mover, copiar, recortar, colar, renomear e excluir arquivos, com as confirmações previstas. |
| Interface | Mostrar etiquetas junto dos arquivos e comunicar alterações às apresentações interessadas. |
| Atualização | Conferir disponibilidade e metadados nos momentos definidos e permitir localizar novamente arquivos ausentes. |
| Persistência e ambiente | Guardar dados no MySQL local e administrar a instância da aplicação por scripts de plataforma. |

Diretórios servem à navegação e aos destinos das operações. Não haverá classificação de diretórios, operações recursivas sobre árvores de pastas ou importação automática de subpastas. Também não há nuvem, arquivos remotos, contas de usuários do aplicativo ou armazenamento do conteúdo dos arquivos no SQL.

<a id="restricoes"></a>

## Limites da versão

| Tema | Decisão |
|---|---|
| Dependências | Sem Maven ou infraestrutura adicional por convenção. A referência é Connector/J manual, sem outro gerenciador, framework ou ORM. |
| Conexão | DatabaseConnection compartilhada, sem pool. |
| Transações | Sem controle explícito de transações; operações podem terminar parcialmente. |
| Coordenação | Controllers chamam Managers/Services diretamente. A retirada de Command está encerrada em [DEC-01](decisoes-e-pendencias.md#dec-01). |
| Execução | Apenas uma ação por vez em toda a aplicação, compartilhada pelos dois exploradores. Novas ações são bloqueadas, sem fila ou sequências armazenadas; a UI permanece responsiva. [DEC-02](decisoes-e-pendencias.md#dec-02). |
| Undo/Redo | Fora da implementação atual; evolução apenas comentada. |
| Clipboard | Compartilhado internamente; integração com o sistema operacional apenas comentada como evolução. |
| Monitoramento | Sem vigilância contínua do sistema de arquivos. |
| Eventos | Sem classe ExplorerEvent e sem enum de eventos nesta versão; evolução apenas comentada. |
| Banco | Sem schema_history e sem contador persistido de disponíveis em Tag. |
| Criação de objetos | Uma Factory para Tag e LocalFile; nenhuma Factory para NativeFile/NativeDirectory. |
| Busca | AND/OR, sem NOT. |

As limitações de transações e reconexão exigem informar corretamente os resultados parciais. Os detalhes estão em [requisitos](requisitos-e-regras.md#err-01), [banco](banco-de-dados.md#sql-05) e [ambiente](instalacao-e-execucao.md#amb-05).

## Vocabulário de apoio

| Termo | Significado no projeto |
|---|---|
| Entidade | Conceito com identidade própria, como LocalFile ou Tag. |
| Metadados | Informações sobre o arquivo, como tamanho, caminho e datas. |
| Persistir | Gravar dados para recuperá-los em outra execução. |
| Associação | Vínculo entre um cadastro de arquivo e uma Tag. |
| UUID | Identificador do cadastro ou da etiqueta. |
| Disponibilidade | Estado conhecido do arquivo no caminho registrado. |
| Sentinela | Papel da Tag protegida Etiqueta Ausente durante a reorganização de um cadastro. |
| Refresh | Atualização de disponibilidade/metadados, separada da limpeza de inicialização. |
| Contrato de método | Combinação de entradas, resultado e erros possíveis de um método. |

As escolhas desta versão estão em [decisões da implementação](decisoes-implementacao.md).
