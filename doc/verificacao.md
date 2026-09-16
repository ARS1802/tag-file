# Verificação da implementação — 16/09/2026

Investigação/verificação em profundidade de trabalho, a partir da revisão `7a815cac76b94409f5165b7b689cf07c02bf1ef9`. O checkout inicial continha apenas `Main` com Hello World e documentação; não havia alterações locais a preservar além desses arquivos versionados. As verificações abaixo se referem às alterações de trabalho, ainda sem commit.

Ambiente executado: Linux Mint 22.3 x86_64, Amazon Corretto **24.0.2**, MySQL portátil **8.4.9**, Connector/J **9.7.0**. A instalação MySQL global permaneceu fora das operações. Os testes JDBC usam servidor real, schema real e conexão compartilhada; apenas respostas de diálogos e a recusa de instalação têm dublês identificados.

## Comandos e resultados

Na raiz do projeto, com a configuração local e o JAR descritos em [implementação](implementacao.md):

```sh
TAG_FILE_JDK=/home/arthur/.jdks/corretto-24.0.2 bash scripts/build.sh
/home/arthur/.jdks/corretto-24.0.2/bin/java -cp 'out/classes:lib/*' Main --env-prepare
/home/arthur/.jdks/corretto-24.0.2/bin/java -cp 'out/classes:lib/*' Main --demo-local
/home/arthur/.jdks/corretto-24.0.2/bin/java -cp 'out/classes:lib/*' Main --demo-db
/home/arthur/.jdks/corretto-24.0.2/bin/java -cp 'out/classes:lib/*' Main --demo-ui-check
/home/arthur/.jdks/corretto-24.0.2/bin/java -cp 'out/classes:lib/*' Main --demo-env
/home/arthur/.jdks/corretto-24.0.2/bin/java -cp 'out/classes:lib/*' Main --env-stop
```

| Verificação | Estado | Resultado observado |
|---|---|---|
| Compilação de todos os 31 fontes | **PASSOU** | Saída 0; `--release 24`, UTF-8, tipos e Main compilados juntos. |
| Javadoc privado com `-Xdoclint:all` | **PASSOU** | Saída 0, sem erros ou avisos; inclui métodos privados, interfaces, abstratos e sobrescritos. |
| Inventários no início dos arquivos | **PASSOU** | 31 blocos antes de package/import; inventário conferido por análise sintática do JDK para 266 construtores/métodos explícitos, incluindo anônimos. Resumos de domínio, DAOs, controle, UI e Main também revisados manualmente. |
| `--demo-local` | **PASSOU** | Saída 0 e `DEMO-LOCAL: PASSOU`; sem MySQL e sem janela. |
| Instalação portátil Linux limpa e reutilização | **PASSOU** | `install.sh --authorized` executado duas vezes em raiz temporária própria: download oficial, extração, biblioteca local e executáveis 8.4.9 conferidos. Saída 0 em ambas; nenhum servidor iniciado nesse teste. |
| Preparação/reuso/schema e parada Linux | **PASSOU** | Saída 0; dados em runtime, porta 3333; schema reaplicado sem recriar predefinidas nem apagar tabelas; parada identifica instância/PID. |
| `--demo-db` | **PASSOU** | Saída 0 e `DEMO-DB: PASSOU`; inclui falha SQL proposital reconhecida como falha parcial. |
| `--demo-ui-check` | **PASSOU** | Saída 0 e `DEMO-UI-CHECK: PASSOU`; componentes reais na EDT, diálogos reais e captura em `out/ui-check.png`. |
| `--demo-env` | **PASSOU** | Saída 0; configuração, driver e executáveis identificados, sem imprimir senha. |
| Sessão manual prolongada de `--demo-ui` | **NÃO EXECUTADO** | O mesmo caminho de montagem foi executado pelo modo gráfico automático; não se afirma inspeção humana de todas as interações. O modo interativo permanece aberto por implementação. |
| Scripts PowerShell / Windows | **NÃO EXECUTADO** | Host Linux, sem PowerShell/Windows. Implementação revisada, sem evidência de execução nessa plataforma. |
| Abrir arquivo na aplicação associada | **NÃO EXECUTADO** | Implementado via `Desktop.OPEN`; não foi iniciado aplicativo pessoal do usuário nos testes. Pode ser exercitado no botão Abrir do modo gráfico. |

Logs locais reproduzíveis: `out/build.log`, `out/demo-local.log`, `out/demo-db.log`, `out/demo-ui-check.log`, `out/demo-env.log`, `out/installer-linux.log`. `out` não é versionado. Os comandos acima independem desses logs preexistentes. Para conferir o instalador isoladamente, foram copiados `common.sh`, `check.sh` e `install.sh` para `database/scripts/linux` de uma raiz temporária; `bash database/scripts/linux/install.sh --authorized` foi executado duas vezes nessa raiz, removida ao terminar.

No terminal administrado pelo agente, processos descendentes são encerrados ao terminar certas execuções. Por isso, a verificação agrupou início, demos e parada da instância na mesma execução de terminal. Uma conexão recusada entre execuções foi diagnosticada por ausência do processo, sem apagar/reinicializar os dados. Isso é distinto de uma falha funcional do DAO.

## Matriz dos 25 critérios

“PASSOU” abaixo vale para os cenários descritos, não é uma alegação de cobertura exaustiva. Roteiros de confirmação exercitam os Controllers e o Manager reais por `Interaction`; não substituem MySQL. O teste gráfico adicional confere a implementação Swing dessa fronteira.

