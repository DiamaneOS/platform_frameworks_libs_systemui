/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The DiamaneOS Project
 */

package com.android.systemui.monet;

import android.content.theming.ThemeStyle;

import com.google.ux.material.libmonet.dynamiccolor.ColorSpec.SpecVersion;
import com.google.ux.material.libmonet.dynamiccolor.DynamicColor;
import com.google.ux.material.libmonet.dynamiccolor.DynamicScheme;
import com.google.ux.material.libmonet.dynamiccolor.Variant;
import com.google.ux.material.libmonet.hct.Hct;
import com.google.ux.material.libmonet.palettes.TonalPalette;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The DiamaneOS palette style of the Tally design language.
 *
 * <p>Every system theme style (the styles Wallpaper &amp; style and the theme service can set)
 * builds this scheme, so the palette applies whatever style is chosen; see {@link #appliesTo}.
 * The quiet styles, Monochrome and Spritz, take the prototype's near-grey palettes; see
 * {@link #isQuiet}. Its roles, and how they follow the contrast level, are in
 * {@link TallyDynamicColors}.
 *
 * <p>The palettes are the Tally prototype's (diamaneos-design, design/prototypes/tally,
 * {@code T.styleFor} and {@code T.post}): each a fixed OKLCH hue and chroma, reduced to fit sRGB,
 * at a tone measured as CIELAB L*. HCT shares the tone but not the hue and chroma, and such a
 * palette is no HCT palette (its CAM16 hue drifts with tone, most for blues), so this class keeps
 * the prototype's colour math: it takes the decisions on the seed (a near-grey seed, the tertiary
 * hue, the accent's hue shift, the dark primary's distance from the lamp) and computes every colour
 * Tally defines, roles and palette stops, as the prototype does. HCT gives back each such colour
 * exactly from its own hue, chroma and tone.
 *
 * <p>The scheme's HCT palettes, used by the roles Tally leaves to libmonet, approximate the Tally
 * palettes: the primary palette runs through its colour at the lamp's tone, the others through
 * the prototype's colour at T40. Those roles resolve as Tonal Spot with the 2021 spec, whose tones
 * the prototype mostly shares, in every style.
 *
 * <p>The palettes are the same in the light and the dark scheme, so each palette stop and the lamp
 * are one colour in both.
 */
public class SchemeTally extends DynamicScheme {

    /**
     * The Tally palettes, and two colour families of the primary role that are not palettes. Each
     * is a fixed OKLCH hue and chroma; a role's colour at any tone comes from its family.
     */
    enum Palette {
        PRIMARY, SECONDARY, TERTIARY, NEUTRAL, NEUTRAL_VARIANT, ERROR,
        /** The light primary, the accent: the seed's hue, 8° lower for seeds at 55°-110°. */
        ACCENT,
        /** The dark primary above the lamp: the primary palette's hue at chroma 0.10 at most. */
        PALE_PRIMARY
    }

