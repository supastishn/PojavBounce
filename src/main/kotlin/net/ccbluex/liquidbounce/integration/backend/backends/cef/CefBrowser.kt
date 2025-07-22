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
package net.ccbluex.liquidbounce.integration.backend.backends.cef

import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport

/**
 * Stub implementation of CEF Browser for native GUI migration
 * 
 * This replaces the original CEF/JCEF browser functionality with no-op stubs
 * since the native GUI doesn't require embedded browsers.
 */
class CefBrowser(
    initialUrl: String,
    initialWidth: Int = 0,
    initialHeight: Int = 0
) : Browser {
    
    private var stubViewport = BrowserViewport(0, 0, initialWidth, initialHeight)
    
    override var viewport: BrowserViewport
        get() = stubViewport
        set(value) {
            stubViewport = value
            // No-op for native GUI
        }
    
    override var visible: Boolean = false
    override var priority: Short = 0
    override var url: String = initialUrl
    override val texture: net.ccbluex.liquidbounce.integration.backend.BrowserTexture? = null
    
    override fun reload() {
        // No-op for native GUI
    }
    
    override fun forceReload() {
        // No-op for native GUI
    }
    
    override fun goBack() {
        // No-op for native GUI
    }
    
    override fun goForward() {
        // No-op for native GUI
    }
    
    override fun update(width: Int, height: Int) {
        // No-op for native GUI
    }
    
    override fun invalidate() {
        // No-op for native GUI
    }
    
    override fun close() {
        // No-op for native GUI
    }
    
    override fun toString(): String = "CefBrowser(stub)"
}