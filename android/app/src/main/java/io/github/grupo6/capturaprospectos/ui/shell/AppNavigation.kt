package io.github.grupo6.capturaprospectos.ui.shell

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import io.github.grupo6.capturaprospectos.AppScreen
import io.github.grupo6.capturaprospectos.R
import io.github.grupo6.capturaprospectos.ui.components.*

/** El ancho de ventana decide el menú; el formulario conserva su confirmación de salida. */
@Composable fun AppNavigation(selected: AppScreen, enabled: Boolean, onSelect: (AppScreen) -> Unit,
    modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    val items=listOf(AppScreen.HUB to "Inicio",AppScreen.TRAY to "Registros",AppScreen.SUMMARY to "Resumen",AppScreen.PROFILE to "Perfil")
    fun resource(route: AppScreen)=when(route) {
        AppScreen.HUB -> if(route==selected) R.drawable.g6_home_frame1 else R.drawable.g6_records_frame1
        AppScreen.TRAY -> if(route==selected) R.drawable.g6_records_frame2 else R.drawable.g6_home_frame2
        AppScreen.SUMMARY -> if(route==selected) R.drawable.g6_summary_frame3 else R.drawable.g6_home_frame3
        else -> if(route==selected) R.drawable.g6_profile_frame3 else R.drawable.g6_home_frame
    }
    ConexionesBackground(modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val menuWidth = if (LocalDensity.current.fontScale > 1.2f) 224.dp else 176.dp
            if(maxWidth>=840.dp) Row(Modifier.fillMaxSize().padding(24.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                GlassCard(Modifier.width(menuWidth).fillMaxHeight()) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        ProductIdentity()
                        items.forEach { (route,label) ->
                            NavigationDrawerItem(label={Text(label)},icon={DesignIcon(resource(route))},selected=route==selected,
                                onClick={if(enabled) onSelect(route)},modifier=Modifier.fillMaxWidth().semantics { if(!enabled) disabled() },
                                colors=NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor=MaterialTheme.colorScheme.primary,selectedTextColor=MaterialTheme.colorScheme.onPrimary,
                                    unselectedContainerColor=Color.Transparent))
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight(),contentAlignment=Alignment.TopCenter) {
                    content(Modifier.widthIn(max=1100.dp).fillMaxSize())
                }
            } else Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { content(Modifier.fillMaxSize()) }
                GlassCard(Modifier.padding(start=16.dp,end=16.dp,top=6.dp,bottom=12.dp).fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().selectableGroup().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        items.forEach { (route,label) ->
                            Surface(Modifier.weight(1f).selectable(selected=route==selected,enabled=enabled,role=Role.Tab,
                                onClick={onSelect(route)}),shape=MaterialTheme.shapes.medium,
                                color=if(route==selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor=if(route==selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) {
                                Column(Modifier.heightIn(min=64.dp).padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally,
                                    verticalArrangement=Arrangement.spacedBy(8.dp,Alignment.CenterVertically)) {
                                    DesignIcon(resource(route));Text(label,style=MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
