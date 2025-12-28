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

import net.ccbluex.liquidbounce.config.types.ChooseListValue
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.util.ARGB

/**
 * Native Minecraft GUI implementation of ClickGUI
 * Replaces the browser/svelte-based ClickGUI with pure Minecraft widgets
 */
class NativeClickGuiScreen : Screen("ClickGUI".asPlainText()) {

    private val panels = mutableListOf<CategoryPanel>()

    companion object {
        private const val PANEL_WIDTH = 120
        private const val PANEL_HEADER_HEIGHT = 16
        private const val MODULE_HEIGHT = 14
        private const val SETTING_HEIGHT = 12
        private const val PANEL_SPACING = 10
        private const val PANEL_MARGIN = 10
        private const val UNBOUND_KEY_NAME = "None"
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
        // Use solid semi-transparent background instead of blur to avoid "can only blur once per frame" error
        context.fill(0, 0, width, height, ARGB.color(180, 20, 20, 20))

        // Render all panels
        for (panel in panels) {
            panel.render(context, mouseX, mouseY, delta, mc.font)
        }

        super.render(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        for (panel in panels) {
            if (panel.mouseClicked(click.x, click.y, click.button())) {
                return true
            }
        }
        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        for (panel in panels) {
            if (panel.mouseDragged(click.x, click.y, click.button(), offsetX, offsetY)) {
                return true
            }
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        // Stop dragging all panels
        for (panel in panels) {
            panel.dragging = false
        }
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        for (panel in panels) {
            if (panel.mouseScrolled(mouseX, mouseY, vertical)) {
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun isPauseScreen() = false

    override fun shouldCloseOnEsc() = true

    /**
     * Represents a category panel in the ClickGUI
     */
    private inner class CategoryPanel(
        private val category: Category,
        private val modules: List<ClientModule>,
        private var x: Int,
        private var y: Int,
        private val panelWidth: Int
    ) {
        var expanded = true
        var dragging = false
        private var dragOffsetX = 0.0
        private var dragOffsetY = 0.0

        // Track which module has its settings expanded (null = none)
        private var expandedModule: ClientModule? = null

        @Suppress("UnusedParameter")
        fun render(
            context: GuiGraphics,
            mouseX: Int,
            mouseY: Int,
            delta: Float,
            textRenderer: Font
        ) {
            // Calculate total height including expanded settings
            val contentHeight = if (expanded) {
                var totalHeight = 0
                for (module in modules) {
                    totalHeight += MODULE_HEIGHT
                    if (module == expandedModule) {
                        totalHeight += getModuleSettingsHeight(module)
                    }
                }
                totalHeight
            } else {
                0
            }
            val totalPanelHeight = PANEL_HEADER_HEIGHT + contentHeight

            // Panel background - argb(180, 30, 30, 30)
            context.fill(x, y, x + panelWidth, y + totalPanelHeight, 0xB41E1E1E.toInt())

            // Panel header - argb(200, 50, 50, 50)
            val headerColor = 0xC8323232.toInt()
            context.fill(x, y, x + panelWidth, y + PANEL_HEADER_HEIGHT, headerColor)

            // Category name
            val categoryName = category.choiceName
            context.drawString(
                textRenderer,
                categoryName,
                x + 4,
                y + 4,
                0xFFFFFFFF.toInt(),
                true
            )

            // Expand/collapse indicator
            val indicator = if (expanded) "v" else ">"
            context.drawString(
                textRenderer,
                indicator,
                x + panelWidth - 12,
                y + 4,
                0xFFFFFFFF.toInt(),
                true
            )

            // Render modules if expanded
            if (expanded) {
                var moduleY = y + PANEL_HEADER_HEIGHT
                for (module in modules) {
                    renderModule(context, module, moduleY, mouseX, mouseY, textRenderer)
                    moduleY += MODULE_HEIGHT

                    // Render settings if this module is expanded
                    if (module == expandedModule) {
                        moduleY = renderModuleSettings(context, module, moduleY, mouseX, mouseY, textRenderer)
                    }
                }
            }
        }

        private fun getModuleSettingsHeight(module: ClientModule): Int {
            val settings = getFilteredSettings(module)
            return settings.size * SETTING_HEIGHT
        }

        private fun getFilteredSettings(module: ClientModule): List<Value<*>> {
            return module.containedValues.filter { value ->
                // Skip certain internal values
                value.name != "Enabled" &&
                value.name != "Bind" &&
                value.name != "Hidden" &&
                !value.notAnOption
            }
        }

        private fun renderModule(
            context: GuiGraphics,
            module: ClientModule,
            moduleY: Int,
            mouseX: Int,
            mouseY: Int,
            textRenderer: Font
        ) {
            // Module background (highlight if enabled or hovered)
            val isHovered = mouseX >= x && mouseX < x + panelWidth &&
                mouseY >= moduleY && mouseY < moduleY + MODULE_HEIGHT
            val isExpanded = module == expandedModule
            val backgroundColor = when {
                module.enabled && isExpanded -> 0xA05050D0.toInt() // enabled + expanded
                module.enabled -> 0x964646C8.toInt() // argb(150, 70, 70, 200)
                isExpanded -> 0x64404070.toInt() // expanded but not enabled
                isHovered -> 0x64505050.toInt() // argb(100, 80, 80, 80)
                else -> 0x50282828.toInt() // argb(80, 40, 40, 40)
            }
            context.fill(x + 2, moduleY, x + panelWidth - 2, moduleY + MODULE_HEIGHT, backgroundColor)

            // Module name
            val color = if (module.enabled) 0xFF00FF00.toInt() else 0xFFFFFFFF.toInt()
            context.drawString(
                textRenderer,
                module.name,
                x + 6,
                moduleY + 3,
                color,
                true
            )

            // Settings indicator (show "+" or "-" if module has settings)
            val settings = getFilteredSettings(module)
            if (settings.isNotEmpty()) {
                val settingsIndicator = if (isExpanded) "-" else "+"
                context.drawString(
                    textRenderer,
                    settingsIndicator,
                    x + panelWidth - 12,
                    moduleY + 3,
                    0xFFAAAAAA.toInt(),
                    true
                )
            }

            // Bind indicator (only show if there's room and it's bound)
            val bindText = module.bind.keyName
            if (bindText.isNotEmpty() && bindText != UNBOUND_KEY_NAME && settings.isEmpty()) {
                val displayText = "[$bindText]"
                context.drawString(
                    textRenderer,
                    displayText,
                    x + panelWidth - textRenderer.width(displayText) - 6,
                    moduleY + 3,
                    0xFF888888.toInt(),
                    true
                )
            }
        }

        private fun renderModuleSettings(
            context: GuiGraphics,
            module: ClientModule,
            startY: Int,
            mouseX: Int,
            mouseY: Int,
            textRenderer: Font
        ): Int {
            var settingY = startY
            val settings = getFilteredSettings(module)

            for (value in settings) {
                renderSetting(context, value, settingY, mouseX, mouseY, textRenderer)
                settingY += SETTING_HEIGHT
            }

            return settingY
        }

        private fun renderSetting(
            context: GuiGraphics,
            value: Value<*>,
            settingY: Int,
            mouseX: Int,
            mouseY: Int,
            textRenderer: Font
        ) {
            val isHovered = mouseX >= x && mouseX < x + panelWidth &&
                mouseY >= settingY && mouseY < settingY + SETTING_HEIGHT

            // Settings background (slightly indented and darker)
            val backgroundColor = if (isHovered) 0x50404060.toInt() else 0x40202030.toInt()
            context.fill(x + 4, settingY, x + panelWidth - 4, settingY + SETTING_HEIGHT, backgroundColor)

            // Render based on value type
            val displayText = getSettingDisplayText(value)
            val nameColor = 0xFFCCCCCC.toInt()

            // Truncate name if too long
            val maxNameWidth = panelWidth - 50
            var displayName = value.name
            if (textRenderer.width(displayName) > maxNameWidth) {
                while (textRenderer.width(displayName + "..") > maxNameWidth && displayName.isNotEmpty()) {
                    displayName = displayName.dropLast(1)
                }
                displayName += ".."
            }

            context.drawString(
                textRenderer,
                displayName,
                x + 8,
                settingY + 2,
                nameColor,
                false
            )

            // Value on the right side
            val valueColor = getValueColor(value)
            context.drawString(
                textRenderer,
                displayText,
                x + panelWidth - textRenderer.width(displayText) - 8,
                settingY + 2,
                valueColor,
                false
            )
        }

        private fun getSettingDisplayText(value: Value<*>): String {
            return when (val inner = value.get()) {
                is Boolean -> if (inner) "ON" else "OFF"
                is Number -> {
                    if (value is RangedValue<*>) {
                        when (inner) {
                            is Float -> String.format("%.1f", inner)
                            is Double -> String.format("%.2f", inner)
                            else -> inner.toString()
                        }
                    } else {
                        inner.toString()
                    }
                }
                is NamedChoice -> inner.choiceName
                is Enum<*> -> inner.name
                else -> inner.toString().take(8)
            }
        }

        private fun getValueColor(value: Value<*>): Int {
            return when (val inner = value.get()) {
                is Boolean -> if (inner) 0xFF00FF00.toInt() else 0xFFFF6666.toInt()
                is Number -> 0xFF66CCFF.toInt()
                is NamedChoice -> 0xFFFFCC66.toInt()
                else -> 0xFFAAAAAA.toInt()
            }
        }

        fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            // Check if clicking on header
            if (isMouseInHeader(mouseX, mouseY)) {
                return handleHeaderClick(mouseX, mouseY, button)
            }

            // Check if clicking on a module or its settings
            if (expanded) {
                return handleContentClick(mouseX, mouseY, button)
            }

            return false
        }

        private fun isMouseInHeader(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x && mouseX < x + panelWidth &&
                mouseY >= y && mouseY < y + PANEL_HEADER_HEIGHT
        }

        @Suppress("UNUSED_PARAMETER")
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

        private fun handleContentClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
            var currentY = y + PANEL_HEADER_HEIGHT

            for (module in modules) {
                // Check module row
                val moduleEndY = currentY + MODULE_HEIGHT
                if (mouseX >= x && mouseX < x + panelWidth &&
                    mouseY >= currentY && mouseY < moduleEndY) {
                    return handleModuleClick(module, button)
                }
                currentY = moduleEndY

                // Check settings if this module is expanded
                if (module == expandedModule) {
                    val settings = getFilteredSettings(module)
                    for (value in settings) {
                        val settingEndY = currentY + SETTING_HEIGHT
                        if (mouseX >= x && mouseX < x + panelWidth &&
                            mouseY >= currentY && mouseY < settingEndY) {
                            return handleSettingClick(value, button)
                        }
                        currentY = settingEndY
                    }
                }
            }
            return false
        }

        private fun handleModuleClick(module: ClientModule, button: Int): Boolean {
            when (button) {
                0 -> {
                    // Left click - toggle module
                    module.enabled = !module.enabled
                    return true
                }
                1 -> {
                    // Right click - toggle settings expansion
                    expandedModule = if (expandedModule == module) null else module
                    return true
                }
            }
            return false
        }

        private fun handleSettingClick(value: Value<*>, button: Int): Boolean {
            when (val inner = value.get()) {
                is Boolean -> {
                    // Toggle boolean
                    @Suppress("UNCHECKED_CAST")
                    (value as Value<Boolean>).set(!inner)
                    return true
                }
                is NamedChoice -> {
                    // Cycle through choices
                    if (value is ChooseListValue<*>) {
                        cycleChoice(value, button == 1) // right click = reverse
                        return true
                    }
                }
            }
            // For other types, left/right click could increment/decrement
            if (value is RangedValue<*>) {
                handleRangedValueClick(value, button)
                return true
            }
            return false
        }

        private fun <T : NamedChoice> cycleChoice(value: ChooseListValue<T>, reverse: Boolean) {
            val choices = value.choices.toList()
            val currentIndex = choices.indexOf(value.get())
            val newIndex = if (reverse) {
                (currentIndex - 1 + choices.size) % choices.size
            } else {
                (currentIndex + 1) % choices.size
            }
            value.set(choices[newIndex])
        }

        private fun handleRangedValueClick(value: RangedValue<*>, button: Int) {
            // Increment/decrement based on button
            val step = if (button == 1) -1 else 1
            adjustRangedValue(value, step)
        }

        private fun adjustRangedValue(value: RangedValue<*>, direction: Int) {
            when (val current = value.get()) {
                is Int -> {
                    @Suppress("UNCHECKED_CAST")
                    val intValue = value as RangedValue<Int>
                    val newVal = (current + direction).coerceIn(intValue.range)
                    intValue.set(newVal)
                }
                is Float -> {
                    @Suppress("UNCHECKED_CAST")
                    val floatValue = value as RangedValue<Float>
                    val step = (floatValue.range.endInclusive - floatValue.range.start) / 20f
                    val newVal = (current + step * direction).coerceIn(floatValue.range)
                    floatValue.set(newVal)
                }
                is Double -> {
                    @Suppress("UNCHECKED_CAST")
                    val doubleValue = value as RangedValue<Double>
                    val step = (doubleValue.range.endInclusive - doubleValue.range.start) / 20.0
                    val newVal = (current + step * direction).coerceIn(doubleValue.range)
                    doubleValue.set(newVal)
                }
            }
        }

        fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
            // Check if scrolling over a setting
            if (!expanded) return false

            var currentY = y + PANEL_HEADER_HEIGHT
            for (module in modules) {
                currentY += MODULE_HEIGHT

                if (module == expandedModule) {
                    val settings = getFilteredSettings(module)
                    for (value in settings) {
                        val settingEndY = currentY + SETTING_HEIGHT
                        if (mouseX >= x && mouseX < x + panelWidth &&
                            mouseY >= currentY && mouseY < settingEndY) {
                            // Scroll on ranged value
                            if (value is RangedValue<*>) {
                                adjustRangedValue(value, if (amount > 0) 1 else -1)
                                return true
                            }
                        }
                        currentY = settingEndY
                    }
                }
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
