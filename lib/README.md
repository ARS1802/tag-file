# Driver JDBC manual

Use `mysql-connector-j-9.7.0.jar` nesta pasta, sem descompactar. O JAR é ignorado pelo Git.

```sh
curl -fL -o lib/mysql-connector-j-9.7.0.jar https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.7.0/mysql-connector-j-9.7.0.jar
```

O Connector/J 9.7.0 suporta MySQL 8.0 ou posterior e JDBC 4.2, segundo as
[notas oficiais](https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-7-0.html).
Esta implementação foi executada com JDK 24.0.2, MySQL 8.4.9 e esse JAR.
Compilar usa apenas a API JDBC do JDK; conectar ao servidor exige o driver no classpath.

No IntelliJ, adicione o JAR em **Project Structure → Modules → Dependencies**.
Nos comandos: `-cp 'out/classes:lib/*'` no Linux e `-cp 'out/classes;lib/*'` no Windows.
