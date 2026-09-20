#requires -Version 5.1
# Solicita UAC pelo Windows e devolve o resultado do PowerShell elevado.
param(
    [Parameter(Mandatory = $true)][string]$ScriptPath,
    [Parameter(Mandatory = $true)][string]$LogPath
)
$ErrorActionPreference = 'Stop'
try {
    $script = (Resolve-Path -LiteralPath $ScriptPath -ErrorAction Stop).ProviderPath
    # -File recebe um caminho literal; as aspas preservam espacos no nome.
    $arguments = '-NoLogo -NoProfile -ExecutionPolicy Bypass -File "{0}"' -f $script
    $process = Start-Process -FilePath (Join-Path $PSHOME 'powershell.exe') `
        -Verb RunAs -ArgumentList $arguments -WorkingDirectory (Get-Location).Path `
        -WindowStyle Normal -Wait -PassThru
    if ($null -eq $process.ExitCode) { throw 'O Windows nao informou o codigo de saida do script.' }
    exit $process.ExitCode
} catch {
    $failure = $_
    $code = 127
    $cause = $failure.Exception
    while ($null -ne $cause) {
        if ($cause -is [ComponentModel.Win32Exception] -and $cause.NativeErrorCode -eq 1223) {
            $code = 126
            break
        }
        $cause = $cause.InnerException
    }
    $message = if ($code -eq 126) { 'Autorizacao cancelada pelo usuario no UAC.' }
        else { $failure | Format-List * -Force | Out-String -Width 4096 }
    [IO.File]::AppendAllText($LogPath, $message, (New-Object Text.UTF8Encoding $false))
    Write-Error $message -ErrorAction Continue
    exit $code
}
