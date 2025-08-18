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
package net.ccbluex.liquidbounce.features.module.modules.render.gui.settings

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text

/**
 * Base class for setting widgets in the native ClickGUI
 */
abstract class SettingWidget<T>(
    val name: String,
    var value: T,
    var x: Int,
    var y: Int,
    val width: Int,
    val height: Int
) {
    
    abstract fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean)
    
    abstract fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean
    
    abstract fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean
    
    open fun charTyped(chr: Char, modifiers: Int): Boolean = false
    
    fun isMouseOver(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
    }
    
    protected fun renderBackground(context: DrawContext, isHovered: Boolean) {
        val backgroundColor = if (isHovered) 0xFF333333.toInt() else 0xFF222222.toInt()
        context.fill(x, y, x + width, y + height, backgroundColor)
        context.drawBorder(x, y, width, height, 0xFF444444.toInt())
    }
}

/**
 * Configuration for setting widgets
 */
data class WidgetConfig(
    val x: Int,
    val y: Int,
    val width: Int = 200,
    val height: Int = 20
)

/**
 * Boolean toggle setting widget
 */
class BooleanSettingWidget(
    name: String,
    value: Boolean,
    config: WidgetConfig,
    private val onValueChanged: (Boolean) -> Unit = {}
) : SettingWidget<Boolean>(name, value, config.x, config.y, config.width, config.height) {
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        renderBackground(context, isHovered)
        
        // Setting name
        context.drawText(mc.textRenderer, Text.literal(name), x + 5, y + 5, 0xFFFFFF, false)
        
        // Toggle switch
        val switchX = x + width - 40
        val switchY = y + 2
        val switchWidth = 35
        val switchHeight = height - 4
        
        val switchColor = if (value) 0xFF00AA00.toInt() else 0xFF440044.toInt()
        context.fill(switchX, switchY, switchX + switchWidth, switchY + switchHeight, switchColor)
        
        // Switch handle
        val handleX = if (value) switchX + switchWidth - 12 else switchX + 2
        val handleY = switchY + 2
        val handleSize = switchHeight - 4
        
        context.fill(handleX, handleY, handleX + handleSize, handleY + handleSize, 0xFFFFFFFF.toInt())
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isMouseOver(mouseX.toInt(), mouseY.toInt())) {
            value = !value
            onValueChanged(value)
            return true
        }
        return false
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
}

/**
 * Range configuration for numeric widgets
 */
data class RangeWidgetConfig(
    val x: Int,
    val y: Int,
    val min: Float,
    val max: Float,
    val width: Int = 200,
    val height: Int = 20
)

/**
 * Float/Double slider setting widget
 */
class FloatSettingWidget(
    name: String,
    value: Float,
    config: RangeWidgetConfig,
    private val onValueChanged: (Float) -> Unit = {}
) : SettingWidget<Float>(name, value, config.x, config.y, config.width, config.height) {
    
    private val min = config.min
    private val max = config.max
    
    private var isDragging = false
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        renderBackground(context, isHovered)
        
        // Setting name and value
        val displayText = "$name: ${String.format(java.util.Locale.US, "%.2f", value)}"
        context.drawText(mc.textRenderer, Text.literal(displayText), x + 5, y + 5, 0xFFFFFF, false)
        
        // Slider track
        val sliderY = y + height - 6
        val sliderHeight = 4
        context.fill(x + 5, sliderY, x + width - 5, sliderY + sliderHeight, 0xFF444444.toInt())
        
        // Slider handle
        val progress = (value - min) / (max - min)
        val handleX = (x + 5 + (width - 10) * progress).toInt()
        val handleWidth = 8
        
        context.fill(
            handleX - handleWidth / 2, 
            sliderY - 2, 
            handleX + handleWidth / 2, 
            sliderY + sliderHeight + 2, 
            0xFF00AAFF.toInt()
        )
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isMouseOver(mouseX.toInt(), mouseY.toInt())) {
            isDragging = true
            updateValue(mouseX)
            return true
        }
        return false
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    
    @Suppress("UnusedParameter")
    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isDragging && button == 0) {
            updateValue(mouseX)
            return true
        }
        return false
    }
    
    @Suppress("UnusedParameter")
    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int) {
        isDragging = false
    }
    
    private fun updateValue(mouseX: Double) {
        val sliderStart = x + 5
        val sliderWidth = width - 10
        val progress = ((mouseX - sliderStart) / sliderWidth).coerceIn(0.0, 1.0)
        
        value = (min + (max - min) * progress).toFloat()
        onValueChanged(value)
    }
}

