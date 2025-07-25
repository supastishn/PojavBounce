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
        
        // Category name (adjusted for smaller tabs)
        val categoryName = category.name.lowercase().replaceFirstChar { it.uppercase() }
        context.drawText(mc.textRenderer, categoryName, x + 4, y + 4, GuiConfig.textColor, false)
        
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
            renderData.module.running -> GuiConfig.enabledModuleColor
            isHovered -> GuiConfig.hoverColor
            else -> 0xFF111111.toInt()
        }
        renderData.context.fill(
            renderData.moduleX, 
            renderData.moduleY, 
            renderData.moduleX + width, 
            renderData.moduleY + moduleHeight, 
            bgColor
        )
        
        // Module border
        renderData.context.fill(
            renderData.moduleX, 
            renderData.moduleY + moduleHeight - 1, 
            renderData.moduleX + width, 
            renderData.moduleY + moduleHeight, 
            GuiConfig.borderColor
        )
        
        // Module name (adjusted for smaller tabs)
        val textColor = if (renderData.module.running) GuiConfig.accentColor else 0xBBBBBB
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
        
        context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFFAAAAAA.toInt())
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
    
    private fun handleModuleClick(@Suppress("UnusedParameter") mouseX: Int, mouseY: Int, button: Int): Boolean {
        val moduleIndex = (mouseY - y - headerHeight + scrollOffset) / moduleHeight
        if (moduleIndex >= 0 && moduleIndex < filteredModules.size) {
            val module = filteredModules[moduleIndex]
            
            when (button) {
                0 -> {
                    // Toggle module
                    module.enabled = !module.enabled
                    return true
                }
                1 -> {
                    // Open module settings popup next to the module
                    val moduleX = x
                    val moduleY = y + headerHeight + moduleIndex * moduleHeight - scrollOffset
                    onOpenSettings(module, moduleX, moduleY, width, moduleHeight)
                    return true
                }
            }
        }
        return false
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
    
    /**
     * Set the expanded state of the panel
     */
    fun setExpanded(expanded: Boolean) {
        this.expanded = expanded
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
        
        // Button background
        context.fill(buttonX, buttonY, buttonX + buttonSize, buttonY + buttonSize, 0x88444444.toInt())
        
        // Plus/minus icon (adjusted for smaller button)
        val centerX = buttonX + buttonSize / 2
        val centerY = buttonY + buttonSize / 2
        
        // Horizontal line (smaller)
        context.fill(centerX - 3, centerY - 1, centerX + 3, centerY + 1, 0xFFFFFF)
        
        // Vertical line (only for collapsed state, smaller)
        if (!expanded) {
            context.fill(centerX - 1, centerY - 3, centerX + 1, centerY + 3, 0xFFFFFF)
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
            bounds.headerHeight + min(bounds.moduleCount * bounds.moduleHeight, 300)
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
