param(
  [string]$Project = ".",
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$MavenArgs
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
$jdkHome = Join-Path $repoRoot ".jdks\jdk-17"
$javaExe = Join-Path $jdkHome "bin\java.exe"

if (-not (Test-Path -LiteralPath $javaExe)) {
  throw "JDK 17 was not found at $jdkHome. Extract a JDK 17 distribution there first."
}

$mvn = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $mvn) {
  $mvn = Get-Command mvn -ErrorAction SilentlyContinue
}
if (-not $mvn) {
  throw "Maven was not found on PATH."
}

$projectPath = Resolve-Path -LiteralPath (Join-Path $repoRoot $Project)

$env:JAVA_HOME = $jdkHome
$env:Path = (Join-Path $jdkHome "bin") + [System.IO.Path]::PathSeparator + $env:Path

Push-Location $projectPath
try {
  & $mvn.Source @MavenArgs
  exit $LASTEXITCODE
}
finally {
  Pop-Location
}
