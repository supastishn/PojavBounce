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

import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.GuiGraphics

class ArrayListComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<HudComponentTweak> = emptyArray()
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    @Suppress("unused")
    private val margin = 4

    @Suppress("unused")
    private val moduleSpacing = 2

    override fun render(context: GuiGraphics) {
        val enabledModules = ModuleManager
            .filter { it.enabled && !it.hidden }
            .sortedByDescending { mc.font.width(getModuleDisplayText(it)) }

        var yOffset = 4
        val screenWidth = mc.window.guiScaledWidth

        for (module in enabledModules) {
            val displayText = getModuleDisplayText(module)
            val textWidth = mc.font.width(displayText)
            val xPos = screenWidth - textWidth - 4

            // Background
            context.fill(
                xPos - 2,
                yOffset,
                screenWidth - 4 + 2,
                yOffset + mc.font.lineHeight + 2,
                java.awt.Color(0, 0, 0, 120).rgb
            )

            // Module text
            context.drawString(
                mc.font,
                displayText,
                xPos,
                yOffset + 1,
                java.awt.Color.HSBtoRGB(((System.currentTimeMillis() % 3600L) / 3600.0f), 0.8f, 1f)
            )

            yOffset += mc.font.lineHeight + 2
        }
    }

    private fun getModuleDisplayText(module: net.ccbluex.liquidbounce.features.module.ClientModule): String {
        return if (module.tag != null) {
            "${module.name} §7${module.tag}"
        } else {
            module.name
        }
    }
}
