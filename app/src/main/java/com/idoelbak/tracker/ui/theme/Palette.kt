package com.idoelbak.tracker.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Every colour the app draws with, for one palette in one mode.
 *
 * Palettes are data, not hard-coded colours: Settings iterates over [Palettes.all] and swaps the
 * whole set at once, so adding an eighth palette is one entry in a list.
 */
data class Tokens(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val onPrimary: Color,
    val success: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    /** The unfilled part of a ring or bar. */
    val track: Color,
    /** Hairline borders and dividers. */
    val rule: Color,
    /**
     * Five steps for the consistency grid, quiet to strong. One hue, never a rainbow: the grid is
     * read as intensity, and a rainbow makes an empty day compete with a full one for attention.
     */
    val heat: List<Color>
)

data class Palette(
    val id: String,
    val name: String,
    val light: Tokens,
    val dark: Tokens
)

private fun mix(a: Color, b: Color, amount: Float) = Color(
    red = a.red + (b.red - a.red) * amount,
    green = a.green + (b.green - a.green) * amount,
    blue = a.blue + (b.blue - a.blue) * amount
)

private val LightInk = Color(0xFF1D2430)
private val LightInk2 = Color(0xFF4E5765)
private val LightMuted = Color(0xFF767E8B)

private val DarkBackground = Color(0xFF16181D)
private val DarkSurface = Color(0xFF1F242D)
private val DarkSurfaceAlt = Color(0xFF262C36)
private val DarkInk = Color(0xFFE8EAEE)
private val DarkInk2 = Color(0xFFB4BBC7)
private val DarkMuted = Color(0xFF8A93A3)
private val DarkTrack = Color(0xFF333944)
private val DarkRule = Color(0xFF2A303A)

/**
 * Builds both modes from the three colours that actually distinguish a palette. The neutrals are
 * derived from the background so a cream palette gets warm rules and a cool one gets cool rules,
 * and the dark mode lightens primary and success rather than keeping unreadable ink-on-ink.
 */
private fun palette(
    id: String,
    name: String,
    primary: Color,
    success: Color,
    lightBackground: Color
) = Palette(
    id = id,
    name = name,
    light = Tokens(
        background = lightBackground,
        surface = Color.White,
        surfaceAlt = mix(lightBackground, Color.White, 0.45f),
        primary = primary,
        onPrimary = Color.White,
        success = success,
        ink = LightInk,
        ink2 = LightInk2,
        muted = LightMuted,
        track = mix(lightBackground, LightInk, 0.12f),
        rule = mix(lightBackground, LightInk, 0.07f),
        heat = ramp(mix(lightBackground, Color.White, 0.3f), mix(success, Color.Black, 0.18f))
    ),
    dark = Tokens(
        background = DarkBackground,
        surface = DarkSurface,
        surfaceAlt = DarkSurfaceAlt,
        primary = mix(primary, Color.White, 0.45f),
        onPrimary = Color(0xFF10141B),
        success = mix(success, Color.White, 0.35f),
        ink = DarkInk,
        ink2 = DarkInk2,
        muted = DarkMuted,
        track = DarkTrack,
        rule = DarkRule,
        heat = ramp(DarkSurfaceAlt, mix(success, Color.White, 0.25f))
    )
)

/** Five steps from an almost-empty square to a full one. */
private fun ramp(from: Color, to: Color) = listOf(0f, 0.28f, 0.5f, 0.74f, 1f).map { mix(from, to, it) }

object Palettes {

    /** The default. Its tokens are the ones drawn on the design canvas, so they are spelled out. */
    val IndigoSage = Palette(
        id = "indigo_sage",
        name = "Indigo & Sage",
        light = Tokens(
            background = Color(0xFFF4F0E6),
            surface = Color(0xFFFFFFFF),
            surfaceAlt = Color(0xFFFAF7EF),
            primary = Color(0xFF2E4A7D),
            onPrimary = Color(0xFFFFFFFF),
            success = Color(0xFF7A8B5A),
            ink = Color(0xFF1D2430),
            ink2 = Color(0xFF4E5765),
            muted = Color(0xFF767E8B),
            track = Color(0xFFD9D4C6),
            rule = Color(0xFFE6E0D2),
            // The exact ramp drawn on the design canvas.
            heat = listOf(
                Color(0xFFF0ECE0), Color(0xFFD4DCBF), Color(0xFFB2C094),
                Color(0xFF8FA16C), Color(0xFF6B7C47)
            )
        ),
        dark = Tokens(
            background = Color(0xFF16181D),
            surface = Color(0xFF1F242D),
            surfaceAlt = Color(0xFF262C36),
            primary = Color(0xFF7B9BD6),
            onPrimary = Color(0xFF10141B),
            success = Color(0xFF9DB37A),
            ink = Color(0xFFE8EAEE),
            ink2 = Color(0xFFB4BBC7),
            muted = Color(0xFF8A93A3),
            track = Color(0xFF333944),
            rule = Color(0xFF2A303A),
            heat = listOf(
                Color(0xFF262C36), Color(0xFF3B4A38), Color(0xFF546A44),
                Color(0xFF7B9457), Color(0xFF9DB37A)
            )
        )
    )

    val all = listOf(
        IndigoSage,
        palette("navy_teal", "Navy & Teal", Color(0xFF1B2A4A), Color(0xFF1F9C92), Color(0xFFF7F7F4)),
        palette("blue_plum", "Blue & Plum", Color(0xFF3A5BA0), Color(0xFF7E5A78), Color(0xFFEFEDEA)),
        // Midnight & Mint is a dark-first palette; #0E1520 is its dark ground, not a light one.
        palette("midnight_mint", "Midnight & Mint", Color(0xFF4C7DF0), Color(0xFF4FD1A5), Color(0xFFEDF1F7))
            .let { it.copy(dark = it.dark.copy(background = Color(0xFF0E1520))) },
        palette("steel_olive", "Steel & Olive", Color(0xFF40566E), Color(0xFF6E7A3F), Color(0xFFEFE7DA)),
        palette("blue_amber", "Blue & Amber", Color(0xFF2E4A7D), Color(0xFFD99A2B), Color(0xFFF6F2E9)),
        palette("blue_crimson", "Blue & Crimson", Color(0xFF2E4A7D), Color(0xFFB0453F), Color(0xFFF3EFEA))
    )

    val default = IndigoSage

    fun byId(id: String?): Palette = all.firstOrNull { it.id == id } ?: default
}
