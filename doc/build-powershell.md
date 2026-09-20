# Build PowerShell: contrato e verificação

O `scripts/build.ps1` compila os fontes Java e gera Javadoc. O alvo é Java 22,
interpretando o requisito **JDK superior à versão 21**. O compilador precisa
suportar `--release 22`; o Javadoc é selecionado na mesma pasta de ferramentas.
O script foi escrito para Windows PowerShell 5.1 e PowerShell 7. A execução
real em Windows PowerShell 5.1 ainda precisa ser validada.

## Execução no Windows

Na raiz do projeto, usando o caminho de um JDK completo instalado:

```powershell
$env:TAG_FILE_JDK = 'C:\caminho\do\jdk'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
if ($LASTEXITCODE -eq 0) {
    & "$env:TAG_FILE_JDK\bin\java.exe" -cp 'out/classes;lib/*' Main
}
```

Sem `TAG_FILE_JDK`, o script procura `javac` no PATH e usa o `javadoc` da mesma
pasta. Um caminho inválido na variável não é silenciosamente substituído pelo PATH.
O build direto usa as permissões da conta atual. `Main --build` mantém o pedido
de UAC solicitado para execução elevada. `Bypass` vale para o processo iniciado;
políticas de grupo e restrições de arquivos continuam aplicáveis.

Este ajuste abrange o build PowerShell. O script Bash mantém seu alvo 24 e o
IntelliJ mantém sua configuração própria. Ao alternar entre eles, confira qual
JDK gerou as classes utilizadas na execução.

## Garantias e limites

- Valida ferramentas, versão, alvo, fontes e destinos antes de substituir saídas.
- Usa somente arquivos `.java`, inclusive quando existe apenas um fonte.
- Usa caminhos literais e uma lista de argumentos UTF-8 com nomes delimitados.
  O `.ps1` usa BOM UTF-8 para leitura também pelo Windows PowerShell 5.1.
- Isola `JDK_JAVAC_OPTIONS`, `JDK_JAVA_OPTIONS`, `JAVA_TOOL_OPTIONS` e `_JAVA_OPTIONS`
  somente nos processos das ferramentas; informa os nomes presentes sem imprimir
  seus valores. Não modifica permanentemente o ambiente do usuário.
- Usa `out/.build.lock` com acesso exclusivo entre execuções deste script. O
  arquivo pode permanecer após o término: o bloqueio é o identificador aberto,
  não a existência do arquivo.
- Compila e gera documentação em `out/.build-<identificador>`. Só publica após
  sucesso de ambas. Uma falha tratada durante a publicação restaura os destinos
  anteriores; se a restauração falhar, preserva backups e informa o caminho.
- As duas pastas não são substituídas em uma única operação atômica. Encerramento
  forçado ou falta de energia podem interromper a publicação. Uma área `.build-*`
  remanescente bloqueia o próximo build para inspeção: preserve-a, confira as pastas
  `previous-classes`/`previous-javadoc` e restaure o par anterior antes de liberar
  uma nova execução. Não apague backups sem verificar seu conteúdo.
- Recusa links/junções nas saídas antes de movê-las ou limpá-las. Não altera o banco.
- Registra etapa, ferramentas, saídas e códigos em `out/build-logs/*.log`, em UTF-8.
  Avisos em stderr não transformam uma saída zero em falha. Cada ferramenta tem
  limite de dez minutos; sucesso significa compilação **e** Javadoc concluídos.
- O executor elevado usa [scripts/elevate.ps1](../scripts/elevate.ps1) para chamar
  `Start-Process powershell.exe -Verb RunAs -WindowStyle Normal -Wait -PassThru`.
  O console elevado mostra a saída durante a execução, sem outro PowerShell oculto.
  `Start-Transcript` registra mensagens do host, exceções e resultado em
  `tag-file-elevated-*.log`; escritas diretas em `[Console]` não são garantidas nessa
  transcrição. O diagnóstico completo do build permanece em `out/build-logs`.
  Em caso de falha, o console aguarda Enter antes de devolver o código ao Java.
  A espera do executor Windows é nativa, sem o antigo limite adicional de 14/15
  minutos. O limite de dez minutos por ferramenta do build permanece.

