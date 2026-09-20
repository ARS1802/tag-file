$ErrorActionPreference = 'Stop'
$ProjectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$Runtime = Join-Path $ProjectRoot 'database/runtime'
$Data = Join-Path $Runtime 'data'
$Run = Join-Path $Runtime 'run'
$Config = Join-Path $Runtime 'mysql.ini'
$ClientConfig = Join-Path $Runtime 'client.ini'
$env:PATH = (Join-Path $Runtime 'mysql/bin') + ';' + $env:PATH
function Assert-Owner {
    $owner = Join-Path $Runtime 'instance.owner'
    if (!(Test-Path $owner) -or (Get-Content $owner -Raw).Trim() -ne $ProjectRoot) { throw 'Instância sem identificação deste projeto' }
}
function Assert-Server {
    Assert-Owner
    $result = & mysql "--defaults-extra-file=$ClientConfig" --batch --skip-column-names -e 'SELECT @@datadir,@@port'
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao identificar servidor' }
    $parts = $result -split "`t"
    if ([IO.Path]::GetFullPath($parts[0]).TrimEnd('\','/') -ne $Data.TrimEnd('\','/') -or $parts[1] -ne '3333') { throw 'Porta ocupada por outra instância' }
}
