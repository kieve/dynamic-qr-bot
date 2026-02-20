@echo off
setlocal

set APP_NAME=dynamic-qr-bot
set VERSION=0.0.1-SNAPSHOT
set JAR_NAME=%APP_NAME%-%VERSION%.jar
set REMOTE_USER=root
set REMOTE_HOST=tatakai.tokyo
set REMOTE_PATH=~/%JAR_NAME%

echo Building %APP_NAME%...
call "%~dp0..\gradlew.bat" build
if %ERRORLEVEL% neq 0 (
    echo Build failed.
    exit /b 1
)

set JAR_PATH=%~dp0..\build\libs\%JAR_NAME%
if not exist "%JAR_PATH%" (
    echo JAR not found at %JAR_PATH%
    exit /b 1
)

echo Deploying to %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_PATH%...
scp "%JAR_PATH%" %REMOTE_USER%@%REMOTE_HOST%:%REMOTE_PATH%
if %ERRORLEVEL% neq 0 (
    echo Deploy failed.
    exit /b 1
)

echo Deploy complete.
endlocal
