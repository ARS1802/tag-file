# Tag-File

Aplicativo didático Java 24, Swing e MySQL para classificar arquivos com etiquetas.

- [Compilação, execução e exemplos dos 24 tipos](doc/implementacao.md)
- [Decisões confirmadas e contratos técnicos](doc/decisoes-implementacao.md)
- [Matriz de verificação e limitações](doc/verificacao.md)
- [Documentação do domínio e arquitetura](doc/README.md)

Sem Maven/Gradle. Coloque o Connector/J manualmente em `lib` conforme [estas instruções](lib/README.md).
Com o JDK 24 no PATH:

```sh
bash scripts/build.sh
java -cp 'out/classes:lib/*' Main --demo-local
```

Se o JDK não estiver no PATH, defina `TAG_FILE_JDK` para o script de compilação e use o `bin/java` do mesmo JDK na execução. As demonstrações locais não precisam de banco ou janela. Os modos de banco e interface têm preparação própria documentada.
