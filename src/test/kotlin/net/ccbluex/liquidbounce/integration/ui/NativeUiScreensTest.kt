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

import org.junit.jupiter.api.Test

/**
 * Tests for native UI screens
 *
 * Note: These tests require Minecraft runtime context and cannot be run in unit test mode.
 * They are kept as placeholders for integration testing.
 */
class NativeUiScreensTest {

    @Test
    fun `Native UI screen classes exist and are accessible`() {
        // Verify the classes are accessible without instantiation
        // (actual instantiation requires Minecraft runtime)
        val clickGuiClass = Class.forName(
            "net.ccbluex.liquidbounce.integration.ui.clickgui.NativeClickGuiScreen"
        )
        val altManagerClass = Class.forName(
            "net.ccbluex.liquidbounce.integration.ui.altmanager.NativeAltManagerScreen"
        )
        val proxyManagerClass = Class.forName(
            "net.ccbluex.liquidbounce.integration.ui.proxymanager.NativeProxyManagerScreen"
        )

        assert(clickGuiClass != null) { "NativeClickGuiScreen class should exist" }
        assert(altManagerClass != null) { "NativeAltManagerScreen class should exist" }
        assert(proxyManagerClass != null) { "NativeProxyManagerScreen class should exist" }
    }
}
