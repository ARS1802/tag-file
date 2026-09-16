# Casos de uso: ações e resultados

[Índice](README.md) · [Requisitos](requisitos-e-regras.md) · [Critérios de aceite](criterios-de-aceite.md)

Um caso de uso descreve uma ação do usuário, seus passos e o resultado esperado. Use esta página ao construir uma funcionalidade. As regras detalhadas ficam nos links de cada caso.

Arquivo físico, cadastro e associação são diferentes: retirar uma etiqueta não significa apagar o arquivo. Cancelar impede a ação ainda não confirmada, mas não desfaz etapas já concluídas. Alguns contratos e a ordem técnica de certas operações continuam em aberto, conforme indicado.

As verificações relacionadas conferem aspectos de cada fluxo, não sua cobertura completa. As regras e pendências continuam sendo a referência para o restante do comportamento.

**Regra comum a todos os fluxos:** apenas uma ação pode estar em andamento na aplicação, compartilhando esse limite entre os dois exploradores. Novas solicitações durante uma ação são bloqueadas e não ficam em fila. As etapas e os diálogos do fluxo atual pertencem à mesma ação; ao encerrar por sucesso, falha ou cancelamento, liberar a aplicação. Aplicar [OP-09](requisitos-e-regras.md#op-09) e verificar pelo [ACE-25](criterios-de-aceite.md#ace-25).

<a id="uc-01"></a>

## UC-01 — Preparar e abrir o Tag-File

**Regras:** [AMB-01](instalacao-e-execucao.md#amb-01) a [AMB-06](instalacao-e-execucao.md#amb-06), [CIC-02](requisitos-e-regras.md#cic-02), [SYN-01](requisitos-e-regras.md#syn-01). Os comandos específicos ainda precisam ser definidos.

**Gatilho:** abertura do aplicativo.

**Sequência esperada:** ler a configuração; verificar cliente e servidor MySQL; solicitar autorização para instalar ausências; preparar uma instância ainda não inicializada ou reutilizar seus dados; iniciar/reconhecer o servidor; preparar a estrutura do banco; estabelecer a conexão; executar a limpeza exclusiva da inicialização; atualizar todos os registros remanescentes; disponibilizar os exploradores.

**Persistência:** reutilizar os dados existentes. A inicialização do servidor não deve apagar o banco anterior. A limpeza de domínio tem apenas os efeitos de [CIC-02](requisitos-e-regras.md#cic-02).

**Alternativas e falhas:** recusa ou cancelamento da autorização não significa instalação concluída. Falha em uma etapa deve ser mostrada com seu resultado parcial. Não tratar erro de conexão como prova de que o diretório precisa ser reinicializado. As políticas exatas de recuperação de instalação ou schema parcialmente preparado não estão fechadas.

**Verificação relacionada:** [ACE-12](criterios-de-aceite.md#ace-12), [ACE-23](criterios-de-aceite.md#ace-23) e [ACE-24](criterios-de-aceite.md#ace-24): limpeza inicial, parâmetros e autorização. Não cobrem todos os procedimentos de instalação, alteração de schema ou encerramento.

<a id="uc-02"></a>

## UC-02 — Navegar em Arquivos Local

**Regras:** [DOM-01](modelo-de-dominio.md#dom-01), [EXP-01](interface-e-fluxos.md#exp-01) a [EXP-03](interface-e-fluxos.md#exp-03), [UI-02](interface-e-fluxos.md#ui-02), [SYN-01](requisitos-e-regras.md#syn-01).

**Gatilho:** abrir a visão local ou navegar para um `NativeDirectory`.

**Sequência:** listar o conteúdo nativo; aplicar os critérios físicos pertinentes; consultar em lote os registros correspondentes dos arquivos resultantes; mostrar os arquivos com as etiquetas encontradas pelo `Map<Path, LocalFile>`; deixar em branco a região de etiquetas quando não houver associação.

**Efeitos:** leitura do sistema de arquivos e do banco, sem criação de `LocalFile` pela simples apresentação. Entrar em uma pasta não dispara por si só sincronização completa de todos os registros.

**Diretórios:** podem ser mostrados para navegação e escolhidos como destino. Isso não aprova registrá-los como `LocalFile` ou executar operações recursivas sobre eles.

**Verificação relacionada:** [ACE-01](criterios-de-aceite.md#ace-01), [ACE-03](criterios-de-aceite.md#ace-03) e [ACE-16](criterios-de-aceite.md#ace-16): navegação sem cadastro automático, arquivos de mesmo nome e correspondências em lote.

<a id="uc-03"></a>

## UC-03 — Pesquisar em Arquivos por Tag

**Regras:** [EXP-01](interface-e-fluxos.md#exp-01), [EXP-02](interface-e-fluxos.md#exp-02), [EXP-04](interface-e-fluxos.md#exp-04), [SYN-01](requisitos-e-regras.md#syn-01).

**Gatilho:** seleção de uma ou várias Tags e aplicação de critérios.

**Sequência:** antes de aplicar filtros, atualizar todos os `LocalFile`; executar a consulta de arquivos associados com AND ou OR; apresentar os arquivos resultantes com suas Tags, sem alterar fisicamente sua organização.

**Resultado:** lista de registros de arquivos. AND exige todas as Tags; OR exige ao menos uma. Não incluir NOT.

**Pendências:** encaixe concreto de `TagFilter`/consulta de associações em [P-05](decisoes-e-pendencias.md#p-05), resultado sem Tags selecionadas e ordenação em [P-13](decisoes-e-pendencias.md#p-13). A Screen observadora não deve iniciar novamente o Refresh global apenas por ter recebido a notificação dessa atualização.

**Verificação relacionada:** [ACE-15](criterios-de-aceite.md#ace-15): AND/OR. Busca sem Tag, ordenação e API continuam nas pendências indicadas.

<a id="uc-04"></a>

## UC-04 — Criar uma Tag e associar arquivos iniciais

**Regras:** [TAG-01](requisitos-e-regras.md#tag-01), [TAG-02](requisitos-e-regras.md#tag-02), [EXT-01](requisitos-e-regras.md#ext-01), [OP-01](requisitos-e-regras.md#op-01), [ARQ-02](arquitetura-e-padroes.md#arq-02).

**Entrada:** nome, cor hexadecimal, restrições opcionais de extensão e seleção opcional de arquivos.

**Sequência:** configurar a Tag; normalizar extensões; se houver possível duplicidade de nome, solicitar confirmação; permitir selecionar arquivos de uma pasta pelo `JFileChooser` filtrado; reutilizar registros existentes e criar registros necessários pela Factory; persistir a Tag, suas extensões e associações de acordo com a operação confirmada.

**Exemplo:** ao selecionar a pasta `~/Jogos/Megaman` com restrição `.pdf`, o usuário pode marcar só `prova.pdf`, deixando `Megaman-Manual.pdf` sem a nova Tag.

**Cancelamento:** não confirmar a criação ou a seleção não autoriza cadastrar silenciosamente a lista mostrada. A ordem técnica de persistência e a política de reversão de eventual criação parcial não foram definidas; seguir [ERR-01](requisitos-e-regras.md#err-01) se houver falha depois de uma etapa persistida.

**Limites:** não adicionar uma Tag vazia artificial para cada pasta, não importar subpastas e não criar duplicatas de `LocalFile` pelo mesmo caminho.

**Verificação relacionada:** [ACE-02](criterios-de-aceite.md#ace-02), [ACE-04](criterios-de-aceite.md#ace-04), [ACE-05](criterios-de-aceite.md#ace-05), [ACE-06](criterios-de-aceite.md#ace-06) e [ACE-09](criterios-de-aceite.md#ace-09): reutilização de cadastro, extensões e nome repetido. A sugestão de predefinida segue TAG-04/P-03.

<a id="uc-05"></a>

## UC-05 — Associar arquivo a Tag existente

**Regras:** [OP-01](requisitos-e-regras.md#op-01), [EXT-02](requisitos-e-regras.md#ext-02), [DOM-02](modelo-de-dominio.md#dom-02), [CIC-01](requisitos-e-regras.md#cic-01), [TAG-04](requisitos-e-regras.md#tag-04).

**Gatilho:** seleção individual ou Drop de arquivo na representação da Tag, a partir do explorador externo ou interno.

**Sequência:** identificar o arquivo e a Tag por seus dados adequados; verificar compatibilidade da extensão; apresentar as escolhas de [EXT-02](requisitos-e-regras.md#ext-02) se necessário; reutilizar ou criar o registro; criar a associação aprovada; retirar a associação com `Etiqueta Ausente` quando uma Tag normal for aplicada.

**Identidade:** se o caminho já estiver cadastrado, usar o mesmo UUID.

**Sugestão adicional:** quando um novo `LocalFile` for criado, aplicar o fluxo de sugestão de Tag predefinida, respeitando [TAG-04](requisitos-e-regras.md#tag-04) e a pendência [P-03](decisoes-e-pendencias.md#p-03). A sugestão é ligada à criação do cadastro, não a cada consulta de um registro existente.

**Cancelamento:** não associar uma extensão incompatível nem modificar a Tag sem a decisão correspondente. Uma nova Tag criada a partir do diálogo não autoriza supor outras associações não confirmadas.

**Verificação relacionada:** [ACE-02](criterios-de-aceite.md#ace-02), [ACE-06](criterios-de-aceite.md#ace-06), [ACE-07](criterios-de-aceite.md#ace-07) e [ACE-11](criterios-de-aceite.md#ace-11): reutilização, compatibilidade e retirada da sentinela. A sugestão de predefinida continua limitada por P-03.

<a id="uc-06"></a>

## UC-06 — Remover uma associação durante a reorganização

**Regras:** [CIC-01](requisitos-e-regras.md#cic-01), [DEL-01](requisitos-e-regras.md#del-01) quando aplicável, [UI-02](interface-e-fluxos.md#ui-02).

**Entrada:** um registro e uma associação escolhida.

**Sequência:** retirar a associação solicitada; se restarem Tags normais, preservar o registro com elas; se não restar nenhuma, associar `Etiqueta Ausente`.

**Efeitos:** o arquivo físico continua intacto e o registro continua acessível durante a sessão. Receber uma nova Tag normal retira a sentinela.

**Limite:** a remoção explícita de todos os registros tem seu alcance pendente em [P-02](decisoes-e-pendencias.md#p-02).

**Verificação relacionada:** [ACE-10](criterios-de-aceite.md#ace-10): retirada da última Tag normal durante a reorganização.

<a id="uc-07"></a>

## UC-07 — Editar uma Tag

**Regras:** [TAG-01](requisitos-e-regras.md#tag-01), [EXT-01](requisitos-e-regras.md#ext-01), [EXT-03](requisitos-e-regras.md#ext-03), [DOM-04](modelo-de-dominio.md#dom-04).

**Ação:** editar os dados da etiqueta, incluindo nome, cor e extensões; identidade permanece associada ao UUID, não ao nome.

**Para extensões:** considerar o conjunto final; identificar os arquivos que se tornarão incompatíveis; listar os afetados e aguardar confirmação; remover somente as associações incompatíveis quando autorizado; aplicar `Etiqueta Ausente` aos arquivos que perderem a última Tag normal.

**Preservação:** outras Tags, outros arquivos e o conteúdo físico permanecem, exceto operações separadas explicitamente solicitadas.

**Pendências:** detalhes da validação de nome/cor, edição de campos especiais de `Etiqueta Ausente`, efeito exato de determinadas ações em `lastFileTaggedAt` e UI de mudanças em lote.

**Verificação relacionada:** [ACE-08](criterios-de-aceite.md#ace-08): efeito da alteração de restrições. [ACE-04](criterios-de-aceite.md#ace-04), [ACE-05](criterios-de-aceite.md#ace-05) e [ACE-06](criterios-de-aceite.md#ace-06) apoiam a configuração de extensões; não verificam integralmente nome, cor e datas.

<a id="uc-08"></a>

## UC-08 — Copiar arquivo, com ou sem substituição

**Regras:** [OP-04](requisitos-e-regras.md#op-04), [OP-06](requisitos-e-regras.md#op-06), [OP-08](requisitos-e-regras.md#op-08).

**Entrada:** arquivo de origem e pasta de destino, usando o clipboard interno.

**Interações necessárias:** perguntar se a cópia deve herdar Tags e, quando houver conflito, apresentar Substituir, Manter os dois ou Cancelar, conforme aplicável.

**Efeitos definidos:** a origem física permanece. Uma cópia bem-sucedida produz outro arquivo físico. Substituir sobrescreve fisicamente o destino; Manter os dois conserva caminhos distintos.

**Decisões abertas:** a identidade e as associações que sobrevivem na substituição e o cadastro de cópia sem Tags permanecem em [P-01](decisoes-e-pendencias.md#p-01). A ordem completa das confirmações e da execução ainda precisa ser definida; a lista de efeitos acima não é uma sequência operacional.

**Cancelamento e falha:** cancelar não autoriza a ação ainda pendente. Não há garantia de desfazer uma etapa já concluída. Em resultado parcial, aplicar [ERR-01](requisitos-e-regras.md#err-01), distinguindo disco e SQL.

**Verificação relacionada:** [ACE-20](criterios-de-aceite.md#ace-20): herança de Tags e alternativas de conflito, com o resultado SQL limitado por P-01.

<a id="uc-09"></a>

## UC-09 — Recortar em uma visão e colar na outra

**Regras:** [OP-03](requisitos-e-regras.md#op-03), [OP-08](requisitos-e-regras.md#op-08), [EXP-01](interface-e-fluxos.md#exp-01).

**Cenário:** selecionar `prova.pdf` na Tag `Faculdade`, recortar e colar em `~/Documentos/Faculdade` pela visão local.

**Antes da colagem:** arquivo continua na origem; clipboard interno guarda a intenção CUT.

**Depois da colagem bem-sucedida:** arquivo está no destino; o mesmo registro mantém UUID e Tags; o caminho conhecido é atualizado; a apresentação local mostra essas Tags.

**Colaboradores:** Screen, Controller de origem, `ClipboardService` e Controller que recebe a colagem. A colagem utiliza o `NativeFileService` para a operação física e, quando houver `LocalFile`, o `LocalFileManager` e os DAOs pertinentes para coordenar a atualização do registro. O compartilhamento não exige dependência direta entre os Controllers.

**Exceções:** conflitos seguem [OP-06](requisitos-e-regras.md#op-06); arquivo desaparecido segue [OP-02](requisitos-e-regras.md#op-02); falha de disco/banco segue [ERR-01](requisitos-e-regras.md#err-01). Não há política fechada de repetição de colagem CUT ou limpeza do clipboard após sucesso.

**Verificação relacionada:** [ACE-17](criterios-de-aceite.md#ace-17): preservação de UUID/Tags no movimento. O estado posterior do clipboard continua em P-12.

<a id="uc-10"></a>

## UC-10 — Renomear arquivo

**Regras:** [OP-05](requisitos-e-regras.md#op-05), [EXT-02](requisitos-e-regras.md#ext-02)/[EXT-03](requisitos-e-regras.md#ext-03) por compatibilidade, [CIC-01](requisitos-e-regras.md#cic-01).

**Entrada:** arquivo escolhido e novo nome.

**Sem mudança de extensão:** preservar a identidade do registro, atualizar a representação e o caminho conforme a operação física.

**Com mudança de extensão:** identificar Tags incompatíveis e oferecer retirar essas Tags, acrescentar a nova extensão a elas ou cancelar. Somente prosseguir conforme a decisão. Preservar as associações compatíveis.

**Exemplo:** `prova.pdf` com a Tag `PDF` restrita a `.pdf` e a Tag `Faculdade` sem restrição passa a `prova.txt`. Se o usuário escolher retirar as incompatíveis, `Faculdade` permanece. Se ela também não existir, [CIC-01](requisitos-e-regras.md#cic-01) cobre a perda da última Tag normal.

**Limite:** renomear não converte o conteúdo. Tratamento de conflito com outro nome existente é o de [OP-06](requisitos-e-regras.md#op-06), sem uma matriz por operação ainda aprovada.

**Verificação relacionada:** [ACE-18](criterios-de-aceite.md#ace-18): alternativas diante da nova extensão incompatível.

<a id="uc-11"></a>

## UC-11 — Relocalizar ou remover referência indisponível

**Regras:** [OP-02](requisitos-e-regras.md#op-02), [DOM-02](modelo-de-dominio.md#dom-02), [SYN-01](requisitos-e-regras.md#syn-01), [P-02](decisoes-e-pendencias.md#p-02).

**Gatilho:** tentativa de usar um arquivo não encontrado no caminho salvo.

**Alternativas:** localizar outro caminho, remover do Tag-File ou cancelar.

**Relocalização bem-sucedida:** o registro conserva UUID e Tags e passa a referenciar o novo endereço. Isso é atualização da referência, não afirmação de que o Tag-File descobriu sozinho o movimento externo.

**Pendência:** colisão com um registro existente no caminho indicado e a relação da remoção explícita com o adiamento de [CIC-01](requisitos-e-regras.md#cic-01). Não há fusão automática de registros nem exclusão física nesse fluxo.

**Verificação relacionada:** Não há ACE específico para este fluxo. Conferir [OP-02](requisitos-e-regras.md#op-02), [P-02](decisoes-e-pendencias.md#p-02) e [P-08](decisoes-e-pendencias.md#p-08); os critérios de associação não comprovam relocalização.

<a id="uc-12"></a>

## UC-12 — Excluir Tag em uma das três modalidades

**Regras:** [DEL-01](requisitos-e-regras.md#del-01) a [DEL-04](requisitos-e-regras.md#del-04), [CIC-01](requisitos-e-regras.md#cic-01), [P-02](decisoes-e-pendencias.md#p-02).

**Entrada:** Tag selecionada por identidade e modalidade explicitamente escolhida.

| Modalidade | Tag selecionada | Associações | LocalFile | Arquivo físico |
|---|---|---|---|---|
| Apenas a etiqueta | Excluir | Retirar apenas as dessa Tag | Preservar; aplicar `Etiqueta Ausente` se perder a última normal | Preservar |
| Todas as etiquetas dos arquivos da selecionada | Pendente se exclui ou fica vazia | Originalmente: remover todas dos registros atingidos | Originalmente: excluir; conflito de adiamento em [P-02](decisoes-e-pendencias.md#p-02) | Preservar |
| Arquivos permanentemente | Excluir | Remover todas dos registros atingidos | Excluir | Apagar permanentemente |

Antes de afetar outras Tags por remoção de registro/arquivo, listar essas etiquetas e pedir confirmação adicional. Para predefinidas comuns, reforçar a confirmação. A Tag protegida não pode ser excluída.

Não prometer execução atômica nem reversão. Se houver falha parcial, informar as etapas e não apresentar o conjunto como integralmente concluído.

**Verificação relacionada:** [ACE-19](criterios-de-aceite.md#ace-19) e [ACE-14](criterios-de-aceite.md#ace-14): consequências e proteção da sentinela. [ACE-10](criterios-de-aceite.md#ace-10) apoia a retirada da última Tag normal; a modalidade 2 permanece em P-02.

<a id="uc-13"></a>

## UC-13 — Localizar Tags vazias

**Regras:** [TAG-03](requisitos-e-regras.md#tag-03), [TAG-02](requisitos-e-regras.md#tag-02).

**Gatilho:** funcionalidade solicitada para encontrar etiquetas sem associações e permitir removê-las.

**Resultado:** identificar ausência real de associações, não apenas ausência de arquivos disponíveis. Proteger `Etiqueta Ausente`. As confirmações aplicáveis às demais Tags continuam valendo.

**Pendente:** disposição visual, filtros auxiliares, seleção múltipla e automação da busca de Tags vazias. O fluxo definido permite localizar e escolher o que excluir; não há exclusão automática recorrente.

**Verificação relacionada:** [ACE-14](criterios-de-aceite.md#ace-14) confere a proteção de Etiqueta Ausente. A busca geral de Tags vazias deve ser conferida também pela regra [TAG-03](requisitos-e-regras.md#tag-03).

<a id="uc-14"></a>

## UC-14 — Refresh e filtros

**Regras:** [SYN-01](requisitos-e-regras.md#syn-01) a [SYN-03](requisitos-e-regras.md#syn-03), [UI-03](interface-e-fluxos.md#ui-03), [ARQ-05](arquitetura-e-padroes.md#arq-05) a [ARQ-07](arquitetura-e-padroes.md#arq-07).

**Gatilho:** botão Refresh único, troca de aba ou aplicação de filtros; a inicialização também atualiza todos, mas possui sua limpeza própria.

**Sequência:** admitir a solicitação somente se nenhuma ação estiver em andamento; mostrar Loading; coordenar uma atualização de todos os `LocalFile`; executar a consulta pretendida quando houver; publicar o evento pertinente pelo Controller; atualizar a apresentação das duas visões; encerrar Loading e liberar o controle compartilhado ao terminar. Atualização e consulta integram a mesma ação. Solicitações recebidas durante outra ação são bloqueadas, sem fila.

**Invariantes:** não limpar `Etiqueta Ausente`; não atualizar todos novamente apenas porque cada Screen recebeu evento; não iniciar Refresh por toda navegação de pasta.

**Implementação interna:** a interface precisa permanecer responsiva enquanto somente uma ação executa. SwingWorker é uma alternativa de estudo; sua adoção e o mecanismo que garante [OP-09](requisitos-e-regras.md#op-09) continuam em P-12. A versão não usa pool de conexões. Veja o [Observer e Swing](arquitetura-e-padroes.md#arq-06).

**Verificação relacionada:** [ACE-13](criterios-de-aceite.md#ace-13), [ACE-21](criterios-de-aceite.md#ace-21) e [ACE-25](criterios-de-aceite.md#ace-25): ausência de limpeza na sessão, atualização sem duplicação e bloqueio global de novas ações, sem fila.

<a id="uc-15"></a>

## UC-15 — Apresentar falha parcial

**Regras:** [ERR-01](requisitos-e-regras.md#err-01), [SQL-05](banco-de-dados.md#sql-05), [AMB-05](instalacao-e-execucao.md#amb-05).

**Gatilho:** uma consulta, etapa física, persistência ou processo externo falha.

**Resultado esperado:** mensagem compreensível, etapas concluídas, etapa com falha e detalhes específicos disponíveis em uma área expansível. Ao terminar o tratamento da ação, encerrar Loading e liberar o controle compartilhado para permitir nova solicitação. Não executar automaticamente ações que foram bloqueadas durante a anterior.

**Sem garantias:** não dizer que `autoReconnect=true` desfez ou repetiu uma operação; não prometer recuperação física ou transação entre banco e disco. Falha de instalação também não autoriza apagar dados existentes.

Diagnósticos devem usar somente os detalhes disponíveis e não expor credenciais pessoais.

**Verificação relacionada:** [ACE-22](criterios-de-aceite.md#ace-22) e [ACE-25](criterios-de-aceite.md#ace-25): mensagem do resultado parcial, encerramento de Loading e liberação do controle compartilhado. Recuperação e política de lotes continuam abertas.
