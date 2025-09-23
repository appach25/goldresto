param(
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$ArgsPassthrough
)
n$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir   = Join-Path $ScriptDir '..'
$Jar       = Join-Path $RootDir 'target/goldresto.jar'

if (-not (Test-Path $Jar)) {
  Write-Error "`"$Jar`" not found. Build the project first with: mvn -DskipTests package"
  exit 1
}

$JAVA_OPTS = "-Xms512m -Xmx1024m --add-opens java.base/java.lang=ALL-UNNAMED"
Write-Host "Starting GoldResto..."
& java $JAVA_OPTS -jar $Jar @ArgsPassthrough
