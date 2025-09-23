$ErrorActionPreference = 'Stop'

# Read version from pom.xml
[xml]$pom = Get-Content (Join-Path (Join-Path $PSScriptRoot '..') 'pom.xml')
$version = $pom.project.version
if ([string]::IsNullOrWhiteSpace($version)) { $version = 'unknown' }

# Paths
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$dist = Join-Path $root 'dist'
$targetJar = Join-Path $root 'target/goldresto.jar'

if (-not (Test-Path $targetJar)) {
  Write-Error "goldresto.jar not found at $targetJar. Build first: mvn -DskipTests package"
  exit 1
}

# Prepare timestamped bundle
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'
$bundleName = "goldresto-$version-$ts"
$bundleDir = Join-Path $dist $bundleName

New-Item -ItemType Directory -Path $bundleDir -Force | Out-Null

# Copy artifacts
Copy-Item -Path $targetJar -Destination (Join-Path $bundleDir 'goldresto.jar') -Force
Copy-Item -Path (Join-Path $root 'pom.xml') -Destination (Join-Path $bundleDir 'pom.xml') -Force
Copy-Item -Path (Join-Path $root 'deploy') -Destination (Join-Path $bundleDir 'deploy') -Recurse -Force

# Create ZIP
$zipPath = Join-Path $dist ($bundleName + '.zip')
New-Item -ItemType Directory -Path $dist -Force | Out-Null
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
Compress-Archive -Path (Join-Path $bundleDir '*') -DestinationPath $zipPath -Force

Write-Host "Created bundle directory: $bundleDir"
Write-Host "Created ZIP: $zipPath"
