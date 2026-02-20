@echo off
setlocal

set REMOTE_USER=root
set REMOTE_HOST=tatakai.tokyo
set REMOTE=%REMOTE_USER%@%REMOTE_HOST%
set DEPLOY_DIR=%~dp0

echo Copying files to server...
scp "%DEPLOY_DIR%run_qr.sh" "%DEPLOY_DIR%start_qr.sh" "%DEPLOY_DIR%stop_qr.sh" "%DEPLOY_DIR%restart_qr.sh" "%DEPLOY_DIR%qr-bot.service" "%DEPLOY_DIR%Caddyfile" "%DEPLOY_DIR%..\src\main\resources\application-prod.yaml" %REMOTE%:~/
if %ERRORLEVEL% neq 0 goto :fail

echo Running setup on server...
ssh %REMOTE% "chmod +x ~/run_qr.sh ~/start_qr.sh ~/stop_qr.sh ~/restart_qr.sh && mv ~/qr-bot.service /etc/systemd/system/ && systemctl daemon-reload && systemctl enable qr-bot"
if %ERRORLEVEL% neq 0 goto :fail

echo Installing Caddy...
ssh %REMOTE% "apt install -y caddy && mv ~/Caddyfile /etc/caddy/Caddyfile && systemctl restart caddy"
if %ERRORLEVEL% neq 0 goto :fail

echo Install complete. Run 'deploy.bat' to build and deploy the JAR, then '~/start_qr.sh' on the server.
goto :end

:fail
echo Install failed.
exit /b 1

:end
endlocal
