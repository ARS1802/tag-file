# Implementação e exemplos executáveis

A implementação mantém Java/Swing/JDBC, sem gerenciador de dependências ou framework.
Os 24 tipos do catálogo estão em arquivos separados; `src/Main.java` continua sendo a entrada.
As escolhas confirmadas e técnicas estão em [decisões desta implementação](decisoes-implementacao.md).
A cobertura e os limites dos testes estão em [verificação](verificacao.md).

## Compilar e gerar documentação

Execute da raiz do projeto com JDK 24. Neste ambiente:

```sh
TAG_FILE_JDK=/home/arthur/.jdks/corretto-24.0.2 bash scripts/build.sh
```

O script compila **todos** os fontes em `src` com UTF-8 e `--release 24`, depois gera Javadoc privado com `-Xdoclint:all`. Não depende do IntelliJ. Se `javac`/`javadoc` corretos estiverem no PATH, basta `bash scripts/build.sh`.

Equivalente explícito:

```sh
mkdir -p out/classes out/javadoc lib
find src -name '*.java' -type f | sort > out/sources.txt
javac --release 24 -encoding UTF-8 -cp 'lib/*' -d out/classes @out/sources.txt
javadoc -private -Xdoclint:all -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -cp 'lib/*' -d out/javadoc @out/sources.txt
java -cp 'out/classes:lib/*' Main --demo-local
```

No Windows: defina `$env:TAG_FILE_JDK` para o JDK e execute `powershell -File scripts/build.ps1`. Use `java -cp 'out/classes;lib/*' Main --demo-local` (separador `;`).

## Preparar o banco

Copie `database/config/database.properties.example` para `database/config/database.properties` se este ainda não existir. O arquivo real, dados, binários, logs e JAR são ignorados pelo Git. Os parâmetros públicos da instância didática continuam os de AMB-03.

Inclua manualmente [Connector/J 9.7.0 em lib](../lib/README.md), também no classpath. Servidor, driver e conexão são três peças distintas.

```sh
# Inspeção sem instalar/iniciar/parar; informa ausências como erro.
java -cp 'out/classes:lib/*' Main --demo-env

# Instalação portátil quando faltar, explicitamente autorizada por este argumento.
java -cp 'out/classes:lib/*' Main --env-prepare --allow-install

# Próximas aberturas reutilizam os dados e não precisam autorizar instalação.
java -cp 'out/classes:lib/*' Main --env-prepare
java -cp 'out/classes:lib/*' Main --demo-db
java -cp 'out/classes:lib/*' Main --env-stop
```

No Linux, o pacote portátil requer x86_64, glibc 2.28 ou posterior e bibliotecas nativas, incluindo libaio. O script não instala pacotes de sistema. Ausência dessas bibliotecas é apresentada como erro; instale-as pelo mecanismo administrativo da sua distribuição antes de repetir. Download/extração portátil não exige alterar o MySQL global. No Windows, o ZIP x64 é extraído no runtime; bibliotecas de execução do MySQL também precisam estar presentes. **Windows não foi executado nesta entrega.**

O estado é reutilizado em `database/runtime/data`. Falha não autoriza apagar esse diretório. Instalação parcialmente extraída é preservada para diagnóstico. Porta ocupada por outra instância gera erro, sem mudar de porta ou parar esse servidor. `autoReconnect=true` não repete uma ação nem desfaz efeitos.

## Modos de Main

| Argumento | O que executa | Pré-requisitos |
|---|---|---|
| Sem argumento / `--demo-local` | Domínio, Factory, filtros físicos, operações em disco, clipboard, três callbacks, exclusividade e recusa administrativa simulada. | JDK; sem banco ou janela. |
| `--demo-db` | DAOs JDBC reais; associação, AND/OR, edição, cópia/substituição, exclusões, sugestões, relocalização, lotes, CUT entre Controllers e falha parcial real. | Instância preparada/aberta, configuração e JAR. |
| `--demo-ui` | Janela interativa com os dois Panels, abas/lado a lado, seleção, Drag/Drop, atalhos, confirmações, filtros e Loading. | Ambiente gráfico e banco; oferece instalação autorizada quando componentes faltam. |
| `--demo-ui-check` | Teste gráfico automático em janela própria, incluindo botão, atalho, Drop, diálogo modal e pop-up real de falha SQL; salva `out/ui-check.png`. | Ambiente gráfico e banco preparado. Fecha apenas sua janela ao terminar. |
| `--demo-env` | Inspeção real de configuração, driver e executáveis, sem administração. | Configuração local. |
| `--env-prepare [--allow-install]` | Instalação autorizada se necessária, preparação/reuso da instância, schema e predefinidas. | Internet para instalação; não precisa internet nas próximas aberturas. |
| `--env-stop` | Para somente a instância identificada do projeto. | Instância respondendo e marcador/PID válidos. |

