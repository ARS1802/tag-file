# Tag-File

O Tag-File é um aplicativo desktop didático para navegar pelos arquivos da máquina, organizá-los com etiquetas e encontrá-los por essas classificações. O projeto usa **Java, Swing e MySQL** para estudar orientação a objetos e padrões de projeto.

<a id="doc-01"></a>

## Situação do projeto

Os tipos do desenho, a interface, o schema e os scripts estão implementados. `Main` inicia a sessão por `application.Application`; a janela e os componentes visuais ficam em `src/GUI`. Consulte [compilação e organização do código](implementacao.md), [decisões desta implementação](decisoes-implementacao.md) e a [matriz histórica de verificação](verificacao.md). A execução dos scripts Windows continua não verificada. As páginas de desenho ajudam a entender os requisitos, e o registro da implementação fixa os detalhes antes abertos.

<a id="doc-02"></a>

## Por onde começar

| Ordem | Documento | O que explica |
|---|---|---|
| 1 | [Visão geral](visao-geral.md) | O funcionamento do aplicativo, o escopo e as tecnologias. |
| 2 | [Modelo de domínio](modelo-de-dominio.md) | Arquivo físico, cadastro, Tag e relações entre objetos. |
| 3 | [Interface e fluxos](interface-e-fluxos.md) | O que aparece nas duas visões e como o usuário interage. |
| 4 | [Arquitetura e padrões](arquitetura-e-padroes.md) | Responsabilidades, chamadas entre classes, Observer e DAO. |

Para quem construirá a UI, a [explicação do Observer](arquitetura-e-padroes.md#arq-06) relaciona os avisos à atualização das apresentações. A [ordem de construção](arquitetura-e-padroes.md#orientacao-construcao) orienta a leitura das dependências da implementação.

## Documentos de consulta

| Documento | Quando usar |
|---|---|
| [Requisitos e regras](requisitos-e-regras.md) | Para conferir o comportamento de uma função e seus limites. |
| [Casos de uso](casos-de-uso.md) | Para acompanhar uma ação do usuário do início ao resultado. |
| [Banco de dados](banco-de-dados.md) | Para entender tabelas, associações, identidade e persistência. |
| [Instalação e execução](instalacao-e-execucao.md) | Para preparar a instância local no Windows, Ubuntu e Linux Mint. |
| [Critérios de aceite](criterios-de-aceite.md) | Para saber como conferir uma funcionalidade implementada. |
| [Decisões e pendências](decisoes-e-pendencias.md) | Para localizar DEC-01/DEC-02 e os contratos P-01 a P-13 resolvidos nesta versão. |

<a id="doc-03"></a>

## Como ler as referências

Identificadores como UC-05 e ACE-02 ligam uma ação à sua verificação. Cada caso de uso aponta para as regras e os critérios relacionados. Eles são atalhos de consulta; você não precisa decorá-los.

Uma **pendência** indica algo ainda não decidido, não apenas código por escrever. Um **exemplo** ajuda a explicar, mas não fixa uma assinatura de método nem resolve uma pendência.

<a id="doc-04"></a>

## Como trabalhar em equipe

Antes de dividir uma funcionalidade, expliquem qual ação a inicia, quais objetos colaboram e o que muda na tela, no banco e no disco. Combinem os contratos afetados e usem os critérios de aceite para conferir o resultado.

Quando uma decisão mudar, atualizem a regra, o caso de uso, os dados/contratos envolvidos e o critério correspondente. Os 13 grupos de contratos e suas decisões estão centralizados em sua página. As decisões encerradas são a retirada de Command em [DEC-01](decisoes-e-pendencias.md#dec-01) e a execução de apenas uma ação por vez, sem fila, em [DEC-02](decisoes-e-pendencias.md#dec-02).

- [Preparação portátil no Windows, pacote offline e diagnóstico](windows-portatil.md)
