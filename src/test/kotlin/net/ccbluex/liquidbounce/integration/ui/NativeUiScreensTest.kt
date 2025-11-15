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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for native UI screens to ensure they can be instantiated
 */
class NativeUiScreensTest {

    @Test
    fun `NativeClickGuiScreen can be instantiated`() {
        // Verify the class can be instantiated without errors
        val screen = NativeClickGuiScreen()
        assertNotNull(screen)
        assertEquals("ClickGUI", screen.title.string)
        assertFalse(screen.shouldPause())
        assertTrue(screen.shouldCloseOnEsc())
    }

    @Test
    fun `NativeAltManagerScreen can be instantiated`() {
        val screen = NativeAltManagerScreen(null)
        assertNotNull(screen)
        assertEquals("Alt Manager", screen.title.string)
        assertTrue(screen.shouldPause())
    }

    @Test
    fun `NativeProxyManagerScreen can be instantiated`() {
        val screen = NativeProxyManagerScreen(null)
        assertNotNull(screen)
        assertEquals("Proxy Manager", screen.title.string)
        assertTrue(screen.shouldPause())
    }
}
