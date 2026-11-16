@echo off
pushd "%~dp0"
title DroUbicaciones - Servidor Web Local Profit Plus
color 0A
echo ========================================================
echo   Iniciando Servidor Web Local DroUbicaciones
echo   Base de Datos: Profit Plus (SRVDB0101 / DROA_A)
echo   Puerto Web: 8085 (evita conflicto con XAMPP en 8080)
echo ========================================================
echo.
java -jar "%~dp0target\dro-ubicaciones-1.0-SNAPSHOT.jar"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] No se pudo iniciar con 'java'. Intentando con JDK del sistema...
    "C:\Program Files\Java\jdk-26.0.1\bin\java.exe" -jar "%~dp0target\dro-ubicaciones-1.0-SNAPSHOT.jar"
)
popd
pause
