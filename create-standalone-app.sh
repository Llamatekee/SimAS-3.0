#!/bin/bash

# Script para crear una aplicación completamente independiente

echo "=== Creando aplicación independiente ==="

APP_NAME="SimAS"
OUTPUT_DIR="./dist-standalone"
BUILD_DIR="./build"

if [ ! -d "$BUILD_DIR" ]; then
    echo "ERROR: No se encontró el directorio de build"
    echo "Ejecuta primero: ./build-with-natives.sh"
    exit 1
fi

# Crear directorio de salida
mkdir -p "$OUTPUT_DIR"

echo "SUCCESS: Directorio de salida creado"

# Crear estructura de la aplicación
APP_PATH="$OUTPUT_DIR/$APP_NAME.app"
mkdir -p "$APP_PATH/Contents/MacOS"
mkdir -p "$APP_PATH/Contents/Java"
mkdir -p "$APP_PATH/Contents/Resources"

echo "SUCCESS: Estructura de aplicación creada"

# Copiar JAR principal
cp "$BUILD_DIR/SimAS.jar" "$APP_PATH/Contents/Java/"

# Copiar todas las librerías de JavaFX
echo "Copiando librerías de JavaFX..."
cp lib/javafx-sdk-17.0.12/lib/*.jar "$APP_PATH/Contents/Java/"
cp lib/javafx-sdk-17.0.12/lib/*.dylib "$APP_PATH/Contents/Java/"

# Crear script de lanzamiento
echo "Creando script de lanzamiento..."
cat > "$APP_PATH/Contents/MacOS/$APP_NAME" << 'EOF'
#!/bin/bash

# Script de lanzamiento para SimAS
APP_DIR="$(dirname "$0")/.."
JAR_PATH="$APP_DIR/Java/SimAS.jar"
JAVA_LIB_PATH="$APP_DIR/Java"

# Construir classpath
CLASSPATH="$JAR_PATH"
for jar in "$JAVA_LIB_PATH"/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# Ejecutar con JavaFX modules
exec java \
    --module-path "$JAVA_LIB_PATH" \
    --add-modules javafx.controls,javafx.fxml \
    --add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED \
    --add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED \
    --add-opens=javafx.fxml/javafx.fxml=ALL-UNNAMED \
    -cp "$CLASSPATH" \
    -Djava.library.path="$JAVA_LIB_PATH" \
    bienvenida.Bienvenida
EOF

# Hacer el script ejecutable
chmod +x "$APP_PATH/Contents/MacOS/$APP_NAME"

# Crear Info.plist
echo "Creando Info.plist..."
cat > "$APP_PATH/Contents/Info.plist" << EOF
<?xml version="1.0" ?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "https://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
 <dict>
  <key>LSMinimumSystemVersion</key>
  <string>10.11</string>
  <key>CFBundleDevelopmentRegion</key>
  <string>English</string>
  <key>CFBundleAllowMixedLocalizations</key>
  <true/>
  <key>CFBundleExecutable</key>
  <string>$APP_NAME</string>
  <key>CFBundleIconFile</key>
  <string>$APP_NAME.icns</string>
  <key>CFBundleIdentifier</key>
  <string>com.simas.app</string>
  <key>CFBundleInfoDictionaryVersion</key>
  <string>6.0</string>
  <key>CFBundleName</key>
  <string>$APP_NAME</string>
  <key>CFBundlePackageType</key>
  <string>APPL</string>
  <key>CFBundleShortVersionString</key>
  <string>3.0</string>
  <key>CFBundleSignature</key>
  <string>????</string>
  <key>LSApplicationCategoryType</key>
  <string>public.app-category.utilities</string>
  <key>CFBundleVersion</key>
  <string>3.0</string>
  <key>NSHumanReadableCopyright</key>
  <string>Copyright (C) 2025 Universidad de Córdoba</string>
  <key>NSHighResolutionCapable</key>
  <string>true</string>
 </dict>
</plist>
EOF

echo "SUCCESS: Aplicación independiente creada en $APP_PATH"

# Probar la aplicación
echo "Probando la aplicación..."
"$APP_PATH/Contents/MacOS/$APP_NAME" &
APP_PID=$!

sleep 5

if kill -0 $APP_PID 2>/dev/null; then
    echo "SUCCESS: ¡La aplicación se está ejecutando correctamente!"
    kill $APP_PID
else
    echo "ERROR: La aplicación no se pudo ejecutar"
fi

echo "=== Aplicación independiente creada ==="
echo "Ubicación: $APP_PATH"
echo "Puedes hacer doble clic en $APP_PATH para ejecutar la aplicación"
