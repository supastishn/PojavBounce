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
package net.ccbluex.liquidbounce.features.module.modules.render.gui

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

/**
 * Native Minecraft ClickGUI Screen that replaces the Svelte-based implementation
 */
class ClickGuiScreen : Screen(Text.literal("ClickGUI")) {
    
    private val panels = mutableMapOf<Category, ClickGuiPanel>()
    private var searchText = ""
    private var searchVisible = false
    
    override fun init() {
        super.init()
        
        // Initialize panels for each category
        val modulesByCategory = ModuleManager.modules.groupBy { it.category }
        var panelIndex = 0
        
        for ((category, modules) in modulesByCategory) {
            val panel = ClickGuiPanel(
                category = category,
                modules = modules,
                x = 20,
                y = 20 + panelIndex * 50,
                width = 250,
                height = 25
            )
            panels[category] = panel
            panelIndex++
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Render background with grid if enabled
        renderBackground(context, mouseX, mouseY, delta)
        
        // Render all panels
        for (panel in panels.values) {
            panel.render(context, mouseX, mouseY, delta)
        }
        
        // Render search bar if visible
        if (searchVisible) {
            renderSearchBar(context, mouseX, mouseY)
        }
        
        super.render(context, mouseX, mouseY, delta)
    }
    
    private fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Semi-transparent background
        context.fill(0, 0, width, height, 0x99000000.toInt())
        
        // Grid background if snapping is enabled
        if (ModuleClickGui.Snapping.enabled) {
            val gridSize = 10 // TODO: Get from ModuleClickGui.Snapping.gridSize
            val gridColor = 0x44FFFFFF
            
            // Draw vertical lines
            var x = 0
            while (x < width) {
                context.fill(x, 0, x + 1, height, gridColor)
                x += gridSize
            }
            
            // Draw horizontal lines
            var y = 0
            while (y < height) {
                context.fill(0, y, width, y + 1, gridColor)
                y += gridSize
            }
        }
    }
    
    private fun renderSearchBar(context: DrawContext, mouseX: Int, mouseY: Int) {
        val searchBarWidth = 300
        val searchBarHeight = 30
        val x = (width - searchBarWidth) / 2
        val y = 20
        
        // Background
        context.fill(x, y, x + searchBarWidth, y + searchBarHeight, 0xCC000000.toInt())
        
        // Border
        context.drawBorder(x, y, searchBarWidth, searchBarHeight, 0xFF555555.toInt())
        
        // Search text
        context.drawText(textRenderer, "Search: $searchText", x + 5, y + 10, 0xFFFFFF, false)
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle panel clicks
        for (panel in panels.values) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                return true
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        // Handle panel dragging
        for (panel in panels.values) {
            if (panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
    
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle panel mouse release
        for (panel in panels.values) {
            panel.mouseReleased(mouseX, mouseY, button)
        }
        
        return super.mouseReleased(mouseX, mouseY, button)
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Handle search functionality
        if (keyCode == 256) { // ESC key
            searchVisible = false
            searchText = ""
        } else if (keyCode == 265) { // F6 or other search key
            searchVisible = !searchVisible
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (searchVisible && chr.isLetterOrDigit()) {
            searchText += chr
            // Filter modules based on search
            filterModules()
            return true
        }
        
        return super.charTyped(chr, modifiers)
    }
    
    private fun filterModules() {
        // Update panel visibility based on search
        for (panel in panels.values) {
            panel.filterModules(searchText)
        }
    }
    
    override fun shouldPause(): Boolean {
        return false // Don't pause the game
    }
    
    override fun close() {
        mc.mouse.lockCursor()
        super.close()
    }
}