@echo off
setlocal enabledelayedexpansion

REM Script de build para SimAS 3.0 en Windows
REM Crea ejecutables usando jpackage

REM Configuración
set APP_NAME=SimAS
set APP_VERSION=3.0
set MAIN_CLASS=bienvenida.Bienvenida
set JAVAFX_PATH=.\lib\javafx-sdk-17.0.12
set OUTPUT_DIR=.\dist
set BUILD_DIR=.\build

echo === SimAS 3.0 Build Script ===

REM Verificar que Java esté instalado
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java no está instalado
    exit /b 1
)

REM Verificar versión de Java
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
    set JAVA_VERSION=!JAVA_VERSION:"=!
    for /f "tokens=1 delims=." %%v in ("!JAVA_VERSION!") do set JAVA_MAJOR=%%v
)

if !JAVA_MAJOR! LSS 17 (
    echo Error: Se requiere Java 17 o superior. Versión actual: !JAVA_VERSION!
    exit /b 1
)

echo Java version: !JAVA_VERSION!

REM Verificar que jpackage esté disponible
jpackage --help >nul 2>&1
if errorlevel 1 (
    echo Error: jpackage no está disponible. Asegúrate de tener Java 14+ instalado
    exit /b 1
)

REM Crear directorios
if not exist "!BUILD_DIR!" mkdir "!BUILD_DIR!"
if not exist "!OUTPUT_DIR!" mkdir "!OUTPUT_DIR!"

echo Compilando aplicación...

REM Compilar sin módulos (enfoque tradicional)
javac -cp "!JAVAFX_PATH!\lib\*;!JAVAFX_PATH!\lib\itextpdf-5.5.13.3.jar" ^
      -d "!BUILD_DIR!" ^
      src\bienvenida\*.java ^
      src\editor\*.java ^
      src\simulador\*.java ^
      src\utils\*.java ^
      src\gramatica\*.java ^
      src\centroayuda\*.java

if errorlevel 1 (
    echo Error en la compilación
    exit /b 1
)

echo Compilación completada

REM Copiar recursos
echo Copiando recursos...
xcopy /E /I /Y src\vistas "!BUILD_DIR!\vistas" >nul
xcopy /E /I /Y src\resources "!BUILD_DIR!\resources" >nul
copy /Y src\centroayuda\ayuda.html "!BUILD_DIR!\" >nul
copy /Y src\centroayuda\SimAS.html "!BUILD_DIR!\" >nul
copy /Y src\centroayuda\*.pdf "!BUILD_DIR!\" >nul
xcopy /E /I /Y src\centroayuda\imagenes "!BUILD_DIR!\imagenes" >nul
copy /Y src\utils\*.properties "!BUILD_DIR!\" >nul

REM Copiar iText JAR al directorio de build
echo Copiando dependencias...
copy "!JAVAFX_PATH!\lib\itextpdf-5.5.13.3.jar" "!BUILD_DIR!\" >nul

REM Crear JAR ejecutable
echo Creando JAR ejecutable...
cd "!BUILD_DIR!"

REM Crear MANIFEST.MF
echo Manifest-Version: 1.0 > MANIFEST.MF
echo Main-Class: !MAIN_CLASS! >> MANIFEST.MF
echo Class-Path: itextpdf-5.5.13.3.jar >> MANIFEST.MF

REM Crear JAR con MANIFEST
jar cfm SimAS.jar MANIFEST.MF *
cd /d "%~dp0"

echo Creando ejecutable para Windows...

REM Crear el ejecutable usando jpackage (sin módulos)
jpackage ^
    --input "!BUILD_DIR!" ^
    --name "!APP_NAME!" ^
    --main-jar SimAS.jar ^
    --main-class "!MAIN_CLASS!" ^
    --type app-image ^
    --dest "!OUTPUT_DIR!" ^
    --app-version "!APP_VERSION!" ^
    --vendor "Universidad de Córdoba" ^
    --description "Simulador de Autómatas y Sintaxis 3.0" ^
    --icon "src\resources\logo.png" ^
    --java-options "--module-path !JAVAFX_PATH!\lib" ^
    --java-options "--add-modules javafx.controls,javafx.fxml" ^
    --java-options "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED" ^
    --java-options "--add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED" ^
    --java-options "--add-opens=javafx.fxml/javafx.fxml=ALL-UNNAMED" ^
    --java-options "-cp !JAVAFX_PATH!\lib\itextpdf-5.5.13.3.jar"

if errorlevel 1 (
    echo Error al crear el ejecutable
    exit /b 1
)

echo ¡Ejecutable creado exitosamente!
echo Ubicación: !OUTPUT_DIR!\!APP_NAME!

REM Crear instalador EXE
echo Creando instalador...
jpackage ^
    --input "!BUILD_DIR!" ^
    --name "!APP_NAME!" ^
    --main-jar SimAS.jar ^
    --main-class "!MAIN_CLASS!" ^
    --type exe ^
    --dest "!OUTPUT_DIR!" ^
    --app-version "!APP_VERSION!" ^
    --vendor "Universidad de Córdoba" ^
    --description "Simulador de Autómatas y Sintaxis 3.0" ^
    --icon "src\resources\logo.png" ^
    --java-options "--module-path !JAVAFX_PATH!\lib" ^
    --java-options "--add-modules javafx.controls,javafx.fxml" ^
    --java-options "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED" ^
    --java-options "--add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED" ^
    --java-options "--add-opens=javafx.fxml/javafx.fxml=ALL-UNNAMED" ^
    --java-options "-cp !JAVAFX_PATH!\lib\itextpdf-5.5.13.3.jar"

if errorlevel 1 (
    echo Error al crear el instalador
    exit /b 1
)

echo ¡Instalador creado exitosamente!
echo === Build completado ===
echo Archivos generados en: !OUTPUT_DIR!

pause
