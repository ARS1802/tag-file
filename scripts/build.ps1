#requires -Version 5.1
# Compila para Java 22 e publica classes/Javadoc somente quando ambos terminam.
# Execucao direta nao exige elevacao se o usuario puder escrever no projeto.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0
$ProjectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$Release = 22
$Utf8 = New-Object Text.UTF8Encoding $false
$BuildLog = $null
$Work = $null
$Lock = $null
$KeepWork = $false
$ResultCode = 1
$OriginalLocation = Get-Location

function Write-BuildMessage([string]$Message) {
    [Console]::WriteLine($Message)
    if ($script:BuildLog) { [IO.File]::AppendAllText($script:BuildLog, $Message + [Environment]::NewLine, $script:Utf8) }
}

function Assert-PlainDirectory([string]$Path) {
    if (Test-Path -LiteralPath $Path) {
        $item = Get-Item -LiteralPath $Path -Force
        if (!$item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw "Diretorio invalido ou redirecionado por link/juncao: $Path"
        }
    }
}

function Assert-PlainTree([string]$Path) {
    Assert-PlainDirectory $Path
    if (Test-Path -LiteralPath $Path) {
        # Nao remove arvores contendo links, mesmo quando criadas por outro processo.
        foreach ($item in Get-ChildItem -LiteralPath $Path -Force) {
            if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Link/juncao inesperado: $($item.FullName)" }
            if ($item.PSIsContainer) { Assert-PlainTree $item.FullName }
        }
    }
}

function ConvertTo-ProcessArgument([string]$Value) {
    # Regras de aspas do Windows/.NET; nao envolve cmd.exe nem interpretacao de shell.
    $escaped = [regex]::Replace($Value, '(\\*)"', '$1$1\"')
    return '"' + [regex]::Replace($escaped, '(\\+)$', '$1$1') + '"'
}

