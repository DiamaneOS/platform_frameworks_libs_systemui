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

import android.content.res.Resources
import android.graphics.Color
import android.util.Log
import androidx.annotation.ArrayRes
import androidx.annotation.ColorInt

/**
 * The keys of DiamaneOS Tally's Colour icon style: each of DiamaneOS's own apps, by package, is a
 * keycap in its own colour with its glyph in another, the same in light and dark.
 *
 * The table is the Tally tokens' (`tally_app_key_packages`, `tally_app_key_plates` and
 * `tally_app_key_glyphs`, three arrays in the same order), which Launcher and SystemUI link and
 * read from their own resources with [load]; this library does not link the tokens.
 */
class TallyAppKeys(private val keys: Map<String, Key>) {

    /** One app's key: the colour of its plate and of its glyph. */
    data class Key(@ColorInt val plate: Int, @ColorInt val glyph: Int)

    /** The key of [packageName], or null for an app that keeps its own icon. */
    operator fun get(packageName: String): Key? = keys[packageName]

    /** The number of apps with a key. */
    val size: Int
        get() = keys.size

    companion object {
        private const val TAG = "TallyAppKeys"

        @JvmField val EMPTY = TallyAppKeys(emptyMap())

        /**
         * The table in [res]: the apps' packages ([packages], strings), and their plates' and
         * glyphs' colours ([plates], [glyphs]), in the same order. Arrays that differ in length, or
         * a colour that is not opaque, give no keys at all rather than keys that may be wrong.
         */
        @JvmStatic
        fun load(
            res: Resources,
            @ArrayRes packages: Int,
            @ArrayRes plates: Int,
            @ArrayRes glyphs: Int,
        ): TallyAppKeys {
            val names = res.getStringArray(packages)
            val plateColours = res.obtainTypedArray(plates)
            val glyphColours = res.obtainTypedArray(glyphs)
            try {
                if (plateColours.length() != names.size || glyphColours.length() != names.size) {
                    Log.e(TAG, "The app key arrays differ in length: no app keys")
                    return EMPTY
                }
                val keys = HashMap<String, Key>(names.size)
                for (i in names.indices) {
                    val key = Key(plateColours.getColor(i, 0), glyphColours.getColor(i, 0))
                    if (Color.alpha(key.plate) != 255 || Color.alpha(key.glyph) != 255) {
                        Log.e(TAG, "The key of ${names[i]} is not opaque: no app keys")
                        return EMPTY
                    }
                    keys[names[i]] = key
                }
                return TallyAppKeys(keys)
            } finally {
                plateColours.recycle()
                glyphColours.recycle()
            }
        }
    }
}
