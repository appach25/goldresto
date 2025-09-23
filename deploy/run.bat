@echo off
setlocal ENABLEDELAYEDEXPANSION

set SCRIPT_DIR=%~dp0
set ROOT_DIR=%SCRIPT_DIR%..
set JAR=%ROOT_DIR%\target\goldresto.jar

if not exist "%JAR%" (
  echo ERROR: "%JAR%" not found.
  echo Build the project first with: mvn -DskipTests package
  exit /b 1
)

set JAVA_OPTS=-Xms512m -Xmx1024m --add-opens java.base/java.lang=ALL-UNNAMED

echo Starting GoldResto...
java %JAVA_OPTS% -jar "%JAR%" %*

endlocal
