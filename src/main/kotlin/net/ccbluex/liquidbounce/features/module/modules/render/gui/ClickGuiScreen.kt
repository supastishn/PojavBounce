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
    private var searchVisible = true // Search is always visible now
    
    override fun init() {
        super.init()
        
        // Initialize the config manager
        ClickGuiConfigManager.initialize()
        
        // Initialize the accordion state manager
        AccordionStateManager.initialize()
        
        // Initialize panels for each category
        val modulesByCategory = ModuleManager.getModules().groupBy { it.category }
        var panelIndex = 0
        
        for ((category, modules) in modulesByCategory) {
            // Try to load saved panel state, otherwise use default position
            val savedState = ClickGuiConfigManager.getPanelState(category)
            val (defaultX, defaultY) = ClickGuiConfigManager.getDefaultPosition(panelIndex)
            
            val panelConfig = PanelConfig(
                x = savedState?.x ?: defaultX,
                y = savedState?.y ?: defaultY,
                width = GuiConfig.panelWidth,
                height = GuiConfig.headerHeight
            )
            
            val panel = ClickGuiPanel(
                category = category,
                allModules = modules,
                config = panelConfig,
                onPanelStateChanged = { category, x, y, expanded ->
                    // Save panel state when it changes
                    ClickGuiConfigManager.savePanelState(category, x, y, expanded)
                }
            )
            
            // Restore expanded state if saved
            if (savedState != null) {
                panel.setExpanded(savedState.expanded)
            }
            
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
        
        // Render tooltip for hovered module
        renderTooltip(context, mouseX, mouseY)
        
        super.render(context, mouseX, mouseY, delta)
    }
    
    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Remove dark tint - use a much lighter, barely visible background
        context.fill(0, 0, width, height, 0x10000000) // Very light, barely visible background
        
        // Grid removed as requested - no longer rendering grid lines
    }
    
    private fun renderTooltip(context: DrawContext, mouseX: Int, mouseY: Int) {
        val description = panels.values.firstNotNullOfOrNull { it.hoveredModuleDescription }

        if (description != null && description.isNotBlank()) {
            val tooltipX = mouseX + 12
            val tooltipY = mouseY - 12
            val maxWidth = 150 // Max width for the tooltip

            val wrappedText = textRenderer.wrapLines(Text.literal(description), maxWidth)
            val tooltipHeight = wrappedText.size * textRenderer.fontHeight + 6
            val tooltipWidth = wrappedText.maxOfOrNull { textRenderer.getWidth(it) }?.plus(6) ?: maxWidth

            // Background
            context.fill(
                tooltipX - 3, 
                tooltipY - 3, 
                tooltipX + tooltipWidth, 
                tooltipY + tooltipHeight, 
                0xE0101010.toInt()
            )

            // Render wrapped text
            var currentY = tooltipY
            for (line in wrappedText) {
                context.drawText(textRenderer, line, tooltipX, currentY, 0xFFFFFFFF.toInt(), false)
                currentY += textRenderer.fontHeight
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
        // First check if any panel widget is handling input
        for (panel in panels.values) {
            if (panel.keyPressed(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        
        // Handle search functionality
        return ClickGuiInputHandler.handleKeyInput(
            keyCode, searchVisible, searchText,
            ClickGuiInputHandler.KeyInputHandlers(
                onEscapeKey = { visible ->
                    if (visible && searchText.isNotEmpty()) {
                        searchText = ""
                        filterModules()
                    } else {
                        close()
                    }
                    true
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
        // First check if any panel widget is handling input
        for (panel in panels.values) {
            if (panel.charTyped(chr, modifiers)) {
                return true
            }
        }
        
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
