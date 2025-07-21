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
package net.ccbluex.liquidbounce.integration.backend.backends.minecraft

import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Basic test for MinecraftBrowser functionality
 */
class MinecraftBrowserTest {

    @Test
    fun testMinecraftBrowserCreation() {
        val backend = MinecraftBrowserBackend()
        val viewport = BrowserViewport(0, 0, 800, 600)
        val settings = BrowserSettings(60) // 60 FPS
        
        val browser = MinecraftBrowser(
            backend = backend,
            initialUrl = "https://example.com",
            viewport = viewport,
            settings = settings,
            priority = 1
        )
        
        assertEquals("https://example.com", browser.url)
        assertEquals(viewport, browser.viewport)
        assertTrue(browser.visible)
        assertEquals(1, browser.priority)
        assertNull(browser.texture) // Minecraft UI doesn't provide texture
    }

    @Test
    fun testMinecraftBrowserBackend() {
        val backend = MinecraftBrowserBackend()
        
        assertTrue(backend.isInitialized)
        assertFalse(backend.isAccelerationSupported)
        assertTrue(backend.browsers.isEmpty())
        
        val browser = backend.createBrowser("https://test.com")
        assertEquals(1, backend.browsers.size)
        assertEquals("https://test.com", browser.url)
        
        browser.close()
        assertTrue(backend.browsers.isEmpty())
    }

    @Test
    fun testBrowserNavigation() {
        val backend = MinecraftBrowserBackend()
        val browser = backend.createBrowser("https://initial.com")
        
        browser.url = "https://second.com"
        assertEquals("https://second.com", browser.url)
        
        browser.url = "https://third.com"
        assertEquals("https://third.com", browser.url)
        
        // Test history navigation
        browser.goBack()
        assertEquals("https://second.com", browser.url)
        
        browser.goBack()
        assertEquals("https://initial.com", browser.url)
        
        browser.goForward()
        assertEquals("https://second.com", browser.url)
    }
}