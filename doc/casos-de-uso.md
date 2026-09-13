# Casos de uso do Tag-File

[Índice da documentação](../README.md) · [Requisitos normativos](requisitos-e-regras.md) · [Interface](interface-e-fluxos.md)

Este documento organiza `UC-01` a `UC-15` a partir das regras consolidadas. Os casos descrevem o **comportamento planejado**; não são funcionalidades novas, testes executados ou evidência de implementação. A norma de cada assunto está nos documentos vinculados. [src/Main.java](../src/Main.java) ainda é um exemplo inicial de console e não implementa esses fluxos.

Nos casos a seguir, *associação* é o vínculo do registro com uma Tag, *registro* é o `LocalFile` no banco, e *arquivo físico* é o conteúdo no sistema de arquivos. Excluir um desses elementos não implica apagar os demais. *Tag normal* significa uma etiqueta diferente da sentinela protegida `Etiqueta Ausente`.

As sequências expressam efeitos funcionais; não fixam ordem técnica onde a especificação a deixou aberta. Cancelar uma etapa ainda não confirmada impede essa ação, mas não desfaz uma etapa anterior concluída. Falhas parciais seguem [ERR-01](requisitos-e-regras.md#err-01).

<a id="uc-01"></a>

## UC-01 — Preparar e abrir o Tag-File

**Origem:** [AMB-01](instalacao-e-execucao.md#amb-01) a [AMB-06](instalacao-e-execucao.md#amb-06), [CIC-02](requisitos-e-regras.md#cic-02), [SYN-01](requisitos-e-regras.md#syn-01). **Estado:** fluxo funcional confirmado; comandos específicos não homologados.

**Gatilho:** abertura do aplicativo.

**Sequência esperada:** ler configuração; verificar cliente e servidor MySQL; solicitar autorização para instalar ausências; preparar uma instância ainda não inicializada ou reutilizar seus dados; iniciar/reconhecer o servidor; preparar estrutura do banco; estabelecer conexão; executar limpeza exclusiva da inicialização; atualizar todos os registros remanescentes; disponibilizar exploradores.

**Efeitos:** dados existentes são reutilizados. Inicializar o servidor não deve apagar banco anterior. A limpeza de domínio remove apenas registros e associações nas condições de CIC-02, mantendo arquivos físicos e a sentinela. Reutilizar dados não autoriza recriar predefinidas apagadas pelo usuário ([TAG-02](requisitos-e-regras.md#tag-02)).

**Alternativas e falhas:** recusa ou cancelamento de instalação não significa sucesso. Falha em etapa deve apresentar resultado parcial. Erro de conexão não prova que o diretório precisa ser reinicializado. Recuperação de instalação ou schema parcialmente preparado, versões e comandos definitivos permanecem em [P-11](decisoes-e-pendencias.md#p-11). Identificação/preparação da sentinela e predefinidas permanece em [P-07](decisoes-e-pendencias.md#p-07).

**Limite desta entrega:** a sequência foi documentada, sem iniciar aplicativo, servidor, instalação ou SQL.

<a id="uc-02"></a>

## UC-02 — Navegar em Arquivos Local

**Origem:** [DOM-01](modelo-de-dominio.md#dom-01), [EXP-01](interface-e-fluxos.md#exp-01) a [EXP-03](interface-e-fluxos.md#exp-03), [UI-02](interface-e-fluxos.md#ui-02), [SYN-01](requisitos-e-regras.md#syn-01). **Estado:** confirmado.

**Gatilho:** abrir a visão local ou navegar para um `NativeDirectory`.

**Sequência:** listar conteúdo nativo; aplicar critérios físicos pertinentes; consultar em lote os registros correspondentes aos arquivos resultantes; obter `Map<Path, LocalFile>`; mostrar todos esses arquivos físicos com as Tags encontradas; deixar a região de etiquetas em branco quando não houver associação.

**Efeitos:** leitura do sistema de arquivos e do banco, sem criar `LocalFile` pela apresentação. Entrar em pasta não dispara, por si só, sincronização completa de todos os registros. O Map enriquece a apresentação e não elimina os arquivos desconhecidos pelo banco.

**Diretórios e limites:** podem ser mostrados para navegação e escolhidos como destino. Isso não aprova registrá-los como `LocalFile`, etiquetá-los ou executar operações recursivas. Normalização das chaves do Map e caminhos equivalentes estão em [P-08](decisoes-e-pendencias.md#p-08); estados vazios e ordenação em [P-13](decisoes-e-pendencias.md#p-13).

<a id="uc-03"></a>

## UC-03 — Pesquisar em Arquivos por Tag

**Origem:** [EXP-01](interface-e-fluxos.md#exp-01), [EXP-02](interface-e-fluxos.md#exp-02), [EXP-04](interface-e-fluxos.md#exp-04), [SYN-01](requisitos-e-regras.md#syn-01). **Estado:** regras AND/OR confirmadas, API e alguns estados da consulta abertos.

**Gatilho:** seleção de uma ou várias Tags e aplicação de critérios.

**Sequência:** antes de aplicar filtros, atualizar todos os `LocalFile`; consultar arquivos associados com AND ou OR; apresentar arquivos resultantes com suas Tags. A consulta não altera a organização física das pastas.

**Resultado:** lista de registros de arquivos sem duplicação de identidade. AND exige todas as Tags selecionadas; OR exige ao menos uma. NOT está fora do escopo.

**Pendências:** encaixe de `TagFilter` e consulta de associações em [P-05](decisoes-e-pendencias.md#p-05); resultado sem Tags selecionadas, estados vazios e ordenação em [P-13](decisoes-e-pendencias.md#p-13). `TagDAO.find(...)` não deve retornar arquivos contrariando `CrudDAO<Tag, TagFilter>`. Receber notificação de atualização não deve fazer a Screen iniciar outro Refresh global; a composição concreta da observação segue [P-04](decisoes-e-pendencias.md#p-04).

<a id="uc-04"></a>

## UC-04 — Criar uma Tag e associar arquivos iniciais

**Origem:** [TAG-01](requisitos-e-regras.md#tag-01), [TAG-02](requisitos-e-regras.md#tag-02), [EXT-01](requisitos-e-regras.md#ext-01), [OP-01](requisitos-e-regras.md#op-01), [ARQ-02](arquitetura-e-padroes.md#arq-02). **Estado:** fluxo confirmado; validações e partes da sugestão em aberto.

**Entrada:** nome, cor hexadecimal, restrições opcionais de extensão e seleção opcional de arquivos.

**Sequência:** configurar Tag; normalizar extensões; pedir confirmação se houver possível nome repetido; permitir selecionar arquivos de uma pasta com `JFileChooser` e `FileNameExtensionFilter`; reutilizar registros existentes e criar os necessários pela Factory; persistir Tag, extensões e associações conforme a operação confirmada. A Tag pode ser criada vazia.

**Exemplo didático:** na pasta `~/Jogos/Megaman`, com restrição `.pdf`, aparecem `Megaman-Manual.pdf` e `prova.pdf`. O usuário pode marcar somente `prova.pdf`, mantendo o outro sem a nova Tag. Não há associação obrigatória de toda a pasta.

**Sugestão adicional:** criar novo `LocalFile` aciona a possibilidade de sugerir predefinida compatível ([TAG-04](requisitos-e-regras.md#tag-04)). “Não perguntar novamente nesta sessão” não persiste entre inicializações; seu efeito sobre os próximos arquivos continua em [P-03](decisoes-e-pendencias.md#p-03).

**Cancelamento e falhas:** não confirmar criação ou seleção não autoriza cadastrar silenciosamente a lista mostrada. Ordem técnica de persistência e reversão de criação parcial não foram definidas; etapa persistida seguida de falha exige comunicação por ERR-01.

**Limites e pendências:** não criar Tag artificial por pasta, importar subpastas ou duplicar `LocalFile` para o mesmo caminho. Seleção múltipla está aceita neste fluxo inicial, sem estabelecer uma política geral de lotes ([P-12](decisoes-e-pendencias.md#p-12)). Comparação/validação de nome e cor seguem [P-09](decisoes-e-pendencias.md#p-09), identificação de predefinidas segue [P-07](decisoes-e-pendencias.md#p-07), e DDL físico segue [P-06](decisoes-e-pendencias.md#p-06).

<a id="uc-05"></a>

## UC-05 — Associar arquivo a Tag existente

**Origem:** [OP-01](requisitos-e-regras.md#op-01), [EXT-02](requisitos-e-regras.md#ext-02), [DOM-02](modelo-de-dominio.md#dom-02), [CIC-01](requisitos-e-regras.md#cic-01), [TAG-04](requisitos-e-regras.md#tag-04). **Estado:** fluxo confirmado; sugestão parcialmente pendente.

**Gatilho:** seleção individual ou Drop de arquivo sobre a representação de uma Tag, com origem no explorador do sistema ou interno.

**Sequência:** identificar arquivo e Tag pelos dados adequados; verificar extensão; se incompatível, informar extensão e Tag e oferecer criar nova etiqueta, adicionar extensão à atual ou cancelar; reutilizar ou criar registro; criar associação aprovada; retirar a associação com `Etiqueta Ausente` ao aplicar uma Tag normal.

**Identidade e efeitos:** se o caminho está cadastrado, usar o mesmo UUID. A associação modifica a classificação persistida, sem duplicar ou mover o conteúdo físico. Nomes repetidos de Tag não substituem a identificação por UUID.

**Sugestão adicional:** quando um novo `LocalFile` for criado, apresentar possibilidade de predefinida compatível. O efeito do silêncio permanece em [P-03](decisoes-e-pendencias.md#p-03); não há base para repetir a sugestão em toda consulta de um registro existente ou escolher automaticamente entre várias predefinidas compatíveis.

**Cancelamento:** não associar extensão incompatível nem ampliar a Tag sem decisão. Criar uma Tag no diálogo não autoriza outras associações não confirmadas. A política por lote continua em [P-12](decisoes-e-pendencias.md#p-12); equivalência de caminhos em [P-08](decisoes-e-pendencias.md#p-08).

<a id="uc-06"></a>

## UC-06 — Remover uma associação durante a reorganização

**Origem:** [CIC-01](requisitos-e-regras.md#cic-01), [DEL-01](requisitos-e-regras.md#del-01) quando aplicável, [UI-02](interface-e-fluxos.md#ui-02). **Estado:** confirmado para reorganização durante a sessão.

**Entrada:** registro e associação escolhida.

**Sequência:** retirar a associação solicitada; se restarem Tags normais, preservar registro com elas; se não restar nenhuma, associar `Etiqueta Ausente`.

**Efeitos:** arquivo físico intacto, registro ainda acessível durante a sessão e mesmo UUID. Adicionar uma nova Tag normal retira a sentinela. Na próxima inicialização, a limpeza tem o alcance de [CIC-02](requisitos-e-regras.md#cic-02), sem apagar arquivos físicos.

**Limite:** este caso não resolve a modalidade explícita de remoção de todos os registros. Essa relação continua em [P-02](decisoes-e-pendencias.md#p-02).

<a id="uc-07"></a>

## UC-07 — Editar uma Tag

**Origem:** [TAG-01](requisitos-e-regras.md#tag-01), [EXT-01](requisitos-e-regras.md#ext-01), [EXT-03](requisitos-e-regras.md#ext-03), [DOM-04](modelo-de-dominio.md#dom-04). **Estado:** edição confirmada, com detalhes de validação pendentes.

**Entrada:** Tag identificada por UUID e novos dados, incluindo nome, cor e extensões. A identidade permanece ligada ao UUID, não ao nome.

**Para extensões:** considerar conjunto final; identificar arquivos incompatíveis; listar afetados e aguardar confirmação; remover somente associações incompatíveis se autorizado; aplicar `Etiqueta Ausente` aos registros que perderem a última Tag normal. Sem confirmação não há retirada silenciosa.

Remover a última extensão configurada significa ausência de restrição. Passar de irrestrita para uma lista específica também exige avaliar incompatibilidades, sem um diálogo especial adicional homologado.

**Preservação:** outras Tags, outros arquivos e conteúdo físico permanecem, salvo operações separadas explicitamente solicitadas.

**Pendências:** validação de nome/cor em [P-09](decisoes-e-pendencias.md#p-09), editabilidade de campos especiais de `Etiqueta Ausente` em [P-07](decisoes-e-pendencias.md#p-07), efeito de ações sobre `lastFileTaggedAt` em [P-10](decisoes-e-pendencias.md#p-10) e apresentação/tratamento em lote em [P-12](decisoes-e-pendencias.md#p-12).

<a id="uc-08"></a>

## UC-08 — Copiar arquivo, com ou sem substituição

**Origem:** [OP-04](requisitos-e-regras.md#op-04), [OP-06](requisitos-e-regras.md#op-06), [OP-08](requisitos-e-regras.md#op-08), [P-01](decisoes-e-pendencias.md#p-01). **Estado:** efeitos físicos e pergunta de herança confirmados; resultado de identidade incompleto.

**Gatilho:** copiar/colar arquivo com destino escolhido. O comando de cópia não exige clipboard do sistema operacional.

**Efeitos confirmados:** manter origem física; produzir cópia; perguntar sobre herdar Tags; se houver conflito, oferecer alternativas aprovadas conforme aplicáveis. “Substituir” sobrescreve fisicamente o destino; “Manter os dois” exige caminhos distintos. A ordem técnica entre cópia, perguntas e etapas de persistência não está fechada.

**Exemplo de decisão ainda aberta:** origem O registrada em `/Documentos/prova.pdf` e destino D registrado em `/Backup/prova.pdf`. Após copiar com substituição, qual UUID fica em cada caminho e qual registro é excluído? Se o “LocalFile do arquivo copiado” designa um registro da cópia já criado, ele apenas assume o caminho final, sem gerar outro UUID?

Os exemplos históricos alternaram papéis de A, B e C. Houve orientações de preservar `LocalFile`/UUID/Tags do arquivo copiado e de somente sobrescrever o caminho de `LocalFileB`. Uma resposta posterior moveu o registro da origem como se cópia fosse movimentação; isso não resolveu a decisão. O histórico e as perguntas estão em [P-01](decisoes-e-pendencias.md#p-01).

**Outra pendência:** cópia sem herdar Tags fica apenas como `NativeFile` ou recebe `LocalFile` temporário com `Etiqueta Ausente`? Nenhuma alternativa está homologada.

**Cancelamento e falha:** cancelar etapa pendente não reverte automaticamente cópia já criada. Informar resultados parciais por ERR-01. Nomes de conflito estão em [P-08](decisoes-e-pendencias.md#p-08); clipboard após colagem e lotes, em [P-12](decisoes-e-pendencias.md#p-12). O caso não escolhe UUID sobrevivente, não altera origem como em mover e não afirma resultado SQL completo.

<a id="uc-09"></a>

## UC-09 — Recortar em uma visão e colar na outra

**Origem:** [OP-03](requisitos-e-regras.md#op-03), [OP-08](requisitos-e-regras.md#op-08), [EXP-01](interface-e-fluxos.md#exp-01). **Estado:** confirmado no cenário principal.

**Cenário:** selecionar `prova.pdf` na Tag `Faculdade`, recortar e colar em `~/Documentos/Faculdade` pela visão `Arquivos Local`.

**Antes da colagem:** arquivo permanece na origem e clipboard interno guarda intenção CUT.

**Após sucesso:** arquivo está no destino, mesmo registro mantém UUID e Tags, caminho conhecido é atualizado e apresentação local exibe essas Tags.

**Colaboradores:** Screen, Controller de origem, `ClipboardService`, Controller que recebe a colagem, Command/serviço nativo e persistência pertinente. Compartilhar colaboradores não exige dependência direta entre Controllers. Arquivos sem cadastro também podem ser manipulados pela visão local, sem criar SQL apenas para isso.

**Exceções e limites:** conflito segue [OP-06](requisitos-e-regras.md#op-06); referência indisponível segue [OP-02](requisitos-e-regras.md#op-02); falha entre disco/banco segue ERR-01, sem atomicidade. Repetição de CUT e limpeza do clipboard após sucesso não estão fechadas ([P-12](decisoes-e-pendencias.md#p-12)).

<a id="uc-10"></a>

## UC-10 — Renomear arquivo

**Origem:** [OP-05](requisitos-e-regras.md#op-05), [EXT-02](requisitos-e-regras.md#ext-02)/[EXT-03](requisitos-e-regras.md#ext-03) por compatibilidade, [CIC-01](requisitos-e-regras.md#cic-01). **Estado:** confirmado no fluxo normal.

**Entrada:** arquivo escolhido e novo nome.

**Sem mudança de extensão:** preservar identidade do registro, atualizar representação e caminho conforme a operação física.

**Com mudança de extensão:** identificar Tags incompatíveis e oferecer retirar essas Tags, adicionar a nova extensão a elas ou cancelar. Prosseguir somente conforme a decisão e preservar associações compatíveis. Se retirar as incompatíveis eliminar a última normal, aplicar CIC-01.

**Exemplo didático:** `prova.pdf`, com `PDF` e `Faculdade` sem restrição, passa a `prova.txt`. Se o usuário escolher retirar incompatíveis, `Faculdade` permanece. Se não houver outra Tag normal, o registro recebe `Etiqueta Ausente`.

**Limites:** renomear não converte o conteúdo. Conflitos seguem OP-06, sem matriz de opções por operação homologada. Nomes especiais, extensões compostas/ausentes seguem [P-08](decisoes-e-pendencias.md#p-08); lotes seguem [P-12](decisoes-e-pendencias.md#p-12). Falha parcial não implica restauração automática do nome anterior.

<a id="uc-11"></a>

## UC-11 — Relocalizar ou remover referência indisponível

**Origem:** [OP-02](requisitos-e-regras.md#op-02), [DOM-02](modelo-de-dominio.md#dom-02), [SYN-01](requisitos-e-regras.md#syn-01), [P-02](decisoes-e-pendencias.md#p-02). **Estado:** fluxo principal confirmado; colisão e alcance de remoção abertos.

**Gatilho:** tentativa de usar arquivo não encontrado no caminho salvo.

**Alternativas:** localizar outro caminho, remover do Tag-File ou cancelar.

**Relocalização bem-sucedida:** registro conserva UUID e Tags e referencia novo endereço indicado pelo usuário. É atualização da referência; não significa descoberta automática de movimento externo pelo aplicativo.

**Pendências:** colisão com registro existente no caminho escolhido em [P-08](decisoes-e-pendencias.md#p-08); relação da remoção explícita com adiamento de CIC-01 em [P-02](decisoes-e-pendencias.md#p-02). Não se fundem registros nem se apaga arquivo físico por inferência. Cancelar não autoriza relocalização ou remoção pendente.

<a id="uc-12"></a>

## UC-12 — Excluir Tag em uma das três modalidades

**Origem:** [DEL-01](requisitos-e-regras.md#del-01) a [DEL-04](requisitos-e-regras.md#del-04), [CIC-01](requisitos-e-regras.md#cic-01), [P-02](decisoes-e-pendencias.md#p-02). **Estado:** modalidades 1 e 3 confirmadas; alcance final da modalidade 2 pendente.

**Entrada:** Tag selecionada por identidade e modalidade explicitamente escolhida.

| Modalidade | Tag selecionada | Associações | LocalFile | Arquivo físico |
|---|---|---|---|---|
| Apenas a etiqueta | Excluir | Retirar somente as dessa Tag | Preservar; aplicar sentinela se perder a última normal | Preservar |
| Todas as etiquetas dos arquivos da selecionada | Pendente se exclui ou fica vazia | Originalmente: remover todas dos registros atingidos | Originalmente: excluir; adiamento em P-02 | Preservar |
| Arquivos permanentemente | Excluir | Remover todas dos registros atingidos | Excluir | Apagar permanentemente |

**Confirmações:** antes de afetar outras Tags por remoção de registro/arquivo, listar essas etiquetas e pedir confirmação adicional. Reforçar confirmação para predefinidas comuns. A Tag protegida `Etiqueta Ausente` não pode ser excluída, mesmo vazia.

**Preservação:** outras Tags não são apagadas globalmente; seus demais arquivos não são atingidos. Na modalidade 2, os arquivos físicos permanecem. Sua remoção de registros imediata versus uso da sentinela, assim como o destino da Tag selecionada, continuam em [P-02](decisoes-e-pendencias.md#p-02).

**Falha:** não há execução atômica ou reversão garantida. Informar etapas concluídas e falhas sem apresentar sucesso integral. Ordem técnica e política de lotes permanecem em [P-12](decisoes-e-pendencias.md#p-12).

<a id="uc-13"></a>

## UC-13 — Localizar Tags vazias

**Origem:** [TAG-03](requisitos-e-regras.md#tag-03), [TAG-02](requisitos-e-regras.md#tag-02). **Estado:** finalidade confirmada; apresentação não definida.

**Gatilho:** funcionalidade para encontrar etiquetas sem associações e permitir removê-las.

**Resultado:** identificar ausência real de associações. Uma Tag com somente arquivos indisponíveis não está vazia. A contagem de disponíveis é consultada quando necessária e não é contador persistido.

**Proteções:** `Etiqueta Ausente` não pode ser apagada. Confirmações aplicáveis às demais Tags continuam valendo, inclusive reforço para predefinidas comuns.

**Não definido:** disposição visual, filtros auxiliares, seleção múltipla de Tags vazias e automação dessa limpeza ([P-12](decisoes-e-pendencias.md#p-12), [P-13](decisoes-e-pendencias.md#p-13)). A funcionalidade não é exclusão automática recorrente.

<a id="uc-14"></a>

## UC-14 — Refresh e filtros

**Origem:** [SYN-01](requisitos-e-regras.md#syn-01) a [SYN-03](requisitos-e-regras.md#syn-03), [UI-03](interface-e-fluxos.md#ui-03), [ARQ-05](arquitetura-e-padroes.md#arq-05) a [ARQ-07](arquitetura-e-padroes.md#arq-07). **Estado:** interação e alcance confirmados; mecanismo interno parcialmente aberto.

**Gatilho:** botão Refresh único, troca de aba ou aplicação de filtros. A inicialização também atualiza todos, com sua limpeza própria.

**Sequência:** mostrar Loading; coordenar uma atualização de todos os `LocalFile`; executar consulta pretendida quando houver; publicar evento pertinente pelo Controller; atualizar apresentação das duas visões; encerrar Loading conforme sucesso ou falha.

**Invariantes:** não limpar registros por `Etiqueta Ausente`; não atualizar todos de novo ao receber evento em cada Screen; não disparar Refresh por toda navegação de pasta. A verificação antes de utilizar um arquivo é pontual.

**Implementação interna:** `SwingWorker` foi alternativa explicada, sem obrigatoriedade confirmada. A UI deve permanecer responsiva durante Loading, sem introduzir pool. Mecanismo de fila/reentrância não foi fechado ([P-12](decisoes-e-pendencias.md#p-12)); ligação observadora das Screens e assinaturas estão em [P-04](decisoes-e-pendencias.md#p-04)/[P-05](decisoes-e-pendencias.md#p-05). Metadados ausentes e conversões continuam em [P-10](decisoes-e-pendencias.md#p-10).

<a id="uc-15"></a>

## UC-15 — Apresentar falha parcial

**Origem:** [ERR-01](requisitos-e-regras.md#err-01), [SQL-05](banco-de-dados.md#sql-05), [AMB-05](instalacao-e-execucao.md#amb-05). **Estado:** comunicação confirmada; recuperação completa não definida.

**Gatilho:** falha em consulta, etapa física, persistência ou processo externo.

**Resultado esperado:** mensagem compreensível, etapas concluídas, etapa com falha e detalhes disponíveis em área expansível. Encerrar Loading para não deixar UI bloqueada indefinidamente. SQLState, código MySQL ou saída de processo só aparecem quando realmente disponíveis e pertinentes.

**Exemplo didático, não executado:** o arquivo chegou à pasta de destino, mas o MySQL falhou ao salvar seu novo caminho. O pop-up distingue essas duas etapas, sem afirmar que o arquivo voltou à origem.

**Limites:** `autoReconnect=true` não desfaz nem repete uma operação por garantia. Não há recuperação física ou transação entre disco e banco. Falha de instalação não autoriza apagar dados existentes. Política de continuar/parar lotes permanece em [P-12](decisoes-e-pendencias.md#p-12), e recuperação do ambiente em [P-11](decisoes-e-pendencias.md#p-11).

Exemplos de diagnóstico não incluem segredos reais e não são resultados de execução. Os [cenários de aceite](criterios-de-aceite.md) são material para verificação futura, sem testes executados nesta missão.
