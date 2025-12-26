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
package net.ccbluex.liquidbounce.integration.ui.clickgui

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.util.math.ColorHelper

/**
 * Native Minecraft GUI implementation of ClickGUI
 * Replaces the browser/svelte-based ClickGUI with pure Minecraft widgets
 */
class NativeClickGuiScreen : Screen("ClickGUI".asPlainText()) {

    private val panels = mutableListOf<CategoryPanel>()
    private var selectedModule: ClientModule? = null

    companion object {
        private const val PANEL_WIDTH = 120
        private const val PANEL_HEADER_HEIGHT = 16
        private const val MODULE_HEIGHT = 14
        private const val PANEL_SPACING = 10
        private const val PANEL_MARGIN = 10
    }

    override fun init() {
        super.init()
        panels.clear()
        
        // Create panels for each category
        var xPos = PANEL_MARGIN
        var yPos = PANEL_MARGIN
        val maxPanelsPerRow = (width - PANEL_MARGIN * 2) / (PANEL_WIDTH + PANEL_SPACING)
        var panelCount = 0

        for (category in Category.entries) {
            val modules = ModuleManager.filter { it.category == category }
            if (modules.isEmpty()) continue

            // Wrap to next row if needed
            if (panelCount >= maxPanelsPerRow) {
                xPos = PANEL_MARGIN
                yPos += 300 // Approximate panel height
                panelCount = 0
            }

            panels.add(CategoryPanel(category, modules, xPos, yPos, PANEL_WIDTH))
            xPos += PANEL_WIDTH + PANEL_SPACING
            panelCount++
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // Render semi-transparent background
        renderBackground(context, mouseX, mouseY, delta)

        // Render all panels
        for (panel in panels) {
            panel.render(context, mouseX, mouseY, delta, textRenderer)
        }

        super.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for (panel in panels) {
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        for (panel in panels) {
            if (panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Stop dragging all panels
        for (panel in panels) {
            panel.dragging = false
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun shouldPause() = false

    override fun shouldCloseOnEsc() = true

    /**
     * Represents a category panel in the ClickGUI
     */
    private inner class CategoryPanel(
        private val category: Category,
        private val modules: List<ClientModule>,
        private var x: Int,
        private var y: Int,
        private val width: Int
    ) {
        var expanded = true
        var dragging = false
        private var dragOffsetX = 0.0
        private var dragOffsetY = 0.0

        @Suppress("UnusedParameter")
        fun render(
            context: DrawContext,
            mouseX: Int,
            mouseY: Int,
            delta: Float,
            textRenderer: net.minecraft.client.font.TextRenderer
        ) {
            val height = if (expanded) {
                PANEL_HEADER_HEIGHT + (modules.size * MODULE_HEIGHT)
            } else {
                PANEL_HEADER_HEIGHT
            }

            // Panel background
            context.fill(x, y, x + width, y + height, ColorHelper.getArgb(180, 30, 30, 30))

            // Panel header
            val headerColor = ColorHelper.getArgb(200, 50, 50, 50)
            context.fill(x, y, x + width, y + PANEL_HEADER_HEIGHT, headerColor)

            // Category name
            val categoryName = category.choiceName
            context.drawText(
                textRenderer,
                categoryName,
                x + 4,
                y + 4,
                0xFFFFFF,
                true
            )

            // Expand/collapse indicator
            val indicator = if (expanded) "▼" else "▶"
            context.drawText(
                textRenderer,
                indicator,
                x + width - 12,
                y + 4,
                0xFFFFFF,
                false
            )

            // Render modules if expanded
            if (expanded) {
                var moduleY = y + PANEL_HEADER_HEIGHT
                for (module in modules) {
                    renderModule(context, module, moduleY, mouseX, mouseY, textRenderer)
                    moduleY += MODULE_HEIGHT
                }
            }
        }

        private fun renderModule(
            context: DrawContext,
            module: ClientModule,
            moduleY: Int,
            mouseX: Int,
            mouseY: Int,
            textRenderer: net.minecraft.client.font.TextRenderer
        ) {
            // Module background (highlight if enabled or hovered)
            val isHovered = mouseX >= x && mouseX < x + width && mouseY >= moduleY && mouseY < moduleY + MODULE_HEIGHT
            val backgroundColor = when {
                module.enabled -> ColorHelper.getArgb(150, 70, 70, 200)
                isHovered -> ColorHelper.getArgb(100, 80, 80, 80)
                else -> ColorHelper.getArgb(80, 40, 40, 40)
            }
            context.fill(x + 2, moduleY, x + width - 2, moduleY + MODULE_HEIGHT, backgroundColor)

            // Module name
            val color = if (module.enabled) 0x00FF00 else 0xFFFFFF
            context.drawText(
                textRenderer,
                module.name,
                x + 6,
                moduleY + 3,
                color,
                false
            )

            // Bind indicator
            val bindText = module.bind.keyName
            if (bindText.isNotEmpty() && bindText != "key.keyboard.unknown") {
                val displayText = "[$bindText]"
                context.drawText(
                    textRenderer,
                    displayText,
                    x + width - textRenderer.getWidth(displayText) - 6,
                    moduleY + 3,
                    0x888888,
                    false
                )
            }
        }

        fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            // Check if clicking on header
            if (isMouseInHeader(mouseX, mouseY)) {
                return handleHeaderClick(mouseX, mouseY, button)
            }

            // Check if clicking on a module
            if (expanded) {
                return handleModuleClick(mouseX, mouseY, button)
            }

            return false
        }

        private fun isMouseInHeader(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x && mouseX < x + width && 
                mouseY >= y && mouseY < y + PANEL_HEADER_HEIGHT
        }

        private fun handleHeaderClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
            when (button) {
                0 -> {
                    // Left click - start dragging
                    dragging = true
                    dragOffsetX = mouseX - x
                    dragOffsetY = mouseY - y
                    return true
                }
                1 -> {
                    // Right click - toggle expansion
                    expanded = !expanded
                    return true
                }
            }
            return false
        }

        private fun handleModuleClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
            var moduleY = y + PANEL_HEADER_HEIGHT
            for (module in modules) {
                val isModuleHit = mouseX >= x && mouseX < x + width && 
                    mouseY >= moduleY && mouseY < moduleY + MODULE_HEIGHT
                if (isModuleHit) {
                    when (button) {
                        0 -> {
                            // Left click - toggle module
                            module.enabled = !module.enabled
                            return true
                        }
                        1 -> {
                            // Right click - expand module settings (future implementation)
                            selectedModule = module
                            return true
                        }
                    }
                }
                moduleY += MODULE_HEIGHT
            }
            return false
        }

        @Suppress("UnusedParameter")
        fun mouseDragged(
            mouseX: Double,
            mouseY: Double,
            button: Int,
            deltaX: Double,
            deltaY: Double
        ): Boolean {
            if (dragging && button == 0) {
                x = (mouseX - dragOffsetX).toInt()
                y = (mouseY - dragOffsetY).toInt()
                return true
            }
            return false
        }
    }
}