O modo interativo permanece aberto. Fechar sua janela enquanto há trabalho não interrompe/fecha a conexão em uso; aguarde a ação terminar. Fechamento normal remove recursos da demonstração, fecha JDBC e encerra a instância exclusiva. O modo gráfico automático não afirma ter testado todos os gestos possíveis do usuário.

As demos criam pastas por `Files.createTempDirectory` e removem apenas suas árvores, seus cadastros e suas Tags rastreadas por UUID. A limpeza global de domínio só é testada quando os cadastros existentes pertencem ao próprio cenário; havendo dados externos, o cenário informa **NÃO EXECUTADO**. A UI demonstrativa inicia esse ciclo automaticamente se não há cadastros existentes. O método de aplicação `LocalFileManager.initialize()` implementa a limpeza global uma vez por sessão; não é chamado por reconexão, Observer ou Refresh.

## Organização e leitura do código

| Pacote | Responsabilidade |
|---|---|
| `model` | Entidades imutáveis, referências nativas e Factory simples. |
| `filter` | Critérios de consultas; não são seleção confirmada nem operação física. |
| `persistence` | JDBC, DAOs, schema e administração da instância. |
| `manager` | Regras de classificação e sequência de disco/SQL, com confirmações pela fronteira `Interaction`. |
| `service` | Disco, clipboard, Observer, exclusividade e descrição da falha parcial. |
| `controller` | Entradas funcionais e composição compartilhada para publicar as duas visões. |
| `ui` | Swing, diálogos, Drag/Drop e apresentação de fotografias consultadas. |

Uma rota de estudo: `Main.demoClassification` → `TagExplorerController.associate` → `ExplorerContext.execute` → `ActionGate.submit` → `LocalFileManager.associate` → `EntityFactory` / DAOs → `ExplorerEventService.publish` → Panels. O controle continua ocupado até os callbacks terminarem na EDT; esses callbacks não pedem outro Refresh.

Em `Main.demoClipboardAndFailures`, uma trigger **temporária de teste**, restrita ao UUID criado pela demo e removida em `finally`, provoca uma falha SQL depois do movimento. Essa é uma injeção de falha demonstrativa, não uma trigger de produção nem substituição do MySQL por memória. A mensagem informa o movimento concluído e a persistência que falhou, com causa SQL preservada.

Antes de 2025, Swing, JDBC, DAO e Observer já eram técnicas estabelecidas. O JDK 24 não exige mudar esses fundamentos. Aqui o impacto das versões é a compatibilidade do compilador/driver/servidor; não se introduziu framework para acompanhar versões. O projeto usa sua própria interface de Observer, em vez de depender de `java.util.Observable`, depreciada desde Java 9. [API no JDK 24](https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/Observable.html).

## Tipo → arquivo → exemplo

Todos os caminhos abaixo são relativos à raiz. “Executado” significa exercício pelo cenário indicado; não significa cobertura de todos os ramos de cada método. Para UI, o modo automático monta os mesmos Panels do modo interativo.

