. "$PSScriptRoot/common.ps1"
Assert-Owner
& mysqladmin "--defaults-extra-file=$ClientConfig" ping 2>$null
if ($LASTEXITCODE -eq 0) { Assert-Server; exit 0 }
$pidFile = Join-Path $Run 'mysql.pid'
if ((Test-Path $pidFile) -and (Get-Process -Id ([int](Get-Content $pidFile)) -ErrorAction SilentlyContinue)) { throw 'Processo existente não responde; não iniciado outro' }
$arguments = @("--defaults-file=`"$Config`"")
$bootstrap = Join-Path $Run 'bootstrap.sql'
if (Test-Path $bootstrap) { $arguments += "--init-file=`"$bootstrap`"" }
$server = Start-Process (Get-Command mysqld).Source -ArgumentList $arguments -PassThru -WindowStyle Hidden
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    & mysql "--defaults-extra-file=$ClientConfig" -e 'SELECT 1' 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { Assert-Server; if (Test-Path $bootstrap) { Remove-Item $bootstrap }; exit 0 }
    if ($server.HasExited) { throw 'Servidor encerrou; consulte database/runtime/logs/mysql.log' }
    Start-Sleep -Seconds 1
}
throw 'Tempo de inicialização excedido; dados e processo preservados'
