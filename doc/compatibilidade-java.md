# Compatibilidade Java e JDBC — 20/09/2026

## Requisito do projeto

O projeto requer **JDK superior à versão 21**.

## Verificação de compatibilidade

Os 34 arquivos de `src` compilaram sem modificações
com o JDK 21.0.12 e `--release 21`. A versão mínima, mantendo exatamente os fontes
atuais, é Java 21: há uma chamada a `List.getFirst()` em
`LocalFileManager.resolveAvailable`, e esse método foi acrescentado no Java 21.
[Referência da API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#getFirst()).

Escopo: verificação de compatibilidade, em profundidade de trabalho, do checkout
local baseado em `9293e728639bbbdf4c99b65a51ca2578ce38e1cf`, incluindo as alterações
locais de GUI, inicialização e execução elevada de scripts. Esta análise não altera
o alvo dos scripts nem o JDK configurado no IntelliJ.

## Evidências

| Verificação | Resultado |
|---|---|
| Compilar todos os fontes com `javac --release 20` | Falha em `selected.getFirst()`, em `src/manager/LocalFileManager.java`. |
| Compilar todos os fontes com `javac --release 21` | Sucesso, sem modificar os fontes e sem precisar do JAR no classpath de compilação. |
| Carregar Connector/J 9.7.0 com Java 21.0.12 | Sucesso; `DriverManager.getDriver` encontrou `com.mysql.cj.jdbc.Driver`. |
| Carregar o mesmo driver com Java 24.0.2 | Sucesso. |
| Executar as verificações existentes de instalação/elevação com Java 21 | Sucesso; cancelamento e erros simulados, sem executar processos privilegiados. |
| Interface e operações SQL completas com Java 21 | Não executadas nesta análise. |

Os testes de carregamento não abriram conexão SQL. A compilação isolada foi feita
em diretório temporário, preservando a saída de compilação em `out/classes`.

## Configuração de compilação

O argumento `--release` determina a versão das APIs disponíveis na compilação e do
bytecode gerado. O compilador selecionado precisa suportar esse alvo; a versão usada
para executar a aplicação também precisa ser compatível com os arquivos `.class`.

A atualização inicial do requisito abrangeu somente a documentação. Na correção
posterior aprovada, `scripts/build.ps1` passou a usar `--release 22` e a conferir
o JDK antes de substituir os resultados. Veja o [contrato e a verificação do build
PowerShell](build-powershell.md). `scripts/build.sh` mantém `--release 24`, e
`.idea/misc.xml` seleciona o SDK `corretto-24`. Confira o alvo do caminho de
compilação utilizado; depois de alterar o alvo, recompile os fontes antes de executar.

## Driver JDBC

O artefato obtido foi `com.mysql:mysql-connector-j:9.7.0`, mantido em
`lib/mysql-connector-j-9.7.0.jar`. O download foi validado contra o SHA-256 publicado
no Maven Central e apresentou o mesmo conteúdo do JAR que já estava no diretório.
O módulo `tag-file.iml` agora declara essa dependência; os comandos de terminal já
incluem `lib/*` no classpath. O JAR permanece ignorado pelo Git.

JDBC é a API fornecida pelo JDK; Connector/J é a implementação que permite comunicar
com MySQL. A versão 9.7.0 implementa JDBC 4.2 e suporta MySQL 8.0 ou posterior,
abrangendo a instância MySQL 8.4.9 do projeto.
[Notas da versão 9.7.0](https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-7-0.html).