/**
 * Integer range configuration for integer widgets
 */
data class IntRangeWidgetConfig(
    val x: Int,
    val y: Int,
    val min: Int,
    val max: Int,
    val width: Int = 200,
    val height: Int = 20
)

/**
 * Integer slider setting widget
 */
class IntSettingWidget(
    name: String,
    value: Int,
    config: IntRangeWidgetConfig,
    private val onValueChanged: (Int) -> Unit = {}
) : SettingWidget<Int>(name, value, config.x, config.y, config.width, config.height) {
    
    private val min = config.min
    private val max = config.max
    
    private var isDragging = false
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        renderBackground(context, isHovered)
        
        // Setting name and value
        val displayText = "$name: $value"
        context.drawText(mc.textRenderer, Text.literal(displayText), x + 5, y + 5, 0xFFFFFF, false)
        
        // Slider track
        val sliderY = y + height - 6
        val sliderHeight = 4
        context.fill(x + 5, sliderY, x + width - 5, sliderY + sliderHeight, 0xFF444444.toInt())
        
        // Slider handle
        val progress = (value - min).toFloat() / (max - min).toFloat()
        val handleX = (x + 5 + (width - 10) * progress).toInt()
        val handleWidth = 8
        
        context.fill(
            handleX - handleWidth / 2, 
            sliderY - 2, 
            handleX + handleWidth / 2, 
            sliderY + sliderHeight + 2, 
            0xFF00AAFF.toInt()
        )
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isMouseOver(mouseX.toInt(), mouseY.toInt())) {
            isDragging = true
            updateValue(mouseX)
            return true
        }
        return false
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    
    @Suppress("UnusedParameter")
    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isDragging && button == 0) {
            updateValue(mouseX)
            return true
        }
        return false
    }
    
    @Suppress("UnusedParameter")
    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int) {
        isDragging = false
    }
    
    private fun updateValue(mouseX: Double) {
        val sliderStart = x + 5
        val sliderWidth = width - 10
        val progress = ((mouseX - sliderStart) / sliderWidth).coerceIn(0.0, 1.0)
        
        value = (min + (max - min) * progress).toInt()
        onValueChanged(value)
    }
}

/**
 * Text input setting widget
 */
class TextSettingWidget(
    name: String,
    value: String,
    config: WidgetConfig,
    private val onValueChanged: (String) -> Unit = {}
) : SettingWidget<String>(name, value, config.x, config.y, config.width, config.height) {
    
    private var isEditing = false
    private var editingValue = value
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        renderBackground(context, isHovered)
        
        // Setting name
        context.drawText(mc.textRenderer, Text.literal(name), x + 5, y + 2, 0xFFFFFF, false)
        
        // Text input field
        val fieldY = y + 12
        val fieldHeight = 8
        val displayValue = if (isEditing) editingValue else value
        
        context.fill(x + 5, fieldY, x + width - 5, fieldY + fieldHeight, 0xFF333333.toInt())
        val borderColor = if (isEditing) 0xFF00AAFF.toInt() else 0xFF666666.toInt()
        context.drawBorder(x + 5, fieldY, width - 10, fieldHeight, borderColor)
        
        // Text content
        val textColor = if (isEditing) 0xFFFFFF else 0xCCCCCC
        context.drawText(mc.textRenderer, Text.literal(displayValue), x + 7, fieldY + 1, textColor, false)
        
        // Cursor
        if (isEditing) {
            val textWidth = mc.textRenderer.getWidth(editingValue)
            context.fill(x + 7 + textWidth, fieldY + 1, x + 8 + textWidth, fieldY + 7, 0xFFFFFF)
        }
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isMouseOver(mouseX.toInt(), mouseY.toInt())) {
            isEditing = !isEditing
            if (isEditing) {
                editingValue = value
            } else {
                value = editingValue
                onValueChanged(value)
            }
            return true
        }
        return false
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!isEditing) return false
        
        when (keyCode) {
            257 -> { // Enter
                isEditing = false
                value = editingValue
                onValueChanged(value)
                return true
            }
            256 -> { // Escape
                isEditing = false
                editingValue = value
                return true
            }
            259 -> { // Backspace
                if (editingValue.isNotEmpty()) {
                    editingValue = editingValue.dropLast(1)
                }
                return true
            }
        }
        
        return false
    }
    
    private fun isValidTextChar(chr: Char): Boolean {
        // Allow most printable characters for text input
        return chr.isLetterOrDigit() || chr in " !@#$%^&*()_+-=[]{}|;':\",./<>?`~\\"
    }
    
    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (!isEditing) return false
        
        if (isValidTextChar(chr)) {
            editingValue += chr
            return true
        }
        
        return false
    }
}