    // The prototype's palette style, T.styleFor, T.post and T.QUIET: chroma in OKLCH, hues in
    // OKLCH degrees, tones in L*.
    private static final double NEAR_GREY_CHROMA = 0.03;
    private static final double NEUTRAL_CHROMA = 0.006;
    private static final double NEUTRAL_VARIANT_CHROMA = 0.012;
    private static final double PRIMARY_MIN_CHROMA = 0.12;
    private static final double PRIMARY_GREY_CHROMA = 0.012;
    private static final double SECONDARY_CHROMA = 0.05;
    private static final double SECONDARY_GREY_CHROMA = 0.010;
    private static final double TERTIARY_HUE = 150;
    private static final double TERTIARY_HUE_NEAR_GREEN = 305;
    private static final double TERTIARY_NEAR_GREEN_DISTANCE = 40;
    private static final double TERTIARY_CHROMA = 0.06;
    private static final double ERROR_HUE = 27;
    private static final double ERROR_CHROMA = 0.17;
    private static final double ACCENT_SHIFT_MIN_HUE = 55;
    private static final double ACCENT_SHIFT_MAX_HUE = 110;
    private static final double ACCENT_HUE_SHIFT = -8;
    private static final double LAMP_MIN_TONE = 74;
    private static final double LAMP_MAX_TONE = 82;
    private static final double LAMP_GAP = 5;
    private static final double DARK_PRIMARY_MAX_CHROMA = 0.10;
    private static final double DARK_PRIMARY_LIFT = 6;
    private static final double DARK_PRIMARY_MAX_TONE = 90;
    // Quiet containers {chroma, tone}: the containers apps paint big areas with stay near grey.
    private static final double[] QUIET_PRIMARY_LIGHT = {0.045, 90};
    private static final double[] QUIET_SECONDARY_LIGHT = {0.022, 88};
    private static final double[] QUIET_TERTIARY_LIGHT = {0.05, 90};
    private static final double[] QUIET_PRIMARY_DARK = {0.035, 30};
    private static final double[] QUIET_SECONDARY_DARK = {0.02, 28};
    private static final double[] QUIET_TERTIARY_DARK = {0.04, 30};
    // Where the scheme's HCT palettes are measured: T40, where the light primary, secondary,
    // tertiary and error roles sit, keeps them closest to the prototype's over the tones in use.
    private static final double REFERENCE_TONE = 40;

    private final Style mStyle;
    private final Map<String, Hct> mColors = new ConcurrentHashMap<>();

    public SchemeTally(List<Hct> sourceColorHctList, @ThemeStyle.Type int style, boolean isDark,
            double contrastLevel, Platform platform) {
        this(new Style(sourceColorHctList.get(0).toInt(), isQuiet(style)), sourceColorHctList,
                isDark, contrastLevel, platform);
    }

    private SchemeTally(Style style, List<Hct> sourceColorHctList, boolean isDark,
            double contrastLevel, Platform platform) {
        super(sourceColorHctList, Variant.TONAL_SPOT, isDark, contrastLevel, platform,
                SpecVersion.SPEC_2021, style.hctPalette(Palette.PRIMARY),
                style.hctPalette(Palette.SECONDARY), style.hctPalette(Palette.TERTIARY),
                style.hctPalette(Palette.NEUTRAL), style.hctPalette(Palette.NEUTRAL_VARIANT),
                Optional.of(style.hctPalette(Palette.ERROR)));
        mStyle = style;
    }

    /**
     * Whether {@link ColorScheme} builds this scheme for a style: every system theme style. CONTENT
     * (colours from media art and app icons) and the clock styles keep their stock schemes.
     */
    public static boolean appliesTo(@ThemeStyle.Type int style) {
        return switch (style) {
            case ThemeStyle.SPRITZ, ThemeStyle.TONAL_SPOT, ThemeStyle.VIBRANT,
                    ThemeStyle.EXPRESSIVE, ThemeStyle.RAINBOW, ThemeStyle.FRUIT_SALAD,
                    ThemeStyle.MONOCHROMATIC, ThemeStyle.CMF -> true;
            default -> false;
        };
    }

    /**
     * Whether a style is quiet: Monochrome and Spritz, which take the prototype's rule for a
     * near-grey seed (OKLCH chroma under 0.03, in {@code T.styleFor} and {@code T.post}) whatever
     * the seed's chroma, so a quiet style still looks quiet. Every other system style gives the
     * one Tally palette.
     *
     * <p>How the rule applies here is an interpretation of the design decision of 28 September
     * 2026 and can be changed in {@link Style}: the primary palette takes chroma 0.012 and the
     * secondary 0.010, the tertiary keeps hue 150 (no turn to violet near green), and the accent,
     * the dark primary and the quiet containers follow from those palettes as they do for a
     * near-grey seed. Only the lamp (primary_fixed_dim, what shows that something is on) keeps
     * the seed's own colour, as {@code T.post} computes it for that seed, so colour stays only
     * where something is on. The neutral, tertiary (chroma 0.06) and error palettes are as in
     * every style. Everything else drawn from the primary palette is near grey too: its stops,
     * among them the lamp's outline in light theme (accent1_700), the text on the lamp
     * (accent1_900) and the dark accent (accent1_200) the shell draws lit lamps with in dark
     * theme.
     */
    public static boolean isQuiet(@ThemeStyle.Type int style) {
        return style == ThemeStyle.MONOCHROMATIC || style == ThemeStyle.SPRITZ;
    }

