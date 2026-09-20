param([switch]$Authorized)
. "$PSScriptRoot/common.ps1"
if (!$Authorized) { throw 'Instalação não autorizada' }
# Instalação portátil não registra serviço nem altera contas de outras instâncias.
$archive = Join-Path $Runtime 'mysql-winx64.zip'
$destination = Join-Path $Runtime 'mysql'
if (Test-Path $destination) { & "$PSScriptRoot/check.ps1"; exit $LASTEXITCODE }
New-Item -ItemType Directory -Force $Runtime | Out-Null
Invoke-WebRequest 'https://dev.mysql.com/get/Downloads/MySQL-8.4/mysql-8.4.9-winx64.zip' -OutFile $archive
(Get-FileHash $archive -Algorithm SHA256).Hash | Set-Content "$archive.download.sha256"
$staging = Join-Path $Runtime ('extract-' + [guid]::NewGuid())
Expand-Archive -LiteralPath $archive -DestinationPath $staging
$binary = Get-ChildItem $staging -Filter mysqld.exe -Recurse | Select-Object -First 1
if (!$binary) { throw 'Pacote não contém mysqld.exe' }
if (Test-Path $destination) { throw 'Instalação concorrente detectada; destino preservado' }
Move-Item $binary.Directory.Parent.FullName $destination
Remove-Item $staging -Recurse
& "$PSScriptRoot/check.ps1"
exit $LASTEXITCODE
