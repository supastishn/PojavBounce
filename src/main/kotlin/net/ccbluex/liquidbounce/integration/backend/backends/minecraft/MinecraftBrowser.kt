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

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.integration.backend.BrowserTexture
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserViewport
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.backend.input.InputHandler
import net.ccbluex.liquidbounce.integration.backend.input.InputListener
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * Minecraft UI-based browser that uses native Minecraft GUI components instead of JCEF.
 * This implementation provides basic browser-like functionality for PojavLauncher compatibility.
 *
 * @author Copilot AI
 */
class MinecraftBrowser(
    private val backend: MinecraftBrowserBackend,
    initialUrl: String,
    override var viewport: BrowserViewport,
    val settings: BrowserSettings,
    override var priority: Short = 0,
    inputAcceptor: InputAcceptor? = null
) : Browser, InputHandler, MinecraftShortcuts {

    override var visible = true
    private var currentUrl = initialUrl
    private val urlHistory = mutableListOf<String>()
    private var historyIndex = -1

    private val inputListener: InputListener? = inputAcceptor?.let { inputChecker ->
        InputListener(this, this, inputAcceptor)
    }

    init {
        addToHistory(initialUrl)
        logger.info("MinecraftBrowser created for URL: $initialUrl")
    }

    override var url: String
        get() = currentUrl
        set(value) {
            if (value != currentUrl) {
                currentUrl = value
                addToHistory(value)
                logger.info("MinecraftBrowser navigated to: $value")
            }
        }

    override val texture: BrowserTexture?
        get() {
            // Return null as we don't render web content in Minecraft UI mode
            // The UI will be handled by Minecraft's native GUI system
            return null
        }

    override fun forceReload() {
        logger.info("MinecraftBrowser force reload requested for: $currentUrl")
        // In Minecraft UI mode, reload is just a no-op since we don't render web content
    }

    override fun reload() {
        logger.info("MinecraftBrowser reload requested for: $currentUrl")
        // In Minecraft UI mode, reload is just a no-op since we don't render web content
    }

    override fun goForward() {
        if (historyIndex < urlHistory.size - 1) {
            historyIndex++
            currentUrl = urlHistory[historyIndex]
            logger.info("MinecraftBrowser navigated forward to: $currentUrl")
        }
    }

    override fun goBack() {
        if (historyIndex > 0) {
            historyIndex--
            currentUrl = urlHistory[historyIndex]
            logger.info("MinecraftBrowser navigated back to: $currentUrl")
        }
    }

    override fun close() {
        inputListener?.close()
        backend.removeBrowser(this)
        logger.info("MinecraftBrowser closed")
    }

    override fun update(width: Int, height: Int) {
        if (!viewport.fullScreen) {
            return
        }
        viewport = viewport.copy(width = width, height = height)
    }

    override fun invalidate() {
        // No texture to invalidate in Minecraft UI mode
    }

    override fun toString() = "MinecraftBrowser(url='$url', viewport=$viewport, visible=$visible, priority=$priority)"

    // Input handling methods - basic implementation
    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        logger.debug("MinecraftBrowser mouse clicked at ($mouseX, $mouseY) button $mouseButton")
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int) {
        logger.debug("MinecraftBrowser mouse released at ($mouseX, $mouseY) button $mouseButton")
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        // No action needed for mouse movement in Minecraft UI mode
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double) {
        logger.debug("MinecraftBrowser mouse scrolled at ($mouseX, $mouseY) delta $delta")
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) {
        logger.debug("MinecraftBrowser key pressed: $keyCode")
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int) {
        logger.debug("MinecraftBrowser key released: $keyCode")
    }

    override fun charTyped(char: Char, modifiers: Int) {
        logger.debug("MinecraftBrowser char typed: $char")
    }

    private fun addToHistory(url: String) {
        // Remove any forward history when navigating to a new URL
        if (historyIndex < urlHistory.size - 1) {
            urlHistory.subList(historyIndex + 1, urlHistory.size).clear()
        }
        
        urlHistory.add(url)
        historyIndex = urlHistory.size - 1
    }
}