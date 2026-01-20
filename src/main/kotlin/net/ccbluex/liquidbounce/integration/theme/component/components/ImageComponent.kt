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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.GuiGraphics

class ImageComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<HudComponentTweak> = emptyArray(),
    val values: Array<JsonObject> = emptyArray()
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    override fun render(context: GuiGraphics) {
        // Simple static image rendering for theme images. We'll parse the 'value' key for image path
        // TODO: Load the image from the theme's resource. For now, render a placeholder box.
        val x = 10
        val y = 80
        val width = 80
        val height = 80
        context.fill(x, y, x + width, y + height, 0xFF444444.toInt())
        context.drawString(mc.font, "Image", x + 4, y + 4, 0xFFFFFFFF.toInt())
    }
}
