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

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Process
import android.os.UserHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.launcher3.icons.BaseIconFactory
import com.android.launcher3.icons.BaseIconFactory.IconOptions
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.icons.IconProvider
import com.android.launcher3.icons.PersistedItemState
import com.android.launcher3.icons.SourceHint
import com.android.launcher3.icons.ThemedBitmap
import com.android.launcher3.icons.cache.CachingLogic
import com.android.launcher3.icons.cache.IconLoadRequest
import com.android.launcher3.icons.mono.ThemedIconInfo
import com.android.launcher3.util.ComponentKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TallyColourIconThemeControllerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val keys =
        TallyAppKeys(
            mapOf(
                SYSTEM_APP to TallyAppKeys.Key(plate = PLATE, glyph = Color.WHITE),
                INSTALLED_APP to TallyAppKeys.Key(plate = PLATE, glyph = Color.WHITE),
            )
        )

    private val controller =
        TallyColourIconThemeController(keys, isSystemApp = { _, pkg -> pkg == SYSTEM_APP })

    private val factory = BaseIconFactory(context, 160, 48, themeController = controller)

    /** An adaptive icon with a monochrome layer: a white plate, an inset glyph. */
    private val icon =
        AdaptiveIconDrawable(
            ColorDrawable(Color.WHITE),
            InsetDrawable(ColorDrawable(Color.BLACK), 0.3f),
            InsetDrawable(ColorDrawable(Color.BLACK), 0.3f),
        )

    private fun hintOf(packageName: String) =
        SourceHint(
            ComponentKey(ComponentName(packageName, "$packageName.Main"), Process.myUserHandle()),
            NoCachingLogic,
        )

    private fun iconOf(packageName: String): BitmapInfo =
        factory.createBadgedIconBitmap(icon, IconOptions().setSourceHint(hintOf(packageName)))

    @Test
    fun `a listed system app is drawn on its key, in the key's colours`() {
        val info = iconOf(SYSTEM_APP)
        val drawn = info.themedBitmap!!.newDelegateFactory(info, context) as ThemedIconInfo
        assertEquals(PLATE, drawn.colorBg)
        assertEquals(Color.WHITE, drawn.colorFg)
    }

    @Test
    fun `an app with no key keeps its own icon`() {
        assertSame(ThemedBitmap.NOT_SUPPORTED, iconOf(OTHER_APP).themedBitmap)
        assertNull(controller.forPackage(context, OTHER_APP))
    }

    @Test
    fun `a listed app that is not a system app keeps its own icon`() {
        assertSame(ThemedBitmap.NOT_SUPPORTED, iconOf(INSTALLED_APP).themedBitmap)
        assertNull(controller.forPackage(context, INSTALLED_APP))
    }

    @Test
    fun `an icon with no source hint keeps its own icon`() {
        assertSame(ThemedBitmap.NOT_SUPPORTED, factory.createBadgedIconBitmap(icon).themedBitmap)
    }

    @Test
    fun `an app's own controller draws its key`() {
        val own = controller.forPackage(context, SYSTEM_APP)
        assertNotNull(own)
        val info =
            BaseIconFactory(context, 160, 48, themeController = own).createBadgedIconBitmap(icon)
        val drawn = info.themedBitmap!!.newDelegateFactory(info, context) as ThemedIconInfo
        assertEquals(PLATE, drawn.colorBg)
        assertEquals(Color.WHITE, drawn.colorFg)
    }

    @Test
    fun `a key keeps its colours through the icon cache`() {
        val info = iconOf(SYSTEM_APP)
        val decoded =
            controller.decode(info.themedBitmap!!.serialize(), info, factory, hintOf(SYSTEM_APP))
        val drawn = decoded.newDelegateFactory(info, context) as ThemedIconInfo
        assertEquals(PLATE, drawn.colorBg)
        assertEquals(Color.WHITE, drawn.colorFg)
    }

    @Test
    fun `the arrays load as keys`() {
        assertEquals(0, TallyAppKeys.EMPTY.size)
        assertEquals(Color.WHITE, keys[SYSTEM_APP]?.glyph)
        assertNull(keys[OTHER_APP])
    }

    /** A source hint's caching logic, which the controller never calls. */
    private object NoCachingLogic : CachingLogic<Any> {
        override fun getComponent(item: Any): ComponentName = error("not used")

        override fun getUser(item: Any): UserHandle = error("not used")

        override fun getLabel(item: Any): CharSequence? = error("not used")

        override fun getApplicationInfo(item: Any): ApplicationInfo? = error("not used")

        override fun loadIcon(request: IconLoadRequest<Any>): BitmapInfo = error("not used")

        override fun getFreshnessIdentifier(
            item: Any,
            iconProvider: IconProvider,
        ): PersistedItemState? = error("not used")
    }

    private companion object {
        const val SYSTEM_APP = "com.example.system"
        const val INSTALLED_APP = "com.example.installed"
        const val OTHER_APP = "com.example.other"
        const val PLATE = 0xFF2E7D32.toInt()
    }
}
