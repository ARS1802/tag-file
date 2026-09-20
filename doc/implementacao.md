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

Os scripts recompilam todos os fontes de `src` com UTF-8 e o alvo definido em `--release`, geram Javadoc privado com `-Xdoclint:all` e limpam as saídas anteriores de classes/documentação para não conservar tipos removidos ou renomeados. Não alteram o banco.

O script Bash confere o suporte ao alvo configurado antes de limpar as saídas anteriores. Se houver erro de versão não suportada, confira `javac -version` e a [configuração de compilação](compatibilidade-java.md#configuração-de-compilação). A configuração do JDK no IntelliJ é independente da seleção feita pelo terminal. `TAG_FILE_JDK` afeta o script de compilação; use explicitamente o `bin/java` do mesmo JDK para executar. O `&&` impede a tentativa de executar `Main` quando a compilação falha.

No Windows, use `powershell -File scripts/build.ps1` e `java -cp 'out/classes;lib/*' Main`. `$env:TAG_FILE_JDK` pode indicar o JDK. A execução no Windows continua não verificada neste ambiente.

No IntelliJ, configure um JDK superior à versão 21, mantenha `src` como **Sources Root**, adicione o Connector/J às dependências do módulo e execute a classe `Main`, sem argumentos, com o diretório de trabalho na raiz do projeto.

## Execução de scripts com elevação

`Main.executeScriptWithElevation(Path, String...)` delega a execução ao serviço `ElevatedScriptExecutor`:

- **Linux:** usa `pkexec` e o agente de autenticação da sessão gráfica. Não há leitura de senha pelo Java nem fallback para solicitar senha pelo terminal.
- **Windows:** usa `Start-Process -Verb RunAs`, que solicita a autorização pelo UAC. A aplicação Java continua com as permissões da conta atual.

São dois pontos de entrada:

| Ação | Como executar |
|---|---|
| Compilar com elevação | Execute `Main --build`. O script usado é `scripts/build.sh` ou `scripts/build.ps1`. Ao terminar, esse comando encerra; a aplicação é aberta em uma nova execução de `Main`. |
| Instalar MySQL quando faltar | Execute `Main` normalmente. A verificação do ambiente chama o método de elevação apenas quando precisa executar `database/scripts/linux/install.sh` ou `database/scripts/windows/install.ps1`. Cancelar a autorização interrompe a inicialização. |

Com o JDK no PATH, solicite a compilação elevada assim:

```sh
java -cp 'out/classes:lib/*' Main --build
```

O `Main` precisa estar compilado para executar esse comando. A primeira compilação pode ser feita pelo IntelliJ ou pelo script com um JDK superior à versão 21, conforme os comandos anteriores. Elevação não substitui o JDK correto. O executor transmite o `java.home` da aplicação como `TAG_FILE_JDK`, inclusive após a limpeza de ambiente feita pelo `pkexec`.

As saídas dos scripts são registradas em arquivos temporários `tag-file-elevated-*.log`; o caminho aparece no console ou na mensagem de erro. A espera por autorização/execução tem limite de 15 minutos. Cancelamento e falha não permitem seguir para a preparação do banco. No Linux, `scripts/elevated-common.sh` devolve à conta solicitante a propriedade das saídas de compilação e dos arquivos criados pelo instalador, também quando o script falha. A aplicação e o servidor MySQL continuam sendo executados sem elevação.

Referências dos mecanismos nativos: [pkexec/polkit](https://polkit.pages.freedesktop.org/polkit/pkexec.1.html) e [Start-Process/UAC](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.management/start-process?view=powershell-5.1).

Verificação desta integração: compilação e Javadoc concluídos sem avisos; cenários isolados confirmaram a ordem instalar → inicializar → iniciar, a ausência de nova instalação quando o ambiente está pronto, a interrupção por cancelamento/falha simulados e a preservação do JDK e dos argumentos nos comandos construídos. Os resultados locais estão em `out/elevation-check/results.log`. A autenticação nativa real, a restauração de propriedade executada como root e a execução no Windows não foram testadas nesta verificação.

## Ambiente e ciclo de vida

Copie `database/config/database.properties.example` para `database/config/database.properties` se o arquivo real ainda não existir. Inclua o [Connector/J em lib](../lib/README.md) e no classpath. Configuração local, JAR, dados e binários não são versionados.

A aplicação exige ambiente gráfico. Na abertura, verifica e prepara a instância MySQL exclusiva do projeto. Se os executáveis estiverem ausentes, solicita autorização nativa do sistema para executar a instalação portátil. Cancelar essa confirmação encerra a inicialização. Dependências nativas do MySQL precisam estar disponíveis no sistema; veja [instalação e execução](instalacao-e-execucao.md).

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
