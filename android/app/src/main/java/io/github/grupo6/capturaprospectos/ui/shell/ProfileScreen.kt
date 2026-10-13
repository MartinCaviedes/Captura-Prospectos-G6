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
@Composable fun ProfileScreen(profile: DemoProfile,event: CaptureEvent,data: DemoDataState,
    onChangeEvent: ()->Unit,onLoadData: ()->Unit,onLogout: ()->Unit,modifier: Modifier=Modifier) {
    var confirmLoad by remember { mutableStateOf(false) }
    var photoSheet by remember { mutableStateOf(false) }
    val initials=profile.displayName.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
    if(photoSheet) ModalBottomSheet(onDismissRequest={photoSheet=false}) {
        Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Avatar(initials)
            Text("Cambiar foto",style=MaterialTheme.typography.titleLarge)
            listOf("Elegir de galería","Tomar foto","Eliminar foto").forEach { label ->
                OutlinedButton({},enabled=false,modifier=Modifier.fillMaxWidth().semantics {
                    stateDescription="Disponible en una entrega futura"
                }) { Text(label) }
            }
            TextButton({photoSheet=false}) { Text("Cancelar") }
        }
    }
    if(confirmLoad) AlertDialog(onDismissRequest={confirmLoad=false},title={Text("¿Preparar 24 prospectos ficticios?")},
        text={Text("Se guardan en el evento demo seleccionado con consentimiento ficticio y sin ubicación. No se borran registros previos. Repetir la carga no duplica inscripciones activas con el mismo RUT.")},
        confirmButton={TextButton({confirmLoad=false;onLoadData()}) { Text("Crear datos ficticios") }},
        dismissButton={TextButton({confirmLoad=false}) { Text("Cancelar") }})
    LazyColumn(modifier,contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Text("Mi perfil",style=MaterialTheme.typography.headlineMedium,modifier=Modifier.semantics { heading() }) }
        item { GlassCard {
            Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Avatar(initials)
                Text(profile.displayName,style=MaterialTheme.typography.headlineMedium)
                Text(DemoAccessFixtures.accounts.firstOrNull { it.actorId==profile.actor.id }?.email.orEmpty(),
                    style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton({photoSheet=true},modifier=Modifier.fillMaxWidth()) { Text("Cambiar foto") }
            }
        } }
        item { GlassCard {
            Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Contexto de trabajo",style=MaterialTheme.typography.titleMedium)
                Text(event.name,color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onChangeEvent,enabled=!data.busy,modifier=Modifier.fillMaxWidth()) { Text("Cambiar evento") }
            }
        } }
        item { GlassCard {
            Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Datos de evaluación",style=MaterialTheme.typography.titleMedium)
                Text("Perfiles e inscripciones ficticios para explorar tus registros y gráficos.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(DemoAccessFixtures.canLoad(profile.actor.id,event.id)) Button({confirmLoad=true},enabled=!data.busy) { Text("Preparar datos ficticios") }
                else Text("La carga se realiza desde un captador en su evento original.")
                if(data.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                data.message?.let { Text(it,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) }
            }
        } }
        item { OutlinedButton(onLogout,enabled=!data.busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("Cerrar sesión") } }
    }
}