    /** The Tally colour of {@code palette} at {@code tone}. */
    Hct color(Palette palette, double tone) {
        return mColors.computeIfAbsent(palette + " " + tone,
                key -> Hct.fromInt(mStyle.color(palette, tone)));
    }

    /**
     * The palette stop {@code system_<name>_<shade>}, where {@code name} is accent1, accent2,
     * accent3, neutral1, neutral2 or error.
     */
    Hct stop(String name, double tone) {
        return color(switch (name) {
            case "accent1" -> Palette.PRIMARY;
            case "accent2" -> Palette.SECONDARY;
            case "accent3" -> Palette.TERTIARY;
            case "neutral1" -> Palette.NEUTRAL;
            case "neutral2" -> Palette.NEUTRAL_VARIANT;
            case "error" -> Palette.ERROR;
            default -> throw new IllegalArgumentException("Unknown palette: " + name);
        }, tone);
    }

    /**
     * The lamp: the seed's own colour, its hue at the prototype's primary chroma for the seed, at
     * the seed's own tone held to T74-T82. The same in light and dark, in every style.
     */
    Hct lamp() {
        return mColors.computeIfAbsent("lamp", key -> Hct.fromInt(mStyle.mLamp));
    }

    /**
     * The primary's family: the accent in light; in dark the primary palette, or its paler family
     * when T80 is too close to the lamp.
     */
    Palette primaryFamily() {
        return !isDark ? Palette.ACCENT
                : mStyle.mDarkPrimaryNearLamp ? Palette.PALE_PRIMARY : Palette.PRIMARY;
    }

    /**
     * The primary's tone: T40 in light; in dark T80, or six tones above the lamp when T80 is fewer
     * than five tones from it, so the dark app primary is never the lamp.
     */
    double primaryTone() {
        return !isDark ? 40 : mStyle.mDarkPrimaryNearLamp ? mStyle.mDarkPrimaryTone : 80;
    }

    /** The quiet containers of this scheme. */
    Hct primaryContainer() {
        return quiet("primary container", Palette.PRIMARY,
                isDark ? QUIET_PRIMARY_DARK : QUIET_PRIMARY_LIGHT);
    }

    Hct secondaryContainer() {
        return quiet("secondary container", Palette.SECONDARY,
                isDark ? QUIET_SECONDARY_DARK : QUIET_SECONDARY_LIGHT);
    }

    Hct tertiaryContainer() {
        return quiet("tertiary container", Palette.TERTIARY,
                isDark ? QUIET_TERTIARY_DARK : QUIET_TERTIARY_LIGHT);
    }

    private Hct quiet(String key, Palette palette, double[] chromaAndTone) {
        return mColors.computeIfAbsent(key,
                k -> Hct.fromInt(mStyle.quiet(palette, chromaAndTone[0], chromaAndTone[1])));
    }

    /** The colour of the role {@code name} in this scheme, computed once. */
    Hct role(String name, Supplier<Hct> color) {
        // Not computeIfAbsent: a role's colour can depend on another role's.
        String key = "role " + name;
        Hct hct = mColors.get(key);
        if (hct == null) {
            hct = color.get();
            mColors.putIfAbsent(key, hct);
        }
        return hct;
    }

    // Asked for a role, the scheme gives Tally's, as monet's role map does. Its role getters
    // (getPrimary and so on) come here too, and Wallpaper & style's previews ask it this way.

