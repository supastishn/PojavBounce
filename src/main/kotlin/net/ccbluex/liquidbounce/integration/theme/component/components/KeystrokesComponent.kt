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

package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.GuiGraphics

class KeystrokesComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<HudComponentTweak> = emptyArray()
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    override fun render(context: GuiGraphics) {
        val margin = 10
        val keySize = 12
        val spacing = 4
        val xPos = margin
        val yPos = mc.window.guiScaledHeight - margin - keySize

        // W key
        drawKey(context, xPos, yPos - (keySize + spacing), "W", mc.options.keyUp.isDown)

        // A S D arranged below
        drawKey(context, xPos - (keySize + spacing), yPos, "A", mc.options.keyLeft.isDown)
        drawKey(context, xPos, yPos, "S", mc.options.keyDown.isDown)
        drawKey(context, xPos + (keySize + spacing), yPos, "D", mc.options.keyRight.isDown)

        // Space (jump)
        drawKey(context, xPos + (2 * (keySize + spacing)), yPos, "J", mc.options.keyJump.isDown)
    }

    private fun drawKey(context: GuiGraphics, x: Int, y: Int, key: String, pressed: Boolean) {
        val size = 18
        val background = if (pressed) 0xFF00FF00.toInt() else 0xFF000000.toInt()
        context.fill(x, y, x + size, y + size, background)
        context.drawString(mc.font, key, x + 4, y + 4, 0xFFFFFFFF.toInt())
    }
}
