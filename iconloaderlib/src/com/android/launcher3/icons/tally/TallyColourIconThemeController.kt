/*
 * Copyright (C) 2026 The DiamaneOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.icons.tally

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import com.android.launcher3.icons.BaseIconFactory
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.icons.IconThemeController
import com.android.launcher3.icons.SourceHint
import com.android.launcher3.icons.ThemedBitmap
import com.android.launcher3.icons.mono.ColorList
import com.android.launcher3.icons.mono.MonoIconThemeController
import com.android.launcher3.icons.mono.MonoThemedBitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * DiamaneOS Tally's Colour icon style, its default (the prototype's "each app its own colour, white
 * glyph"): each of DiamaneOS's own apps ([TallyAppKeys], from the Tally tokens) is a key in its own
 * colour with its glyph in the key's glyph colour, the same in light and dark. Every other app
 * keeps its own icon, as with no style, and so does a listed app that is not a system app: an app
 * installed under a listed name never looks like one of the system's own.
 *
 * The glyph is the app's monochrome layer, or the one the Minimal style makes for an icon without
 * one ([TallyMonoIconThemeController]); a clock icon keeps its moving hands. The key's colours are
 * fixed: a generated glyph's colours are not adapted for contrast, as stock adapts a theme's.
 *
 * Launcher sets this controller on its icon factory and finds each icon's app from the icon's
 * source hint. SystemUI, which draws one app's icon at a time, sets the app's own controller from
 * [forPackage] on the factory instead.
 */
class TallyColourIconThemeController
@JvmOverloads
constructor(
    private val keys: TallyAppKeys,
    /** Whether a package is a system app; may read the package manager. */
    private val isSystemApp: (Context, String) -> Boolean = ::isSystemPackage,
) : IconThemeController {

    override val themeID = THEME_ID

    /** Each listed package's controller, or [NO_KEY], once looked up. */
    private val controllers = ConcurrentHashMap<String, Any>()

    /**
     * The controller that draws [packageName]'s key, or null when the app keeps its own icon: it
     * has no key, or it is not a system app. May read the package manager, so call it off the main
     * thread.
     */
    fun forPackage(context: Context, packageName: String): IconThemeController? {
        val key = keys[packageName] ?: return null
        val controller =
            controllers.getOrPut(packageName) {
                if (isSystemApp(context, packageName)) KeyController(key) else NO_KEY
            }
        return controller as? IconThemeController
    }

    override fun createThemedBitmap(
        icon: AdaptiveIconDrawable,
        info: BitmapInfo,
        factory: BaseIconFactory,
        sourceHint: SourceHint?,
    ): ThemedBitmap =
        controllerFor(factory, sourceHint)?.createThemedBitmap(icon, info, factory, sourceHint)
            ?: ThemedBitmap.NOT_SUPPORTED

    override fun decode(
        bytes: ByteArray,
        info: BitmapInfo,
        factory: BaseIconFactory,
        sourceHint: SourceHint,
    ): ThemedBitmap =
        controllerFor(factory, sourceHint)?.decode(bytes, info, factory, sourceHint)
            ?: ThemedBitmap.NOT_SUPPORTED

    override fun createThemedAdaptiveIcon(
        context: Context,
        originalIcon: AdaptiveIconDrawable,
        info: BitmapInfo?,
    ): AdaptiveIconDrawable? {
        // An app's own icon stays its own; a key is drawn by its app's controller.
        val keyed = info?.themedBitmap as? KeyBitmap ?: return originalIcon
        return keyed.controller.createThemedAdaptiveIcon(context, originalIcon, info)
    }

    private fun controllerFor(factory: BaseIconFactory, sourceHint: SourceHint?) =
        sourceHint?.key?.componentName?.packageName?.let { forPackage(factory.context, it) }

    /**
     * One app's key: the Minimal style's glyph ([TallyMonoIconThemeController]) on the key's plate,
     * in the key's own colours.
     */
    private class KeyController(key: TallyAppKeys.Key) : IconThemeController {
        private val colours =
            ColorList(
                iconBackgroundColor = key.plate,
                iconForegroundColor = key.glyph,
                iconAdaptiveBackgroundColor = key.plate,
                badgeBackgroundColor = key.plate,
                badgeForegroundColor = key.glyph,
            )
        private val colourProvider: (Context) -> ColorList = { colours }
        private val mono =
            MonoIconThemeController(shouldForceThemeIcon = true, colorProvider = colourProvider)
        private val glyphs = TallyMonoIconThemeController(mono)

        override val themeID = THEME_ID

        override fun createThemedBitmap(
            icon: AdaptiveIconDrawable,
            info: BitmapInfo,
            factory: BaseIconFactory,
            sourceHint: SourceHint?,
        ): ThemedBitmap = keyed(glyphs.createThemedBitmap(icon, info, factory, sourceHint))

        override fun decode(
            bytes: ByteArray,
            info: BitmapInfo,
            factory: BaseIconFactory,
            sourceHint: SourceHint,
        ): ThemedBitmap = keyed(mono.decode(bytes, info, factory, sourceHint))

        override fun createThemedAdaptiveIcon(
            context: Context,
            originalIcon: AdaptiveIconDrawable,
            info: BitmapInfo?,
        ): AdaptiveIconDrawable? {
            // Stock reads a generated glyph from the info's themed bitmap: hand it the key's.
            val unwrapped =
                info?.let { i ->
                    (i.themedBitmap as? KeyBitmap)?.let { i.copy(themedBitmap = it.base) } ?: i
                }
            return mono.createThemedAdaptiveIcon(context, originalIcon, unwrapped)
        }

        /**
         * The themed bitmap in the key's own colours: a generated glyph keeps no contrast
         * adaptation (its luminance difference), so its colours stay the key's.
         */
        private fun keyed(themed: ThemedBitmap): ThemedBitmap =
            when {
                themed === ThemedBitmap.NOT_SUPPORTED -> themed
                themed is MonoThemedBitmap ->
                    KeyBitmap(MonoThemedBitmap(themed.mono, colourProvider), this)
                else -> KeyBitmap(themed, this)
            }
    }

    /** A key's themed bitmap, with the controller that drew it. */
    private class KeyBitmap(val base: ThemedBitmap, val controller: IconThemeController) :
        ThemedBitmap by base

    companion object {
        /** The style's theme ID, part of the value Launcher stores for it. */
        const val THEME_ID = "colour"

        private val NO_KEY = Any()

        /** Whether [packageName] is a system app (an updated one included). */
        @JvmStatic
        fun isSystemPackage(context: Context, packageName: String): Boolean =
            try {
                val info =
                    context.packageManager.getApplicationInfo(
                        packageName,
                        PackageManager.MATCH_UNINSTALLED_PACKAGES,
                    )
                (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
    }
}