| Critério | Estado | Evidência em Main e resultado |
|---|---|---|
| ACE-01 | **PASSOU** | `demoClassification`: navegação apresenta `nativo.txt` e consulta por caminho continua vazia. |
| ACE-02 | **PASSOU** | `demoClassification`: segunda Tag e seleção múltipla reutilizam o UUID do mesmo caminho. |
| ACE-03 | **PASSOU** | `demoEditingAndRenaming`: homônimos em duas pastas têm UUIDs diferentes. |
| ACE-04 | **PASSOU** | `demoLocal`: Tag aceita `.pdf` e `.cdr`; recusa `.png`. |
| ACE-05 | **PASSOU** | `demoLocal`/`demoPersistence`: PDF/.PDF normalizam para `.pdf`; conjunto e chave composta evitam duplicação. |
| ACE-06 | **PASSOU** | `demoLocal`/`demoClassification`: conjunto vazio aceita extensões distintas, inclusive arquivo sem extensão. |
| ACE-07 | **PASSOU** | `demoDecisions`: cancelar não grava; ampliar grava só após resposta; nova Tag recebe a associação. Drop usa o mesmo Controller/Manager; rejeição ocupada exercitada na UI. |
| ACE-08 | **PASSOU** | `demoEditingAndRenaming`: cancelamento preserva vínculo; confirmação retira somente incompatíveis e aplica sentinela; último sufixo retirado elimina restrição. |
| ACE-09 | **PASSOU** | `demoDecisions`: nome repetido cancelado não insere; confirmado recebe outro UUID. |
| ACE-10 | **PASSOU** | `demoClassification`: retirada da última Tag normal deixa a sentinela. |
| ACE-11 | **PASSOU** | `demoClassification`: nova Tag normal remove somente a associação com a sentinela. |
| ACE-12 | **PASSOU** | `demoPersistence`: duas fixtures, uma órfã e outra na sentinela, removidas na inicialização; disco/sentinela preservados. Teste se recusa a limpar cadastros externos à demo. |
| ACE-13 | **PASSOU** | `demoClassification`: Refresh mantém cadastro na sentinela; reconexão não chama initialize. |
| ACE-14 | **PASSOU** | `demoClassification`: exclusão da sentinela rejeitada; consulta de Tags vazias não confunde zero disponíveis com ausência de vínculos. |
| ACE-15 | **PASSOU** | `demoClassification`: AND retorna um, OR dois, sem repetir UUID. |
| ACE-16 | **PASSOU** | `demoPersistence`/`demoClassification`/`verifyUi`: Map só contém cadastrados, lista física e Panel mantêm o outro arquivo. |
| ACE-17 | **PASSOU** | `demoClipboardAndFailures`: CUT no Controller de Tags, colagem no local, mesmo UUID/Tags, caminho novo e clipboard limpo. |
| ACE-18 | **PASSOU** | `demoEditingAndRenaming`: cancelar, retirar incompatíveis e ampliar extensões; UUID preservado e conteúdo não convertido. |
| ACE-19 | **PASSOU** | `demoCopyAndDeletion`: três modalidades e confirmação das outras Tags; verifica disco, cadastros, Tag selecionada e demais Tags conforme P-02 confirmado. |
| ACE-20 | **PASSOU** | `demoCopyAndDeletion`: herança perguntada, destino cadastrado substituído por novo UUID; cópia sem herança só nativa; origem permanece; manter ambos/cancelar verificados. |
| ACE-21 | **PASSOU** | `demoClassification`: contador confirma uma sincronização global; `verifyUi`: ambos os Panels recebem fotografias sem Refresh adicional. |
| ACE-22 | **PASSOU** | `demoClipboardAndFailures` + `verifyFailureDialog`: erro SQL real após movimento; caminho antigo permanece no SQL; pop-up mostra concluído/falhou, expande SQLState e libera Loading. |
| ACE-23 | **PASSOU** | `--env-prepare`/`--demo-env`: parâmetros públicos e `@@datadir`/`@@port` da instância exclusiva conferidos. |
| ACE-24 | **PASSOU** no controle de recusa/cancelamento | `demoEnvironmentRefusal`: DatabaseManager real recebe processos simulando ausência e cancelamento; sem autorização não instala, código 17 não vira sucesso. Cancelamento de instalação real por diálogo de sistema/Windows é **NÃO EXECUTADO**. |
| ACE-25 | **PASSOU** | `demoActionGate`, `demoControllerGate`, `verifyUi`: dois Controllers, botão, atalho, Drop, chamada automática/reentrante; sem fila; EDT pulsa durante trabalho; sucesso/falha/cancelamento liberam ao terminar. Cancelamento do futuro não libera tarefa ainda ativa. |

Sugestões de predefinidas: **PASSOU** em `demoSuggestions`. Duas predefinidas compatíveis próprias são oferecidas; só a escolhida é associada; silenciar não classifica; outro Manager de sessão volta a perguntar.

Verificações adicionais: reconstrução preserva UUID/datas; DAOs não fecham conexão; erro JDBC não vira lista vazia; UPDATE usa dados físicos; relocalização preserva identidade, recusa colisão e permite remoção explícita imediata; lote preserva os concluídos e não executa os posteriores à falha.

## Limites das evidências

Não há prova de atomicidade, recuperação automática ou transações: essas capacidades estão fora do escopo e não foram simuladas como sucesso. Não houve teste de queda de energia, disco cheio, todos os sistemas de arquivos ou diferenças de caixa em Windows. A política de caminhos é lexical e seus limites estão em P-08 do [registro de decisões](decisoes-implementacao.md).

A instalação inicialmente tentada com `/usr/sbin/mysqld` foi recusada pelo AppArmor; o log de auditoria identificou exatamente o acesso ao runtime. Foi usada a distribuição portátil, mantendo o serviço global intacto. A biblioteca libaio do host tem nome t64; o link de compatibilidade ficou dentro do runtime. Esses fatos são dependências do ambiente, não erros ocultados por retorno vazio.
