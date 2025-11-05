@echo off
setlocal enabledelayedexpansion

REM Check for Java installation
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Java is not installed or not in PATH
    pause
    exit /b 1
)

REM Get Java installation path
for /f "tokens=*" %%i in ('where java') do set JAVA_PATH=%%i
set JAVA_PATH=!JAVA_PATH:java.exe=javaw.exe!

echo Installing Gold Resto Management System...

REM Create installation directory
mkdir "C:\Program Files\GoldResto"

REM Copy application files
copy goldresto.jar "C:\Program Files\GoldResto\"
copy application.properties "C:\Program Files\GoldResto\"

REM Create data directory
mkdir "C:\Program Files\GoldResto\data"

REM Create logs directory
mkdir "C:\Program Files\GoldResto\logs"

REM Create startup batch file
echo @echo on > "C:\Program Files\GoldResto\start.bat"
echo cd /d "C:\Program Files\GoldResto" >> "C:\Program Files\GoldResto\start.bat"
echo java --version ^| findstr "17" > nul >> "C:\Program Files\GoldResto\start.bat"
echo if errorlevel 1 ( >> "C:\Program Files\GoldResto\start.bat"
echo     echo Error: Java 17 is required to run this application >> "C:\Program Files\GoldResto\start.bat"
echo     pause >> "C:\Program Files\GoldResto\start.bat"
echo     exit /b 1 >> "C:\Program Files\GoldResto\start.bat"
echo ) >> "C:\Program Files\GoldResto\start.bat"
echo java -Xms512m -Xmx1024m --add-opens java.base/java.lang=ALL-UNNAMED -jar goldresto.jar --spring.config.location=application.properties 2^>error.log >> "C:\Program Files\GoldResto\start.bat"
echo if errorlevel 1 type error.log >> "C:\Program Files\GoldResto\start.bat"
echo pause >> "C:\Program Files\GoldResto\start.bat"

REM Create desktop shortcut
echo Set oWS = WScript.CreateObject("WScript.Shell") > CreateShortcut.vbs
echo sLinkFile = "%USERPROFILE%\Desktop\Gold Resto.lnk" >> CreateShortcut.vbs
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> CreateShortcut.vbs
echo oLink.TargetPath = "C:\Program Files\GoldResto\start.bat" >> CreateShortcut.vbs
echo oLink.WorkingDirectory = "C:\Program Files\GoldResto" >> CreateShortcut.vbs
echo oLink.Description = "Gold Resto Management System" >> CreateShortcut.vbs
echo oLink.Save >> CreateShortcut.vbs
cscript CreateShortcut.vbs
del CreateShortcut.vbs

REM Create application.properties if it doesn't exist
if not exist "C:\Program Files\GoldResto\application.properties" (
    echo # Database Configuration > "C:\Program Files\GoldResto\application.properties"
    echo spring.datasource.url=jdbc:postgresql://localhost:5432/goldresto >> "C:\Program Files\GoldResto\application.properties"
    echo spring.datasource.username=postgres >> "C:\Program Files\GoldResto\application.properties"
    echo spring.datasource.password=postgres >> "C:\Program Files\GoldResto\application.properties"
    echo spring.jpa.hibernate.ddl-auto=update >> "C:\Program Files\GoldResto\application.properties"
    echo spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect >> "C:\Program Files\GoldResto\application.properties"
    echo # Server Configuration >> "C:\Program Files\GoldResto\application.properties"
    echo server.port=8081 >> "C:\Program Files\GoldResto\application.properties"
    echo # Logging Configuration >> "C:\Program Files\GoldResto\application.properties"
    echo logging.file.path=C:/Program Files/GoldResto/logs >> "C:\Program Files\GoldResto\application.properties"
    echo logging.file.name=C:/Program Files/GoldResto/logs/goldresto.log >> "C:\Program Files\GoldResto\application.properties"
    echo logging.level.root=INFO >> "C:\Program Files\GoldResto\application.properties"
    echo logging.level.com.goldresto=DEBUG >> "C:\Program Files\GoldResto\application.properties"
)

echo Installation completed!
echo Please ensure PostgreSQL is installed and running on port 5432
pause
