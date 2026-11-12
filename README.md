# CHAT DJ'S VIP

Aplicación Android independiente en español: asistente conversacional, Escuela DJ y Estudio de Aplicaciones.

## Funciones incluidas
- Chat con historial local, respuestas con Gemini opcional, entrada por voz, análisis de imágenes, lectura en voz alta, copiar y compartir respuestas.
- Escuela DJ con contenidos guiados sobre software, controladoras, CDJ, mezcladoras, tornamesas, teoría musical y preparación de eventos.
- Biblioteca musical local con controles de reproducción y cola interactiva; la cola conserva todas las canciones aunque se aplique una búsqueda o filtro de favoritas; la reproducción automática utiliza el estado actualizado de la cola y los controles al terminar cada pista; ahora se pueden añadir canciones manualmente a la cola desde la biblioteca, sin duplicarlas.
- Estudio de Aplicaciones para diseñar proyectos Android y web, generar código, conservar el contexto, continuar mejoras y guardar resultados.
- Importación segura de proyectos ZIP con lectura de archivos de texto y contexto de código para solicitar mejoras y correcciones sobre el proyecto existente. Límites de tamaño y filtrado de rutas para reducir riesgos al importar.
- Explorador y editor de archivos dentro de App Studio: seleccionar archivos importados, editar su contenido, crear archivos nuevos y eliminar archivos con confirmación; los cambios se guardan en el proyecto.
- Exportación del proyecto del Estudio de Aplicaciones como Markdown o ZIP; los archivos importados también pueden exportarse aunque aún no exista una respuesta de IA, incluyendo el contenido actual del editor. El ZIP reúne los archivos importados, creados y editados, el documento completo y los bloques de código detectados; aplica límites de tamaño y normaliza rutas para evitar entradas inseguras.
- Diagnóstico de preparación de proyectos Android: revisa Gradle, módulo app, manifiesto, código fuente y Gradle Wrapper, y sugiere el comando de compilación. Es una revisión estática; no ejecuta ni certifica una compilación.
- No se presentan como reales integraciones de búsqueda web, ejecución de código, compilación o publicación que todavía no estén conectadas.

## Construcción y entrega
- Versión vigente: **3.6.5**.
- El identificador de aplicación se conserva como `com.djsebas.chatdjsvip.v320`, con `versionCode` incremental y la firma configurada del workflow para actualizar la instalación existente.
- GitHub Actions ejecuta pruebas unitarias disponibles, Android Lint y compila `app-debug.apk` al actualizar `main` o al ejecutar manualmente el workflow `Android APK`. La etiqueta y el nombre del artefacto se obtienen automáticamente de `versionName`.
- El workflow adjunta el APK como artefacto y también lo publica como archivo individual en GitHub Releases.
- La compilación remota desde la pantalla Estudio de Aplicaciones y la ejecución de pruebas de los proyectos importados/generados todavía requieren integrar un entorno de construcción seguro. El diagnóstico actual solo valida estructura y archivos.
- La IA en nube requiere una clave de Google AI Studio configurada por el usuario y está sujeta a los límites y condiciones del proveedor.
