/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The DiamaneOS Project
 */

package com.android.systemui.monet;

import static com.android.systemui.monet.SchemeTally.Palette.ACCENT;
import static com.android.systemui.monet.SchemeTally.Palette.ERROR;
import static com.android.systemui.monet.SchemeTally.Palette.NEUTRAL;
import static com.android.systemui.monet.SchemeTally.Palette.NEUTRAL_VARIANT;
import static com.android.systemui.monet.SchemeTally.Palette.PRIMARY;
import static com.android.systemui.monet.SchemeTally.Palette.SECONDARY;
import static com.android.systemui.monet.SchemeTally.Palette.TERTIARY;

import com.android.systemui.monet.SchemeTally.Palette;

import com.google.ux.material.libmonet.contrast.Contrast;
import com.google.ux.material.libmonet.dynamiccolor.ContrastCurve;
import com.google.ux.material.libmonet.dynamiccolor.DynamicColor;
import com.google.ux.material.libmonet.dynamiccolor.DynamicScheme;
import com.google.ux.material.libmonet.dynamiccolor.MaterialDynamicColors;
import com.google.ux.material.libmonet.hct.Hct;
import com.google.ux.material.libmonet.palettes.TonalPalette;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * The roles of the DiamaneOS palette style ({@link SchemeTally}): the roles the Tally prototype
 * hands to apps ({@code T.appRoles}), each the prototype's colour at a fixed tone in the light and
 * the dark scheme. The shell's tokens read these roles and the palette stops (diamaneos-design,
 * design/tokens).
 *
 * <p>The roles apps pair with them have Tally tones too, at the 2021 spec's tones for standard
 * contrast: the other surfaces (background, dim, bright; surface variant as the highest
 * container, which Material 3 now uses in its place), the surface tint (the primary) and the rest
 * of the primary fixed family around the lamp.
 *
 * <p><b>Contrast.</b> At standard contrast (0, and below it) every role is exactly its Tally
 * colour. Above it the text and outline roles gain contrast against what they are drawn on, as
 * stock Material 3's contrast curves make them do. Each keeps its Tally colour while that gives
 * the ratio the contrast level asks for; otherwise it moves along its own Tally palette, away from
 * what it is drawn on, just far enough to give it, or to black or white. What a role is drawn on
 * and the ratio it needs are libmonet's own for the role, from the 2021 spec the scheme resolves
 * with (libmonet f56ffa0, ColorSpec2021), read from libmonet's role; at medium (0.5) and high
 * (1.0) contrast they are:
 *
 * <pre>
 * role                                     drawn on                           medium  high
 * on_surface                               highest surface *                  11      21
 * on_surface_variant                       highest surface                     7      11
 * outline                                  highest surface                     4.5     7
 * outline_variant                          highest surface                     3       4.5
 * primary, secondary, tertiary, error      highest surface                     7       7
 * on_primary, on_secondary, on_tertiary,   primary, secondary, tertiary,      11      21
 *   on_error                                 error
 * on_primary_container, on_secondary_      its container                       7      11
 *   container, on_tertiary_container,
 *   on_error_container
 * on_background                            background                          4.5     7
 * inverse_on_surface                       inverse_surface                    11      21
 * inverse_primary                          inverse_surface                     7       7
 * on_primary_fixed                         the lamp and primary_fixed         11      21
 * on_primary_fixed_variant                 the lamp and primary_fixed          7      11
 *
 * * surface_dim in light, surface_bright in dark: the surface with the least contrast to them.
 * </pre>
 *
 * <p>Below medium the ratio rises from the role's own Tally ratio to the medium one, so no colour
 * jumps at small levels. Which roles count as text: the on_* roles and the outlines, and primary,
 * secondary, tertiary, error and inverse_primary, which apps also draw text and icons in on
 * surfaces (text buttons, links, selected labels), the reason libmonet gives them a contrast
 * curve against the surfaces; libmonet stops them at 7:1, so they keep their colour. Every other
 * role holds its colour at every level: the surfaces and inverse_surface; the lamp; the
 * containers and primary_fixed, the large areas apps paint, which stay quiet while the text on
 * them gains contrast (stock raises containers against the surface instead); and the surface
 * tint, which tints surfaces.
 *
 * <p>For any other scheme each role is the stock libmonet role, so CONTENT and the clock styles
 * are unchanged.
 */
public final class TallyDynamicColors {
    private static final MaterialDynamicColors MDC = new MaterialDynamicColors();

