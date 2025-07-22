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

/**
 * Base class for setting widgets in the native ClickGUI
 */
abstract class SettingWidget<T>(
    val name: String,
    var value: T,
    val x: Int,
    val y: Int,
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
 * Boolean toggle setting widget
 */
class BooleanSettingWidget(
    name: String,
    value: Boolean,
    x: Int,
    y: Int,
    width: Int = 200,
    height: Int = 20,
    private val onValueChanged: (Boolean) -> Unit = {}
) : SettingWidget<Boolean>(name, value, x, y, width, height) {
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        renderBackground(context, isHovered)
        
        // Setting name
        context.drawText(mc.textRenderer, name, x + 5, y + 5, 0xFFFFFF, false)
        
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
 * Float/Double slider setting widget
 */
class FloatSettingWidget(
    name: String,
    value: Float,
    x: Int,
    y: Int,
    private val min: Float,
    private val max: Float,
    width: Int = 200,
    height: Int = 20,
    private val onValueChanged: (Float) -> Unit = {}
) : SettingWidget<Float>(name, value, x, y, width, height) {
    
    private var isDragging = false
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        renderBackground(context, isHovered)
        
        // Setting name and value
        val displayText = "$name: ${String.format(java.util.Locale.US, "%.2f", value)}"
        context.drawText(mc.textRenderer, displayText, x + 5, y + 5, 0xFFFFFF, false)
        
        // Slider track
        val sliderY = y + height - 6
        val sliderHeight = 4
        context.fill(x + 5, sliderY, x + width - 5, sliderY + sliderHeight, 0xFF444444.toInt())
        
        // Slider handle
        val progress = (value - min) / (max - min)
        val handleX = (x + 5 + (width - 10) * progress).toInt()
        val handleWidth = 8
        
        context.fill(handleX - handleWidth / 2, sliderY - 2, handleX + handleWidth / 2, sliderY + sliderHeight + 2, 0xFF00AAFF.toInt())
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
 * Integer slider setting widget
 */
class IntSettingWidget(
    name: String,
    value: Int,
    x: Int,
    y: Int,
    private val min: Int,
    private val max: Int,
    width: Int = 200,
    height: Int = 20,
    private val onValueChanged: (Int) -> Unit = {}
) : SettingWidget<Int>(name, value, x, y, width, height) {
    
    private var isDragging = false
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, isHovered: Boolean) {
        renderBackground(context, isHovered)
        
        // Setting name and value
        val displayText = "$name: $value"
        context.drawText(mc.textRenderer, displayText, x + 5, y + 5, 0xFFFFFF, false)
        
        // Slider track
        val sliderY = y + height - 6
        val sliderHeight = 4
        context.fill(x + 5, sliderY, x + width - 5, sliderY + sliderHeight, 0xFF444444.toInt())
        
        // Slider handle
        val progress = (value - min).toFloat() / (max - min).toFloat()
        val handleX = (x + 5 + (width - 10) * progress).toInt()
        val handleWidth = 8
        
        context.fill(handleX - handleWidth / 2, sliderY - 2, handleX + handleWidth / 2, sliderY + sliderHeight + 2, 0xFF00AAFF.toInt())
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
