# Requisitos e regras

[Índice](README.md) · [Casos de uso](casos-de-uso.md) · [Critérios de aceite](criterios-de-aceite.md)

Esta página é a referência do comportamento a implementar. Arquivo físico, cadastro LocalFile e associação com Tag têm efeitos distintos. As situações que ainda exigem escolha remetem a [P-01 a P-13](decisoes-e-pendencias.md).

<a id="tag-01"></a>

## Criar e editar etiquetas

O usuário pode criar e editar Tags com nome, cor hexadecimal e extensões permitidas. O nome pode repetir: a UI pede confirmação e permite outra Tag com outro UUID. Ações e associações identificam a Tag pelo UUID, não apenas pelo nome.

Comparação de nomes com diferenças de caixa/espaços, nome vazio, limites e formato completo da cor permanecem em [P-09](decisoes-e-pendencias.md#p-09). Editar extensões obedece à regra de compatibilidade abaixo.

<a id="tag-02"></a>

## Etiquetas predefinidas e proteção

As predefinidas são **Imagens, PDF, Audios, Videos e Etiqueta Ausente**. As quatro primeiras podem ser apagadas mediante confirmação reforçada. Etiqueta Ausente não pode ser apagada pelo usuário, mesmo vazia.

Exemplos de extensões, sem constituir um catálogo inicial fechado:

| Tag | Exemplos |
|---|---|
| Imagens | .jpg, .jpeg, .png, .gif, .webp |
| PDF | .pdf |
| Audios | .mp3, .wav, .flac, .aac |
| Videos | .mp4, .mkv, .avi, .mov |

A preparação inicial não recria automaticamente, em cada abertura, predefinidas que o usuário apagou. Identificação técnica, editabilidade especial e preparação permanecem em [P-07](decisoes-e-pendencias.md#p-07).

<a id="tag-03"></a>

## Etiquetas vazias e arquivos disponíveis

Tags podem existir vazias. Deve haver uma função para encontrá-las e permitir sua exclusão, preservando a proteção de Etiqueta Ausente.

**Vazia significa sem associações.** Uma Tag que só tem arquivos indisponíveis continua tendo associações, embora tenha zero disponíveis. A quantidade de associados com available = true é consultada no banco quando necessária; não existe contador persistido na Tag.

<a id="ext-01"></a>

## Extensões permitidas

Uma Tag aceita zero, uma ou várias extensões reais, inclusive específicas como .cdr. Sem extensões configuradas, aceita qualquer extensão.

```text
PDF → .pdf
.PDF → .pdf
jpeg → .jpeg
CDR → .cdr
```

As extensões são normalizadas em minúsculas e com ponto inicial. A combinação Tag/extensão não se repete; Tags diferentes podem aceitar a mesma extensão. Coleção vazia e nulabilidade da API ainda não estão totalmente definidas. Não há enumeração obrigatória de categorias IMAGE/AUDIO/VIDEO nem classe FileType obrigatória.

<a id="ext-02"></a>

## Associar um arquivo incompatível

Se uma Tag restrita não aceitar a extensão do arquivo, informar a Tag e a extensão e oferecer:

- Criar nova etiqueta.
- Adicionar a extensão à etiqueta atual.
- Cancelar.

A associação ou ampliação da restrição depende da escolha do usuário. Cancelar não autoriza nenhuma delas. O aviso é uma situação tratável, não uma falha irrecuperável.

Exemplo: foto.png não pode ser associada silenciosamente à Tag PDF que só aceita .pdf. Essa regra vale tanto no seletor de arquivos quanto no Drag and Drop.

<a id="ext-03"></a>

## Alterar extensões de uma Tag em uso

Avaliar a compatibilidade contra o **conjunto final** de extensões. Se arquivos associados ficarem incompatíveis, listá-los e pedir confirmação antes de retirar as extensões e associações afetadas.

Exemplo: Faculdade aceita .pdf e .docx. Ao passar a aceitar apenas .docx, prova.pdf perde sua associação com Faculdade somente após confirmação. As outras Tags e o arquivo físico são preservados. Se essa era a última Tag normal, aplica-se Etiqueta Ausente.

Remover a última extensão configurada deixa a Tag sem restrição. Passar de uma Tag livre para uma lista específica pode tornar arquivos incompatíveis e exige a mesma verificação. O tratamento detalhado por lote permanece aberto.

<a id="tag-04"></a>

## Sugerir uma etiqueta predefinida

Ao criar um LocalFile, apresentar a possibilidade de associar uma predefinida compatível. Por exemplo, ao registrar prova.pdf em Faculdade, oferecer também PDF.

Há uma opção equivalente a **Não perguntar novamente nesta sessão**. A supressão acaba na próxima inicialização; não é uma preferência permanente. O efeito nos próximos arquivos — apenas silenciar ou repetir a escolha — está em [P-03](decisoes-e-pendencias.md#p-03). A escolha entre várias predefinidas compatíveis também continua aberta.

Não existe classificação automática obrigatória geral nem reclassificação automática a cada mudança de extensão.

<a id="cic-01"></a>

## Primeira classificação e retirada da última Tag

Mostrar um arquivo em Arquivos Local não cria cadastro SQL. A primeira associação exige procurar LocalFile pelo caminho e reutilizar o existente ou criar o necessário.

Durante a reorganização, retirar a última Tag normal mantém o cadastro e associa Etiqueta Ausente:

```text
Faculdade, PDF → retirar Faculdade → PDF
PDF → retirar PDF → Etiqueta Ausente
Etiqueta Ausente → adicionar Importante → Importante
```

A sentinela mantém o arquivo acessível em Arquivos por Tag durante a sessão. Adicionar uma Tag normal retira apenas a associação com Etiqueta Ausente; a Tag protegida permanece.

<a id="cic-02"></a>

## Limpeza na inicialização

Ao abrir o programa, remover do banco os LocalFile:

1. Cuja única Tag seja Etiqueta Ausente.
2. Sem qualquer associação em LOCAL_FILE_TAG, como limpeza defensiva.

Remover também os vínculos pertinentes. Preservar os arquivos físicos e a Tag Etiqueta Ausente, que fica vazia após a limpeza. Estar indisponível, sozinho, não é motivo para excluir um cadastro.

O mecanismo de identificação da sentinela permanece em P-07. Como nomes podem repetir, identificar apenas pelo nome é insuficiente.

<a id="cic-03"></a>

## Quando a limpeza não acontece

Refresh, troca de aba, aplicação de filtros, navegação, reconexão e notificações do Observer não executam a limpeza da inicialização. Reconectar não inicia outra sessão de organização.

A relação dessa regra com remoções explícitas e a segunda modalidade de exclusão ainda depende de [P-02](decisoes-e-pendencias.md#p-02).

<a id="op-01"></a>

## Selecionar arquivos e associá-los

Formas previstas: seleção individual, Drop de arquivos sobre uma Tag e seleção de arquivos dentro de uma pasta durante a criação da Tag. JFileChooser e FileNameExtensionFilter são os componentes adotados.

Ao criar Faculdade com restrição .pdf, o usuário pode abrir uma pasta e escolher apenas prova.pdf entre os PDFs compatíveis. Não precisa classificar todos os arquivos encontrados. Para cada arquivo confirmado, reutilizar LocalFile pelo caminho ou criar o necessário pela Factory, persistindo a associação.

Seleção múltipla está definida nesse fluxo inicial; não há um comportamento geral fechado para todas as operações em lote. Diretórios não são etiquetáveis; navegar e selecionar uma pasta não importam automaticamente arquivos ou subpastas.

<a id="op-02"></a>

## Usar e localizar novamente um arquivo

Antes de usar um arquivo cadastrado em uma operação, verificar sua disponibilidade. Se estiver ausente do caminho registrado, oferecer **Localizar**, **Remover do Tag-File** ou **Cancelar**.

Localizar permite informar o caminho correspondente, preservando UUID e associações. Se esse caminho já pertencer a outro LocalFile, há uma colisão a resolver em P-08; não há mesclagem automática definida.

Remover do Tag-File preserva o conteúdo físico; seu alcance imediato deve ser resolvido com P-02. Abrir o arquivo na aplicação associada faz parte do fluxo de utilização, mas a API Java concreta ainda não foi escolhida.

<a id="op-03"></a>

## Mover, recortar e colar

Mover um arquivo cadastrado preserva LocalFile, UUID e Tags. Depois da movimentação física, o cadastro deve refletir o novo caminho.

Recortar guarda a intenção CUT no ClipboardService; não move imediatamente. Colar em uma pasta executa a movimentação. Exemplo: recortar prova.pdf em Arquivos por Tag e colar em Documentos/Faculdade na visão Arquivos Local.

Arquivos sem LocalFile também podem ser manipulados na visão local, sem cadastro artificial. Disco e banco são etapas sujeitas a falhas; não há garantia de que ambas concluam juntas.

<a id="op-04"></a>

## Copiar

Copiar cria outro arquivo físico e mantém a origem. A UI pergunta se a cópia receberá as Tags do original.

Dois arquivos registrados em caminhos distintos precisam ser distinguíveis. O UUID e as associações que sobrevivem na substituição de um destino já cadastrado estão em [P-01](decisoes-e-pendencias.md#p-01). Também está aberta a escolha entre cópia sem Tags apenas nativa ou cadastro temporário em Etiqueta Ausente.

Copiar não altera a origem como se fosse mover. Esses efeitos definidos não estabelecem a ordem completa das confirmações e das etapas de execução.

<a id="op-05"></a>

## Renomear e mudar extensão

A renomeação normal preserva a identidade do cadastro e atualiza sua localização. Se a nova extensão tornar Tags incompatíveis, oferecer antes de concluir:

- Remover as Tags incompatíveis.
- Adicionar a nova extensão às Tags incompatíveis.
- Cancelar.

As Tags compatíveis permanecem. Se a retirada deixar o arquivo sem Tag normal, aplica-se Etiqueta Ausente. Renomear a extensão não converte o conteúdo para outro formato.

<a id="op-06"></a>

## Conflitos de nome ou caminho

Oferecer **Substituir**, **Manter os dois** ou **Cancelar**, conforme aplicável.

| Alternativa | Efeito e limite |
|---|---|
| Substituir | Sobrescreve fisicamente o destino. Em cópia, UUID e associações dependem de P-01. |
| Manter os dois | Preserva os arquivos em caminhos distintos. prova (1).pdf é exemplo, não algoritmo final de nomes. |
| Cancelar | Não autoriza a ação conflitante ainda não executada. |

A matriz de alternativas por operação, especialmente renomeação, e conflitos apenas no SQL continuam abertos em P-08.

<a id="op-07"></a>

## Cancelamento e falha parcial

Cancelar antes da confirmação impede aquela ação. Não desfaz etapas já concluídas. Sem Undo/Redo, controle explícito de transações ou restauração garantida, uma falha posterior pode deixar um resultado parcial, comunicado conforme ERR-01.

<a id="op-08"></a>

## Clipboard interno

ClipboardService é compartilhado pelos exploradores e guarda a intenção COPY ou CUT usada na colagem. Deve atender arquivos com e sem cadastro, conservando a relação com LocalFile quando houver.

A representação interna, comportamento após colar, repetição de CUT, histórico e persistência entre execuções não estão definidos. [P-12](decisoes-e-pendencias.md#p-12) reúne esses contratos. A integração com o clipboard do sistema operacional é apenas uma evolução comentada.

<a id="op-09"></a>

## Uma ação por vez em toda a aplicação

**A aplicação permite apenas uma ação em andamento por vez.** O limite é compartilhado pelos dois exploradores: iniciar uma ação em Arquivos por Tag impede iniciar outra em Arquivos Local, e vice-versa. A decisão está registrada em [DEC-02](decisoes-e-pendencias.md#dec-02).

Uma ação é uma solicitação funcional, como associar uma seleção de arquivos, colar ou executar um Refresh global. Suas confirmações, consultas, etapas de disco e SQL e atualização da apresentação pertencem à mesma ação. A seleção múltipla já prevista na associação inicial continua permitida como uma única ação; esta regra não limita toda ação a um único arquivo nem resolve as demais políticas de lotes de P-12.

Antes de iniciar uma ação, verificar se outra está em andamento. Enquanto estiver ocupada, a aplicação bloqueia novas ações por botão, atalho, Drop ou pelo outro explorador. Solicitações automáticas e chamadas repetidas também respeitam esse limite, impedindo reentrância — iniciar novamente uma ação antes de a anterior terminar.

**Não há fila de ações nem montagem de sequências para executar depois.** Uma nova solicitação recebida durante outra ação não inicia trabalho, não fica pendente e não é executada automaticamente ao liberar a aplicação. A sequência interna de um fluxo, como atualizar e depois consultar ao aplicar filtros, continua pertencendo à mesma solicitação.

Manter a UI responsiva e permitir as interações necessárias para concluir a ação atual, como responder aos seus diálogos. Ao encerrar a ação por sucesso, falha ou cancelamento, encerrar Loading e liberar a aplicação para uma nova solicitação. Cancelar não desfaz etapas já concluídas, conforme OP-07.

O limite funcional e a prevenção de reentrância estão definidos; o mecanismo técnico compartilhado para garanti-los e a escolha de execução em segundo plano permanecem em [P-12](decisoes-e-pendencias.md#p-12). Desabilitar botões isoladamente não garante a regra para as demais entradas. Verificar pelo [ACE-25](criterios-de-aceite.md#ace-25).

<a id="del-01"></a>

## Exclusão 1: apenas a etiqueta

Excluir a Tag selecionada e somente suas associações. Preservar arquivos físicos e outras Tags.

```text
Antes: prova.pdf → Faculdade, PDF, Importante
Excluir apenas Faculdade
Depois: prova.pdf → PDF, Importante
```

Se era a última Tag normal, o cadastro passa a Etiqueta Ausente durante a reorganização.

<a id="del-02"></a>

## Exclusão 2: todas as etiquetas dos arquivos da Tag selecionada

O efeito originalmente definido retira os LocalFile atingidos e todas as suas associações, preservando os arquivos físicos. Não apaga globalmente as demais Tags nem afeta arquivos fora do conjunto escolhido.

Exemplo: prova.pdf tem Faculdade, PDF e Importante. A modalidade 2 aplicada a Faculdade retira as classificações do cadastro atingido; os demais arquivos classificados como PDF ou Importante continuam preservados.

**P-02 permanece aberta:** decidir se os registros são removidos imediatamente ou ficam temporariamente na sentinela, e se a Tag selecionada também é excluída ou permanece vazia.

<a id="del-03"></a>

## Exclusão 3: arquivos físicos da etiqueta

Excluir permanentemente os arquivos físicos associados, seus LocalFile, todas as associações desses cadastros e a Tag selecionada. Outras Tags continuam existindo, embora percam os vínculos com os arquivos excluídos.

A ordem exata, política por lote e recuperação entre disco e SQL não estão definidas. Não há garantia de lixeira, restauração ou reversibilidade.

<a id="del-04"></a>

## Confirmações de exclusão

A UI deve distinguir as três consequências. Antes de excluir registros ou arquivos que tenham outras Tags, pedir confirmação adicional listando essas etiquetas afetadas.

Predefinidas comuns exigem confirmação reforçada; Etiqueta Ausente permanece protegida. Os textos precisam deixar clara a diferença entre retirar associações, remover cadastros e apagar conteúdo físico.

<a id="syn-01"></a>

## Quando atualizar disponibilidade e metadados

| Gatilho | Alcance |
|---|---|
| Inicialização | Todos os LocalFile, além da limpeza de inicialização como tarefa separada. |
| Uso de um arquivo em uma operação | Verificação do arquivo utilizado. |
| Troca de aba | Refresh completo de todos os LocalFile. |
| Antes de aplicar filtros | Refresh completo de todos os LocalFile. |
| Botão Refresh | Refresh completo de todos os LocalFile. |

Navegar por uma pasta realiza as leituras e a consulta de correspondências necessárias à listagem, sem sincronização global a cada pasta. Não há monitoramento contínuo.

<a id="syn-02"></a>

## Metadados sincronizados

Atualizar disponibilidade, tamanho, criação, modificação e último acesso físicos. LocalFileManager coordena NativeFileService e DAOs.

Fazer UPDATE quando nada mudou e tratar datas não disponíveis permanecem em [P-10](decisoes-e-pendencias.md#p-10). Não há valores substitutos, datas de cadastro ou zeros definidos para informações ausentes.

<a id="syn-03"></a>

## Um Refresh por solicitação

Existe um único botão Refresh compartilhado. Uma solicitação global não deve ser duplicada porque duas apresentações receberam o aviso. Observer atualiza a apresentação, sem iniciar outro Refresh. refreshAll não realiza a limpeza por ausência de Tags.

<a id="err-01"></a>

## Informar falhas e resultados parciais

Exibir um pop-up com mensagem compreensível, o que foi concluído, o que falhou e uma área expansível de detalhes técnicos.

```text
Concluído: o arquivo foi movido para a pasta de destino.
Falhou: o novo caminho não foi salvo no MySQL.
Detalhes: mensagem e identificadores disponíveis do erro.
```

Quando existirem, os detalhes podem incluir exceção, SQLState, código MySQL e código de saída de processo. Erro de compilação e erro durante execução são situações diferentes; código de saída se aplica quando há processo externo. Diagnósticos não devem expor credenciais pessoais.

Não há recuperação automática, repetição garantida ou restauração de arquivo definida. Continuar ou parar um lote após falha também está em aberto. O estado Loading termina ao concluir ou falhar, e a interface apresenta o resultado real.
