GoldResto Deployment Bundle
===========================

Requirements:
- Java 17 JRE/JDK on target machine (java -version should show 17)
- Maven on build machine (mvn -version)
- PostgreSQL reachable with credentials configured in application.properties

Build steps (on your development machine):
1) From the project root, build the fat JAR:
   mvn -DskipTests package

2) The executable JAR will be created at:
   target/goldresto.jar

Run locally after build:
- Windows PowerShell:
  .\deploy\run.ps1
- Windows CMD:
  deploy\run.bat

Deploy to another computer:
1) Copy the generated ZIP bundle to the target machine.
2) Unzip it to a folder with write permissions.
3) Ensure Java 17 is installed and on PATH.
4) Start the app with:
   - PowerShell: .\deploy\run.ps1
   - CMD:       deploy\run.bat

Configuration:
- Spring Boot configuration files are under src/main/resources/ (e.g., application.properties or profile-specific files).
- Adjust database and other settings as needed before building the bundle.

Notes:
- The JAR is built with Spring Boot Maven Plugin and is self-contained (Spring Boot loader plus dependencies).
- Default JVM memory flags are set in the run scripts and can be adjusted if necessary.
