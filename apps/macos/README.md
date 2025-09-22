# SimAS 3.0 – macOS (app-image y DMG)

Este directorio contiene los artefactos de macOS para distribuir SimAS.

## Estructura
- `SimAS.app`: aplicación macOS lista para usar (doble clic para ejecutar).
- `SimAS-3.0.dmg`: instalador opcional en formato DMG.
- `SimAS.jar`: jar ejecutable usado por jpackage (no necesario distribuir si se envía el `.app` o `.dmg`).
- `icon.icns`: icono de la aplicación (usado por jpackage).
- `app-resources/`: recursos adicionales (manual, ayuda HTML, etc.) que se empaquetan.
- `input/`: carpeta auxiliar con JavaFX y recursos que jpackage usa para construir el `.app` (puede omitirse al distribuir, pero se conserva por reproducibilidad del build).
- `manifest.mf`: manifiesto usado para crear `SimAS.jar`.

## Requisitos de construcción
- JDK 17 (incluye `jpackage`). En macOS puedes comprobarlo con:
  ```bash
  /usr/libexec/java_home -V
  jpackage --version
  ```
- JavaFX SDK 17.0.12 ya está incluido en `input/javafx`.
- Herramientas de iconos (ya ejecutadas): `sips` e `iconutil` (presentes en macOS).

## Construir el .app (app-image)
Desde la raíz del proyecto:
```bash
cd "/Users/llamateke/Desktop/SimAS-3.0"
# 1) Asegúrate de tener clases compiladas
# (ya presentes en out/). Vuelve a crear el jar ejecutable:
MANIFEST="apps/macos/manifest.mf"
echo "Manifest-Version: 1.0" > "$MANIFEST"
echo "Main-Class: bienvenida.Bienvenida" >> "$MANIFEST"
jar --create --file apps/macos/SimAS.jar --manifest "$MANIFEST" -C out .

# 2) Genera el .app con icono y JavaFX (module-path relativo al bundle)
jpackage --type app-image \
  --name SimAS \
  --icon "/Users/llamateke/Desktop/SimAS-3.0/apps/macos/icon.icns" \
  --input "/Users/llamateke/Desktop/SimAS-3.0/apps/macos/input" \
  --main-jar SimAS.jar \
  --main-class bienvenida.Bienvenida \
  --java-options "--module-path $APPDIR/javafx --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.web" \
  --resource-dir "/Users/llamateke/Desktop/SimAS-3.0/apps/macos/app-resources" \
  --dest "/Users/llamateke/Desktop/SimAS-3.0/apps/macos"
```

### Incluir recursos (manual y ayuda)

- Coloca `Manual_de_Usuario.pdf` en `apps/macos/app-resources/` antes de ejecutar `jpackage`.
- El archivo HTML del tutorial debe estar en `apps/macos/app-resources/src/centroayuda/SimAS.html` (ya incluido en este repo).
- En tiempo de ejecución dentro del bundle, las rutas esperadas serán:
  - `SimAS.app/Contents/app/Manual_de_Usuario.pdf`
  - `SimAS.app/Contents/app/src/centroayuda/SimAS.html`
- La aplicación resuelve estas rutas automáticamente relativo al directorio del JAR (`Contents/app`).

## Construir el DMG
```bash
jpackage --type dmg \
  --name SimAS \
  --app-image "/Users/llamateke/Desktop/SimAS-3.0/apps/macos/SimAS.app" \
  --icon "/Users/llamateke/Desktop/SimAS-3.0/apps/macos/icon.icns" \
  --dest "/Users/llamateke/Desktop/SimAS-3.0/apps/macos"
```

## Instalación y uso (Usuario final)
- Opción 1: DMG
  1. Abrir `SimAS-3.0.dmg`.
  2. Arrastrar `SimAS.app` a la carpeta Aplicaciones.
  3. Primera ejecución (app no firmada): doble clic (o clic derecho > Abrir la primera vez).
- Opción 2: App suelta
  - Copiar `SimAS.app` a `/Applications` y abrir con doble clic (o clic derecho > Abrir la primera vez).

No es necesario tener Java instalado en el sistema del usuario: el `.app` incluye su propio runtime.

## Firma y notarización (opcional, recomendado)
Para evitar el aviso de seguridad en la primera ejecución, firma y notariza el `.app` con un Apple Developer ID:
```bash
codesign --deep --force --options runtime --sign "Developer ID Application: TU_NOMBRE (TEAMID)" \
  "/Users/llamateke/Desktop/SimAS-3.0/apps/macos/SimAS.app"

# Crear ZIP para subir a notarización
/usr/bin/ditto -c -k --keepParent \
  "/Users/llamateke/Desktop/SimAS-3.0/apps/macos/SimAS.app" \
  "/Users/llamateke/Desktop/SimAS-3.0/apps/macos/SimAS.zip"

xcrun notarytool submit "/Users/llamateke/Desktop/SimAS-3.0/apps/macos/SimAS.zip" \
  --apple-id TU_APPLE_ID --team-id TEAMID --password "app-specific-password" --wait

xcrun stapler staple "/Users/llamateke/Desktop/SimAS-3.0/apps/macos/SimAS.app"
```

## Notas
- Si se cambian librerías JavaFX o recursos, se debe volver a ejecutar los pasos de construcción.
- Para distribuir, basta con entregar el `.dmg` o `SimAS.app` (o mejor, un `.zip` de `SimAS.app`).
