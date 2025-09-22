# SimAS 3.0 – Windows (app-image y MSI)

Este directorio contiene los artefactos para construir y distribuir SimAS en Windows.

## Estructura
- `SimAS/`: app-image generado por `jpackage` (ejecutable `SimAS.exe`).
- `SimAS-3.0.msi`: instalador MSI generado por el script (si hay WiX).
- `SimAS.jar`: JAR ejecutable usado internamente por `jpackage` (no es necesario distribuirlo).
- `icon.ico` / `icon.png`: iconos generados desde `src/resources/logo2.png` si está disponible.
- `input/`: carpeta auxiliar con JavaFX, jar y recursos que `jpackage` usa para construir (se limpia en cada build).
- `libs/`: dependencias externas (iText) que el build descarga si faltan.
- `build.ps1`: script para compilar, empaquetar y crear el MSI.

## Requisitos de construcción
- Windows con JDK 17 en el `PATH` (incluye `jpackage`).
- PowerShell 5+ o PowerShell 7.
- `WiX Toolset 3.11+` para poder generar MSI (`light.exe` debe estar en el `PATH`).
  - Descarga: `https://wixtoolset.org/`.
- Opcional: `ImageMagick` (`magick` o `convert`) para generar `icon.ico` desde `logo2.png`.

Comprueba las herramientas:
```powershell
java -version
jpackage --version
Get-Command light.exe  # WiX (opcional, solo para MSI)
```

## Construir app-image y MSI
Desde la raíz del proyecto:
```powershell
pwsh -File ".\apps\windows\build.ps1"
# o con PowerShell clásico
powershell -ExecutionPolicy Bypass -File ".\apps\windows\build.ps1"
```
Resultados:
- App image: `apps/windows/SimAS/SimAS.exe`
- MSI: `apps/windows/SimAS-3.0.msi` (si WiX está instalado)

Para omitir el MSI explícitamente:
```powershell
pwsh -File ".\apps\windows\build.ps1" -SkipMSI
```

## Icono de la aplicación
- El build usa `src/resources/logo2.png` si existe.
- Genera `icon.ico` automáticamente si `ImageMagick` está disponible.
- El MSI crea acceso directo en el menú Inicio con el icono.

## Ejecución (usuario final)
- Ejecutar sin instalar: `apps\windows\SimAS\SimAS.exe`.
- Instalar: doble clic en `apps\windows\SimAS-3.0.msi` y seguir el asistente.

## Notas
- El paquete incluye JavaFX y dependencias: no requiere Java en el sistema del usuario.
- Si cambias recursos (manual, ayuda), vuelve a ejecutar el script.
- Para firmar el MSI, usa `signtool.exe` tras la generación.