    @Override
    public Hct getHct(DynamicColor dynamicColor) {
        DynamicColor tally = TallyDynamicColors.forName(dynamicColor.name);
        return (tally != null ? tally : dynamicColor).getHct(this);
    }

    @Override
    public int getArgb(DynamicColor dynamicColor) {
        DynamicColor tally = TallyDynamicColors.forName(dynamicColor.name);
        return (tally != null ? tally : dynamicColor).getArgb(this);
    }

    private static double hueDistance(double a, double b) {
        double d = Math.abs(a - b) % 360;
        return d > 180 ? 360 - d : d;
    }

    /** The prototype's palette style for one seed; the same for light and dark. */
    static final class Style {
        final double mLampTone;
        final int mLamp;
        final boolean mDarkPrimaryNearLamp;
        final double mDarkPrimaryTone;
        private final double mSeedHue;
        private final double mAccentHue;
        private final double mPrimaryChroma;
        private final double mSecondaryChroma;
        private final double mTertiaryHue;
        // The scheme's HCT palettes, {hue, chroma}.
        private final double[] mHctPrimary;
        private final double[] mHctSecondary;
        private final double[] mHctTertiary;
        private final double[] mHctNeutral;
        private final double[] mHctNeutralVariant;
        private final double[] mHctError;

        /** The palette style for {@code seed}; {@code quiet} for Monochrome and Spritz. */
        Style(int seed, boolean quiet) {
            double[] lch = Oklch.fromArgb(seed);
            double seedChroma = lch[1];
            mSeedHue = lch[2];
            boolean greySeed = seedChroma < NEAR_GREY_CHROMA;
            // T.post's primary chroma for the seed, which the lamp keeps in every style.
            double seedPrimaryChroma = greySeed ? PRIMARY_GREY_CHROMA
                    : Math.max(PRIMARY_MIN_CHROMA, seedChroma);
            // The quiet styles take the near-grey rule whatever the seed (see isQuiet).
            boolean nearGrey = greySeed || quiet;
            mPrimaryChroma = nearGrey ? PRIMARY_GREY_CHROMA : seedPrimaryChroma;
            mSecondaryChroma = nearGrey ? SECONDARY_GREY_CHROMA : SECONDARY_CHROMA;
            mTertiaryHue = !nearGrey
                    && hueDistance(mSeedHue, TERTIARY_HUE) < TERTIARY_NEAR_GREEN_DISTANCE
                    ? TERTIARY_HUE_NEAR_GREEN : TERTIARY_HUE;
            mAccentHue = mSeedHue >= ACCENT_SHIFT_MIN_HUE && mSeedHue <= ACCENT_SHIFT_MAX_HUE
                    ? mSeedHue + ACCENT_HUE_SHIFT : mSeedHue;
            mLampTone = Math.min(LAMP_MAX_TONE, Math.max(LAMP_MIN_TONE, Oklch.tone(seed)));
            mLamp = Oklch.toArgb(mSeedHue, seedPrimaryChroma, mLampTone, null);
            // The dark app primary is never the lamp: where T80 is fewer than five tones from it,
            // the primary moves to a paler colour six tones above the lamp.
            mDarkPrimaryNearLamp = Math.abs(Oklch.tone(color(Palette.PRIMARY, 80)) - mLampTone)
                    < LAMP_GAP;
            mDarkPrimaryTone = Math.min(DARK_PRIMARY_MAX_TONE, mLampTone + DARK_PRIMARY_LIFT);

            // The primary HCT palette runs through the primary palette's colour at the lamp's
            // tone, which is the lamp but in the quiet styles. Where sRGB cannot hold that colour
            // at full chroma, it keeps the chroma the prototype asks for, not to dull other tones.
            boolean[] clipped = new boolean[1];
            mHctPrimary = hueAndChroma(
                    Oklch.toArgb(mSeedHue, mPrimaryChroma, mLampTone, clipped));
            if (clipped[0]) {
                double requested = !nearGrey && seedChroma >= PRIMARY_MIN_CHROMA
                        ? Hct.fromInt(seed).getChroma()
                        : Hct.fromInt(color(Palette.PRIMARY, REFERENCE_TONE)).getChroma();
                mHctPrimary[1] = Math.max(mHctPrimary[1], requested);
            }
            mHctSecondary = hueAndChroma(color(Palette.SECONDARY, REFERENCE_TONE));
            mHctTertiary = hueAndChroma(color(Palette.TERTIARY, REFERENCE_TONE));
            mHctNeutral = hueAndChroma(color(Palette.NEUTRAL, REFERENCE_TONE));
            mHctNeutralVariant = hueAndChroma(color(Palette.NEUTRAL_VARIANT, REFERENCE_TONE));
            mHctError = hueAndChroma(color(Palette.ERROR, REFERENCE_TONE));
        }