Compilar não verifica conexão SQL nem disponibilidade da GUI. O driver JDBC
continua sendo uma dependência de execução. Os comandos do instalador do MySQL
foram preservados; os scripts Windows receberam BOM UTF-8 para leitura correta no
Windows PowerShell 5.1. O erro anterior de `Invoke-WebRequest` precisa de seu
diagnóstico completo para identificar a causa do download.

## Testes reproduzíveis

Os testes usam diretórios temporários e não solicitam elevação nem iniciam o MySQL.
O teste de captura executa o script que rodaria depois do UAC e o launcher real,
substituindo apenas a chamada que solicita privilégios. Inclui progresso visível
antes do término, códigos de saída, exceções, argumentos literais e cancelamento
simulado. A suite PowerShell usa ferramentas JDK reais, não mocks do compilador.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tests\build-contract.ps1 -Jdk $env:TAG_FILE_JDK
& "$env:TAG_FILE_JDK\bin\javac.exe" --release 22 -encoding UTF-8 -cp out/classes -d out/test-classes tests/ElevatedCaptureCheck.java
if ($LASTEXITCODE -eq 0) {
    & "$env:TAG_FILE_JDK\bin\java.exe" -cp 'out/classes;out/test-classes' ElevatedCaptureCheck "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe"
}
```

O parâmetro opcional `-IncompatibleJdk` da suite aponta para um JDK anterior a 22
para verificar sua recusa. Repita os testes com `pwsh` para cobrir PowerShell 7.

## Evidências iniciais de 20/09/2026

Este registro descreve a versão anterior do executor; a substituição por console
nativo e seus resultados estão registrados ao final deste documento.

Ambiente executado: Linux, PowerShell portátil 7.6.5 e Corretto 24.0.2. O pacote
PowerShell foi obtido da distribuição oficial e seu SHA-256 foi conferido. Ele
ficou em `/tmp`, sem instalação no sistema.

Resultado: 44 verificações da suite de build e 18 da captura elevada passaram,
além da compilação e do Javadoc completos do projeto, sem avisos.

| Verificação | Resultado |
|---|---|
| Regressão no script antigo: JDK inexistente deve preservar classes anteriores | Reproduzida: o teste falhou porque as classes anteriores eram apagadas. |
| Mesmo cenário no script corrigido | Passou. |
| JDK inválido/antigo, Javadoc ausente, zero fontes e erros Java/Javadoc | Falhas identificadas e saídas anteriores preservadas. |
| Um/múltiplos fontes; espaços, acentos, apóstrofo, cifrão e colchetes | Compilação e documentação concluídas; diretório terminado em `.java` ignorado. |
| Aviso real do Javadoc em stderr com código zero | Build bem-sucedido, aviso preservado no diagnóstico. |
| Bloqueio concorrente, rollback da segunda publicação e área interrompida | Passaram; backups não foram descartados indevidamente. |
| Link em saída e opções de ambiente conflitantes | Destino externo e ambiente do chamador preservados. |
| Projeto completo, 34 fontes | Compilação e Javadoc passaram; `Main.class` com major version 66, correspondente ao alvo 22. |
| Captura elevada: códigos 0/7, exceção, argumentos especiais e UTF-8 | Passou usando o comando real após a etapa de autorização. |
| Captura de mais de 128 KiB em cada canal | Conteúdo integral, sem bloqueio dos buffers. |
| Windows PowerShell 5.1, UAC real, ACL/Execution Policy e arquivo bloqueado no Windows | Não executados neste host. A suite inclui o teste de arquivo bloqueado quando executada no Windows. |

Logs da verificação local: `/tmp/tag-file-build-contract.log`,
`/tmp/tag-file-powershell-project-build.log` e `/tmp/tag-file-elevated-capture.log`.
Esses arquivos temporários não são versionados; os testes acima permitem reproduzir
as verificações. Sucesso no Linux não deve ser apresentado como certificação Windows.

## Correção posterior: autorização do instalador

O log `tag-file-elevated-10250527699575625213.log.txt` identificou uma regressão
no executor: a chamada PowerShell recebia `'-Authorized'` como texto posicional.
Isso deixava `[switch]$Authorized` desativado e causava a exceção da linha 3 do
instalador, antes de qualquer download. O chamado UAC e o parâmetro lógico de
autorização do script são etapas distintas.

O executor agora emite o switch conhecido `-Authorized` como parâmetro PowerShell;
os demais argumentos continuam escapados como valores literais. A guarda do
instalador permanece ativa. Somente o nome exato desse switch, sem diferenciar
maiúsculas, recebe esse tratamento: texto com comandos adicionais não é executado.

O log também mostrou mensagens de progresso em CLIXML e acentos corrompidos.
Naquela revisão, o executor definia `ProgressPreference=SilentlyContinue` antes de
carregar módulos, em todas as camadas PowerShell. Isso suprimia progresso, mantendo
erros e códigos. A revisão abaixo remove essa supressão do executor.
Os seis arquivos `.ps1` de `database/scripts/windows` receberam somente o BOM
UTF-8, sem alterar seus comandos ou regras.

A regressão foi reproduzida antes da correção usando as três primeiras linhas do
instalador real e seu `common.ps1`, interrompendo a fixture antes do download.
Após a correção, 49 verificações de captura passaram no PowerShell 7.6.5/Linux:
incluem autorização, recusa sem autorização, argumento inválido, preservação dos
acentos e ausência de progresso no log. O projeto e o Javadoc também compilaram.
UAC real, Windows PowerShell 5.1 e download/instalação MySQL não foram executados.

Logs desta regressão: `/tmp/tag-file-authorization-before.log`,
`/tmp/tag-file-authorization-after.log` e `/tmp/tag-file-authorization-build.log`.

## Substituição do executor Windows por console nativo

O relato seguinte foi de uma execução pelo IntelliJ que permanecia ativa sem abrir
a interface nem apresentar erro. A investigação identificou que o executor antigo
criava um PowerShell oculto e só gravava sua saída depois do término. Um teste com
uma mensagem inicial seguida de espera reproduziu a falta de visibilidade: a
mensagem não aparecia enquanto o script trabalhava, embora terminasse com sucesso.
Isso demonstra a retenção da saída; não comprova a causa exata da espera relatada
no Windows.

O fluxo atual é:

1. Java informa o script e o caminho do log no console do IntelliJ.
2. `scripts/elevate.ps1` solicita UAC usando `Start-Process` com `-Verb RunAs`.
3. O PowerShell elevado executa diretamente o `.ps1` temporário via `-File`, com
   console visível e transcrição nativa. O progresso do instalador fica habilitado.
4. O launcher aguarda e devolve o código. Cancelamento do UAC não executa o script;
   uma falha do script fica visível até pressionar Enter. `Main` então recebe a falha
   para seu diálogo de erro. Após instalação bem-sucedida, a preparação do aplicativo
   continua.

Foram removidos os comandos em Base64 e a camada que criava outro PowerShell oculto
para capturar seus canais. O JDK da aplicação e o switch `-Authorized` continuam
transmitidos; os demais argumentos são valores literais. O build ainda mantém sua
própria captura dos compiladores, seus logs e seus limites de execução.

Na verificação local com PowerShell 7.6.5/Linux e Corretto 24.0.2, o teste de
visibilidade passou com o novo executor, e `ElevatedCaptureCheck` concluiu 52
verificações, incluindo sucesso, falha, cancelamento simulado e a guarda real do
instalador. A compilação e o Javadoc de todos os fontes também foram executados
pelo novo script, em uma cópia temporária, sem avisos.

Não foram executados UAC real, Windows PowerShell 5.1, GUI nem download/instalação
do MySQL. A validação no Windows deve acompanhar o console elevado e os logs;
cancelar o UAC deve encerrar a inicialização sem executar a instalação.

Referência do mecanismo nativo:
[Start-Process, RunAs e espera pelo processo](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.management/start-process?view=powershell-5.1).
