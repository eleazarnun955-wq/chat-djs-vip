# CHAT DJ'S VIP

Aplicación Android con asistente universal local, Escuela DJ, Biblioteca musical, Estudio de Aplicaciones y DJ SEBAS PRODUCCIONES.

## Cerebro universal local
- Integra LiteRT-LM para ejecutar un modelo generativo directamente en el teléfono.
- No usa Gemini, OpenAI, claves API, servidores de inferencia ni permiso INTERNET.
- El modelo se obtiene una sola vez desde el navegador y se importa desde Archivos; queda guardado en el almacenamiento privado de la app y funciona sin conexión. Al volver a abrir la app, se recupera la ruta guardada y el motor vuelve a cargar el modelo automáticamente al enviar el primer mensaje; si el backend anterior falla, intenta el alternativo CPU/GPU.
- Modelo recomendado para esta compilación: LFM2.5 350M INT4, archivo `LFM2.5-350M_int4.litertlm` (aprox. 248 MB), publicado públicamente por LiteRT Community; no requiere aceptar condiciones de acceso de Gemma ni iniciar sesión para autorizar el modelo.
- El paquete INT4 actual usa pesos externos y requiere LiteRT-LM 0.19.0 o posterior; esta app integra 0.19.1. Para máxima estabilidad en teléfonos modestos, la app inicializa el modelo con CPU. El Moto G24 aún debe validarse en ejecución real.
- El SHA-256 oficial se usa como referencia, pero no se rechaza un archivo solo por tener otro hash: LiteRT-LM debe inicializar correctamente el contenido para aceptarlo. Así se permiten empaquetados compatibles con UUID/fecha diferentes y se siguen rechazando modelos realmente incompatibles.
- Licencia: LFM Open License v1.0. Revisar sus condiciones antes de distribuir comercialmente. El modelo es ligero y admite español, pero su ficha recomienda tareas generales, extracción y uso de herramientas; no ofrece la misma profundidad de conocimiento o programación que un modelo grande.
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
- Versión: 3.7.8.
- applicationId: com.djsebas.chatdjsvip.v320 (se conserva para actualizar la instalación existente).
- versionCode: 50.
- El workflow Android APK ejecuta pruebas unitarias, Android Lint y compila app-debug.apk.
- La aplicación no declara permiso INTERNET; el modelo se obtiene fuera de la app y luego se importa localmente.
- El Estudio de Aplicaciones realiza análisis estático local. La compilación y publicación remota de proyectos importados no están implementadas.
- Compilación 3.7.8: CI verifica pruebas unitarias, Android Lint, firma, alineación, applicationId, versionCode y versionName.
