package io.github.grupo6.capturaprospectos.ui.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.disabled
import io.github.grupo6.capturaprospectos.AppScreen

/** Usa el ancho disponible de la ventana; funciona también con pantalla dividida. */
@Composable fun AppNavigation(selected: AppScreen, enabled: Boolean, onSelect: (AppScreen) -> Unit,
    modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    val items = listOf(AppScreen.HUB to "Inicio", AppScreen.TRAY to "Registros",
        AppScreen.SUMMARY to "Resumen", AppScreen.PROFILE to "Perfil")
    BoxWithConstraints(modifier) {
        val expanded = maxWidth >= 840.dp
        if (expanded) Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(176.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CAPTURA G6", style = MaterialTheme.typography.titleLarge)
                items.forEach { (route, label) ->
                    NavigationDrawerItem(label = { Text(label) }, selected = route == selected,
                        onClick = { if (enabled) onSelect(route) }, modifier = Modifier.fillMaxWidth().semantics { if (!enabled) disabled() },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = MaterialTheme.colorScheme.surface))
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                content(Modifier.widthIn(max = 1100.dp).fillMaxSize())
            }
        } else Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { content(Modifier.fillMaxSize()) }
            NavigationBar {
                items.forEach { (route, label) -> NavigationBarItem(selected = route == selected,
                    onClick = { onSelect(route) }, enabled = enabled,
                    icon = { Text(when (route) { AppScreen.HUB -> "⌂"; AppScreen.TRAY -> "≡"; AppScreen.SUMMARY -> "▥"; else -> "○" },
                        modifier = Modifier.clearAndSetSemantics { }) }, label = { Text(label) }) }
            }
        }
    }
}
