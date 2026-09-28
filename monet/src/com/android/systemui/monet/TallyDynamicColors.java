/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 The DiamaneOS Project
 */

package com.android.systemui.monet;

import static com.android.systemui.monet.SchemeTally.Palette.ERROR;
import static com.android.systemui.monet.SchemeTally.Palette.NEUTRAL;
import static com.android.systemui.monet.SchemeTally.Palette.NEUTRAL_VARIANT;
import static com.android.systemui.monet.SchemeTally.Palette.PRIMARY;
import static com.android.systemui.monet.SchemeTally.Palette.SECONDARY;
import static com.android.systemui.monet.SchemeTally.Palette.TERTIARY;

import com.google.ux.material.libmonet.dynamiccolor.DynamicColor;
import com.google.ux.material.libmonet.dynamiccolor.DynamicScheme;
import com.google.ux.material.libmonet.dynamiccolor.MaterialDynamicColors;
import com.google.ux.material.libmonet.hct.Hct;
import com.google.ux.material.libmonet.palettes.TonalPalette;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The roles of the DiamaneOS palette style ({@link SchemeTally}): the roles the Tally prototype
 * hands to apps ({@code T.appRoles}), each the prototype's colour at a fixed tone in the light and
 * the dark scheme, the same at every contrast level. The shell's tokens read these roles and the
 * palette stops (diamaneos-design, design/tokens).
 *
 * <p>The roles apps pair with them hold their tones too, at the 2021 spec's tones for standard
 * contrast: the other surfaces (background, dim, bright; surface variant as the highest
 * container, which Material 3 now uses in its place), the surface tint (the primary) and the rest
 * of the primary fixed family around the lamp. Left to the contrast curves, only those would move
 * at higher contrast and the pairs would lose contrast, for example white on-primary-fixed text on
 * the lamp.
 *
 * <p>For any other scheme each role is the stock libmonet role, so CONTENT and the clock styles
 * are unchanged.
 */
public final class TallyDynamicColors {
    private static final MaterialDynamicColors MDC = new MaterialDynamicColors();

    private static final Map<String, Supplier<DynamicColor>> ROLES = Map.ofEntries(
            Map.entry("primary", TallyDynamicColors::primary),
            Map.entry("on_primary", TallyDynamicColors::onPrimary),
            Map.entry("primary_container", TallyDynamicColors::primaryContainer),
            Map.entry("on_primary_container", TallyDynamicColors::onPrimaryContainer),
            Map.entry("inverse_primary", TallyDynamicColors::inversePrimary),
            Map.entry("surface_tint", TallyDynamicColors::surfaceTint),
            Map.entry("primary_fixed", TallyDynamicColors::primaryFixed),
            Map.entry("primary_fixed_dim", TallyDynamicColors::primaryFixedDim),
            Map.entry("on_primary_fixed", TallyDynamicColors::onPrimaryFixed),
            Map.entry("on_primary_fixed_variant", TallyDynamicColors::onPrimaryFixedVariant),
            Map.entry("secondary", TallyDynamicColors::secondary),
            Map.entry("on_secondary", TallyDynamicColors::onSecondary),
            Map.entry("secondary_container", TallyDynamicColors::secondaryContainer),
            Map.entry("on_secondary_container", TallyDynamicColors::onSecondaryContainer),
            Map.entry("tertiary", TallyDynamicColors::tertiary),
            Map.entry("on_tertiary", TallyDynamicColors::onTertiary),
            Map.entry("tertiary_container", TallyDynamicColors::tertiaryContainer),
            Map.entry("on_tertiary_container", TallyDynamicColors::onTertiaryContainer),
            Map.entry("error", TallyDynamicColors::error),
            Map.entry("on_error", TallyDynamicColors::onError),
            Map.entry("error_container", TallyDynamicColors::errorContainer),
            Map.entry("on_error_container", TallyDynamicColors::onErrorContainer),
            Map.entry("background", TallyDynamicColors::background),
            Map.entry("on_background", TallyDynamicColors::onBackground),
            Map.entry("surface", TallyDynamicColors::surface),
            Map.entry("surface_dim", TallyDynamicColors::surfaceDim),
            Map.entry("surface_bright", TallyDynamicColors::surfaceBright),
            Map.entry("surface_variant", TallyDynamicColors::surfaceVariant),
            Map.entry("surface_container_lowest", TallyDynamicColors::surfaceContainerLowest),
            Map.entry("surface_container_low", TallyDynamicColors::surfaceContainerLow),
            Map.entry("surface_container", TallyDynamicColors::surfaceContainer),
            Map.entry("surface_container_high", TallyDynamicColors::surfaceContainerHigh),
            Map.entry("surface_container_highest", TallyDynamicColors::surfaceContainerHighest),
            Map.entry("on_surface", TallyDynamicColors::onSurface),
            Map.entry("on_surface_variant", TallyDynamicColors::onSurfaceVariant),
            Map.entry("outline", TallyDynamicColors::outline),
            Map.entry("outline_variant", TallyDynamicColors::outlineVariant),
            Map.entry("inverse_surface", TallyDynamicColors::inverseSurface),
            Map.entry("inverse_on_surface", TallyDynamicColors::inverseOnSurface));

