# CHAT DJ'S VIP

Aplicación Android con asistente universal local, Escuela DJ, Biblioteca musical, Estudio de Aplicaciones y DJ SEBAS PRODUCCIONES.

## Cerebro universal local
- Integra LiteRT-LM para ejecutar un modelo generativo directamente en el teléfono.
- No usa Gemini, OpenAI, claves API, servidores de inferencia ni permiso INTERNET.
- El modelo se obtiene una sola vez desde el navegador y se importa desde Archivos; queda guardado en el almacenamiento privado de la app y funciona sin conexión.
- Modelo inicial: Qwen3-0.6B LiteRT-LM, variante dinámica INT4, archivo Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm (aprox. 328 MiB), licencia Apache-2.0.
- LiteRT-LM 0.19.1 es necesario para este archivo con pesos externos. La app intenta GPU/OpenCL primero para reducir presión de RAM y usa CPU como respaldo si GPU no está disponible.
- El modelo es pequeño para móviles: la calidad y velocidad son inferiores a modelos grandes y el consumo de RAM depende del teléfono. En dispositivos de 4 GB de RAM física puede quedar justo; los benchmarks de referencia de Qwen3-0.6B muestran alrededor de 1.9–2.1 GB de pico en GPU y hasta 2.7 GB en CPU con la variante anterior. La compatibilidad real debe validarse en el teléfono de destino.
- El chat conserva contexto reciente y usa instrucciones de asistente general en español, además de memoria local.
- El modelo inicial es solo texto; análisis directo de imágenes y audio con IA local queda pendiente de un modelo multimodal compatible.
- No consulta noticias, clima, precios ni resultados actuales porque el chat no tiene conexión de red.

### Instalar el modelo
1. En el chat, pulsa Obtener modelo y descarga el archivo .litertlm.
2. Pulsa Importar modelo y selecciona el archivo descargado.
3. Espera a que la app lo copie y cargue. La primera carga puede tardar varios segundos.
4. Después puedes conversar sin conexión. Mantén espacio libre suficiente para el archivo y el uso de memoria del modelo.

## Funciones de la aplicación
- Escuela DJ con contenidos sobre software, controladoras, CDJ, mezcladoras, tornamesas, teoría musical y preparación de eventos.
- Biblioteca musical local con reproducción, favoritos y cola persistente.
- Estudio de Aplicaciones con importación ZIP, explorador/editor de archivos, guardado de proyectos, diagnóstico estático y exportación ZIP/Markdown.
- DJ SEBAS PRODUCCIONES integrado: herramientas de audio, edición y exportación MP3 local.
- Las funciones de audio trabajan con archivos del dispositivo; la generación de música por IA no forma parte de esta versión.

## Construcción
- Versión: 3.6.9.
- applicationId: com.djsebas.chatdjsvip.v320 (se conserva para actualizar la instalación existente).
- versionCode: 41.
- El workflow Android APK ejecuta pruebas unitarias, Android Lint y compila app-debug.apk.
- La aplicación no declara permiso INTERNET; el modelo se obtiene fuera de la app y luego se importa localmente.
- El Estudio de Aplicaciones realiza análisis estático local. La compilación y publicación remota de proyectos importados no están implementadas.