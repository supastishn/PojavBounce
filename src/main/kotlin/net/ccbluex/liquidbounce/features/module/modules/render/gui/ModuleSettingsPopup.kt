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
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.ChooseListValue
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.BooleanSettingWidget
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.FloatSettingWidget
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.IntSettingWidget
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.SettingWidget
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.WidgetConfig
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.RangeWidgetConfig
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.IntRangeWidgetConfig
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.TextSettingWidget
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.EnumSettingWidget
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import kotlin.math.max
import kotlin.math.min

/**
 * Popup widget that shows module settings next to the module in the ClickGUI
 */
@Suppress("TooManyFunctions")
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
    private var scrollOffset = 0
    private var x = 0
    private var y = 0
    private var height = 0
    private var isVisible = false
    private var isScrollDragging = false
    private var scrollDragStartY = 0.0
    
    init {
        calculatePosition()
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
     * Calculate the optimal position for the popup next to the module
     */
    private fun calculatePosition() {
        val screenWidth = mc.window.scaledWidth
        val screenHeight = mc.window.scaledHeight
        
        // Try to position popup to the right of the module first
        var popupX = moduleX + moduleWidth + 10
        var popupY = moduleY
        
        // If popup would go off the right edge, position it to the left
        if (popupX + POPUP_WIDTH > screenWidth) {
            popupX = moduleX - POPUP_WIDTH - 10
            
            // If it still goes off the left edge, center it horizontally
            if (popupX < 0) {
                popupX = (screenWidth - POPUP_WIDTH) / 2
            }
        }
        
        // Calculate popup height based on number of settings
        val contentHeight = settingWidgets.size * (SETTING_HEIGHT + SETTING_SPACING) + POPUP_PADDING * 2
        height = min(max(contentHeight, POPUP_MIN_HEIGHT), POPUP_MAX_HEIGHT)
        
        // Ensure popup doesn't go off the bottom of the screen
        if (popupY + height > screenHeight) {
            popupY = screenHeight - height - 10
        }
        
        // Ensure popup doesn't go off the top of the screen
        if (popupY < 10) {
            popupY = 10
        }
        
        x = popupX
        y = popupY
    }
    
    /**
     * Initialize setting widgets from module configuration
     */
    private fun initializeSettingWidgets() {
        settingWidgets.clear()
        
        var currentY = y + POPUP_PADDING
        
        try {
            val values = module.containedValues
            
            for (value in values) {
                val widget = createWidgetForValue(value, x + POPUP_PADDING, currentY)
                if (widget != null) {
                    settingWidgets.add(widget)
                    currentY += SETTING_HEIGHT + SETTING_SPACING
                }
            }
        } catch (e: Exception) {
            println("Error initializing settings for module ${module.name}: ${e.message}")
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun createWidgetForValue(value: Value<*>, widgetX: Int, widgetY: Int): SettingWidget<*>? {
        val widgetWidth = POPUP_WIDTH - POPUP_PADDING * 2
        
        return when (value.valueType) {
            ValueType.BOOLEAN -> createBooleanWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.FLOAT -> createFloatWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.INT -> createIntWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.TEXT -> createTextWidget(value, widgetX, widgetY, widgetWidth)
            ValueType.CHOOSE -> createChooseWidget(value, widgetX, widgetY, widgetWidth)
            else -> null
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
                typedValue.set(newValue)
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
        context.fill(x + 2, y + 2, x + POPUP_WIDTH + 2, y + height + 2, 0x88000000.toInt()) // Shadow
        context.fill(x, y, x + POPUP_WIDTH, y + height, 0xFF1A1A1A.toInt()) // Main background
        
        // Popup border
        context.drawBorder(x, y, POPUP_WIDTH, height, GuiConfig.accentColor)
        
        // Title bar
        val titleBarHeight = 20
        context.fill(x, y, x + POPUP_WIDTH, y + titleBarHeight, GuiConfig.headerColor)
        context.fill(x, y + titleBarHeight - 1, x + POPUP_WIDTH, y + titleBarHeight, GuiConfig.accentColor)
        
        // Module name in title bar
        val title = "${module.name} Settings"
        val titleWidth = mc.textRenderer.getWidth(title)
        val titleX = x + (POPUP_WIDTH - titleWidth) / 2
        context.drawText(mc.textRenderer, title, titleX, y + 6, GuiConfig.textColor, false)
        
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
        
        // Enable scissor for scrolling
        context.enableScissor(x, settingsAreaY, x + POPUP_WIDTH, settingsAreaY + settingsAreaHeight)
        
        try {
            for (widget in settingWidgets) {
                val adjustedY = widget.y - scrollOffset
                if (adjustedY + SETTING_HEIGHT > settingsAreaY && adjustedY < settingsAreaY + settingsAreaHeight) {
                    val adjustedWidget = createAdjustedWidget(widget, adjustedY)
                    val isHovered = adjustedWidget.isMouseOver(mouseX, mouseY)
                    adjustedWidget.render(context, mouseX, mouseY, isHovered)
                }
            }
        } finally {
            context.disableScissor()
        }
        
        // Scrollbar if needed
        val totalHeight = settingWidgets.size * (SETTING_HEIGHT + SETTING_SPACING)
        if (totalHeight > settingsAreaHeight) {
            renderScrollbar(context, settingsAreaY, settingsAreaHeight, totalHeight)
        }
    }
    
    private fun createAdjustedWidget(original: SettingWidget<*>, newY: Int): SettingWidget<*> {
        val widgetWidth = POPUP_WIDTH - POPUP_PADDING * 2
        
        return when (original) {
            is BooleanSettingWidget -> BooleanSettingWidget(
                original.name, 
                original.value, 
                WidgetConfig(original.x, newY, widgetWidth)
            )
            is FloatSettingWidget -> FloatSettingWidget(
                original.name, 
                original.value, 
                RangeWidgetConfig(original.x, newY, 0.0f, 10.0f, widgetWidth)
            )
            is IntSettingWidget -> IntSettingWidget(
                original.name, 
                original.value,
                IntRangeWidgetConfig(original.x, newY, 0, 1000, widgetWidth)
            )
            is TextSettingWidget -> TextSettingWidget(
                original.name,
                original.value,
                WidgetConfig(original.x, newY, widgetWidth)
            )
            is EnumSettingWidget -> EnumSettingWidget(
                original.name,
                original.value,
                original.choices,
                WidgetConfig(original.x, newY, widgetWidth)
            )
            else -> original
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
        
        context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, GuiConfig.accentColor)
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
        val closeButtonX = x + POPUP_WIDTH - 16
        val closeButtonY = y + 4
        if (intMouseX >= closeButtonX && intMouseX <= closeButtonX + 12 && 
            intMouseY >= closeButtonY && intMouseY <= closeButtonY + 12) {
            hide()
            return true
        }
        
        // Check if clicking outside popup to close it
        if (!isMouseOver(intMouseX, intMouseY)) {
            hide()
            return false
        }
        
        // Handle widget clicks
        var foundWidgetClick = false
        for (widget in settingWidgets) {
            if (handleWidgetClick(widget, mouseX, mouseY, button)) {
                foundWidgetClick = true
                break
            }
        }
        
        if (!foundWidgetClick && button == 0) {
            // Check if clicking in scrollable area (below title bar, not on widgets)
            val titleBarHeight = 20
            if (intMouseY > y + titleBarHeight && canScroll()) {
                isScrollDragging = true
                scrollDragStartY = mouseY
                return true
            }
        }
        
        return foundWidgetClick || true // Consume click if inside popup
    }
    
    private fun handleWidgetClick(widget: SettingWidget<*>, mouseX: Double, mouseY: Double, button: Int): Boolean {
        val adjustedY = widget.y - scrollOffset
        val adjustedWidget = createAdjustedWidget(widget, adjustedY)
        
        if (adjustedWidget.isMouseOver(mouseX.toInt(), mouseY.toInt())) {
            if (adjustedWidget.mouseClicked(mouseX, mouseY, button)) {
                updateWidgetValue(widget, adjustedWidget)
                return true
            }
        }
        return false
    }
    
    private fun updateWidgetValue(original: SettingWidget<*>, adjusted: SettingWidget<*>) {
        when {
            original is BooleanSettingWidget && adjusted is BooleanSettingWidget -> {
                original.value = adjusted.value
            }
            original is FloatSettingWidget && adjusted is FloatSettingWidget -> {
                original.value = adjusted.value
            }
            original is IntSettingWidget && adjusted is IntSettingWidget -> {
                original.value = adjusted.value
            }
            original is TextSettingWidget && adjusted is TextSettingWidget -> {
                original.value = adjusted.value
            }
            original is EnumSettingWidget && adjusted is EnumSettingWidget -> {
                original.value = adjusted.value
            }
        }
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
        if (isScrollDragging && button == 0) {
            val deltaY = mouseY - scrollDragStartY
            val scrollSensitivity = 2.0 // How much to scroll per pixel of mouse movement
            val scrollDelta = (deltaY * scrollSensitivity).toInt()
            
            val totalHeight = settingWidgets.size * (SETTING_HEIGHT + SETTING_SPACING)
            val areaHeight = height - 20 // Subtract title bar height
            val maxScroll = totalHeight - areaHeight
            
            if (maxScroll > 0) {
                scrollOffset = max(0, min(maxScroll, scrollOffset - scrollDelta))
            }
            
            scrollDragStartY = mouseY
            return true
        }
        
        for (widget in settingWidgets) {
            val adjustedY = widget.y - scrollOffset
            val adjustedWidget = createAdjustedWidget(widget, adjustedY)
            
            when (adjustedWidget) {
                is FloatSettingWidget -> {
                    if (adjustedWidget.mouseDragged(mouseX, mouseY, button)) {
                        (widget as FloatSettingWidget).value = adjustedWidget.value
                        return true
                    }
                }
                is IntSettingWidget -> {
                    if (adjustedWidget.mouseDragged(mouseX, mouseY, button)) {
                        (widget as IntSettingWidget).value = adjustedWidget.value
                        return true
                    }
                }
            }
        }
        
        return false
    }
    
    /**
     * Handle mouse release for sliders
     */
    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isVisible) return false
        
        isScrollDragging = false
        
        for (widget in settingWidgets) {
            when (widget) {
                is FloatSettingWidget -> widget.mouseReleased(mouseX, mouseY, button)
                is IntSettingWidget -> widget.mouseReleased(mouseX, mouseY, button)
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
        
        // Handle widget key presses for text input
        for (widget in settingWidgets) {
            val adjustedY = widget.y - scrollOffset
            val adjustedWidget = createAdjustedWidget(widget, adjustedY)
            
            if (adjustedWidget.keyPressed(keyCode, scanCode, modifiers)) {
                updateWidgetValue(widget, adjustedWidget)
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
            val adjustedY = widget.y - scrollOffset
            val adjustedWidget = createAdjustedWidget(widget, adjustedY)
            
            if (adjustedWidget.charTyped(chr, modifiers)) {
                updateWidgetValue(widget, adjustedWidget)
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
}