| Tipo | Arquivo | Método/cenário de Main | Modo e pré-requisitos | Execução realizada |
|---|---|---|---|---|
| `NativeFile` | `src/model/NativeFile.java` | `demoLocal` | local: caminho ausente e referência física | Executado em Linux. |
| `NativeDirectory` | `src/model/NativeDirectory.java` | `demoLocal` | local: pasta temporária e listagem | Executado em Linux. |
| `LocalFile` | `src/model/LocalFile.java` | `demoLocal / demoPersistence / demoClipboardAndFailures` | local/db: composição, datas e UUID ao mover | Executado em Linux. |
| `Tag` | `src/model/Tag.java` | `demoLocal / demoDecisions` | local/db: restrições, identidade e confirmações | Executado em Linux. |
| `EntityFactory` | `src/model/EntityFactory.java` | `demoLocal / demoPersistence` | local/db: produtos novos e reconstrução distinta | Executado em Linux. |
| `Filter` | `src/filter/Filter.java` | `demoLocal` | local: referência abstrata para filtro concreto | Executado em Linux. |
| `NativeFileFilter` | `src/filter/NativeFileFilter.java` | `demoLocal` | local: listagem física filtrada | Executado em Linux. |
| `LocalFileFilter` | `src/filter/LocalFileFilter.java` | `demoClassification` | db: AND/OR com DAO real | Executado em Linux. |
| `TagFilter` | `src/filter/TagFilter.java` | `demoPersistence / demoClassification` | db: Tags e ausência de associações | Executado em Linux. |
| `CrudDAO` | `src/persistence/CrudDAO.java` | `demoPersistence` | db: chamadas por interface genérica | Executado em Linux. |
| `LocalFileDAO` | `src/persistence/LocalFileDAO.java` | `demoPersistence / demoClassification` | db: CRUD, reconstrução e Map em lote | Executado em Linux. |
| `TagDAO` | `src/persistence/TagDAO.java` | `demoPersistence / demoDecisions` | db: extensões, contagem, edição e exclusões | Executado em Linux. |
| `LocalFileTagDAO` | `src/persistence/LocalFileTagDAO.java` | `demoPersistence` | db: vínculo, retirada e idempotência | Executado em Linux. |
| `DatabaseConnection` | `src/persistence/DatabaseConnection.java` | `demoPersistence / DemoSession.close` | db: conexão real compartilhada, fechamento e reabertura | Executado em Linux. |
| `DatabaseManager` | `src/persistence/DatabaseManager.java` | `main --demo-env / prepareEnvironment / demoEnvironmentRefusal` | env/db/local: inspeção, schema, parada e recusa simulada | Executado em Linux. |
| `TagExplorerController` | `src/controller/TagExplorerController.java` | `demoClassification / demoClipboardAndFailures` | db: associação e CUT de origem | Executado em Linux. |
| `LocalFileExplorerController` | `src/controller/LocalFileExplorerController.java` | `demoClassification / demoClipboardAndFailures` | db: navegação e colagem de destino | Executado em Linux. |
| `LocalFileManager` | `src/manager/LocalFileManager.java` | `demoPersistence / demoClassification / demoDecisions` | db: inicialização, Refresh, regras e efeitos parciais | Executado em Linux. |
| `NativeFileService` | `src/service/NativeFileService.java` | `demoLocal / demoClipboardAndFailures` | local/db: disco e metadados reais | Executado em Linux. |
| `ClipboardService` | `src/service/ClipboardService.java` | `demoLocal / demoCopyAndDeletion / demoClipboardAndFailures` | local/db: COPY/CUT comum | Executado em Linux. |
| `ExplorerEventService` | `src/service/ExplorerEventService.java` | `demoObserver / demoUi` | local/ui: publicação, EDT e inscrição | Executado em Linux. |
| `ExplorerListener` | `src/service/ExplorerListener.java` | `demoObserver / demoUi` | local/ui: três callbacks efetivos | Executado em Linux. |
| `TagExplorerPanel` | `src/ui/TagExplorerPanel.java` | `demoUi / verifyUi` | ui: apresentação, Drop e atualização | Executado em Linux. |
| `LocalFileExplorerPanel` | `src/ui/LocalFileExplorerPanel.java` | `demoUi / verifyUi` | ui: nativos sem cadastro, botão/atalho | Executado em Linux. |
| `ActionGate` | `src/service/ActionGate.java` | `demoActionGate / demoControllerGate / verifyUi` | local/db/ui: rejeição sem fila e liberação real | Executado em Linux. |
| `OperationFailure` | `src/service/OperationFailure.java` | `demoClipboardAndFailures / verifyFailureDialog` | db/ui: movimento seguido de erro SQL e mensagem expansível | Executado em Linux. |
| `ExplorerContext` | `src/controller/ExplorerContext.java` | `DemoSession / demoControllerGate / demoUi` | db/ui: colaboradores compartilhados e publicação sem Refresh adicional | Executado em Linux. |
| `Interaction` | `src/ui/Interaction.java` | `DemoInteraction / demoDecisions / demoUi` | db/ui: respostas explícitas e diálogos reais | Executado em Linux. |
| `SwingInteraction` | `src/ui/SwingInteraction.java` | `verifyUi / verifyFailureDialog` | ui: diálogo modal, cancelamento e detalhes técnicos | Executado em Linux. |
| `FileTransferHandler` | `src/ui/FileTransferHandler.java` | `verifyUi` | ui: Transferable, Drop real rejeitado e exportação configurada | Executado em Linux. |
| `Main` | `src/Main.java` | `main` e métodos de cenário | Todos os modos de demonstração | Executado; janela interativa montada pelo mesmo código do modo gráfico de verificação. |
| `Main.DemoSession` (privado) | `src/Main.java` | `demoDatabase / demoUi` | Auxiliar da demonstração: possui e limpa recursos próprios; não substitui produção por memória | Executado. |
| `Main.DemoInteraction` (privado) | `src/Main.java` | `demoDecisions` | Auxiliar da demonstração: roteiro de respostas para verificar confirmações; diálogo inesperado falha | Executado. |
| `Main.DemoWindow` (privado) | `src/Main.java` | `demoUi / verifyUi` | Auxiliar da demonstração: composição da janela, abas, lado a lado e fechamento | Executado. |

Os seis auxiliares públicos de produção resolvem necessidades concretas: exclusividade (`ActionGate`), resultados parciais (`OperationFailure`), composição compartilhada (`ExplorerContext`), separação de decisões da UI (`Interaction`/`SwingInteraction`) e o protocolo Swing de Drag/Drop (`FileTransferHandler`). Os três auxiliares privados de Main pertencem apenas às demonstrações. Implementações anônimas de listeners/adaptadores ficam junto de seu uso e também constam no inventário do arquivo; não são novas camadas arquiteturais.

