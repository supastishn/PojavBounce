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
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.GuiGraphics

class WatermarkComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<HudComponentTweak> = emptyArray()
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    fun render(context: GuiGraphics) {
        try {
            val watermarkText = "LiquidBounce ${net.ccbluex.liquidbounce.LiquidBounce.clientVersion}"
            val textWidth = mc.font.width(watermarkText)

            val margin = 4
            context.fill(
                margin - 2,
                margin,
                margin + textWidth + 2,
                margin + mc.font.lineHeight + 2,
                java.awt.Color(0, 0, 0, 120).rgb
            )

            context.drawString(
                mc.font,
                watermarkText,
                margin,
                margin + 1,
                java.awt.Color.HSBtoRGB(((System.currentTimeMillis() % 3600L) / 3600.0f), 0.8f, 1f)
            )
        } catch (e: Exception) {
            logger.error("Failed to render watermark component", e)
        }
    }
}
