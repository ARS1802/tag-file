# Preparação portátil do MySQL no Windows

A abertura normal por `Main` prepara o MySQL usando a conta atual. Não chama UAC,
não registra serviço, não muda o Registro, as políticas permanentes de execução ou o firewall.
A pasta do projeto precisa ser gravável. `Main --build` continua sendo a opção
explícita de compilação elevada; no Linux permanece o executor já existente.

## Levar o projeto para a apresentação

1. Transfira os fontes, scripts e `database/config/mysql-windows-package.json`
   da mesma revisão. Uma edição local não chega ao Windows por um `git pull`
   enquanto não estiver no repositório remoto.
2. Leve também `database/packages/mysql-8.4.9-winx64.zip` e o Connector/J em `lib`.
   Ambos são dependências locais ignoradas pelo Git. Não é necessário levar dados
   de uma instância MySQL de outra máquina.
3. Use um JDK superior a 21, configure o classpath com `lib/*` e execute `Main`
   sem argumentos no IntelliJ. Ao faltar a instalação, confirme **Preparar**.
   **Cancelar**, ou fechar a confirmação, encerra sem instalar/inicializar dados.
4. No console Run aparece o caminho do script, seu SHA-256 e o log da etapa em
   `database/runtime/logs`. Em caso de falha, o diálogo informa esse log. Durante
   o download/extração a janela principal ainda não está aberta; acompanhe o log.

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
