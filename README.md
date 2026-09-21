# Tag-File

Aplicativo didático Java, Swing e MySQL para classificar arquivos com etiquetas. Requer JDK superior à versão 21.

- [Compilação, execução e organização do código](doc/implementacao.md)
- [Decisões confirmadas e contratos técnicos](doc/decisoes-implementacao.md)
- [Matriz de verificação e limitações](doc/verificacao.md)
- [Documentação do domínio e arquitetura](doc/README.md)
- [Análise da versão mínima do Java e do driver JDBC](doc/compatibilidade-java.md)

Sem Maven/Gradle. O Connector/J já acompanha o repositório em `lib`, com a dependência configurada no IntelliJ e nos scripts. Veja [versão e integridade](lib/README.md).
Com um JDK superior à versão 21 no PATH:

```sh
bash scripts/build.sh &&
java -cp 'out/classes:lib/*' Main
```

Se o JDK não estiver no PATH, defina `TAG_FILE_JDK` para o script de compilação e use o `bin/java` do mesmo JDK na execução. Execute da raiz do projeto, com ambiente gráfico e a configuração do banco descrita no guia. O `Main` abre a aplicação na pasta pessoal do usuário; a interface está em `src/GUI`.

O script Bash verifica a compatibilidade do compilador com o alvo configurado antes de limpar as saídas anteriores. Confira a [configuração de compilação](doc/compatibilidade-java.md#configuração-de-compilação). `TAG_FILE_JDK` seleciona o compilador do script; essa variável não muda o comando `java` do terminal. O `&&` executa o programa somente se a compilação terminar com sucesso.

Com `Main` já compilado, `java -cp 'out/classes:lib/*' Main --build` solicita autorização nativa do sistema para executar o script de compilação. Use o mesmo JDK também nesse comando. Na abertura normal, a mesma autorização é solicitada caso seja necessário instalar os componentes do MySQL. Veja [elevação de scripts](doc/implementacao.md#execução-de-scripts-com-elevação).
