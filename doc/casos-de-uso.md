# Casos de uso

[Índice](README.md) · [Rastreabilidade](rastreabilidade.md) · [Decisões e pendências](decisoes-e-pendencias.md)

[UC-01](casos-de-uso.md#uc-01) a [UC-15](casos-de-uso.md#uc-15) organizam os percursos que a equipe deverá construir. Os documentos vinculados contêm a norma de cada assunto. **São fluxos futuros, não cenários executados.** A sequência funcional não determina a ordem técnica onde a especificação a deixou aberta.

A associação é o vínculo entre um LocalFile e uma Tag. O registro é o que o banco conhece sobre o arquivo; o arquivo físico continua no sistema de arquivos. Cancelar uma ação pendente não desfaz etapas anteriores concluídas ([OP-07](requisitos-e-regras.md#op-07)).

<a id="uc-01"></a>

## UC-01 — Preparar e abrir o Tag-File

**Origem:** [AMB-01](instalacao-e-execucao.md#amb-01) a [AMB-06](instalacao-e-execucao.md#amb-06), [CIC-02](requisitos-e-regras.md#cic-02), [SYN-01](requisitos-e-regras.md#syn-01). **Estado:** fluxo funcional confirmado; comandos específicos não homologados.

**Gatilho:** abertura do aplicativo.

**Sequência esperada:** ler a configuração; verificar cliente e servidor MySQL; solicitar autorização para instalar ausências; preparar uma instância ainda não inicializada ou reutilizar seus dados; iniciar/reconhecer o servidor; preparar a estrutura do banco; estabelecer a conexão; executar a limpeza exclusiva da inicialização; atualizar todos os registros remanescentes; disponibilizar os exploradores.

**Persistência:** reutilizar os dados existentes. A inicialização do servidor não deve apagar o banco anterior. A limpeza de domínio tem apenas os efeitos de [CIC-02](requisitos-e-regras.md#cic-02).

**Alternativas e falhas:** recusa ou cancelamento da autorização não significa instalação concluída. Falha em uma etapa deve ser mostrada com seu resultado parcial. Não tratar erro de conexão como prova de que o diretório precisa ser reinicializado. As políticas exatas de recuperação de instalação ou schema parcialmente preparado não estão fechadas.

**Não executar esta sequência na missão documental.** Descrever o comportamento planejado e a responsabilidade dos scripts que a equipe deverá construir. Não exigir que esses scripts já existam para documentar a preparação.

<a id="uc-02"></a>

## UC-02 — Navegar em Arquivos Local

**Origem:** [DOM-01](modelo-de-dominio.md#dom-01), [EXP-01](interface-e-fluxos.md#exp-01) a [EXP-03](interface-e-fluxos.md#exp-03), [UI-02](interface-e-fluxos.md#ui-02), [SYN-01](requisitos-e-regras.md#syn-01).

**Gatilho:** abrir a visão local ou navegar para um `NativeDirectory`.

**Sequência:** listar o conteúdo nativo; aplicar os critérios físicos pertinentes; consultar em lote os registros correspondentes dos arquivos resultantes; mostrar os arquivos com as etiquetas encontradas pelo `Map<Path, LocalFile>`; deixar em branco a região de etiquetas quando não houver associação.

**Efeitos:** leitura do sistema de arquivos e do banco, sem criação de `LocalFile` pela simples apresentação. Entrar em uma pasta não dispara por si só sincronização completa de todos os registros.

**Diretórios:** podem ser mostrados para navegação e escolhidos como destino. Isso não aprova registrá-los como `LocalFile` ou executar operações recursivas sobre eles.

<a id="uc-03"></a>

## UC-03 — Pesquisar em Arquivos por Tag

**Origem:** [EXP-01](interface-e-fluxos.md#exp-01), [EXP-02](interface-e-fluxos.md#exp-02), [EXP-04](interface-e-fluxos.md#exp-04), [SYN-01](requisitos-e-regras.md#syn-01).

**Gatilho:** seleção de uma ou várias Tags e aplicação de critérios.

**Sequência:** antes de aplicar filtros, atualizar todos os `LocalFile`; executar a consulta de arquivos associados com AND ou OR; apresentar os arquivos resultantes com suas Tags, sem alterar fisicamente sua organização.

**Resultado:** lista de registros de arquivos. AND exige todas as Tags; OR exige ao menos uma. Não incluir NOT.

**Pendências:** encaixe concreto de `TagFilter`/consulta de associações em [P-05](decisoes-e-pendencias.md#p-05), resultado sem Tags selecionadas e ordenação em [P-13](decisoes-e-pendencias.md#p-13). A Screen observadora não deve iniciar novamente o Refresh global apenas por ter recebido a notificação dessa atualização.

<a id="uc-04"></a>

## UC-04 — Criar uma Tag e associar arquivos iniciais

**Origem:** [TAG-01](requisitos-e-regras.md#tag-01), [TAG-02](requisitos-e-regras.md#tag-02), [EXT-01](requisitos-e-regras.md#ext-01), [OP-01](requisitos-e-regras.md#op-01), [ARQ-02](arquitetura-e-padroes.md#arq-02).

**Entrada:** nome, cor hexadecimal, restrições opcionais de extensão e seleção opcional de arquivos.

**Sequência:** configurar a Tag; normalizar extensões; se houver possível duplicidade de nome, solicitar confirmação; permitir selecionar arquivos de uma pasta pelo `JFileChooser` filtrado; reutilizar registros existentes e criar registros necessários pela Factory; persistir a Tag, suas extensões e associações de acordo com a operação confirmada.

**Exemplo:** ao selecionar a pasta `~/Jogos/Megaman` com restrição `.pdf`, o usuário pode marcar só `prova.pdf`, deixando `Megaman-Manual.pdf` sem a nova Tag.

**Cancelamento:** não confirmar a criação ou a seleção não autoriza cadastrar silenciosamente a lista mostrada. A ordem técnica de persistência e a política de reversão de eventual criação parcial não foram definidas; seguir [ERR-01](requisitos-e-regras.md#err-01) se houver falha depois de uma etapa persistida.

**Limites:** não adicionar uma Tag vazia artificial para cada pasta, não importar subpastas e não criar duplicatas de `LocalFile` pelo mesmo caminho.

<a id="uc-05"></a>

## UC-05 — Associar arquivo a Tag existente

**Origem:** [OP-01](requisitos-e-regras.md#op-01), [EXT-02](requisitos-e-regras.md#ext-02), [DOM-02](modelo-de-dominio.md#dom-02), [CIC-01](requisitos-e-regras.md#cic-01), [TAG-04](requisitos-e-regras.md#tag-04).

**Gatilho:** seleção individual ou Drop de arquivo na representação da Tag, a partir do explorador externo ou interno.

**Sequência:** identificar o arquivo e a Tag por seus dados adequados; verificar compatibilidade da extensão; apresentar as escolhas de [EXT-02](requisitos-e-regras.md#ext-02) se necessário; reutilizar ou criar o registro; criar a associação aprovada; retirar a associação com `Etiqueta Ausente` quando uma Tag normal for aplicada.

**Identidade:** se o caminho já estiver cadastrado, usar o mesmo UUID.

**Sugestão adicional:** quando um novo `LocalFile` for criado, aplicar o fluxo de sugestão de Tag predefinida, respeitando [TAG-04](requisitos-e-regras.md#tag-04) e a pendência [P-03](decisoes-e-pendencias.md#p-03). Não há base para repetir essa sugestão em toda consulta de um registro já existente.

**Cancelamento:** não associar uma extensão incompatível nem modificar a Tag sem a decisão correspondente. Uma nova Tag criada a partir do diálogo não autoriza supor outras associações não confirmadas.

<a id="uc-06"></a>

## UC-06 — Remover uma associação durante a reorganização

**Origem:** [CIC-01](requisitos-e-regras.md#cic-01), [DEL-01](requisitos-e-regras.md#del-01) quando aplicável, [UI-02](interface-e-fluxos.md#ui-02).

**Entrada:** um registro e uma associação escolhida.

**Sequência:** retirar a associação solicitada; se restarem Tags normais, preservar o registro com elas; se não restar nenhuma, associar `Etiqueta Ausente`.

**Efeitos:** o arquivo físico continua intacto e o registro continua acessível durante a sessão. Receber uma nova Tag normal retira a sentinela.

**Limite:** não usar este caso para resolver silenciosamente a modalidade explícita de remoção de todos os registros, que está em [P-02](decisoes-e-pendencias.md#p-02).

<a id="uc-07"></a>

## UC-07 — Editar uma Tag

**Origem:** [TAG-01](requisitos-e-regras.md#tag-01), [EXT-01](requisitos-e-regras.md#ext-01), [EXT-03](requisitos-e-regras.md#ext-03), [DOM-04](modelo-de-dominio.md#dom-04).

**Operações discutidas:** editar os dados da etiqueta, incluindo nome, cor e extensões; identidade permanece associada ao UUID, não ao nome.

**Para extensões:** considerar o conjunto final; identificar os arquivos que se tornarão incompatíveis; listar os afetados e aguardar confirmação; remover somente as associações incompatíveis quando autorizado; aplicar `Etiqueta Ausente` aos arquivos que perderem a última Tag normal.

**Preservação:** outras Tags, outros arquivos e o conteúdo físico permanecem, exceto operações separadas explicitamente solicitadas.

**Pendências:** detalhes da validação de nome/cor, edição de campos especiais de `Etiqueta Ausente`, efeito exato de determinadas ações em `lastFileTaggedAt` e UI de mudanças em lote.

<a id="uc-08"></a>

## UC-08 — Copiar arquivo, com ou sem substituição

**Origem:** [OP-04](requisitos-e-regras.md#op-04), [OP-06](requisitos-e-regras.md#op-06), [OP-08](requisitos-e-regras.md#op-08), [P-01](decisoes-e-pendencias.md#p-01).

**Gatilho:** copiar/colar um arquivo com destino escolhido. A ação de copiar não exige integração com o clipboard do sistema operacional.

**Sequência confirmada:** manter a origem; criar a cópia física; perguntar sobre herdar Tags; se o destino conflitar, oferecer as alternativas aprovadas conforme aplicáveis. “Substituir” sobrescreve fisicamente o arquivo de destino. “Manter os dois” exige caminhos distintos.

**Efeitos não fechados:** UUID e associações sobreviventes durante a substituição; existência de `LocalFile` para cópia sem Tags. Documentar [P-01](decisoes-e-pendencias.md#p-01) e os exemplos conflitantes sem escolher a interpretação.

**Cancelamento/falha:** não afirmar que cancelar uma etapa reverte automaticamente uma cópia já criada; a ordenação ainda não aprovada do fluxo deve permanecer identificada como não definida, sem ser deduzida de uma implementação. Resultados parciais seguem [ERR-01](requisitos-e-regras.md#err-01).

<a id="uc-09"></a>

## UC-09 — Recortar em uma visão e colar na outra

**Origem:** [OP-03](requisitos-e-regras.md#op-03), [OP-08](requisitos-e-regras.md#op-08), [EXP-01](interface-e-fluxos.md#exp-01).

**Cenário:** selecionar `prova.pdf` na Tag `Faculdade`, recortar e colar em `~/Documentos/Faculdade` pela visão local.

**Antes da colagem:** arquivo continua na origem; clipboard interno guarda a intenção CUT.

**Depois da colagem bem-sucedida:** arquivo está no destino; o mesmo registro mantém UUID e Tags; o caminho conhecido é atualizado; a apresentação local mostra essas Tags.

**Colaboradores:** Screen, Controller de origem, `ClipboardService` e Controller que recebe a colagem. A colagem utiliza o `NativeFileService` para a operação física e, quando houver `LocalFile`, o `LocalFileManager` e os DAOs pertinentes para coordenar a atualização do registro. O compartilhamento não exige dependência direta entre os Controllers.

**Exceções:** conflitos seguem [OP-06](requisitos-e-regras.md#op-06); arquivo desaparecido segue [OP-02](requisitos-e-regras.md#op-02); falha de disco/banco segue [ERR-01](requisitos-e-regras.md#err-01). Não há política fechada de repetição de colagem CUT ou limpeza do clipboard após sucesso.

<a id="uc-10"></a>

## UC-10 — Renomear arquivo

**Origem:** [OP-05](requisitos-e-regras.md#op-05), [EXT-02](requisitos-e-regras.md#ext-02)/[EXT-03](requisitos-e-regras.md#ext-03) por compatibilidade, [CIC-01](requisitos-e-regras.md#cic-01).

**Entrada:** arquivo escolhido e novo nome.

**Sem mudança de extensão:** preservar a identidade do registro, atualizar a representação e o caminho conforme a operação física.

**Com mudança de extensão:** identificar Tags incompatíveis e oferecer retirar essas Tags, acrescentar a nova extensão a elas ou cancelar. Somente prosseguir conforme a decisão. Preservar as associações compatíveis.

**Exemplo:** `prova.pdf` com Tags `PDF` e `Faculdade` sem restrição passa a `prova.txt`. Se o usuário escolher retirar as incompatíveis, `Faculdade` permanece. Se ela também não existir, [CIC-01](requisitos-e-regras.md#cic-01) cobre a perda da última Tag normal.

**Limite:** renomear não converte o conteúdo. Tratamento de conflito com outro nome existente é o de [OP-06](requisitos-e-regras.md#op-06), sem uma matriz por operação ainda aprovada.

<a id="uc-11"></a>

## UC-11 — Relocalizar ou remover referência indisponível

**Origem:** [OP-02](requisitos-e-regras.md#op-02), [DOM-02](modelo-de-dominio.md#dom-02), [SYN-01](requisitos-e-regras.md#syn-01), [P-02](decisoes-e-pendencias.md#p-02).

**Gatilho:** tentativa de usar um arquivo não encontrado no caminho salvo.

**Alternativas:** localizar outro caminho, remover do Tag-File ou cancelar.

**Relocalização bem-sucedida:** o registro conserva UUID e Tags e passa a referenciar o novo endereço. Isso é atualização da referência, não afirmação de que o Tag-File descobriu sozinho o movimento externo.

**Pendência:** colisão com um registro existente no caminho indicado e a relação da remoção explícita com o adiamento de [CIC-01](requisitos-e-regras.md#cic-01). Não fundir registros nem apagar arquivo físico por inferência.

<a id="uc-12"></a>

## UC-12 — Excluir Tag em uma das três modalidades

**Origem:** [DEL-01](requisitos-e-regras.md#del-01) a [DEL-04](requisitos-e-regras.md#del-04), [CIC-01](requisitos-e-regras.md#cic-01), [P-02](decisoes-e-pendencias.md#p-02).

**Entrada:** Tag selecionada por identidade e modalidade explicitamente escolhida.

| Modalidade | Tag selecionada | Associações | LocalFile | Arquivo físico |
|---|---|---|---|---|
| Apenas a etiqueta | Excluir | Retirar apenas as dessa Tag | Preservar; aplicar `Etiqueta Ausente` se perder a última normal | Preservar |
| Todas as etiquetas dos arquivos da selecionada | Pendente se exclui ou fica vazia | Originalmente: remover todas dos registros atingidos | Originalmente: excluir; conflito de adiamento em [P-02](decisoes-e-pendencias.md#p-02) | Preservar |
| Arquivos permanentemente | Excluir | Remover todas dos registros atingidos | Excluir | Apagar permanentemente |

Antes de afetar outras Tags por remoção de registro/arquivo, listar essas etiquetas e pedir confirmação adicional. Para predefinidas comuns, reforçar a confirmação. A Tag protegida não pode ser excluída.

Não prometer execução atômica nem reversão. Se houver falha parcial, informar as etapas e não apresentar o conjunto como integralmente concluído.

<a id="uc-13"></a>

## UC-13 — Localizar Tags vazias

**Origem:** [TAG-03](requisitos-e-regras.md#tag-03), [TAG-02](requisitos-e-regras.md#tag-02).

**Gatilho:** funcionalidade solicitada para encontrar etiquetas sem associações e permitir removê-las.

**Resultado:** identificar ausência real de associações, não apenas ausência de arquivos disponíveis. Proteger `Etiqueta Ausente`. As confirmações aplicáveis às demais Tags continuam valendo.

**Não definido:** disposição visual, filtros auxiliares, seleção múltipla de Tags vazias e automação dessa limpeza. Não transformá-la em exclusão automática recorrente.

<a id="uc-14"></a>

## UC-14 — Refresh e filtros

**Origem:** [SYN-01](requisitos-e-regras.md#syn-01) a [SYN-03](requisitos-e-regras.md#syn-03), [UI-03](interface-e-fluxos.md#ui-03), [ARQ-05](arquitetura-e-padroes.md#arq-05) a [ARQ-07](arquitetura-e-padroes.md#arq-07).

**Gatilho:** botão Refresh único, troca de aba ou aplicação de filtros; a inicialização também atualiza todos, mas possui sua limpeza própria.

**Sequência:** mostrar Loading; coordenar uma atualização de todos os `LocalFile`; executar a consulta pretendida quando houver; publicar o evento pertinente pelo Controller; atualizar a apresentação das duas visões; encerrar Loading conforme sucesso ou falha.

**Invariantes:** não limpar `Etiqueta Ausente`; não atualizar todos novamente apenas porque cada Screen recebeu evento; não iniciar Refresh por toda navegação de pasta.

**Implementação interna:** não foi fechada a escolha de `SwingWorker` nem um mecanismo de fila. Documente a necessidade de não congelar a UI e a ausência de pool, sem introduzir infraestrutura como fato aprovado.

<a id="uc-15"></a>

## UC-15 — Apresentar falha parcial

**Origem:** [ERR-01](requisitos-e-regras.md#err-01), [SQL-05](banco-de-dados.md#sql-05), [AMB-05](instalacao-e-execucao.md#amb-05).

**Gatilho:** uma consulta, etapa física, persistência ou processo externo falha.

**Resultado esperado:** mensagem compreensível, etapas concluídas, etapa com falha e detalhes específicos disponíveis em uma área expansível. Encerrar o estado de Loading para não deixar a UI bloqueada indefinidamente.

**Sem garantias:** não dizer que `autoReconnect=true` desfez ou repetiu uma operação; não prometer recuperação física ou transação entre banco e disco. Falha de instalação também não autoriza apagar dados existentes.

**Documentação:** apresente logs somente como exemplos ilustrativos e omita segredos de eventuais materiais de apoio. Não atribua resultados de execução real aos cenários planejados.

## Efeitos e colaboradores nos percursos principais

| Percurso | Memória e coordenação | Banco | Disco e apresentação |
|---|---|---|---|
| [UC-05](casos-de-uso.md#uc-05): associar `prova.pdf` a `Faculdade` | A Screen encaminha a escolha; Controller aciona colaboradores aprovados. Reutilizar identidade do caminho ou construir entidade pela Factory. | Persistir associação; retirar sentinela se receber Tag normal. | Não mover nem copiar o conteúdo; apresentar as Tags. Se um LocalFile foi criado, a sugestão segue [TAG-04](requisitos-e-regras.md#tag-04) e [P-03](decisoes-e-pendencias.md#p-03). |
| [UC-09](casos-de-uso.md#uc-09): recortar e colar entre visões | ClipboardService guarda CUT; o Controller de destino chama NativeFileService e, para registro existente, LocalFileManager. | Salvar o novo caminho com o mesmo UUID e Tags. | Mover somente ao colar; após sucesso, mostrar arquivo e Tags no destino. Falhas entre etapas seguem [ERR-01](requisitos-e-regras.md#err-01). |
| [UC-14](casos-de-uso.md#uc-14): Refresh | Mostrar Loading; Controller solicita atualização global por LocalFileManager uma vez. | Sincronizar disponibilidade e metadados conforme [SYN-02](requisitos-e-regras.md#syn-02). Não limpar registros. | Ler metadados e atualizar as apresentações; evento não inicia outro Refresh. |

As assinaturas de eventos/consultas continuam em [P-04](decisoes-e-pendencias.md#p-04)/[P-05](decisoes-e-pendencias.md#p-05). Atribuições não fechadas de validação entre Factory, entidade e serviço não são resolvidas por essa tabela. [P-12](decisoes-e-pendencias.md#p-12) mantém em aberto lotes e estado do clipboard depois de colar. O fluxo [UC-04](casos-de-uso.md#uc-04) também deverá aplicar [TAG-04](requisitos-e-regras.md#tag-04) quando criar LocalFile: a duração da supressão está definida, mas seu efeito automático continua em [P-03](decisoes-e-pendencias.md#p-03).
