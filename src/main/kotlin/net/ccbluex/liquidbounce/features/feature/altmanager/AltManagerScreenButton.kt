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
 *
 */
package net.ccbluex.liquidbounce.features.feature.altmanager

import net.ccbluex.liquidbounce.integration.ui.altmanager.NativeAltManagerScreen
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.util.ARGB

/**
 * Adds an Alt Manager button to the top-right corner of the home/title screen
 * for quick access to account switching without opening the Integration Menu.
 */
@Suppress("unused")
object AltManagerScreenButton {
    private const val BUTTON_WIDTH = 60
    private const val BUTTON_HEIGHT = 20
    private const val MARGIN = 10
    private val BUTTON_COLOR = 0xFF1a1a2e.toInt()
    private val BUTTON_HOVER_COLOR = 0xFF2a2a4e.toInt()
    private val BUTTON_TEXT_COLOR = 0xFFFFFFFF.toInt()

    private var lastScreenWidth = 0
    private var lastScreenHeight = 0
    private var buttonX = 0
    private var buttonY = 0

    /**
     * Render the Alt Manager button on the screen
     */
    @JvmStatic
    fun renderAltManagerButton(guiGraphics: GuiGraphics, screen: TitleScreen, mouseX: Int, mouseY: Int) {
        // Update button position if screen size changed
        if (screen.width != lastScreenWidth || screen.height != lastScreenHeight) {
            lastScreenWidth = screen.width
            lastScreenHeight = screen.height
            buttonX = screen.width - BUTTON_WIDTH - MARGIN
            buttonY = MARGIN
        }

        val isHovering = isMouseOverButton(mouseX, mouseY, screen.width)

        // Draw button background
        val color = if (isHovering) BUTTON_HOVER_COLOR else BUTTON_COLOR
        val argbColor = ARGB.color(200, (color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF)
        guiGraphics.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT, argbColor.toInt())

        // Draw button border
        val borderColor = ARGB.color(255, 0x4a, 0x4a, 0x7a)
        guiGraphics.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + 1, borderColor.toInt())
        guiGraphics.fill(buttonX, buttonY + BUTTON_HEIGHT - 1, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT, borderColor.toInt())
        guiGraphics.fill(buttonX, buttonY, buttonX + 1, buttonY + BUTTON_HEIGHT, borderColor.toInt())
        guiGraphics.fill(buttonX + BUTTON_WIDTH - 1, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT, borderColor.toInt())

        // Draw button text (centered)
        val text = "Alts"
        val textWidth = mc.font.width(text)
        val textX = buttonX + (BUTTON_WIDTH - textWidth) / 2
        val textY = buttonY + (BUTTON_HEIGHT - 8) / 2
        guiGraphics.drawString(mc.font, text, textX, textY, BUTTON_TEXT_COLOR, false)
    }

    /**
     * Handle click on the Alt Manager button
     */
    @JvmStatic
    fun handleButtonClick(mouseX: Int, mouseY: Int, screenWidth: Int): Boolean {
        if (isMouseOverButton(mouseX, mouseY, screenWidth)) {
            mc.setScreen(NativeAltManagerScreen(null))
            return true
        }
        return false
    }

    /**
     * Check if mouse is over the button
     */
    private fun isMouseOverButton(mouseX: Int, mouseY: Int, screenWidth: Int): Boolean {
        val x = screenWidth - BUTTON_WIDTH - MARGIN
        val y = MARGIN
        return mouseX >= x && mouseX <= x + BUTTON_WIDTH && mouseY >= y && mouseY <= y + BUTTON_HEIGHT
    }
}