        /** The prototype's colour of {@code palette} at {@code tone}. */
        int color(Palette palette, double tone) {
            return switch (palette) {
                case PRIMARY -> Oklch.toArgb(mSeedHue, mPrimaryChroma, tone, null);
                case SECONDARY -> Oklch.toArgb(mSeedHue, mSecondaryChroma, tone, null);
                case TERTIARY -> Oklch.toArgb(mTertiaryHue, TERTIARY_CHROMA, tone, null);
                case NEUTRAL -> Oklch.toArgb(mSeedHue, NEUTRAL_CHROMA, tone, null);
                case NEUTRAL_VARIANT -> Oklch.toArgb(mSeedHue, NEUTRAL_VARIANT_CHROMA, tone, null);
                case ERROR -> Oklch.toArgb(ERROR_HUE, ERROR_CHROMA, tone, null);
                case ACCENT -> Oklch.toArgb(mAccentHue, mPrimaryChroma, tone, null);
                case PALE_PRIMARY -> Oklch.toArgb(mSeedHue,
                        Math.min(mPrimaryChroma, DARK_PRIMARY_MAX_CHROMA), tone, null);
            };
        }

        /** A quiet container of {@code palette}: its hue at a lower chroma. */
        int quiet(Palette palette, double chroma, double tone) {
            return switch (palette) {
                case PRIMARY ->
                        Oklch.toArgb(mSeedHue, Math.min(chroma, mPrimaryChroma), tone, null);
                case SECONDARY ->
                        Oklch.toArgb(mSeedHue, Math.min(chroma, mSecondaryChroma), tone, null);
                case TERTIARY -> Oklch.toArgb(mTertiaryHue, chroma, tone, null);
                default -> throw new IllegalArgumentException("No quiet container: " + palette);
            };
        }

        /** A new HCT palette approximating {@code palette}, for the scheme's palette fields. */
        TonalPalette hctPalette(Palette palette) {
            double[] hueAndChroma = switch (palette) {
                case PRIMARY -> mHctPrimary;
                case SECONDARY -> mHctSecondary;
                case TERTIARY -> mHctTertiary;
                case NEUTRAL -> mHctNeutral;
                case NEUTRAL_VARIANT -> mHctNeutralVariant;
                case ERROR -> mHctError;
                case ACCENT, PALE_PRIMARY ->
                        throw new IllegalArgumentException("Not a palette: " + palette);
            };
            return TonalPalette.fromHueAndChroma(hueAndChroma[0], hueAndChroma[1]);
        }

        private static double[] hueAndChroma(int argb) {
            Hct hct = Hct.fromInt(argb);
            return new double[] {hct.getHue(), hct.getChroma()};
        }
    }

    /**
     * The prototype's colour math (design/prototypes/harness.js: H.hexToOklch, H.tone and
     * H.fromHCT), kept formula for formula so the colours and decisions match the prototype's.
     */
    static final class Oklch {
        private Oklch() {}

        /** {L, C, h} in OKLCH, h in degrees. */
        static double[] fromArgb(int argb) {
            double[] lab = linearToOklab(linear(argb));
            return new double[] {lab[0], Math.hypot(lab[1], lab[2]),
                    (Math.atan2(lab[2], lab[1]) * 180 / Math.PI + 360) % 360};
        }