/**
 * Dual-slider widget for integer range values (like CPS)
 */
class IntRangeSliderWidget(
    name: String,
    value: IntRange,
    config: IntRangeWidgetConfig,
    private val onValueChanged: (IntRange) -> Unit = {}
) : SettingWidget<IntRange>(name, value, config.x, config.y, config.width, config.height) {
    
    private val min = config.min
    private val max = config.max
    private var isDraggingStart = false
    private var isDraggingEnd = false
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        renderBackground(context, isHovered)
        
        // Setting name and value
        val displayText = "$name: ${value.first}..${value.last}"
        context.drawText(mc.textRenderer, Text.literal(displayText), x + 5, y + 2, 0xFFFFFF, false)
        
        // Slider track
        val sliderY = y + height - 8
        val sliderHeight = 4
        val sliderStart = x + 5
        val sliderWidth = width - 10
        
        context.fill(sliderStart, sliderY, sliderStart + sliderWidth, sliderY + sliderHeight, 0xFF444444.toInt())
        
        // Calculate positions for start and end handles
        val startProgress = (value.first - min).toDouble() / (max - min).toDouble()
        val endProgress = (value.last - min).toDouble() / (max - min).toDouble()
        
        val startHandleX = (sliderStart + sliderWidth * startProgress).toInt()
        val endHandleX = (sliderStart + sliderWidth * endProgress).toInt()
        
        // Highlight range between handles
        context.fill(startHandleX, sliderY, endHandleX, sliderY + sliderHeight, 0xFF0088FF.toInt())
        
        // Draw handles
        val handleWidth = 8
        
        // Start handle
        context.fill(
            startHandleX - handleWidth / 2, 
            sliderY - 2, 
            startHandleX + handleWidth / 2, 
            sliderY + sliderHeight + 2, 
            if (isDraggingStart) 0xFFFFFF00.toInt() else 0xFF00AAFF.toInt()
        )
        
        // End handle
        context.fill(
            endHandleX - handleWidth / 2, 
            sliderY - 2, 
            endHandleX + handleWidth / 2, 
            sliderY + sliderHeight + 2, 
            if (isDraggingEnd) 0xFFFFFF00.toInt() else 0xFF00AAFF.toInt()
        )
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isMouseOver(mouseX.toInt(), mouseY.toInt())) {
            val sliderStart = x + 5
            val sliderWidth = width - 10
            val startProgress = (value.first - min).toDouble() / (max - min).toDouble()
            val endProgress = (value.last - min).toDouble() / (max - min).toDouble()
            
            val startHandleX = sliderStart + sliderWidth * startProgress
            val endHandleX = sliderStart + sliderWidth * endProgress
            
            // Determine which handle is closer
            val distToStart = kotlin.math.abs(mouseX - startHandleX)
            val distToEnd = kotlin.math.abs(mouseX - endHandleX)
            
            if (distToStart < distToEnd) {
                isDraggingStart = true
            } else {
                isDraggingEnd = true
            }
            
            updateValue(mouseX)
            return true
        }
        return false
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    
    @Suppress("UnusedParameter")
    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if ((isDraggingStart || isDraggingEnd) && button == 0) {
            updateValue(mouseX)
            return true
        }
        return false
    }
    
    @Suppress("UnusedParameter")
    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int) {
        isDraggingStart = false
        isDraggingEnd = false
    }
    
    private fun updateValue(mouseX: Double) {
        val sliderStart = x + 5
        val sliderWidth = width - 10
        val progress = ((mouseX - sliderStart) / sliderWidth).coerceIn(0.0, 1.0)
        
        val newValue = (min + (max - min) * progress).toInt()
        
        if (isDraggingStart) {
            val newStart = if (newValue <= value.last) newValue else value.last
            value = newStart..value.last
        } else if (isDraggingEnd) {
            val newEnd = if (newValue >= value.first) newValue else value.first
            value = value.first..newEnd
        }
        
        onValueChanged(value)
    }
}

