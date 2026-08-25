package com.idoelbak.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.idoelbak.tracker.R

private fun variable(resId: Int, weight: Int) =
    Font(resId, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** UI and body text. */
val Figtree = FontFamily(
    variable(R.font.figtree, 400),
    variable(R.font.figtree, 500),
    variable(R.font.figtree, 600),
    variable(R.font.figtree, 700)
)

/** Numbers and titles -- tabular figures so counters do not jitter as they change. */
val Grotesk = FontFamily(
    variable(R.font.familjen_grotesk, 500),
    variable(R.font.familjen_grotesk, 600),
    variable(R.font.familjen_grotesk, 700)
)

/**
 * Every style carries [TextDirection.Content]: paragraph direction comes from the first strong
 * character of the string itself, not from the app locale. Together with `String.isolated()` at the
 * call sites, that is what stops a Hebrew habit name with an English word in it from reordering.
 */
private fun style(
    family: FontFamily,
    size: Double,
    weight: Int,
    letterSpacing: Double = 0.0,
    lineHeight: Double = size * 1.3
) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = letterSpacing.sp,
    lineHeight = lineHeight.sp,
    textDirection = TextDirection.Content
)

val TrackerTypography = Typography(
    // Big numbers -- the streak headline.
    displaySmall = style(Grotesk, 30.0, 700, letterSpacing = -0.6),
    // Screen title.
    headlineMedium = style(Grotesk, 27.0, 700, letterSpacing = -0.5),
    // Counters inside pills and rings.
    titleLarge = style(Grotesk, 19.0, 700, letterSpacing = -0.2),
    // Card title.
    titleMedium = style(Figtree, 14.5, 600),
    // A habit row.
    bodyLarge = style(Figtree, 14.0, 500),
    // Supporting copy.
    bodyMedium = style(Figtree, 12.5, 400),
    bodySmall = style(Figtree, 11.0, 400),
    labelLarge = style(Grotesk, 15.0, 700),
    labelMedium = style(Figtree, 12.0, 600),
    // Section overline -- always uppercased at the call site.
    labelSmall = style(Figtree, 10.5, 600, letterSpacing = 1.05)
)

val LocalTokens = staticCompositionLocalOf { Palettes.default.light }

/** Shorthand: `theme.surface` reads better than `LocalTokens.current.surface` in a layout. */
val theme: Tokens
    @Composable get() = LocalTokens.current

@Composable
fun TrackerTheme(
    palette: Palette = Palettes.default,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val tokens = if (dark) palette.dark else palette.light

    // Material's scheme exists so stock components (switches, sliders, ripples) land in the palette.
    // Everything the app draws itself reads from LocalTokens instead.
    val scheme = if (dark) {
        darkColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            secondary = tokens.success,
            background = tokens.background,
            onBackground = tokens.ink,
            surface = tokens.surface,
            onSurface = tokens.ink,
            surfaceVariant = tokens.surfaceAlt,
            onSurfaceVariant = tokens.ink2,
            outline = tokens.rule
        )
    } else {
        lightColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            secondary = tokens.success,
            background = tokens.background,
            onBackground = tokens.ink,
            surface = tokens.surface,
            onSurface = tokens.ink,
            surfaceVariant = tokens.surfaceAlt,
            onSurfaceVariant = tokens.ink2,
            outline = tokens.rule
        )
    }

    CompositionLocalProvider(LocalTokens provides tokens) {
        MaterialTheme(colorScheme = scheme, typography = TrackerTypography, content = content)
    }
}
