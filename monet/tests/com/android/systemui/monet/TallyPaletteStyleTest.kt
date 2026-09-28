/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The DiamaneOS Project
 */
package com.android.systemui.monet

import android.content.theming.ThemeStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.ux.material.libmonet.dynamiccolor.DynamicColor
import com.google.ux.material.libmonet.hct.Hct
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A seed, whether its dark primary moves above the lamp (T80 is fewer than 5 tones away), and some
 * of the prototype's colours for it (diamaneos-design 65ed406, design/prototypes/tally), in this
 * order: the lamp, onLamp, the accent in light and dark, the dark app primary, the background in
 * light and dark, the surface in light.
 */
private class Seed(
    val name: String,
    seed: String,
    val darkPrimaryNearLamp: Boolean,
    prototype: String,
) {
    val argb = argb(seed)
    /** The lamp's tone: the seed's own tone held to T74-T82. */
    val lampTone = Hct.fromInt(argb).tone.coerceIn(74.0, 82.0)
    val darkPrimaryTone = if (darkPrimaryNearLamp) minOf(90.0, lampTone + 6) else 80.0
    val prototype: Map<String, Int> =
        listOf(
                listOf("primary_fixed_dim_light", "primary_fixed_dim_dark"),
                listOf("accent1_900_light", "accent1_900_dark"),
                listOf("primary_light"),
                listOf("accent1_200_dark"),
                listOf("primary_dark"),
                listOf("surface_container_low_light"),
                listOf("surface_container_lowest_dark"),
                listOf("neutral1_10_light"),
            )
            .zip(prototype.split(" ").map(::argb))
            .flatMap { (names, color) -> names.map { it to color } }
            .toMap()
}

private fun argb(hex: String) = (0xFF000000 or hex.removePrefix("#").toLong(16)).toInt()

// The five presets and the prototype's wallpaper seed (extracted from its 'Night tram').
private val SEEDS =
    listOf(
        Seed(
            "Sodium",
            "#FBA700",
            darkPrimaryNearLamp = false,
            prototype = "#FBA700 #2A1800 #8A5100 #FFB956 #FFB956 #F6F3EF #100E0B #FFFCF8",
        ),
        Seed(
            "Phosphor",
            "#00D7D4",
            darkPrimaryNearLamp = true,
            prototype = "#00D7D4 #00201F #006A68 #1ADCD9 #77E3E0 #EFF5F4 #0B0F0F #F8FDFD",
        ),
        Seed(
            "Signal violet",
            "#7D39FF",
            darkPrimaryNearLamp = false,
            prototype = "#B8ABFF #25005A #7225F0 #C8BFFF #C8BFFF #F3F3F8 #0E0E11 #FCFCFF",
        ),
        Seed(
            "Rose",
            "#D6408F",
            darkPrimaryNearLamp = false,
            prototype = "#FF95C5 #3E0023 #B21571 #FFAFD1 #FFAFD1 #F7F2F4 #110D0F #FFFBFD",
        ),
        Seed(
            "Lichen",
            "#B2CC4C",
            darkPrimaryNearLamp = true,
            prototype = "#B2CC4C #181E00 #556500 #B7D252 #C7DA8E #F3F4F0 #0E0E0C #FBFDF8",
        ),
        Seed(
            "Wallpaper",
            "#C0563A",
            darkPrimaryNearLamp = false,
            prototype = "#FF9C82 #3B0900 #A43C20 #FFB5A0 #FFB5A0 #F8F2F1 #110D0C #FFFBFA",
        ),
    )

private val CONTRASTS = listOf(0.0, 0.5, 1.0)

// Every style Wallpaper & style or the theme service can set; the palette applies under each.
private val SYSTEM_STYLES =
    listOf(
        ThemeStyle.SPRITZ,
        ThemeStyle.TONAL_SPOT,
        ThemeStyle.VIBRANT,
        ThemeStyle.EXPRESSIVE,
        ThemeStyle.RAINBOW,
        ThemeStyle.FRUIT_SALAD,
        ThemeStyle.MONOCHROMATIC,
        ThemeStyle.CMF,
    )

