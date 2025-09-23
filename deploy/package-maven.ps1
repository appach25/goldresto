$ErrorActionPreference = 'Stop'

# Read version from root pom.xml
[xml]$pom = Get-Content (Join-Path (Join-Path $PSScriptRoot '..') 'pom.xml')
$version = $pom.project.version
if ([string]::IsNullOrWhiteSpace($version)) { $version = 'unknown' }

# Paths
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$dist = Join-Path $root 'dist'
$ts   = Get-Date -Format 'yyyyMMdd-HHmmss'
$bundleName = "goldresto-maven-$version-$ts"
$bundleDir  = Join-Path $dist $bundleName
$zipPath    = Join-Path $dist ($bundleName + '.zip')

# Prepare bundle directory
New-Item -ItemType Directory -Path $bundleDir -Force | Out-Null
New-Item -ItemType Directory -Path $dist -Force | Out-Null

# Items to include for a Maven-buildable project
$includePaths = @(
  'pom.xml',
  'mvnw',
  'mvnw.cmd',
  '.mvn',
  'src',
  'deploy',
  'INSTALL.md',
  'install.bat'
)

# Exclusions (not needed for source build archive)
$excludes = @(
  '.git',
  'target',
  'dist',
  'logs',
  'uploads',
  '.vscode'
)

Push-Location $root
try {
  foreach ($path in $includePaths) {
    if (Test-Path $path) {
      # Skip if excluded by top-level name
      if ($excludes -contains $path) { continue }

      $destination = Join-Path $bundleDir $path
      Copy-Item -Path $path -Destination $destination -Recurse -Force -Exclude $excludes -ErrorAction SilentlyContinue
    }
  }

  # Create ZIP
  if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
  Compress-Archive -Path (Join-Path $bundleDir '*') -DestinationPath $zipPath -Force

  Write-Host "Created Maven source bundle directory: $bundleDir"
  Write-Host "Created ZIP: $zipPath"
}
finally {
  Pop-Location
}
