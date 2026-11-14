package com.djsebas.chatdjsvip

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val LOCAL_MEMORY_PREFS = "chat_djs_vip_local_brain"
private const val LOCAL_MEMORY_KEY = "facts"

internal fun parseLocalMemoryFacts(raw: String?): List<String> =
    raw.orEmpty().lineSequence().map(String::trim).filter(String::isNotBlank).distinct().takeLast(40)

internal fun addLocalMemoryFact(raw: String?, fact: String): String {
    val clean = fact.trim()
    if (clean.isBlank()) return parseLocalMemoryFacts(raw).joinToString("\n")
    return (parseLocalMemoryFacts(raw) + clean).distinctBy { it.lowercase() }.takeLast(40).joinToString("\n")
}

internal fun removeLocalMemoryFact(raw: String?, index: Int): String =
    parseLocalMemoryFacts(raw).filterIndexed { i, _ -> i != index }.joinToString("\n")

@Composable
fun LocalMemoryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(LOCAL_MEMORY_PREFS, Context.MODE_PRIVATE) }
    var facts by remember { mutableStateOf(parseLocalMemoryFacts(prefs.getString(LOCAL_MEMORY_KEY, ""))) }
    var draft by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Memoria privada del teléfono") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Estos datos se guardan localmente y el asistente puede usarlos como contexto. No se envían a servidores.")
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dato que quieres recordar") },
                    placeholder = { Text("Ej. Prefiero respuestas en español") },
                    minLines = 2
                )
                Button(
                    onClick = {
                        val updated = addLocalMemoryFact(facts.joinToString("\n"), draft)
                        prefs.edit().putString(LOCAL_MEMORY_KEY, updated).apply()
                        facts = parseLocalMemoryFacts(updated)
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Guardar en memoria") }
                if (facts.isEmpty()) Text("Todavía no hay datos guardados.")
                else {
                    Text("Datos guardados: ${facts.size}/40", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        itemsIndexed(facts) { index, fact ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(fact, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    val updated = removeLocalMemoryFact(facts.joinToString("\n"), index)
                                    prefs.edit().putString(LOCAL_MEMORY_KEY, updated).apply()
                                    facts = parseLocalMemoryFacts(updated)
                                }) { Icon(Icons.Default.Delete, contentDescription = "Eliminar dato") }
                            }
                            HorizontalDivider()
                        }
                    }
                    TextButton(onClick = { confirmClear = true }) { Text("Borrar toda la memoria") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Borrar toda la memoria") },
            text = { Text("Se eliminarán los datos guardados, pero no tus conversaciones.") },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().remove(LOCAL_MEMORY_KEY).apply()
                    facts = emptyList()
                    confirmClear = false
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } }
        )
    }
}
