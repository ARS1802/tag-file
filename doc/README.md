# Tag-File

O Tag-File será um aplicativo desktop para navegar pelos arquivos da máquina, organizá-los com etiquetas e encontrá-los por essas classificações. O projeto usa **Java, Swing e MySQL** para estudar orientação a objetos e padrões de projeto.

<a id="doc-01"></a>

## Situação do projeto

As funcionalidades ainda não foram implementadas. Classes, tabelas e scripts descritos representam o desenho a construir. Exemplos explicam esse desenho; escolhas ainda abertas estão em [decisões e pendências](decisoes-e-pendencias.md).

<a id="doc-02"></a>

## Por onde começar

| Ordem | Documento | O que explica |
|---|---|---|
| 1 | [Visão geral](visao-geral.md) | O funcionamento do aplicativo, o escopo e as tecnologias. |
| 2 | [Modelo de domínio](modelo-de-dominio.md) | Arquivo físico, cadastro, Tag e relações entre objetos. |
| 3 | [Interface e fluxos](interface-e-fluxos.md) | O que aparece nas duas visões e como o usuário interage. |
| 4 | [Arquitetura e padrões](arquitetura-e-padroes.md) | Responsabilidades, chamadas entre classes, Observer e DAO. |

Para quem construirá a UI, a [explicação do Observer](arquitetura-e-padroes.md#arq-06) relaciona os avisos à atualização das apresentações. A [ordem de construção](arquitetura-e-padroes.md#orientacao-construcao) reúne os próximos passos de implementação.

## Documentos de consulta

| Documento | Quando usar |
|---|---|
| [Requisitos e regras](requisitos-e-regras.md) | Para conferir o comportamento de uma função e seus limites. |
| [Casos de uso](casos-de-uso.md) | Para acompanhar uma ação do usuário do início ao resultado. |
| [Banco de dados](banco-de-dados.md) | Para entender tabelas, associações, identidade e persistência. |
| [Instalação e execução](instalacao-e-execucao.md) | Para preparar a instância local no Windows, Ubuntu e Linux Mint. |
| [Critérios de aceite](criterios-de-aceite.md) | Para saber como conferir uma funcionalidade implementada. |
| [Decisões e pendências](decisoes-e-pendencias.md) | Para localizar contratos e comportamentos que ainda exigem uma escolha. |

<a id="doc-03"></a>

## Como ler as referências

Identificadores como UC-05 e ACE-02 ligam uma ação à sua verificação. Cada caso de uso aponta para as regras e os critérios relacionados. Eles são atalhos de consulta; você não precisa decorá-los.

Uma **pendência** indica algo ainda não decidido, não apenas código por escrever. Um **exemplo** ajuda a explicar, mas não fixa uma assinatura de método nem resolve uma pendência.

<a id="doc-04"></a>

## Como trabalhar em equipe

Antes de dividir uma funcionalidade, expliquem qual ação a inicia, quais objetos colaboram e o que muda na tela, no banco e no disco. Combinem os contratos afetados e usem os critérios de aceite para conferir o resultado.

Quando uma decisão mudar, atualizem a regra, o caso de uso, os dados/contratos envolvidos e o critério correspondente. As 13 pendências estão centralizadas em sua página; a retirada de Command permanece encerrada em [DEC-01](decisoes-e-pendencias.md#dec-01).