/**
 * Dual-slider widget for float range values
 */
class FloatRangeSliderWidget(
    name: String,
    value: ClosedFloatingPointRange<Float>,
    config: RangeWidgetConfig,
    private val onValueChanged: (ClosedFloatingPointRange<Float>) -> Unit = {}
) : SettingWidget<ClosedFloatingPointRange<Float>>(name, value, config.x, config.y, config.width, config.height) {
    
    private val min = config.min
    private val max = config.max
    private var isDraggingStart = false
    private var isDraggingEnd = false
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        renderBackground(context, isHovered)
        
        // Setting name and value
        val startStr = String.format(java.util.Locale.US, "%.2f", value.start)
        val endStr = String.format(java.util.Locale.US, "%.2f", value.endInclusive)
        val displayText = "$name: $startStr..$endStr"
        context.drawText(mc.textRenderer, Text.literal(displayText), x + 5, y + 2, 0xFFFFFF, false)
        
        // Slider track
        val sliderY = y + height - 8
        val sliderHeight = 4
        val sliderStart = x + 5
        val sliderWidth = width - 10
        
        context.fill(sliderStart, sliderY, sliderStart + sliderWidth, sliderY + sliderHeight, 0xFF444444.toInt())
        
        // Calculate positions for start and end handles
        val startProgress = (value.start - min) / (max - min)
        val endProgress = (value.endInclusive - min) / (max - min)
        
        val startHandleX = (sliderStart + sliderWidth * startProgress).toInt()
        val endHandleX = (sliderStart + sliderWidth * endProgress).toInt()
        
        // Highlight range between handles
        context.fill(startHandleX, sliderY, endHandleX, sliderY + sliderHeight, 0xFF0088FF.toInt())
        
        // Draw handles
        val handleWidth = 8
        
        // Start handle
        context.fill(
            startHandleX - handleWidth / 2, 
            sliderY - 2, 
            startHandleX + handleWidth / 2, 
            sliderY + sliderHeight + 2, 
            if (isDraggingStart) 0xFFFFFF00.toInt() else 0xFF00AAFF.toInt()
        )
        
        // End handle
        context.fill(
            endHandleX - handleWidth / 2, 
            sliderY - 2, 
            endHandleX + handleWidth / 2, 
            sliderY + sliderHeight + 2, 
            if (isDraggingEnd) 0xFFFFFF00.toInt() else 0xFF00AAFF.toInt()
        )
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isMouseOver(mouseX.toInt(), mouseY.toInt())) {
            val sliderStart = x + 5
            val sliderWidth = width - 10
            val startProgress = (value.start - min) / (max - min)
            val endProgress = (value.endInclusive - min) / (max - min)
            
            val startHandleX = sliderStart + sliderWidth * startProgress
            val endHandleX = sliderStart + sliderWidth * endProgress
            
            // Determine which handle is closer
            val distToStart = kotlin.math.abs(mouseX - startHandleX)
            val distToEnd = kotlin.math.abs(mouseX - endHandleX)
            
            if (distToStart < distToEnd) {
                isDraggingStart = true
            } else {
                isDraggingEnd = true
            }
            
            updateValue(mouseX)
            return true
        }
        return false
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    
    @Suppress("UnusedParameter")
    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if ((isDraggingStart || isDraggingEnd) && button == 0) {
            updateValue(mouseX)
            return true
        }
        return false
    }
    
    @Suppress("UnusedParameter")
    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int) {
        isDraggingStart = false
        isDraggingEnd = false
    }
    
    private fun updateValue(mouseX: Double) {
        val sliderStart = x + 5
        val sliderWidth = width - 10
        val progress = ((mouseX - sliderStart) / sliderWidth).coerceIn(0.0, 1.0)
        
        val newValue = (min + (max - min) * progress).toFloat()
        
        if (isDraggingStart) {
            val newStart = if (newValue <= value.endInclusive) newValue else value.endInclusive
            value = newStart..value.endInclusive
        } else if (isDraggingEnd) {
            val newEnd = if (newValue >= value.start) newValue else value.start
            value = value.start..newEnd
        }
        
        onValueChanged(value)
    }
}

