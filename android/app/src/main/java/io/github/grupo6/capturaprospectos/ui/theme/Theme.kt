package io.github.grupo6.capturaprospectos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = DuocYellow, onPrimary = DuocBlack,
    primaryContainer = DuocYellow, onPrimaryContainer = DuocBlack,
    secondary = DuocLightGray, onSecondary = DuocBlack,
    secondaryContainer = DuocGray, onSecondaryContainer = DuocWhite,
    background = androidx.compose.ui.graphics.Color(0xFF101B28), onBackground = DuocWhite,
    surface = androidx.compose.ui.graphics.Color(0xFF1B2A3B), onSurface = DuocWhite,
    surfaceVariant = G6Navy, onSurfaceVariant = DuocLightGray,
    outline = DuocLightGray,
)

private val LightColorScheme = lightColorScheme(
    primary = DuocYellow, onPrimary = DuocBlack,
    primaryContainer = DuocYellow, onPrimaryContainer = DuocBlack,
    secondary = DuocBlack, onSecondary = DuocWhite,
    secondaryContainer = DuocLightGray, onSecondaryContainer = DuocBlack,
    background = G6Paper, onBackground = G6Navy,
    surface = DuocWhite, onSurface = G6Navy,
    surfaceVariant = DuocLightGray, onSurfaceVariant = G6Navy,
    outline = DuocGray,
)

/** Tema fijo de referencia; fuentes nativas hasta verificar los recursos tipográficos. */
@Composable
fun CapturaProspectosG6Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)), content = content)
}
