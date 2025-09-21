# SimAS 3.0 – Linux (AppImage)

Este directorio contiene los artefactos de Linux para distribuir SimAS.

## Estructura
- `SimAS/`: aplicación Linux lista para usar (ejecutar con `./SimAS/bin/SimAS`).
- `SimAS.jar`: jar ejecutable usado por jpackage (no necesario distribuir si se envía el directorio `SimAS/`).
- `icon.png`: icono de la aplicación (usado por jpackage).
- `app-resources/`: recursos adicionales (manual, ayuda HTML, etc.) que se empaquetan.
- `input/`: carpeta auxiliar con JavaFX y recursos que jpackage usa para construir la aplicación.

## Requisitos de construcción
- JDK 17 (incluye `jpackage`). En Linux puedes comprobarlo con:
  ```bash
  java -version
  jpackage --version
  ```
- JavaFX SDK 17.0.12 para Linux (ya incluido en `input/javafx`).

## Construir la aplicación Linux
Desde la raíz del proyecto:
```bash
cd "/home/llamateke/SimAS-3.0"

# 1) Compilar las clases Java
javac -d out -cp "lib/javafx-sdk-17.0.12/lib/*" $(find src -name "*.java" -type f)

# 2) Crear el JAR ejecutable con recursos
jar --create --file apps/linux/SimAS.jar --manifest apps/linux/manifest.mf -C out . -C src .

# 3) Copiar el JAR al directorio input
cp apps/linux/SimAS.jar apps/linux/input/

# 4) Generar la aplicación con jpackage
jpackage --type app-image \
  --name SimAS \
  --icon "/home/llamateke/SimAS-3.0/apps/linux/icon.png" \
  --input "/home/llamateke/SimAS-3.0/apps/linux/input" \
  --main-jar SimAS.jar \
  --main-class bienvenida.Bienvenida \
  --resource-dir "/home/llamateke/SimAS-3.0/apps/linux/app-resources" \
  --dest "/home/llamateke/SimAS-3.0/apps/linux"
```

## Instalación y uso (Usuario final)
- Copiar el directorio `SimAS/` a la ubicación deseada
- Ejecutar con: `./SimAS/bin/SimAS`
- O crear un enlace simbólico: `ln -s /ruta/a/SimAS/bin/SimAS /usr/local/bin/simas`

No es necesario tener Java instalado en el sistema del usuario: la aplicación incluye su propio runtime.

## Notas
- Si se cambian librerías JavaFX o recursos, se debe volver a ejecutar los pasos de construcción.
- Para distribuir, basta con entregar el directorio `SimAS/` (o mejor, un `.tar.gz` del directorio).
- La aplicación incluye JavaFX 17.0.12 para Linux con todas las librerías nativas necesarias.
