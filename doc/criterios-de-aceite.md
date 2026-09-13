# Critérios de aceite

ACE-01 a ACE-24 são cenários derivados da especificação para futura verificação e apresentação acadêmica. Não representam uma rubrica adicional atribuída ao professor. **Todos estão NÃO EXECUTADOS**: não houve execução da aplicação, criação de massa de testes, compilação, consulta ao banco ou teste de implementação nesta missão.

O código encontrado em [`src/Main.java`](../src/Main.java), método `Main.main`, é um template de saudação e laço de 1 a 5. As funcionalidades abaixo não foram encontradas no conjunto de arquivos do projeto. As condições descritas são preparações hipotéticas para uma verificação futura, não dados que se afirmem existentes. As regras vinculadas são a fonte principal; este documento organiza o que observar sem definir contratos pendentes.

## Arquivos, Tags e extensões

<a id="ace-01"></a>

### ACE-01 — Navegar sem cadastrar todos os arquivos

- **Origem:** [DOM-01](modelo-de-dominio.md#dom-01), [EXP-01](interface-e-fluxos.md#exp-01).
- **Condição e ação futuras:** abrir em `Arquivos Local` uma pasta que contenha arquivos sem registro.
- **Resultado esperado:** os arquivos aparecem como representações nativas, sem criar um `LocalFile` ou linha SQL para cada arquivo simplesmente exibido. Não são exigidas Tags artificiais.
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-02"></a>

### ACE-02 — Reutilizar identidade ao associar

- **Origem:** [DOM-02](modelo-de-dominio.md#dom-02), [OP-01](requisitos-e-regras.md#op-01).
- **Condição e ação futuras:** associar uma Tag a um arquivo cujo caminho já está cadastrado.
- **Resultado esperado:** reutilizar `LocalFile` e UUID, criando somente a associação necessária. Não criar outro registro para o mesmo caminho.
- **Limite:** equivalência e normalização de caminhos dependem de [P-08](decisoes-e-pendencias.md#p-08); o cenário básico considera o mesmo caminho cadastrado.
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-03"></a>

### ACE-03 — Distinguir arquivos homônimos

- **Origem:** [DOM-02](modelo-de-dominio.md#dom-02).
- **Condição e ação futuras:** apresentar ou classificar arquivos com o mesmo nome em pastas diferentes.
- **Resultado esperado:** permitir arquivos distintos; nome repetido não funde registros. Quando ambos estiverem cadastrados, seus registros têm identidade própria.
- **Limite:** não pressupõe deduplicação por conteúdo nem resolve caminhos equivalentes de [P-08](decisoes-e-pendencias.md#p-08).
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-04"></a>

### ACE-04 — Aceitar várias extensões específicas

- **Origem:** [EXT-01](requisitos-e-regras.md#ext-01).
- **Condição e ação futuras:** configurar uma Tag com mais de uma extensão real, incluindo `.cdr`.
- **Resultado esperado:** persistir as restrições e permitir associar arquivos compatíveis com qualquer extensão configurada. Não restringir as escolhas a categorias fechadas como IMAGE ou AUDIO.
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-05"></a>

### ACE-05 — Normalizar e evitar repetição de extensão

- **Origem:** [EXT-01](requisitos-e-regras.md#ext-01), [SQL-02](banco-de-dados.md#sql-02).
- **Condição e ação futuras:** informar `PDF` e `.PDF` nas extensões da mesma Tag.
- **Resultado esperado:** normalizar ambas para `.pdf`, sem duplicar a extensão nessa Tag. A mesma extensão continua permitida em Tags distintas.
- **Limite:** o critério não homologa uma constraint física específica ou resolve extensões compostas e nomes especiais de [P-08](decisoes-e-pendencias.md#p-08).
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-06"></a>

### ACE-06 — Aceitar qualquer extensão sem restrição

- **Origem:** [EXT-01](requisitos-e-regras.md#ext-01).
- **Condição e ação futuras:** associar arquivos com extensões diferentes a uma Tag sem extensões configuradas.
- **Resultado esperado:** aceitar as extensões, pois ausência de restrição não significa rejeitar todos os arquivos.
- **Limite:** isso não decide a nulabilidade da coleção Java nem o tratamento de nomes sem extensão.
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-07"></a>

### ACE-07 — Tratar associação incompatível

- **Origem:** [EXT-02](requisitos-e-regras.md#ext-02).
- **Condição e ação futuras:** tentar associar um arquivo incompatível com a restrição da Tag, por seleção ou Drop aprovado.
- **Resultado esperado:** informar a extensão e a Tag e oferecer criar nova etiqueta, adicionar a extensão à etiqueta atual ou cancelar. A associação incompatível não ocorre silenciosamente; cancelar não amplia a restrição nem autoriza a associação.
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-08"></a>

### ACE-08 — Confirmar efeitos de editar restrições

- **Origem:** [EXT-03](requisitos-e-regras.md#ext-03).
- **Condição e ação futuras:** editar as extensões de uma Tag de modo que o conjunto final torne arquivos associados incompatíveis.
- **Resultado esperado:** listar os afetados e aguardar confirmação antes de retirar as associações incompatíveis. Confirmar preserva outras Tags e arquivos físicos; perder a última Tag normal leva a `Etiqueta Ausente`. Não confirmar impede a retirada silenciosa.
- **Limite:** remover a última extensão configurada elimina a restrição; não torna todos incompatíveis. Apresentação e tratamento geral de lotes continuam em [P-12](decisoes-e-pendencias.md#p-12).
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-09"></a>

### ACE-09 — Confirmar nome repetido de Tag

- **Origem:** [TAG-01](requisitos-e-regras.md#tag-01).
- **Condição e ação futuras:** tentar criar outra Tag com um nome já existente.
- **Resultado esperado:** permitir prosseguir após a confirmação correspondente; a nova Tag tem UUID próprio e o nome não é UNIQUE.
- **Limite:** comparação de maiúsculas/minúsculas, espaços, vazio e outros detalhes de validação estão em [P-09](decisoes-e-pendencias.md#p-09).
- **Estado de validação:** NÃO EXECUTADO.

## Ciclo de vida e consultas

<a id="ace-10"></a>

### ACE-10 — Manter registro ao perder a última Tag normal

- **Origem:** [CIC-01](requisitos-e-regras.md#cic-01).
- **Condição e ação futuras:** retirar a última Tag normal no fluxo comum de reorganização durante a sessão.
- **Resultado esperado:** associar automaticamente `Etiqueta Ausente`, preservando o registro e o arquivo físico e permitindo continuar a reorganização.
- **Limite:** este cenário não define a modalidade 2 de exclusão ou a remoção explícita de uma referência indisponível; o alcance dessas ações está em [P-02](decisoes-e-pendencias.md#p-02).
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-11"></a>

### ACE-11 — Retirar a sentinela ao reclassificar

- **Origem:** [CIC-01](requisitos-e-regras.md#cic-01).
- **Condição e ação futuras:** adicionar uma Tag normal a um arquivo associado a `Etiqueta Ausente`.
- **Resultado esperado:** retirar automaticamente a associação com a Tag de sistema e preservar o mesmo registro com a nova classificação. A Tag de sistema permanece cadastrada.
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-12"></a>

### ACE-12 — Limpar somente na inicialização

- **Origem:** [CIC-02](requisitos-e-regras.md#cic-02).
- **Condição e ação futuras:** iniciar o aplicativo com registros cuja única Tag seja `Etiqueta Ausente` e registros defensivamente sem associações.
- **Resultado esperado:** remover esses `LocalFile` e suas associações pertinentes, sem apagar arquivos físicos; preservar `Etiqueta Ausente`, que fica vazia. Um registro com Tags normais não é removido apenas por estar indisponível.
- **Limite:** identificar tecnicamente a Tag protegida sem depender ingenuamente de nome repetível continua em [P-07](decisoes-e-pendencias.md#p-07). A condição é hipotética; não foram inseridos órfãos em banco.
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-13"></a>

### ACE-13 — Separar Refresh de limpeza

- **Origem:** [CIC-03](requisitos-e-regras.md#cic-03), [SYN-03](requisitos-e-regras.md#syn-03).
- **Condição e ação futuras:** acionar Refresh durante uma sessão que contenha um registro em `Etiqueta Ausente`.
- **Resultado esperado:** atualizar disponibilidade/metadados sem executar a limpeza de inicialização. A associação temporária e o registro continuam acessíveis na sessão. Troca de aba, filtros, navegação, notificação e reconexão também não autorizam essa limpeza.
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-14"></a>

### ACE-14 — Proteger `Etiqueta Ausente`, inclusive vazia

- **Origem:** [TAG-02](requisitos-e-regras.md#tag-02), [TAG-03](requisitos-e-regras.md#tag-03).
- **Condição e ação futuras:** tentar apagar a Tag de sistema diretamente ou pela funcionalidade de encontrar/excluir Tags vazias.
- **Resultado esperado:** impedir sua exclusão pelo usuário. A busca por vazias considera ausência de associações; uma Tag com arquivos associados indisponíveis não se torna vazia por ter zero disponíveis.
- **Limite:** o mecanismo de identidade da Tag protegida permanece em [P-07](decisoes-e-pendencias.md#p-07).
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-15"></a>

### ACE-15 — Consultar arquivos com AND e OR

- **Origem:** [EXP-04](interface-e-fluxos.md#exp-04).
- **Condição e ação futuras:** selecionar várias Tags e consultar arquivos alternando AND e OR.
- **Resultado esperado:** AND retorna arquivos com todas as Tags selecionadas; OR retorna arquivos com pelo menos uma. Não há NOT nem duplicação de `LocalFile` por corresponder a várias Tags.
- **Limite:** encaixe das APIs está em [P-05](decisoes-e-pendencias.md#p-05); consulta sem Tags selecionadas e ordenação estão em [P-13](decisoes-e-pendencias.md#p-13).
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-16"></a>

### ACE-16 — Enriquecer a visão local com correspondência em lote

- **Origem:** [EXP-03](interface-e-fluxos.md#exp-03).
- **Condição e ação futuras:** listar uma pasta com arquivos cadastrados e não cadastrados, aplicando os critérios físicos pertinentes.
- **Resultado esperado:** consultar correspondências persistidas em lote, obtendo `Map<Path, LocalFile>`; mostrar as Tags onde existir registro e preservar os `NativeFile` sem registro na listagem, deixando a área de Tags em branco quando não houver associação. Não exigir uma consulta SQL separada por arquivo.
- **Limite:** equivalência das chaves de caminho permanece em [P-08](decisoes-e-pendencias.md#p-08).
- **Estado de validação:** NÃO EXECUTADO.

## Operações, atualização e falhas

<a id="ace-17"></a>

### ACE-17 — Recortar e colar entre os exploradores

- **Origem:** [OP-03](requisitos-e-regras.md#op-03).
- **Condição e ação futuras:** recortar `prova.pdf` de `Faculdade` em `Arquivos por Tag` e colar em `~/Documentos/Faculdade` por `Arquivos Local`, conforme o exemplo aprovado.
- **Resultado esperado:** recortar registra intenção no clipboard interno e mantém a origem até colar. Após movimentação bem-sucedida, o arquivo físico está no destino e o registro conserva UUID e Tags com o novo caminho; a visão local mostra as Tags.
- **Limite:** o critério não garante atomicidade entre disco e banco. Estado do clipboard após colagem e repetição de CUT permanecem em [P-12](decisoes-e-pendencias.md#p-12).
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-18"></a>

### ACE-18 — Confirmar renomeação com extensão incompatível

- **Origem:** [OP-05](requisitos-e-regras.md#op-05).
- **Condição e ação futuras:** renomear um arquivo alterando a extensão de forma incompatível com alguma Tag atual.
- **Resultado esperado:** antes de concluir, oferecer remover as Tags incompatíveis, adicionar a nova extensão a elas ou cancelar. Preservar Tags compatíveis; aplicar `Etiqueta Ausente` se a remoção deixar o arquivo sem Tag normal. A renomeação normal preserva UUID e não converte o conteúdo.
- **Limite:** conflitos de nome e matriz de alternativas por operação não estão completamente definidos; ver [P-08](decisoes-e-pendencias.md#p-08) e [P-12](decisoes-e-pendencias.md#p-12).
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-19"></a>

### ACE-19 — Distinguir as três modalidades de exclusão

- **Origem:** [DEL-01](requisitos-e-regras.md#del-01), [DEL-02](requisitos-e-regras.md#del-02), [DEL-03](requisitos-e-regras.md#del-03), [DEL-04](requisitos-e-regras.md#del-04).
- **Condição e ação futuras:** abrir a exclusão de uma Tag cujos arquivos também possuem outras Tags.
- **Resultado esperado:** diferenciar excluir somente a etiqueta; retirar todas as etiquetas dos arquivos atingidos; excluir permanentemente esses arquivos. Antes de remover registros/arquivos, listar outras Tags afetadas e pedir confirmação adicional. Predefinidas comuns exigem confirmação reforçada; `Etiqueta Ausente` permanece protegida.
- **Limite obrigatório:** o resultado completo da modalidade 2 está **pendente em [P-02](decisoes-e-pendencias.md#p-02)**: remoção imediata ou adiamento dos registros e exclusão ou preservação da Tag selecionada. Estão confirmadas a preservação dos arquivos físicos nessa modalidade e a não exclusão global das outras Tags. Não atribuir a ela um resultado SQL definitivo.
- **Estado de validação:** NÃO EXECUTADO; a distinção visual é definida, mas o aceite completo da modalidade 2 depende da decisão pendente.

<a id="ace-20"></a>

### ACE-20 — Perguntar sobre herdar Tags na cópia

- **Origem:** [OP-04](requisitos-e-regras.md#op-04), [P-01](decisoes-e-pendencias.md#p-01).
- **Condição e ação futuras:** copiar um arquivo e escolher destino, contemplando o diálogo sobre receber as Tags do original.
- **Resultado esperado confirmado:** manter a origem física e perguntar sobre herdar Tags. Se houver conflito e a opção aplicável for confirmada, Substituir sobrescreve fisicamente o destino; Manter os dois preserva caminhos distintos; Cancelar não autoriza a ação conflitante ainda não realizada.
- **Limite obrigatório:** **P-01 continua aberta** para UUID/associações sobreviventes na substituição e para decidir se cópia sem Tags é apenas `NativeFile` ou recebe `LocalFile` temporário em `Etiqueta Ausente`. Não prever a origem sendo movida ou seu registro transferido como solução da cópia; não prometer reversão de etapas já concluídas.
- **Estado de validação:** NÃO EXECUTADO; o resultado SQL completo não pode ser aprovado enquanto P-01 não for resolvida.

<a id="ace-21"></a>

### ACE-21 — Um Refresh global sem duplicação

- **Origem:** [SYN-01](requisitos-e-regras.md#syn-01), [SYN-03](requisitos-e-regras.md#syn-03).
- **Condição e ação futuras:** acionar o botão Refresh único com os dois exploradores disponíveis.
- **Resultado esperado:** atualizar todos os `LocalFile` uma vez por solicitação global e atualizar as duas apresentações. Notificações do Observer não disparam outro Refresh completo por Screen. Troca de aba e aplicação de filtros também exigem atualização global; navegação de cada pasta não é gatilho global.
- **Limite:** não homologa um mecanismo de concorrência ou `SwingWorker`; prevenção interna de reentrância está em [P-12](decisoes-e-pendencias.md#p-12).
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-22"></a>

### ACE-22 — Informar uma falha parcial

- **Origem:** [ERR-01](requisitos-e-regras.md#err-01).
- **Condição e ação futuras:** observar, em uma futura verificação controlada, falha após uma etapa concluída, como o exemplo aprovado de mover fisicamente e falhar ao persistir o novo caminho.
- **Resultado esperado:** mensagem compreensível, indicação do que concluiu, do que falhou e área expansível de detalhes técnicos disponíveis. Informar SQLState, código MySQL ou código de processo somente quando existirem; encerrar Loading sem afirmar sucesso total ou reversão garantida.
- **Limite:** nenhuma falha foi provocada nesta missão. Não há teste de recuperação ou promessa de que `autoReconnect=true` repete ou desfaz a operação.
- **Estado de validação:** NÃO EXECUTADO.

## Ambiente local

<a id="ace-23"></a>

### ACE-23 — Preservar a configuração pública de referência

- **Origem:** [AMB-02](instalacao-e-execucao.md#amb-02), [AMB-03](instalacao-e-execucao.md#amb-03).
- **Condição e ação futuras:** conferir a configuração operacional quando os arquivos e a implementação existirem.
- **Resultado esperado:** dados em `database/runtime/data`, relativos à raiz do projeto, porta `3333`, endereço `127.0.0.1`, banco `tag_file`, usuário `root`, senha didática pública `TagFile123!` e `autoReconnect=true`. Não redefinir credenciais ou dados de outras instâncias.
- **Limite:** a presença desses valores na documentação não é validação do aplicativo. Não há configuração real de banco encontrada no repositório; versões e mecanismos de preparação seguem em [P-11](decisoes-e-pendencias.md#p-11).
- **Estado de validação:** NÃO EXECUTADO.

<a id="ace-24"></a>

### ACE-24 — Solicitar autorização para instalar ausências

- **Origem:** [AMB-01](instalacao-e-execucao.md#amb-01).
- **Condição e ação futuras:** abrir o aplicativo em ambiente preparado para verificação futura no qual falte um componente necessário do cliente ou servidor MySQL; contemplar recusa/cancelamento.
- **Resultado esperado:** identificar a ausência e solicitar autorização; Windows utiliza UAC na elevação prevista, e Linux contempla Ubuntu/Linux Mint. Cancelamento não é apresentado como instalação concluída. Falhas apresentam etapas e detalhes disponíveis.
- **Limite:** `pkexec` é proposta, não requisito fechado. Não foram instalados, removidos ou executados componentes para criar essa condição; detecção e instaladores permanecem em [P-11](decisoes-e-pendencias.md#p-11).
- **Estado de validação:** NÃO EXECUTADO.

## Dependências que impedem o aceite completo de certos fluxos

| Pendência | Parte confirmada que pode orientar cenários futuros | Resultado ainda não fechável |
|---|---|---|
| [P-01](decisoes-e-pendencias.md#p-01) | ACE-20: manter origem física, perguntar sobre Tags e sobrescrever fisicamente o destino quando aprovado | UUID e associações na cópia/substituição; cadastro da cópia sem Tags. |
| [P-02](decisoes-e-pendencias.md#p-02) | ACE-10/ACE-11 no fluxo comum; ACE-19 na clareza dos diálogos e na preservação física da modalidade 2 | Remoção imediata ou adiamento na modalidade 2 e remoção explícita de referência; destino da Tag selecionada. |
| [P-03](decisoes-e-pendencias.md#p-03) | [TAG-04](requisitos-e-regras.md#tag-04) e [UC-05](casos-de-uso.md#uc-05): sugestão ao criar `LocalFile`, com opção de não perguntar novamente apenas na sessão; nova inicialização volta a permitir a pergunta | Silenciar sem associar automaticamente ou repetir a resposta nos próximos arquivos; não há aceite de uma política presumida. |

O fluxo de sugestão não recebeu um novo identificador ACE neste documento: sua dependência foi mantida explícita para não apresentar o conjunto ACE-01 a ACE-24 como aprovação de todos os resultados do sistema. As verificações futuras de sugestão também estão **NÃO EXECUTADAS**. Múltiplas predefinidas compatíveis não têm critério final de escolha.

[P-04 a P-13](decisoes-e-pendencias.md#p-04) continuam pertinentes aos contratos, modelo físico, identidade de Tags especiais, caminhos, validações, datas, ambiente, lotes e consultas. Nenhuma delas foi resolvida pela redação dos cenários. A revisão textual da documentação e de seus links é distinta de executar estes critérios de aceite; o relatório de entrega documental está no [índice](../README.md).
