# Instalação e execução

Este documento descreve o ambiente **planejado** do Tag-File, conforme AMB-01 a AMB-07 da especificação. Não é um guia de execução já validado: o repositório inspecionado contém apenas o programa inicial em [`src/Main.java`](../src/Main.java), que imprime uma saudação e os números de 1 a 5. Não foram encontrados a aplicação Swing, `DatabaseManager`, `DatabaseConnection`, scripts de banco, schemas ou driver JDBC no conjunto de arquivos do projeto.

Nenhuma instalação, aplicação, compilação, consulta SQL, preparação de dados ou parada de serviço foi executada nesta missão. A inspeção foi de arquivos como texto. O inventário e as divergências gerais estão em [decisões e pendências](decisoes-e-pendencias.md); o fluxo funcional de abertura está em [UC-01](casos-de-uso.md#uc-01).

| Evidência observada | O que comprova | Limite da conclusão |
|---|---|---|
| [`.idea/misc.xml`](../.idea/misc.xml), componente `ProjectRootManager` | `languageLevel="JDK_24"` e SDK nomeado `corretto-24` | Configuração da IDE; não prova instalação do SDK ou execução com ele. |
| [`tag-file.iml`](../tag-file.iml), `NewModuleRootManager` | Fonte em `src`, JDK herdado e entrada de fontes | Não há entrada de biblioteca Connector/J nesse módulo. |
| [`src/Main.java`](../src/Main.java), `Main.main` | Código inicial de demonstração | Não implementa abertura, interface ou administração do Tag-File. |
| Inventário dos arquivos do projeto | Não encontrados `database`, `lib`, scripts SQL ou JAR do driver | Não verifica programas instalados fora do repositório. |
| [`.gitignore`](../.gitignore) | Regras de ferramentas/IDE e saídas de compilação | Não contém regras específicas para dados, logs ou configuração real de `database`. |

<a id="amb-01"></a>

## AMB-01 — Plataformas e autorização para instalar

**Estado: confirmado como requisito; implementação não encontrada.** As plataformas consideradas são Windows, Ubuntu e Linux Mint. Na abertura, o aplicativo deve verificar a disponibilidade dos componentes de cliente e servidor MySQL. Se algum componente necessário faltar, deve solicitar autorização antes da instalação.

| Plataforma | Solução prevista | Ainda não definido |
|---|---|---|
| Windows | Scripts PowerShell e solicitação de elevação pelo UAC | Instalador, obtenção dos binários, comandos definitivos e detecção completa. |
| Ubuntu e Linux Mint | Scripts Bash para verificação, instalação e administração | Pacotes, comandos e mecanismo definitivo de autenticação; `pkexec` foi somente uma possibilidade. |

O uso de `ProcessBuilder` para iniciar scripts/processos externos pelo Java foi aprovado. Verificar somente o PATH não foi definido como estratégia completa de localização de instalações.

O cliente `mysql` pode preparar ou administrar a estrutura; o servidor `mysqld` atende às conexões. Eles não são dois servidores que precisam ficar abertos. O driver JDBC Java também não substitui o servidor e não requer abrir o cliente de terminal a cada consulta dos DAOs.

Recusar ou cancelar a autorização não significa instalação concluída. A autorização para instalar não exige manter a interface permanentemente elevada e não autoriza redefinir credenciais ou modificar dados de outras instâncias. Falhas devem informar as etapas concluídas e a etapa que falhou, conforme [ERR-01](requisitos-e-regras.md#err-01).

Não foram fechadas versões de servidor, fontes de download, repositórios de pacotes ou distribuição de binários junto da aplicação. Esses detalhes estão em [P-11](decisoes-e-pendencias.md#p-11). Nenhum dos procedimentos foi executado para descobri-los.

<a id="amb-02"></a>

## AMB-02 — Estrutura de `database` e localização dos dados

**Estado: organização confirmada; nomes operacionais de referência; diretórios não encontrados no repositório.** `database` é relativo à raiz do projeto, assim como `doc`. Não significa `/database` na raiz do sistema operacional.

A estrutura abaixo é uma referência documental, não um inventário de arquivos existentes:

```text
<raiz-do-projeto>/
└── database/
    ├── config/
    │   ├── database.properties.example
    │   ├── database.properties
    │   ├── mysql-windows.ini.template
    │   └── mysql-linux.cnf.template
    ├── scripts/
    │   ├── windows/
    │   │   ├── check.ps1
    │   │   ├── install.ps1
    │   │   ├── initialize.ps1
    │   │   ├── start.ps1
    │   │   └── stop.ps1
    │   └── linux/
    │       ├── check.sh
    │       ├── install.sh
    │       ├── initialize.sh
    │       ├── start.sh
    │       └── stop.sh
    ├── schema/
    │   └── scripts de criação e alteração (nomes não fechados)
    └── runtime/
        ├── data/
        ├── logs/
        └── run/
```

`config` reúne configurações e modelos; `scripts` separa tarefas por plataforma; `schema` reúne SQL; `runtime/data` guarda dados da instância; `runtime/logs` e `runtime/run` destinam-se a logs e arquivos de execução.

| Referência de script | Responsabilidade prevista |
|---|---|
| `check` | Verificar os componentes necessários. |
| `install` | Instalar componentes ausentes após autorização. |
| `initialize` | Preparar pela primeira vez um diretório de dados ainda não inicializado. |
| `start` | Iniciar ou reconhecer a instância administrada, conforme mecanismo ainda pendente. |
| `stop` | Participar da parada da instância identificada como pertencente ao Tag-File. |

A localização de dados consolidada é `database/runtime/data`. A primeira preparação utiliza o subdiretório vazio/ainda não preparado. As aberturas seguintes reutilizam os dados. A proposta anterior de inicializar fora de `database` e transferir os dados foi descartada; o subdiretório é permitido.

Para schema, apareceram as alternativas `001_initial_schema.sql`/`002_add_indexes.sql` e `create.sql`/`update.sql`. Não são dois mecanismos cumulativos. Não há arquivo existente que resolva a nomenclatura, e o nome `add_indexes` não aprova um índice específico. O requisito de criação e alteração, com estratégia detalhada aberta e sem `schema_history`, está em [SQL-06](banco-de-dados.md#sql-06) e [P-11](decisoes-e-pendencias.md#p-11).

O `.gitignore` observado não define tratamento específico para esse ambiente ainda ausente. Isso não equivale a aprovar o versionamento de dados reais, logs ou credenciais pessoais. A política concreta permanece a definir; nenhum arquivo de controle foi alterado nesta missão.

<a id="amb-03"></a>

## AMB-03 — Parâmetros públicos de desenvolvimento

**Estado: referência consolidada; não encontrada configuração operacional correspondente.** Estes valores foram deliberadamente definidos para a instância local didática:

| Parâmetro | Referência |
|---|---|
| Usuário dos DAOs | `root` |
| Senha pública didática | `TagFile123!` |
| Endereço local | `127.0.0.1` |
| Porta | `3333` |
| Banco | `tag_file` |
| Reconexão | `autoReconnect=true` |
| Diretório de dados | `database/runtime/data` |

Exemplo de configuração, **não criado como arquivo operacional e não validado por conexão**:

```properties
db.url=jdbc:mysql://127.0.0.1:3333/tag_file?autoReconnect=true
db.user=root
db.password=TagFile123!
```

O cliente fornece a senha da conta existente no servidor; não há duas senhas independentes. A conta administrativa e a senha previsível são limitações conhecidas do projeto didático local, não uma recomendação geral de configuração para servidor exposto.

A instância discutida é exclusiva do Tag-File. Esses parâmetros não autorizam redefinir `root` de outras instalações. Não trocar silenciosamente usuário, senha ou porta; a porta `3333` substituiu o exemplo anterior `3307`. A necessidade de identificar porta ocupada e apresentar o erro pode ser documentada, mas mudar automaticamente de porta não foi aprovado.

Não foram reproduzidas credenciais pessoais de arquivos do usuário. A senha acima pode aparecer porque foi explicitamente definida como pública na especificação.

<a id="amb-04"></a>

## AMB-04 — Preparação inicial e aberturas seguintes

**Estado: sequência funcional discutida; procedimentos técnicos não homologados.** `DatabaseManager` administra a instância local; `DatabaseConnection` centraliza a conexão; a inicialização do domínio aplica as regras de registros. As responsabilidades detalhadas estão em [ARQ-07](arquitetura-e-padroes.md#arq-07) e [ARQ-08](arquitetura-e-padroes.md#arq-08).

Na primeira preparação, o comportamento esperado é:

1. Ler a configuração e verificar os componentes necessários.
2. Solicitar e realizar a instalação autorizada, se faltarem componentes.
3. Inicializar o diretório de dados ainda não preparado.
4. Iniciar o servidor administrado e preparar conta e banco.
5. Criar a estrutura inicial e disponibilizar a conexão da aplicação.
6. Executar a inicialização do domínio: limpeza exclusiva da abertura, atualização dos registros remanescentes e disponibilização dos exploradores.

Nas aberturas seguintes, o comportamento esperado é:

1. Reutilizar a instalação e os dados existentes.
2. Iniciar ou reconhecer a instância administrada.
3. Preparar as alterações necessárias do schema, conforme estratégia ainda pendente.
4. Conectar a aplicação.
5. Executar a limpeza exclusiva da inicialização, atualizar todos os `LocalFile` remanescentes e disponibilizar os exploradores.

Inicializar dados pela primeira vez é diferente de iniciar um servidor preparado. Uma conexão que falha não prova que o diretório precisa ser reinicializado e não autoriza apagá-lo ou abrir outro servidor sobre os mesmos dados.

A limpeza de [CIC-02](requisitos-e-regras.md#cic-02) remove apenas registros cuja única Tag seja `Etiqueta Ausente` e, defensivamente, registros sem associações, com a remoção das associações pertinente. Preserva arquivos físicos e a Tag de sistema. Indisponibilidade não é motivo isolado para excluir um registro. Essa limpeza não ocorre em Refresh ou reconexão, conforme [CIC-03](requisitos-e-regras.md#cic-03).

Preparação de dados, manutenção estrutural e limpeza de domínio são etapas diferentes. A preparação também não autoriza recriar em toda abertura as Tags predefinidas que o usuário excluiu; a identificação e preparação dessas Tags estão em [P-07](decisoes-e-pendencias.md#p-07).

O aplicativo deve possuir scripts SQL de criação e alteração. Consultar a estrutura existente antes de mudanças repetíveis foi uma sugestão; não há algoritmo completo de migração, reaplicação ou recuperação de schema parcial aprovado. Não executar `ALTER` cegamente a cada abertura, apagar/recriar tabelas ou introduzir `schema_history` para preencher essa lacuna. O modelo e seus limites constam de [SQL-01 a SQL-06](banco-de-dados.md#sql-01).

Comandos finais, tempos de espera, identificação da instância, instalação incompleta e recuperação permanecem em [P-11](decisoes-e-pendencias.md#p-11). Falha após uma etapa concluída exige resultado parcial, conforme [UC-15](casos-de-uso.md#uc-15), sem declarar tudo concluído nem restaurado.

<a id="amb-05"></a>

## AMB-05 — Conexão JDBC compartilhada e reconexão

**Estado: modelo simplificado confirmado; classes não encontradas.** Uma mesma instância de `DatabaseConnection` é compartilhada pelos DAOs, que guardam um atributo desse tipo. Ela concentra abertura, tratamento e fechamento da conexão JDBC. Não há pool nem conjunto adicional de classes para administrar sessões.

`autoReconnect=true` é a opção solicitada. Reconexão não garante a conclusão ou repetição de uma operação interrompida, não desfaz alterações e não elimina exceções. Não se deve inferir repetição automática de escritas. Como não há controle explícito de transações, mudanças em várias tabelas ou em banco e disco podem ficar parcialmente concluídas; ver [SQL-05](banco-de-dados.md#sql-05).

O Loading bloqueia novas interações conflitantes, mas isso não define sozinho como impedir tarefas automáticas simultâneas. Execução concorrente, prevenção de reentrância e validade da conexão não têm contrato final em [P-12](decisoes-e-pendencias.md#p-12). `isClosed()`, `isValid()`, `getConnection()` e `close()` apareceram como referências de API; não são implementação testada nem uma lista homologada de métodos.

DAOs não devem fechar inadvertidamente a conexão compartilhada depois de cada chamada. Seu fechamento cabe à classe central. Reconectar durante a sessão não repete a limpeza de `Etiqueta Ausente`. O Loading e a apresentação de falhas estão em [UI-03](interface-e-fluxos.md#ui-03) e [ERR-01](requisitos-e-regras.md#err-01).

<a id="amb-06"></a>

## AMB-06 — JDK, JDBC e dependências

**Estado: JDK 24 considerado na especificação e configurado na IDE; dependência do driver proposta, sem artefato encontrado.** O componente `ProjectRootManager` de `.idea/misc.xml` registra `JDK_24` e `corretto-24`. O módulo herda o JDK e define `src` como fonte. Não foi verificada a versão de um executável Java instalado nem a compatibilidade de execução da aplicação.

JDBC é a API Java de acesso a bancos; `Connection`, `PreparedStatement`, `ResultSet`, `DriverManager` e `SQLException` pertencem a essa API. O Connector/J é o driver de referência discutido para comunicar-se com MySQL. Portanto, conseguir escrever os imports de JDBC não comprova que o driver, o servidor ou a conexão estejam disponíveis.

Maven foi expressamente rejeitado. A inclusão manual do JAR do Connector/J foi a proposta utilizada, com esta referência:

```text
<raiz-do-projeto>/
└── lib/
    └── mysql-connector-j-<versão>.jar
```

`<versão>` é uma decisão ainda aberta, não um nome de arquivo pronto. Não há `lib` ou JAR correspondente no inventário, e `tag-file.iml` não registra biblioteca adicional. As versões finais do MySQL Server e Connector/J permanecem em [P-11](decisoes-e-pendencias.md#p-11).

Quando a implementação e as dependências existirem, preparar a execução exigirá distinguir estas tarefas: disponibilizar o JDK configurado; obter a versão de driver decidida; colocar seu JAR no local adotado; incluí-lo como biblioteca/classpath da IDE ou da execução; completar e conferir configurações e scripts; então validar o fluxo de abertura. Colocar o arquivo em `lib` e configurar o classpath são ações distintas. Esta enumeração documenta requisitos de preparação futura, sem executar ou certificar nenhuma dessas tarefas.

Não há comando de execução do Tag-File validado para fornecer neste estado: executar o `Main` observado só alcançaria o template inicial. Não se apresentam comandos para scripts inexistentes como se estivessem prontos. Também não foram acrescentados Maven, Gradle, Spring, Hibernate, ORM, pool ou outra infraestrutura como substituição.

A evolução relevante documentada é a do próprio projeto: o JDK 24 foi considerado, Maven foi rejeitado e o driver manual foi proposto. A inspeção não fornece histórico técnico que permita atribuir mudanças do projeto a lançamentos recentes de Java, MySQL ou Connector/J; nenhuma versão adicional ou necessidade de migração foi presumida.

<a id="amb-07"></a>

## AMB-07 — Encerramento e preservação

**Estado: responsabilidade aprovada; sequência completa não definida; implementação não encontrada.** `DatabaseManager` administra os procedimentos de parada da instância do Tag-File. `DatabaseConnection` concentra o fechamento da conexão compartilhada. A ordem completa do encerramento, o reconhecimento de processo já iniciado e o tratamento de fechamento forçado não foram formalizados.

Não é permitido presumir que todo `mysqld` encontrado pertence ao projeto. O aplicativo não deve parar arbitrariamente outros servidores MySQL do computador. Falha operacional também não autoriza apagar ou reinicializar dados existentes.

Os nomes `stop.ps1` e `stop.sh` são referências da estrutura planejada; esses arquivos não foram encontrados nem executados. Identificação da instância administrada, parada e recuperação permanecem em [P-11](decisoes-e-pendencias.md#p-11).

## Limite de validação deste documento

Foram lidos a especificação integral, o inventário de arquivos, a configuração da IDE/módulo, o `Main` inicial e o `.gitignore`. Os valores operacionais da tabela são referências aprovadas, não saídas de conexão ou de comandos. Nenhum arquivo de implementação, configuração real ou script foi criado para transformar os procedimentos acima em funcionamento. Os cenários futuros de ambiente são [ACE-23 e ACE-24](criterios-de-aceite.md#ace-23), ambos não executados.
