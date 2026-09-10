@echo off
setlocal

set "MAVEN_VERSION=3.9.6"
set "MAVEN_DIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_DIR%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd"

if exist "%MAVEN_BIN%" (
    goto runMaven
)

echo [MVNW] Downloading Apache Maven %MAVEN_VERSION%...
set "MAVEN_ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"
powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%MAVEN_ZIP%'"

if not exist "%MAVEN_DIR%" (
    mkdir "%MAVEN_DIR%"
)

echo [MVNW] Extracting Maven...
powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%MAVEN_DIR%' -Force"

:runMaven
call "%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%
