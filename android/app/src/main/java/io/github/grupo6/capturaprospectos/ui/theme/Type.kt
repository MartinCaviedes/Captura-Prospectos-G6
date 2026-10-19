package io.github.grupo6.capturaprospectos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import io.github.grupo6.capturaprospectos.R

private val Manrope = FontFamily(Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold), Font(R.font.manrope_extrabold, FontWeight.ExtraBold))
private fun title(size: Int, line: Int, weight: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = Manrope, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)
private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)
val Typography = Typography(
    displaySmall = title(36,44,FontWeight.ExtraBold),
    headlineLarge = title(28,36), headlineMedium = title(28,36), headlineSmall = title(24,32),
    titleLarge = title(20,28), titleMedium = title(16,24,FontWeight.SemiBold),
    titleSmall = title(14,20,FontWeight.SemiBold),
    bodyLarge = body(16,24), bodyMedium = body(14,20), bodySmall = body(12,18),
    labelLarge = body(14,20,FontWeight.Medium), labelMedium = body(12,18,FontWeight.Medium),
    labelSmall = body(12,18),
)
