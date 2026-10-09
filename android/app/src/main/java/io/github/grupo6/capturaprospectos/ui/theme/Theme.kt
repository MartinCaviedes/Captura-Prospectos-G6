package io.github.grupo6.capturaprospectos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DuocYellow, onPrimary = DuocBlack,
    primaryContainer = DuocYellow, onPrimaryContainer = DuocBlack,
    secondary = DuocLightGray, onSecondary = DuocBlack,
    secondaryContainer = DuocGray, onSecondaryContainer = DuocWhite,
    background = DuocBlack, onBackground = DuocWhite,
    surface = DuocBlack, onSurface = DuocWhite,
    surfaceVariant = DuocBlack, onSurfaceVariant = DuocLightGray,
    outline = DuocLightGray,
)

private val LightColorScheme = lightColorScheme(
    primary = DuocYellow, onPrimary = DuocBlack,
    primaryContainer = DuocYellow, onPrimaryContainer = DuocBlack,
    secondary = DuocBlack, onSecondary = DuocWhite,
    secondaryContainer = DuocLightGray, onSecondaryContainer = DuocBlack,
    background = DuocWhite, onBackground = DuocBlack,
    surface = DuocWhite, onSurface = DuocBlack,
    surfaceVariant = DuocLightGray, onSurfaceVariant = DuocBlack,
    outline = DuocGray,
)

/** Tema fijo de referencia; fuentes nativas hasta verificar los recursos tipográficos. */
@Composable
fun CapturaProspectosG6Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography, content = content)
}
