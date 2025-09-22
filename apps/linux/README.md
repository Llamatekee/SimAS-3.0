# SimAS 3.0 – Linux (app-image y .deb)

Este directorio contiene los artefactos para construir y distribuir SimAS en Linux.

## Estructura
- `SimAS/`: app-image generado por jpackage (puede ejecutarse directamente con `bin/SimAS`).
- `SimAS-3.0.deb`: instalador Debian/Ubuntu generado por el script.
- `SimAS.jar`: JAR ejecutable usado internamente por jpackage (no es necesario distribuirlo).
- `icon.png`: icono temporal generado a partir de `src/resources/logo2.png`.
- `libs/`: dependencias externas (p.ej. iText) que el build descarga si faltan.
- `input/`: carpeta auxiliar con JavaFX y recursos que jpackage usa para construir (se limpia/recrea en cada build).
- `build.sh`: script único para compilar, empaquetar y crear el `.deb`.

## Requisitos de construcción
- JDK 17 con `jpackage` disponible en el `PATH`.
- Herramienta `curl` o `wget` (para descargar dependencias si faltan).
- `unzip` para extraer JavaFX si no está presente.
- Opcional para mejor calidad de iconos: `ImageMagick` (`convert`).

Puedes comprobarlo con:
```bash
java -version
jpackage --version
convert --version   # opcional
```

## Construir app-image y .deb
Desde la raíz del proyecto:
```bash
bash apps/linux/build.sh
```
El script compila el código, crea el `SimAS.jar`, genera el app-image y empaqueta el instalador `.deb` con nombre fijo `SimAS-3.0.deb`.

### Icono de la aplicación
- El build usa siempre `src/resources/logo2.png`.
- El `.deb` instala iconos en el tema del sistema (`/usr/share/icons/hicolor/{64,128,256,512}x{size}/apps/simas.png`) y modifica la entrada `.desktop` para `Icon=simas`.
- Esto garantiza que el icono correcto se vea en el lanzador incluso antes de ejecutar la app.

## Instalación (usuario final)
Instalar/reinstalar el paquete:
```bash
sudo apt install ./apps/linux/SimAS-3.0.deb
# o
sudo apt install --reinstall ./apps/linux/SimAS-3.0.deb
```
Esto instalará la aplicación en `/opt/simas` y registrará un lanzador en el menú.

## Ejecución
- Desde el menú de aplicaciones: busca "SimAS".
- Desde terminal tras instalar el `.deb`:
```bash
/opt/simas/bin/SimAS
```
- Ejecutar el app-image sin instalar:
```bash
apps/linux/SimAS/bin/SimAS
```

## Resolución de problemas
- Icono no actualizado en el lanzador:
  - Asegúrate de haber reinstalado el `.deb` más reciente.
  - Refresca la caché de iconos (según distro/DE):
    ```bash
    sudo update-icon-caches /usr/share/icons/hicolor 2>/dev/null || \
    sudo gtk-update-icon-cache -f /usr/share/icons/hicolor 2>/dev/null || true
    ```
  - Elimina posibles accesos directos antiguos en `~/.local/share/applications/` que apunten a un PNG absoluto.
- Falta JavaFX: el paquete incluye su propio runtime; no requiere Java en el sistema.
- `jpackage` no encontrado al construir: instala OpenJDK 17 (`sudo apt install openjdk-17-jdk`) o utiliza un JDK 17 que lo incluya.

## Notas
- El script baja automáticamente JavaFX 17.0.12 para Linux si no está presente.
- El nombre del paquete de salida está fijado a `SimAS-3.0.deb` para facilitar la distribución.
- Si cambias recursos (iconos, manual, etc.), vuelve a ejecutar `build.sh`.
