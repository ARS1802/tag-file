# Driver JDBC incluído no projeto

`mysql-connector-j-9.7.0.jar` é versionado nesta pasta e acompanha o clone do projeto. Não é necessário baixar ou adicionar o driver manualmente no uso normal. Mantenha o JAR sem descompactar.

Origem oficial do artefato publicado no Maven Central (somente para manutenção):

```sh
curl -fL -o lib/mysql-connector-j-9.7.0.jar https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.7.0/mysql-connector-j-9.7.0.jar
```

O Connector/J 9.7.0 suporta MySQL 8.0 ou posterior e JDBC 4.2, segundo as
[notas oficiais](https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-7-0.html).
Esta implementação foi executada com JDK 24.0.2, MySQL 8.4.9 e esse JAR.
Compilar usa apenas a API JDBC do JDK; conectar ao servidor exige o driver no classpath.

O módulo `tag-file.iml` já inclui este JAR nas dependências do IntelliJ. Ele aparece em
**Project Structure → Modules → Dependencies** como **MySQL Connector/J 9.7.0**.
Nos comandos: `-cp 'out/classes:lib/*'` no Linux e `-cp 'out/classes;lib/*'` no Windows.

Download conferido em 20/09/2026, com SHA-256 igual ao publicado no Maven Central. A referência também está em `mysql-connector-j-9.7.0.jar.sha256`:

```text
0353648eaa1c91e0f4020c959abf756bc866ffd583df22ae6b6f6e0cbd43eb44
```

O driver foi carregado automaticamente pelo `DriverManager` tanto no Java 21.0.12
quanto no Java 24.0.2, sem abrir conexão com o banco. A necessidade de versão do
Java para o código da aplicação está na [análise de compatibilidade](../doc/compatibilidade-java.md).

O JAR original foi preservado, incluindo seus arquivos de licença. Para conferir
a integridade no Linux, execute `sha256sum -c mysql-connector-j-9.7.0.jar.sha256`
dentro de `lib`. No PowerShell, use `Get-FileHash -Algorithm SHA256` sobre o JAR
e compare com a referência acima.
