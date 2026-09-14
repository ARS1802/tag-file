# Critérios de aceite: como conferir o comportamento

[Índice](README.md) · [Casos de uso](casos-de-uso.md) · [Ordem de construção](arquitetura-e-padroes.md#orientacao-construcao)

Um critério de aceite descreve uma situação e o resultado que a funcionalidade precisa produzir. Os 24 cenários abaixo estão previstos para validação após a implementação; não representam testes já executados nem uma rubrica adicional da disciplina.

Para cada cenário, prepare a condição, execute a ação e compare o resultado. Registre a evidência quando fizer a verificação. Se houver uma pendência indicada, seu resultado completo depende da decisão correspondente.

<a id="ace-01"></a>

## ACE-01 — Navegar sem cadastrar todos os arquivos

- **Regras:** [DOM-01](modelo-de-dominio.md#dom-01), [EXP-01](interface-e-fluxos.md#exp-01).
- **Condição:** Pasta com arquivos sem registro.
- **Ação:** Abrir ou navegar para a pasta em Arquivos Local.
- **Resultado esperado:** Abrir uma pasta mostra arquivos sem criar LocalFile para cada um.

<a id="ace-02"></a>

## ACE-02 — Reutilizar o cadastro ao associar outra Tag

- **Regras:** [DOM-02](modelo-de-dominio.md#dom-02), [OP-01](requisitos-e-regras.md#op-01).
- **Condição:** Caminho já cadastrado e Tag a associar.
- **Ação:** Confirmar a associação e comparar a identidade do registro.
- **Resultado esperado:** Associar Tag a um caminho cadastrado reutiliza o registro e UUID.
- **Limites e consequências:** O cenário básico considera o mesmo caminho. Equivalência, links e normalização dependem de [P-08](decisoes-e-pendencias.md#p-08).

<a id="ace-03"></a>

## ACE-03 — Distinguir arquivos de mesmo nome em pastas diferentes

- **Regras:** [DOM-02](modelo-de-dominio.md#dom-02).
- **Condição:** Arquivos homônimos em pastas diferentes.
- **Ação:** Classificá-los ou apresentá-los e distinguir os caminhos e registros.
- **Resultado esperado:** Arquivos homônimos em pastas diferentes podem aparecer como arquivos distintos.
- **Limites e consequências:** Não há deduplicação por conteúdo aprovada; [P-08](decisoes-e-pendencias.md#p-08) mantém caminhos equivalentes abertos.

<a id="ace-04"></a>

## ACE-04 — Aceitar múltiplas extensões específicas

- **Regras:** [EXT-01](requisitos-e-regras.md#ext-01).
- **Condição:** Tag com mais de uma extensão específica, incluindo .cdr.
- **Ação:** Configurar as restrições e selecionar arquivos compatíveis.
- **Resultado esperado:** Uma Tag pode aceitar múltiplas extensões específicas, incluindo .cdr.

<a id="ace-05"></a>

## ACE-05 — Normalizar extensões sem duplicação

- **Regras:** [EXT-01](requisitos-e-regras.md#ext-01), [SQL-02](banco-de-dados.md#sql-02).
- **Condição:** Entradas PDF e .PDF para extensões da mesma Tag.
- **Ação:** Confirmar a configuração e verificar o conjunto normalizado.
- **Resultado esperado:** PDF e .PDF são normalizados para .pdf; a extensão não se duplica na mesma Tag.
- **Limites e consequências:** A unicidade por Tag está definida, mas a constraint física específica segue [P-06](decisoes-e-pendencias.md#p-06). Tags distintas podem usar a mesma extensão.

<a id="ace-06"></a>

## ACE-06 — Aceitar qualquer extensão quando não há restrição

- **Regras:** [EXT-01](requisitos-e-regras.md#ext-01).
- **Condição:** Tag sem extensões configuradas.
- **Ação:** Associar arquivos com extensões diferentes.
- **Resultado esperado:** Tag sem restrição aceita extensões diferentes.
- **Limites e consequências:** Não decide nulabilidade da coleção Java nem casos especiais de arquivos sem extensão ([P-08](decisoes-e-pendencias.md#p-08)).

<a id="ace-07"></a>

## ACE-07 — Tratar associação incompatível

- **Regras:** [EXT-02](requisitos-e-regras.md#ext-02).
- **Condição:** Arquivo incompatível com uma Tag restrita.
- **Ação:** Tentar associá-lo por seleção ou Drop e verificar as alternativas.
- **Resultado esperado:** Associação incompatível oferece criar Tag, adicionar extensão ou cancelar.
- **Limites e consequências:** Cancelar não amplia a Tag nem autoriza associação incompatível. Lotes permanecem em [P-12](decisoes-e-pendencias.md#p-12).

<a id="ace-08"></a>

## ACE-08 — Confirmar o efeito de editar restrições

- **Regras:** [EXT-03](requisitos-e-regras.md#ext-03).
- **Condição:** Tag cuja edição de restrições afetaria arquivos associados.
- **Ação:** Alterar o conjunto final de extensões e observar a confirmação antes das retiradas.
- **Resultado esperado:** Editar restrições lista os arquivos que perderiam compatibilidade e aguarda confirmação.
- **Limites e consequências:** Retirar somente associações incompatíveis confirmadas; preservar outras Tags e disco; aplicar [CIC-01](requisitos-e-regras.md#cic-01) quando necessário. Remover a última extensão configurada elimina a restrição. [P-12](decisoes-e-pendencias.md#p-12) mantém apresentação em lote aberta.

<a id="ace-09"></a>

## ACE-09 — Confirmar nome repetido de Tag

- **Regras:** [TAG-01](requisitos-e-regras.md#tag-01).
- **Condição:** Tag já existente com o nome informado.
- **Ação:** Tentar criar outra com esse nome e confirmar ou cancelar.
- **Resultado esperado:** Nome de Tag repetido permite prosseguir somente após a confirmação correspondente.
- **Limites e consequências:** Nome não é UNIQUE; a nova Tag possui identidade própria. Comparação de caixa/espaços e validação em [P-09](decisoes-e-pendencias.md#p-09).

<a id="ace-10"></a>

## ACE-10 — Manter o cadastro ao retirar a última Tag normal

- **Regras:** [CIC-01](requisitos-e-regras.md#cic-01).
- **Condição:** Registro classificado no fluxo comum de reorganização da sessão.
- **Ação:** Retirar sua última Tag normal.
- **Resultado esperado:** Retirar a última Tag normal durante reorganização associa Etiqueta Ausente.
- **Limites e consequências:** Não resolve [DEL-02](requisitos-e-regras.md#del-02) ou remoção explícita de [OP-02](requisitos-e-regras.md#op-02); o alcance permanece em [P-02](decisoes-e-pendencias.md#p-02).

<a id="ace-11"></a>

## ACE-11 — Retirar a sentinela ao receber uma Tag normal

- **Regras:** [CIC-01](requisitos-e-regras.md#cic-01).
- **Condição:** Registro associado a Etiqueta Ausente.
- **Ação:** Adicionar uma Tag normal.
- **Resultado esperado:** Adicionar Tag normal remove a associação temporária com Etiqueta Ausente.
- **Limites e consequências:** Retirar somente a associação temporária; a Tag de sistema permanece.

<a id="ace-12"></a>

## ACE-12 — Limpar cadastros sem classificação na inicialização

- **Regras:** [CIC-02](requisitos-e-regras.md#cic-02).
- **Condição:** Na abertura, registros somente na sentinela e registros sem associação.
- **Ação:** Executar a inicialização do domínio e observar os registros remanescentes.
- **Resultado esperado:** Na inicialização, registros somente na sentinela e órfãos são removidos sem apagar os arquivos físicos.
- **Limites e consequências:** Indisponibilidade não é motivo isolado de exclusão. Identificação da sentinela está em [P-07](decisoes-e-pendencias.md#p-07).

<a id="ace-13"></a>

## ACE-13 — Preservar a sentinela durante Refresh

- **Regras:** [CIC-03](requisitos-e-regras.md#cic-03), [SYN-03](requisitos-e-regras.md#syn-03).
- **Condição:** Sessão com registro em Etiqueta Ausente.
- **Ação:** Acionar o Refresh compartilhado.
- **Resultado esperado:** Refresh não executa a limpeza de inicialização.
- **Limites e consequências:** Reconexão, filtros, troca de aba, navegação e Observer também não executam a limpeza da inicialização ([CIC-03](requisitos-e-regras.md#cic-03)).

<a id="ace-14"></a>

## ACE-14 — Proteger Etiqueta Ausente mesmo vazia

- **Regras:** [TAG-02](requisitos-e-regras.md#tag-02), [TAG-03](requisitos-e-regras.md#tag-03).
- **Condição:** Tag de sistema, inclusive quando vazia.
- **Ação:** Tentar sua exclusão direta e pela funcionalidade de localizar Tags vazias.
- **Resultado esperado:** Etiqueta Ausente não pode ser apagada, nem pela seleção de Tags vazias.
- **Limites e consequências:** Zero disponíveis não equivale a zero associações. O mecanismo de identidade especial está em [P-07](decisoes-e-pendencias.md#p-07).

<a id="ace-15"></a>

## ACE-15 — Pesquisar por AND e OR

- **Regras:** [EXP-04](interface-e-fluxos.md#exp-04).
- **Condição:** Uma ou várias Tags selecionadas.
- **Ação:** Aplicar AND e OR e comparar os arquivos apresentados.
- **Resultado esperado:** AND exige todas as Tags selecionadas e OR exige ao menos uma.
- **Limites e consequências:** Não incluir NOT ou duplicar LocalFile. Retorno/API em [P-05](decisoes-e-pendencias.md#p-05); nenhuma Tag selecionada e ordenação em [P-13](decisoes-e-pendencias.md#p-13).

<a id="ace-16"></a>

## ACE-16 — Mostrar cadastrados e não cadastrados na visão local

- **Regras:** [EXP-03](interface-e-fluxos.md#exp-03).
- **Condição:** Pasta com arquivos cadastrados e não cadastrados.
- **Ação:** Listar e aplicar critérios físicos; consultar correspondências em lote.
- **Resultado esperado:** A correspondência Map acrescenta etiquetas aos NativeFile sem eliminar os não registrados.
- **Limites e consequências:** Manter todos os NativeFile resultantes; Tags em branco quando não houver associação. Assinatura em lote e equivalência das chaves seguem abertas, com [P-08](decisoes-e-pendencias.md#p-08).

<a id="ace-17"></a>

## ACE-17 — Recortar e colar preservando UUID e Tags

- **Regras:** [OP-03](requisitos-e-regras.md#op-03).
- **Condição:** prova.pdf em Faculdade, selecionado em Arquivos por Tag.
- **Ação:** Recortar, navegar por Arquivos Local até ~/Documentos/Faculdade e colar.
- **Resultado esperado:** Recortar em Arquivos por Tag e colar em Arquivos Local preserva UUID e Tags do arquivo movido.
- **Limites e consequências:** Recortar não move imediatamente. Após sucesso, caminho muda; UUID e Tags permanecem. Não há atomicidade disco/SQL. Clipboard após colagem e repetição de CUT seguem [P-12](decisoes-e-pendencias.md#p-12).

<a id="ace-18"></a>

## ACE-18 — Renomear com mudança de extensão

- **Regras:** [OP-05](requisitos-e-regras.md#op-05).
- **Condição:** Arquivo cuja nova extensão seria incompatível com Tags atuais.
- **Ação:** Solicitar renomeação e observar as alternativas antes de concluir.
- **Resultado esperado:** Renomeação com extensão incompatível apresenta as três alternativas aprovadas.
- **Limites e consequências:** Preservar Tags compatíveis e identidade na renomeação normal. Não converter conteúdo; conflitos e casos especiais seguem [P-08](decisoes-e-pendencias.md#p-08)/[P-12](decisoes-e-pendencias.md#p-12).

<a id="ace-19"></a>

## ACE-19 — Distinguir as três modalidades de exclusão

- **Regras:** [DEL-01](requisitos-e-regras.md#del-01) a [DEL-04](requisitos-e-regras.md#del-04).
- **Condição:** Tag cujos arquivos também possuem outras Tags.
- **Ação:** Escolher entre as três modalidades e observar informações e confirmações.
- **Resultado esperado:** A UI distingue as três exclusões e alerta sobre outras Tags afetadas.
- **Limites e consequências:** [P-02](decisoes-e-pendencias.md#p-02) impede fechar o resultado completo da modalidade 2: remoção imediata ou adiamento e destino da Tag selecionada. Estão definidos preservar o disco e não apagar globalmente outras Tags. Predefinidas comuns exigem reforço; sentinela não pode ser excluída.

<a id="ace-20"></a>

## ACE-20 — Perguntar sobre herança de Tags na cópia

- **Regras:** [OP-04](requisitos-e-regras.md#op-04), [P-01](decisoes-e-pendencias.md#p-01).
- **Condição:** Arquivo a copiar, com destino escolhido e eventual conflito.
- **Ação:** Copiar/colar e observar a pergunta sobre herdar Tags e as escolhas de conflito.
- **Resultado esperado:** Copiar pergunta sobre herdar Tags; a origem permanece. O resultado SQL completo depende de P-01.
- **Limites e consequências:** [P-01](decisoes-e-pendencias.md#p-01) impede fechar UUID/associações na substituição e cadastro de cópia sem Tags. A origem física permanece; não aplicar resultado de movimento à cópia nem prometer reversão de etapas concluídas.

<a id="ace-21"></a>

## ACE-21 — Atualizar as duas apresentações com um Refresh

- **Regras:** [SYN-01](requisitos-e-regras.md#syn-01), [SYN-03](requisitos-e-regras.md#syn-03).
- **Condição:** Dois exploradores disponíveis na sessão.
- **Ação:** Acionar o botão Refresh único e observar o alcance e a atualização visual.
- **Resultado esperado:** O único Refresh atualiza todos os LocalFile e permite atualizar as duas apresentações sem duplicação.
- **Limites e consequências:** Atualizar todos os LocalFile uma vez por solicitação; Observer não gera outro Refresh por Screen. [P-04](decisoes-e-pendencias.md#p-04)/[P-05](decisoes-e-pendencias.md#p-05)/[P-12](decisoes-e-pendencias.md#p-12) preservam contratos e mecanismo interno abertos.

<a id="ace-22"></a>

## ACE-22 — Apresentar o resultado de uma falha parcial

- **Regras:** [ERR-01](requisitos-e-regras.md#err-01).
- **Condição:** Falha posterior a uma etapa concluída, no exemplo de mover no disco e falhar ao salvar caminho.
- **Ação:** Em uma verificação controlada, observar a mensagem e a saída de Loading.
- **Resultado esperado:** Falha parcial mostra o concluído, a falha e detalhes expansíveis.
- **Limites e consequências:** Incluir detalhes técnicos somente quando disponíveis; não garantir rollback, restauração ou repetição por autoReconnect.

<a id="ace-23"></a>

## ACE-23 — Usar os parâmetros da instância local

- **Regras:** [AMB-02](instalacao-e-execucao.md#amb-02), [AMB-03](instalacao-e-execucao.md#amb-03).
- **Condição:** Configuração operacional que será construída para a instância local.
- **Ação:** Conferir parâmetros e a localização dos dados em relação à raiz do projeto.
- **Resultado esperado:** A configuração de referência usa dados dentro de database/runtime/data, porta 3333 e credenciais públicas definidas.
- **Limites e consequências:** Referência completa em [AMB-03](instalacao-e-execucao.md#amb-03): 127.0.0.1, 3333, tag_file, root, senha didática pública TagFile123!, autoReconnect=true e database/runtime/data. Não alterar outras instâncias. Versões em [P-11](decisoes-e-pendencias.md#p-11).

<a id="ace-24"></a>

## ACE-24 — Tratar recusa ou cancelamento da instalação

- **Regras:** [AMB-01](instalacao-e-execucao.md#amb-01).
- **Condição:** Ambiente futuro no qual falte cliente ou servidor MySQL necessário.
- **Ação:** Abrir a aplicação e contemplar recusa ou cancelamento da autorização de instalação.
- **Resultado esperado:** Ausência de componentes solicita autorização para instalar; cancelamento não é apresentado como sucesso.
- **Limites e consequências:** UAC está previsto no Windows; Linux contempla Ubuntu/Mint, com pkexec apenas proposto. Instaladores/detecção em [P-11](decisoes-e-pendencias.md#p-11).

## Verificação das sugestões de etiquetas

Ao implementar a sugestão de predefinidas, conferir a criação de LocalFile, a opção de silenciar perguntas na sessão e a volta da pergunta na próxima abertura. O efeito automático dentro da sessão e a escolha entre várias predefinidas compatíveis dependem de [P-03](decisoes-e-pendencias.md#p-03). Esses resultados ainda não estão fechados.
