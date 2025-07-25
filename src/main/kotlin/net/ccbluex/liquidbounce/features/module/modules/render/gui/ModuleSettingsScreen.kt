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
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import kotlin.math.max
import kotlin.math.min

/**
 * Screen for configuring module settings
 */
@Suppress("TooManyFunctions")
class ModuleSettingsScreen(
    private val module: ClientModule, 
    private val parent: Screen
) : Screen(Text.literal("${module.name} Settings")) {
    
    private val settingWidgets = mutableListOf<SettingWidget<*>>()
    private var scrollOffset = 0
    private val settingHeight = 25
    private val settingSpacing = 5
    
    override fun init() {
        super.init()
        
        // Back button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Back")) { 
                mc.setScreen(parent)
            }.dimensions(10, height - 30, 60, 20).build()
        )
        
        // Reset button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Reset to Defaults")) { 
                resetToDefaults()
            }.dimensions(width - 130, height - 30, 120, 20).build()
        )
        
        initializeSettingWidgets()
    }
    
    private fun initializeSettingWidgets() {
        settingWidgets.clear()
        
        val startY = 60
        var currentY = startY
        
        try {
            // Dynamically discover module's configurable values
            val values = module.containedValues
            
            for (value in values) {
                val widget = createWidgetForValue(value, 20, currentY)
                if (widget != null) {
                    settingWidgets.add(widget)
                    currentY += settingHeight + settingSpacing
                }
            }
            
        } catch (e: Exception) {
            // Error handling for missing or corrupted module data
            println("Error initializing settings for module ${module.name}: ${e.message}")
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun createWidgetForValue(value: Value<*>, x: Int, y: Int): SettingWidget<*>? {
        return when (value.valueType) {
            ValueType.BOOLEAN -> createBooleanWidget(value, x, y)
            ValueType.FLOAT -> createFloatWidget(value, x, y)
            ValueType.INT -> createIntWidget(value, x, y)
            ValueType.TEXT -> createTextWidget(value, x, y)
            ValueType.CHOOSE -> createChooseWidget(value, x, y)
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createBooleanWidget(value: Value<*>, x: Int, y: Int): BooleanSettingWidget {
        val typedValue = value as Value<Boolean>
        return BooleanSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = WidgetConfig(x = x, y = y),
            onValueChanged = { newValue -> typedValue.set(newValue) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createFloatWidget(value: Value<*>, x: Int, y: Int): FloatSettingWidget {
        val typedValue = value as Value<Float>
        val currentValue = typedValue.get()
        val (min, max) = getRangeForValue(value, 0.0f, 10.0f)
        return FloatSettingWidget(
            name = value.name,
            value = currentValue,
            config = RangeWidgetConfig(x = x, y = y, min = min, max = max),
            onValueChanged = { newValue -> typedValue.set(newValue) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createIntWidget(value: Value<*>, x: Int, y: Int): IntSettingWidget {
        val typedValue = value as Value<Int>
        val currentValue = typedValue.get()
        val (min, max) = getRangeForValue(value, 0, 1000)
        return IntSettingWidget(
            name = value.name,
            value = currentValue,
            config = IntRangeWidgetConfig(x = x, y = y, min = min, max = max),
            onValueChanged = { newValue -> typedValue.set(newValue) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createTextWidget(value: Value<*>, x: Int, y: Int): TextSettingWidget {
        val typedValue = value as Value<String>
        return TextSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = WidgetConfig(x = x, y = y),
            onValueChanged = { newValue -> typedValue.set(newValue) }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createChooseWidget(value: Value<*>, x: Int, y: Int): EnumSettingWidget {
        val chooseValue = value as ChooseListValue<*>
        val currentChoice = chooseValue.get() as NamedChoice
        val choiceNames = chooseValue.choices.map { it.choiceName }.toTypedArray()
        
        return EnumSettingWidget(
            name = value.name,
            value = currentChoice.choiceName,
            choices = choiceNames,
            config = WidgetConfig(x = x, y = y, height = 25),
            onValueChanged = { choiceName -> 
                chooseValue.setByString(choiceName)
            }
        )
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <T : Comparable<T>> getRangeForValue(value: Value<*>, defaultMin: T, defaultMax: T): Pair<T, T> {
        return try {
            // Check if this is a RangedValue
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
            // Log the exception for debugging purposes
            println("Warning: Failed to extract range from value '${value.name}': ${e.message}")
            Pair(defaultMin, defaultMax)
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Fixed background - lighter and cleaner to reduce darkish tint
        context.fill(0, 0, width, height, 0x80000000.toInt()) // Reduced opacity from 90% to 50%
        
        // Title
        val title = "${module.name} Settings"
        val titleWidth = textRenderer.getWidth(title)
        context.drawText(textRenderer, title, (width - titleWidth) / 2, 20, 0xFFFFFF, true)
        
        // Module description
        val description = "Configure settings for the ${module.name} module"
        val descWidth = textRenderer.getWidth(description)
        context.drawText(textRenderer, description, (width - descWidth) / 2, 35, 0xAAAAAA, false)
        
        // Settings area background - lighter to eliminate dark tint
        val settingsAreaY = 55
        val settingsAreaHeight = height - 100
        // Reduced opacity
        context.fill(10, settingsAreaY, width - 10, settingsAreaY + settingsAreaHeight, 0x99000000.toInt())
        context.drawBorder(10, settingsAreaY, width - 20, settingsAreaHeight, 0xFF444444.toInt())
        
        // Render settings with scrolling
        renderSettings(context, mouseX, mouseY, settingsAreaY, settingsAreaHeight)
        
        super.render(context, mouseX, mouseY, delta)
    }
    
    private fun renderSettings(context: DrawContext, mouseX: Int, mouseY: Int, areaY: Int, areaHeight: Int) {
        // Enable scissor for scrolling
        context.enableScissor(10, areaY, width - 10, areaY + areaHeight)
        
        try {
            for (widget in settingWidgets) {
                val adjustedY = widget.y - scrollOffset
                if (adjustedY + settingHeight > areaY && adjustedY < areaY + areaHeight) {
                    // Create a temporary widget with adjusted position
                    val adjustedWidget = createAdjustedWidget(widget, adjustedY)
                    val isHovered = adjustedWidget.isMouseOver(mouseX, mouseY)
                    adjustedWidget.render(context, mouseX, mouseY, isHovered)
                }
            }
        } catch (e: Exception) {
            // Error handling for rendering issues
            context.drawText(textRenderer, "Error rendering settings: ${e.message}", 20, areaY + 20, 0xFF4444, false)
        } finally {
            context.disableScissor()
        }
        
        // Scrollbar if needed
        val totalHeight = settingWidgets.size * (settingHeight + settingSpacing)
        if (totalHeight > areaHeight) {
            renderScrollbar(context, areaY, areaHeight, totalHeight)
        }
    }
    
    private fun createAdjustedWidget(original: SettingWidget<*>, newY: Int): SettingWidget<*> {
        return when (original) {
            is BooleanSettingWidget -> BooleanSettingWidget(
                original.name, 
                original.value, 
                WidgetConfig(original.x, newY)
            )
            is FloatSettingWidget -> FloatSettingWidget(
                original.name, 
                original.value, 
                RangeWidgetConfig(original.x, newY, 0.0f, 10.0f) // Mock min/max, would get from actual setting
            )
            is IntSettingWidget -> IntSettingWidget(
                original.name, 
                original.value,
                IntRangeWidgetConfig(original.x, newY, 0, 1000) // Mock min/max, would get from actual setting  
            )
            is TextSettingWidget -> TextSettingWidget(
                original.name,
                original.value,
                WidgetConfig(original.x, newY)
            )
            is EnumSettingWidget -> EnumSettingWidget(
                original.name,
                original.value,
                original.choices,
                WidgetConfig(original.x, newY, height = 25)
            )
            else -> original
        }
    }
    
    private fun renderScrollbar(context: DrawContext, areaY: Int, areaHeight: Int, totalHeight: Int) {
        val scrollbarX = width - 15
        val scrollbarWidth = 5
        
        // Scrollbar track
        context.fill(scrollbarX, areaY, scrollbarX + scrollbarWidth, areaY + areaHeight, 0x88444444.toInt())
        
        // Scrollbar thumb
        val thumbHeight = max(20, (areaHeight * areaHeight) / totalHeight)
        val maxScroll = totalHeight - areaHeight
        val thumbY = if (maxScroll > 0) {
            areaY + (scrollOffset * (areaHeight - thumbHeight)) / maxScroll
        } else {
            areaY
        }
        
        context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFFAAAAAA.toInt())
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle widget clicks
        for (widget in settingWidgets) {
            if (handleWidgetClick(widget, mouseX, mouseY, button)) {
                return true
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
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
    
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        // Handle slider dragging
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
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
    
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle slider release
        for (widget in settingWidgets) {
            when (widget) {
                is FloatSettingWidget -> widget.mouseReleased(mouseX, mouseY, button)
                is IntSettingWidget -> widget.mouseReleased(mouseX, mouseY, button)
            }
        }
        
        return super.mouseReleased(mouseX, mouseY, button)
    }
    
    override fun mouseScrolled(
        mouseX: Double, 
        mouseY: Double, 
        horizontalAmount: Double, 
        verticalAmount: Double
    ): Boolean {
        val totalHeight = settingWidgets.size * (settingHeight + settingSpacing)
        val areaHeight = height - 100
        
        if (totalHeight > areaHeight) {
            val maxScroll = totalHeight - areaHeight
            scrollOffset = max(0, min(maxScroll, scrollOffset - (verticalAmount * 30).toInt()))
            return true
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Handle widget key presses for text input
        for (widget in settingWidgets) {
            val adjustedY = widget.y - scrollOffset
            val adjustedWidget = createAdjustedWidget(widget, adjustedY)
            
            if (adjustedWidget.keyPressed(keyCode, scanCode, modifiers)) {
                updateWidgetValue(widget, adjustedWidget)
                return true
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        // Handle widget character input for text widgets
        for (widget in settingWidgets) {
            val adjustedY = widget.y - scrollOffset
            val adjustedWidget = createAdjustedWidget(widget, adjustedY)
            
            if (adjustedWidget.charTyped(chr, modifiers)) {
                updateWidgetValue(widget, adjustedWidget)
                return true
            }
        }
        
        return super.charTyped(chr, modifiers)
    }
    
    private fun resetToDefaults() {
        try {
            // Reset module to defaults
            module.restore()
            // Reinitialize the widgets to reflect the reset values
            initializeSettingWidgets()
        } catch (e: Exception) {
            println("Error resetting module settings: ${e.message}")
        }
    }
    
    override fun shouldPause(): Boolean {
        return false
    }
}
