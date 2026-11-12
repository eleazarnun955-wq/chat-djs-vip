# CHAT DJ'S VIP

Aplicación Android independiente en español: asistente conversacional y Escuela DJ.

## Objetivos
- Interfaz móvil de chat con historial local.
- Escuela DJ: aprendizaje guiado sobre Serato DJ, rekordbox, VirtualDJ, Traktor, djay, controladoras, CDJ, mezcladoras y tornamesas.
- Arquitectura preparada para incorporar modelos locales y proveedores en nube opcionales, sin suscripción obligatoria.
- Las funciones externas y de IA en nube se habilitarán solo cuando exista un proveedor configurado; no se presentan simulaciones como integraciones reales.

## Construcción y entrega
- GitHub Actions compila `app-debug.apk` al actualizar `main` o al ejecutar manualmente el workflow `Android APK`.
- El workflow adjunta el APK como artefacto y también lo publica como archivo individual en GitHub Releases.
- En Android, descarga el archivo `.apk` desde la sección **Assets** de la versión publicada; no descargues el ZIP del artefacto de Actions.
- La versión 3.3.0 actualiza la instalación independiente 3.2.0 con el mismo paquete y firma, por lo que conserva los datos. Incluye el módulo inicial Estudio de Aplicaciones para generar especificaciones y código con Gemini; la compilación remota desde la app aún requiere integración adicional. La versión 3.2.0 usa `applicationId` independiente `com.djsebas.chatdjsvip.v320`, por lo que se instala como aplicación separada. Incluye historial local, respuestas por Gemini opcional, entrada por voz, análisis de imágenes, lectura en voz alta, compartir respuestas, Escuela DJ y biblioteca musical local.
