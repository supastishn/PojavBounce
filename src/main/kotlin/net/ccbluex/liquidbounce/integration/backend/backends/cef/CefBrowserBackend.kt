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

import net.ccbluex.liquidbounce.integration.backend.BrowserBackend
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * Stub implementation of CEF Browser Backend for native GUI migration
 * 
 * This replaces the original CEF/JCEF browser functionality with no-op stubs
 * since the native GUI doesn't require embedded browsers.
 */
class CefBrowserBackend : BrowserBackend {
    
    private val logger = logger()
    
    override val isInitialized: Boolean = false
    override val browsers: List<Browser> = emptyList()
    
    override fun makeDependenciesAvailable(taskManager: TaskManager, onAvailable: () -> Unit) {
        logger.info("CEF Browser Backend is disabled - using native GUI")
        // Don't call onAvailable since we don't want to initialize browsers
    }
    
    override fun start() {
        logger.info("CEF Browser Backend start() called - no-op for native GUI")
    }
    
    override fun stop() {
        logger.info("CEF Browser Backend stop() called - no-op for native GUI")  
    }
    
    override fun update() {
        // No-op for native GUI
    }
    
    override fun createBrowser(url: String): Browser {
        logger.warn("createBrowser() called on stub implementation - returning stub browser")
        return CefBrowser("", 0, 0)
    }
}