/**
 * Section header widget for regular Configurable objects
 */
class SectionHeaderWidget(
    name: String,
    val isExpanded: Boolean,
    config: WidgetConfig
) : SettingWidget<Boolean>(name, isExpanded, config.x, config.y, config.width, config.height) {

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        val backgroundColor = if (isHovered) 0xFF3A3A3A.toInt() else 0xFF2A2A2A.toInt()
        context.fill(x, y, x + width, y + height, backgroundColor)
        context.drawBorder(x, y, width, height, 0xFF444444.toInt())

        // Arrow
        val arrowChar = if (isExpanded) "▾" else "▸"
        context.drawText(mc.textRenderer, arrowChar, x + 5, y + 7, 0xFFFFFFFF.toInt(), false)

        // Section name
        context.drawText(mc.textRenderer, Text.literal(name), x + 15, y + 7, 0xFFFFFFFF.toInt(), false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isMouseOver(mouseX.toInt(), mouseY.toInt())) {
            // The actual collapse/expand logic is in ModuleSettingsPopup
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Support Space (32) and Enter (257) to toggle the section
        if (keyCode == 32 || keyCode == 257) {
            // Return true to indicate the key was handled
            // The actual toggle logic is handled in ModuleSettingsPopup
            return true
        }
        return false
    }
}

/**
 * Toggleable section header widget for ToggleableConfigurable objects
 * Shows both the toggle state and expand/collapse functionality
 */
class ToggleableSectionHeaderWidget(
    name: String,
    var isEnabled: Boolean,
    val isExpanded: Boolean,
    config: WidgetConfig,
    private val onToggleChanged: (Boolean) -> Unit = {}
) : SettingWidget<Boolean>(name, isExpanded, config.x, config.y, config.width, config.height) {

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        val backgroundColor = if (isHovered) 0xFF3A3A3A.toInt() else 0xFF2A2A2A.toInt()
        context.fill(x, y, x + width, y + height, backgroundColor)
        context.drawBorder(x, y, width, height, 0xFF444444.toInt())

        // Arrow for expand/collapse
        val arrowChar = if (isExpanded) "▾" else "▸"
        context.drawText(mc.textRenderer, arrowChar, x + 5, y + 7, 0xFFFFFFFF.toInt(), false)

        // Section name with color based on enabled state
        val textColor = if (isEnabled) 0xFF80BFFF.toInt() else 0xBBBBBB
        context.drawText(mc.textRenderer, Text.literal(name), x + 15, y + 7, textColor, false)

        // Toggle switch on the right side
        val switchX = x + width - 40
        val switchY = y + 2
        val switchWidth = 35
        val switchHeight = height - 4
        
        val switchColor = if (isEnabled) 0xFF00AA00.toInt() else 0xFF440044.toInt()
        context.fill(switchX, switchY, switchX + switchWidth, switchY + switchHeight, switchColor)
        
        // Switch handle
        val handleX = if (isEnabled) switchX + switchWidth - 12 else switchX + 2
        val handleY = switchY + 2
        val handleSize = switchHeight - 4
        
        context.fill(handleX, handleY, handleX + handleSize, handleY + handleSize, 0xFFFFFFFF.toInt())
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isMouseOver(mouseX.toInt(), mouseY.toInt())) {
            // Check if click is on the toggle switch area (right side)
            val switchX = x + width - 40
            if (mouseX >= switchX) {
                // Toggle the enabled state
                isEnabled = !isEnabled
                onToggleChanged(isEnabled)
            }
            // Always return true to indicate the widget handled the click
            // The expand/collapse logic is handled by the parent component
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Support Space (32) and Enter (257) to toggle the section
        if (keyCode == 32 || keyCode == 257) {
            // Return true to indicate the key was handled
            // The actual toggle logic is handled in ModuleSettingsPopup
            return true
        }
        return false
    }
}

/**
 * Enum/Choice setting widget
 */
