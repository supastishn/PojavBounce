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

/**
 * Taco HUD component - a fun decorative element.
 * Note: Uses text instead of emoji due to Minecraft font renderer limitations with Unicode emojis.
 */
class TacoComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<HudComponentTweak> = emptyArray()
) : NativeHudComponent(name, enabled, alignment, tweaks) {
    fun render(context: GuiGraphics) {
        val x = mc.window.guiScaledWidth - 50
        val y = mc.window.guiScaledHeight - 50
        context.fill(x, y, x + 40, y + 20, 0xFFAA4400.toInt())
        context.drawString(mc.font, "Taco", x + 10, y + 2, 0xFFFFFFFF.toInt())
    }
}
