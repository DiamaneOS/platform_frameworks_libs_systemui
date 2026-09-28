/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The DiamaneOS Project
 */
package com.android.systemui.monet

import android.app.WallpaperColors
import android.content.theming.ThemeStyle
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.ux.material.libmonet.contrast.Contrast
import com.google.ux.material.libmonet.dynamiccolor.ColorSpec.SpecVersion
import com.google.ux.material.libmonet.dynamiccolor.DynamicColor
import com.google.ux.material.libmonet.dynamiccolor.DynamicScheme.Platform
import com.google.ux.material.libmonet.dynamiccolor.MaterialDynamicColors
import com.google.ux.material.libmonet.hct.Hct
import com.google.ux.material.libmonet.scheme.SchemeTonalSpot
import com.google.ux.material.libmonet.utils.ColorUtils
import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The resources of some of the prototype's colours (diamaneos-design d7078f8,
 * design/prototypes/tally), in this order: the lamp, onLamp, the accent in light and dark, the dark
 * app primary, the background in light and dark, the surface in light, the tertiary in light.
 */
private val PROTOTYPE_RESOURCES =
    listOf(
        listOf("primary_fixed_dim_light", "primary_fixed_dim_dark"),
        listOf("accent1_900_light", "accent1_900_dark"),
        listOf("primary_light"),
        listOf("accent1_200_dark"),
        listOf("primary_dark"),
        listOf("surface_container_low_light"),
        listOf("surface_container_lowest_dark"),
        listOf("neutral1_10_light"),
        listOf("tertiary_light"),
    )

/** Of those, the ones that hold at every contrast level. */
private val FIXED_PROTOTYPE_RESOURCES =
    PROTOTYPE_RESOURCES.flatten() - listOf("primary_light", "primary_dark", "tertiary_light")

/**
 * A seed, whether its dark primary moves above the lamp (T80 is fewer than 5 tones away), and the
 * prototype's colours for it: [prototype] for every style but the quiet ones, [quiet] for
 * Monochrome and Spritz (the prototype's rule for a near-grey seed applied whatever the seed's
 * chroma, with the seed's own lamp).
 */
private class Seed(
    val name: String,
    seed: String,
    val darkPrimaryNearLamp: Boolean,
    prototype: String,
    quiet: String,
) {
    val argb = argb(seed)
    /** The lamp's tone: the seed's own tone held to T74-T82. */
    val lampTone = Hct.fromInt(argb).tone.coerceIn(74.0, 82.0)
    val darkPrimaryTone = if (darkPrimaryNearLamp) minOf(90.0, lampTone + 6) else 80.0
    val prototype = prototypeColors(prototype)
    val quiet = prototypeColors(quiet)
}

private fun prototypeColors(colors: String): Map<String, Int> =
    PROTOTYPE_RESOURCES.zip(colors.split(" ").map(::argb))
        .flatMap { (names, color) -> names.map { it to color } }
        .toMap()

private fun argb(hex: String) = (0xFF000000 or hex.removePrefix("#").toLong(16)).toInt()

