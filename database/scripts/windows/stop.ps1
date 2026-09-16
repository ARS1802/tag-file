. "$PSScriptRoot/common.ps1"
Assert-Server
$serverId = [int](Get-Content (Join-Path $Run 'mysql.pid'))
$processInfo = Get-CimInstance Win32_Process -Filter "ProcessId=$serverId"
if (!$processInfo -or !$processInfo.CommandLine.Contains($Config)) { throw 'Processo não identificado como Tag-File' }
& mysqladmin "--defaults-extra-file=$ClientConfig" shutdown
exit $LASTEXITCODE
