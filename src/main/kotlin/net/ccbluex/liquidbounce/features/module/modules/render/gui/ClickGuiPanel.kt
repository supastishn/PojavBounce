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
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import kotlin.math.max
import kotlin.math.min

/**
 * A draggable panel containing modules for a specific category
 */
class ClickGuiPanel(
    val category: Category,
    private val allModules: List<ClientModule>,
    var x: Int,
    var y: Int,
    val width: Int,
    var height: Int,
    private val onOpenSettings: (ClientModule) -> Unit = {}
) {
    private var expanded = false
    private var isDragging = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    private var scrollOffset = 0
    private var filteredModules = allModules
    private val moduleHeight get() = GuiConfig.moduleHeight
    private val headerHeight get() = GuiConfig.headerHeight
    
    @Suppress("UnusedParameter")
    fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Calculate actual height based on expansion state
        val actualHeight = if (expanded) {
            headerHeight + min(filteredModules.size * moduleHeight, GuiConfig.panelMaxHeight)
        } else {
            headerHeight
        }
        
        // Panel background
        context.fill(x, y, x + width, y + actualHeight, GuiConfig.backgroundColor)
        
        // Panel border
        context.drawBorder(x, y, width, actualHeight, GuiConfig.borderColor)
        
        // Header background
        context.fill(x, y, x + width, y + headerHeight, GuiConfig.headerColor)
        
        // Accent border at bottom of header
        context.fill(x, y + headerHeight - 2, x + width, y + headerHeight, GuiConfig.accentColor)
        
        // Category name
        val categoryName = category.name.lowercase().replaceFirstChar { it.uppercase() }
        context.drawText(mc.textRenderer, categoryName, x + 10, y + 8, GuiConfig.textColor, false)
        
        // Expand/collapse button
        renderExpandButton(context, x + width - 20, y + 5)
        
        // Module list if expanded
        if (expanded) {
            renderModules(context, mouseX, mouseY)
        }
    }
    
    private fun renderExpandButton(context: DrawContext, buttonX: Int, buttonY: Int) {
        val buttonSize = 15
        
        // Button background
        context.fill(buttonX, buttonY, buttonX + buttonSize, buttonY + buttonSize, 0x88444444.toInt())
        
        // Plus/minus icon
        val centerX = buttonX + buttonSize / 2
        val centerY = buttonY + buttonSize / 2
        
        // Horizontal line
        context.fill(centerX - 4, centerY - 1, centerX + 4, centerY + 1, 0xFFFFFF)
        
        // Vertical line (only for collapsed state)
        if (!expanded) {
            context.fill(centerX - 1, centerY - 4, centerX + 1, centerY + 4, 0xFFFFFF)
        }
    }
    
    private fun renderModules(context: DrawContext, mouseX: Int, mouseY: Int) {
        val moduleAreaY = y + headerHeight
        val moduleAreaHeight = min(filteredModules.size * moduleHeight, GuiConfig.panelMaxHeight)
        
        // Clip rendering to module area
        context.enableScissor(x, moduleAreaY, x + width, moduleAreaY + moduleAreaHeight)
        
        var currentY = moduleAreaY - scrollOffset
        
        for (module in filteredModules) {
            if (currentY + moduleHeight > moduleAreaY && currentY < moduleAreaY + moduleAreaHeight) {
                renderModule(context, module, x, currentY, mouseX, mouseY)
            }
            currentY += moduleHeight
        }
        
        context.disableScissor()
        
        // Scrollbar if needed
        if (filteredModules.size * moduleHeight > GuiConfig.panelMaxHeight) {
            renderScrollbar(context, moduleAreaY, moduleAreaHeight)
        }
    }
    
    private fun renderModule(
        context: DrawContext, 
        module: ClientModule, 
        moduleX: Int, 
        moduleY: Int, 
        mouseX: Int, 
        mouseY: Int
    ) {
        val isHovered = mouseX >= moduleX && mouseX <= moduleX + width && 
                       mouseY >= moduleY && mouseY <= moduleY + moduleHeight
        
        // Module background
        val bgColor = when {
            module.running -> GuiConfig.enabledModuleColor
            isHovered -> GuiConfig.hoverColor
            else -> 0xFF111111.toInt()
        }
        context.fill(
            moduleX, 
            moduleY, 
            moduleX + width, 
            moduleY + moduleHeight, 
            bgColor
        )
        
        // Module border
        context.fill(moduleX, moduleY + moduleHeight - 1, moduleX + width, moduleY + moduleHeight, GuiConfig.borderColor)
        
        // Module name
        val textColor = if (module.running) GuiConfig.accentColor else 0xBBBBBB
        context.drawText(mc.textRenderer, module.name, moduleX + 10, moduleY + 8, textColor, false)
        
        // Settings indicator if module has settings
        if (moduleHasSettings(module)) {
            context.drawText(mc.textRenderer, "...", moduleX + width - 20, moduleY + 8, 0x888888, false)
        }
    }
    
    private fun renderScrollbar(context: DrawContext, moduleAreaY: Int, moduleAreaHeight: Int) {
        val scrollbarX = x + width - 5
        val scrollbarWidth = 3
        
        // Scrollbar track
        context.fill(scrollbarX, moduleAreaY, scrollbarX + scrollbarWidth, moduleAreaY + moduleAreaHeight, 0x88444444.toInt())
        
        // Scrollbar thumb
        val totalHeight = filteredModules.size * moduleHeight
        val thumbHeight = max(10, (moduleAreaHeight * moduleAreaHeight) / totalHeight)
        val thumbY = moduleAreaY + (scrollOffset * (moduleAreaHeight - thumbHeight)) / (totalHeight - moduleAreaHeight)
        
        context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFFAAAAAA.toInt())
    }
    
    @Suppress("UnusedParameter")
    private fun moduleHasSettings(module: ClientModule): Boolean {
        // Simple check - in real implementation, would check module's configuration tree
        return true // Placeholder
    }
    
    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val intMouseX = mouseX.toInt()
        val intMouseY = mouseY.toInt()
        
        // Check if click is within panel bounds
        if (intMouseX < x || intMouseX > x + width || intMouseY < y) {
            return false
        }
        
        val actualHeight = if (expanded) headerHeight + min(filteredModules.size * moduleHeight, 300) else headerHeight
        if (intMouseY > y + actualHeight) {
            return false
        }
        
        // Header clicks
        if (intMouseY <= y + headerHeight) {
            // Expand button click
            if (intMouseX >= x + width - 20) {
                expanded = !expanded
                return true
            }
            
            // Start dragging
            if (button == 0) {
                isDragging = true
                dragOffsetX = intMouseX - x
                dragOffsetY = intMouseY - y
                return true
            }
            
            // Right click to expand/collapse
            if (button == 1) {
                expanded = !expanded
                return true
            }
        }
        
        // Module clicks
        if (expanded && intMouseY > y + headerHeight) {
            val moduleIndex = (intMouseY - y - headerHeight + scrollOffset) / moduleHeight
            if (moduleIndex >= 0 && moduleIndex < filteredModules.size) {
                val module = filteredModules[moduleIndex]
                
                if (button == 0) {
                    // Toggle module
                    module.enabled = !module.enabled
                    return true
                } else if (button == 1) {
                    // Open module settings
                    onOpenSettings(module)
                    return true
                }
            }
        }
        
        return false
    }
    
    @Suppress("UnusedParameter")
    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (isDragging && button == 0) {
            x = mouseX.toInt() - dragOffsetX
            y = mouseY.toInt() - dragOffsetY
            
            // Keep panel within screen bounds
            x = max(0, min(x, mc.window.scaledWidth - width))
            y = max(0, min(y, mc.window.scaledHeight - height))
            
            return true
        }
        
        return false
    }
    
    @Suppress("UnusedParameter")
    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int) {
        isDragging = false
    }
    
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!expanded) return false
        
        val intMouseX = mouseX.toInt()
        val intMouseY = mouseY.toInt()
        
        // Check if mouse is over module area
        if (intMouseX >= x && intMouseX <= x + width && 
            intMouseY >= y + headerHeight && intMouseY <= y + headerHeight + 300) {
            
            val maxScroll = max(0, filteredModules.size * moduleHeight - 300)
            scrollOffset = max(0, min(maxScroll, scrollOffset - (amount * 30).toInt()))
            return true
        }
        
        return false
    }
    
    fun filterModules(searchText: String) {
        filteredModules = if (searchText.isEmpty()) {
            allModules
        } else {
            allModules.filter { module ->
                module.name.contains(searchText, ignoreCase = true)
            }
        }
        
        // Reset scroll when filtering
        scrollOffset = 0
    }
}
