param([switch]$Authorized, [string]$ArchivePath = $env:TAG_FILE_MYSQL_ARCHIVE)
. "$PSScriptRoot/common.ps1"
if (!$Authorized) { throw 'Instalação não autorizada' }
# Instalação portátil: somente arquivos do projeto, sem serviço ou elevação.
$lock = $null
$staging = $null
$step = 'preparação'
try {
    Write-Host "Instalador portátil: $PSCommandPath"
    Write-Host "SHA-256: $((Get-FileHash -LiteralPath $PSCommandPath -Algorithm SHA256).Hash)"
    Write-Host "PowerShell: $($PSVersionTable.PSVersion)"
    $destination = Join-Path $Runtime 'mysql'
    if (Test-Path -LiteralPath $destination) {
        & "$PSScriptRoot/check.ps1"
        exit $LASTEXITCODE
    }
    $step = 'acesso à pasta do projeto / bloqueio de instalação'
    [void][IO.Directory]::CreateDirectory($Runtime)
    $lock = [IO.File]::Open((Join-Path $Runtime 'install.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    # Outra execução pode ter concluído antes da obtenção do bloqueio.
    if (Test-Path -LiteralPath $destination) {
        & "$PSScriptRoot/check.ps1"
        exit $LASTEXITCODE
    }
    $step = 'leitura da referência do pacote'
    $manifest = Get-Content -LiteralPath (Join-Path $ProjectRoot 'database/config/mysql-windows-package.json') -Raw | ConvertFrom-Json
    if ($manifest.sha256 -notmatch '^[a-fA-F0-9]{64}$' -or
        $manifest.file -notmatch '^mysql-[0-9.]+-winx64\.zip$' -or
        $manifest.directory -ne [IO.Path]::GetFileNameWithoutExtension($manifest.file) -or
        $manifest.url -ne ('https://cdn.mysql.com/Downloads/MySQL-8.4/' + $manifest.file)) {
        throw 'Referência de pacote inválida'
    }
    $staging = Join-Path $Runtime ('install-' + [guid]::NewGuid())
    [void][IO.Directory]::CreateDirectory($staging)
    $archive = Join-Path $staging 'package.zip'
    $localDefault = Join-Path $ProjectRoot ('database/packages/' + $manifest.file)
    if (!$ArchivePath -and (Test-Path -LiteralPath $localDefault)) { $ArchivePath = $localDefault }
    if ($ArchivePath) {
        $step = 'leitura do pacote offline'
        Write-Host "Pacote offline: $ArchivePath"
        # Caminho explícito inválido não provoca download inesperado.
        [IO.File]::Copy($ArchivePath, $archive, $false)
    } else {
        $step = 'download HTTP'
        Write-Host "Baixando MySQL $($manifest.version): $($manifest.url)"
        Invoke-WebRequest -Uri $manifest.url -UseBasicParsing -TimeoutSec 300 -OutFile $archive
    }
    $step = 'verificação de integridade'
    $actualHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    if ($actualHash -ne $manifest.sha256) { throw 'SHA-256 divergente; pacote recusado antes da extração' }
    Write-Host "Pacote verificado: SHA-256 $actualHash"
    $step = 'validação e extração do ZIP'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($archive)
    try {
        foreach ($entry in $zip.Entries) {
            $name = $entry.FullName.Replace('\', '/')
            if (!$name.StartsWith($manifest.directory + '/', [StringComparison]::Ordinal) -or
                ($name.Split('/') -contains '..') -or $name.Contains(':')) {
                throw "Entrada fora da estrutura esperada do pacote: $name"
            }
        }
    } finally { $zip.Dispose() }
    $extracted = Join-Path $staging 'extracted'
    Expand-Archive -LiteralPath $archive -DestinationPath $extracted
    $candidate = Join-Path $extracted $manifest.directory
    $step = 'verificação dos executáveis e dependências nativas'
    & "$PSScriptRoot/check.ps1" -InstallationPath $candidate
    if ($LASTEXITCODE -ne 0) { throw "Pacote não executável (código $LASTEXITCODE); instalação não publicada" }
    $step = 'publicação da instalação'
    if (Test-Path -LiteralPath $destination) { throw 'Instalação concorrente detectada; destino preservado' }
    [IO.File]::WriteAllText((Join-Path $candidate 'package.sha256'), $actualHash)
    [IO.Directory]::Move($candidate, $destination)
    Write-Host 'MySQL portátil preparado com as permissões do usuário.'
    exit 0
} catch {
    Write-Host "Falha na etapa: $step"
    Write-Host ($_ | Format-List * -Force | Out-String -Width 4096)
    if ($step -eq 'download HTTP') {
        Write-Host 'Para execução offline, forneça o ZIP oficial em database/packages ou TAG_FILE_MYSQL_ARCHIVE.'
    }
    exit 1
} finally {
    if ($staging -and [IO.Directory]::Exists($staging)) {
        try { Remove-Item -LiteralPath $staging -Recurse -Force }
        catch { Write-Warning "Temporários preservados para inspeção: $staging" }
    }
    if ($lock) { $lock.Dispose() }
}
