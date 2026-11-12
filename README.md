# CHAT DJ'S VIP

Aplicación Android en español con chat local, Escuela DJ, Biblioteca musical, Estudio de Aplicaciones y DJ SEBAS PRODUCCIONES.

## Cerebro local y privacidad
- El chat funciona con un motor local de reglas, clasificación de intenciones, conocimiento integrado, contexto reciente y memoria explícita en el dispositivo.
- No usa Gemini, claves API, proveedores externos ni conexión a internet.
- Comandos de memoria: «Recuerda: ...», «¿Qué recuerdas?», «Busca en memoria: ...» y «Olvida memoria».
- El motor local ofrece guías DJ, conceptos técnicos frecuentes y análisis estructurado.
- No es un modelo lingüístico generativo (LLM): cuando no tiene conocimiento local suficiente, lo indica en vez de inventar. No sustituye todas las capacidades de ChatGPT.
- La selección de imágenes no se envía a servidores; el análisis visual offline aún no está implementado.

## Funciones de la aplicación
- Escuela DJ con contenidos sobre software, controladoras, CDJ, mezcladoras, tornamesas, teoría musical y preparación de eventos.
- Biblioteca musical local con reproducción, favoritos y cola persistente.
- Estudio de Aplicaciones con importación ZIP, explorador/editor de archivos, guardado de proyectos, diagnóstico estático y exportación ZIP/Markdown.
- DJ SEBAS PRODUCCIONES integrado: herramientas de audio, edición y exportación MP3 local.
- Las funciones de audio trabajan con archivos del dispositivo; la generación de música por IA no forma parte de esta versión.

## Construcción
- Versión: **3.6.8**.
- applicationId: com.djsebas.chatdjsvip.v320 (se conserva para actualizar la instalación existente de la rama 3.6.7).
- versionCode: 40.
- El workflow Android APK ejecuta pruebas unitarias, Android Lint y compila app-debug.apk.
- La aplicación no declara permiso INTERNET; el chat no puede realizar solicitudes de red.
- El Estudio de Aplicaciones realiza análisis estático local. La compilación y publicación remota de proyectos importados no están implementadas.
