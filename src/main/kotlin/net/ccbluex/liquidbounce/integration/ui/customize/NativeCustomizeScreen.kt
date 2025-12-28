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
package net.ccbluex.liquidbounce.integration.ui.customize

import net.ccbluex.liquidbounce.integration.theme.component.HudComponent
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.util.ARGB

/**
 * Native HUD Customize screen with Inter font support
 * Features:
 * - List of HUD components
 * - Enable/disable components
 * - Adjust component positions (alignment)
 * - Shows component info (name, enabled status)
 */
class NativeCustomizeScreen(private val parent: Screen?) : Screen("HUD Customize".asPlainText()) {

    private val fontRenderer: FontRenderer get() = FontManager.FONT_RENDERER
    private val fontScale = 0.28f

    private var scrollOffset = 0
    private var selectedIndex = -1
    private var statusMessage = ""
    private var statusColor = COLOR_WHITE

    companion object {
        private const val COMPONENT_ROW_HEIGHT = 40
        private const val LIST_TOP = 50
        private const val LIST_PADDING = 20

        private val COLOR_WHITE = Color4b(255, 255, 255, 255)
        private val COLOR_GREEN = Color4b(0, 255, 0, 255)
        private val COLOR_RED = Color4b(255, 102, 102, 255)
        private val COLOR_CYAN = Color4b(0, 255, 255, 255)
        private val COLOR_GRAY = Color4b(170, 170, 170, 255)
        private val COLOR_YELLOW = Color4b(255, 204, 102, 255)
        private val COLOR_PURPLE = Color4b(200, 150, 255, 255)
        private val COLOR_ORANGE = Color4b(255, 165, 0, 255)
    }

