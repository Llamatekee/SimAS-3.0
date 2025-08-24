# Funcionalidad de Informes PDF - SimAS 3.0

## Descripción

Se ha implementado una nueva funcionalidad en el Editor de SimAS que permite generar informes en formato PDF con toda la información de una gramática. Esta funcionalidad está disponible a través del botón "Informe" en la interfaz del editor.

## Características del Informe PDF

### Contenido del Informe

El informe PDF generado incluye:

1. **Portada profesional** con:
   - Logo de la aplicación SimAS
   - Título del informe
   - Nombre de la aplicación y versión
   - Información básica de la gramática
   - Fecha de generación

2. **Página de detalles** con:
   - Símbolo inicial de la gramática
   - Lista completa de símbolos no terminales
   - Lista completa de símbolos terminales
   - Todas las producciones numeradas
   - Información adicional (estado de validación, estadísticas)

### Elementos Diferenciadores

- **Logo de la aplicación** en cada página
- **Numeración de páginas** en la parte inferior
- **Diseño profesional** con colores corporativos
- **Tipografía mejorada** con diferentes tamaños y estilos
- **Información de la aplicación** en el pie de página
- **Fecha de generación** automática
- **Estadísticas de la gramática** (número de producciones, símbolos, etc.)

## Requisitos

Para generar un informe PDF, la gramática debe cumplir con los siguientes requisitos:

1. **Tener un nombre** asignado
2. **Estar validada** (estado = 1)
3. **Contener al menos**:
   - Un símbolo inicial
   - Al menos un símbolo no terminal
   - Al menos un símbolo terminal
   - Al menos una producción

## Cómo Usar

### Desde la Interfaz Gráfica

1. **Cargar o crear una gramática** en el Editor
2. **Validar la gramática** usando el botón "Validar"
3. **Hacer clic en el botón "Informe"**
4. **Seleccionar la ubicación** donde guardar el archivo PDF
5. **Confirmar** la generación del informe

### Manejo de Errores

El sistema maneja los siguientes casos de error:

- **Sin gramática cargada**: Muestra un mensaje de advertencia
- **Gramática no validada**: Solicita validar la gramática primero
- **Errores de generación**: Muestra el error específico
- **Cancelación del usuario**: No genera el archivo

## Archivos Modificados

### Código Java
- `src/editor/Editor.java`: Implementación del método `generarInformePDF()`
- `src/gramatica/Gramatica.java`: Mejora del método `generarInforme()`

### Recursos
- `src/messages_es.properties`: Nuevas claves de texto en español
- `src/messages_en.properties`: Nuevas claves de texto en inglés

## Dependencias

La funcionalidad utiliza las siguientes librerías:
- **iText PDF** (versión 5.5.13.3) para la generación de PDFs
- **JavaFX** para la interfaz de usuario
- **Fuentes Arial** incluidas en el proyecto

## Estructura del PDF

```
Página 1: Portada
├── Logo de SimAS
├── Título del informe
├── Información de la aplicación
├── Datos básicos de la gramática
└── Fecha de generación

Página 2: Detalles
├── Símbolo inicial
├── Símbolos no terminales
├── Símbolos terminales
├── Producciones numeradas
├── Información adicional
└── Pie de página con información de la app
```

## Personalización

El informe se puede personalizar modificando:

- **Colores**: Variables `colorPrincipal`, `colorSecundario`, `colorAcento`
- **Fuentes**: Tamaños y estilos en las variables `titulo`, `subtitulo`, etc.
- **Layout**: Márgenes y espaciado del documento
- **Contenido**: Agregar o quitar secciones según necesidades

## Notas Técnicas

- El informe se genera en formato A4
- Se incluye numeración automática de páginas
- El logo se escala al 35% del tamaño original
- Se utilizan fuentes embebidas para compatibilidad
- El archivo se guarda con codificación UTF-8

## Pruebas

Para probar la funcionalidad:

1. Ejecutar la aplicación SimAS
2. Crear una gramática válida
3. Validar la gramática
4. Generar el informe PDF
5. Verificar que el archivo se crea correctamente
6. Revisar el contenido del PDF generado

## Soporte

En caso de problemas con la generación de informes:

1. Verificar que la gramática esté validada
2. Comprobar que hay suficiente espacio en disco
3. Asegurar que las fuentes estén disponibles
4. Revisar los logs de error en la consola

