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

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.BooleanSettingWidget
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.FloatSettingWidget
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.IntSettingWidget
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.SettingWidget
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
class ModuleSettingsScreen(private val module: Module, private val parent: Screen) : Screen(Text.literal("${module.name} Settings")) {
    
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
        
        // Mock settings for demonstration - in real implementation, would iterate through module's configurable
        val startY = 60
        var currentY = startY
        
        try {
            // Example boolean setting
            settingWidgets.add(
                BooleanSettingWidget(
                    name = "Enabled",
                    value = module.running,
                    x = 20,
                    y = currentY,
                    onValueChanged = { module.running = it }
                )
            )
            currentY += settingHeight + settingSpacing
            
            // Example float settings (mock)
            settingWidgets.add(
                FloatSettingWidget(
                    name = "Range",
                    value = 4.0f,
                    x = 20,
                    y = currentY,
                    min = 1.0f,
                    max = 10.0f,
                    onValueChanged = { /* save to module config */ }
                )
            )
            currentY += settingHeight + settingSpacing
            
            // Example int setting (mock)
            settingWidgets.add(
                IntSettingWidget(
                    name = "Delay",
                    value = 100,
                    x = 20,
                    y = currentY,
                    min = 0,
                    max = 1000,
                    onValueChanged = { /* save to module config */ }
                )
            )
            currentY += settingHeight + settingSpacing
            
            // Example boolean setting (mock)
            settingWidgets.add(
                BooleanSettingWidget(
                    name = "Through Walls",
                    value = false,
                    x = 20,
                    y = currentY,
                    onValueChanged = { /* save to module config */ }
                )
            )
            
        } catch (e: Exception) {
            // Error handling for missing or corrupted module data
            println("Error initializing settings for module ${module.name}: ${e.message}")
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Background
        context.fillGradient(0, 0, width, height, 0x80000000.toInt(), 0x80222222.toInt())
        
        // Title
        val title = "${module.name} Settings"
        val titleWidth = textRenderer.getWidth(title)
        context.drawText(textRenderer, title, (width - titleWidth) / 2, 20, 0xFFFFFF, true)
        
        // Module description
        val description = "Configure settings for the ${module.name} module"
        val descWidth = textRenderer.getWidth(description)
        context.drawText(textRenderer, description, (width - descWidth) / 2, 35, 0xAAAAAA, false)
        
        // Settings area background
        val settingsAreaY = 55
        val settingsAreaHeight = height - 100
        context.fill(10, settingsAreaY, width - 10, settingsAreaY + settingsAreaHeight, 0x88000000.toInt())
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
                original.name, original.value, original.x, newY
            )
            is FloatSettingWidget -> FloatSettingWidget(
                original.name, original.value, original.x, newY, 
                0.0f, 10.0f // Mock min/max, would get from actual setting
            )
            is IntSettingWidget -> IntSettingWidget(
                original.name, original.value, original.x, newY,
                0, 1000 // Mock min/max, would get from actual setting  
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
            val adjustedY = widget.y - scrollOffset
            val adjustedWidget = createAdjustedWidget(widget, adjustedY)
            
            if (adjustedWidget.isMouseOver(mouseX.toInt(), mouseY.toInt())) {
                if (adjustedWidget.mouseClicked(mouseX, mouseY, button)) {
                    // Update original widget value
                    when {
                        widget is BooleanSettingWidget && adjustedWidget is BooleanSettingWidget -> {
                            widget.value = adjustedWidget.value
                        }
                        widget is FloatSettingWidget && adjustedWidget is FloatSettingWidget -> {
                            widget.value = adjustedWidget.value
                        }
                        widget is IntSettingWidget && adjustedWidget is IntSettingWidget -> {
                            widget.value = adjustedWidget.value
                        }
                    }
                    return true
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
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
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val totalHeight = settingWidgets.size * (settingHeight + settingSpacing)
        val areaHeight = height - 100
        
        if (totalHeight > areaHeight) {
            val maxScroll = totalHeight - areaHeight
            scrollOffset = max(0, min(maxScroll, scrollOffset - (verticalAmount * 30).toInt()))
            return true
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
    
    private fun resetToDefaults() {
        try {
            // Reset all settings to defaults
            // In real implementation, would reset module's configurable values
            initializeSettingWidgets()
        } catch (e: Exception) {
            println("Error resetting module settings: ${e.message}")
        }
    }
    
    override fun shouldPause(): Boolean {
        return false
    }
}