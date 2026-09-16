. "$PSScriptRoot/common.ps1"
foreach ($binary in 'mysqld', 'mysql', 'mysqladmin') {
    if (!(Test-Path (Join-Path $Runtime "mysql/bin/$binary.exe"))) { Write-Error "Componente portátil ausente: $binary"; exit 4 }
}
& mysqld --version
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& mysql --version
exit $LASTEXITCODE
