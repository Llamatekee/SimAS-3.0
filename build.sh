#!/bin/bash

# Script de build para SimAS 3.0
# Crea ejecutables multiplataforma usando jpackage

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuración
APP_NAME="SimAS"
APP_VERSION="3.0"
MAIN_CLASS="bienvenida.Bienvenida"
JAVAFX_PATH="./lib/javafx-sdk-17.0.12"
OUTPUT_DIR="./dist"
BUILD_DIR="./build"

echo -e "${GREEN}=== SimAS 3.0 Build Script ===${NC}"

# Verificar que Java 17+ esté instalado
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java no está instalado${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Error: Se requiere Java 17 o superior. Versión actual: $JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${GREEN}Java version: $(java -version 2>&1 | head -n 1)${NC}"

# Verificar que jpackage esté disponible
if ! command -v jpackage &> /dev/null; then
    echo -e "${RED}Error: jpackage no está disponible. Asegúrate de tener Java 14+ instalado${NC}"
    exit 1
fi

# Crear directorios
mkdir -p "$BUILD_DIR"
mkdir -p "$OUTPUT_DIR"

echo -e "${YELLOW}Compilando aplicación...${NC}"

# Compilar con módulos JavaFX pero sin iText como módulo
javac --module-path "$JAVAFX_PATH/lib" \
      --add-modules javafx.controls,javafx.fxml \
      -cp "$JAVAFX_PATH/lib/itextpdf-5.5.13.3.jar" \
      -d "$BUILD_DIR" \
      $(find src -name "*.java")

echo -e "${GREEN}Compilación completada${NC}"

# Copiar recursos
echo -e "${YELLOW}Copiando recursos...${NC}"
cp -r src/vistas "$BUILD_DIR/"
cp -r src/resources "$BUILD_DIR/"
cp -r src/centroayuda/ayuda.html "$BUILD_DIR/"
cp -r src/centroayuda/SimAS.html "$BUILD_DIR/"
cp -r src/centroayuda/*.pdf "$BUILD_DIR/"
cp -r src/centroayuda/imagenes "$BUILD_DIR/"
cp -r src/utils/*.properties "$BUILD_DIR/"

# Copiar iText JAR al directorio de build
echo -e "${YELLOW}Copiando dependencias...${NC}"
cp "$JAVAFX_PATH/lib/itextpdf-5.5.13.3.jar" "$BUILD_DIR/"

# Crear JAR ejecutable
echo -e "${YELLOW}Creando JAR ejecutable...${NC}"
cd "$BUILD_DIR"

# Crear MANIFEST.MF
cat > MANIFEST.MF << EOF
Manifest-Version: 1.0
Main-Class: $MAIN_CLASS
Class-Path: itextpdf-5.5.13.3.jar
EOF

# Crear JAR con MANIFEST
jar cfm SimAS.jar MANIFEST.MF *
cd - > /dev/null

# Detectar el sistema operativo
OS=$(uname -s)
case "$OS" in
    Darwin*)    PLATFORM="mac";;
    Linux*)     PLATFORM="linux";;
    MINGW*|CYGWIN*|MSYS*) PLATFORM="windows";;
    *)          echo -e "${RED}Sistema operativo no soportado: $OS${NC}"; exit 1;;
esac

echo -e "${YELLOW}Creando ejecutable para $PLATFORM...${NC}"

# Crear el ejecutable usando jpackage
jpackage \
    --input "$BUILD_DIR" \
    --name "$APP_NAME" \
    --main-jar SimAS.jar \
    --main-class "$MAIN_CLASS" \
    --module-path "$JAVAFX_PATH/lib" \
    --add-modules javafx.controls,javafx.fxml \
    --type app-image \
    --dest "$OUTPUT_DIR" \
    --app-version "$APP_VERSION" \
    --vendor "Universidad de Córdoba" \
    --description "Simulador de Autómatas y Sintaxis 3.0" \
    --icon "src/resources/logo.png" \
    --java-options "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED" \
    --java-options "--add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED" \
    --java-options "--add-opens=javafx.fxml/javafx.fxml=ALL-UNNAMED" \
    --java-options "-cp $JAVAFX_PATH/lib/itextpdf-5.5.13.3.jar"

echo -e "${GREEN}¡Ejecutable creado exitosamente!${NC}"
echo -e "${GREEN}Ubicación: $OUTPUT_DIR/$APP_NAME${NC}"

# Crear instalador si es posible
echo -e "${YELLOW}Creando instalador...${NC}"

case "$PLATFORM" in
            "mac")
            jpackage \
                --input "$BUILD_DIR" \
                --name "$APP_NAME" \
                --main-jar SimAS.jar \
                --main-class "$MAIN_CLASS" \
                --module-path "$JAVAFX_PATH/lib" \
                --add-modules javafx.controls,javafx.fxml \
                --type dmg \
                --dest "$OUTPUT_DIR" \
                --app-version "$APP_VERSION" \
                --vendor "Universidad de Córdoba" \
                --description "Simulador de Autómatas y Sintaxis 3.0" \
                --icon "src/resources/logo.png" \
                --java-options "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED" \
                --java-options "--add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED" \
                --java-options "--add-opens=javafx.fxml/javafx.fxml=ALL-UNNAMED" \
                --java-options "-cp $JAVAFX_PATH/lib/itextpdf-5.5.13.3.jar"
        ;;
    "linux")
        jpackage \
            --input "$BUILD_DIR" \
            --name "$APP_NAME" \
            --main-jar SimAS.jar \
            --main-class "$MAIN_CLASS" \
            --module-path "$JAVAFX_PATH/lib" \
            --add-modules javafx.controls,javafx.fxml \
            --type deb \
            --dest "$OUTPUT_DIR" \
            --app-version "$APP_VERSION" \
            --vendor "Universidad de Córdoba" \
            --description "Simulador de Autómatas y Sintaxis 3.0" \
            --icon "src/resources/logo.png" \
            --java-options "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED" \
            --java-options "--add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED" \
            --java-options "--add-opens=javafx.fxml/javafx.fxml=ALL-UNNAMED" \
            --java-options "-cp $JAVAFX_PATH/lib/itextpdf-5.5.13.3.jar"
        ;;
    "windows")
        jpackage \
            --input "$BUILD_DIR" \
            --name "$APP_NAME" \
            --main-jar SimAS.jar \
            --main-class "$MAIN_CLASS" \
            --module-path "$JAVAFX_PATH/lib" \
            --add-modules javafx.controls,javafx.fxml \
            --type exe \
            --dest "$OUTPUT_DIR" \
            --app-version "$APP_VERSION" \
            --vendor "Universidad de Córdoba" \
            --description "Simulador de Autómatas y Sintaxis 3.0" \
            --icon "src/resources/logo.png" \
            --java-options "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED" \
            --java-options "--add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED" \
            --java-options "--add-opens=javafx.fxml/javafx.fxml=ALL-UNNAMED" \
            --java-options "-cp $JAVAFX_PATH/lib/itextpdf-5.5.13.3.jar"
        ;;
esac

echo -e "${GREEN}¡Instalador creado exitosamente!${NC}"
echo -e "${GREEN}=== Build completado ===${NC}"
echo -e "${YELLOW}Archivos generados en: $OUTPUT_DIR${NC}"
