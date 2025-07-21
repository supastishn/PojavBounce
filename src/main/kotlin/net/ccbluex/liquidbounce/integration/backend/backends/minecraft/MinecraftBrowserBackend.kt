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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.integration.backend.BrowserBackend
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.sortedInsert

/**
 * Minecraft UI-based browser backend that replaces JCEF with native Minecraft GUI components.
 * This backend is designed for compatibility with PojavLauncher and other environments
 * where JCEF is not supported.
 *
 * @author Copilot AI
 */
class MinecraftBrowserBackend : BrowserBackend, EventListener {

    override val isInitialized: Boolean = true
    override var browsers = mutableListOf<MinecraftBrowser>()
    override var isAccelerationSupported: Boolean = false

    override fun makeDependenciesAvailable(taskManager: TaskManager, whenAvailable: () -> Unit) {
        // No external dependencies needed for Minecraft UI
        logger.info("Using Minecraft UI browser backend - no dependencies required")
        whenAvailable()
    }

    override fun start() {
        logger.info("Minecraft UI browser backend started")
        // No initialization needed for Minecraft UI
    }

    override fun stop() {
        logger.info("Stopping Minecraft UI browser backend")
        browsers.forEach { it.close() }
        browsers.clear()
    }

    override fun update() {
        // No global update needed for Minecraft UI
    }

    override fun createBrowser(
        url: String,
        position: BrowserViewport,
        settings: BrowserSettings,
        priority: Short,
        inputAcceptor: InputAcceptor?
    ) = MinecraftBrowser(this, url, position, settings, priority, inputAcceptor)
        .apply(::addBrowser)

    private fun addBrowser(browser: MinecraftBrowser) {
        browsers.sortedInsert(browser, MinecraftBrowser::priority)
    }

    internal fun removeBrowser(browser: MinecraftBrowser) {
        browsers.remove(browser)
    }
}