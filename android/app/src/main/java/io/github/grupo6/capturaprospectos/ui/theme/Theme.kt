package io.github.grupo6.capturaprospectos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = DuocYellow, onPrimary = DuocBlack,
    primaryContainer = G6Navy, onPrimaryContainer = DuocWhite,
    secondary = G6Cyan, onSecondary = DuocBlack,
    secondaryContainer = G6Navy, onSecondaryContainer = DuocWhite,
    tertiary = G6Cobalt, onTertiary = Color.White,
    tertiaryContainer = G6Navy, onTertiaryContainer = DuocWhite,
    background = DuocBlack, onBackground = DuocWhite,
    surface = G6Navy, onSurface = DuocWhite,
    surfaceVariant = G6Navy, onSurfaceVariant = DuocLightGray,
    surfaceContainer = G6Navy, surfaceContainerLow = G6Navy,
    surfaceContainerHigh = G6Navy, surfaceContainerHighest = G6Navy,
    outline = DuocLightGray, outlineVariant = Color(0xFF395169),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
)
private val LightColors = lightColorScheme(
    primary = G6Ink, onPrimary = Color.White,
    primaryContainer = G6Paper, onPrimaryContainer = G6Ink,
    secondary = G6Ink, onSecondary = Color.White,
    secondaryContainer = G6Paper, onSecondaryContainer = G6Ink,
    tertiary = G6Cobalt, onTertiary = Color.White,
    tertiaryContainer = G6Paper, onTertiaryContainer = G6Ink,
    background = G6Paper, onBackground = G6Ink,
    surface = Color.White, onSurface = G6Ink,
    surfaceVariant = G6Paper, onSurfaceVariant = DuocGray,
    surfaceContainer = Color.White, surfaceContainerLow = G6Paper,
    surfaceContainerHigh = Color.White, surfaceContainerHighest = G6Paper,
    outline = Color(0xFF8496AD), outlineVariant = Color(0xFFC4D0E3),
)

/** V3 fija la paleta institucional; el formulario usa su variante luminosa. */
@Composable
fun CapturaProspectosG6Theme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp)), content = content)
}