function Invoke-JdkTool([string]$Tool, [string[]]$ToolArguments, [string]$Stage) {
    Write-BuildMessage "Etapa: $Stage | ferramenta: $Tool"
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $Tool
    $info.WorkingDirectory = $script:ProjectRoot
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = $script:Utf8
    $info.StandardErrorEncoding = $script:Utf8
    # Evita opcoes externas alterarem o alvo/encoding; nao altera o ambiente do usuario.
    foreach ($name in @('JDK_JAVAC_OPTIONS', 'JDK_JAVA_OPTIONS', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS')) {
        $info.EnvironmentVariables.Remove($name)
    }
    $allArguments = @('-J-Dfile.encoding=UTF-8', '-J-Dstdout.encoding=UTF-8', '-J-Dstderr.encoding=UTF-8') + $ToolArguments
    $info.Arguments = (@($allArguments | ForEach-Object { ConvertTo-ProcessArgument $_ }) -join ' ')
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $info
    try {
        if (!$process.Start()) { throw "Nao foi possivel iniciar $Tool" }
        # Drena os dois canais ao mesmo tempo para evitar bloqueio quando os buffers enchem.
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (!$process.WaitForExit(600000)) {
            $process.Kill()
            $process.WaitForExit()
            Write-BuildMessage ($stdout.GetAwaiter().GetResult())
            Write-BuildMessage ($stderr.GetAwaiter().GetResult())
            throw "Tempo excedido na etapa $Stage (10 minutos)."
        }
        $output = $stdout.GetAwaiter().GetResult()
        $errors = $stderr.GetAwaiter().GetResult()
        if ($output) { Write-BuildMessage "[stdout]`n$output" }
        if ($errors) { Write-BuildMessage "[stderr]`n$errors" }
        $code = $process.ExitCode
        Write-BuildMessage "Etapa: $Stage | codigo: $code"
        if ($code -ne 0) {
            $failure = New-Object Exception "Falha na etapa $Stage; codigo $code."
            $failure.Data['ExitCode'] = $code
            throw $failure
        }
        return ($output + $errors)
    } finally {
        try { if ($process.Id -and !$process.HasExited) { $process.Kill() } } catch { }
        $process.Dispose()
    }
}

function Publish-Build([string]$OutputRoot, [string]$Staging) {
    $changes = New-Object 'Collections.Generic.List[object]'
    try {
        foreach ($name in @('classes', 'javadoc')) {
            $change = [pscustomobject]@{
                Destination = (Join-Path $OutputRoot $name)
                Fresh = (Join-Path $Staging $name)
                Backup = (Join-Path $Staging "previous-$name")
                Saved = $false
                Published = $false
            }
            $changes.Add($change)
            Assert-PlainTree $change.Destination
            if ([IO.Directory]::Exists($change.Destination)) {
                [IO.Directory]::Move($change.Destination, $change.Backup)
                $change.Saved = $true
            }
            [IO.Directory]::Move($change.Fresh, $change.Destination)
            $change.Published = $true
        }
    } catch {
        $originalFailure = $_
        $recoveryErrors = New-Object 'Collections.Generic.List[string]'
        for ($index = $changes.Count - 1; $index -ge 0; $index--) {
            $change = $changes[$index]
            try {
                if ($change.Published) { [IO.Directory]::Move($change.Destination, $change.Fresh) }
                if ($change.Saved) { [IO.Directory]::Move($change.Backup, $change.Destination) }
            } catch { $recoveryErrors.Add($_.Exception.Message) }
        }
        if ($recoveryErrors.Count -gt 0) {
            $failure = New-Object Exception ("Recuperacao incompleta; preserve $Staging. " + ($recoveryErrors -join ' | '))
            $failure.Data['RecoveryDirectory'] = $Staging
            throw $failure
        }
        throw $originalFailure
    }
}

try {
    Set-Location -LiteralPath $ProjectRoot
    $outputRoot = Join-Path $ProjectRoot 'out'
    Assert-PlainDirectory $outputRoot
    [void][IO.Directory]::CreateDirectory($outputRoot)
    $lockPath = Join-Path $outputRoot '.build.lock'
    if ((Test-Path -LiteralPath $lockPath) -and ((Get-Item -LiteralPath $lockPath -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw "Arquivo de bloqueio redirecionado: $lockPath"
    }
    try { $Lock = [IO.File]::Open($lockPath, [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None) }
    catch { throw "Nao foi possivel obter o bloqueio do build (concorrencia ou permissao): $lockPath. $($_.Exception.Message)" }
    $logDirectory = Join-Path $outputRoot 'build-logs'
    Assert-PlainDirectory $logDirectory
    [void][IO.Directory]::CreateDirectory($logDirectory)
    $BuildLog = Join-Path $logDirectory (([guid]::NewGuid().ToString()) + '.log')
    Write-BuildMessage "Build Java $Release | PowerShell $($PSVersionTable.PSVersion) | raiz: $ProjectRoot"
    Write-BuildMessage "Log UTF-8: $BuildLog"
    # Um encerramento abrupto pode deixar backups; nunca os apaga automaticamente.
    $pending = @(Get-ChildItem -LiteralPath $outputRoot -Force | Where-Object { $_.Name -like '.build-*' })
    if ($pending.Count -gt 0) { throw "Area de build anterior requer inspecao: $($pending.FullName -join ', ')" }
    foreach ($name in @('classes', 'javadoc')) { Assert-PlainTree (Join-Path $outputRoot $name) }

    $suffix = if ([IO.Path]::DirectorySeparatorChar -eq '\') { '.exe' } else { '' }
    if ($env:TAG_FILE_JDK) {
        $jdkRoot = [IO.Path]::GetFullPath($env:TAG_FILE_JDK)
        $compiler = Join-Path $jdkRoot "bin/javac$suffix"
    } else {
        $compiler = (Get-Command "javac$suffix" -CommandType Application -ErrorAction Stop | Select-Object -First 1).Source
    }
    # A documentacao sempre vem da mesma pasta de ferramentas do compilador.
    $docTool = Join-Path ([IO.Path]::GetDirectoryName($compiler)) "javadoc$suffix"
    foreach ($tool in @($compiler, $docTool)) {
        if (![IO.File]::Exists($tool)) { throw "Ferramenta JDK ausente: $tool. Configure TAG_FILE_JDK para um JDK completo." }
    }
    foreach ($name in @('JDK_JAVAC_OPTIONS', 'JDK_JAVA_OPTIONS', 'JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS')) {
        if ([Environment]::GetEnvironmentVariable($name)) { Write-BuildMessage "Opcao herdada $name sera isolada nos processos deste build." }
    }
    $version = Invoke-JdkTool $compiler @('--version') 'verificar javac'
    if ($version -notmatch 'javac\s+(\d+)' -or [int]$Matches[1] -lt $Release) { throw "O projeto requer JDK $Release ou posterior. Selecionado: $version" }
    $compilerMajor = [int]$Matches[1]
    $docVersion = Invoke-JdkTool $docTool @('--version') 'verificar javadoc'
    if ($docVersion -notmatch 'javadoc\s+(\d+)' -or [int]$Matches[1] -ne $compilerMajor) { throw "javac e javadoc devem pertencer ao mesmo JDK: $version / $docVersion" }
    [void](Invoke-JdkTool $compiler @('--release', "$Release", '-version') 'verificar alvo')
    $sourceRoot = Join-Path $ProjectRoot 'src'
    if (![IO.Directory]::Exists($sourceRoot)) { throw "Diretorio de fontes ausente: $sourceRoot" }
    [string[]]$sources = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter '*.java' | Sort-Object FullName | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    if ($sources.Count -eq 0) { throw 'Nenhum fonte .java encontrado; build anterior preservado.' }
    $library = Join-Path $ProjectRoot 'lib'
    Assert-PlainDirectory $library
    [void][IO.Directory]::CreateDirectory($library)
    $Work = Join-Path $outputRoot ('.build-' + [guid]::NewGuid())
    foreach ($name in @('classes', 'javadoc')) { [void][IO.Directory]::CreateDirectory((Join-Path $Work $name)) }
    $sourceList = Join-Path $Work 'sources.txt'
    [IO.File]::WriteAllLines($sourceList, $sources, $Utf8)
    $classpath = Join-Path $library '*'
    [void](Invoke-JdkTool $compiler @('--release', "$Release", '-encoding', 'UTF-8', '-cp', $classpath, '-d', (Join-Path $Work 'classes'), "@$sourceList") 'compilacao')
    [void](Invoke-JdkTool $docTool @('--release', "$Release", '-private', '-Xdoclint:all', '-encoding', 'UTF-8', '-docencoding', 'UTF-8', '-charset', 'UTF-8', '-cp', $classpath, '-d', (Join-Path $Work 'javadoc'), "@$sourceList") 'javadoc')
    Publish-Build $outputRoot $Work
    $ResultCode = 0
    Write-BuildMessage 'Compilacao e Javadoc publicados com sucesso. Banco e interface nao foram iniciados.'
} catch {
    $failure = $_
    $ResultCode = 1
    if ($failure.Exception.Data.Contains('ExitCode')) { $ResultCode = [int]$failure.Exception.Data['ExitCode'] }
    if ($failure.Exception.Data.Contains('RecoveryDirectory')) { $KeepWork = $true }
    Write-BuildMessage ("Falha no build: " + ($failure | Format-List * -Force | Out-String -Width 4096))
} finally {
    try {
        if ($Work -and !$KeepWork -and [IO.Directory]::Exists($Work)) {
            Assert-PlainTree $Work
            Remove-Item -LiteralPath $Work -Recurse -Force
        }
    } catch {
        [Console]::WriteLine("Area temporaria preservada para inspecao: $Work. $($_.Exception.Message)")
        $ResultCode = 1
    } finally {
        if ($Lock) { $Lock.Dispose() }
        Set-Location -LiteralPath $OriginalLocation.Path
    }
}
exit $ResultCode
