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
    private var scrollOffset = 0
    private var isScrollDragging = false
    private var scrollDragStartY = 0.0
    private var filteredModules = allModules
    var hoveredModuleDescription: String? = null
    private val moduleHeight get() = GuiConfig.moduleHeight + 4 // Add padding
    private val headerHeight get() = GuiConfig.headerHeight

    private val expandedModules = mutableMapOf<ClientModule, Boolean>()
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
        val moduleAreaY = y + headerHeight
        val totalContentHeight = filteredModules.sumOf { module ->
            var height = moduleHeight
            if (expandedModules.getOrDefault(module, false)) {
                height += (moduleSettingWidgets[module]?.size ?: 0) * (SETTING_HEIGHT + SETTING_SPACING)
            }
            height
        }
        val moduleAreaHeight = min(totalContentHeight, GuiConfig.panelMaxHeight)
        
        // Clip rendering to module area
        context.enableScissor(x, moduleAreaY, x + width, moduleAreaY + moduleAreaHeight)
        
        var currentY = moduleAreaY - scrollOffset
        
        for (module in filteredModules) {
            if (currentY >= moduleAreaY + moduleAreaHeight) break // Stop rendering if below visible area

            if (currentY + moduleHeight > moduleAreaY && currentY < moduleAreaY + moduleAreaHeight) {
                renderModule(ModuleRenderData(context, module, x, currentY, mouseX, mouseY))
            }
            currentY += moduleHeight
        }
        
        context.disableScissor()
        
        // Render open dropdown outside scissor
        openDropdown?.let { dropdown ->
            context.matrices.push()
            context.matrices.translate(0.0, -scrollOffset.toDouble(), 0.0)
            dropdown.render(context, mouseX, mouseY + scrollOffset, true)
            context.matrices.pop()
        }

        // Scrollbar if needed
        if (totalContentHeight > moduleAreaHeight) {
            renderScrollbar(context, moduleAreaY, moduleAreaHeight, totalContentHeight)
        }
    }
    
    private fun renderModuleSettings(context: DrawContext, module: ClientModule, startY: Int, mouseX: Int, mouseY: Int): Int {
        var settingsY = startY
        val widgets = moduleSettingWidgets[module] ?: return 0

        for (widget in widgets) {
            widget.y = settingsY
            widget.render(context, mouseX, mouseY, widget.isMouseOver(mouseX, mouseY))
            settingsY += SETTING_HEIGHT + SETTING_SPACING
        }
        return settingsY - startY
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
        
        context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFF80BFFF.toInt())
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

        if (!isClickWithinBounds(intMouseX, intMouseY)) {
            return false
        }
        
        if (isHeaderClick(intMouseY)) {
            return handleHeaderClick(intMouseX, intMouseY, button)
        }
        
        if (isModuleAreaClick(intMouseY)) {
            return handleModuleAreaClick(intMouseX, intMouseY, button)
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
    
    private fun handleModuleAreaClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        var currentY = y + headerHeight - scrollOffset
        for (module in filteredModules) {
            val moduleBottomY = currentY + moduleHeight

            // Check if click is on the module button itself
            if (mouseY >= currentY && mouseY < moduleBottomY) {
                if (handleSpecificModuleClick(module, button)) return true
            }

            currentY = moduleBottomY

            // Check for clicks on settings if expanded
            if (expandedModules.getOrDefault(module, false)) {
                val widgets = moduleSettingWidgets[module]
                if (widgets != null) {
                    for (widget in widgets) {
                        widget.y = currentY // Temporarily set y for hit-testing
                        if (widget.isMouseOver(mouseX, mouseY + scrollOffset)) {
                            if (handleWidgetClick(widget, mouseX.toDouble(), (mouseY + scrollOffset).toDouble(), button)) {
                                return true
                            }
                        }
                        currentY += SETTING_HEIGHT + SETTING_SPACING
                    }
                }
            }
        }

        // If no widget was clicked but we are in the module area, start scrolling
        if (button == 0) {
            isScrollDragging = true
            scrollDragStartY = mouseY.toDouble()
            return true
        }
        return false
    }

    private fun handleWidgetClick(widget: SettingWidget<*>, mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (widget.mouseClicked(mouseX, mouseY, button)) {
            if (widget is SectionHeaderWidget) {
                // This logic is now inside the panel, not a popup
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
            if (dropdown.mouseClicked(mouseX, mouseY + scrollOffset, button)) {
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
                // Open module settings popup next to the module
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
            scrollOffset = max(0, min(maxScroll, (scrollOffset - scrollDelta).toInt()))
            
            scrollDragStartY = mouseY
            return true
        }

        // Delegate drag to setting widgets
        moduleSettingWidgets.values.flatten().forEach { widget ->
            if (widget.isMouseOver(mouseX.toInt(), (mouseY + scrollOffset).toInt())) {
                when (widget) {
                    is FloatSettingWidget -> widget.mouseDragged(mouseX, mouseY + scrollOffset, button)
                    is IntSettingWidget -> widget.mouseDragged(mouseX, mouseY + scrollOffset, button)
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
            if (widget is FloatSettingWidget) widget.mouseReleased(mouseX, mouseY + scrollOffset, button)
            if (widget is IntSettingWidget) widget.mouseReleased(mouseX, mouseY + scrollOffset, button)
        }
    }
    
    fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!expanded) return false
        
        val intMouseX = mouseX.toInt()
        val intMouseY = mouseY.toInt()
        
        if (intMouseX >= x && intMouseX <= x + width &&
            intMouseY >= y + headerHeight && intMouseY <= y + height) {
            if (canScroll()) {
                val totalContentHeight = filteredModules.sumOf { module ->
                    var height = moduleHeight
                    if (expandedModules.getOrDefault(module, false)) {
                        height += (moduleSettingWidgets[module]?.size ?: 0) * (SETTING_HEIGHT + SETTING_SPACING)
                    }
                    height
                }
                val maxScroll = max(0, totalContentHeight - GuiConfig.panelMaxHeight)
                scrollOffset = max(0, min(maxScroll, scrollOffset - (amount * 20).toInt()))
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

    // WIDGET CREATION AND MANAGEMENT LOGIC (from ModuleSettingsPopup)

    private fun initializeSettingsWidgets(module: ClientModule) {
        if (moduleSettingWidgets.containsKey(module)) return

        val widgets = mutableListOf<SettingWidget<*>>()
        val valueCreators = mutableListOf<Pair<Value<*>, Int>>()
        collectValues(module, valueCreators, 0)

        var currentY = 0 // y is set dynamically during render
        for ((value, indent) in valueCreators) {
            val widgetX = this.x + 10 + indent * 10
            val widgetWidth = this.width - 20 - indent * 10
            val widget = createWidgetForValue(value, widgetX, currentY, widgetWidth, module)
            if (widget != null) {
                widgets.add(widget)
                currentY += SETTING_HEIGHT + SETTING_SPACING
            }
        }
        moduleSettingWidgets[module] = widgets
    }

    private fun collectValues(configurable: Configurable, list: MutableList<Pair<Value<*>, Int>>, indent: Int) {
        for (value in configurable.inner) {
            list.add(Pair(value, indent))

            if (value is net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable<*>) {
                collectValues(value.activeChoice, list, indent + 1)
            } else if (value is Configurable) {
                // Could add section support here if needed
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createWidgetForValue(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): SettingWidget<*>? {
        return when (value.valueType) {
            ValueType.BOOLEAN -> createBooleanWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.FLOAT -> createFloatWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.INT -> createIntWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.TEXT -> createTextWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.CHOOSE -> createChooseWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.CHOICE -> createChoiceConfigurableWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.BIND -> createBindWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.LIST, ValueType.BLOCK -> createListWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.COLOR -> createColorWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.MULTI_CHOOSE -> createMultiChooseWidget(value, widgetX, widgetY, widgetWidth, module)
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createBooleanWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): BooleanSettingWidget {
        val typedValue = value as Value<Boolean>
        return BooleanSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                typedValue.set(newValue)
                saveModuleConfiguration(module)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createFloatWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): FloatSettingWidget {
        val typedValue = value as Value<Float>
        val (min, max) = getRangeForValue(value, 0.0f, 10.0f)
        return FloatSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = RangeWidgetConfig(x = widgetX, y = widgetY, min = min, max = max, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                typedValue.set(newValue)
                saveModuleConfiguration(module)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createIntWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): IntSettingWidget {
        val typedValue = value as Value<Int>
        val (min, max) = getRangeForValue(value, 0, 1000)
        return IntSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = IntRangeWidgetConfig(x = widgetX, y = widgetY, min = min, max = max, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                typedValue.set(newValue)
                saveModuleConfiguration(module)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createTextWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): TextSettingWidget {
        val typedValue = value as Value<String>
        return TextSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                value.setByString(newValue)
                saveModuleConfiguration(module)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createChooseWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): EnumSettingWidget {
        val chooseValue = value as ChooseListValue<*>
        val currentChoice = chooseValue.get() as NamedChoice
        val choiceNames = chooseValue.choices.map { it.choiceName }.toTypedArray()

        return EnumSettingWidget(
            name = value.name,
            value = currentChoice.choiceName,
            choices = choiceNames,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { choiceName ->
                chooseValue.setByString(choiceName)
                saveModuleConfiguration(module)
                // Re-initialize widgets for the parent module
                initializeSettingsWidgets(module)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createChoiceConfigurableWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): EnumSettingWidget {
        val choiceConfigurable = value as net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable<*>
        val currentChoice = choiceConfigurable.activeChoice
        val choiceNames = choiceConfigurable.choices.map { it.choiceName }.toTypedArray()

        return EnumSettingWidget(
            name = value.name,
            value = currentChoice.choiceName,
            choices = choiceNames,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { choiceName ->
                choiceConfigurable.setByString(choiceName)
                saveModuleConfiguration(module)
                initializeSettingsWidgets(module)
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Comparable<T>> getRangeForValue(value: Value<*>, defaultMin: T, defaultMax: T): Pair<T, T> {
        return try {
            if (value is RangedValue<*>) {
                val range = value.range
                when (range) {
                    is ClosedFloatingPointRange<*> -> Pair(range.start as T, range.endInclusive as T)
                    is IntRange -> Pair(range.first as T, range.last as T)
                    else -> Pair(defaultMin, defaultMax)
                }
            } else {
                Pair(defaultMin, defaultMax)
            }
        } catch (e: Exception) {
            Pair(defaultMin, defaultMax)
        }
    }

    private fun saveModuleConfiguration(module: ClientModule) {
        try {
            ConfigSystem.storeConfigurable(module)
        } catch (e: Exception) {
            println("Error saving configuration for module ${module.name}: ${e.message}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createBindWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): TextSettingWidget {
        val typedValue = value as Value<InputBind>
        return TextSettingWidget(
            name = value.name,
            value = typedValue.get().boundKey.translationKey,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    saveModuleConfiguration(module)
                } catch (e: Exception) { /* Ignore */ }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createListWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): TextSettingWidget {
        val typedValue = value as ListValue<*, *>
        val valueString = if (typedValue is RegistryListValue<*, *>) {
            val collection = typedValue.get() as Collection<Any>
            try {
                when (typedValue.innerType) {
                    Block::class.java -> {
                        val registry = Registries.BLOCK
                        collection.joinToString(", ") { registry.getId(it as Block).toString() }
                    }
                    Item::class.java -> {
                        val registry = Registries.ITEM
                        collection.joinToString(", ") { registry.getId(it as Item).toString() }
                    }
                    else -> collection.joinToString(", ")
                }
            } catch (e: Exception) {
                collection.joinToString(", ")
            }
        } else {
            typedValue.get().joinToString(", ")
        }

        return TextSettingWidget(
            name = value.name,
            value = valueString,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    saveModuleConfiguration(module)
                } catch (e: Exception) { /* Ignore */ }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createColorWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): TextSettingWidget {
        val typedValue = value as Value<Color4b>
        return TextSettingWidget(
            name = value.name,
            value = "#" + typedValue.get().toARGB().toUInt().toString(16),
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    saveModuleConfiguration(module)
                } catch (e: Exception) { /* Ignore */ }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createMultiChooseWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int, module: ClientModule): TextSettingWidget {
        val typedValue = value as MultiChooseListValue<*>
        val valueString = typedValue.get().joinToString(", ") {
            if (it is Enum<*>) it.name else it.toString()
        }

        return TextSettingWidget(
            name = value.name,
            value = valueString,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    saveModuleConfiguration(module)
                } catch (e: Exception) { /* Ignore */ }
            }
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
