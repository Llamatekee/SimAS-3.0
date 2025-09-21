#!/bin/bash

# Script para construir SimAS para Linux
# Uso: ./build-linux.sh

set -e  # Salir si hay algún error

echo "=== Construyendo SimAS para Linux ==="

# Verificar que estamos en el directorio correcto
if [ ! -f "src/bienvenida/Bienvenida.java" ]; then
    echo "Error: Ejecuta este script desde la raíz del proyecto SimAS-3.0"
    exit 1
fi

# Verificar Java 17
echo "Verificando Java 17..."
if ! java -version 2>&1 | grep -q "17\."; then
    echo "Error: Se requiere Java 17"
    exit 1
fi

# Verificar jpackage
echo "Verificando jpackage..."
if ! jpackage --version > /dev/null 2>&1; then
    echo "Error: jpackage no está disponible"
    exit 1
fi

# Limpiar compilaciones anteriores
echo "Limpiando compilaciones anteriores..."
rm -rf out
rm -rf apps/linux/SimAS
rm -f apps/linux/SimAS.jar

# Crear directorios necesarios
echo "Creando estructura de directorios..."
mkdir -p out
mkdir -p apps/linux/input
mkdir -p apps/linux/app-resources

# Compilar clases Java
echo "Compilando clases Java..."
javac -d out -cp "lib/javafx-sdk-17.0.12/lib/*" $(find src -name "*.java" -type f)

# Crear JAR ejecutable
echo "Creando JAR ejecutable..."
jar --create --file apps/linux/SimAS.jar --manifest apps/linux/manifest.mf -C out . -C src .

# Copiar recursos
echo "Copiando recursos..."
cp -r lib/javafx-sdk-17.0.12 apps/linux/input/javafx
cp -r src/resources apps/linux/input/
cp -r apps/macos/app-resources/* apps/linux/app-resources/
cp apps/macos/SimAS.iconset/icon_256x256.png apps/linux/icon.png

# Copiar JAR al directorio input
cp apps/linux/SimAS.jar apps/linux/input/

# Crear aplicación con jpackage
echo "Creando aplicación con jpackage..."
jpackage --type app-image \
  --name SimAS \
  --icon "/home/llamateke/SimAS-3.0/apps/linux/icon.png" \
  --input "/home/llamateke/SimAS-3.0/apps/linux/input" \
  --main-jar SimAS.jar \
  --main-class bienvenida.Bienvenida \
  --resource-dir "/home/llamateke/SimAS-3.0/apps/linux/app-resources" \
  --dest "/home/llamateke/SimAS-3.0/apps/linux"

echo "=== ¡Construcción completada! ==="
echo "La aplicación está disponible en: apps/linux/SimAS/"
echo "Para ejecutar: ./apps/linux/SimAS/bin/SimAS"