    private static final Map<String, Role> ROLES = Map.ofEntries(
            // Primary: the light primary is the accent (T40, hue 8° lower for seeds at 55°-110°);
            // the dark primary is T80, or a paler colour above the lamp when T80 is too close.
            text("primary", MDC::primary, SchemeTally::primaryFamily, SchemeTally::primaryTone),
            text("on_primary", MDC::onPrimary, PRIMARY, 100, 20),
            fixed("primary_container", MDC::primaryContainer, SchemeTally::primaryContainer),
            text("on_primary_container", MDC::onPrimaryContainer, PRIMARY, 10, 90),
            text("inverse_primary", MDC::inversePrimary, s -> s.isDark ? ACCENT : PRIMARY,
                    s -> s.isDark ? 40 : 80),
            fixed("surface_tint", MDC::surfaceTint,
                    s -> s.color(s.primaryFamily(), s.primaryTone())),

            // The primary fixed family, one colour each in light and dark, and the lamp.
            fixed("primary_fixed", MDC::primaryFixed, s -> s.color(PRIMARY, 90)),
            fixed("primary_fixed_dim", MDC::primaryFixedDim, SchemeTally::lamp),
            text("on_primary_fixed", MDC::onPrimaryFixed, PRIMARY, 10, 10),
            text("on_primary_fixed_variant", MDC::onPrimaryFixedVariant, PRIMARY, 30, 30),

            // Secondary and tertiary; their containers are quiet (near grey).
            text("secondary", MDC::secondary, SECONDARY, 40, 80),
            text("on_secondary", MDC::onSecondary, SECONDARY, 100, 20),
            fixed("secondary_container", MDC::secondaryContainer,
                    SchemeTally::secondaryContainer),
            text("on_secondary_container", MDC::onSecondaryContainer, SECONDARY, 15, 90),
            text("tertiary", MDC::tertiary, TERTIARY, 40, 80),
            text("on_tertiary", MDC::onTertiary, TERTIARY, 100, 20),
            fixed("tertiary_container", MDC::tertiaryContainer, SchemeTally::tertiaryContainer),
            text("on_tertiary_container", MDC::onTertiaryContainer, TERTIARY, 10, 90),

            // Error
            text("error", MDC::error, ERROR, 40, 80),
            text("on_error", MDC::onError, ERROR, 100, 20),
            fixed("error_container", MDC::errorContainer, s -> s.color(ERROR, s.isDark ? 30 : 90)),
            text("on_error_container", MDC::onErrorContainer, ERROR, 15, 90),

            // Surfaces, text and outlines
            fixed("background", MDC::background, s -> s.color(NEUTRAL, s.isDark ? 6 : 98)),
            text("on_background", MDC::onBackground, NEUTRAL, 10, 92),
            fixed("surface", MDC::surface, s -> s.color(NEUTRAL, s.isDark ? 6 : 98)),
            fixed("surface_dim", MDC::surfaceDim, s -> s.color(NEUTRAL, s.isDark ? 6 : 87)),
            fixed("surface_bright", MDC::surfaceBright, s -> s.color(NEUTRAL, s.isDark ? 24 : 98)),
            fixed("surface_variant", MDC::surfaceVariant,
                    s -> s.color(NEUTRAL, s.isDark ? 22 : 90)),
            fixed("surface_container_lowest", MDC::surfaceContainerLowest,
                    s -> s.color(NEUTRAL, s.isDark ? 4 : 100)),
            fixed("surface_container_low", MDC::surfaceContainerLow,
                    s -> s.color(NEUTRAL, s.isDark ? 10 : 96)),
            fixed("surface_container", MDC::surfaceContainer,
                    s -> s.color(NEUTRAL, s.isDark ? 12 : 94)),
            fixed("surface_container_high", MDC::surfaceContainerHigh,
                    s -> s.color(NEUTRAL, s.isDark ? 17 : 92)),
            fixed("surface_container_highest", MDC::surfaceContainerHighest,
                    s -> s.color(NEUTRAL, s.isDark ? 22 : 90)),
            text("on_surface", MDC::onSurface, NEUTRAL, 10, 92),
            text("on_surface_variant", MDC::onSurfaceVariant, NEUTRAL_VARIANT, 38, 76),
            text("outline", MDC::outline, NEUTRAL_VARIANT, 50, 58),
            text("outline_variant", MDC::outlineVariant, NEUTRAL_VARIANT, 82, 28),
            fixed("inverse_surface", MDC::inverseSurface,
                    s -> s.color(NEUTRAL, s.isDark ? 90 : 20)),
            text("inverse_on_surface", MDC::inverseOnSurface, NEUTRAL, 95, 20));

    private TallyDynamicColors() {}

    /** {@code colors} with each role Tally defines replaced by Tally's. */
    static List<Supplier<DynamicColor>> withTallyRoles(List<Supplier<DynamicColor>> colors) {
        return colors.stream()
                .map(color -> {
                    Role role = ROLES.get(color.get().name);
                    return role == null ? color : (Supplier<DynamicColor>) role::dynamicColor;
                })
                .toList();
    }