    private TallyDynamicColors() {}

    /** {@code colors} with each role Tally defines replaced by Tally's. */
    static List<Supplier<DynamicColor>> withTallyRoles(List<Supplier<DynamicColor>> colors) {
        return colors.stream()
                .map(color -> ROLES.getOrDefault(color.get().name, color))
                .toList();
    }

    /** Tally's role of this name, or null if Tally does not define it. */
    static DynamicColor forName(String name) {
        Supplier<DynamicColor> role = ROLES.get(name);
        return role == null ? null : role.get();
    }

    // Primary: the light primary is the accent (T40, hue 8° lower for seeds at 55°-110°); the
    // dark primary is T80, or a paler colour above the lamp when T80 is too close to it.

    public static DynamicColor primary() {
        return role(MDC.primary(), s -> s.isDark ? s.darkPrimary() : s.lightPrimary());
    }

    public static DynamicColor onPrimary() {
        return role(MDC.onPrimary(), s -> s.color(PRIMARY, s.isDark ? 20 : 100));
    }

    public static DynamicColor primaryContainer() {
        return role(MDC.primaryContainer(), SchemeTally::primaryContainer);
    }

    public static DynamicColor onPrimaryContainer() {
        return role(MDC.onPrimaryContainer(), s -> s.color(PRIMARY, s.isDark ? 90 : 10));
    }

    public static DynamicColor inversePrimary() {
        return role(MDC.inversePrimary(),
                s -> s.isDark ? s.lightPrimary() : s.color(PRIMARY, 80));
    }

    public static DynamicColor surfaceTint() {
        return role(MDC.surfaceTint(), s -> s.isDark ? s.darkPrimary() : s.lightPrimary());
    }

    // The primary fixed family, one colour each in light and dark.

    public static DynamicColor primaryFixed() {
        return role(MDC.primaryFixed(), s -> s.color(PRIMARY, 90));
    }

    /** The lamp: the seed's own tone held to T74-T82. */
    public static DynamicColor primaryFixedDim() {
        return role(MDC.primaryFixedDim(), SchemeTally::lamp);
    }

    public static DynamicColor onPrimaryFixed() {
        return role(MDC.onPrimaryFixed(), s -> s.color(PRIMARY, 10));
    }

    public static DynamicColor onPrimaryFixedVariant() {
        return role(MDC.onPrimaryFixedVariant(), s -> s.color(PRIMARY, 30));
    }

    // Secondary and tertiary; their containers are quiet (near grey).

    public static DynamicColor secondary() {
        return role(MDC.secondary(), s -> s.color(SECONDARY, s.isDark ? 80 : 40));
    }

    public static DynamicColor onSecondary() {
        return role(MDC.onSecondary(), s -> s.color(SECONDARY, s.isDark ? 20 : 100));
    }

    public static DynamicColor secondaryContainer() {
        return role(MDC.secondaryContainer(), SchemeTally::secondaryContainer);
    }

    public static DynamicColor onSecondaryContainer() {
        return role(MDC.onSecondaryContainer(), s -> s.color(SECONDARY, s.isDark ? 90 : 15));
    }

    public static DynamicColor tertiary() {
        return role(MDC.tertiary(), s -> s.color(TERTIARY, s.isDark ? 80 : 40));
    }

    public static DynamicColor onTertiary() {
        return role(MDC.onTertiary(), s -> s.color(TERTIARY, s.isDark ? 20 : 100));
    }

    public static DynamicColor tertiaryContainer() {
        return role(MDC.tertiaryContainer(), SchemeTally::tertiaryContainer);
    }

    public static DynamicColor onTertiaryContainer() {
        return role(MDC.onTertiaryContainer(), s -> s.color(TERTIARY, s.isDark ? 90 : 10));
    }

    // Error

    public static DynamicColor error() {
        return role(MDC.error(), s -> s.color(ERROR, s.isDark ? 80 : 40));
    }

    public static DynamicColor onError() {
        return role(MDC.onError(), s -> s.color(ERROR, s.isDark ? 20 : 100));
    }

    public static DynamicColor errorContainer() {
        return role(MDC.errorContainer(), s -> s.color(ERROR, s.isDark ? 30 : 90));
    }

