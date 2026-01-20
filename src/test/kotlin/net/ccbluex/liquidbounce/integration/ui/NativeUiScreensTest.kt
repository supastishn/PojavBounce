/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.integration.ui

import net.ccbluex.liquidbounce.integration.ui.altmanager.NativeAltManagerScreen
import net.ccbluex.liquidbounce.integration.ui.clickgui.NativeClickGuiScreen
import net.ccbluex.liquidbounce.integration.ui.proxymanager.NativeProxyManagerScreen
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Tests for native UI screens
 *
 * Note: These tests verify that the classes are accessible without requiring
 * Minecraft runtime context for instantiation.
 */
class NativeUiScreensTest {

    @Test
    fun `Native UI screen classes exist and are accessible`() {
        // Verify the classes are accessible using class references
        // (actual instantiation requires Minecraft runtime)
        val clickGuiClass = NativeClickGuiScreen::class.java
        val altManagerClass = NativeAltManagerScreen::class.java
        val proxyManagerClass = NativeProxyManagerScreen::class.java

        assertNotNull(clickGuiClass, "NativeClickGuiScreen class should exist")
        assertNotNull(altManagerClass, "NativeAltManagerScreen class should exist")
        assertNotNull(proxyManagerClass, "NativeProxyManagerScreen class should exist")
    }
}
