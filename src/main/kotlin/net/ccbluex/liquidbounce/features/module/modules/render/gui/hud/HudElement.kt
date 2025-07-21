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

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext

/**
 * Base class for HUD elements
 */
abstract class HudElement(
    x: Int,
    y: Int,
    val width: Int,
    val height: Int
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
        val backgroundColor = if (isSelected) 0x80444444.toInt() else 0x80222222.toInt()
        val borderColor = if (isSelected) 0xFF00AAFF.toInt() else 0xFF444444.toInt()
        
        context.fill(x, y, x + width, y + height, backgroundColor)
        context.drawBorder(x, y, width, height, borderColor)
    }
}

/**
 * Watermark HUD element showing client name
 */
class WatermarkElement(x: Int, y: Int) : HudElement(x, y, 120, 20) {
    override val name = "Watermark"
    
    override fun render(context: DrawContext, isSelected: Boolean) {
        renderBackground(context, isSelected)
        
        val text = "LiquidBounce"
        context.drawText(mc.textRenderer, text, x + 5, y + 6, 0x00AAFF, true)
    }
}

/**
 * ArrayList HUD element showing enabled modules
 */
class ArrayListElement(x: Int, y: Int) : HudElement(x, y, 180, 100) {
    override val name = "ArrayList"
    
    override fun render(context: DrawContext, isSelected: Boolean) {
        renderBackground(context, isSelected)
        
        // Mock enabled modules for preview
        val enabledModules = listOf("KillAura", "Fly", "Speed", "NoFall", "ESP")
        
        var yOffset = y + 5
        for ((index, module) in enabledModules.withIndex()) {
            val color = 0xFF0000 + (index * 0x003300) // Rainbow-ish effect
            context.drawText(mc.textRenderer, module, x + 5, yOffset, color, true)
            yOffset += mc.textRenderer.fontHeight + 2
        }
    }
}

/**
 * Coordinates HUD element showing player position
 */
class CoordinatesElement(x: Int, y: Int) : HudElement(x, y, 150, 20) {
    override val name = "Coordinates"
    
    override fun render(context: DrawContext, isSelected: Boolean) {
        renderBackground(context, isSelected)
        
        val player = mc.player
        val text = if (player != null) {
            "XYZ: ${player.x.toInt()}, ${player.y.toInt()}, ${player.z.toInt()}"
        } else {
            "XYZ: 0, 0, 0"
        }
        
        context.drawText(mc.textRenderer, text, x + 5, y + 6, 0xFFFFFF, true)
    }
}

/**
 * FPS HUD element showing current framerate
 */
class FpsElement(x: Int, y: Int) : HudElement(x, y, 80, 20) {
    override val name = "FPS"
    
    override fun render(context: DrawContext, isSelected: Boolean) {
        renderBackground(context, isSelected)
        
        val fps = mc.currentFps
        val text = "FPS: $fps"
        val color = when {
            fps >= 60 -> 0x00FF00  // Green
            fps >= 30 -> 0xFFFF00  // Yellow
            else -> 0xFF0000       // Red
        }
        
        context.drawText(mc.textRenderer, text, x + 5, y + 6, color, true)
    }
}