// The five presets and the prototype's wallpaper seed (extracted from its 'Night tram').
private val SEEDS =
    listOf(
        Seed(
            "Sodium",
            "#FBA700",
            darkPrimaryNearLamp = false,
            prototype = "#FBA700 #2A1800 #8A5100 #FFB956 #FFB956 #F6F3EF #100E0B #FFFCF8 #43664A",
            quiet = "#FBA700 #1F1B16 #635D58 #CBC6BE #CBC6BE #F6F3EF #100E0B #FFFCF8 #43664A",
        ),
        Seed(
            "Phosphor",
            "#00D7D4",
            darkPrimaryNearLamp = true,
            prototype = "#00D7D4 #00201F #006A68 #1ADCD9 #77E3E0 #EFF5F4 #0B0F0F #F8FDFD #43664A",
            quiet = "#00D7D4 #151D1D #576060 #BEC9C8 #C9D4D3 #EFF5F4 #0B0F0F #F8FDFD #43664A",
        ),
        Seed(
            "Signal violet",
            "#7D39FF",
            darkPrimaryNearLamp = false,
            prototype = "#B8ABFF #25005A #7225F0 #C8BFFF #C8BFFF #F3F3F8 #0E0E11 #FCFCFF #43664A",
            quiet = "#B8ABFF #1C1B21 #5E5D65 #C6C6CE #C6C6CE #F3F3F8 #0E0E11 #FCFCFF #43664A",
        ),
        Seed(
            "Rose",
            "#D6408F",
            darkPrimaryNearLamp = false,
            prototype = "#FF95C5 #3E0023 #B21571 #FFAFD1 #FFAFD1 #F7F2F4 #110D0F #FFFBFD #43664A",
            quiet = "#FF95C5 #211A1C #655C5F #CEC4C8 #CEC4C8 #F7F2F4 #110D0F #FFFBFD #43664A",
        ),
        Seed(
            "Lichen",
            "#B2CC4C",
            darkPrimaryNearLamp = true,
            prototype = "#B2CC4C #181E00 #556500 #B7D252 #C7DA8E #F3F4F0 #0E0E0C #FBFDF8 #68577C",
            quiet = "#B2CC4C #1B1C16 #5D5F58 #C5C7BF #D0D3CA #F3F4F0 #0E0E0C #FBFDF8 #43664A",
        ),
        Seed(
            "Wallpaper",
            "#C0563A",
            darkPrimaryNearLamp = false,
            prototype = "#FF9C82 #3B0900 #A43C20 #FFB5A0 #FFB5A0 #F8F2F1 #110D0C #FFFBFA #43664A",
            quiet = "#FF9C82 #211A18 #655C5A #CEC4C2 #CEC4C2 #F8F2F1 #110D0C #FFFBFA #43664A",
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

/** Monochrome and Spritz, which take the near-grey palettes. */
private val QUIET_STYLES = listOf(ThemeStyle.MONOCHROMATIC, ThemeStyle.SPRITZ)

/** Tones are L*; 8-bit rounding moves a colour's tone by up to about 0.3. */
private const val TONE_TOLERANCE = 0.5

/**
 * A resource's expected tone in the light and the dark scheme at standard contrast. The rows are
 * the 'Value' column of the Tally tokens (diamaneos-design, design/tokens/README.md: Colour roles
 * and Shell-only colours), through the resource each token reads, plus the other of the 13 roles
 * step 2 defines.
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

/**
 * A text or outline role, what it is drawn on in the light and the dark scheme, and the contrast
 * ratio it keeps against that at medium (0.5) and high (1.0) contrast: libmonet's own for the role
 * (ColorSpec2021, the spec the Tally scheme resolves with; see TallyDynamicColors).
 */
private class ContrastPair(
    val role: String,
    val light: String,
    val dark: String,
    val medium: Double,
    val high: Double,
) {
    fun on(isDark: Boolean) = if (isDark) dark else light
}

private fun onSurfaces(role: String, medium: Double, high: Double) =
    ContrastPair(role, "surface_dim", "surface_bright", medium, high)

private fun on(role: String, background: String, medium: Double, high: Double) =
    ContrastPair(role, background, background, medium, high)

private val CONTRAST_PAIRS =
    listOf(
        onSurfaces("on_surface", 11.0, 21.0),
        onSurfaces("on_surface_variant", 7.0, 11.0),
        onSurfaces("outline", 4.5, 7.0),
        onSurfaces("outline_variant", 3.0, 4.5),
        onSurfaces("primary", 7.0, 7.0),
        onSurfaces("secondary", 7.0, 7.0),
        onSurfaces("tertiary", 7.0, 7.0),
        onSurfaces("error", 7.0, 7.0),
        on("on_primary", "primary", 11.0, 21.0),
        on("on_secondary", "secondary", 11.0, 21.0),
        on("on_tertiary", "tertiary", 11.0, 21.0),
        on("on_error", "error", 11.0, 21.0),
        on("on_primary_container", "primary_container", 7.0, 11.0),
        on("on_secondary_container", "secondary_container", 7.0, 11.0),
        on("on_tertiary_container", "tertiary_container", 7.0, 11.0),
        on("on_error_container", "error_container", 7.0, 11.0),
        on("on_background", "background", 4.5, 7.0),
        on("inverse_on_surface", "inverse_surface", 11.0, 21.0),
        on("inverse_primary", "inverse_surface", 7.0, 7.0),
        on("on_primary_fixed", "primary_fixed_dim", 11.0, 21.0),
        on("on_primary_fixed", "primary_fixed", 11.0, 21.0),
        on("on_primary_fixed_variant", "primary_fixed_dim", 7.0, 11.0),
        on("on_primary_fixed_variant", "primary_fixed", 7.0, 11.0),
    )

/** The roles that hold their colour at every contrast level; the palette stops do too. */
private val FIXED_ROLES =
    listOf(
        "background",
        "surface",
        "surface_dim",
        "surface_bright",
        "surface_variant",
        "surface_container_lowest",
        "surface_container_low",
        "surface_container",
        "surface_container_high",
        "surface_container_highest",
        "inverse_surface",
        "surface_tint",
        "primary_container",
        "secondary_container",
        "tertiary_container",
        "error_container",
        "primary_fixed",
        "primary_fixed_dim",
    )

/** The primary and secondary roles the quiet styles make near grey; the lamp keeps its colour. */
private val QUIET_ROLES =
    listOf(
        "primary",
        "on_primary",
        "primary_container",
        "on_primary_container",
        "inverse_primary",
        "surface_tint",
        "primary_fixed",
        "on_primary_fixed",
        "on_primary_fixed_variant",
        "secondary",
        "on_secondary",
        "secondary_container",
        "on_secondary_container",
    )

/**
 * The most CIELAB chroma a near-grey colour has: the near-grey palettes are OKLCH chroma 0.012 and
 * 0.010, at most 5.3 in CIELAB over the harness's seed grid. The presets' lamps have 44 or more.
 */
private const val NEAR_GREY_CHROMA = 6.0

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

private val PALETTE_STOPS by lazy {
    COLORS.keys.filter { it.matches(Regex("(accent[123]|neutral[12]|error)_\\d+")) }
}

private fun colors(scheme: ColorScheme) =
    COLORS.mapValues { it.value.getArgb(scheme.materialScheme) }

/** One seed, style and contrast, with the colours of the light and the dark copies. */
private class Case(val seed: Seed, val style: Int, val contrast: Double) {
    val where = "${seed.name}, ${ThemeStyle.name(style)}, contrast $contrast"
    val quiet = style in QUIET_STYLES
    val light = colors(ColorScheme(seed.argb, false, style, contrast))
    val dark = colors(ColorScheme(seed.argb, true, style, contrast))
}

private val CASES: List<Case> by lazy {
    SEEDS.flatMap { seed ->
        SYSTEM_STYLES.flatMap { style -> CONTRASTS.map { Case(seed, style, it) } }
    }
}

private fun case(seed: Seed, style: Int, contrast: Double) =
    CASES.first { it.seed == seed && it.style == style && it.contrast == contrast }

@SmallTest
@RunWith(AndroidJUnit4::class)
class TallyPaletteStyleTest {
    @Test
    fun valueTonesAtStandardContrast() {
        for (case in CASES.filter { it.contrast == 0.0 }) {
            for (e in EXPECTED) {
                assertTone(case.light, e.resource, e.light(case.seed), "_light, ${case.where}")
                assertTone(case.dark, e.resource, e.dark(case.seed), "_dark, ${case.where}")
            }
        }
    }

    @Test
    fun colorsAreThePrototypes() {
        for (case in CASES) {
            val prototype = if (case.quiet) case.seed.quiet else case.seed.prototype
            for ((resource, color) in prototype) {
                if (case.contrast > 0 && resource !in FIXED_PROTOTYPE_RESOURCES) continue
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
    fun textAndOutlineRolesGainContrast() {
        for (case in CASES.filter { it.contrast > 0 }) {
            val standard = case(case.seed, case.style, 0.0)
            for (pair in CONTRAST_PAIRS) {
                for (isDark in listOf(false, true)) {
                    val copy = if (isDark) case.dark else case.light
                    val base = if (isDark) standard.dark else standard.light
                    val on = pair.on(isDark)
                    val ratio = ratio(copy, pair.role, on)
                    val wanted = if (case.contrast >= 1.0) pair.high else pair.medium
                    val where =
                        "system_${pair.role} on $on ${"%.2f".format(ratio)}:1 " +
                            "(${hex(copy, pair.role)}), ${mode(isDark)}, ${case.where}"
                    // The ratio libmonet asks for, or as close as black or white gets.
                    val extreme = copy.getValue(pair.role) in listOf(Color.BLACK, Color.WHITE)
                    assertTrue("$where, wanted $wanted:1", ratio >= wanted || extreme)
                    // Never less than at standard contrast.
                    assertTrue(where, ratio >= ratio(base, pair.role, on) - 0.01)
                }
            }
        }
    }

    @Test
    fun contrastTargetsAreLibmonets() {
        val roles =
            MaterialDynamicColors().allDynamicColors().map { it.get() }.associateBy { it.name }
        for (isDark in listOf(false, true)) {
            val scheme =
                SchemeTonalSpot(
                    Hct.fromInt(SEEDS.first().argb),
                    isDark,
                    0.0,
                    SpecVersion.SPEC_2021,
                    Platform.PHONE,
                )
            for (pair in CONTRAST_PAIRS) {
                val role = roles.getValue(pair.role)
                val where = "${pair.role}, ${mode(isDark)}"
                val curve = role.contrastCurve.apply(scheme)
                assertEquals(where, pair.medium, curve.get(0.5), 0.0)
                assertEquals(where, pair.high, curve.get(1.0), 0.0)
                val on =
                    listOfNotNull(
                        role.background?.apply(scheme)?.name,
                        role.secondBackground?.apply(scheme)?.name,
                    )
                assertTrue("$where on $on", pair.on(isDark) in on)
            }
        }
    }

    @Test
    fun surfacesContainersAndPaletteStopsHoldAtEveryContrast() {
        for (case in CASES.filter { it.contrast > 0 }) {
            assertSameColours(case(case.seed, case.style, 0.0), case, FIXED_ROLES + PALETTE_STOPS)
        }
    }

    @Test
    fun lampIsTheSeedsOwnColourInLightAndDarkAtEveryContrast() {
        for (case in CASES) {
            val lamp = hex(case.light, "primary_fixed_dim")
            assertEquals(
                "system_primary_fixed_dim_light and _dark, ${case.where}",
                lamp,
                hex(case.dark, "primary_fixed_dim"),
            )
            val prototype = "#%08X".format(case.seed.prototype.getValue("primary_fixed_dim_light"))
            assertEquals("lamp, ${case.where}", prototype, lamp)
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
    fun everyStyleButTheQuietOnesGivesTheSameColours() {
        for (case in CASES) {
            // The quiet styles give the same colours as each other.
            val style = if (case.quiet) ThemeStyle.SPRITZ else ThemeStyle.TONAL_SPOT
            assertSameColours(case(case.seed, style, case.contrast), case, COLORS.keys)
        }
    }

    @Test
    fun quietStylesAreNearGreyButTheLamp() {
        for (case in CASES.filter { it.quiet }) {
            val tonalSpot = case(case.seed, ThemeStyle.TONAL_SPOT, case.contrast)
            val primaryAndSecondary =
                PALETTE_STOPS.filter { it.startsWith("accent1_") || it.startsWith("accent2_") }
            for (name in primaryAndSecondary + QUIET_ROLES) {
                for ((copy, isDark) in listOf(case.light to false, case.dark to true)) {
                    val chroma = chroma(copy.getValue(name))
                    val where = "system_${name}_${mode(isDark)} chroma $chroma, ${case.where}"
                    assertTrue(where, chroma < NEAR_GREY_CHROMA)
                }
            }
            // The lamp, the neutral palettes and roles, and the error palette are every style's.
            val shared =
                PALETTE_STOPS.filter { it.startsWith("neutral") || it.startsWith("error_") } +
                    listOf("primary_fixed_dim", "surface", "on_surface", "outline", "error")
            assertSameColours(tonalSpot, case, shared)
            assertTrue(case.where, chroma(case.light.getValue("primary_fixed_dim")) > 30)
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

    @Test
    fun sodiumIsTheFallbackSeed() {
        assertEquals(argb("#FBA700"), ColorScheme.SODIUM)
        val grey = argb("#808080")
        // A wallpaper whose colours are all too grey for a seed.
        val wallpaper = WallpaperColors(Color.valueOf(grey), Color.valueOf(Color.WHITE), null)
        assertEquals(listOf(ColorScheme.SODIUM), ColorScheme.getSeedColors(wallpaper))
        // A transparent seed, and a grey one where the style needs colour, give Sodium's colours.
        for (style in SYSTEM_STYLES + ThemeStyle.CONTENT) {
            for (isDark in listOf(false, true)) {
                val sodium = colors(ColorScheme(ColorScheme.SODIUM, isDark, style))
                val where = "${ThemeStyle.name(style)}, ${mode(isDark)}"
                assertEquals(where, sodium, colors(ColorScheme(Color.TRANSPARENT, isDark, style)))
                if (style != ThemeStyle.CONTENT) {
                    assertEquals(where, sodium, colors(ColorScheme(grey, isDark, style)))
                }
            }
        }
    }

    /** Each of [names] is the same colour in [actual] as in [expected], in light and in dark. */
    private fun assertSameColours(expected: Case, actual: Case, names: Iterable<String>) {
        for (name in names) {
            val where = ", ${actual.where}"
            assertEquals(
                "system_${name}_light$where",
                hex(expected.light, name),
                hex(actual.light, name),
            )
            assertEquals(
                "system_${name}_dark$where",
                hex(expected.dark, name),
                hex(actual.dark, name),
            )
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

    private fun ratio(copy: Map<String, Int>, a: String, b: String) =
        Contrast.ratioOfTones(tone(copy, a), tone(copy, b))

    private fun hex(copy: Map<String, Int>, name: String) = "#%08X".format(copy.getValue(name))

    /** CIELAB chroma. */
    private fun chroma(argb: Int) = ColorUtils.labFromArgb(argb).let { hypot(it[1], it[2]) }

    private fun mode(isDark: Boolean) = if (isDark) "dark" else "light"
}
