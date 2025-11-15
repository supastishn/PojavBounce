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
package net.ccbluex.liquidbounce.integration.backend.backends.minecraftgui

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.integration.backend.BrowserBackend
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.sortedInsert

/**
 * Pure Minecraft GUI browser backend - replaces JCEF/MCEF with native Minecraft GUI rendering.
 * This backend provides a lightweight alternative that doesn't require external dependencies.
 *
 * @author PojavBounce Contributors
 */
class MinecraftGuiBrowserBackend : BrowserBackend, EventListener {

    override val isInitialized: Boolean
        get() = initialized

    override var browsers = mutableListOf<MinecraftGuiBrowser>()
    override var isAccelerationSupported: Boolean = false

    private var initialized = false

    override fun makeDependenciesAvailable(taskManager: TaskManager, whenAvailable: () -> Unit) {
        // No dependencies needed for pure Minecraft GUI
        logger.info("MinecraftGui backend has no external dependencies")
        whenAvailable()
    }

    override fun start() {
        if (!initialized) {
            logger.info("Initializing MinecraftGui browser backend...")
            initialized = true
            logger.info("MinecraftGui browser backend initialized successfully")
        }
    }

    override fun stop() {
        logger.info("Shutting down MinecraftGui browser backend...")
        browsers.forEach { it.close() }
        browsers.clear()
        initialized = false
        logger.info("MinecraftGui browser backend stopped")
    }

    override fun update() {
        // Pure Minecraft GUI doesn't need global updates like CEF message loop
        // Individual browsers handle their own rendering
    }

    override fun createBrowser(
        url: String,
        position: BrowserViewport,
        settings: BrowserSettings,
        priority: Short,
        inputAcceptor: InputAcceptor?
    ) = MinecraftGuiBrowser(this, url, position, settings, priority, inputAcceptor)
        .apply(::addBrowser)

    private fun addBrowser(browser: MinecraftGuiBrowser) {
        browsers.sortedInsert(browser, MinecraftGuiBrowser::priority)
    }

    internal fun removeBrowser(browser: MinecraftGuiBrowser) {
        browsers.remove(browser)
    }
}
