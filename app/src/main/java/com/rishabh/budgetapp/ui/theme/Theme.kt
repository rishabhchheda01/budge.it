package com.rishabh.budgetapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.rishabh.budgetapp.R
import com.rishabh.budgetapp.ThemeMode

// ---------------------------------------------------------------------------
// This file is the ONE place to change how the app AND the widget look.
// Change a hex value (0xFF + six digits) and every screen follows.
// ---------------------------------------------------------------------------

// The font used for the logo and the big number.
val KdamThmorPro = FontFamily(Font(R.font.kdam_thmor_pro))

// ---- The core palette (the widget uses these too) ----
val LightBackground = Color(0xFFF4F4F2)   // screen background
val DarkBackground = Color(0xFF14161A)    // slate
val LightText = Color(0xFF141414)         // main text, and the Add button
val DarkText = Color(0xFFECEDEE)
val LightQuietText = Color(0xFF77777A)    // quieter text (labels, times)
val DarkQuietText = Color(0xFF8B909A)
val LightTrack = Color(0xFFE2E2DE)        // thin divider lines and the empty part of the progress bar
val DarkTrack = Color(0xFF2B2F36)

// ---- Widget: always sand with black text, in both light and dark mode ----
val WidgetBackground = Color(0xFFECD9BA)   // sand
val WidgetText = Color(0xFF111111)         // the number and the "+" button
val WidgetQuietText = Color(0xFF5E5340)    // the small "remaining" label
val WidgetTrack = Color(0xFFF7EFDF)        // empty part of the progress bar (a lighter cream)

// ---- Light mode: off-white and near-black ----
private val LightColors = lightColorScheme(
    background = LightBackground,
    surface = LightBackground,
    surfaceContainerHigh = Color(0xFFFAFAF8), // pop-up cards
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = LightQuietText,
    primary = LightText,                      // the Add button and selected options
    onPrimary = LightBackground,              // text on the Add button
    outline = Color(0xFFC9C9C5),              // outlines of text fields
    outlineVariant = LightTrack,
    inverseSurface = LightText,               // the "deleted / Undo" bar
    inverseOnSurface = LightBackground,
    inversePrimary = Color(0xFFC9C9C5)
)

// ---- Dark mode: slate and off-white ----
private val DarkColors = darkColorScheme(
    background = DarkBackground,
    surface = DarkBackground,
    surfaceContainerHigh = Color(0xFF1C1F24),
    onBackground = DarkText,
    onSurface = DarkText,
    onSurfaceVariant = DarkQuietText,
    primary = DarkText,
    onPrimary = DarkBackground,
    outline = Color(0xFF3A3F47),
    outlineVariant = DarkTrack,
    inverseSurface = DarkText,
    inverseOnSurface = DarkBackground,
    inversePrimary = Color(0xFF3A3F47)
)

// Corner roundness. Bigger number = softer.
private val BudgetShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// ---- Progress bar: the only color in the app ----
private val ProgressGreen = Color(0xFF5BAA6E)
private val ProgressAmber = Color(0xFFE0A93B)
private val ProgressRed = Color(0xFFD9534F)

// How the bar changes color as the budget is used (0.0 = nothing spent, 1.0 = fully spent).
private const val GREEN_UNTIL = 0.4f   // stays green up to 40%
private const val AMBER_AT = 0.7f      // fully amber at 70%, then fades toward red at 100%

/** Smoothly blends green -> amber -> red. [progress] is 0f to 1f. */
fun progressColor(progress: Float): Color = when {
    progress <= GREEN_UNTIL -> ProgressGreen
    progress <= AMBER_AT -> lerp(ProgressGreen, ProgressAmber, (progress - GREEN_UNTIL) / (AMBER_AT - GREEN_UNTIL))
    else -> lerp(ProgressAmber, ProgressRed, ((progress - AMBER_AT) / (1f - AMBER_AT)).coerceAtMost(1f))
}

/** Is the app dark right now? SYSTEM follows the phone's setting; LIGHT and DARK force one. */
@Composable
fun ThemeMode.resolveDarkTheme(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Wraps the whole app. Follows the phone's light/dark setting automatically. */
@Composable
fun BudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = BudgetShapes,
        content = content
    )
}
