package io.github.grupo6.capturaprospectos.ui.context

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.domain.context.*
import io.github.grupo6.capturaprospectos.domain.capture.LocalRole

/** Ambos puntos de entrada consumen el mismo editor retenido por DemoContextViewModel. */
@Composable fun EventEditorRoute(vm:DemoContextViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val input=state.eventEditor ?: return
    val snapshot=(state.context as? DemoContextState.Ready)?.snapshot
    val actor=snapshot?.currentProfile?.actor
    val sites=snapshot?.catalog?.sites?.filter { it.id in actor?.siteIds.orEmpty() }.orEmpty()
    val captors=snapshot?.profiles?.filter { it.actor.role==LocalRole.CAPTOR }.orEmpty()
    Dialog(onDismissRequest=vm::dismissEventEditor,properties=DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=!state.busy,dismissOnClickOutside=!state.busy)) {
        Surface(Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.95f),shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(if(input.eventId==null) "Nuevo evento local" else "Editar evento local",style=MaterialTheme.typography.titleLarge)
                Text("Datos ficticios del dispositivo. Los registros anteriores conservan su contexto.")
                if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.message?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                fun edit(change:(EventEditorInput)->EventEditorInput) = vm.eventInput(change)
                @Composable fun field(label:String,value:String,change:(String)->EventEditorInput) {
                    OutlinedTextField(value,{ v -> edit { change(v) } },label={Text(label)},enabled=!state.busy,modifier=Modifier.fillMaxWidth())
                }
                field("Nombre del evento",input.name) { input.copy(name=it) }
                Text("Sede autorizada")
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { sites.forEach { site ->
                    FilterChip(input.siteId==site.id,{edit { it.copy(siteId=site.id) }},label={Text(site.name)},enabled=!state.busy)
                } }
                field("Inicio con offset",input.startsAt) { input.copy(startsAt=it) }
                field("Término con offset",input.endsAt) { input.copy(endsAt=it) }
                Text("Formato: 2026-10-12T09:00-03:00. Puedes programar varios días.")
                field("Zona horaria",input.timeZone) { input.copy(timeZone=it) }
                field("Latitud declarada (opcional)",input.latitude) { input.copy(latitude=it) }
                field("Longitud declarada (opcional)",input.longitude) { input.copy(longitude=it) }
                field("Colegio o institución",input.establishment) { input.copy(establishment=it) }
                field("Comuna del colegio",input.commune) { input.copy(commune=it) }
                Text("Captadores: sólo pueden asignarse a sus sedes autorizadas.")
                captors.forEach { captor ->
                    val selected=captor.actor.id in input.captorIds
                    val allowed=input.siteId in captor.actor.siteIds
                    FilterChip(selected,{edit { it.copy(captorIds=if(selected) it.captorIds-captor.actor.id else it.captorIds+captor.actor.id) }},
                        label={Text(captor.displayName + if(allowed) "" else " · Fuera de sede")},enabled=!state.busy && (allowed || selected))
                }
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(vm::dismissEventEditor,enabled=!state.busy) { Text("Cancelar edición de evento") }
                    Button({vm.saveEventEditor()},enabled=!state.busy) { Text("Guardar evento") }
                }
            }
        }
    }
    if(state.confirmEventDiscard) AlertDialog(onDismissRequest={vm.confirmEventDiscard(false)},title={Text("¿Descartar cambios del evento?")},
        text={Text("Los cambios del editor se quitarán de memoria.")},
        confirmButton={TextButton({vm.confirmEventDiscard(true)},enabled=!state.busy) { Text("Descartar evento") }},
        dismissButton={TextButton({vm.confirmEventDiscard(false)},enabled=!state.busy) { Text("Conservar evento") }})
}
