# CHAT DJ'S VIP

Aplicación Android independiente en español: asistente conversacional, Escuela DJ y Estudio de Aplicaciones.

## Funciones incluidas
- Chat con historial local, respuestas con Gemini opcional, entrada por voz, análisis de imágenes, lectura en voz alta, copiar y compartir respuestas.
- Escuela DJ con contenidos guiados sobre software, controladoras, CDJ, mezcladoras, tornamesas, teoría musical y preparación de eventos.
- Biblioteca musical local.
- Estudio de Aplicaciones para diseñar proyectos Android y web, generar código, conservar el contexto, continuar mejoras y guardar resultados.
- Importación segura de proyectos ZIP con lectura de archivos de texto y contexto de código para solicitar mejoras y correcciones sobre el proyecto existente. Límites de tamaño y filtrado de rutas para reducir riesgos al importar.
- Explorador y editor de archivos dentro de App Studio: seleccionar archivos importados, editar su contenido, crear archivos nuevos y eliminar archivos con confirmación; los cambios se guardan en el proyecto.
- Exportación del resultado del Estudio de Aplicaciones como Markdown o ZIP. El ZIP incluye el documento completo y los bloques de código detectados, usando rutas declaradas por la IA cuando están disponibles.
- No se presentan como reales integraciones de búsqueda web, ejecución de código, compilación o publicación que todavía no estén conectadas.

## Construcción y entrega
- GitHub Actions ejecuta pruebas unitarias disponibles, Android Lint y compila `app-debug.apk` al actualizar `main` o al ejecutar manualmente el workflow `Android APK`. La etiqueta y el nombre del artefacto se obtienen automáticamente de `versionName`.
- El workflow adjunta el APK como artefacto y también lo publica como archivo individual en GitHub Releases.
- La versión 3.4.2 conserva `applicationId` `com.djsebas.chatdjsvip.v320`, `versionCode` incremental y la firma configurada del workflow para actualizar la instalación 3.2.0/3.3.x sin reemplazar otras aplicaciones.
- La compilación remota desde la pantalla Estudio de Aplicaciones, la ejecución de pruebas de los proyectos generados y la publicación automática todavía requieren integrar un entorno de construcción seguro.
- La IA en nube requiere una clave de Google AI Studio configurada por el usuario y está sujeta a los límites y condiciones del proveedor.
