# Implementação e execução

O projeto usa Java, Swing e JDBC, sem Maven/Gradle, e requer JDK superior à versão 21. `Main` inicia a aplicação gráfica; não há modos de teste ou demonstração nessa entrada. As decisões de domínio estão em [decisões da implementação](decisoes-implementacao.md).

## Compilar e executar

Execute da raiz do projeto, com um JDK superior à versão 21 no PATH:

```sh
bash scripts/build.sh &&
java -cp 'out/classes:lib/*' Main
```

Se o JDK não estiver no PATH, substitua `/caminho/do/jdk` pelo diretório da instalação:

```sh
TAG_FILE_JDK=/caminho/do/jdk bash scripts/build.sh &&
/caminho/do/jdk/bin/java -cp 'out/classes:lib/*' Main
```

Os scripts recompilam todos os fontes de `src` com UTF-8 e o alvo definido em `--release` e geram Javadoc privado com `-Xdoclint:all`. O Bash limpa as saídas antes de compilar; o PowerShell publica novas saídas somente após compilação e documentação bem-sucedidas. Não alteram o banco.

O script Bash confere o suporte ao alvo configurado antes de limpar as saídas anteriores. Se houver erro de versão não suportada, confira `javac -version` e a [configuração de compilação](compatibilidade-java.md#configuração-de-compilação). A configuração do JDK no IntelliJ é independente da seleção feita pelo terminal. `TAG_FILE_JDK` afeta o script de compilação; use explicitamente o `bin/java` do mesmo JDK para executar. O `&&` impede a tentativa de executar `Main` quando a compilação falha.

No Windows, use `powershell -NoProfile -File scripts/build.ps1` e, somente após sucesso, `java -cp 'out/classes;lib/*' Main`. `$env:TAG_FILE_JDK` pode indicar o JDK. O build PowerShell usa alvo Java 22, valida as ferramentas antes de substituir saídas e registra logs em `out/build-logs`. Consulte o [contrato, testes e limites do build PowerShell](build-powershell.md). A execução real no Windows continua não verificada neste ambiente.

No IntelliJ, mantenha `src` como **Sources Root** e o Connector/J nas dependências do módulo. Na configuração de execução **Application**:

1. Selecione a classe principal `Main` e um JDK compatível com o alvo do projeto (a configuração atual do IntelliJ usa Java 24).
2. Deixe **Program arguments** vazio para abrir o aplicativo e **Working directory** na raiz do projeto.
3. Execute **Run**. A compilação do IntelliJ precede a inicialização do Java; a janela principal só aparece depois da preparação do MySQL e da conexão JDBC.
4. Se faltar a instalação, confirme **Preparar** no diálogo. No Windows, o pacote portátil é preparado com a conta atual, sem UAC; uma janela mostra progresso e oferece acesso ao log. Para apresentar sem rede, siga [Windows portátil](windows-portatil.md).

Se a janela principal ainda não aparecer, observe o console **Run** e o log informado: o executor informa o script solicitado e o caminho de seu log. A opção `--build` seleciona apenas a compilação elevada; sua existência não comprova que seja a causa de uma execução que continua aguardando.

## Execução de scripts com elevação

Esta seção descreve `Main --build` e a instalação Linux. A instalação portátil
Windows usa a conta atual e está descrita em [Windows portátil](windows-portatil.md).

`Main.executeScriptWithElevation(Path, String...)` delega a execução ao serviço `ElevatedScriptExecutor`:

- **Linux:** usa `pkexec` e o agente de autenticação da sessão gráfica. Não há leitura de senha pelo Java nem fallback para solicitar senha pelo terminal.
- **Windows:** chama [scripts/elevate.ps1](../scripts/elevate.ps1), que usa `Start-Process powershell.exe -Verb RunAs -WindowStyle Normal -Wait -PassThru`. O Windows solicita UAC e abre o console elevado. A aplicação Java continua com as permissões da conta atual.

São dois pontos de entrada:

| Ação | Como executar |
|---|---|
| Compilar com elevação | Execute `Main --build`. O script usado é `scripts/build.sh` ou `scripts/build.ps1`. Ao terminar, esse comando encerra; a aplicação é aberta em uma nova execução de `Main`. |
| Instalar MySQL quando faltar | Execute `Main` normalmente. Após consentimento na interface, Linux usa o executor elevado; Windows executa `database/scripts/windows/install.ps1` com a conta atual. Cancelar a confirmação interrompe a inicialização. |

Com o JDK no PATH, solicite a compilação elevada assim:

```sh
java -cp 'out/classes:lib/*' Main --build
```

O `Main` precisa estar compilado para executar esse comando. A primeira compilação pode ser feita pelo IntelliJ ou pelo script com um JDK superior à versão 21, conforme os comandos anteriores. Elevação não substitui o JDK correto. O executor transmite o `java.home` da aplicação como `TAG_FILE_JDK`, inclusive após a limpeza de ambiente feita pelo `pkexec`.

No Windows, o executor grava um pequeno `.ps1` temporário, em UTF-8 com BOM, com os caminhos, argumentos e JDK da execução. O launcher entrega esse arquivo ao PowerShell com `-File`; o console elevado executa o script diretamente, sem outro processo oculto ou comandos em Base64. O arquivo temporário é removido após o retorno; o log permanece. Se a espera Java for interrompida, o arquivo é preservado porque o processo elevado ainda pode estar usando-o.

O console mostra a saída durante a execução. `Start-Transcript` registra mensagens do host PowerShell, exceções e o resultado em `tag-file-elevated-*.log`; o caminho aparece no console do Java e em mensagens de falha. A transcrição não garante a captura de escritas diretas em `[Console]`, usadas pelo build: os diagnósticos completos do compilador continuam em `out/build-logs/*.log`. Em caso de falha do script, a janela elevada aguarda Enter antes de devolver o código ao Java.

No Windows, a espera é a do próprio `Start-Process -Wait`, sem o antigo limite adicional de 14/15 minutos do executor. Os limites internos das ferramentas do build continuam valendo. Cancelar o UAC encerra a inicialização; falhas retornam para o tratamento de erros de `Main`.

No Linux, stdout/stderr continuam no log temporário e a espera mantém o limite de 15 minutos. `scripts/elevated-common.sh` devolve à conta solicitante a propriedade das saídas de compilação e dos arquivos criados pelo instalador, também quando o script falha. A aplicação e o servidor MySQL continuam sendo executados sem elevação.

Referências dos mecanismos nativos: [pkexec/polkit](https://polkit.pages.freedesktop.org/polkit/pkexec.1.html) e [Start-Process/UAC](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.management/start-process?view=powershell-5.1).

Verificações anteriores da integração confirmaram a ordem instalar → inicializar → iniciar, a ausência de nova instalação quando o ambiente está pronto e a interrupção por cancelamento/falha simulados. A revisão do executor Windows tem testes reproduzíveis e limites descritos em [build PowerShell](build-powershell.md#substituição-do-executor-windows-por-console-nativo). A autenticação nativa real e a execução no Windows não foram testadas neste ambiente Linux.

## Ambiente e ciclo de vida

Na primeira abertura, a aplicação cria `database/config/database.properties` a partir do modelo versionado `database.properties.example`, se o arquivo local estiver ausente. Configurações existentes são preservadas, inclusive quando inválidas; nesse caso, a mensagem informa o que revisar. Configuração e driver são verificados antes de iniciar o MySQL. O [Connector/J em lib](../lib/README.md) é versionado e já está configurado no classpath do módulo IntelliJ e dos scripts. Configuração local, dados e binários do servidor não são versionados.

A aplicação exige ambiente gráfico. Na abertura, verifica e prepara a instância MySQL exclusiva do projeto. Se os executáveis estiverem ausentes, solicita consentimento na interface para preparar o pacote; somente no Linux a instalação continua usando autorização nativa do sistema. Cancelar essa confirmação encerra a inicialização. Dependências nativas do MySQL precisam estar disponíveis no sistema; veja [instalação e execução](instalacao-e-execucao.md).

O fluxo atual é:

1. `Main` cria os diálogos e `Application`, então inicia a sessão.
2. `Application` prepara o ambiente, abre JDBC, confere o schema e cria os DAOs e serviços compartilhados.
3. `LocalFileManager.initialize()` executa uma vez por sessão: remove cadastros órfãos ou associados apenas à sentinela e sincroniza os restantes, conforme o contrato de domínio.
4. `Application` cria `MainWindow` na EDT, carrega os dados iniciais e exibe os exploradores na pasta pessoal do usuário (`user.home`).
5. Ao fechar, a aplicação libera JDBC, encerra a instância exclusiva e descarta os componentes visuais. Cadastros, etiquetas e arquivos físicos são preservados no encerramento.

Enquanto há uma operação em andamento, o fechamento é ignorado; solicite-o novamente após o término. Os dados do banco são reutilizados em `database/runtime/data`. Erros de preparação não autorizam apagar esse diretório nem encerrar outra instância MySQL.

## Organização do código

| Local | Responsabilidade |
|---|---|
| `src/Main.java` | Entrada da aplicação e apresentação de falhas de inicialização. |
| `src/application/Application.java` | Composição dos objetos compartilhados, inicialização e encerramento da sessão. |
| `src/GUI` | Janela, painéis Swing, diálogos e arrastar/soltar. |
| `src/controller` | Recebe ações dos exploradores e coordena a publicação dos resultados. |
| `src/manager` | Aplica regras de classificação e coordena etapas no disco e no banco. |
| `src/service` | Disco, clipboard, eventos, exclusividade, falhas parciais e execução elevada de scripts. |
| `src/persistence` | Conexão JDBC, DAOs, schema e administração da instância MySQL. |
| `src/model` | Entidades, referências a arquivos/diretórios e fábrica de entidades. |
| `src/filter` | Critérios de pesquisa e de listagem física. |

## Interface gráfica

| Tipo e arquivo | Função |
|---|---|
| [MainWindow](../src/GUI/MainWindow.java) | Janela principal, abas/lado a lado, Refresh, Loading e solicitação de fechamento. Substitui a antiga `Main.DemoWindow`. |
| [TagExplorerPanel](../src/GUI/TagExplorerPanel.java) | Etiquetas, busca e ações sobre arquivos classificados. |
| [LocalFileExplorerPanel](../src/GUI/LocalFileExplorerPanel.java) | Árvore de pastas, listagem de arquivos físicos e ações locais. |
| [SwingInteraction](../src/GUI/SwingInteraction.java) | Confirmações, entrada de valores, seleção de arquivos e apresentação de erros. |
| [Interaction](../src/GUI/Interaction.java) | Contrato pelo qual as regras solicitam decisões à interface. |
| [FileTransferHandler](../src/GUI/FileTransferHandler.java) | Transferência de arquivos e associação de etiquetas por arrastar/soltar. |

O diretório e o pacote usam exatamente `GUI`, conforme a organização solicitada. Os controladores continuam no pacote `controller`, pois coordenam ações sem montar componentes visuais.

Para acompanhar uma operação, siga: botão em `TagExplorerPanel` → `TagExplorerController` → `ExplorerContext` / `ActionGate` → `LocalFileManager` / DAOs → `ExplorerEventService` → painéis. As atualizações visuais são entregues na EDT; o controle de operações permanece ocupado até os callbacks terminarem.

## Registros de verificação

A [matriz de 16/09/2026](verificacao.md) preserva as evidências históricas dos cenários que existiam em `Main`. Esses cenários foram removidos da entrada de produção. A reorganização da interface está registrada em [melhorias de UI](Visual/melhorias-ui.md).