    override fun init() {
        super.init()

        val buttonWidth = 100
        val buttonHeight = 20
        val buttonY = height - 30
        val spacing = 10

        // Calculate button positions
        val totalWidth = buttonWidth * 4 + spacing * 3
        var xPos = width / 2 - totalWidth / 2

        // Toggle button
        addRenderableWidget(
            Button.builder("Toggle".asPlainText()) {
                val components = HudComponentManager.components
                if (selectedIndex >= 0 && selectedIndex < components.size) {
                    val component = components[selectedIndex]
                    component.enabled = !component.enabled
                    val status = if (component.enabled) "enabled" else "disabled"
                    statusMessage = "${component.name} $status"
                    statusColor = if (component.enabled) COLOR_GREEN else COLOR_YELLOW
                    HudComponentManager.updateComponents()
                } else {
                    statusMessage = "Select a component first!"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Move Up button (alignment)
        addRenderableWidget(
            Button.builder("Move Up".asPlainText()) {
                val components = HudComponentManager.components
                if (selectedIndex >= 0 && selectedIndex < components.size) {
                    val component = components[selectedIndex]
                    val current = component.alignment.verticalAlignment
                    val newValue = (current - 0.1f).coerceIn(0f, 1f)
                    component.alignment.verticalAlignment = newValue
                    statusMessage = "Moved ${component.name} up (${String.format("%.1f", newValue)})"
                    statusColor = COLOR_CYAN
                    HudComponentManager.updateComponents()
                } else {
                    statusMessage = "Select a component first!"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Move Down button (alignment)
        addRenderableWidget(
            Button.builder("Move Down".asPlainText()) {
                val components = HudComponentManager.components
                if (selectedIndex >= 0 && selectedIndex < components.size) {
                    val component = components[selectedIndex]
                    val current = component.alignment.verticalAlignment
                    val newValue = (current + 0.1f).coerceIn(0f, 1f)
                    component.alignment.verticalAlignment = newValue
                    statusMessage = "Moved ${component.name} down (${String.format("%.1f", newValue)})"
                    statusColor = COLOR_CYAN
                    HudComponentManager.updateComponents()
                } else {
                    statusMessage = "Select a component first!"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Close button
        addRenderableWidget(
            Button.builder("Close".asPlainText()) {
                onClose()
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // Background
        context.fill(0, 0, width, height, ARGB.color(200, 35, 25, 45))

        // Title
        drawText(context, "HUD Customize", width / 2f - getTextWidth("HUD Customize") / 2, 10f, COLOR_CYAN)

        // Component count
        val countText = "Components: ${HudComponentManager.components.size}"
        drawText(context, countText, width / 2f - getTextWidth(countText) / 2, 25f, COLOR_GRAY)

        // Instructions
        val instructionsText = "Left/Right click to adjust horizontal alignment"
        drawText(context, instructionsText, width / 2f - getTextWidth(instructionsText) / 2, 37f, COLOR_GRAY)

        // Component list
        renderComponentList(context, mouseX, mouseY)

        // Status message
        if (statusMessage.isNotEmpty()) {
            drawText(context, statusMessage, width / 2f - getTextWidth(statusMessage) / 2, height - 55f, statusColor)
        }

        super.render(context, mouseX, mouseY, delta)
    }

    private fun renderComponentList(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val listX = LIST_PADDING
        val listWidth = width - LIST_PADDING * 2
        val listHeight = height - LIST_TOP - 70
        val listBottom = LIST_TOP + listHeight

        // List background
        context.fill(listX, LIST_TOP, listX + listWidth, listBottom, ARGB.color(150, 45, 35, 60))

        // Border
        context.fill(listX, LIST_TOP, listX + listWidth, LIST_TOP + 1, ARGB.color(255, 90, 70, 110))
        context.fill(listX, listBottom - 1, listX + listWidth, listBottom, ARGB.color(255, 90, 70, 110))
        context.fill(listX, LIST_TOP, listX + 1, listBottom, ARGB.color(255, 90, 70, 110))
        context.fill(listX + listWidth - 1, LIST_TOP, listX + listWidth, listBottom, ARGB.color(255, 90, 70, 110))

        // Enable scissor for clipping
        context.enableScissor(listX + 1, LIST_TOP + 1, listX + listWidth - 1, listBottom - 1)

        val components = HudComponentManager.components
        var yPos = LIST_TOP + 2 - scrollOffset

        for ((index, component) in components.withIndex()) {
            if (yPos + COMPONENT_ROW_HEIGHT > LIST_TOP && yPos < listBottom) {
                renderComponentRow(context, component, index, listX + 2, yPos, listWidth - 4, mouseX, mouseY)
            }
            yPos += COMPONENT_ROW_HEIGHT
        }

        if (components.isEmpty()) {
            val noComponents = "No HUD components available"
            drawText(context, noComponents, listX + listWidth / 2f - getTextWidth(noComponents) / 2, LIST_TOP + listHeight / 2f - 5, COLOR_GRAY)
        }

        context.disableScissor()

        // Scroll bar
        if (components.size * COMPONENT_ROW_HEIGHT > listHeight) {
            val maxScroll = components.size * COMPONENT_ROW_HEIGHT - listHeight
            val scrollBarHeight = (listHeight.toFloat() / (components.size * COMPONENT_ROW_HEIGHT) * listHeight).toInt().coerceAtLeast(20)
            val scrollBarY = LIST_TOP + (scrollOffset.toFloat() / maxScroll * (listHeight - scrollBarHeight)).toInt()
            context.fill(listX + listWidth - 5, scrollBarY, listX + listWidth - 2, scrollBarY + scrollBarHeight, ARGB.color(180, 140, 100, 160))
        }
    }

    private fun renderComponentRow(context: GuiGraphics, component: HudComponent, index: Int, x: Int, y: Int, w: Int, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + COMPONENT_ROW_HEIGHT
        val isSelected = index == selectedIndex
        val isEnabled = component.enabled

        // Row background
        val bgColor = when {
            isEnabled && isSelected -> ARGB.color(200, 70, 110, 70)
            isEnabled -> ARGB.color(150, 50, 90, 50)
            isSelected -> ARGB.color(200, 100, 70, 120)
            isHovered -> ARGB.color(150, 80, 60, 100)
            else -> ARGB.color(100, 55, 45, 75)
        }
        context.fill(x, y, x + w, y + COMPONENT_ROW_HEIGHT - 2, bgColor)

        // Component name
        val nameColor = when {
            isEnabled -> COLOR_GREEN
            isSelected -> COLOR_WHITE
            isHovered -> COLOR_WHITE
            else -> COLOR_GRAY
        }
        drawText(context, component.name, x + 8f, y + 3f, nameColor)

        // Native/Theme badge
        val isNative = HudComponentManager.nativeComponents.contains(component)
        val typeText = if (isNative) "[Native]" else "[Theme]"
        val typeColor = if (isNative) COLOR_ORANGE else COLOR_PURPLE
        drawText(context, typeText, x + w - getTextWidth(typeText) - 8, y + 3f, typeColor)

        // Alignment info
        val hAlign = component.alignment.horizontalAlignment
        val vAlign = component.alignment.verticalAlignment
        val alignmentText = "Position: H=${String.format("%.2f", hAlign)}, V=${String.format("%.2f", vAlign)}"
        drawText(context, alignmentText, x + 8f, y + 14f, COLOR_GRAY)

        // Status
        val statusText = if (isEnabled) "[ENABLED]" else "[DISABLED]"
        val statusColorVal = if (isEnabled) COLOR_GREEN else COLOR_RED
        drawText(context, statusText, x + w - getTextWidth(statusText) - 8, y + 14f, statusColorVal)

        // Tweaks info
        if (component.tweaks.isNotEmpty()) {
            val tweaksText = "Tweaks: ${component.tweaks.joinToString(", ") { it.name }}"
            val displayText = if (getTextWidth(tweaksText) > w - 16) {
                "Tweaks: ${component.tweaks.size} active"
            } else {
                tweaksText
            }
            drawText(context, displayText, x + 8f, y + 25f, COLOR_GRAY)
        }
    }

    private fun drawText(context: GuiGraphics, text: String, x: Float, y: Float, color: Color4b, shadow: Boolean = true) {
        val processedText = fontRenderer.process(text.asText(), color)
        context.pose().pushMatrix()
        context.pose().translate(x, y)
        context.pose().scale(fontScale, fontScale)
        with(context) {
            fontRenderer.draw(processedText, 0f, 0f, shadow = shadow)
        }
        context.pose().popMatrix()
    }

    private fun getTextWidth(text: String): Float {
        val processedText = fontRenderer.process(text.asText(), COLOR_WHITE)
        return fontRenderer.getStringWidth(processedText, shadow = true) * fontScale
    }

    override fun mouseClicked(click: net.minecraft.client.input.MouseButtonEvent, doubled: Boolean): Boolean {
        val mouseX = click.x.toInt()
        val mouseY = click.y.toInt()
        val button = click.button
        val listX = LIST_PADDING
        val listWidth = width - LIST_PADDING * 2
        val listHeight = height - LIST_TOP - 70
        val listBottom = LIST_TOP + listHeight

        // Check if clicked in component list
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= LIST_TOP && mouseY < listBottom) {
            val components = HudComponentManager.components
            var yPos = LIST_TOP + 2 - scrollOffset

            for ((index, component) in components.withIndex()) {
                if (mouseY >= yPos && mouseY < yPos + COMPONENT_ROW_HEIGHT) {
                    selectedIndex = index

                    // Left click = move alignment left, Right click = move alignment right
                    if (button == 0) {
                        // Left click - decrease horizontal alignment
                        val current = component.alignment.horizontalAlignment
                        val newValue = (current - 0.1f).coerceIn(0f, 1f)
                        component.alignment.horizontalAlignment = newValue
                        statusMessage = "Moved ${component.name} left (${String.format("%.1f", newValue)})"
                        statusColor = COLOR_CYAN
                        HudComponentManager.updateComponents()
                    } else if (button == 1) {
                        // Right click - increase horizontal alignment
                        val current = component.alignment.horizontalAlignment
                        val newValue = (current + 0.1f).coerceIn(0f, 1f)
                        component.alignment.horizontalAlignment = newValue
                        statusMessage = "Moved ${component.name} right (${String.format("%.1f", newValue)})"
                        statusColor = COLOR_CYAN
                        HudComponentManager.updateComponents()
                    }

                    // Double click to toggle
                    if (doubled) {
                        component.enabled = !component.enabled
                        val status = if (component.enabled) "enabled" else "disabled"
                        statusMessage = "${component.name} $status"
                        statusColor = if (component.enabled) COLOR_GREEN else COLOR_YELLOW
                        HudComponentManager.updateComponents()
                    }
                    return true
                }
                yPos += COMPONENT_ROW_HEIGHT
            }
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val listHeight = height - LIST_TOP - 70
        val totalHeight = HudComponentManager.components.size * COMPONENT_ROW_HEIGHT

        if (totalHeight > listHeight) {
            val maxScroll = totalHeight - listHeight
            scrollOffset = (scrollOffset - (vertical * 20).toInt()).coerceIn(0, maxScroll)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun onClose() {
        mc.setScreen(parent)
    }

    override fun isPauseScreen() = true
}
