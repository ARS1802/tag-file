#requires -Version 5.1
# Simula o NativeCommandError do PowerShell 5.1; servidor e espera sao substituidos.
$ErrorActionPreference = 'Stop'
$root = Join-Path ([IO.Path]::GetTempPath()) ('tag-file-start-test-' + [guid]::NewGuid())
$project = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$shell = (Get-Process -Id $PID).Path
$utf8 = New-Object Text.UTF8Encoding $true
$checks = 0
try {
    $scripts = Join-Path $root 'database/scripts/windows'
    [void][IO.Directory]::CreateDirectory($scripts)
    foreach ($name in @('start.ps1', 'common.ps1')) {
        [IO.File]::Copy((Join-Path $project "database/scripts/windows/$name"), (Join-Path $scripts $name))
    }
    $harness = Join-Path $root 'harness.ps1'
    [IO.File]::WriteAllText($harness, @'
param($Root, $Mode)
$ErrorActionPreference = 'Stop'
. (Join-Path $Root 'database/scripts/windows/common.ps1')
[void][IO.Directory]::CreateDirectory($Run)
[IO.File]::WriteAllText((Join-Path $Runtime 'instance.owner'), $ProjectRoot)
[IO.File]::WriteAllText((Join-Path $Run 'bootstrap.sql'), 'fixture')
$script:attempt = 0
function mysqladmin {
    if ($Mode -in @('existing', 'foreign')) { $global:LASTEXITCODE = 0; return }
    # Emula a conversao de stderr para erro PowerShell anterior a 7.2.
    Write-Error 'mysqladmin: connect to server failed' -ErrorId NativeCommandError
    $global:LASTEXITCODE = 1
}
function mysql {
    if ($args -contains 'SELECT @@datadir,@@port') {
        if ($ErrorActionPreference -ne 'Stop') { throw 'Preferencia Stop nao restaurada' }
        $global:LASTEXITCODE = 0
        if ($Mode -eq 'foreign') { Write-Output "other-directory`t3333" }
        else { Write-Output "$Data`t3333" }
        return
    }
    $script:attempt++
    if ($Mode -ne 'timeout' -and $script:attempt -ge 3) { $global:LASTEXITCODE = 0; return }
    Write-Error 'mysql: server not ready' -ErrorId NativeCommandError
    $global:LASTEXITCODE = 1
}
function Start-Process {
    param($FilePath, $ArgumentList, [switch]$PassThru, $WindowStyle)
    [IO.File]::WriteAllText((Join-Path $Root 'started'), 'yes')
    return [pscustomobject]@{ HasExited = ($Mode -eq 'crash') }
}
function Get-Command { param($Name) return [pscustomobject]@{ Source = $Name } }
function Start-Sleep { param($Seconds) }
if ($Mode -eq 'busy') { [IO.File]::WriteAllText((Join-Path $Run 'mysql.pid'), [string]$PID) }
try {
    & (Join-Path $Root 'database/scripts/windows/start.ps1')
    exit $LASTEXITCODE
} catch { Write-Output ($_ | Out-String); exit 1 }
'@, $utf8)
    foreach ($mode in @('cold', 'existing', 'foreign', 'timeout', 'crash', 'busy')) {
        $marker = Join-Path $root 'started'
        Remove-Item -LiteralPath $marker -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath (Join-Path $root 'database/runtime/run/mysql.pid') -ErrorAction SilentlyContinue
        $output = & $shell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File $harness -Root $root -Mode $mode
        $code = $LASTEXITCODE
        if ($mode -in @('cold', 'existing')) {
            if ($code -ne 0) { throw "$mode falhou: $($output | Out-String)" }
        } elseif ($code -eq 0) { throw "Falha ignorada: $mode" }
        $checks++
        $shouldStart = $mode -in @('cold', 'timeout', 'crash')
        if ([IO.File]::Exists($marker) -ne $shouldStart) { throw "Inicio indevido/ausente: $mode" }
        $checks++
        $bootstrapExists = [IO.File]::Exists((Join-Path $root 'database/runtime/run/bootstrap.sql'))
        if ($bootstrapExists -ne ($mode -ne 'cold')) { throw "Bootstrap removido indevidamente: $mode" }
        $checks++
        Write-Host "PASS: $mode"
    }
    # Verifica tambem o escopo real de LASTEXITCODE, alem da simulacao de stderr.
    $tokens = $null; $errors = $null
    $ast = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $project 'database/scripts/windows/start.ps1'), [ref]$tokens, [ref]$errors)
    $definition = $ast.Find({ param($node) $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Invoke-ConnectionProbe' }, $true)
    . ([scriptblock]::Create($definition.Extent.Text))
    foreach ($expected in @(0, 7)) {
        $native = Invoke-ConnectionProbe $shell @('-NoProfile', '-NonInteractive', '-Command', "[Console]::Error.WriteLine('stderr nativo'); exit $expected")
        if ($native -ne $expected) { throw "Codigo nativo incorreto: $native; esperado $expected" }
        $checks++
    }
    if ($ErrorActionPreference -ne 'Stop') { throw 'Preferencia do chamador alterada' }
    $checks++
    $rejected = $false
    try { Invoke-ConnectionProbe 'tag-file-command-that-does-not-exist' @() | Out-Null }
    catch { $rejected = $true }
    if (!$rejected) { throw 'Comando ausente tratado como sucesso' }
    $checks++
    Write-Host "WINDOWS-START-CONTRACT: $checks verificacoes; NativeCommandError 5.1 simulado."
} finally { if ([IO.Directory]::Exists($root)) { Remove-Item -LiteralPath $root -Recurse -Force } }
