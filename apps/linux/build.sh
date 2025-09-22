#!/bin/bash

# Minimal builder for SimAS (Linux app-image + .deb) with logo2.png icon
# Requirements provided so far:
# 1) Application name: SimAS
# 2) Use src/resources/logo2.png as application icon

set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
LINUX_DIR="$ROOT_DIR/apps/linux"
OUT_DIR="$ROOT_DIR/out"
INPUT_DIR="$LINUX_DIR/input"
APP_NAME="SimAS"
APP_DIR="$LINUX_DIR/$APP_NAME"
APP_VERSION="3.0.0"
APP_VENDOR="SimAS Project"
APP_COMMENT="Simulador de Análisis Sintáctico"

# OpenJFX 17 for Linux (SDK)
FX_VERSION="17.0.12"
FX_BASENAME="openjfx-${FX_VERSION}_linux-x64_bin-sdk.zip"
FX_URL="https://download2.gluonhq.com/openjfx/${FX_VERSION}/${FX_BASENAME}"
FX_SDK_DIR="$LINUX_DIR/javafx-sdk-${FX_VERSION}"
FX_ZIP="$LINUX_DIR/${FX_BASENAME}"

# Third-party runtime libs
LIBS_DIR="$LINUX_DIR/libs"
ITEXT_VER="5.5.13.3"
ITEXT_JAR="itextpdf-${ITEXT_VER}.jar"
ITEXT_URL="https://repo1.maven.org/maven2/com/itextpdf/itextpdf/${ITEXT_VER}/${ITEXT_JAR}"

echo "=== Building $APP_NAME for Linux ==="

# 0) Guard: run from project root
if [ ! -f "$ROOT_DIR/src/bienvenida/Bienvenida.java" ]; then
  echo "Error: run this script from anywhere, it will auto-detect root at $ROOT_DIR, but project structure seems missing."
  exit 1
fi

# 1) Tooling checks
echo "Checking Java 17..."
if ! java -version 2>&1 | grep -q "17\."; then
  echo "Error: Java 17 is required"
  exit 1
fi

echo "Checking jpackage..."
if ! jpackage --version >/dev/null 2>&1; then
  echo "Error: jpackage is not available"
  exit 1
fi

# 2) Clean previous build
echo "Cleaning previous build..."
rm -rf "$OUT_DIR" "$APP_DIR" "$LINUX_DIR/${APP_NAME}.jar" "$INPUT_DIR" "$LINUX_DIR"/*.deb "$LINUX_DIR/deb-work" 2>/dev/null || true
mkdir -p "$OUT_DIR" "$INPUT_DIR"

# 3) Ensure OpenJFX Linux SDK present
if [ ! -d "$FX_SDK_DIR" ]; then
  echo "Fetching OpenJFX $FX_VERSION for Linux..."
  if [ ! -f "$FX_ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -L -o "$FX_ZIP" "$FX_URL"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$FX_ZIP" "$FX_URL"
    else
      echo "Error: neither curl nor wget available to download OpenJFX"
      exit 1
    fi
  fi
  unzip -q "$FX_ZIP" -d "$LINUX_DIR"
fi

# 4) Compile sources
echo "Preparing third-party libraries..."
mkdir -p "$LIBS_DIR"
if [ ! -f "$LIBS_DIR/${ITEXT_JAR}" ]; then
  echo "Downloading iText ${ITEXT_VER}..."
  if command -v curl >/dev/null 2>&1; then
    curl -L -o "$LIBS_DIR/${ITEXT_JAR}" "$ITEXT_URL"
  else
    wget -O "$LIBS_DIR/${ITEXT_JAR}" "$ITEXT_URL"
  fi
fi

echo "Compiling Java sources..."
javac -d "$OUT_DIR" -cp "$FX_SDK_DIR/lib/*:$LIBS_DIR/*" $(find "$ROOT_DIR/src" -name "*.java" -type f)

# 5) Create executable JAR (no Main-Class needed because jpackage sets it)
echo "Creating JAR..."
jar --create --file "$LINUX_DIR/${APP_NAME}.jar" -C "$OUT_DIR" . -C "$ROOT_DIR/src" .

# 6) Prepare inputs for jpackage
echo "Preparing inputs..."
cp -r "$FX_SDK_DIR" "$INPUT_DIR/javafx"
cp "$LINUX_DIR/${APP_NAME}.jar" "$INPUT_DIR/"
cp "$LIBS_DIR/${ITEXT_JAR}" "$INPUT_DIR/"

# 7) Prepare icon from src/resources/logo2.png
ICON_SRC="$ROOT_DIR/src/resources/logo2.png"
ICON_DST="$LINUX_DIR/icon.png"
if [ -f "$ICON_SRC" ]; then
  echo "Preparing icon from $ICON_SRC"
  if command -v convert >/dev/null 2>&1; then
    convert "$ICON_SRC" -resize 256x256 -background none -gravity center -extent 256x256 "$ICON_DST"
  else
    cp "$ICON_SRC" "$ICON_DST"
  fi
else
  echo "Warning: $ICON_SRC not found; icon will be missing"
fi

# 8) Build app-image
echo "Creating app-image..."
jpackage --type app-image \
  --name "$APP_NAME" \
  --icon "$ICON_DST" \
  --input "$INPUT_DIR" \
  --main-jar "${APP_NAME}.jar" \
  --main-class "bienvenida.Bienvenida" \
  --java-options "--module-path \$APPDIR/javafx/lib" \
  --java-options "--add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web,javafx.swing" \
  --dest "$LINUX_DIR"

# 9) Build .deb package
echo "Creating .deb package..."
jpackage --type deb \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "$APP_VENDOR" \
  --app-image "$APP_DIR" \
  --linux-shortcut \
  --linux-menu-group "Education" \
  --linux-deb-maintainer "simas@example.com" \
  --linux-package-name simas \
  --dest "$LINUX_DIR"

# 10) Optional post-process to add CLI symlink (disabled by default; set PACKAGE_SYMLINKS=1 to enable)
if [ "${PACKAGE_SYMLINKS:-0}" = "1" ]; then
  DEB_PATH=$(ls -1 "$LINUX_DIR"/simas_*_amd64.deb | tail -n 1 || true)
  if [ -n "$DEB_PATH" ]; then
    echo "Adjusting .deb (CLI symlink)..."
    WORK_DIR="$LINUX_DIR/deb-work"
    rm -rf "$WORK_DIR" && mkdir -p "$WORK_DIR"
    dpkg-deb -R "$DEB_PATH" "$WORK_DIR"
    mkdir -p "$WORK_DIR/usr/local/bin"
    ln -sf /opt/simas/bin/SimAS "$WORK_DIR/usr/local/bin/simas"
    NEW_DEB="$LINUX_DIR/simas_${APP_VERSION}-1_amd64.deb"
    dpkg-deb -b "$WORK_DIR" "$NEW_DEB" >/dev/null
    echo "Created $NEW_DEB"
  fi
fi

echo "=== Done ==="
echo "- App image: $APP_DIR"
echo "- Run locally: $APP_DIR/bin/SimAS"
echo "- Installer:  $LINUX_DIR/simas_${APP_VERSION}-1_amd64.deb"


