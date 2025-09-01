# SimAS 3.0 - Generador de Ejecutables

Este proyecto contiene scripts para crear ejecutables de la aplicación SimAS 3.0.

## 🎯 Solución Actual

La aplicación ejecutable se encuentra en: **`./dist-standalone/SimAS.app`**

### ✅ Cómo Usar

```bash
# Hacer doble clic en:
./dist-standalone/SimAS.app
```

## 🛠️ Scripts Disponibles

### Script Principal
- **`create-standalone-app.sh`** - Crea la aplicación independiente para macOS

### Scripts de Compilación (para desarrolladores)
- **`build.sh`** - Compila y empaqueta para macOS/Linux
- **`build.bat`** - Compila y empaqueta para Windows

## 📋 Prerrequisitos

### Para Desarrollo
- Java 17 o superior
- JavaFX SDK 17.0.12
- jpackage (incluido en JDK 14+)

### Para Usuarios Finales
- **Java Runtime Environment (JRE) 17+** - Requerido
- **Ninguna otra dependencia** - La aplicación incluye JavaFX

## 🚀 Crear Ejecutable

### Opción 1: Aplicación Independiente (macOS)
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

## ⚠️ Dependencias

### Para Usuarios Finales
**La aplicación requiere Java Runtime Environment (JRE) 17 o superior**

#### Instalar Java en macOS:
```bash
# Con Homebrew
brew install openjdk@17

# O descargar desde Oracle
# https://www.oracle.com/java/technologies/downloads/
```

## 🐛 Solución de Problemas

### La aplicación no se abre al hacer doble clic
1. Verifica que tienes Java 17+ instalado
2. Ejecuta `./create-standalone-app.sh` para recrear la aplicación
3. Asegúrate de que tienes permisos de ejecución

### Error: "Java no encontrado"
- Instalar Java 17+ desde https://adoptium.net/

### Error: "JavaFX no encontrado"
- Los ejecutables incluyen JavaFX, pero requieren Java base

## 📝 Notas Técnicas

- **JavaFX**: Configurado con módulos para compatibilidad
- **iText PDF**: Incluido en el classpath para funcionalidad de PDF
- **Librerías nativas**: Copiadas automáticamente a la aplicación
- **Resource bundles**: Configurados correctamente para internacionalización

## 🎉 ¡Listo!

Tu aplicación SimAS 3.0 tiene un ejecutable completamente funcional que se abre al hacer doble clic. ¡Puedes distribuir `./dist-standalone/SimAS.app` a otros usuarios de macOS!

## 🌍 Próximos Pasos: Multiplataforma

Para crear instaladores para Linux y Windows, necesitarás:

1. **Linux**: Ejecutar el proyecto en una máquina Linux
2. **Windows**: Ejecutar el proyecto en una máquina Windows
3. **Usar jpackage** para crear instaladores nativos (.deb, .exe)

¡El código está listo para ser compilado en cualquier plataforma! 🚀
