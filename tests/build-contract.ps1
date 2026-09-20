param(
    [Parameter(Mandatory = $true)][string]$Jdk,
    [string]$IncompatibleJdk,
    [switch]$RegressionOnly
)
$ErrorActionPreference = 'Stop'
$BuildScript = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../scripts/build.ps1'))
$Shell = (Get-Process -Id $PID).Path
$Sandbox = Join-Path ([IO.Path]::GetTempPath()) ('tag-file-build-test-' + [guid]::NewGuid())
$Utf8 = New-Object Text.UTF8Encoding $false
$Checks = 0

function Assert-That([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw $Message }
    $script:Checks++
    Write-Host "PASS: $Message"
}

function New-Fixture([string]$Name) {
    $root = Join-Path $Sandbox $Name
    foreach ($directory in @('scripts', 'src', 'out/classes', 'out/javadoc')) {
        [void][IO.Directory]::CreateDirectory((Join-Path $root $directory))
    }
    [IO.File]::Copy($BuildScript, (Join-Path $root 'scripts/build.ps1'))
    [IO.File]::WriteAllText((Join-Path $root 'src/Hello.java'), '/** Example. */ public class Hello {}', $Utf8)
    foreach ($directory in @('classes', 'javadoc')) {
        [IO.File]::WriteAllText((Join-Path $root "out/$directory/previous.txt"), 'previous', $Utf8)
    }
    return $root
}

function Invoke-Build([string]$Root, [string]$SelectedJdk) {
    $previousJdk = $env:TAG_FILE_JDK
    $previousPreference = $ErrorActionPreference
    try {
        $env:TAG_FILE_JDK = $SelectedJdk
        # A captura do teste deve aceitar stderr; o resultado vem do codigo do processo.
        $ErrorActionPreference = 'Continue'
        $output = & $Shell -NoProfile -NonInteractive -File (Join-Path $Root 'scripts/build.ps1') 2>&1
        $code = $LASTEXITCODE
        return [pscustomobject]@{ Code = $code; Output = ($output | Out-String) }
    } finally {
        $ErrorActionPreference = $previousPreference
        $env:TAG_FILE_JDK = $previousJdk
    }
}

function Assert-Preserved([string]$Root) {
    foreach ($directory in @('classes', 'javadoc')) {
        $path = Join-Path $Root "out/$directory/previous.txt"
        Assert-That ([IO.File]::Exists($path) -and [IO.File]::ReadAllText($path) -eq 'previous') "Preserva $directory anterior"
    }
}

