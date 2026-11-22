package io.github.grupo6.capturaprospectos.ui.admission

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** Consulta opcional del lugar del captador; no representa el domicilio del prospecto. */
@Composable fun LocalityLookupCard(state:GeocodingState,enabled:Boolean,onLookup:()->Unit) {
    if(!state.available && state.message==null && state.label==null) return
    val uri=LocalUriHandler.current
    Column(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Localidad del punto de captura",style=MaterialTheme.typography.titleSmall)
        if(state.label!=null) {
            Text("Localidad aproximada: ${state.label}")
            Text("Fuente: BigDataCloud. No corresponde al domicilio del prospecto.",style=MaterialTheme.typography.bodySmall)
        } else if(state.available) {
            Text("Consulta opcional: se enviarán las coordenadas actuales de este dispositivo a BigDataCloud, que también recibirá tu IP para mejorar su geolocalización. No se envían datos del prospecto.",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={uri.openUri("https://www.bigdatacloud.com/privacy-and-cookie-policy")}) {
                Text("Privacidad de BigDataCloud")
            }
            OutlinedButton(onClick=onLookup,enabled=enabled && !state.busy) {
                Text(if(state.busy) "Consultando localidad…" else "Consultar localidad con BigDataCloud")
            }
        }
        if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let {
            Text(it,style=MaterialTheme.typography.bodySmall,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
        }
    }
}

@Composable fun StreetAddressLookupCard(state:GeocodingState,enabled:Boolean,onLookup:()->Unit) {
    if(!state.available && state.message==null && state.label==null) return
    Column(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("Dirección del punto de captura",style=MaterialTheme.typography.titleSmall)
        if(state.label!=null) {
            Text("Dirección aproximada: ${state.label}")
            Text("Fuente: servicio de geocodificación de Android. La calle o el número pueden corresponder a un punto cercano; confirma visualmente. No es el domicilio del prospecto.",style=MaterialTheme.typography.bodySmall)
        } else if(state.available) {
            Text("Consulta opcional al servicio de geocodificación del teléfono. Puede usar Internet y enviar estas coordenadas al proveedor configurado en Android. Se intentará obtener calle y número, si están disponibles.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick=onLookup,enabled=enabled && !state.busy) {
                Text(if(state.busy) "Consultando dirección…" else "Consultar dirección aproximada")
            }
        }
        if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let {Text(it,style=MaterialTheme.typography.bodySmall,modifier=Modifier.semantics {liveRegion=LiveRegionMode.Polite})}
    }
}
