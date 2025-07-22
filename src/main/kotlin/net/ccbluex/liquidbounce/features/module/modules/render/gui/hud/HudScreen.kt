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

import net.ccbluex.liquidbounce.features.module.modules.render.gui.GuiConfig
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

/**
 * Native HUD configuration screen that replaces the Svelte-based implementation
 */
class HudScreen : Screen(Text.literal("HUD Editor")) {
    
    private val hudElements = mutableListOf<HudElement>()
    private var selectedElement: HudElement? = null
    private var isDragging = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    
    override fun init() {
        super.init()
        
        // Initialize HUD elements
        initializeHudElements()
    }
    
    private fun initializeHudElements() {
        // Add basic HUD elements
        hudElements.add(WatermarkElement(10, 10))
        hudElements.add(ArrayListElement(width - 200, 10))
        hudElements.add(CoordinatesElement(10, height - 30))
        hudElements.add(FpsElement(10, 30))
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Semi-transparent background
        context.fill(0, 0, width, height, 0x80000000.toInt())
        
        // Render grid
        renderGrid(context)
        
        // Render all HUD elements
        for (element in hudElements) {
            element.render(context, element == selectedElement)
        }
        
        // Render instructions
        renderInstructions(context)
        
        super.render(context, mouseX, mouseY, delta)
    }
    
    private fun renderGrid(context: DrawContext) {
        val gridSize = GuiConfig.gridSize
        val gridColor = 0x33FFFFFF
        
        // Vertical lines
        var x = 0
        while (x < width) {
            context.fill(x, 0, x + 1, height, gridColor)
            x += gridSize
        }
        
        // Horizontal lines
        var y = 0
        while (y < height) {
            context.fill(0, y, width, y + 1, gridColor)
            y += gridSize
        }
    }
    
    private fun renderInstructions(context: DrawContext) {
        val instructions = listOf(
            "Left-click and drag to move HUD elements",
            "Right-click elements to toggle enabled/disabled", 
            "Press ESC to save and exit HUD editor",
            "Elements snap to ${GuiConfig.gridSize}px grid for alignment"
        )
        
        // Background for instructions
        context.fill(5, 5, 300, 5 + instructions.size * 12 + 10, 0x80000000.toInt())
        context.drawBorder(5, 5, 295, instructions.size * 12 + 10, 0xFF444444.toInt())
        
        var yOffset = 10
        for (instruction in instructions) {
            context.drawText(textRenderer, instruction, 10, yOffset, 0xFFFFFF, false)
            yOffset += 12
        }
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val intMouseX = mouseX.toInt()
        val intMouseY = mouseY.toInt()
        
        // Find clicked element
        selectedElement = null
        for (element in hudElements.reversed()) { // Check from top to bottom
            if (element.isMouseOver(intMouseX, intMouseY)) {
                selectedElement = element
                
                if (button == 0) { // Left click - start dragging
                    isDragging = true
                    dragOffsetX = intMouseX - element.x
                    dragOffsetY = intMouseY - element.y
                } else if (button == 1) { // Right click - open settings
                    element.openSettings()
                }
                
                return true
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (isDragging && selectedElement != null) {
            val newX = mouseX.toInt() - dragOffsetX
            val newY = mouseY.toInt() - dragOffsetY
            
            // Snap to grid using config
            val snapped = GuiConfig.getGridSnappedPosition(newX, newY)
            selectedElement!!.x = snapped.first
            selectedElement!!.y = snapped.second
            
            // Keep within bounds
            selectedElement!!.x = selectedElement!!.x.coerceIn(0, width - selectedElement!!.width)
            selectedElement!!.y = selectedElement!!.y.coerceIn(0, height - selectedElement!!.height)
            
            return true
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
    
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isDragging = false
        
        // Save positions (in real implementation would persist to config)
        saveElementPositions()
        
        return super.mouseReleased(mouseX, mouseY, button)
    }
    
    private fun saveElementPositions() {
        try {
            // Save all element positions to configuration
            HudConfig.saveConfig()
        } catch (e: Exception) {
            println("Error saving HUD element positions: ${e.message}")
        }
    }
    
    override fun shouldPause(): Boolean {
        return false
    }
    
    override fun close() {
        // Apply changes and return to game
        super.close()
    }
}