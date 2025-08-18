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

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.*
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.*
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.block.Block
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.Item
import net.minecraft.registry.Registries
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
    private var scrollOffset = 0.0
    private var isScrollDragging = false
    private var scrollDragStartY = 0.0
    private var filteredModules = allModules
    var hoveredModuleDescription: String? = null
    private val moduleHeight get() = GuiConfig.moduleHeight + 4 // Add padding
    private val headerHeight get() = GuiConfig.headerHeight

    private val expandedModules = mutableMapOf<ClientModule, Boolean>()
    private val expandedSettingSections = mutableMapOf<String, Boolean>()
    private val moduleSettingWidgets = mutableMapOf<ClientModule, List<SettingWidget<*>>>()
    private var openDropdown: EnumSettingWidget? = null

    companion object {
        private const val SETTING_HEIGHT = 18
        private const val SETTING_SPACING = 2
    }
    
    @Suppress("UnusedParameter")
    fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        hoveredModuleDescription = null // Reset every frame

        // Calculate actual height based on expansion state
        val modulesAndSettingsHeight = filteredModules.sumOf { module ->
            var height = moduleHeight
            if (expandedModules.getOrDefault(module, false)) {
                height += (moduleSettingWidgets[module]?.size ?: 0) * (SETTING_HEIGHT + SETTING_SPACING)
            }
            height
        }
        val actualHeight = if (expanded) {
            headerHeight + min(modulesAndSettingsHeight, GuiConfig.panelMaxHeight)
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
        context.drawText(mc.textRenderer, categoryName, x + 4, y + 4, 0xFFFFFFFF.toInt(), false)
        
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
        val moduleAreaData = calculateModuleAreaData()
        
        // Clip rendering to module area
        context.enableScissor(x, moduleAreaData.y, x + width, moduleAreaData.y + moduleAreaData.height)
        
        renderFilteredModules(context, mouseX, mouseY, moduleAreaData)
        
        context.disableScissor()
        
        // Render open dropdown outside scissor
        openDropdown?.let { dropdown ->
            // The dropdown's position is already relative to the screen, so we don't need to translate.
            // We pass the raw mouse coordinates as the widget will handle hover checks internally.
            dropdown.render(context, mouseX, mouseY, true)
        }

        // Scrollbar if needed
        if (moduleAreaData.totalContentHeight > moduleAreaData.height) {
            renderScrollbar(context, moduleAreaData.y, moduleAreaData.height, moduleAreaData.totalContentHeight)
        }
    }
    
    private data class ModuleAreaData(
        val y: Int,
        val height: Int,
        val totalContentHeight: Int
    )
    
    private fun calculateModuleAreaData(): ModuleAreaData {
        val moduleAreaY = y + headerHeight
        val totalContentHeight = filteredModules.sumOf { module ->
            var height = moduleHeight
            if (expandedModules.getOrDefault(module, false)) {
                height += (moduleSettingWidgets[module]?.size ?: 0) * (SETTING_HEIGHT + SETTING_SPACING)
            }
            height
        }
        val moduleAreaHeight = min(totalContentHeight, GuiConfig.panelMaxHeight)
        
        return ModuleAreaData(moduleAreaY, moduleAreaHeight, totalContentHeight)
    }
    
    private data class RenderContext(
        val context: DrawContext,
        val mouseX: Int,
        val mouseY: Int,
        val moduleAreaData: ModuleAreaData
    )
    
    private fun renderFilteredModules(context: DrawContext, mouseX: Int, mouseY: Int, moduleAreaData: ModuleAreaData) {
        val renderContext = RenderContext(context, mouseX, mouseY, moduleAreaData)
        var currentY = moduleAreaData.y - scrollOffset.toInt()
        
        for (module in filteredModules) {
            if (currentY >= moduleAreaData.y + moduleAreaData.height) break
            
            currentY = renderSingleModule(renderContext, module, currentY)
        }
    }
    
    private fun renderSingleModule(renderContext: RenderContext, module: ClientModule, initialY: Int): Int {
        var currentY = initialY
        
        // Render the module itself
        if (isInVisibleArea(currentY, moduleHeight, renderContext.moduleAreaData)) {
            val moduleData = ModuleRenderData(
                renderContext.context, module, x, currentY, renderContext.mouseX, renderContext.mouseY
            )
            renderModule(moduleData)
        }
        currentY += moduleHeight
        
        // Render settings if module is expanded
        if (expandedModules.getOrDefault(module, false)) {
            currentY = renderModuleSettings(renderContext, module, currentY)
        }
        
        return currentY
    }
    
    private fun renderModuleSettings(renderContext: RenderContext, module: ClientModule, initialY: Int): Int {
        var currentY = initialY
        val settingsWidgets = moduleSettingWidgets[module] ?: emptyList()
        
        for (widget in settingsWidgets) {
            if (currentY >= renderContext.moduleAreaData.y + renderContext.moduleAreaData.height) break
            
            if (isInVisibleArea(currentY, SETTING_HEIGHT, renderContext.moduleAreaData)) {
                // Update widget position to match current panel position
                widget.x = x + 10 // Indented from panel edge
                widget.y = currentY
                val isHovered = renderContext.mouseX >= widget.x && renderContext.mouseX <= widget.x + widget.width && 
                              renderContext.mouseY >= widget.y && renderContext.mouseY <= widget.y + widget.height
                widget.render(renderContext.context, renderContext.mouseX, renderContext.mouseY, isHovered)
            }
            currentY += SETTING_HEIGHT + SETTING_SPACING
        }
        
        return currentY
    }
    
    private fun isInVisibleArea(y: Int, height: Int, moduleAreaData: ModuleAreaData): Boolean {
        return y + height > moduleAreaData.y && y < moduleAreaData.y + moduleAreaData.height
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
        
        if (isHovered) {
            hoveredModuleDescription = renderData.module.description.get()
        }
        
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
        val textColor = if (renderData.module.running) 0xFF80BFFF.toInt() else 0xBBBBBB
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
    
    private fun renderScrollbar(context: DrawContext, moduleAreaY: Int, moduleAreaHeight: Int, totalHeight: Int) {
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
        val thumbHeight = max(10, (moduleAreaHeight * moduleAreaHeight) / totalHeight)
        val thumbY = moduleAreaY + 
            (scrollOffset * (moduleAreaHeight - thumbHeight)) / (totalHeight - moduleAreaHeight)
        
        context.fill(
            scrollbarX, thumbY.toInt(), 
            scrollbarX + scrollbarWidth, thumbY.toInt() + thumbHeight, 
            0xFF80BFFF.toInt()
        )
    }
    
    @Suppress("UnusedParameter", "FunctionOnlyReturningConstant")
    private fun moduleHasSettings(module: ClientModule): Boolean {
        return ClickGuiPanelHelper.moduleHasSettings(module)
    }
    
    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val intMouseX = mouseX.toInt()
        val intMouseY = mouseY.toInt()
        
        // Handle open dropdown first
        if (handleOpenDropdownClick(mouseX, mouseY, button)) {
            return true
        }

        // Check if click is within bounds and handle accordingly
        return if (!isClickWithinBounds(intMouseX, intMouseY)) {
            false
        } else {
            when {
                isHeaderClick(intMouseY) -> {
                    handleHeaderClick(intMouseX, intMouseY, button)
                }
                isModuleAreaClick(intMouseY) -> {
                    handleModuleAreaClick(intMouseX, intMouseY, button)
                }
                else -> {
                    false
                }
            }
        }
    }
    
    private fun isClickWithinBounds(mouseX: Int, mouseY: Int): Boolean {
        val totalContentHeight = filteredModules.sumOf { module ->
            var height = moduleHeight
            if (expandedModules.getOrDefault(module, false)) {
                height += (moduleSettingWidgets[module]?.size ?: 0) * (SETTING_HEIGHT + SETTING_SPACING)
            }
            height
        }
        val actualHeight = if (expanded) {
            headerHeight + min(totalContentHeight, GuiConfig.panelMaxHeight)
        } else {
            headerHeight
        }
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + actualHeight
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
    
    private fun handleModuleAreaClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        var currentY = y + headerHeight - scrollOffset.toInt()
        
        for (module in filteredModules) {
            val moduleBottomY = currentY + moduleHeight

            // Check if click is on the module button itself
            if (isClickOnModule(mouseY, currentY, moduleBottomY)) {
                if (handleSpecificModuleClick(module, button)) return true
            }

            currentY = moduleBottomY

            // Check for clicks on settings if expanded
            val settingsClickResult = handleModuleSettingsClick(module, mouseX, mouseY, button, currentY)
            if (settingsClickResult.handled) return true
            
            currentY = settingsClickResult.updatedY
        }

        return handleScrollingClick(mouseY, button)
    }
    
    private fun isClickOnModule(mouseY: Int, currentY: Int, moduleBottomY: Int): Boolean {
        return mouseY >= currentY && mouseY < moduleBottomY
    }
    
    private data class SettingsClickResult(val handled: Boolean, val updatedY: Int)
    
    private fun handleModuleSettingsClick(
        module: ClientModule, 
        mouseX: Int, 
        mouseY: Int, 
        button: Int, 
        startY: Int
    ): SettingsClickResult {
        var currentY = startY
        
        if (!expandedModules.getOrDefault(module, false)) {
            return SettingsClickResult(false, currentY)
        }
        
        val widgets = moduleSettingWidgets[module] ?: return SettingsClickResult(false, currentY)
        
        for (widget in widgets) {
            // Update widget position for hit-testing
            widget.x = x + 10 // Indented from panel edge
            widget.y = currentY
            
            // Fixed hit testing: account for scroll offset correctly
            if (widget.isMouseOver(mouseX, mouseY)) {
                val widgetClick = handleWidgetClick(
                    module,
                    widget, 
                    mouseX.toDouble(), 
                    mouseY.toDouble(), 
                    button
                )
                if (widgetClick) {
                    return SettingsClickResult(true, currentY)
                }
            }
            currentY += SETTING_HEIGHT + SETTING_SPACING
        }
        return SettingsClickResult(false, currentY)
    }
    
    private fun handleScrollingClick(mouseY: Int, button: Int): Boolean {
        return if (button == 0) {
            isScrollDragging = true
            scrollDragStartY = mouseY.toDouble()
            true
        } else {
            false
        }
    }

    private fun handleWidgetClick(
        module: ClientModule, 
        widget: SettingWidget<*>, 
        mouseX: Double, 
        mouseY: Double, 
        button: Int
    ): Boolean {
        if (widget.mouseClicked(mouseX, mouseY, button)) {
            if (widget is SectionHeaderWidget || widget is ToggleableSectionHeaderWidget) {
                expandedSettingSections[widget.name] = !expandedSettingSections.getOrDefault(widget.name, true)
                initializeSettingsWidgets(module)
            }
            if (widget is EnumSettingWidget && widget.isDropdownOpen) {
                openDropdown = widget
            }
            return true
        }
        return false
    }

    private fun handleOpenDropdownClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        openDropdown?.let { dropdown ->
            // Pass raw mouse coordinates as widget positions are screen-relative.
            if (dropdown.mouseClicked(mouseX, mouseY, button)) {
                if (!dropdown.isDropdownOpen) {
                    openDropdown = null
                }
            } else {
                dropdown.isDropdownOpen = false
                openDropdown = null
            }
            return true
        }
        return false
    }

    private fun handleSpecificModuleClick(module: ClientModule, button: Int): Boolean {
        return when (button) {
            0 -> {
                // Toggle module
                module.enabled = !module.enabled
                true
            }
            1 -> {
                // Toggle settings expansion
                if (moduleHasSettings(module)) {
                    val isExpanded = expandedModules.getOrDefault(module, false)
                    if (!isExpanded) {
                        initializeSettingsWidgets(module)
                    }
                    expandedModules[module] = !isExpanded
                }
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
            val scrollSensitivity = 1.0 // Adjusted sensitivity
            val scrollDelta = (deltaY * scrollSensitivity)
            
            val totalContentHeight = filteredModules.sumOf { module ->
                var height = moduleHeight
                if (expandedModules.getOrDefault(module, false)) {
                    height += (moduleSettingWidgets[module]?.size ?: 0) * (SETTING_HEIGHT + SETTING_SPACING)
                }
                height
            }
            val maxScroll = max(0, totalContentHeight - GuiConfig.panelMaxHeight)
            // Fixed drag direction: dragging down (positive deltaY) should increase scroll offset
            scrollOffset = (scrollOffset + scrollDelta).coerceIn(0.0, maxScroll.toDouble())
            
            scrollDragStartY = mouseY
            return true
        }

        // Delegate drag to setting widgets
        if (button == 0) {
            var currentY = y + headerHeight - scrollOffset.toInt()
            for (module in filteredModules) {
                currentY += moduleHeight

                if (expandedModules.getOrDefault(module, false)) {
                    val widgets = moduleSettingWidgets[module] ?: continue
                    for (widget in widgets) {
                        widget.y = currentY // Update position before passing event

                        // Let the widget handle the drag event; it knows if it's being dragged
                        when (widget) {
                            is FloatSettingWidget -> widget.mouseDragged(mouseX, mouseY, button)
                            is IntSettingWidget -> widget.mouseDragged(mouseX, mouseY, button)
                            is IntRangeSliderWidget -> widget.mouseDragged(mouseX, mouseY, button)
                            is FloatRangeSliderWidget -> widget.mouseDragged(mouseX, mouseY, button)
                        }

                        currentY += SETTING_HEIGHT + SETTING_SPACING
                    }
                }
            }
        }
        
        return false
    }
    
    @Suppress("UnusedParameter")
    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int) {
        isDragging = false
        isScrollDragging = false
        moduleSettingWidgets.values.flatten().forEach { widget ->
            if (widget is FloatSettingWidget) widget.mouseReleased(mouseX, mouseY, button)
            if (widget is IntSettingWidget) widget.mouseReleased(mouseX, mouseY, button)
            if (widget is IntRangeSliderWidget) widget.mouseReleased(mouseX, mouseY, button)
            if (widget is FloatRangeSliderWidget) widget.mouseReleased(mouseX, mouseY, button)
        }
    }
    
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!expanded) return false
        
        val intMouseX = mouseX.toInt()
        val intMouseY = mouseY.toInt()
        
        // Calculate actual panel height (same as in render method)
        val totalContentHeight = filteredModules.sumOf { module ->
            var height = moduleHeight
            if (expandedModules.getOrDefault(module, false)) {
                height += (moduleSettingWidgets[module]?.size ?: 0) * (SETTING_HEIGHT + SETTING_SPACING)
            }
            height
        }
        val actualPanelHeight = headerHeight + min(totalContentHeight, GuiConfig.panelMaxHeight)
        
        if (intMouseX >= x && intMouseX <= x + width &&
            intMouseY >= y + headerHeight && intMouseY <= y + actualPanelHeight) {
            if (canScroll()) {
                val maxScroll = max(0, totalContentHeight - GuiConfig.panelMaxHeight)
                // Fixed scroll direction: Minecraft gives negative amount for scrolling down, positive for up
                // We want scrolling down (negative) to increase scroll, scrolling up (positive) to decrease
                scrollOffset = (scrollOffset - amount * 20).coerceIn(0.0, maxScroll.toDouble())
                return true
            }
        }
        
        return false
    }
    
    /**
     * Handle key press events and forward to active setting widgets
     */
    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!expanded) return false
        
        return forwardInputToExpandedModules { widgets ->
            widgets.any { widget ->
                widget.keyPressed(keyCode, scanCode, modifiers)
            }
        }
    }

    /**
     * Handle character input and forward to active setting widgets
     */
    fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (!expanded) return false
        
        return forwardInputToExpandedModules { widgets ->
            widgets.any { widget ->
                widget.charTyped(chr, modifiers)
            }
        }
    }

    /**
     * Helper method to forward input events to expanded modules
     */
    private fun forwardInputToExpandedModules(inputHandler: (List<SettingWidget<*>>) -> Boolean): Boolean {
        for (module in filteredModules) {
            if (expandedModules.getOrDefault(module, false)) {
                val widgets = moduleSettingWidgets[module] ?: continue
                if (inputHandler(widgets)) {
                    return true
                }
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
        scrollOffset = 0.0
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
        if (!expanded) return false
        val totalContentHeight = filteredModules.sumOf { module ->
            var height = moduleHeight
            if (expandedModules.getOrDefault(module, false)) {
                height += (moduleSettingWidgets[module]?.size ?: 0) * (SETTING_HEIGHT + SETTING_SPACING)
            }
            height
        }
        return totalContentHeight > GuiConfig.panelMaxHeight
    }
    
    /**
     * Get the expanded state of the panel
     */
    fun getExpanded(): Boolean = expanded

    // WIDGET CREATION AND MANAGEMENT LOGIC (using factory)

    private fun initializeSettingsWidgets(module: ClientModule) {
        ClickGuiPanelWidgetFactory.initializeSettingsWidgets(
            module, this.x, this.width, moduleSettingWidgets, expandedSettingSections
        )
    }
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
        context.fill(centerX - 3, centerY, centerX + 4, centerY + 1, 0xFFFFFFFF.toInt())
        
        // Vertical line for plus
        if (!expanded) {
            context.fill(centerX, centerY - 3, centerX + 1, centerY + 4, 0xFFFFFFFF.toInt())
        }
    }
}

/**
 * Helper object for ClickGui panel utilities
 */
object ClickGuiPanelHelper {
    fun moduleHasSettings(module: ClientModule): Boolean {
        // Simple check - in real implementation, would check module's configuration tree
        return module.inner.isNotEmpty()
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
