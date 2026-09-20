. "$PSScriptRoot/common.ps1"
& "$PSScriptRoot/check.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (Test-Path (Join-Path $Data 'mysql')) { Assert-Owner; exit 0 }
if ((Test-Path $Data) -and (Get-ChildItem -Force $Data | Measure-Object).Count -gt 0) { throw 'Dados incompletos preservados; inicialização recusada' }
New-Item -ItemType Directory -Force $Data, $Run, (Join-Path $Runtime 'logs') | Out-Null
$template = Get-Content (Join-Path $ProjectRoot 'database/config/mysql-windows.ini.template') -Raw
[IO.File]::WriteAllText($Config, $template.Replace('@ROOT@', $ProjectRoot.Replace('\','/')), (New-Object Text.UTF8Encoding $false))
& mysqld --no-defaults --initialize-insecure "--datadir=$Data" "--log-error=$Runtime/logs/initialize.log"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
[IO.File]::WriteAllText((Join-Path $Runtime 'instance.owner'), $ProjectRoot)
$client = "[client]`nhost=127.0.0.1`nport=3333`nprotocol=TCP`nuser=root`npassword=TagFile123!`nconnect-timeout=3`n"
[IO.File]::WriteAllText($ClientConfig, $client)
[IO.File]::WriteAllText((Join-Path $Run 'bootstrap.sql'), "ALTER USER 'root'@'localhost' IDENTIFIED BY 'TagFile123!';`nCREATE DATABASE IF NOT EXISTS tag_file CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin;`n")
