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
import androidx.compose.ui.graphics.luminance
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

// ---- Widget: the same in light and dark mode. The background is chosen in Settings (default: sand) ----
val WidgetBackground = Color(0xFFECD9BA)   // sand, the default background
val WidgetDarkText = Color(0xFF111111)     // text and "+" button on a LIGHT background
val WidgetLightText = Color(0xFFF4F4F2)    // text and "+" button on a DARK background

// The colors you can pick for the widget background (Settings). The FIRST one is the default.
// Add, remove or change any of them: the text color is worked out for each one automatically.
val WidgetColorChoices = listOf(
    // neutrals
    WidgetBackground,        // sand (default)
    Color(0xFFF4F4F2),       // off-white
    Color(0xFFD9D9D4),       // mist
    Color(0xFFA7ABB3),       // gray
    Color(0xFF6B7078),       // slate gray
    Color(0xFF2B2F36),       // charcoal
    // soft colors
    Color(0xFFF2C9C4),       // blush
    Color(0xFFF5D3A8),       // apricot
    Color(0xFFF3E3A3),       // butter
    Color(0xFFCFE3B4),       // pistachio
    Color(0xFFB5DCCB),       // mint
    Color(0xFFB9D9EB),       // sky
    Color(0xFFC9C4EE),       // lavender
    Color(0xFFEBC4DE),       // orchid
    Color(0xFFD9A58B),       // clay
    Color(0xFFC9B79C),       // stone
    Color(0xFF9CC5A1),       // sage
    Color(0xFF8DB7D6),       // cornflower
    // deep colors
    Color(0xFFA94442),       // brick
    Color(0xFFC4793A),       // rust
    Color(0xFF4F7A57),       // forest
    Color(0xFF2F6F73),       // teal
    Color(0xFF2F5D9E),       // blue
    Color(0xFF4B3F8F),       // indigo
    Color(0xFF7A3E7D),       // plum
    Color(0xFF5C3D2E),       // espresso
    Color(0xFF6E2C3A),       // burgundy
    Color(0xFF1F3A5F),       // navy
    Color(0xFF14161A),       // slate (the app's dark)
    Color(0xFF0D0D0D)        // black
)

/**
 * The text / "+" button color that is readable on [background]: dark on light colors, light on dark ones.
 */
fun contentColorFor(background: Color): Color {
    if (background.luminance() < 0.18) {
        return WidgetLightText
    } else { return WidgetDarkText}
}

/**
 * How different two colors look in brightness: 1 = identical, 21 = black on white. Anything under about 3 is hard to
 * tell apart (this is the standard "contrast ratio" used for readable colors).
 */
fun contrastRatio(a: Color, b: Color): Float {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

/** The least contrast the progress bar's fill must have against the widget background to stand out. */
const val MIN_FILL_CONTRAST = 3f

/**
 * The progress bar's fill color ([fill]: green, amber or red) adjusted so it stays visible on [background].
 * If it is already different enough from the background it comes back unchanged. If it is too similar (a green bar
 * on a green widget), it is shifted toward black or white, whichever [contentColorFor] picks for that background,
 * just far enough to stand out.
 */
fun fillColorOn(fill: Color, background: Color): Color {
    if (contrastRatio(fill, background) >= MIN_FILL_CONTRAST) return fill

    // Too similar to the background: move the fill toward black or white in small steps and stop at the
    // first step that stands out, so the color changes as little as possible (a green stays green-ish).
    val target = contentColorFor(background)
    var step = 1
    while (step <= 20) {
        val shifted = lerp(fill, target, step / 20f)
        if (contrastRatio(shifted, background) >= MIN_FILL_CONTRAST) return shifted
        step++
    }
    return target // fully black or white always stands out
}

/** Every color the widget draws, all worked out from the one background color the user picked. */
data class WidgetColors(
    val background: Color,
    val text: Color,       // the number and the "+" button
    val quietText: Color,  // the small "left today" label
    val track: Color       // the empty part of the progress bar
)

fun widgetColorsFor(background: Color): WidgetColors {
    val text = contentColorFor(background)
    return WidgetColors(
        background = background,
        text = text,
        quietText = lerp(text, background, 0.45f),   // text color, faded toward the background
        track = lerp(background, text, 0.14f)         // background, nudged toward the text color
    )
}

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
