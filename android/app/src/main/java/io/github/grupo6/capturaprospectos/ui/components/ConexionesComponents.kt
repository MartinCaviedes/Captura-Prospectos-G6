package io.github.grupo6.capturaprospectos.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import io.github.grupo6.capturaprospectos.R
import io.github.grupo6.capturaprospectos.ui.theme.*

/** Luz y superficies sin aplicar blur al texto ni exigir APIs exclusivas de Android 12. */
@Composable fun ConexionesBackground(modifier: Modifier = Modifier, light: Boolean = !LocalG6DarkTheme.current,
    content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(modifier.background(MaterialTheme.colorScheme.background)) {
        val expanded = maxWidth >= 840.dp
        Image(painterResource(if (expanded && light) R.drawable.g6_tablet_capture_identity_ambient_light else if (expanded) R.drawable.g6_tablet_home_identity_ambient_light else if (light) R.drawable.g6_capture_identity_ambient_light else R.drawable.g6_welcome_identity_ambient_light),
            contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.matchParentSize())
        content()
    }
}

@Composable fun GlassCard(modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(6.dp),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = shape, border = border, elevation = elevation,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = colors.contentColor)) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
            colors.containerColor.copy(alpha = .94f), colors.containerColor.copy(alpha = .86f)))),
            content = content)
    }
}

@Composable fun DesignIcon(@DrawableRes resource: Int, modifier: Modifier = Modifier, description: String? = null) {
    Image(painterResource(resource), description, modifier.size(24.dp), contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(LocalContentColor.current))
}

/** La marca institucional conserva su trazado y área propia, separada del símbolo de la app. */
@Composable fun ProductIdentity(modifier: Modifier = Modifier) {
    Column(modifier.semantics { contentDescription = "Captura de Prospectos · Grupo 6" },
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.widthIn(max = 220.dp).fillMaxWidth().background(DuocBlack, MaterialTheme.shapes.small)
            .padding(horizontal = 14.dp, vertical = 12.dp)) {
            Image(painterResource(R.drawable.duoc_uc_oficial), "Duoc UC",
                Modifier.fillMaxWidth().height(42.dp), contentScale = ContentScale.Fit)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(R.drawable.g6_capture_mark), null,
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f)) {
                Text("CAPTURA", style = MaterialTheme.typography.labelLarge)
                Text("DE PROSPECTOS", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable fun MetricTile(value: String, label: String, modifier: Modifier = Modifier) {
    GlassCard(modifier.semantics(mergeDescendants = true) {}) {
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(value,style=MaterialTheme.typography.headlineMedium)
            Text(label,style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable fun Avatar(initials: String, modifier: Modifier = Modifier) {
    Box(modifier.size(88.dp).clip(androidx.compose.foundation.shape.CircleShape).background(G6Cobalt),
        contentAlignment=Alignment.Center) {
        Text(initials,style=MaterialTheme.typography.headlineMedium,color=Color.White,
            modifier=Modifier.clearAndSetSemantics {})
    }
}

@Composable fun PrimaryActionButton(onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier, enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = DuocYellow, contentColor = DuocBlack), content = content)
}

