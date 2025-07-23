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
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

/**
 * Native Minecraft ClickGUI Screen that replaces the Svelte-based implementation
 */
@Suppress("TooManyFunctions")
class ClickGuiScreen : Screen(Text.literal("ClickGUI")) {
    
    private val panels = mutableMapOf<Category, ClickGuiPanel>()
    private var searchText = ""
    private var searchVisible = false
    
    override fun init() {
        super.init()
        
        // Initialize panels for each category
        val modulesByCategory = ModuleManager.getModules().groupBy { it.category }
        var panelIndex = 0
        
        for ((category, modules) in modulesByCategory) {
            val panelConfig = PanelConfig(
                x = 20 + panelIndex * (GuiConfig.panelWidth + 20), // Space panels with config width
                y = 20,
                width = GuiConfig.panelWidth,
                height = GuiConfig.headerHeight
            )
            val panel = ClickGuiPanel(
                category = category,
                allModules = modules,
                config = panelConfig,
                onOpenSettings = { module -> 
                    mc.setScreen(ModuleSettingsScreen(module, this))
                }
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
    
    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
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
    
    @Suppress("UnusedParameter")
    private fun renderSearchBar(context: DrawContext, mouseX: Int, mouseY: Int) {
        val searchBarWidth = 300
        val searchBarHeight = 30
        val x = (width - searchBarWidth) / 2
        val y = 20
        
        // Background
        context.fill(x, y, x + searchBarWidth, y + searchBarHeight, 0xCC000000.toInt())
        
        // Border
        context.drawBorder(x, y, searchBarWidth, searchBarHeight, 0xFF555555.toInt())
        
        // Search text with placeholder
        val displayText = if (searchText.isEmpty()) {
            "Type to search modules... (F3 to toggle, ESC/DEL to clear)"
        } else {
            "Search: $searchText"
        }
        
        val textColor = if (searchText.isEmpty()) 0x888888 else 0xFFFFFF
        context.drawText(textRenderer, displayText, x + 5, y + 10, textColor, false)
        
        // Cursor indicator when typing
        if (searchText.isNotEmpty()) {
            val textWidth = textRenderer.getWidth("Search: $searchText")
            context.fill(x + 5 + textWidth, y + 8, x + 5 + textWidth + 1, y + 22, 0xFFFFFF)
        }
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
    
    override fun mouseScrolled(
        mouseX: Double, 
        mouseY: Double, 
        horizontalAmount: Double, 
        verticalAmount: Double
    ): Boolean {
        // Handle panel scrolling
        for (panel in panels.values) {
            if (panel.mouseScrolled(mouseX, mouseY, verticalAmount)) {
                return true
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Handle search functionality
        return ClickGuiInputHandler.handleKeyInput(
            keyCode, searchVisible, searchText,
            ClickGuiInputHandler.KeyInputHandlers(
                onEscapeKey = { visible ->
                    if (visible) {
                        searchVisible = false
                        searchText = ""
                        filterModules()
                        true
                    } else {
                        close()
                        false
                    }
                },
                onToggleSearch = {
                    searchVisible = !searchVisible
                    if (!searchVisible) {
                        searchText = ""
                        filterModules()
                    }
                },
                onBackspace = {
                    if (searchText.isNotEmpty()) {
                        searchText = searchText.dropLast(1)
                        filterModules()
                    }
                },
                onDelete = {
                    searchText = ""
                    filterModules()
                }
            )
        ) || super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        return ClickGuiInputHandler.handleCharInput(chr, searchVisible) { newChar ->
            searchText += newChar
            filterModules()
        } || super.charTyped(chr, modifiers)
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

/**
 * Helper object for ClickGui input handling
 */
object ClickGuiInputHandler {
    
    /**
     * Data class for key input handling parameters
     */
    data class KeyInputHandlers(
        val onEscapeKey: (Boolean) -> Boolean,
        val onToggleSearch: () -> Unit,
        val onBackspace: () -> Unit,
        val onDelete: () -> Unit
    )
    
    fun handleCharInput(chr: Char, searchVisible: Boolean, onValidChar: (Char) -> Unit): Boolean {
        if (searchVisible && isValidSearchChar(chr)) {
            onValidChar(chr)
            return true
        }
        return false
    }
    
    private fun isValidSearchChar(chr: Char): Boolean {
        return chr.isLetterOrDigit() || chr == ' ' || chr == '_' || chr == '-'
    }
    
    fun handleKeyInput(
        keyCode: Int,
        searchVisible: Boolean,
        searchText: String,
        handlers: KeyInputHandlers
    ): Boolean {
        return when (keyCode) {
            256 -> handlers.onEscapeKey(searchVisible) // ESC key
            342 -> { // F3 key (configurable)
                handlers.onToggleSearch()
                true
            }
            259 -> { // Backspace key
                if (searchVisible && searchText.isNotEmpty()) {
                    handlers.onBackspace()
                    true
                } else {
                    false
                }
            }
            261 -> { // Delete key
                if (searchVisible) {
                    handlers.onDelete()
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }
}