    public static DynamicColor onErrorContainer() {
        return role(MDC.onErrorContainer(), s -> s.color(ERROR, s.isDark ? 90 : 15));
    }

    // Surfaces, text and outlines

    public static DynamicColor background() {
        return role(MDC.background(), s -> s.color(NEUTRAL, s.isDark ? 6 : 98));
    }

    public static DynamicColor onBackground() {
        return role(MDC.onBackground(), s -> s.color(NEUTRAL, s.isDark ? 92 : 10));
    }

    public static DynamicColor surface() {
        return role(MDC.surface(), s -> s.color(NEUTRAL, s.isDark ? 6 : 98));
    }

    public static DynamicColor surfaceDim() {
        return role(MDC.surfaceDim(), s -> s.color(NEUTRAL, s.isDark ? 6 : 87));
    }

    public static DynamicColor surfaceBright() {
        return role(MDC.surfaceBright(), s -> s.color(NEUTRAL, s.isDark ? 24 : 98));
    }

    public static DynamicColor surfaceVariant() {
        return role(MDC.surfaceVariant(), s -> s.color(NEUTRAL, s.isDark ? 22 : 90));
    }

    public static DynamicColor surfaceContainerLowest() {
        return role(MDC.surfaceContainerLowest(), s -> s.color(NEUTRAL, s.isDark ? 4 : 100));
    }

    public static DynamicColor surfaceContainerLow() {
        return role(MDC.surfaceContainerLow(), s -> s.color(NEUTRAL, s.isDark ? 10 : 96));
    }

    public static DynamicColor surfaceContainer() {
        return role(MDC.surfaceContainer(), s -> s.color(NEUTRAL, s.isDark ? 12 : 94));
    }

    public static DynamicColor surfaceContainerHigh() {
        return role(MDC.surfaceContainerHigh(), s -> s.color(NEUTRAL, s.isDark ? 17 : 92));
    }

    public static DynamicColor surfaceContainerHighest() {
        return role(MDC.surfaceContainerHighest(), s -> s.color(NEUTRAL, s.isDark ? 22 : 90));
    }

    public static DynamicColor onSurface() {
        return role(MDC.onSurface(), s -> s.color(NEUTRAL, s.isDark ? 92 : 10));
    }

    public static DynamicColor onSurfaceVariant() {
        return role(MDC.onSurfaceVariant(), s -> s.color(NEUTRAL_VARIANT, s.isDark ? 76 : 38));
    }

    public static DynamicColor outline() {
        return role(MDC.outline(), s -> s.color(NEUTRAL_VARIANT, s.isDark ? 58 : 50));
    }

    public static DynamicColor outlineVariant() {
        return role(MDC.outlineVariant(), s -> s.color(NEUTRAL_VARIANT, s.isDark ? 28 : 82));
    }

    public static DynamicColor inverseSurface() {
        return role(MDC.inverseSurface(), s -> s.color(NEUTRAL, s.isDark ? 90 : 20));
    }

    public static DynamicColor inverseOnSurface() {
        return role(MDC.inverseOnSurface(), s -> s.color(NEUTRAL, s.isDark ? 20 : 95));
    }

    /**
     * A role that is {@code color} in a {@link SchemeTally}, with no background, contrast curve or
     * tone pair, so it holds at every contrast level; in any other scheme it is {@code stock}. HCT
     * gives the colour back exactly from its own hue, chroma and tone.
     */
    private static DynamicColor role(DynamicColor stock, Function<SchemeTally, Hct> color) {
        return new DynamicColor(
                stock.name,
                s -> s instanceof SchemeTally t
                        ? TonalPalette.fromHct(color.apply(t)) : stock.palette.apply(s),
                s -> s instanceof SchemeTally t ? color.apply(t).getTone() : stock.tone.apply(s),
                stock.isBackground,
                stock.chromaMultiplier == null ? null : s -> s instanceof SchemeTally
                        ? Double.valueOf(1) : stock.chromaMultiplier.apply(s),
                stock.background == null ? null : stockUnlessTally(stock.background),
                stock.secondBackground == null ? null : stockUnlessTally(stock.secondBackground),
                stock.contrastCurve == null ? null : stockUnlessTally(stock.contrastCurve),
                stock.toneDeltaPair == null ? null : stockUnlessTally(stock.toneDeltaPair),
                stock.opacity == null ? null : stockUnlessTally(stock.opacity));
    }

    private static <T> Function<DynamicScheme, T> stockUnlessTally(
            Function<DynamicScheme, T> stock) {
        return s -> s instanceof SchemeTally ? null : stock.apply(s);
    }
}
