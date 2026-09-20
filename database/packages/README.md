# Pacote offline para Windows

Coloque `mysql-8.4.9-winx64.zip` nesta pasta e execute `Main` normalmente.
O instalador usa o arquivo antes de tentar qualquer download. O ZIP não é
versionado: ao levar o projeto a outro computador, copie também esse arquivo.
Como alternativa, defina `TAG_FILE_MYSQL_ARCHIVE` com o caminho completo do ZIP
no ambiente da configuração Run do IntelliJ.

Origem oficial:
https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.9-winx64.zip

Referência versionada: [mysql-windows-package.json](../config/mysql-windows-package.json).
SHA-256 esperado:
`5795ba250e89290f7507ed3bcc6a655be373616abb58b877acdea71e1b8f4e8c`.

Esse hash foi calculado sobre o pacote completo (280452577 bytes), obtido via
HTTPS do CDN oficial em 20/09/2026; todas as entradas ZIP passaram na verificação
CRC. Não é apresentado como assinatura digital ou checksum publicado pela Oracle.
Fixar a referência no projeto permite detectar arquivos diferentes/incompletos
em instalações futuras. Não substitua o valor pelo hash de um arquivo recusado.

O pacote inclui o servidor e ferramentas, não o JDK ou o driver JDBC.
O Windows precisa permitir execução dos binários x64 e ter o Microsoft Visual C++
Redistributable exigido pelo MySQL. O instalador não altera políticas do sistema.
Consulte [preparação portátil](../../doc/windows-portatil.md).
