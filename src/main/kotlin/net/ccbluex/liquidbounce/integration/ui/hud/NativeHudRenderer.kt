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
package net.ccbluex.liquidbounce.integration.ui.hud

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.ColorHelper

/**
 * Native HUD renderer - replaces browser-based HUD with pure Minecraft rendering
 * This renders the arraylist, watermark, and other HUD elements
 */
object NativeHudRenderer : EventListener {

    private const val MARGIN = 4
    private const val MODULE_SPACING = 2

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        if (mc.options.hudHidden) {
            return@handler
        }

        val context = event.context
        renderArrayList(context)
        renderWatermark(context)
        renderCoordinates(context)
    }

    /**
     * Renders the module arraylist (enabled modules on the right side)
     */
    private fun renderArrayList(context: DrawContext) {
        val enabledModules = ModuleManager
            .filter { it.enabled && !it.hidden }
            .sortedByDescending { mc.textRenderer.getWidth(getModuleDisplayText(it)) }

        var yOffset = MARGIN
        val screenWidth = mc.window.scaledWidth

        for (module in enabledModules) {
            val displayText = getModuleDisplayText(module)
            val textWidth = mc.textRenderer.getWidth(displayText)
            val xPos = screenWidth - textWidth - MARGIN

            // Background
            context.fill(
                xPos - 2,
                yOffset,
                screenWidth - MARGIN + 2,
                yOffset + mc.textRenderer.fontHeight + 2,
                ColorHelper.getArgb(120, 0, 0, 0)
            )

            // Module text
            context.drawText(
                mc.textRenderer,
                displayText,
                xPos,
                yOffset + 1,
                getRainbowColor(yOffset),
                true
            )

            yOffset += mc.textRenderer.fontHeight + MODULE_SPACING
        }
    }

    /**
     * Renders the watermark (client name and version)
     */
    private fun renderWatermark(context: DrawContext) {
        val watermarkText = "LiquidBounce ${net.ccbluex.liquidbounce.LiquidBounce.clientVersion}"
        val textWidth = mc.textRenderer.getWidth(watermarkText)

        // Background
        context.fill(
            MARGIN - 2,
            MARGIN,
            MARGIN + textWidth + 2,
            MARGIN + mc.textRenderer.fontHeight + 2,
            ColorHelper.getArgb(120, 0, 0, 0)
        )

        // Text
        context.drawText(
            mc.textRenderer,
            watermarkText,
            MARGIN,
            MARGIN + 1,
            getRainbowColor(0),
            true
        )
    }

    /**
     * Renders player coordinates
     */
    private fun renderCoordinates(context: DrawContext) {
        val player = mc.player ?: return
        val coordText = "XYZ: %.1f, %.1f, %.1f".format(
            java.util.Locale.US,
            player.x,
            player.y,
            player.z
        )
        val textWidth = mc.textRenderer.getWidth(coordText)
        val yPos = mc.window.scaledHeight - mc.textRenderer.fontHeight - MARGIN

        // Background
        context.fill(
            MARGIN - 2,
            yPos - 1,
            MARGIN + textWidth + 2,
            yPos + mc.textRenderer.fontHeight + 1,
            ColorHelper.getArgb(120, 0, 0, 0)
        )

        // Text
        context.drawText(
            mc.textRenderer,
            coordText,
            MARGIN,
            yPos,
            0xFFFFFF,
            true
        )
    }

    /**
     * Gets the display text for a module (name + tag if available)
     */
    private fun getModuleDisplayText(module: net.ccbluex.liquidbounce.features.module.ClientModule): String {
        return if (module.tag != null) {
            "${module.name} §7${module.tag}"
        } else {
            module.name
        }
    }

    /**
     * Generates a rainbow color based on offset
     */
    private fun getRainbowColor(offset: Int): Int {
        val time = System.currentTimeMillis()
        val hue = ((time + offset * 100) % 3600) / 3600.0f
        return java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f)
    }
}