    /** Tally's role of this name, or null if Tally does not define it. */
    static DynamicColor forName(String name) {
        Role role = ROLES.get(name);
        return role == null ? null : role.dynamicColor();
    }

    /**
     * A role: in a {@link SchemeTally} its colour is {@code color}'s, with no background, contrast
     * curve or tone pair left for libmonet to apply; in any other scheme it is {@code stock}, the
     * libmonet role. HCT gives the colour back exactly from its own hue, chroma and tone.
     */
    private record Role(Supplier<DynamicColor> stock, Function<SchemeTally, Hct> color) {
        DynamicColor dynamicColor() {
            DynamicColor base = stock.get();
            return new DynamicColor(
                    base.name,
                    s -> s instanceof SchemeTally t
                            ? TonalPalette.fromHct(color.apply(t)) : base.palette.apply(s),
                    s -> s instanceof SchemeTally t
                            ? color.apply(t).getTone() : base.tone.apply(s),
                    base.isBackground,
                    base.chromaMultiplier == null ? null : s -> s instanceof SchemeTally
                            ? Double.valueOf(1) : base.chromaMultiplier.apply(s),
                    base.background == null ? null : stockUnlessTally(base.background),
                    base.secondBackground == null ? null
                            : stockUnlessTally(base.secondBackground),
                    base.contrastCurve == null ? null : stockUnlessTally(base.contrastCurve),
                    base.toneDeltaPair == null ? null : stockUnlessTally(base.toneDeltaPair),
                    base.opacity == null ? null : stockUnlessTally(base.opacity));
        }
    }

    /** A role that holds its colour at every contrast level. */
    private static Map.Entry<String, Role> fixed(String name, Supplier<DynamicColor> stock,
            Function<SchemeTally, Hct> color) {
        return Map.entry(name, new Role(stock, color));
    }

    /** A text or outline role: {@code palette} at the tone {@code light} or {@code dark}. */
    private static Map.Entry<String, Role> text(String name, Supplier<DynamicColor> stock,
            Palette palette, double light, double dark) {
        return text(name, stock, s -> palette, s -> s.isDark ? dark : light);
    }

    private static Map.Entry<String, Role> text(String name, Supplier<DynamicColor> stock,
            Function<SchemeTally, Palette> palette, ToDoubleFunction<SchemeTally> tone) {
        return Map.entry(name, new Role(stock, s -> s.role(name,
                () -> gainContrast(s, stock.get(), palette.apply(s), tone.applyAsDouble(s)))));
    }

    /**
     * A text or outline role's colour: its Tally colour, {@code palette} at {@code tone}; or, where
     * that gives less than the ratio libmonet's role {@code stock} asks for at this contrast level
     * against what it is drawn on, the colour of {@code palette} just far enough away from it.
     */
    private static Hct gainContrast(SchemeTally s, DynamicColor stock, Palette palette,
            double tone) {
        Hct tally = s.color(palette, tone);
        ContrastCurve curve = stock.contrastCurve == null ? null : stock.contrastCurve.apply(s);
        if (s.contrastLevel <= 0 || curve == null) {
            return tally;
        }
        double level = s.contrastLevel;
        double moved = tally.getTone();
        for (Function<DynamicScheme, DynamicColor> on :
                Arrays.asList(stock.background, stock.secondBackground)) {
            DynamicColor background = on == null ? null : on.apply(s);
            if (background == null) {
                continue;
            }
            double backgroundTone = tone(s, background);
            double own = Contrast.ratioOfTones(tally.getTone(), backgroundTone);
            double wanted = level < 0.5
                    ? own + (curve.get(0.5) - own) * level / 0.5 : curve.get(level);
            if (Contrast.ratioOfTones(moved, backgroundTone) < wanted) {
                moved = moved > backgroundTone
                        ? Math.max(moved, Contrast.lighterUnsafe(backgroundTone, wanted))
                        : Math.min(moved, Contrast.darkerUnsafe(backgroundTone, wanted));
            }
        }
        return moved == tally.getTone() ? tally : s.color(palette, moved);
    }

    /** The tone of {@code role} in {@code s}: Tally's where Tally defines the role. */
    private static double tone(SchemeTally s, DynamicColor role) {
        Role tally = ROLES.get(role.name);
        return tally != null ? tally.color().apply(s).getTone() : role.getTone(s);
    }

    private static <T> Function<DynamicScheme, T> stockUnlessTally(
            Function<DynamicScheme, T> stock) {
        return s -> s instanceof SchemeTally ? null : stock.apply(s);
    }
}
