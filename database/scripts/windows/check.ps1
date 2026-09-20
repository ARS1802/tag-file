param([string]$InstallationPath)
. "$PSScriptRoot/common.ps1"
if (!$InstallationPath) { $InstallationPath = Join-Path $Runtime 'mysql' }
# Somente o código 4 autoriza oferecer uma nova instalação.
if (!(Test-Path -LiteralPath $InstallationPath)) {
    Write-Output 'Componentes portáteis ausentes.'
    exit 4
}
foreach ($binary in @('mysqld', 'mysql', 'mysqladmin')) {
    $executable = Join-Path $InstallationPath "bin/$binary.exe"
    if (![IO.File]::Exists($executable)) {
        Write-Output "Instalação incompleta: $executable. Arquivos existentes preservados."
        exit 5
    }
}
foreach ($binary in @('mysqld', 'mysql', 'mysqladmin')) {
    $executable = Join-Path $InstallationPath "bin/$binary.exe"
    try {
        & $executable --version
        if ($LASTEXITCODE -ne 0) { throw "Código de saída: $LASTEXITCODE" }
    } catch {
        Write-Output "Falha ao executar $executable : $($_.Exception.Message)"
        Write-Output 'Verifique arquitetura x64, permissões de execução e Microsoft Visual C++ Redistributable exigido pelo MySQL 8.4.'
        exit 6
    }
}
exit 0
