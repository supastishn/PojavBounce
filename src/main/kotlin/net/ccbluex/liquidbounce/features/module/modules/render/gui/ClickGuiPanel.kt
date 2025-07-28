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
 * Configuration for a ClickGui panel
 */
data class PanelConfig(
    var x: Int,
    var y: Int,
    val width: Int,
    var height: Int
)

/**
 * A draggable panel containing modules for a specific category
 */
@Suppress("TooManyFunctions")
class ClickGuiPanel(
    val category: Category,
    private val allModules: List<ClientModule>,
    val config: PanelConfig,
    private val onOpenSettings: (ClientModule, Int, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    private val onPanelStateChanged: (Category, Int, Int, Boolean) -> Unit = { _, _, _, _ -> }
) {
    var x: Int
        get() = config.x
        set(value) { config.x = value }
    
    var y: Int
        get() = config.y
        set(value) { config.y = value }
    
    val width: Int get() = config.width
    var height: Int
        get() = config.height
        set(value) { config.height = value }
    private var expanded = false
    private var isDragging = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    private var scrollOffset = 0
    private var isScrollDragging = false
    private var scrollDragStartY = 0.0
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
        context.fill(x, y, x + width, y + actualHeight, 0xAA101010.toInt())
        
        // Panel border
        context.drawBorder(x, y, width, actualHeight, 0xAA252525.toInt())
        
        // Header background
        // No separate header color for a blended look
        
        // Thin separator line below header
        context.fill(x, y + headerHeight - 1, x + width, y + headerHeight, 0xFF505050.toInt())
        
        // Category name (adjusted for smaller tabs)
        val categoryName = category.name.lowercase().replaceFirstChar { it.uppercase() }
        context.drawText(mc.textRenderer, categoryName, x + 4, y + 4, 0xFFFFFFFF, false)
        
        // Expand/collapse button (adjusted for smaller tabs)
        renderExpandButton(context, x + width - 12, y + 2)
        
        // Module list if expanded
        if (expanded) {
            renderModules(context, mouseX, mouseY)
        }
    }
    
    private fun renderExpandButton(context: DrawContext, buttonX: Int, buttonY: Int) {
        ClickGuiPanelRenderer.renderExpandButton(context, buttonX, buttonY, expanded)
    }
    
    private fun renderModules(context: DrawContext, mouseX: Int, mouseY: Int) {
        val moduleAreaY = y + headerHeight
        val moduleAreaHeight = min(filteredModules.size * moduleHeight, GuiConfig.panelMaxHeight)
        
        // Clip rendering to module area
        context.enableScissor(x, moduleAreaY, x + width, moduleAreaY + moduleAreaHeight)
        
        var currentY = moduleAreaY - scrollOffset
        
        for (module in filteredModules) {
            if (currentY + moduleHeight > moduleAreaY && currentY < moduleAreaY + moduleAreaHeight) {
                renderModule(ModuleRenderData(context, module, x, currentY, mouseX, mouseY))
            }
            currentY += moduleHeight
        }
        
        context.disableScissor()
        
        // Scrollbar if needed
        if (filteredModules.size * moduleHeight > GuiConfig.panelMaxHeight) {
            renderScrollbar(context, moduleAreaY, moduleAreaHeight)
        }
    }
    
    /**
     * Data class for module rendering parameters
     */
    private data class ModuleRenderData(
        val context: DrawContext,
        val module: ClientModule,
        val moduleX: Int,
        val moduleY: Int,
        val mouseX: Int,
        val mouseY: Int
    )
    
    private fun renderModule(renderData: ModuleRenderData) {
        val isHovered = renderData.mouseX >= renderData.moduleX && 
                       renderData.mouseX <= renderData.moduleX + width && 
                       renderData.mouseY >= renderData.moduleY && 
                       renderData.mouseY <= renderData.moduleY + moduleHeight
        
        // Module background
        val bgColor = when {
            isHovered -> 0xAA252525.toInt()
            else -> 0x00000000 // Transparent background
        }
        renderData.context.fill(
            renderData.moduleX, 
            renderData.moduleY, 
            renderData.moduleX + width, 
            renderData.moduleY + moduleHeight, 
            bgColor
        )
        
        // Module name (adjusted for smaller tabs)
        val textColor = if (renderData.module.running) 0xFF80BFFF else 0xBBBBBB
        renderData.context.drawText(
            mc.textRenderer, 
            renderData.module.name, 
            renderData.moduleX + 4, 
            renderData.moduleY + 4, 
            textColor, 
            false
        )
        
        // Settings indicator if module has settings (adjusted for smaller tabs)
        if (moduleHasSettings(renderData.module)) {
            renderData.context.drawText(
                mc.textRenderer, 
                "...", 
                renderData.moduleX + width - 12, 
                renderData.moduleY + 4, 
                0x888888, 
                false
            )
        }
    }
    
    private fun renderScrollbar(context: DrawContext, moduleAreaY: Int, moduleAreaHeight: Int) {
        val scrollbarX = x + width - 5
        val scrollbarWidth = 3
        
        // Scrollbar track
        context.fill(
            scrollbarX, 
            moduleAreaY, 
            scrollbarX + scrollbarWidth, 
            moduleAreaY + moduleAreaHeight, 
            0x88444444.toInt()
        )
        
        // Scrollbar thumb
        val totalHeight = filteredModules.size * moduleHeight
        val thumbHeight = max(10, (moduleAreaHeight * moduleAreaHeight) / totalHeight)
        val thumbY = moduleAreaY + 
            (scrollOffset * (moduleAreaHeight - thumbHeight)) / (totalHeight - moduleAreaHeight)
        
        context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFF80BFFF.toInt())
    }
    
    @Suppress("UnusedParameter", "FunctionOnlyReturningConstant")
    private fun moduleHasSettings(module: ClientModule): Boolean {
        return ClickGuiPanelHelper.moduleHasSettings(module)
    }
    
    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val intMouseX = mouseX.toInt()
        val intMouseY = mouseY.toInt()
        
        if (!isClickWithinBounds(intMouseX, intMouseY)) {
            return false
        }
        
        if (isHeaderClick(intMouseY)) {
            return handleHeaderClick(intMouseX, intMouseY, button)
        }
        
        if (isModuleAreaClick(intMouseY)) {
            return handleModuleClick(intMouseX, intMouseY, button)
        }
        
        return false
    }
    
    private fun isClickWithinBounds(mouseX: Int, mouseY: Int): Boolean {
        val bounds = ClickGuiPanelInteraction.PanelBounds(
            x, y, width, expanded, headerHeight, filteredModules.size, moduleHeight
        )
        return ClickGuiPanelInteraction.isClickWithinBounds(mouseX, mouseY, bounds)
    }
    
    private fun isHeaderClick(mouseY: Int): Boolean {
        return ClickGuiPanelInteraction.isHeaderClick(mouseY, y, headerHeight)
    }
    
    private fun isModuleAreaClick(mouseY: Int): Boolean {
        return ClickGuiPanelInteraction.isModuleAreaClick(mouseY, y, headerHeight, expanded)
    }
    
    private fun handleHeaderClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        val clickData = ClickGuiPanelInteraction.HeaderClickData(
            mouseX, mouseY, button, x, y, width, expanded
        )
        return ClickGuiPanelInteraction.handleHeaderClick(clickData) { newExpanded ->
            val wasExpanded = expanded
            expanded = newExpanded
            isDragging = true
            dragOffsetX = mouseX - x
            dragOffsetY = mouseY - y
            
            // Notify about state change if expansion state changed
            if (wasExpanded != expanded) {
                onPanelStateChanged(category, x, y, expanded)
            }
        }
    }
    
    private fun handleModuleClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        // First priority: Check if click is on scrollbar area
        if (canScroll() && button == 0) {
            val scrollbarX = x + width - 5
            val scrollbarWidth = 3
            val moduleAreaY = y + headerHeight
            val moduleAreaHeight = min(filteredModules.size * moduleHeight, GuiConfig.panelMaxHeight)
            
            // Expanded scrollbar hit area for easier use
            val expandedScrollbarX = x + width - 8
            val expandedScrollbarWidth = 8
            
            if (mouseX >= expandedScrollbarX && mouseX <= expandedScrollbarX + expandedScrollbarWidth && 
                mouseY >= moduleAreaY && mouseY <= moduleAreaY + moduleAreaHeight) {
                
                // Start scroll dragging from scrollbar
                isScrollDragging = true
                scrollDragStartY = mouseY.toDouble()
                return true
            }
        }
        
        // Second priority: Check module clicks
        val moduleIndex = (mouseY - y - headerHeight + scrollOffset) / moduleHeight
        val isValidModuleIndex = moduleIndex >= 0 && moduleIndex < filteredModules.size
        
        if (isValidModuleIndex) {
            val module = filteredModules[moduleIndex]
            val result = handleSpecificModuleClick(module, moduleIndex, button)
            if (result) return true
        }
        
        // Third priority: Handle empty area scrolling
        if (button == 0 && canScroll()) {
            isScrollDragging = true
            scrollDragStartY = mouseY.toDouble()
            return true
        }
        
        return false
    }
    
    private fun handleSpecificModuleClick(module: ClientModule, moduleIndex: Int, button: Int): Boolean {
        return when (button) {
            0 -> {
                // Toggle module
                module.enabled = !module.enabled
                true
            }
            1 -> {
                // Open module settings popup next to the module
                val moduleX = x
                val moduleY = y + headerHeight + moduleIndex * moduleHeight - scrollOffset
                onOpenSettings(module, moduleX, moduleY, width, moduleHeight)
                true
            }
            else -> false
        }
    }
    
    @Suppress("UnusedParameter")
    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (isDragging && button == 0) {
            val newX = mouseX.toInt() - dragOffsetX
            val newY = mouseY.toInt() - dragOffsetY
            
            // Keep panel within screen bounds
            x = max(0, min(newX, mc.window.scaledWidth - width))
            y = max(0, min(newY, mc.window.scaledHeight - height))
            
            // Notify about state change
            onPanelStateChanged(category, x, y, expanded)
            
            return true
        }
        
        if (isScrollDragging && button == 0) {
            // Handle scroll dragging
            val deltaY = mouseY - scrollDragStartY
            val scrollSensitivity = 2.0 // How much to scroll per pixel of mouse movement
            val scrollDelta = (deltaY * scrollSensitivity).toInt()
            
            val maxScroll = max(0, filteredModules.size * moduleHeight - GuiConfig.panelMaxHeight)
            scrollOffset = max(0, min(maxScroll, scrollOffset - scrollDelta))
            
            scrollDragStartY = mouseY
            return true
        }
        
        return false
    }
    
    @Suppress("UnusedParameter")
    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int) {
        isDragging = false
        isScrollDragging = false
    }
    
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!expanded) return false
        
        val intMouseX = mouseX.toInt()
        val intMouseY = mouseY.toInt()
        
        // First priority: Check if mouse is over scrollbar area (if scrollable content exists)
        if (canScroll()) {
            val scrollbarX = x + width - 5
            val scrollbarWidth = 3
            val moduleAreaY = y + headerHeight
            val moduleAreaHeight = min(filteredModules.size * moduleHeight, GuiConfig.panelMaxHeight)
            
            // Expanded scrollbar hit area for easier use
            val expandedScrollbarX = x + width - 8
            val expandedScrollbarWidth = 8
            
            if (intMouseX >= expandedScrollbarX && intMouseX <= expandedScrollbarX + expandedScrollbarWidth && 
                intMouseY >= moduleAreaY && intMouseY <= moduleAreaY + moduleAreaHeight) {
                
                val maxScroll = max(0, filteredModules.size * moduleHeight - GuiConfig.panelMaxHeight)
                scrollOffset = max(0, min(maxScroll, scrollOffset - (amount * 30).toInt()))
                return true
            }
        }
        
        // Second priority: Check if mouse is over module area (for content scrolling)
        if (intMouseX >= x && intMouseX <= x + width && 
            intMouseY >= y + headerHeight && intMouseY <= y + headerHeight + GuiConfig.panelMaxHeight) {
            
            if (canScroll()) {
                val maxScroll = max(0, filteredModules.size * moduleHeight - GuiConfig.panelMaxHeight)
                scrollOffset = max(0, min(maxScroll, scrollOffset - (amount * 30).toInt()))
                return true
            }
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
    
    /**
     * Set the expanded state of the panel
     */
    fun setExpanded(expanded: Boolean) {
        this.expanded = expanded
    }
    
    /**
     * Check if content can be scrolled (i.e., there's more content than visible area)
     */
    private fun canScroll(): Boolean {
        return expanded && filteredModules.size * moduleHeight > GuiConfig.panelMaxHeight
    }
    
    /**
     * Get the expanded state of the panel
     */
    fun getExpanded(): Boolean = expanded
}

