# Tag-File

Aplicativo Java com interface Swing e MySQL para classificar arquivos com etiquetas.

## Requisitos

- Git e **JDK superior à versão 21**, com `java`, `javac` e `javadoc` disponíveis no `PATH` e pertencentes ao mesmo JDK.
- Ambiente gráfico e permissão de escrita na pasta do projeto.
- Windows x64 com PowerShell 5.1 ou posterior e as dependências nativas do MySQL, incluindo o Microsoft Visual C++ Redistributable.
- No Linux x86_64: Bash, `curl`, `tar`, suporte a XZ, `libaio` e `pkexec` com agente de autenticação na sessão gráfica.

O driver JDBC já acompanha o repositório em `lib`. A aplicação prepara sua própria instância MySQL, na porta local **3333**, que deve estar livre. A primeira preparação precisa de acesso à internet para baixar o MySQL, salvo quando houver um [pacote offline para Windows](database/packages/README.md).

## Clonar

```sh
git clone https://github.com/ARS1802/tag-file.git
cd tag-file
```

Execute os comandos seguintes na raiz do projeto. Confira o Java selecionado:

```sh
java -version
javac -version
```

## Compilar e executar

### Windows — PowerShell

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
if ($LASTEXITCODE -eq 0) {
    java -cp 'out/classes;lib/*' Main
}
```

O script gera as classes em `out/classes` e a documentação em `out/javadoc`. A aplicação só é iniciada se a compilação terminar com sucesso.

### Linux — Bash

```bash
mkdir -p out/classes
find src -type f -name '*.java' | sort > out/sources.txt
javac --release 22 -encoding UTF-8 -cp 'lib/*' -d out/classes @out/sources.txt &&
java -cp 'out/classes:lib/*' Main
```

### Selecionar outro JDK

Se o terminal estiver usando um Java antigo, coloque o diretório `bin` do JDK desejado no início do `PATH` antes de compilar e executar. Substitua os caminhos dos exemplos pela instalação local.

No PowerShell:

```powershell
$env:TAG_FILE_JDK = 'C:\caminho\do\jdk'
$env:Path = "$env:TAG_FILE_JDK\bin;$env:Path"
```

No Bash:

```bash
export TAG_FILE_JDK='/caminho/do/jdk'
export PATH="$TAG_FILE_JDK/bin:$PATH"
```

## Primeira abertura

Ao solicitar a preparação do MySQL, confirme **Preparar** e aguarde o progresso. No Linux, o sistema solicita autorização para instalar os componentes. No Windows, a preparação usa as permissões da conta atual.

A configuração local é criada automaticamente a partir de `database/config/database.properties.example`. O banco fica em `database/runtime/data` e é reutilizado nas próximas execuções. A janela principal abre na pasta pessoal do usuário.

Em caso de falha, use **Mostrar detalhes técnicos**, **Abrir pasta de logs** ou **Copiar caminho do log**, conforme as opções disponíveis no diálogo. Os logs do banco ficam em `database/runtime/logs`; os da compilação PowerShell, em `out/build-logs`.

## Execuções seguintes

Na raiz do projeto, execute apenas o comando correspondente ao sistema:

Windows — PowerShell:

```powershell
java -cp 'out/classes;lib/*' Main
```

Linux — Bash:

```bash
java -cp 'out/classes:lib/*' Main
```

Após atualizar os fontes, repita a compilação antes de executar.

## Documentação

- [Compilação, execução e organização do código](doc/implementacao.md)
- [Preparação do MySQL no Windows e execução offline](doc/windows-portatil.md)
- [Decisões confirmadas e contratos técnicos](doc/decisoes-implementacao.md)
- [Matriz de verificação e limitações](doc/verificacao.md)
- [Documentação do domínio e arquitetura](doc/README.md)
- [Compatibilidade Java](doc/compatibilidade-java.md) e [driver JDBC](lib/README.md)