/** Tones are L*; 8-bit rounding moves a colour's tone by up to about 0.3. */
private const val TONE_TOLERANCE = 0.5

/**
 * A resource's expected tone in the light and the dark scheme. The rows are the 'Value' column of
 * the Tally tokens (diamaneos-design, design/tokens/README.md: Colour roles and Shell-only
 * colours), through the resource each token reads, plus the other of the 13 roles step 2 defines.
 */
private class Expected(
    val resource: String,
    val light: (Seed) -> Double,
    val dark: (Seed) -> Double,
)

private fun tones(resource: String, light: Double, dark: Double) =
    Expected(resource, { light }, { dark })

private val EXPECTED =
    listOf(
        tones("surface_container_low", 96.0, 10.0), // bg (light), surface (dark)
        tones("neutral1_10", 99.0, 99.0), // surface (light)
        tones("surface_container_lowest", 100.0, 4.0), // bg (dark)
        tones("surface_container_high", 92.0, 17.0), // surfaceHigh
        tones("on_surface", 10.0, 92.0), // ink
        tones("on_surface_variant", 38.0, 76.0), // muted
        tones("outline", 50.0, 58.0),
        tones("outline_variant", 82.0, 28.0),
        Expected("primary", { 40.0 }, { it.darkPrimaryTone }), // accent and lampInk (light)
        tones("on_primary", 100.0, 20.0), // onAccent (light)
        tones("error", 40.0, 80.0),
        tones("inverse_surface", 20.0, 90.0),
        tones("inverse_on_surface", 95.0, 20.0), // onInverse
        Expected("primary_fixed_dim", { it.lampTone }, { it.lampTone }), // lamp
        tones("accent1_200", 80.0, 80.0), // accent (dark)
        tones("accent1_700", 30.0, 30.0), // lampOutline (light)
        tones("accent1_800", 20.0, 20.0), // onAccent (dark)
        tones("accent1_900", 10.0, 10.0), // onLamp
    )

// The colours SystemUI's ThemeOverlayController writes: system_<name>_light from the light scheme
// and system_<name>_dark from the dark scheme.
private val COLORS: Map<String, DynamicColor> by lazy {
    (DynamicColors.getAllAccentPalette() +
            DynamicColors.getAllNeutralPalette() +
            DynamicColors.getAllErrorPalette() +
            DynamicColors.getAllDynamicColorsMapped() +
            DynamicColors.getFixedColorsMapped() +
            DynamicColors.getCustomColorsMapped())
        .associate { it.first to it.second }
}

/** One seed, style and contrast, with the colours of the light and the dark copies. */
private class Case(val seed: Seed, val style: Int, val contrast: Double) {
    val where = "${seed.name}, ${ThemeStyle.name(style)}, contrast $contrast"
    val light = colors(false)
    val dark = colors(true)

    private fun colors(isDark: Boolean): Map<String, Int> {
        val scheme = ColorScheme(seed.argb, isDark, style, contrast).materialScheme
        return COLORS.mapValues { it.value.getArgb(scheme) }
    }
}

private val CASES: List<Case> by lazy {
    SEEDS.flatMap { seed ->
        SYSTEM_STYLES.flatMap { style -> CONTRASTS.map { Case(seed, style, it) } }
    }
}

@SmallTest
@RunWith(AndroidJUnit4::class)
class TallyPaletteStyleTest {
    @Test
    fun valueTonesInBothSchemes() {
        for (case in CASES) {
            for (e in EXPECTED) {
                assertTone(case.light, e.resource, e.light(case.seed), "_light, ${case.where}")
                assertTone(case.dark, e.resource, e.dark(case.seed), "_dark, ${case.where}")
            }
        }
    }