/**
 * Helper object for ClickGui panel rendering operations
 */
object ClickGuiPanelRenderer {
    fun renderExpandButton(context: DrawContext, buttonX: Int, buttonY: Int, expanded: Boolean) {
        val buttonSize = 10  // Made smaller for compact tabs
        
        // Plus/minus icon (adjusted for smaller button)
        val centerX = buttonX + buttonSize / 2
        val centerY = buttonY + buttonSize / 2
        
        // Horizontal line for minus
        context.fill(centerX - 3, centerY, centerX + 4, centerY + 1, 0xFFFFFFFF)
        
        // Vertical line for plus
        if (!expanded) {
            context.fill(centerX, centerY - 3, centerX + 1, centerY + 4, 0xFFFFFFFF)
        }
    }
}

/**
 * Helper object for ClickGui panel utilities
 */
object ClickGuiPanelHelper {
    const val HAS_SETTINGS = true // Placeholder
    
    @Suppress("UnusedParameter")
    fun moduleHasSettings(module: ClientModule): Boolean {
        // Simple check - in real implementation, would check module's configuration tree
        return HAS_SETTINGS
    }
}

/**
 * Helper object for ClickGui panel interaction logic
 */
object ClickGuiPanelInteraction {
    
    /**
     * Data class for panel bounds checking
     */
    data class PanelBounds(
        val x: Int,
        val y: Int,
        val width: Int,
        val expanded: Boolean,
        val headerHeight: Int,
        val moduleCount: Int,
        val moduleHeight: Int
    )
    