try {
    $root = New-Fixture 'invalid-jdk'
    $result = Invoke-Build $root (Join-Path $root 'absent-jdk')
    Assert-That ($result.Code -ne 0) 'JDK inexistente falha'
    Assert-Preserved $root
    if ($RegressionOnly) { return }

    $root = New-Fixture 'missing-javadoc'
    $partialJdk = Join-Path $root 'jdk'
    [void][IO.Directory]::CreateDirectory((Join-Path $partialJdk 'bin'))
    $toolName = if ([IO.Path]::DirectorySeparatorChar -eq '\') { 'javac.exe' } else { 'javac' }
    [IO.File]::WriteAllText((Join-Path $partialJdk "bin/$toolName"), 'not executed', $Utf8)
    $result = Invoke-Build $root $partialJdk
    Assert-That ($result.Code -ne 0 -and $result.Output -match 'javadoc') 'Javadoc ausente detectado antes de executar javac'
    Assert-Preserved $root

    $root = New-Fixture 'empty'
    [IO.File]::Delete((Join-Path $root 'src/Hello.java'))
    $result = Invoke-Build $root $Jdk
    Assert-That ($result.Code -ne 0) 'Nenhum fonte falha'
    Assert-Preserved $root

    $root = New-Fixture 'compile-error'
    [IO.File]::WriteAllText((Join-Path $root 'src/Hello.java'), 'public class Hello { invalid }', $Utf8)
    $result = Invoke-Build $root $Jdk
    Assert-That ($result.Code -ne 0) 'Erro Java falha'
    Assert-That ($result.Output -match 'Hello.java') 'Diagnostico identifica o fonte'
    Assert-Preserved $root

    $root = New-Fixture 'javadoc-error'
    [IO.File]::WriteAllText((Join-Path $root 'src/Hello.java'), '/** <invalidtag> */ public class Hello {}', $Utf8)
    $result = Invoke-Build $root $Jdk
    Assert-That ($result.Code -ne 0) 'Erro Javadoc falha depois de compilar'
    Assert-That ($result.Output -match 'javadoc') 'Diagnostico identifica o Javadoc'
    Assert-Preserved $root

    $root = New-Fixture 'single-source'
    $result = Invoke-Build $root $Jdk
    if ($result.Code -ne 0) { throw $result.Output }
    Assert-That ($result.Code -eq 0) 'Um fonte compila e gera Javadoc'
    Assert-That ($result.Output -match 'warning') 'Aviso em stderr nao vira falha'
    $class = [IO.File]::ReadAllBytes((Join-Path $root 'out/classes/Hello.class'))
    Assert-That (($class[6] * 256 + $class[7]) -eq 66) 'Bytecode usa alvo Java 22'
    Assert-That ([IO.File]::Exists((Join-Path $root 'out/javadoc/index.html'))) 'Javadoc publicado'
    Assert-That (![IO.File]::Exists((Join-Path $root 'out/classes/previous.txt'))) 'Sucesso remove artefatos obsoletos'

    $root = New-Fixture "Cópia [A] d'Arthur `$"
    [IO.File]::WriteAllText((Join-Path $root 'src/Other.java'), '/** Other. */ public class Other {}', $Utf8)
    [void][IO.Directory]::CreateDirectory((Join-Path $root 'src/not-a-file.java'))
    $result = Invoke-Build $root $Jdk
    if ($result.Code -ne 0) { throw $result.Output }
    Assert-That ($result.Code -eq 0) 'Multiplos fontes e caminho especial'
    Assert-That ([IO.File]::Exists((Join-Path $root 'out/classes/Other.class'))) 'Segundo fonte compilado'

    $root = New-Fixture 'locked'
    $lock = [IO.File]::Open((Join-Path $root 'out/.build.lock'), [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
    try { $result = Invoke-Build $root $Jdk } finally { $lock.Dispose() }
    Assert-That ($result.Code -ne 0) 'Build concorrente recusado'
    Assert-Preserved $root

    # Forca falha na segunda substituicao: as classes ja foram publicadas,
    # mas a pasta nova do Javadoc foi retirada da area temporaria.
    $tokens = $null
    $parseErrors = $null
    $ast = [Management.Automation.Language.Parser]::ParseFile($BuildScript, [ref]$tokens, [ref]$parseErrors)
    Assert-That ($parseErrors.Count -eq 0) 'Script passa pelo parser PowerShell'
    $definitions = $ast.FindAll({ param($node)
        $node -is [Management.Automation.Language.FunctionDefinitionAst] -and
        $node.Name -in @('Assert-PlainDirectory', 'Assert-PlainTree', 'Publish-Build')
    }, $false)
    . ([scriptblock]::Create(($definitions.Extent.Text -join "`n")))
    $root = New-Fixture 'rollback'
    $stage = Join-Path $root 'out/staging'
    [void][IO.Directory]::CreateDirectory((Join-Path $stage 'classes'))
    [IO.File]::WriteAllText((Join-Path $stage 'classes/new.txt'), 'new', $Utf8)
    $failed = $false
    try { Publish-Build (Join-Path $root 'out') $stage } catch { $failed = $true }
    Assert-That $failed 'Falha durante a publicacao e propagada'
    Assert-Preserved $root
    Assert-That (![IO.File]::Exists((Join-Path $root 'out/classes/new.txt'))) 'Rollback nao mistura classes novas e antigas'

    $root = New-Fixture 'abandoned-build'
    [void][IO.Directory]::CreateDirectory((Join-Path $root 'out/.build-interrupted/previous-classes'))
    $result = Invoke-Build $root $Jdk
    Assert-That ($result.Code -ne 0) 'Build interrompido exige inspecao dos backups'
    Assert-Preserved $root

    $root = New-Fixture 'inherited-options'
    $originalOptions = $env:JDK_JAVAC_OPTIONS
    try {
        $env:JDK_JAVAC_OPTIONS = '--definitely-invalid'
        $result = Invoke-Build $root $Jdk
        if ($result.Code -ne 0) { throw $result.Output }
        Assert-That ($result.Code -eq 0) 'Opcoes herdadas isoladas no filho'
        Assert-That ($env:JDK_JAVAC_OPTIONS -eq '--definitely-invalid') 'Ambiente do chamador preservado'
    } finally { $env:JDK_JAVAC_OPTIONS = $originalOptions }

    $root = New-Fixture 'linked-output'
    $external = Join-Path $Sandbox 'external'
    [void][IO.Directory]::CreateDirectory($external)
    [IO.File]::WriteAllText((Join-Path $external 'keep.txt'), 'keep', $Utf8)
    $link = Join-Path $root 'out/classes/link'
    $linkType = if ([IO.Path]::DirectorySeparatorChar -eq '\') { 'Junction' } else { 'SymbolicLink' }
    [void](New-Item -ItemType $linkType -Path $link -Target $external)
    try {
        $result = Invoke-Build $root $Jdk
        Assert-That ($result.Code -ne 0) 'Link/juncao em saida recusado'
        Assert-Preserved $root
        Assert-That ([IO.File]::ReadAllText((Join-Path $external 'keep.txt')) -eq 'keep') 'Destino externo preservado'
    } finally { [IO.Directory]::Delete($link) }

    if ([IO.Path]::DirectorySeparatorChar -eq '\') {
        $root = New-Fixture 'locked-file'
        $handle = [IO.File]::Open((Join-Path $root 'out/classes/previous.txt'), [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::None)
        try { $result = Invoke-Build $root $Jdk } finally { $handle.Dispose() }
        Assert-That ($result.Code -ne 0) 'Arquivo aberto sem compartilhamento impede publicacao'
        Assert-Preserved $root
    } else { Write-Host 'NAO EXECUTADO: bloqueio de arquivo exclusivo do Windows.' }

    if ($IncompatibleJdk) {
        $root = New-Fixture 'incompatible'
        $result = Invoke-Build $root $IncompatibleJdk
        Assert-That ($result.Code -ne 0) 'JDK anterior a 22 recusado'
        Assert-Preserved $root
    }
    Write-Host "BUILD-CONTRACT: $Checks verificacoes passaram."
} finally {
    if ([IO.Directory]::Exists($Sandbox)) { Remove-Item -LiteralPath $Sandbox -Recurse -Force }
}
