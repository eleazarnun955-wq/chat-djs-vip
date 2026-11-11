package com.djsebas.chatdjsvip

/** Guía local inicial de formación DJ; no sustituye manuales oficiales ni una IA conectada. */
fun djKnowledgeReply(input: String): String {
    val q = input.lowercase()
    return when {
        listOf("bpm","beatmatch","beatmatching","tempo","sincronizar").any { it in q } ->
            """BEATMATCHING Y BPM
1. Carga una canción en cada deck y revisa el BPM detectado; corrige el beatgrid si está desplazado.
2. Elige la pista guía y escucha su pulso por los audífonos.
3. Ajusta el pitch de la pista entrante hasta acercar los BPM.
4. Busca el primer golpe del compás y alinea los bombos con jog wheel o pitch bend.
5. Escucha varios compases y corrige suavemente si las pistas se separan.
6. Introduce la pista en una frase musical adecuada y mezcla con EQ.
Dime los BPM de ambas canciones, el programa y tu controlador para un ejercicio concreto."""
        listOf("ecual","eq","graves","medios","agudos","bass").any { it in q } ->
            """ECUALIZACIÓN
1. Mantén la ganancia en un nivel sano y evita el rojo.
2. Escucha qué frecuencias dominan en cada pista.
3. Al intercambiar canciones, evita mantener ambos graves fuertes al mismo tiempo.
4. Reduce el grave de la pista entrante y haz el cambio gradualmente.
5. Usa medios y agudos con moderación.
6. Comprueba el resultado en audífonos y bocinas a volumen moderado."""
        listOf("ganancia","gain","clipping","satur","rojo","volumen").any { it in q } ->
            """GANANCIA Y NIVELES
1. Reproduce la parte más fuerte de la canción.
2. Ajusta TRIM/GAIN para que el medidor no permanezca en rojo.
3. Controla el volumen general con Master.
4. Deja margen para transiciones.
5. Si distorsiona, revisa fuente, canal, master y amplificador; baja la etapa que satura.
No existe un valor universal: observa los medidores de tu equipo."""
        listOf("cue","preescucha","audifonos","auriculares","monitor").any { it in q } ->
            """PREESCUCHA (CUE)
1. Conecta audífonos a PHONES.
2. Pulsa CUE en el canal que quieres preparar.
3. Ajusta el nivel de audífonos a un volumen cómodo.
4. Escucha el inicio, ritmo y frase de la pista entrante.
5. Si existe control CUE/MIX, regula la mezcla entre canal y Master.
6. Si no escuchas, revisa la asignación de salidas y el botón CUE."""
        listOf("conectar","conexión","rca","xlr","salida","bocina","altavoz","phono","line").any { it in q } ->
            """CONEXIONES DE AUDIO
1. Identifica las salidas MASTER y las entradas de bocinas o amplificador.
2. Usa un cable compatible con ambos conectores (RCA, TRS o XLR).
3. En tornamesas, PHONO va a PHONO con tierra; LINE va a LINE.
4. Baja los niveles antes de conectar o desconectar.
5. Enciende fuentes primero y bocinas/amplificador al final; apaga en orden inverso.
6. Prueba a volumen bajo.
Dime los modelos exactos para preparar un esquema de conexión."""
        listOf("tornamesa","vinilo","aguja","cápsula","capsula","antiskating","contrapeso","plato").any { it in q } ->
            """TORNAMESA Y VINILO
1. Coloca el plato en una superficie firme y nivelada.
2. Monta cápsula y aguja según el fabricante.
3. Equilibra el brazo y ajusta la fuerza de apoyo al rango recomendado por esa cápsula; no adivines el peso.
4. Ajusta antiskating según el manual.
5. Comprueba PHONO/LINE y conecta a la entrada correspondiente; conecta tierra cuando se requiera.
6. Limpia el disco y practica colocar la aguja sin arrastrarla.
Necesito marca/modelo de tornamesa y cápsula para indicar valores correctos."""
        listOf("scratch","scratchear","scratching","baby scratch").any { it in q } ->
            """PRIMER EJERCICIO DE SCRATCH
1. Usa un disco apropiado y volumen bajo.
2. Localiza un sonido corto y repetible.
3. Con crossfader abierto, mueve el vinilo adelante y atrás lentamente.
4. Practica un movimiento uniforme sin forzar la aguja.
5. Después coordina el movimiento con el crossfader si tu mezcladora lo permite.
6. Haz sesiones cortas y revisa el ajuste del brazo y aguja."""
        listOf("frase","compas","compás","estructura","intro","outro","transición","transicion").any { it in q } ->
            """ESTRUCTURA Y TRANSICIONES
1. Cuenta golpes; mucha música dance agrupa cuatro tiempos por compás, aunque depende del género.
2. Escucha cambios cada 8, 16 o 32 compases.
3. Prepara la pista entrante en una sección compatible.
4. Alinea inicios de frase, no solamente BPM.
5. Haz el cambio de graves y niveles gradualmente.
6. Escucha la mezcla completa y evita choques de voces o melodías."""
        listOf("serato","rekordbox","virtualdj","traktor","djay").any { it in q } ->
            """SOFTWARE DJ
Puedo orientarte con Serato DJ, rekordbox, VirtualDJ, Traktor y djay.
1. Indica el nombre y versión del programa.
2. Dime el modelo de controladora o si trabajas solo con computadora.
3. Describe qué quieres hacer o qué error aparece.
4. Comparte el texto del error o una captura si puedes.
Los menús cambian entre versiones y licencias; no inventaré botones que quizá no existan."""
        listOf("cdj","controladora","mezcladora","mixer","controlador").any { it in q } ->
            """CONTROLADORA, CDJ O MEZCLADORA
1. Identifica modelo y compatibilidad con el software.
2. Instala el driver oficial si se requiere.
3. Conecta según las instrucciones del fabricante.
4. Selecciona dispositivo de audio y asigna Master y Headphones/Monitor.
5. Prueba decks, ganancia y preescucha antes de subir volumen.
6. Guarda copia de cualquier mapeo MIDI personalizado.
Dime marca y modelo para adaptar la guía."""
        listOf("loop","bucle","hot cue","hotcue","memory cue","punto cue").any { it in q } ->
            """HOT CUES Y LOOPS
1. Carga una pista y espera el análisis.
2. Comprueba que el beatgrid coincida con los golpes.
3. Coloca un Hot Cue en un punto musical útil.
4. Prueba loops de 4 u 8 compases y escucha el cierre.
5. Guarda los puntos y vuelve a cargar la pista para confirmar que se conservaron.
Los límites y nombres varían según software y equipo."""
        listOf("no suena", "sin sonido", "no escucho", "no sale audio").any { it in q } ->
            """DIAGNÓSTICO: NO HAY SONIDO
1. Baja Master y volumen de bocinas antes de revisar.
2. Comprueba que las bocinas estén conectadas a MASTER OUT.
3. En Audio Setup selecciona la interfaz/controladora correcta.
4. Verifica la asignación de salidas Master (por ejemplo 1/2, según el equipo).
5. Si solo falla la preescucha, revisa PHONES, CUE y Output Routing.
6. Prueba una pista local y sube el volumen gradualmente.
Dime software, modelo de equipo y qué medidores sí se mueven."""
        listOf("latencia", "retardo", "cortes", "chasquido", "audio entrecortado").any { it in q } ->
            """LATENCIA Y CORTES
1. Cierra programas que consuman CPU.
2. Usa el driver oficial del fabricante; en Windows, ASIO cuando esté disponible.
3. Aumenta el búfer poco a poco hasta eliminar los cortes.
4. Conecta la controladora directamente al ordenador y prueba otro cable/puerto.
5. Evita el ahorro de energía USB durante la sesión.
Dime sistema operativo, software, controlador y búfer actual."""
        listOf("usb", "memoria", "pendrive", "exportar canciones").any { it in q } ->
            """PREPARAR USB PARA DJ
1. Haz copia de seguridad: preparar/formatear una memoria puede borrar sus datos.
2. Confirma formato y capacidad admitidos por tu reproductor.
3. En rekordbox analiza pistas, revisa beatgrid, cues y playlists.
4. Exporta desde la función oficial del programa compatible.
5. Expulsa la memoria de forma segura y pruébala en el equipo del evento.
Dime modelo de CDJ/XDJ/Denon y software para darte pasos precisos."""
        listOf("grabar", "grabación", "grabacion", "record").any { it in q } ->
            """GRABAR UNA SESIÓN
1. Comprueba si tu software tiene REC o si la mezcladora ofrece salida de grabación.
2. Selecciona la fuente Master/Record correcta.
3. Ajusta niveles para que no lleguen al rojo.
4. Haz una prueba breve y escucha el archivo completo.
5. Guarda WAV para edición o MP3 para un archivo más ligero.
Dime software y modelo de mezcladora/controladora para indicarte dónde está cada opción."""
        listOf("serato").any { it in q } ->
            """SERATO DJ: CONFIGURACIÓN
1. Instala Serato y el driver oficial si tu controlador lo requiere.
2. Conecta el equipo y confirma que Serato lo reconozca.
3. En Setup revisa Audio y asignación Master/Headphones.
4. Importa música, analiza BPM y corrige beatgrid si hace falta.
5. Crea crates, coloca Hot Cues y prueba CUE en audífonos.
Dime versión de Serato y modelo exacto para adaptar los pasos."""
        listOf("rekordbox").any { it in q } ->
            """REKORDBOX: PREPARACIÓN
1. Importa música a Collection y analiza BPM/tonalidad.
2. Corrige beatgrid cuando no coincida con los golpes.
3. Organiza playlists y coloca Hot Cues/Memory Cues.
4. Para USB usa Export y espera a que termine la sincronización.
5. Expulsa de forma segura y prueba el USB en tu CDJ/XDJ.
Dime versión de rekordbox y modelo del reproductor."""
        listOf("virtualdj").any { it in q } ->
            """VIRTUALDJ: CONFIGURACIÓN
1. En Settings revisa Audio y selecciona el dispositivo correcto.
2. En Controllers confirma que aparezca tu controladora.
3. Asigna Master y Headphones según las salidas físicas disponibles.
4. Importa música y analiza BPM/Key.
5. Practica CUE, loops, Hot Cues y EQ; guarda copia antes de cambiar mapeos.
Dime versión y modelo de controladora."""
        listOf("traktor").any { it in q } ->
            """TRAKTOR: CONFIGURACIÓN
1. En Preferences > Audio Setup selecciona la interfaz.
2. Ajusta la latencia sin provocar cortes.
3. En Output Routing asigna Master y Monitor.
4. Importa música a Collection y revisa beatgrids.
5. Practica Hotcues, loops y FX; respalda mapeos MIDI antes de editarlos.
Dime versión de Traktor y modelo de interfaz/controladora."""
        listOf("djay").any { it in q } ->
            """DJAY: PRIMEROS PASOS
1. Comprueba la compatibilidad del controlador con tu versión y dispositivo.
2. Conéctalo y concede los permisos solicitados.
3. Configura Master y preescucha si tu dispositivo lo permite.
4. Carga pistas y revisa BPM, forma de onda y Cue.
5. Practica loops, EQ y transiciones. Neural Mix depende de versión y dispositivo.
Dime si usas Android, iPhone, iPad, Mac o Windows y el controlador."""
        else ->
            """Puedo ayudarte con mezcla, beatmatching, BPM, ecualización, ganancia, preescucha, loops, hot cues, software DJ, controladoras, CDJ, mezcladoras y tornamesas.
Escribe tu duda y agrega marca/modelo del equipo y versión del programa cuando aplique.
Esta es una guía local integrada, todavía no una IA conectada a internet ni un sustituto del manual oficial."""
    }
}