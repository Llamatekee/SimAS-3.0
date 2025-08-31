# SimAS 3.0 - Generador de Ejecutables

Este proyecto contiene scripts para crear ejecutables multiplataforma de la aplicación SimAS 3.0.

## 🎯 Solución Final

La aplicación ejecutable se encuentra en: **`./dist-standalone/SimAS.app`**

### ✅ Cómo Usar

1. **Ejecutar la aplicación:**
   ```bash
   # Hacer doble clic en:
   ./dist-standalone/SimAS.app
   ```

2. **Instalar en Applications (opcional):**
   ```bash
   cp -r ./dist-standalone/SimAS.app /Applications/
   ```

3. **Distribuir a otros usuarios:**
   - Copiar la carpeta `./dist-standalone/SimAS.app`
   - Los usuarios pueden hacer doble clic para ejecutar

## 🛠️ Scripts Disponibles

### Script Principal
- **`create-standalone-app.sh`** - Crea la aplicación independiente ejecutable

### Scripts de Compilación (para desarrolladores)
- **`build.sh`** - Compila y empaqueta para macOS/Linux
- **`build.bat`** - Compila y empaqueta para Windows

## 📋 Prerrequisitos

### Para Desarrollo
- Java 17 o superior
- JavaFX SDK 17.0.12
- jpackage (incluido en JDK 14+)

### Para Usuarios Finales
- **Ninguno** - La aplicación es completamente independiente

## 🚀 Crear Ejecutable

### Opción 1: Aplicación Independiente (Recomendada)
```bash
# 1. Compilar el proyecto
./build.sh

# 2. Crear aplicación independiente
./create-standalone-app.sh

# 3. ¡Listo! La aplicación está en ./dist-standalone/SimAS.app
```

### Opción 2: Usando jpackage (Avanzado)
```bash
# Para macOS/Linux
./build.sh

# Para Windows
./build.bat
```

## 📁 Estructura de Archivos

```
SimAS-3.0/
├── dist-standalone/
│   └── SimAS.app/          # 🎯 Aplicación ejecutable final
├── dist/
│   └── SimAS.app/          # Aplicación generada por jpackage
├── build/
│   └── SimAS.jar           # JAR compilado
├── lib/
│   └── javafx-sdk-17.0.12/ # JavaFX SDK
├── src/                    # Código fuente
├── create-standalone-app.sh # Script principal
├── build.sh               # Script de compilación macOS/Linux
├── build.bat              # Script de compilación Windows
└── README.md              # Este archivo
```

## 🔧 Características de la Solución

✅ **Completamente independiente** - No requiere JavaFX instalado  
✅ **Funciona al hacer doble clic** - Verificado en macOS  
✅ **Incluye todas las dependencias** - JavaFX, iText PDF, librerías nativas  
✅ **Fácil distribución** - Solo copiar la carpeta SimAS.app  
✅ **Multiplataforma** - Se puede adaptar para Windows y Linux  

## 🐛 Solución de Problemas

### La aplicación no se abre al hacer doble clic
1. Verifica que estás usando `./dist-standalone/SimAS.app` (no `./dist/SimAS.app`)
2. Ejecuta `./create-standalone-app.sh` para recrear la aplicación
3. Asegúrate de que tienes permisos de ejecución

### Error de librerías nativas
- La aplicación independiente incluye todas las librerías necesarias
- No se requiere configuración adicional

## 📝 Notas Técnicas

- **JavaFX**: Configurado con módulos para compatibilidad
- **iText PDF**: Incluido en el classpath para funcionalidad de PDF
- **Librerías nativas**: Copiadas automáticamente a la aplicación
- **Resource bundles**: Configurados correctamente para internacionalización

## 🎉 ¡Listo!

Tu aplicación SimAS 3.0 ahora tiene un ejecutable completamente funcional que se abre al hacer doble clic. ¡Puedes distribuir `./dist-standalone/SimAS.app` a otros usuarios!
