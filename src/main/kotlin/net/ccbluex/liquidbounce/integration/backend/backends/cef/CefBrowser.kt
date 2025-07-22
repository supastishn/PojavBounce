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
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * Stub implementation of CEF Browser for native GUI migration
 * 
 * This replaces the original CEF/JCEF browser functionality with no-op stubs
 * since the native GUI doesn't require embedded browsers.
 */
class CefBrowser(
    override val url: String,
    initialWidth: Int = 0,
    initialHeight: Int = 0
) : Browser {
    
    private val logger = logger()
    private var stubViewport = BrowserViewport(0, 0, initialWidth, initialHeight)
    
    override var viewport: BrowserViewport
        get() = stubViewport
        set(value) {
            stubViewport = value
            // No-op for native GUI
        }
    
    override val visible: Boolean = false
    override val scale: Float = 1.0f
    
    override fun loadUrl(url: String) {
        logger.debug("CefBrowser.loadUrl($url) called - no-op for native GUI")
    }
    
    override fun reload() {
        logger.debug("CefBrowser.reload() called - no-op for native GUI")
    }
    
    override fun goBack() {
        logger.debug("CefBrowser.goBack() called - no-op for native GUI")
    }
    
    override fun goForward() {
        logger.debug("CefBrowser.goForward() called - no-op for native GUI")
    }
    
    override fun executeJavaScript(script: String) {
        logger.debug("CefBrowser.executeJavaScript() called - no-op for native GUI")
    }
    
    override fun close() {
        logger.debug("CefBrowser.close() called - no-op for native GUI")
    }
    
    override fun onKey(key: Int, scancode: Int, action: Int, mods: Int): Boolean {
        // No-op for native GUI
        return false
    }
    
    override fun onChar(codepoint: Int, mods: Int): Boolean {
        // No-op for native GUI
        return false
    }
    
    override fun onMouseButton(button: Int, action: Int, mods: Int): Boolean {
        // No-op for native GUI
        return false
    }
    
    override fun onMouseMove(mouseX: Double, mouseY: Double): Boolean {
        // No-op for native GUI
        return false
    }
    
    override fun onMouseScroll(xOffset: Double, yOffset: Double): Boolean {
        // No-op for native GUI
        return false
    }
}