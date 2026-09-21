# Preparação portátil do MySQL no Windows

A abertura normal por `Main` prepara o MySQL usando a conta atual. Não chama UAC,
não registra serviço, não muda o Registro, as políticas permanentes de execução ou o firewall.
A pasta do projeto precisa ser gravável. `Main --build` continua sendo a opção
explícita de compilação elevada; no Linux permanece o executor já existente.

## Levar o projeto para a apresentação

1. Transfira os fontes, scripts e `database/config/mysql-windows-package.json`
   da mesma revisão. Uma edição local não chega ao Windows por um `git pull`
   enquanto não estiver no repositório remoto.
2. Para instalação offline, leve também `database/packages/mysql-8.4.9-winx64.zip`,
   que é ignorado pelo Git. O Connector/J em `lib` já acompanha o repositório.
   Não é necessário levar dados de uma instância MySQL de outra máquina.
3. Use um JDK superior a 21, configure o classpath com `lib/*` e execute `Main`
   sem argumentos no IntelliJ. Ao faltar a instalação, confirme **Preparar**.
   **Cancelar**, ou fechar a confirmação, encerra sem instalar/inicializar dados.
4. A janela **Preparando o Tag-File** mostra a etapa desde a abertura. Na cópia
   e no download, a barra representa bytes transferidos em relação ao tamanho
   conhecido do pacote, não uma estimativa do tempo total. Verificação, extração,
   inicialização e conexão usam barra indeterminada com pontos animados.
5. O caminho do log aparece na janela e no console Run. Os botões **Abrir pasta
   de logs** e **Copiar caminho do log** permitem acessá-lo durante a preparação
   e após uma falha. Os arquivos permanecem em `database/runtime/logs` no computador
   que executou o programa; não são apagados ao encerrar. Essa pasta é ignorada pelo
   Git e pode não aparecer em algumas visualizações do IntelliJ.

O instalador procura primeiro o arquivo definido em `TAG_FILE_MYSQL_ARCHIVE`
(caminho completo, inclusive na configuração Run do IntelliJ). Sem essa variável,
usará o ZIP em `database/packages`. Somente se nenhuma fonte offline for indicada
ou encontrada fará o download do CDN oficial. Caminho explícito inexistente ou
pacote inválido causa erro; não dispara um download oculto como alternativa.

## Identificar uma cópia desatualizada

Compare, nos dois computadores, o SHA-256 do script registrado no log e o arquivo
`database/config/mysql-windows-package.json`. No PowerShell:

```powershell
Get-FileHash -LiteralPath .\database\scripts\windows\install.ps1 -Algorithm SHA256
Get-Content -LiteralPath .\database\config\mysql-windows-package.json
```

Conversão de finais de linha também altera o hash: uma diferença exige comparar
o conteúdo. O instalador registra sua versão do PowerShell, sua origem offline
ou a URL de download e o hash do pacote verificado. Esses registros permitem
identificar qual cópia e qual pacote foram realmente usados.

## Integridade e falhas

O manifesto contém a versão, o diretório esperado e o SHA-256 do ZIP obtido do CDN
oficial. Sua origem está registrada em [packages/README](../database/packages/README.md).
O hash é comparado **antes** de extrair e executar binários. O instalador extrai
numa pasta temporária, verifica `mysqld`, `mysql` e `mysqladmin` com `--version`
e publica a pasta somente depois do sucesso. Um bloqueio exclusivo impede duas
instalações simultâneas cooperantes. Uma interrupção forçada pode deixar uma pasta
`install-<identificador>`; ela não é interpretada como instalação concluída.

`check.ps1` diferencia ausência (código 4), instalação incompleta (5) e falha ao
executar componentes (6). Apenas ausência permite oferecer instalação. As demais
falhas preservam a instalação e são mostradas ao usuário, sem reinstalação
automática. Problemas de escrita, ZIP e HTTP incluem a etapa e a exceção original.
O instalador preserva dados e limpa apenas os temporários que ele próprio criou.

HTTP 403 é uma recusa remota; mudar o UAC não a resolve. O modo offline elimina a
necessidade de rede na apresentação. Ele não elimina as dependências nativas:
o MySQL 8.4 requer o Microsoft Visual C++ Redistributable, e políticas da instituição
podem impedir a execução de programas. Nesses casos, o responsável pelo ambiente
precisa disponibilizar os pré-requisitos; a aplicação não contorna essas políticas.

