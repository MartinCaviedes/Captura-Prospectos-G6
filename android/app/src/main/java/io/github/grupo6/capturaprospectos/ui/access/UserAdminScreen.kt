package io.github.grupo6.capturaprospectos.ui.access

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.grupo6.capturaprospectos.data.remote.UserSummaryDto
import io.github.grupo6.capturaprospectos.ui.components.*

@Composable fun UserAdminRoute(model: UserAdminViewModel, account: UserSummaryDto,
    onLogout: () -> Unit, onSessionError: (String) -> Unit, modifier: Modifier = Modifier,onOpenProfile:(()->Unit)?=null) {
    val state by model.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(account.id) { model.openFor(account.id) }
    LaunchedEffect(state.sessionError) { state.sessionError?.let(onSessionError) }
    BackHandler(state.selected != null) { model.close() }
    UserAdminScreen(state, account.displayName, model::load, model::select, model::close,
        model::role, model::status, model::toggleSite, model::save, onLogout, modifier,onOpenProfile)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun UserAdminScreen(state: UserAdminState, adminName: String,
    onLoad: (String, Int) -> Unit, onSelect: (UserSummaryDto) -> Unit, onClose: () -> Unit,
    onRole: (String) -> Unit, onStatus: (String) -> Unit, onSite: (String) -> Unit,
    onSave: () -> Unit, onLogout: () -> Unit, modifier: Modifier = Modifier,onOpenProfile:(()->Unit)?=null) {
    var confirm by remember(state.selected?.id) { mutableStateOf(false) }
    ConexionesBackground(modifier) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProductIdentity()
            Text("Administración de usuarios", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text("$adminName · Administrador")
            onOpenProfile?.let {OutlinedButton(it,enabled=!state.busy) {Text("Mi perfil")}}
            OutlinedButton(onLogout, enabled = !state.busy) { Text("Cerrar sesión") }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            state.message?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            val user = state.selected
            if (user == null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PENDING" to "Pendientes", "ACTIVE" to "Activas", "DISABLED" to "Desactivadas").forEach { (status, label) ->
                        FilterChip(state.filter == status, { onLoad(status, 0) }, enabled = !state.busy, label = { Text(label) })
                    }
                }
                OutlinedButton({ onLoad(state.filter, state.page) }, enabled = !state.busy) { Text("Actualizar listado") }
                Text("${state.total} cuentas · Página ${state.page + 1}")
                if (state.users.isEmpty() && !state.busy) Text("No hay cuentas en este estado.")
                state.users.forEach { item ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(item.email)
                            Text(roleLabel(item.role))
                            OutlinedButton({ onSelect(item) }, enabled = !state.busy) { Text("Revisar cuenta") }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton({ onLoad(state.filter, state.page - 1) }, enabled = !state.busy && state.page > 0) { Text("Anterior") }
                    OutlinedButton({ onLoad(state.filter, state.page + 1) }, enabled = !state.busy && (state.page + 1) * 20L < state.total) { Text("Siguiente") }
                }
            } else {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(user.displayName, style = MaterialTheme.typography.titleLarge)
                        Text(user.email)
                        if (user.role == "ADMIN") Text("Esta cuenta administradora está protegida.")
                        else {
                            Text("Rol")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("CAPTADOR", "SUPERVISOR").forEach { role ->
                                    FilterChip(state.role == role, { onRole(role) }, enabled = !state.busy, label = { Text(roleLabel(role)) })
                                }
                            }
                            Text("Sedes asignadas")
                            state.sites.forEach { site ->
                                Row { Checkbox(site.id in state.assigned, { onSite(site.id) }, enabled = !state.busy); Text(site.name, Modifier.padding(top = 12.dp)) }
                            }
                            Text("Acceso")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(state.status == "ACTIVE", { onStatus("ACTIVE") }, enabled = !state.busy, label = { Text("Habilitar") })
                                FilterChip(state.status == "DISABLED", { onStatus("DISABLED") }, enabled = !state.busy, label = { Text("Desactivar") })
                            }
                            Text("Los cambios no invalidan inmediatamente los tokens ya emitidos.", style = MaterialTheme.typography.bodySmall)
                            Button({ confirm = true }, enabled = !state.busy) { Text("Guardar permisos") }
                        }
                        TextButton(onClose, enabled = !state.busy) { Text("Volver al listado") }
                    }
                }
            }
        }
        if (confirm && state.selected != null) AlertDialog(
            onDismissRequest = { confirm = false }, title = { Text("Confirmar permisos") },
            text = { Text("Se guardará el acceso ${if (state.status == "ACTIVE") "habilitado" else "desactivado"} de esta cuenta con el rol y las sedes seleccionadas.") },
            confirmButton = { TextButton({ confirm = false; onSave() }) { Text("Confirmar") } },
            dismissButton = { TextButton({ confirm = false }) { Text("Cancelar") } })
    }
}
private fun roleLabel(role: String?) = when (role) { "ADMIN" -> "Administrador"; "SUPERVISOR" -> "Supervisor"; "CAPTADOR" -> "Captador"; else -> "Sin permisos asignados" }
