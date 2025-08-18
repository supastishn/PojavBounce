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
@file:Suppress("SwallowedException")
package net.ccbluex.liquidbounce.features.module.modules.render.gui

import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.*
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.util.InputUtil
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.client.gui.DrawContext
import kotlin.math.max
import kotlin.math.min

/**
 * Popup widget that shows module settings next to the module in the ClickGUI
 */
@Suppress("TooManyFunctions", "LargeClass")
class ModuleSettingsPopup(
    private val module: ClientModule,
    private val moduleX: Int,
    private val moduleY: Int,
    private val moduleWidth: Int,
    @Suppress("UNUSED_PARAMETER") private val moduleHeight: Int
) {
    
    companion object {
        private const val POPUP_WIDTH = 250
        private const val POPUP_MIN_HEIGHT = 100
        private const val POPUP_MAX_HEIGHT = 300
        private const val SETTING_HEIGHT = 25
        private const val SETTING_SPACING = 5
        private const val POPUP_PADDING = 10
    }
    
    private val settingWidgets = mutableListOf<SettingWidget<*>>()
    private val widgetToValueMap = mutableMapOf<SettingWidget<*>, Value<*>>()
    private var openDropdown: EnumSettingWidget? = null
    private var scrollOffset = 0
    private var x = 0
    private var y = 0
    private var height = 0
    private var isVisible = false
    private var isScrollDragging = false
    private var scrollDragStartY = 0.0
    
    init {
        initializeSettingWidgets()
    }
    
    /**
     * Show the popup
     */
    fun show() {
        isVisible = true
    }
    
    /**
     * Hide the popup and immediately persist any changes
     */
    fun hide() {
        isVisible = false
        openDropdown = null
        try {
            // write this module's config back out
            ConfigSystem.storeConfigurable(module as net.ccbluex.liquidbounce.config.types.nesting.Configurable)
        } catch (e: Exception) {
            println("Error saving ClickGUI settings for ${module.name}: ${e.message}")
        }
    }
    
    /**
     * Check if popup is currently visible
     */
    fun isVisible(): Boolean = isVisible
    
    /**
     * Initialize setting widgets from module configuration and calculate popup position
     */
    private fun initializeSettingWidgets() {
        settingWidgets.clear()
        widgetToValueMap.clear()

        val widgetCreators = mutableListOf<Pair<Value<*>, Int>>() // Value and indent level
        collectValues(module, widgetCreators, 0)

        // Calculate height based on content
        val contentHeight = widgetCreators.size * (SETTING_HEIGHT + SETTING_SPACING) + POPUP_PADDING * 2
        val popupHeight = min(max(contentHeight, POPUP_MIN_HEIGHT), POPUP_MAX_HEIGHT)

        // Calculate final position
        val screenWidth = mc.window.scaledWidth
        val screenHeight = mc.window.scaledHeight
        
        var popupX = moduleX + moduleWidth + 10
        var popupY = moduleY
        
        if (popupX + POPUP_WIDTH > screenWidth) {
            popupX = moduleX - POPUP_WIDTH - 10
            if (popupX < 0) {
                popupX = (screenWidth - POPUP_WIDTH) / 2
            }
        }
        
        if (popupY + popupHeight > screenHeight) {
            popupY = screenHeight - popupHeight - 10
        }
        
        if (popupY < 10) {
            popupY = 10
        }

        // Set final popup properties
        this.x = popupX
        this.y = popupY
        this.height = popupHeight

        // Create widgets with the final correct positions
        var currentY = this.y + POPUP_PADDING
        for ((value, indent) in widgetCreators) {
            val widgetX = this.x + POPUP_PADDING + indent * 10
            val widgetWidth = POPUP_WIDTH - POPUP_PADDING * 2 - indent * 10
            val widget = createWidgetForValue(value, widgetX, currentY, widgetWidth)
            if (widget != null) {
                settingWidgets.add(widget)
                widgetToValueMap[widget] = value
                currentY += SETTING_HEIGHT + SETTING_SPACING
            }
        }
    }

    private fun collectValues(configurable: Configurable, list: MutableList<Pair<Value<*>, Int>>, indent: Int) {
        for (value in configurable.inner) {
            list.add(Pair(value, indent))

            val isSectionExpanded = AccordionStateManager.isSectionExpanded(module, value.name, true)

            if (isSectionExpanded) {
                if (value is net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable<*>) {
                    collectValues(value.activeChoice, list, indent + 1)
                } else if (value is Configurable) {
                    collectValues(value, list, indent + 1)
                }
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun createWidgetForValue(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int): SettingWidget<*>? {
        return when (value.valueType) {
            ValueType.BOOLEAN -> createBooleanWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.FLOAT -> createFloatWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.INT -> createIntWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.TEXT -> createTextWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.CHOOSE -> createChooseWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.CHOICE -> createChoiceConfigurableWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.INT_RANGE -> createIntRangeSliderWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.FLOAT_RANGE -> createFloatRangeSliderWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.BIND -> createBindWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.KEY -> createKeyWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.LIST, ValueType.BLOCK -> createListWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.COLOR -> createColorWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.MULTI_CHOOSE -> createMultiChooseWidget(value, widgetX, widgetY, widgetWidth)
            else -> if (value is Configurable) {
                createSectionHeaderWidget(value, widgetX, widgetY, widgetWidth)
            } else {
                null
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun createBooleanWidget(
        value: Value<*>, 
        widgetX: Int, 
        widgetY: Int, 
        widgetWidth: Int
    ): BooleanSettingWidget {
        val typedValue = value as Value<Boolean>
        return BooleanSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth),
            onValueChanged = { newValue ->
                typedValue.set(newValue)
                saveModuleConfiguration()
            }
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun createFloatWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int): FloatSettingWidget {
        val typedValue = value as Value<Float>
        val currentValue = typedValue.get()
        val (min, max) = getRangeForValue(value, 0.0f, 10.0f)
        return FloatSettingWidget(
            name = value.name,
            value = currentValue,
            config = RangeWidgetConfig(x = widgetX, y = widgetY, min = min, max = max, width = widgetWidth),
            onValueChanged = { newValue ->
                typedValue.set(newValue)
                saveModuleConfiguration()
            }
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun createIntWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int): IntSettingWidget {
        val typedValue = value as Value<Int>
        val currentValue = typedValue.get()
        val (min, max) = getRangeForValue(value, 0, 1000)
        return IntSettingWidget(
            name = value.name,
            value = currentValue,
            config = IntRangeWidgetConfig(x = widgetX, y = widgetY, min = min, max = max, width = widgetWidth),
            onValueChanged = { newValue ->
                typedValue.set(newValue)
                saveModuleConfiguration()
            }
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun createTextWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int): TextSettingWidget {
        val typedValue = value as Value<String>
        return TextSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth),
            onValueChanged = { newValue -> 
                value.setByString(newValue)
                saveModuleConfiguration()
            }
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun createChooseWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int): EnumSettingWidget {
        val chooseValue = value as ChooseListValue<*>
        val currentChoice = chooseValue.get() as NamedChoice
        val choiceNames = chooseValue.choices.map { it.choiceName }.toTypedArray()
        
        return EnumSettingWidget(
            name = value.name,
            value = currentChoice.choiceName,
            choices = choiceNames,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth),
            onValueChanged = { choiceName -> 
                chooseValue.setByString(choiceName)
                saveModuleConfiguration()

                // Re-initialize widgets to show settings for the new choice
                initializeSettingWidgets()
            }
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun createChoiceConfigurableWidget(
        value: Value<*>,
        widgetX: Int,
        widgetY: Int,
        widgetWidth: Int
    ): EnumSettingWidget {
        val choiceConfigurable = value as net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable<*>
        val currentChoice = choiceConfigurable.activeChoice
        val choiceNames = choiceConfigurable.choices.map { it.choiceName }.toTypedArray()
        
        return EnumSettingWidget(
            name = value.name,
            value = currentChoice.choiceName,
            choices = choiceNames,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth),
            onValueChanged = { choiceName ->
                choiceConfigurable.setByString(choiceName)
                saveModuleConfiguration()

                // Re-initialize widgets to show settings for the new choice
                initializeSettingWidgets()
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
            println("Warning: Failed to extract range from value '${value.name}': ${e.message}")
            Pair(defaultMin, defaultMax)
        }
    }


    /**
     * Render the popup
     */
    fun render(context: DrawContext, mouseX: Int, mouseY: Int, @Suppress("UNUSED_PARAMETER") delta: Float) {
        if (!isVisible) return
        
        // Popup background with shadow effect
        context.fill(x + 2, y + 2, x + POPUP_WIDTH + 2, y + height + 2, 0x99000000.toInt()) // Shadow
        context.fill(x, y, x + POPUP_WIDTH, y + height, 0xCC101010.toInt()) // Main background
        
        // Popup border
        context.drawBorder(x, y, POPUP_WIDTH, height, 0xFF505050.toInt())
        
        // Title bar
        val titleBarHeight = 20
        // No separate title bar color
        context.fill(x, y + titleBarHeight - 1, x + POPUP_WIDTH, y + titleBarHeight, 0xFF505050.toInt())
        
        // Module name in title bar
        val title = "${module.name} Settings"
        val titleWidth = mc.textRenderer.getWidth(title)
        val titleX = x + (POPUP_WIDTH - titleWidth) / 2
        context.drawText(mc.textRenderer, title, titleX, y + 6, 0xFFFFFFFF.toInt(), false)
        
        // Close button (X)
        val closeButtonX = x + POPUP_WIDTH - 16
        val closeButtonY = y + 4
        context.drawText(mc.textRenderer, "×", closeButtonX, closeButtonY, 0xFF8888, false)
        
        // Settings area
        renderSettings(context, mouseX, mouseY, titleBarHeight)
    }
    
    private fun renderSettings(context: DrawContext, mouseX: Int, mouseY: Int, titleBarHeight: Int) {
        val settingsAreaY = y + titleBarHeight
        val settingsAreaHeight = height - titleBarHeight
        
        var hoveredValue: Value<*>? = null
        
        // Enable scissor for scrolling
        context.enableScissor(x, settingsAreaY, x + POPUP_WIDTH, settingsAreaY + settingsAreaHeight)
        
        context.matrices.push()
        context.matrices.translate(0.0, -scrollOffset.toDouble(), 0.0)
        
        try {
            for (widget in settingWidgets) {
                if (widget == openDropdown) continue // Defer rendering of open dropdown
                
                val isHovered = widget.isMouseOver(mouseX, mouseY + scrollOffset)
                widget.render(context, mouseX, mouseY + scrollOffset, isHovered)

                if (isHovered) {
                    hoveredValue = widgetToValueMap[widget]
                }
            }
        } finally {
            context.matrices.pop()
            context.disableScissor()
        }

        // Render the open dropdown last and outside the scissor area
        openDropdown?.let { dropdown ->
            context.matrices.push()
            context.matrices.translate(0.0, -scrollOffset.toDouble(), 0.0)
            dropdown.render(context, mouseX, mouseY + scrollOffset, true)
            context.matrices.pop()
        }
        
        // Render description tooltip
        hoveredValue?.let {
            renderDescription(context, mouseX, mouseY, it)
        }
        
        // Scrollbar if needed
        val totalHeight = settingWidgets.size * (SETTING_HEIGHT + SETTING_SPACING)
        if (totalHeight > settingsAreaHeight) {
            renderScrollbar(context, settingsAreaY, settingsAreaHeight, totalHeight)
        }
    }
    
    private fun renderDescription(context: DrawContext, mouseX: Int, mouseY: Int, value: Value<*>) {
        // The description key is derived from the value's own unique key.
        val descriptionKey = "${value.key}.description"
        if (LanguageManager.hasFallbackTranslation(descriptionKey)) {
            val descriptionText = translation(descriptionKey)
            context.drawTooltip(mc.textRenderer, descriptionText, mouseX, mouseY)
        }
    }
    
    private fun renderScrollbar(context: DrawContext, areaY: Int, areaHeight: Int, totalHeight: Int) {
        val scrollbarX = x + POPUP_WIDTH - 8
        val scrollbarWidth = 4
        
        // Scrollbar track
        context.fill(scrollbarX, areaY, scrollbarX + scrollbarWidth, areaY + areaHeight, 0x88444444.toInt())
        
        // Scrollbar thumb
        val thumbHeight = max(10, (areaHeight * areaHeight) / totalHeight)
        val maxScroll = totalHeight - areaHeight
        val thumbY = if (maxScroll > 0) {
            areaY + (scrollOffset * (areaHeight - thumbHeight)) / maxScroll
        } else {
            areaY
        }
        
        context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFF80BFFF.toInt())
    }
    
    /**
     * Check if a point is inside the popup area
     */
    fun isMouseOver(mouseX: Int, mouseY: Int): Boolean {
        return isVisible && mouseX >= x && mouseX <= x + POPUP_WIDTH && 
               mouseY >= y && mouseY <= y + height
    }
    
    /**
     * Handle mouse click events
     */
    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isVisible) return false
        
        val intMouseX = mouseX.toInt()
        val intMouseY = mouseY.toInt()
        
        // Check close button click
        if (isCloseButtonClicked(intMouseX, intMouseY)) {
            hide()
            return true
        }

        // Check if clicking outside popup to close it
        if (!isMouseOver(intMouseX, intMouseY)) {
            hide()
            return false
        }

        // Handle open dropdown first - it's modal
        val dropdownHandled = handleOpenDropdownClick(mouseX, mouseY, button)
        if (dropdownHandled) return true
        
        // Handle widget clicks
        val foundWidgetClick = handleWidgetClicks(mouseX, mouseY, button)
        
        // Handle scroll dragging for non-widget clicks  
        val scrollHandled = if (!foundWidgetClick && button == 0) {
            handleScrollClick(intMouseY, mouseY)
        } else {
            false
        }
        
        return foundWidgetClick || scrollHandled || true // Consume click if inside popup
    }

    private fun isCloseButtonClicked(mouseX: Int, mouseY: Int): Boolean {
        val closeButtonX = x + POPUP_WIDTH - 16
        val closeButtonY = y + 4
        return mouseX >= closeButtonX && mouseX <= closeButtonX + 12 &&
               mouseY >= closeButtonY && mouseY <= closeButtonY + 12
    }

    private fun handleOpenDropdownClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        openDropdown?.let { dropdown ->
            // Pass click to dropdown
            if (dropdown.mouseClicked(mouseX, mouseY + scrollOffset, button)) {
                // Check if dropdown closed itself
                if (!dropdown.isDropdownOpen) {
                    openDropdown = null
                }
            } else {
                // Click was outside the active dropdown, so close it
                dropdown.isDropdownOpen = false
                openDropdown = null
            }
            return true
        }
        return false
    }

    private fun handleWidgetClicks(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for (widget in settingWidgets) {
            if (handleWidgetClick(widget, mouseX, mouseY, button)) {
                if (widget is EnumSettingWidget && widget.isDropdownOpen) {
                    openDropdown = widget
                }
                return true
            }
        }
        return false
    }

    private fun handleScrollClick(mouseY: Int, originalMouseY: Double): Boolean {
        val titleBarHeight = 20
        if (mouseY > y + titleBarHeight && canScroll()) {
            isScrollDragging = true
            scrollDragStartY = originalMouseY
            return true
        }
        return false
    }
    
    private fun handleWidgetClick(widget: SettingWidget<*>, mouseX: Double, mouseY: Double, button: Int): Boolean {
        val localMouseY = mouseY + scrollOffset
        if (widget.mouseClicked(mouseX, localMouseY, button)) {
            if (widget is SectionHeaderWidget) {
                val currentState = AccordionStateManager.isSectionExpanded(module, widget.name, true)
                AccordionStateManager.setSectionExpanded(module, widget.name, !currentState)
                initializeSettingWidgets() // Rebuild the widget list
            }
            return true
        }
        return false
    }
    
    /**
     * Handle mouse dragging for sliders
     */
    fun mouseDragged(
        mouseX: Double, 
        mouseY: Double, 
        button: Int, 
        @Suppress("UNUSED_PARAMETER") deltaX: Double, 
        @Suppress("UNUSED_PARAMETER") deltaY: Double
    ): Boolean {
        if (!isVisible) return false
        
        // Handle scroll dragging first
        if (handleScrollDragging(mouseY, button)) {
            return true
        }
        
        // Handle widget dragging
        return handleWidgetDragging(mouseX, mouseY, button)
    }
    
    private fun handleScrollDragging(mouseY: Double, button: Int): Boolean {
        if (isScrollDragging && button == 0) {
            val deltaY = mouseY - scrollDragStartY
            val scrollSensitivity = 2.0 // How much to scroll per pixel of mouse movement
            val scrollDelta = (deltaY * scrollSensitivity).toInt()
            
            val totalHeight = settingWidgets.size * (SETTING_HEIGHT + SETTING_SPACING)
            val areaHeight = height - 20 // Subtract title bar height
            val maxScroll = totalHeight - areaHeight
            
            if (maxScroll > 0) {
                // Fixed drag direction: dragging down (positive deltaY) should increase scroll offset
                scrollOffset = max(0, min(maxScroll, scrollOffset + scrollDelta))
            }
            
            scrollDragStartY = mouseY
            return true
        }
        return false
    }
    
    private fun handleWidgetDragging(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val localMouseY = mouseY + scrollOffset
        return settingWidgets.any { widget ->
            when (widget) {
                is FloatSettingWidget -> widget.mouseDragged(mouseX, localMouseY, button)
                is IntSettingWidget -> widget.mouseDragged(mouseX, localMouseY, button)
                is IntRangeSliderWidget -> widget.mouseDragged(mouseX, localMouseY, button)
                is FloatRangeSliderWidget -> widget.mouseDragged(mouseX, localMouseY, button)
                else -> false
            }
        }
    }
    
    /**
     * Handle mouse release for sliders
     */
    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isVisible) return false
        
        isScrollDragging = false
        
        val localMouseY = mouseY + scrollOffset
        for (widget in settingWidgets) {
            when (widget) {
                is FloatSettingWidget -> widget.mouseReleased(mouseX, localMouseY, button)
                is IntSettingWidget -> widget.mouseReleased(mouseX, localMouseY, button)
                is IntRangeSliderWidget -> widget.mouseReleased(mouseX, localMouseY, button)
                is FloatRangeSliderWidget -> widget.mouseReleased(mouseX, localMouseY, button)
            }
        }
        
        return false
    }
    
    /**
     * Handle scrolling within the popup
     */
    fun mouseScrolled(
        mouseX: Double, 
        mouseY: Double, 
        @Suppress("UNUSED_PARAMETER") horizontalAmount: Double, 
        verticalAmount: Double
    ): Boolean {
        if (!isVisible || !isMouseOver(mouseX.toInt(), mouseY.toInt())) return false
        
        val totalHeight = settingWidgets.size * (SETTING_HEIGHT + SETTING_SPACING)
        val areaHeight = height - 20 // Subtract title bar height
        
        if (totalHeight > areaHeight) {
            val maxScroll = totalHeight - areaHeight
            // Fixed scroll direction: Minecraft gives negative verticalAmount for scrolling down, positive for up
            // We want scrolling down (negative) to increase scroll, scrolling up (positive) to decrease
            scrollOffset = max(0, min(maxScroll, scrollOffset - (verticalAmount * 30).toInt()))
            return true
        }
        
        return false
    }
    
    /**
     * Handle key press events
     */
    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!isVisible) return false
        
        // ESC key closes popup
        if (keyCode == 256) { // ESC
            hide()
            return true
        }
        
        // Handle widget key presses for text input and section headers
        for (widget in settingWidgets) {
            if (widget.keyPressed(keyCode, scanCode, modifiers)) {
                // If it was a section header that handled the key, toggle it
                if (widget is SectionHeaderWidget && (keyCode == 32 || keyCode == 257)) {
                    val currentState = AccordionStateManager.isSectionExpanded(module, widget.name, true)
                    AccordionStateManager.setSectionExpanded(module, widget.name, !currentState)
                    initializeSettingWidgets() // Rebuild the widget list
                }
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if content can be scrolled
     */
    private fun canScroll(): Boolean {
        val totalHeight = settingWidgets.size * (SETTING_HEIGHT + SETTING_SPACING)
        val areaHeight = height - 20 // Subtract title bar height
        return totalHeight > areaHeight
    }

    /**
     * Handle character input for text widgets
     */
    fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (!isVisible) return false
        
        for (widget in settingWidgets) {
            if (widget.charTyped(chr, modifiers)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Save the module configuration to persist changes
     */
    private fun saveModuleConfiguration() {
        try {
            ConfigSystem.storeConfigurable(module)
        } catch (e: Exception) {
            println("Error saving configuration for module ${module.name}: ${e.message}")
        }
    }

    private fun createSectionHeaderWidget(
        value: Configurable, 
        widgetX: Int, 
        widgetY: Int, 
        widgetWidth: Int
    ): SectionHeaderWidget {
        return SectionHeaderWidget(
            name = value.name,
            isExpanded = AccordionStateManager.isSectionExpanded(module, value.name, true),
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createBindWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int): TextSettingWidget {
        val typedValue = value as Value<InputBind>
        return TextSettingWidget(
            name = value.name,
            value = typedValue.get().boundKey.translationKey,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    saveModuleConfiguration()
                } catch (e: Exception) {
                    // Ignore invalid input
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createKeyWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int): TextSettingWidget {
        val typedValue = value as Value<InputUtil.Key>
        return TextSettingWidget(
            name = value.name,
            value = typedValue.get().translationKey,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    saveModuleConfiguration()
                } catch (e: Exception) {
                    // Ignore invalid input
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createListWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int): TextSettingWidget {
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
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    saveModuleConfiguration()
                } catch (e: Exception) {
                    // Could show an error, for now ignore invalid input.
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createColorWidget(value: Value<*>, widgetX: Int, widgetY: Int, widgetWidth: Int): TextSettingWidget {
        val typedValue = value as Value<Color4b>
        return TextSettingWidget(
            name = value.name,
            value = "#" + typedValue.get().toARGB().toUInt().toString(16),
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    saveModuleConfiguration()
                } catch (e: Exception) {
                    // Could show an error, for now ignore invalid input.
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createMultiChooseWidget(
        value: Value<*>,
        widgetX: Int,
        widgetY: Int,
        widgetWidth: Int
    ): TextSettingWidget {
        val typedValue = value as MultiChooseListValue<*>
        val valueString = typedValue.get().joinToString(", ") {
            if (it is Enum<*>) it.name else it.toString()
        }

        return TextSettingWidget(
            name = value.name,
            value = valueString,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    saveModuleConfiguration()
                } catch (e: Exception) {
                    // Could show an error, for now ignore invalid input.
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createIntRangeSliderWidget(
        value: Value<*>,
        widgetX: Int,
        widgetY: Int,
        widgetWidth: Int
    ): IntRangeSliderWidget {
        val typedValue = value as Value<IntRange>
        val currentValue = typedValue.get()
        val (min, max) = getRangeForValue(value, 0, 100)
        return IntRangeSliderWidget(
            name = value.name,
            value = currentValue,
            config = IntRangeWidgetConfig(x = widgetX, y = widgetY, min = min, max = max, width = widgetWidth),
            onValueChanged = { newValue ->
                typedValue.set(newValue)
                saveModuleConfiguration()
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createFloatRangeSliderWidget(
        value: Value<*>,
        widgetX: Int,
        widgetY: Int,
        widgetWidth: Int
    ): FloatRangeSliderWidget {
        val typedValue = value as Value<ClosedFloatingPointRange<Float>>
        val currentValue = typedValue.get()
        val (min, max) = getRangeForValue(value, 0.0f, 10.0f)
        return FloatRangeSliderWidget(
            name = value.name,
            value = currentValue,
            config = RangeWidgetConfig(x = widgetX, y = widgetY, min = min, max = max, width = widgetWidth),
            onValueChanged = { newValue ->
                typedValue.set(newValue)
                saveModuleConfiguration()
            }
        )
    }
}
