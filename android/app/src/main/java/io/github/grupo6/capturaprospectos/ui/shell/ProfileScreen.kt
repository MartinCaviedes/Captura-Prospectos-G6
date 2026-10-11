package io.github.grupo6.capturaprospectos.ui.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import io.github.grupo6.capturaprospectos.data.local.DemoAccessFixtures
import io.github.grupo6.capturaprospectos.domain.capture.CaptureEvent
import io.github.grupo6.capturaprospectos.domain.context.DemoProfile
import io.github.grupo6.capturaprospectos.ui.access.DemoDataState

@Composable fun ProfileScreen(profile: DemoProfile, event: CaptureEvent, data: DemoDataState,
    onChangeEvent: () -> Unit, onLoadData: () -> Unit, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    var confirmLoad by remember { mutableStateOf(false) }
    if (confirmLoad) AlertDialog(onDismissRequest = { confirmLoad = false },
        title = { Text("¿Preparar 24 prospectos ficticios?") },
        text = { Text("Se guardan en el evento demo seleccionado con consentimiento ficticio y sin ubicación. No se borran registros previos. Repetir la carga no duplica inscripciones activas con el mismo RUT.") },
        confirmButton = { TextButton({ confirmLoad = false; onLoadData() }) { Text("Crear datos ficticios") } },
        dismissButton = { TextButton({ confirmLoad = false }) { Text("Cancelar") } })
    LazyColumn(modifier, contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Mi perfil", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() }) }
        item { Card { Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
            Text(DemoAccessFixtures.accounts.firstOrNull { it.actorId == profile.actor.id }?.email.orEmpty())
            Text("Perfil ficticio · ${if (profile.actor.role.name == "CAPTOR") "Captador" else "Supervisor"}")
            Text("Acceso local de evaluación; no acredita identidad institucional.")
        } } }
        item { Card { Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Contexto de trabajo", style = MaterialTheme.typography.titleMedium)
            Text(event.name)
            Text("Cambiar de evento o salir conserva los registros guardados.")
            OutlinedButton(onChangeEvent, enabled = !data.busy) { Text("Cambiar evento") }
        } } }
        item { Card { Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Datos de evaluación", style = MaterialTheme.typography.titleMedium)
            Text("Son datos inventados para explorar registros y gráficos. No representan postulaciones ni aceptación institucional.")
            if (DemoAccessFixtures.canLoad(profile.actor.id, event.id)) Button({ confirmLoad = true }, enabled = !data.busy) { Text("Preparar datos ficticios") }
            else Text("Carga los datos desde un captador en su evento original demo. El supervisor puede consultar los registros autorizados después.")
            if (data.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            data.message?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        } } }
        item { OutlinedButton(onLogout, enabled = !data.busy, modifier = Modifier.fillMaxWidth()) { Text("Salir del acceso demo") } }
    }
}
