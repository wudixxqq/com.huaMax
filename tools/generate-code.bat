@echo off
setlocal

set "DAYS=%~1"
if "%DAYS%"=="" set "DAYS=30"

set "NOTE=%~2"
if "%NOTE%"=="" set "NOTE=manual"

powershell -ExecutionPolicy Bypass -File "%~dp0generate-locationmax-code.ps1" -Days %DAYS% -Note "%NOTE%"

echo.
echo Copy the LM1 authorization code above.
pause