    @Test
    fun lampIsOneColourInLightAndDark() {
        for (case in CASES) {
            assertEquals(
                "system_primary_fixed_dim_light and _dark, ${case.where}",
                hex(case.light, "primary_fixed_dim"),
                hex(case.dark, "primary_fixed_dim"),
            )
            val tone = tone(case.light, "primary_fixed_dim")
            val where = "lamp T$tone, ${case.where}"
            assertTrue(where, tone in 74.0 - TONE_TOLERANCE..82.0 + TONE_TOLERANCE)
            assertEquals(where, case.seed.lampTone, tone, TONE_TOLERANCE)
        }
    }

    @Test
    fun onLampIsOneColourInLightAndDark() {
        for (case in CASES) {
            assertEquals(
                "system_accent1_900_light and _dark, ${case.where}",
                hex(case.light, "accent1_900"),
                hex(case.dark, "accent1_900"),
            )
        }
    }

    @Test
    fun darkPrimaryPaletteIsTheLightOne() {
        for (case in CASES) {
            for (shade in TonalPalette.SHADE_KEYS) {
                val name = "accent1_$shade"
                assertEquals(
                    "system_${name}_light and _dark, ${case.where}",
                    hex(case.light, name),
                    hex(case.dark, name),
                )
            }
        }
    }

    @Test
    fun darkPrimaryKeepsAwayFromTheLamp() {
        for (case in CASES) {
            val gap = abs(tone(case.dark, "primary") - tone(case.dark, "primary_fixed_dim"))
            assertTrue(
                "dark primary $gap tones from the lamp, ${case.where}",
                gap >= 5 - TONE_TOLERANCE,
            )
        }
    }

    @Test
    fun colorsAreThePrototypes() {
        for (case in CASES) {
            for ((resource, color) in case.seed.prototype) {
                val copy = if (resource.endsWith("_light")) case.light else case.dark
                val name = resource.removeSuffix("_light").removeSuffix("_dark")
                assertEquals(
                    "system_$resource, ${case.where}",
                    "#%08X".format(color),
                    hex(copy, name),
                )
            }
        }
    }

    @Test
    fun everySystemStyleGivesTheSameColours() {
        val tonalSpot = CASES.filter { it.style == ThemeStyle.TONAL_SPOT }
        for (case in CASES) {
            val base = tonalSpot.first { it.seed == case.seed && it.contrast == case.contrast }
            for (name in COLORS.keys) {
                val where = ", ${case.where}"
                assertEquals(
                    "system_${name}_light$where",
                    hex(base.light, name),
                    hex(case.light, name),
                )
                assertEquals(
                    "system_${name}_dark$where",
                    hex(base.dark, name),
                    hex(case.dark, name),
                )
            }
        }
    }

    @Test
    fun contentAndClockStylesKeepTheirStockSchemes() {
        for (style in listOf(ThemeStyle.CONTENT, ThemeStyle.CLOCK, ThemeStyle.CLOCK_VIBRANT)) {
            for (isDark in listOf(false, true)) {
                val scheme = ColorScheme(SEEDS.first().argb, isDark, style).materialScheme
                assertFalse(ThemeStyle.name(style), scheme is SchemeTally)
            }
        }
        for (style in SYSTEM_STYLES) {
            val scheme = ColorScheme(SEEDS.first().argb, false, style).materialScheme
            assertTrue(ThemeStyle.name(style), scheme is SchemeTally)
        }
    }

    private fun assertTone(copy: Map<String, Int>, name: String, expected: Double, where: String) {
        assertEquals(
            "system_$name$where (${hex(copy, name)})",
            expected,
            tone(copy, name),
            TONE_TOLERANCE,
        )
    }

    private fun tone(copy: Map<String, Int>, name: String) = Hct.fromInt(copy.getValue(name)).tone

    private fun hex(copy: Map<String, Int>, name: String) = "#%08X".format(copy.getValue(name))
}