    /**
     * Data class for header click handling
     */
    data class HeaderClickData(
        val mouseX: Int,
        val mouseY: Int,
        val button: Int,
        val panelX: Int,
        val panelY: Int,
        val panelWidth: Int,
        val expanded: Boolean
    )
    
    fun isClickWithinBounds(mouseX: Int, mouseY: Int, bounds: PanelBounds): Boolean {
        if (mouseX < bounds.x || mouseX > bounds.x + bounds.width || mouseY < bounds.y) {
            return false
        }
        
        val actualHeight = if (bounds.expanded) {
            bounds.headerHeight + min(bounds.moduleCount * bounds.moduleHeight, GuiConfig.panelMaxHeight)
        } else {
            bounds.headerHeight
        }
        return mouseY <= bounds.y + actualHeight
    }
    
    fun isHeaderClick(mouseY: Int, panelY: Int, headerHeight: Int): Boolean {
        return mouseY <= panelY + headerHeight
    }
    
    fun isModuleAreaClick(mouseY: Int, panelY: Int, headerHeight: Int, expanded: Boolean): Boolean {
        return expanded && mouseY > panelY + headerHeight
    }
    
    fun handleHeaderClick(clickData: HeaderClickData, onAction: (Boolean) -> Unit): Boolean {
        // Expand button click (adjusted for smaller button)
        if (clickData.mouseX >= clickData.panelX + clickData.panelWidth - 12) {
            onAction(!clickData.expanded)
            return true
        }
        
        // Start dragging
        if (clickData.button == 0) {
            onAction(clickData.expanded)
            return true
        }
        
        // Right click to expand/collapse
        if (clickData.button == 1) {
            onAction(!clickData.expanded)
            return true
        }
        
        return false
    }
}