Referência: [MySQL 8.4 no Windows](https://dev.mysql.com/doc/refman/8.4/en/windows-installation.html).

## Verificação

Os testes `tests/windows-install-contract.ps1` e `tests/DatabasePreparationCheck.java`
cobrem instalação online/offline, hash inválido, ZIP inválido, HTTP 403, recusa sem
autorização, falha nativa, bloqueio concorrente, reutilização e distinção dos códigos.
Os testes usam rede e binários simulados; extração, hash, scripts e processos são reais.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tests\windows-install-contract.ps1
javac --release 22 -encoding UTF-8 -cp out/classes -d out/test-classes tests/DatabasePreparationCheck.java
java -cp 'out/classes;out/test-classes' DatabasePreparationCheck
```

Na validação Linux, o teste Java usa um shim `powershell.exe` apontando para PowerShell
7 e simula somente a seleção da plataforma, mantendo o backend real de processos
Linux. Isso verifica o encaminhamento sem elevação, não a segurança do Windows.
O teste final de aceitação exige Windows PowerShell 5.1: usuário não elevado,
políticas mantidas, rede desconectada, primeira abertura, cancelamento, abertura
seguinte, conexão JDBC e encerramento do servidor exclusivo.

## Resultado local de 20/09/2026

Compilação de todos os fontes e Javadoc concluídos, sem avisos, usando Corretto
24.0.2 com alvo Java 22. PowerShell portátil 7.6.5/Linux: 64 verificações do
instalador, 22 da coordenação Java e 52 do executor elevado passaram. O executor
elevado continua coberto por regressão para seus usos restantes.

Antes da alteração, o novo teste offline falhou porque o instalador tentou acessar
a rede mesmo com ZIP local. Depois, passaram as fontes offline por pasta e variável,
recusas e instalação online simulada. O pacote oficial completo foi baixado,
conferido e disponibilizado localmente em `database/packages`, fora do Git.

Não foram executados Windows PowerShell 5.1, GUI, UAC real ou MySQL para Windows
neste host. A passagem dos testes locais não substitui a aceitação nesse sistema.

## Correção da sondagem de conexão e progresso

O erro `NativeCommandError` na primeira chamada `mysqladmin ping` de `start.ps1`
ocorria antes do comando que inicia o servidor. O Windows PowerShell 5.1 pode
transformar stderr nativo redirecionado em erro PowerShell; com
`ErrorActionPreference = Stop`, a indisponibilidade esperada abortava o script
antes de conferir o código de saída. Desde PowerShell 7.2, a interação com
redirecionamento é diferente, por isso testar só no 7 não reproduzia o caso.
[Referência Microsoft](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_preference_variables#-erroractionpreference).

As duas sondagens de disponibilidade agora usam preferência `Continue` apenas
no escopo da chamada, descartam a saída esperada e verificam o código retornado.
A preferência anterior é restaurada no `finally`. Ausência do executável não é
aceita como sondagem bem-sucedida. Verificação de identidade, processo existente,
falha do servidor e limite de espera continuam interrompendo a preparação.
Nenhuma permissão administrativa foi acrescentada.

`PreparationWindow` exibe progresso sem executar scripts na EDT. O executor lê
incrementalmente os eventos do log a cada 150 ms, preservando linhas parciais e
UTF-8. Eventos de transferência indicam arquivo e tamanho total; a porcentagem
vem do tamanho já escrito em disco. Não representa que o pacote foi validado
ou que o banco está pronto. O manifesto informa o tamanho esperado do ZIP oficial.

Verificação local: 22 verificações da inicialização, 64 do instalador,
25 da coordenação Java e 12 do progresso/interface passaram. O teste da sondagem
simula a conversão de stderr em `NativeCommandError` do PowerShell 5.1 e também
executa processos nativos reais com saída em stderr, conferindo códigos 0/7,
comando ausente e restauração da preferência. O teste
Java segura um script até receber uma atualização, verificando progresso e log
antes do término. A janela Swing foi exercitada no ambiente gráfico Linux: barra,
animação, responsividade da EDT e fechamento. Ainda é necessária a execução
completa no Windows PowerShell 5.1 e MySQL real.

A compilação e o Javadoc de todos os fontes passaram sem avisos. A regressão do
executor elevado concluiu mais 52 verificações, com a fronteira UAC simulada.
Para repetir os novos testes na raiz do projeto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tests\windows-start-contract.ps1
javac --release 22 -encoding UTF-8 -cp out/classes -d out/test-classes tests/PreparationProgressCheck.java tests/DatabasePreparationCheck.java
java -cp 'out/classes;out/test-classes' PreparationProgressCheck
java -cp 'out/classes;out/test-classes' DatabasePreparationCheck
```

## Configuração JDBC ausente na primeira abertura

O relato com `NoSuchFileException: database/config/database.properties` mostrou
que o MySQL havia passado pelas etapas de verificação, inicialização e início.
O `stop` seguinte era a limpeza da sessão após a falha Java. O diálogo mostrava
somente o caminho porque a mensagem original dessa exceção é o nome do arquivo.

Esse arquivo local é ignorado pelo Git. A entrada agora prepara a configuração
ausente usando `database.properties.example`, valida seu conteúdo e confere o
Connector/J **antes** de preparar o servidor. Mantém arquivos existentes e conserva
a exceção original nos detalhes, com mensagem explicando ausência, leitura ou
conteúdo inválido. A cópia não usa substituição de arquivos existentes.

No comando do IntelliJ fornecido, o classpath continha somente
`out/production/tag-file`. O Connector/J também precisa estar disponível:
`lib/mysql-connector-j-9.7.0.jar` agora é versionado e acompanha o clone. O módulo
`tag-file.iml` já declara o JAR relativo à pasta do projeto. Em cópias antigas,
atualize também o JAR e confira o módulo da configuração **Run**. Se a biblioteca
não aparecer, verifique **Project Structure > Modules > Dependencies**. A verificação do driver não abre conexão de rede.
[Referência Java: DriverManager](https://docs.oracle.com/en/java/javase/24/docs/api/java.sql/java/sql/DriverManager.html).

Verificação local: 10 verificações de configuração sem driver e as mesmas 10 com
o JAR real; mais 5 verificações da entrada sem driver e 5 com driver. Os testes da
entrada usam a janela Swing real, uma pasta temporária e scripts que interrompem
na primeira verificação, comprovando que configuração/driver são conferidos antes
de iniciar os scripts. Nenhum MySQL foi instalado ou iniciado por esses testes.

```powershell
javac --release 22 -encoding UTF-8 -cp out/classes -d out/test-classes tests/DatabaseConfigurationCheck.java tests/ApplicationPrerequisitesCheck.java
java -cp 'out/classes;out/test-classes' DatabaseConfigurationCheck
java -cp 'out/classes;out/test-classes;lib/*' DatabaseConfigurationCheck driver-present
java -cp 'out/classes;out/test-classes' ApplicationPrerequisitesCheck
java -cp 'out/classes;out/test-classes;lib/*' ApplicationPrerequisitesCheck driver-present
```
