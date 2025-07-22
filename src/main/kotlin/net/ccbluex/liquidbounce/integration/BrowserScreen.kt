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
package net.ccbluex.liquidbounce.integration

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserUrlChangeEvent
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
// import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager // Removed: No longer using browser backend
// import net.ccbluex.liquidbounce.integration.backend.browser.Browser // Removed: No longer using browser backend
// import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport // Removed: No longer using browser backend
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

// Stub classes to replace browser backend since we're using native GUI
data class BrowserViewport(val x: Int, val y: Int, val width: Int, val height: Int)

interface Browser {
    var viewport: BrowserViewport
    var url: String
    fun close()
    fun reload()
    fun forceReload()
    fun goForward()
    fun goBack()
}

var browserBrowsers = mutableListOf<Browser>()

class BrowserScreen(val url: String, title: Text = "".asText()) : Screen(title) {

    // todo: implement multi-tab support and tab switching
    var selectedIndex = 0
    private var recentUrl = url

    val browserBrowser: Browser?
        get() = browserBrowsers.getOrNull(selectedIndex)

    override fun init() {
        val viewport = BrowserViewport(
            20,
            20,
            ((width - 20) * mc.window.scaleFactor).toInt(),
            ((height - 50) * mc.window.scaleFactor).toInt()
        )

        if (browserBrowsers.isEmpty()) {
            // Note: Browser creation removed - native GUI doesn't use browsers
            // Create a stub browser object to prevent crashes
            val stubBrowser = object : Browser {
                override var viewport: BrowserViewport = viewport
                override var url: String = this@BrowserScreen.url
                override fun close() {}
                override fun reload() {}
                override fun forceReload() {}
                override fun goForward() {}
                override fun goBack() {}
            }
            browserBrowsers.add(stubBrowser)
            return
        }

        // Update the position of all tabs
        browserBrowsers.forEach { browser -> browser.viewport = viewport }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        browserBrowser?.let { browser ->
            val currentUrl = browser.url

            if (recentUrl != currentUrl) {
                EventManager.callEvent(BrowserUrlChangeEvent(selectedIndex, currentUrl))
                recentUrl = currentUrl
            }
        }

        // render nothing
    }

    override fun shouldPause() = false

    override fun close() {
        // Close all tabs
        browserBrowsers.removeIf {
            it.close()
            true
        }

        super.close()
    }

    override fun shouldCloseOnEsc() = true

}