        /** CIELAB L*. */
        static double tone(int argb) {
            return lstarFromY(luminance(linear(argb)));
        }

        /**
         * The colour of OKLCH hue {@code hue} and chroma {@code chroma}, reduced to fit sRGB,
         * whose L* is {@code tone}. {@code clipped}, if given, receives whether chroma was reduced.
         */
        static int toArgb(double hue, double chroma, double tone, boolean[] clipped) {
            if (clipped != null) {
                clipped[0] = false;
            }
            if (tone <= 0) {
                return 0xff000000;
            }
            if (tone >= 100) {
                return 0xffffffff;
            }
            double radians = hue * Math.PI / 180;
            double lightness = solveLightness(chroma, radians, tone);
            if (!inGamut(oklabToLinear(lightness, chroma, radians))) {
                double low = 0;
                double high = chroma;
                for (int i = 0; i < 20; i++) {
                    double mid = (low + high) / 2;
                    if (inGamut(oklabToLinear(solveLightness(mid, radians, tone), mid, radians))) {
                        low = mid;
                    } else {
                        high = mid;
                    }
                }
                chroma = low;
                lightness = solveLightness(chroma, radians, tone);
                if (clipped != null) {
                    clipped[0] = true;
                }
            }
            double[] rgb = oklabToLinear(lightness, chroma, radians);
            int argb = 0xff000000;
            for (int i = 0; i < 3; i++) {
                double v = Math.min(1, Math.max(0, delinearize(rgb[i])));
                argb |= (int) Math.round(v * 255) << (16 - 8 * i);
            }
            return argb;
        }

        private static double solveLightness(double chroma, double radians, double tone) {
            double low = 0;
            double high = 1;
            for (int i = 0; i < 28; i++) {
                double mid = (low + high) / 2;
                if (lstarFromY(Math.max(0, luminance(oklabToLinear(mid, chroma, radians))))
                        < tone) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
            return (low + high) / 2;
        }

        private static double[] linear(int argb) {
            return new double[] {linearize(((argb >> 16) & 0xff) / 255.0),
                    linearize(((argb >> 8) & 0xff) / 255.0), linearize((argb & 0xff) / 255.0)};
        }

        private static double linearize(double c) {
            return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }

        private static double delinearize(double c) {
            return c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
        }

        private static double luminance(double[] rgb) {
            return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
        }

        private static double lstarFromY(double y) {
            return y > 216.0 / 24389.0 ? 116 * Math.cbrt(y) - 16 : 24389.0 / 27.0 * y;
        }

        private static boolean inGamut(double[] rgb) {
            for (double v : rgb) {
                if (v < -1e-4 || v > 1 + 1e-4) {
                    return false;
                }
            }
            return true;
        }

        private static double[] linearToOklab(double[] rgb) {
            double l = Math.cbrt(0.4122214708 * rgb[0] + 0.5363325363 * rgb[1]
                    + 0.0514459929 * rgb[2]);
            double m = Math.cbrt(0.2119034982 * rgb[0] + 0.6806995451 * rgb[1]
                    + 0.1073969566 * rgb[2]);
            double s = Math.cbrt(0.0883024619 * rgb[0] + 0.2817188376 * rgb[1]
                    + 0.6299787005 * rgb[2]);
            return new double[] {0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
                    1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
                    0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s};
        }

        private static double[] oklabToLinear(double lightness, double chroma, double radians) {
            double a = chroma * Math.cos(radians);
            double b = chroma * Math.sin(radians);
            double l = Math.pow(lightness + 0.3963377774 * a + 0.2158037573 * b, 3);
            double m = Math.pow(lightness - 0.1055613458 * a - 0.0638541728 * b, 3);
            double s = Math.pow(lightness - 0.0894841775 * a - 1.291485548 * b, 3);
            return new double[] {4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
                    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
                    -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s};
        }
    }
}
