. "$PSScriptRoot/common.ps1"
Assert-Owner
# A indisponibilidade durante as sondagens e esperada. No PowerShell 5.1,
# stderr redirecionado pode virar NativeCommandError antes de ler LASTEXITCODE.
function Invoke-ConnectionProbe([string]$Command, [string[]]$Arguments) {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        # Executaveis nativos atualizam LASTEXITCODE no escopo global.
        $global:LASTEXITCODE = $null
        & $Command @Arguments 2>$null | Out-Null
        $code = $global:LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    if ($null -eq $code) { throw "Sondagem nao executou o cliente: $Command" }
    return $code
}
Write-SetupProgress 'Verificando servidor existente'
$probe = Invoke-ConnectionProbe 'mysqladmin' @("--defaults-extra-file=$ClientConfig", 'ping')
if ($probe -eq 0) { Assert-Server; exit 0 }
$pidFile = Join-Path $Run 'mysql.pid'
if ((Test-Path $pidFile) -and (Get-Process -Id ([int](Get-Content $pidFile)) -ErrorAction SilentlyContinue)) { throw 'Processo existente não responde; não iniciado outro' }
$arguments = @("--defaults-file=`"$Config`"")
$bootstrap = Join-Path $Run 'bootstrap.sql'
if (Test-Path $bootstrap) { $arguments += "--init-file=`"$bootstrap`"" }
Write-SetupProgress 'Iniciando servidor MySQL'
$server = Start-Process (Get-Command mysqld).Source -ArgumentList $arguments -PassThru -WindowStyle Hidden
Write-SetupProgress 'Aguardando conexão com o banco'
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    $probe = Invoke-ConnectionProbe 'mysql' @("--defaults-extra-file=$ClientConfig", '-e', 'SELECT 1')
    if ($probe -eq 0) { Assert-Server; if (Test-Path $bootstrap) { Remove-Item $bootstrap }; exit 0 }
    if ($server.HasExited) { throw 'Servidor encerrou; consulte database/runtime/logs/mysql.log' }
    Start-Sleep -Seconds 1
}
throw 'Tempo de inicialização excedido; dados e processo preservados'
