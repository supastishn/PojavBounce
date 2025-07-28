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
package net.ccbluex.liquidbounce.features.module.modules.render.gui.hud

import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import java.awt.Color

/**
 * Base class for HUD elements
 */
abstract class HudElement(
    x: Int,
    y: Int,
    var width: Int,
    var height: Int
) {
    abstract val name: String
    
    var x: Int = x
        set(value) {
            field = value
            savePosition()
        }
    
    var y: Int = y
        set(value) {
            field = value
            savePosition()
        }
    
    var enabled: Boolean = true
        set(value) {
            field = value
            HudConfig.setElementEnabled(name, value)
        }
    
    init {
        // Load saved position and enabled state
        loadFromConfig()
    }
    
    private fun loadFromConfig() {
        try {
            val savedPosition = HudConfig.getElementPosition(name)
            if (savedPosition != null) {
                this.x = savedPosition.first
                this.y = savedPosition.second
            }
            this.enabled = HudConfig.getElementEnabled(name)
        } catch (e: Exception) {
            println("Error loading config for HUD element $name: ${e.message}")
        }
    }
    
    private fun savePosition() {
        try {
            HudConfig.setElementPosition(name, x, y)
        } catch (e: Exception) {
            println("Error saving position for HUD element $name: ${e.message}")
        }
    }
    
    abstract fun render(context: DrawContext, isSelected: Boolean)
    
    open fun renderPreview(context: DrawContext) {
        if (enabled) {
            render(context, false)
        }
    }
    
    fun isMouseOver(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }
    
    open fun openSettings() {
        // Toggle enabled state as default setting
        enabled = !enabled
    }
    
    protected fun renderBackground(context: DrawContext, isSelected: Boolean) {
        if (!isSelected) return // Only render background when selected in HUD editor
        
        context.fill(x, y, x + width, y + height, 0x80222222.toInt())
        context.drawBorder(x, y, width, height, 0xFF00AAFF.toInt())
    }
}

/**
 * Watermark HUD element showing client name
 */
class WatermarkElement(x: Int, y: Int) : HudElement(x, y, 120, 25) {
    override val name = "Watermark"
    
    override fun render(context: DrawContext, isSelected: Boolean) {
        renderBackground(context, isSelected)
        
        val text1 = "LIQUID"
        val text2 = "BOUNCE"
        context.drawText(mc.textRenderer, text1, x + 5, y + 4, 0xFFFFFF, true)
        context.drawText(mc.textRenderer, text2, x + 5, y + 14, 0x00AAFF, true)
    }
}

/**
 * ArrayList HUD element showing enabled modules
 */
class ArrayListElement(x: Int, y: Int) : HudElement(x, y, 1, 1) { // Width and height will be dynamic
    override val name = "ArrayList"
    
    override fun render(context: DrawContext, isSelected: Boolean) {
        val enabledModules = ModuleManager.getModules()
            .filter { it.enabled && !it.hidden }
            .sortedByDescending { mc.textRenderer.getWidth(it.name) }

        if (enabledModules.isEmpty() && !isSelected) {
            width = 0
            height = 0
            return
        }

        // Dynamically adjust width and height
        val dynamicWidth = if (enabledModules.isNotEmpty()) {
            enabledModules.maxOf { mc.textRenderer.getWidth(it.name) } + 10
        } else {
            100
        }
        val dynamicHeight = if (enabledModules.isNotEmpty()) {
            enabledModules.size * (mc.textRenderer.fontHeight + 2) + 5
        } else {
            20
        }
        
        width = dynamicWidth
        height = dynamicHeight

        renderBackground(context, isSelected)
        
        var yOffset = y + 5
        for ((index, module) in enabledModules.withIndex()) {
            val text = module.name
            val textWidth = mc.textRenderer.getWidth(text)
            val color = getRainbowColor(index)
            context.drawText(mc.textRenderer, text, x + width - textWidth - 5, yOffset, color, true)
            yOffset += mc.textRenderer.fontHeight + 2
        }
    }

    private fun getRainbowColor(
        index: Int,
        speed: Float = 2000f,
        saturation: Float = 0.7f,
        brightness: Float = 0.8f
    ): Int {
        var hue = (System.currentTimeMillis() + index * 100) % speed.toLong()
        hue /= speed.toLong()
        return Color.HSBtoRGB(hue.toFloat(), saturation, brightness)
    }
}

/**
 * InfoPanel HUD element showing various game/player stats
 */
class InfoPanelElement(x: Int, y: Int) : HudElement(x, y, 150, 60) {
    override val name = "InfoPanel"

    override fun render(context: DrawContext, isSelected: Boolean) {
        renderBackground(context, isSelected)

        val player = mc.player
        val infoLines = mutableListOf<Pair<String, String>>()

        if (player != null) {
            infoLines.add("XYZ:" to "${player.x.toInt()}, ${player.y.toInt()}, ${player.z.toInt()}")
            infoLines.add("FPS:" to "${mc.currentFps}")
            // In a real scenario, ping would be fetched from the network handler
            val ping = mc.networkHandler?.getPlayerListEntry(player.uuid)?.latency ?: 0
            infoLines.add("Ping:" to "$ping ms")
        } else {
            infoLines.add("InfoPanel" to "Preview")
        }

        var yOffset = y + 5
        for ((label, value) in infoLines) {
            context.drawText(mc.textRenderer, "$label $value", x + 5, yOffset, 0xFFFFFF, true)
            yOffset += mc.textRenderer.fontHeight + 2
        }
        
        // Adjust height dynamically
        height = infoLines.size * (mc.textRenderer.fontHeight + 2) + 10
    }
}

/**
 * Speed HUD element showing player's horizontal speed
 */
class SpeedElement(x: Int, y: Int) : HudElement(x, y, 100, 20) {
    override val name = "Speed"

    override fun render(context: DrawContext, isSelected: Boolean) {
        renderBackground(context, isSelected)

        val player = mc.player
        val speedText = if (player != null) {
            val deltaX = player.x - player.prevX
            val deltaZ = player.z - player.prevZ
            val speed = kotlin.math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20 // blocks per second
            "Speed: %.2f".format(speed)
        } else {
            "Speed: 0.00"
        }

        context.drawText(mc.textRenderer, speedText, x + 5, y + 6, 0xFFFFFF, true)
    }
}