class EnumSettingWidget(
    name: String,
    value: String,
    val choices: Array<String>,
    config: WidgetConfig,
    private val onValueChanged: (String) -> Unit = {}
) : SettingWidget<String>(name, value, config.x, config.y, config.width, config.height) {
    
    var isDropdownOpen = false
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        renderBackground(context, isHovered)
        renderMainDisplay(context)
        renderDropdownArrow(context)
        
        if (isDropdownOpen) {
            renderDropdownList(context, mouseX, mouseY)
        }
    }
    
    private fun renderMainDisplay(context: DrawContext) {
        val displayText = "$name: $value"
        context.drawText(mc.textRenderer, Text.literal(displayText), x + 5, y + 5, 0xFFFFFF, false)
    }
    
    private fun renderDropdownArrow(context: DrawContext) {
        val arrowX = x + width - 15
        val arrowY = y + 7
        val arrowColor = if (isDropdownOpen) 0xFF00AAFF.toInt() else 0xFF888888.toInt()
        
        // Simple triangle arrow
        context.fill(arrowX, arrowY, arrowX + 8, arrowY + 1, arrowColor)
        context.fill(arrowX + 1, arrowY + 1, arrowX + 7, arrowY + 2, arrowColor)
        context.fill(arrowX + 2, arrowY + 2, arrowX + 6, arrowY + 3, arrowColor)
        context.fill(arrowX + 3, arrowY + 3, arrowX + 5, arrowY + 4, arrowColor)
        context.fill(arrowX + 4, arrowY + 4, arrowX + 4, arrowY + 5, arrowColor)
    }
    
    private data class DropdownRenderData(
        val choice: String,
        val index: Int,
        val dropdownY: Int,
        val mouseX: Int,
        val mouseY: Int
    )
    
    private fun renderDropdownList(context: DrawContext, mouseX: Int, mouseY: Int) {
        val dropdownY = y + height
        val dropdownHeight = choices.size * 15
        
        context.fill(x, dropdownY, x + width, dropdownY + dropdownHeight, 0xFF222222.toInt())
        context.drawBorder(x, dropdownY, width, dropdownHeight, 0xFF444444.toInt())
        
        for ((index, choice) in choices.withIndex()) {
            val renderData = DropdownRenderData(choice, index, dropdownY, mouseX, mouseY)
            renderDropdownChoice(context, renderData)
        }
    }
    
    private fun renderDropdownChoice(context: DrawContext, data: DropdownRenderData) {
        val choiceY = data.dropdownY + data.index * 15
        val isChoiceHovered = data.mouseX >= x && data.mouseX <= x + width && 
                             data.mouseY >= choiceY && data.mouseY <= choiceY + 15
        val isCurrentChoice = data.choice == value
        
        val bgColor = when {
            isCurrentChoice -> 0xFF004444.toInt()
            isChoiceHovered -> 0xFF333333.toInt()
            else -> 0x00000000
        }
        
        if (bgColor != 0x00000000) {
            context.fill(x, choiceY, x + width, choiceY + 15, bgColor)
        }
        
        val textColor = if (isCurrentChoice) 0x00FFFF else 0xFFFFFF
        context.drawText(mc.textRenderer, Text.literal(data.choice), x + 5, choiceY + 3, textColor, false)
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return false
        
        // Check if clicking on the main widget
        if (isMouseOver(mouseX.toInt(), mouseY.toInt())) {
            isDropdownOpen = !isDropdownOpen
            return true
        }
        
        // Check if clicking on dropdown options
        if (isDropdownOpen) {
            val dropdownY = y + height
            val dropdownHeight = choices.size * 15
            
            if (mouseX >= x && mouseX <= x + width && 
                mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight) {
                
                val choiceIndex = ((mouseY - dropdownY) / 15).toInt()
                if (choiceIndex >= 0 && choiceIndex < choices.size) {
                    value = choices[choiceIndex]
                    onValueChanged(value)
                    isDropdownOpen = false
                    return true
                }
            } else {
                // Clicked outside dropdown, close it
                isDropdownOpen = false
            }
        }
        
        return false
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!isDropdownOpen) return false
        
        when (keyCode) {
            256 -> { // Escape
                isDropdownOpen = false
                return true
            }
        }
        
        return false
    }
}
