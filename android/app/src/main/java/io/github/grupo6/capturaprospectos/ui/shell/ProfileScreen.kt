package io.github.grupo6.capturaprospectos.ui.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import io.github.grupo6.capturaprospectos.data.local.DemoAccessFixtures
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEvent
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.ui.access.DemoDataState
import io.github.grupo6.capturaprospectos.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ProfileScreen(
    profile: DemoProfile,
    event: CaptureEvent,
    data: DemoDataState = DemoDataState(),
    onChangeEvent: () -> Unit,
    onLoadData: () -> Unit = {},
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    darkMode: Boolean = true,
    onDarkModeChange: (Boolean) -> Unit = {},
) {
    var photoSheet by remember { mutableStateOf(false) }
    val initials = profile.displayName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
    val roleLabel = if (profile.actor.role == io.github.grupo6.capturaprospectos.domain.capture.LocalRole.SUPERVISOR) "Supervisor" else "Captador"
    if (photoSheet) ModalBottomSheet(onDismissRequest = { photoSheet = false }) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(initials)
            Text("Cambiar foto", style = MaterialTheme.typography.titleLarge)
            listOf("Elegir de galería", "Tomar foto", "Eliminar foto").forEach { label ->
                OutlinedButton({}, enabled = false, modifier = Modifier.fillMaxWidth().semantics {
                    stateDescription = "Disponible en una entrega futura"
                }) { Text(label) }
            }
            TextButton({ photoSheet = false }) { Text("Cancelar") }
        }
    }
    LazyColumn(modifier, contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Mi perfil", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() }) }
        item {
            GlassCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(initials)
                    Text(profile.displayName, style = MaterialTheme.typography.headlineMedium)
                    if (profile.email.isNotBlank()) {
                        Text(
                            profile.email,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "Rol: $roleLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton({ photoSheet = true }, modifier = Modifier.fillMaxWidth()) { Text("Cambiar foto") }
                }
            }
        }
        item {
            GlassCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Apariencia", style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Modo oscuro", Modifier.weight(1f))
                        Switch(
                            darkMode,
                            onDarkModeChange,
                            modifier = Modifier.semantics { contentDescription = "Modo oscuro" },
                        )
                    }
                    Text(if (darkMode) "Oscuro" else "Claro", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            GlassCard {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Contexto de trabajo", style = MaterialTheme.typography.titleMedium)
                    Text(event.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onChangeEvent, enabled = !data.busy, modifier = Modifier.fillMaxWidth()) { Text("Cambiar evento") }
                }
            }
        }
        item {
            OutlinedButton(
                onLogout,
                enabled = !data.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Cerrar sesión") }
        }
    }
}
