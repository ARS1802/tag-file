$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
$Compiler = if ($env:TAG_FILE_JDK) { Join-Path $env:TAG_FILE_JDK 'bin/javac.exe' } else { 'javac' }
$DocTool = if ($env:TAG_FILE_JDK) { Join-Path $env:TAG_FILE_JDK 'bin/javadoc.exe' } else { 'javadoc' }
New-Item -ItemType Directory -Force out/classes, out/javadoc, lib | Out-Null
$sources = Get-ChildItem src -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }
[IO.File]::WriteAllLines((Join-Path (Get-Location) 'out/sources.txt'), $sources, (New-Object Text.UTF8Encoding $false))
& $Compiler --release 24 -encoding UTF-8 -cp 'lib/*' -d out/classes '@out/sources.txt'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& $DocTool -private '-Xdoclint:all' -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -cp 'lib/*' -d out/javadoc '@out/sources.txt'
exit $LASTEXITCODE
