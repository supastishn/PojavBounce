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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.integration.backend.BrowserTexture
import net.ccbluex.liquidbounce.integration.backend.browser.*
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.backend.input.InputHandler
import net.ccbluex.liquidbounce.integration.backend.input.InputListener
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * Pure Minecraft GUI browser implementation that replaces CEF browser.
 * This is a stub implementation that provides the browser interface without web rendering.
 */
@Suppress("TooManyFunctions")
class MinecraftGuiBrowser(
    private val backend: MinecraftGuiBrowserBackend,
    url: String,
    viewport: BrowserViewport,
    val settings: BrowserSettings,
    override var priority: Short = 0,
    inputAcceptor: InputAcceptor? = null
) : Browser, InputHandler, MinecraftShortcuts {

    override var viewport: BrowserViewport = viewport
        set(value) {
            field = value
            logger.debug("MinecraftGuiBrowser viewport updated: {}", value)
        }

    override var visible = true

    private var currentUrl: String = url

    private val inputListener: InputListener? = inputAcceptor?.let { _ ->
        InputListener(this, this, inputAcceptor)
    }

    override var url: String
        get() = currentUrl
        set(value) {
            currentUrl = value
            logger.debug("MinecraftGuiBrowser navigating to: {}", value)
        }

    override val texture: BrowserTexture?
        get() {
            // Pure Minecraft GUI doesn't use browser textures
            // The GUI is rendered directly using Minecraft's rendering system
            return null
        }

    override fun forceReload() {
        logger.debug("MinecraftGuiBrowser force reload requested for: {}", currentUrl)
        // No-op for pure Minecraft GUI
    }

    override fun reload() {
        logger.debug("MinecraftGuiBrowser reload requested for: {}", currentUrl)
        // No-op for pure Minecraft GUI
    }

    override fun goForward() {
        logger.debug("MinecraftGuiBrowser go forward requested")
        // No-op for pure Minecraft GUI
    }

    override fun goBack() {
        logger.debug("MinecraftGuiBrowser go back requested")
        // No-op for pure Minecraft GUI
    }

    override fun close() {
        inputListener?.close()
        backend.removeBrowser(this)
        logger.debug("MinecraftGuiBrowser closed: {}", currentUrl)
    }

    override fun update(width: Int, height: Int) {
        if (!viewport.fullScreen) {
            return
        }
        viewport = viewport.copy(width = width, height = height)
    }

    override fun invalidate() {
        logger.debug("MinecraftGuiBrowser invalidate requested")
        // No-op for pure Minecraft GUI
    }

    override fun toString() = "MinecraftGuiBrowser(" +
        "url='$url', viewport=$viewport, visible=$visible, priority=$priority)"

    // Input handling - pass through to input listener
    override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        // Input is handled by the Minecraft GUI system directly
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int) {
        // Input is handled by the Minecraft GUI system directly
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        // Input is handled by the Minecraft GUI system directly
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double) {
        // Input is handled by the Minecraft GUI system directly
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int) {
        // Input is handled by the Minecraft GUI system directly
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int) {
        // Input is handled by the Minecraft GUI system directly
    }

    override fun charTyped(char: Char, modifiers: Int) {
        // Input is handled by the Minecraft GUI system directly
    }
}
