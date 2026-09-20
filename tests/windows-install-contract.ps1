#requires -Version 5.1
# Usa scripts reais, ZIP pequeno e substitutos somente para rede/binarios nativos.
$ErrorActionPreference = 'Stop'
$Project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$Sandbox = Join-Path ([IO.Path]::GetTempPath()) ("tag-file teste [A] d'Arthur-" + [guid]::NewGuid())
$Utf8 = New-Object Text.UTF8Encoding $true
$Shell = (Get-Process -Id $PID).Path
$Checks = 0
function Assert-That([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw $Message }
    $script:Checks++
    Write-Host "PASS: $Message"
}
try {
    [void][IO.Directory]::CreateDirectory($Sandbox)
    $bin = Join-Path $Sandbox 'package/mysql-8.4.9-winx64/bin'
    [void][IO.Directory]::CreateDirectory($bin)
    foreach ($name in @('mysqld', 'mysql', 'mysqladmin')) {
        [IO.File]::WriteAllText((Join-Path $bin "$name.exe"), 'not executable', $Utf8)
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = Join-Path $Sandbox 'package.zip'
    [IO.Compression.ZipFile]::CreateFromDirectory((Join-Path $Sandbox 'package'), $zip)
    $hash = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash
    $harness = Join-Path $Sandbox 'harness.ps1'
    [IO.File]::WriteAllText($harness, @'
param($Root, $Zip, $Mode)
$ErrorActionPreference = 'Stop'
$env:TAG_FILE_MYSQL_ARCHIVE = $null
if ($Mode -eq 'offline-env') { $env:TAG_FILE_MYSQL_ARCHIVE = $Zip }
if ($Mode -eq 'missing-offline') { $env:TAG_FILE_MYSQL_ARCHIVE = Join-Path $Root 'absent.zip' }
function Invoke-WebRequest {
    param([string]$Uri, [string]$OutFile, [switch]$UseBasicParsing, [int]$TimeoutSec)
    [IO.File]::AppendAllText((Join-Path $Root 'requests.txt'), $Uri + [Environment]::NewLine)
    if ($Mode -like '*offline*') { throw 'Offline tentou acessar a rede' }
    if ($Uri -ne 'https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.9-winx64.zip') { throw 'Endpoint antigo: HTTP 403' }
    if (!$UseBasicParsing -or $TimeoutSec -le 0) { throw 'Parametros de download invalidos' }
    if ($Mode -eq 'http-error') {
        [IO.File]::WriteAllText($OutFile, 'download parcial')
        throw [Net.WebException]::new('HTTP 403 de teste no CDN')
    }
    [IO.File]::Copy($Zip, $OutFile, $true)
}
try {
    $installer = Join-Path $Root 'database/scripts/windows/install.ps1'
    if ($Mode -eq 'unauthorized') { & $installer } else { & $installer -Authorized }
    exit $LASTEXITCODE
} catch {
    [Console]::WriteLine(($_ | Format-List * -Force | Out-String -Width 4096))
    exit 1
}
'@, $Utf8)
    foreach ($mode in @('offline', 'offline-env', 'success', 'http-error', 'unauthorized', 'wrong-hash', 'bad-zip', 'native-failure', 'missing-offline', 'existing-failure', 'locked')) {
        $root = Join-Path $Sandbox $mode
        $scripts = Join-Path $root 'database/scripts/windows'
        $config = Join-Path $root 'database/config'
        $runtime = Join-Path $root 'database/runtime'
        foreach ($dir in @($scripts, $config, $runtime)) { [void][IO.Directory]::CreateDirectory($dir) }
        foreach ($name in @('install.ps1', 'common.ps1')) {
            [IO.File]::Copy((Join-Path $Project "database/scripts/windows/$name"), (Join-Path $scripts $name))
        }
        $manifest = @{ version = '8.4.9'; file = 'mysql-8.4.9-winx64.zip'; directory = 'mysql-8.4.9-winx64'; sha256 = $hash; url = 'https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.9-winx64.zip' }
        $inputZip = $zip
        if ($mode -eq 'wrong-hash') { $manifest.sha256 = '0' * 64 }
        if ($mode -eq 'bad-zip') {
            $inputZip = Join-Path $root 'invalid.zip'
            [IO.File]::WriteAllText($inputZip, 'not a ZIP')
            $manifest.sha256 = (Get-FileHash -LiteralPath $inputZip -Algorithm SHA256).Hash
        }
        [IO.File]::WriteAllText((Join-Path $config 'mysql-windows-package.json'), ($manifest | ConvertTo-Json), $Utf8)
        if ($mode -eq 'offline') {
            $packages = Join-Path $root 'database/packages'
            [void][IO.Directory]::CreateDirectory($packages)
            [IO.File]::Copy($zip, (Join-Path $packages $manifest.file))
        }
        # Verifica o diretorio temporario antes da publicacao, sem executar os placeholders.
        [IO.File]::WriteAllText((Join-Path $scripts 'check.ps1'), @'
param([string]$InstallationPath)
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
if (!$InstallationPath) { $InstallationPath = Join-Path $root 'database/runtime/mysql' }
if ((Split-Path $root -Leaf) -in @('native-failure', 'existing-failure')) { Write-Output 'DEPENDENCIA NATIVA AUSENTE'; exit 6 }
foreach ($name in @('mysqld', 'mysql', 'mysqladmin')) {
    if (![IO.File]::Exists((Join-Path $InstallationPath "bin/$name.exe"))) { exit 5 }
}
[IO.File]::WriteAllText((Join-Path $root 'checked.txt'), $InstallationPath)
exit 0
'@, $Utf8)
        if ($mode -eq 'existing-failure') { [void][IO.Directory]::CreateDirectory((Join-Path $runtime 'mysql')) }
        [IO.File]::WriteAllText((Join-Path $runtime 'preserve.txt'), 'existing data')
        $lock = $null
        try {
            if ($mode -eq 'locked') { $lock = [IO.File]::Open((Join-Path $runtime 'install.lock'), 'OpenOrCreate', 'ReadWrite', 'None') }
            $output = & $Shell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $harness -Root $root -Zip $inputZip -Mode $mode
            $code = $LASTEXITCODE
        } finally { if ($lock) { $lock.Dispose() } }
        if ($mode -in @('success', 'offline', 'offline-env')) {
            if ($code -ne 0) { throw ($output | Out-String) }
            Assert-That ($code -eq 0) "Instalacao: $mode"
            Assert-That ((Get-Content -LiteralPath (Join-Path $root 'checked.txt') -Raw) -like '*install-*') 'Binarios verificados antes da publicacao'
            Assert-That ([IO.File]::Exists((Join-Path $runtime 'mysql/bin/mysqladmin.exe'))) 'Instalacao publicada'
            Assert-That (($output | Out-String) -match 'SHA-256.*[A-Fa-f0-9]{64}') 'Identidade do script registrada'
            $output = & $Shell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $harness -Root $root -Zip $inputZip -Mode $mode
            Assert-That ($LASTEXITCODE -eq 0) 'Reutilizacao da instalacao'
            if ($mode -eq 'success') {
                Assert-That ([IO.File]::ReadAllLines((Join-Path $root 'requests.txt')).Length -eq 1) 'Reutilizacao sem segundo download'
            } else { Assert-That (![IO.File]::Exists((Join-Path $root 'requests.txt'))) 'Offline sem rede' }
        } else {
            Assert-That ($code -ne 0) "Falha propagada: $mode"
            Assert-That (![IO.File]::Exists((Join-Path $runtime 'mysql/bin/mysqld.exe'))) 'Falha nao publica binarios'
            if ($mode -eq 'http-error') { Assert-That (($output | Out-String) -match 'HTTP 403') 'Causa HTTP preservada' }
            if ($mode -eq 'wrong-hash') { Assert-That (($output | Out-String) -match 'SHA-256 divergente') 'Integridade validada antes da extracao' }
            if ($mode -in @('unauthorized', 'missing-offline', 'existing-failure', 'locked')) {
                Assert-That (![IO.File]::Exists((Join-Path $root 'requests.txt'))) 'Recusa sem download'
            }
        }
        Assert-That ([IO.File]::ReadAllText((Join-Path $runtime 'preserve.txt')) -eq 'existing data') 'Dados existentes preservados'
        Assert-That (@(Get-ChildItem -LiteralPath $runtime -Filter 'install-*' -Directory).Count -eq 0) 'Temporarios removidos'
    }
    # Codigos reais da verificacao: ausencia e instalacao incompleta sao distintos.
    $realCheck = Join-Path $Project 'database/scripts/windows/check.ps1'
    $missing = Join-Path $Sandbox 'missing-installation'
    $output = & $Shell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $realCheck -InstallationPath $missing
    Assert-That ($LASTEXITCODE -eq 4) 'Check real identifica ausencia com codigo 4'
    [void][IO.Directory]::CreateDirectory($missing)
    $output = & $Shell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $realCheck -InstallationPath $missing
    Assert-That ($LASTEXITCODE -eq 5) 'Check real distingue instalacao incompleta com codigo 5'
    Write-Host "WINDOWS-INSTALL-CONTRACT: $Checks verificacoes passaram; rede/binarios simulados."
} finally {
    if ([IO.Directory]::Exists($Sandbox)) { Remove-Item -LiteralPath $Sandbox -Recurse -Force }
}
