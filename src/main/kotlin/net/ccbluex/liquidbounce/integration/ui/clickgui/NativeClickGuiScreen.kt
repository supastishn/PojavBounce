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
import kotlin.math.max
import kotlin.math.min

/**
 * Native Minecraft GUI implementation of ClickGUI
 * Replaces the browser/svelte-based ClickGUI with pure Minecraft widgets
 * Supports nested configurables with animated accordions
 */
class NativeClickGuiScreen : Screen("ClickGUI".asPlainText()) {

    private val panels = mutableListOf<CategoryPanel>()

    companion object {
        private const val PANEL_WIDTH = 140
        private const val PANEL_HEADER_HEIGHT = 16
        private const val MODULE_HEIGHT = 14
        private const val SETTING_HEIGHT = 12
        private const val CONFIGURABLE_HEADER_HEIGHT = 11
        private const val PANEL_SPACING = 10
        private const val PANEL_MARGIN = 10
        private const val UNBOUND_KEY_NAME = "None"
        private const val INDENT_PER_LEVEL = 6
        private const val ANIMATION_SPEED = 0.15f
    }

    override fun init() {
        super.init()
        panels.clear()

        var xPos = PANEL_MARGIN
        var yPos = PANEL_MARGIN
        val maxPanelsPerRow = max(1, (width - PANEL_MARGIN * 2) / (PANEL_WIDTH + PANEL_SPACING))
        var panelCount = 0

        for (category in Category.entries) {
            val modules = ModuleManager.filter { it.category == category }
            if (modules.isEmpty()) continue

            if (panelCount >= maxPanelsPerRow) {
                xPos = PANEL_MARGIN
                yPos += 350
                panelCount = 0
            }

            panels.add(CategoryPanel(category, modules, xPos, yPos, PANEL_WIDTH))
            xPos += PANEL_WIDTH + PANEL_SPACING
            panelCount++
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, ARGB.color(180, 20, 20, 20))

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
     * Tracks expansion state and animation progress for a configurable
     */
    private class ExpandState {
        var expanded = false
        var animProgress = 0f // 0 = collapsed, 1 = expanded

        fun update(delta: Float) {
            val target = if (expanded) 1f else 0f
            animProgress = if (animProgress < target) {
                min(animProgress + ANIMATION_SPEED * delta * 60f, 1f)
            } else {
                max(animProgress - ANIMATION_SPEED * delta * 60f, 0f)
            }
        }

        val isAnimating: Boolean get() = animProgress != (if (expanded) 1f else 0f)
        val visibleFraction: Float get() = animProgress
    }

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

        // Expansion state per module
        private val moduleExpandStates = mutableMapOf<ClientModule, ExpandState>()
        // Expansion state for nested configurables (by identity)
        private val configurableExpandStates = mutableMapOf<Any, ExpandState>()

        private fun getModuleExpandState(module: ClientModule): ExpandState {
            return moduleExpandStates.getOrPut(module) { ExpandState() }
        }

        private fun getConfigurableExpandState(configurable: Any): ExpandState {
            return configurableExpandStates.getOrPut(configurable) { ExpandState() }
        }

        fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float, textRenderer: Font) {
            // Update all animations
            moduleExpandStates.values.forEach { it.update(delta) }
            configurableExpandStates.values.forEach { it.update(delta) }

            val contentHeight = if (expanded) {
                calculateTotalContentHeight()
            } else 0

            val totalPanelHeight = PANEL_HEADER_HEIGHT + contentHeight
            context.fill(x, y, x + panelWidth, y + totalPanelHeight, 0xB41E1E1E.toInt())

            val headerColor = 0xC8323232.toInt()
            context.fill(x, y, x + panelWidth, y + PANEL_HEADER_HEIGHT, headerColor)

            context.drawString(textRenderer, category.choiceName, x + 4, y + 4, 0xFFFFFFFF.toInt(), true)

            val indicator = if (expanded) "v" else ">"
            context.drawString(textRenderer, indicator, x + panelWidth - 12, y + 4, 0xFFFFFFFF.toInt(), true)

            if (expanded) {
                var currentY = y + PANEL_HEADER_HEIGHT
                for (module in modules) {
                    currentY = renderModuleWithSettings(context, module, currentY, mouseX, mouseY, textRenderer, 0)
                }
            }
        }

        private fun calculateTotalContentHeight(): Int {
            var height = 0
            for (module in modules) {
                height += MODULE_HEIGHT
                val expandState = getModuleExpandState(module)
                if (expandState.visibleFraction > 0f) {
                    val settingsHeight = calculateSettingsHeight(module, 0)
                    height += (settingsHeight * expandState.visibleFraction).toInt()
                }
            }
            return height
        }

        private fun calculateSettingsHeight(configurable: Configurable, depth: Int): Int {
            var height = 0
            val values = getFilteredValues(configurable)
            for (value in values) {
                if (value is Configurable) {
                    height += CONFIGURABLE_HEADER_HEIGHT
                    val subExpandState = getConfigurableExpandState(value)
                    if (subExpandState.visibleFraction > 0f) {
                        val subHeight = calculateSettingsHeight(value, depth + 1)
                        height += (subHeight * subExpandState.visibleFraction).toInt()
                    }
                } else {
                    height += SETTING_HEIGHT
                }
            }
            return height
        }

        private fun getFilteredValues(configurable: Configurable): List<Value<*>> {
            return configurable.containedValues.filter { value ->
                value.name != "Enabled" && value.name != "Bind" && value.name != "Hidden" && !value.notAnOption
            }
        }

        private fun renderModuleWithSettings(
            context: GuiGraphics, module: ClientModule, startY: Int,
            mouseX: Int, mouseY: Int, textRenderer: Font, depth: Int
        ): Int {
            var currentY = startY
            val expandState = getModuleExpandState(module)
            val isExpanded = expandState.expanded || expandState.visibleFraction > 0f

            // Render module row
            renderModuleRow(context, module, currentY, mouseX, mouseY, textRenderer, expandState)
            currentY += MODULE_HEIGHT

            // Render settings if expanded (with animation)
            if (isExpanded) {
                val settingsHeight = calculateSettingsHeight(module, 0)
                val visibleHeight = (settingsHeight * expandState.visibleFraction).toInt()

                if (visibleHeight > 0) {
                    // Enable scissor for animation clipping
                    val clipY = currentY
                    val clipHeight = visibleHeight
                    context.enableScissor(x, clipY, x + panelWidth, clipY + clipHeight)

                    val endY = renderConfigurableSettings(context, module, currentY, mouseX, mouseY, textRenderer, 1)

                    context.disableScissor()
                    currentY += visibleHeight
                }
            }

            return currentY
        }

        private fun renderModuleRow(
            context: GuiGraphics, module: ClientModule, moduleY: Int,
            mouseX: Int, mouseY: Int, textRenderer: Font, expandState: ExpandState
        ) {
            val isHovered = mouseX >= x && mouseX < x + panelWidth &&
                mouseY >= moduleY && mouseY < moduleY + MODULE_HEIGHT
            val isExpanded = expandState.expanded

            val backgroundColor = when {
                module.enabled && isExpanded -> 0xA05050D0.toInt()
                module.enabled -> 0x964646C8.toInt()
                isExpanded -> 0x64404070.toInt()
                isHovered -> 0x64505050.toInt()
                else -> 0x50282828.toInt()
            }
            context.fill(x + 2, moduleY, x + panelWidth - 2, moduleY + MODULE_HEIGHT, backgroundColor)

            val color = if (module.enabled) 0xFF00FF00.toInt() else 0xFFFFFFFF.toInt()
            context.drawString(textRenderer, module.name, x + 6, moduleY + 3, color, true)

            val settings = getFilteredValues(module)
            if (settings.isNotEmpty()) {
                val indicator = if (isExpanded) "-" else "+"
                context.drawString(textRenderer, indicator, x + panelWidth - 12, moduleY + 3, 0xFFAAAAAA.toInt(), true)
            } else {
                val bindText = module.bind.keyName
                if (bindText.isNotEmpty() && bindText != UNBOUND_KEY_NAME) {
                    val displayText = "[$bindText]"
                    val textX = x + panelWidth - textRenderer.width(displayText) - 6
                    context.drawString(textRenderer, displayText, textX, moduleY + 3, 0xFF888888.toInt(), true)
                }
            }
        }

        private fun renderConfigurableSettings(
            context: GuiGraphics, configurable: Configurable, startY: Int,
            mouseX: Int, mouseY: Int, textRenderer: Font, depth: Int
        ): Int {
            var currentY = startY
            val indent = depth * INDENT_PER_LEVEL
            val values = getFilteredValues(configurable)

            for (value in values) {
                when (value) {
                    is ChoiceConfigurable<*> -> {
                        currentY = renderChoiceConfigurable(context, value, currentY, mouseX, mouseY, textRenderer, depth, indent)
                    }
                    is ToggleableConfigurable -> {
                        currentY = renderToggleableConfigurable(context, value, currentY, mouseX, mouseY, textRenderer, depth, indent)
                    }
                    is Configurable -> {
                        currentY = renderNestedConfigurable(context, value, currentY, mouseX, mouseY, textRenderer, depth, indent)
                    }
                    else -> {
                        renderSettingValue(context, value, currentY, mouseX, mouseY, textRenderer, indent)
                        currentY += SETTING_HEIGHT
                    }
                }
            }
            return currentY
        }

        private fun renderChoiceConfigurable(
            context: GuiGraphics, choice: ChoiceConfigurable<*>, startY: Int,
            mouseX: Int, mouseY: Int, textRenderer: Font, depth: Int, indent: Int
        ): Int {
            var currentY = startY
            val expandState = getConfigurableExpandState(choice)
            val isExpanded = expandState.expanded || expandState.visibleFraction > 0f
            val isHovered = mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                mouseY >= currentY && mouseY < currentY + CONFIGURABLE_HEADER_HEIGHT

            // Header background
            val bgColor = when {
                isExpanded -> 0x60506080.toInt()
                isHovered -> 0x50405060.toInt()
                else -> 0x40303050.toInt()
            }
            context.fill(x + 4 + indent, currentY, x + panelWidth - 4, currentY + CONFIGURABLE_HEADER_HEIGHT, bgColor)

            // Choice name and current selection
            val displayName = choice.name
            val activeChoice = choice.activeChoice.name
            context.drawString(textRenderer, displayName, x + 6 + indent, currentY + 2, 0xFFDDAAFF.toInt(), false)

            val choiceText = "[$activeChoice]"
            val choiceX = x + panelWidth - textRenderer.width(choiceText) - 8
            context.drawString(textRenderer, choiceText, choiceX, currentY + 2, 0xFFAADDFF.toInt(), false)

            // Expand indicator
            val indicator = if (expandState.expanded) "v" else ">"
            context.drawString(textRenderer, indicator, x + panelWidth - 8, currentY + 2, 0xFFAAAAAA.toInt(), false)

            currentY += CONFIGURABLE_HEADER_HEIGHT

            // Render active choice settings if expanded
            if (isExpanded && expandState.visibleFraction > 0f) {
                val activeConfigurable = choice.activeChoice as? Configurable
                if (activeConfigurable != null) {
                    currentY = renderConfigurableSettings(context, activeConfigurable, currentY, mouseX, mouseY, textRenderer, depth + 1)
                }
            }

            return currentY
        }

        private fun renderToggleableConfigurable(
            context: GuiGraphics, toggleable: ToggleableConfigurable, startY: Int,
            mouseX: Int, mouseY: Int, textRenderer: Font, depth: Int, indent: Int
        ): Int {
            var currentY = startY
            val expandState = getConfigurableExpandState(toggleable)
            val isExpanded = expandState.expanded || expandState.visibleFraction > 0f
            val isHovered = mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                mouseY >= currentY && mouseY < currentY + CONFIGURABLE_HEADER_HEIGHT

            // Header background
            val bgColor = when {
                toggleable.enabled && isExpanded -> 0x60508060.toInt()
                toggleable.enabled -> 0x50406050.toInt()
                isExpanded -> 0x50505060.toInt()
                isHovered -> 0x40404050.toInt()
                else -> 0x30303040.toInt()
            }
            context.fill(x + 4 + indent, currentY, x + panelWidth - 4, currentY + CONFIGURABLE_HEADER_HEIGHT, bgColor)

            // Toggleable name
            val nameColor = if (toggleable.enabled) 0xFF88FF88.toInt() else 0xFFCCCCCC.toInt()
            context.drawString(textRenderer, toggleable.name, x + 6 + indent, currentY + 2, nameColor, false)

            // ON/OFF indicator
            val statusText = if (toggleable.enabled) "ON" else "OFF"
            val statusColor = if (toggleable.enabled) 0xFF00FF00.toInt() else 0xFFFF6666.toInt()
            context.drawString(textRenderer, statusText, x + panelWidth - textRenderer.width(statusText) - 20, currentY + 2, statusColor, false)

            // Expand indicator
            val indicator = if (expandState.expanded) "v" else ">"
            context.drawString(textRenderer, indicator, x + panelWidth - 8, currentY + 2, 0xFFAAAAAA.toInt(), false)

            currentY += CONFIGURABLE_HEADER_HEIGHT

            // Render nested settings if expanded
            if (isExpanded && expandState.visibleFraction > 0f) {
                currentY = renderConfigurableSettings(context, toggleable, currentY, mouseX, mouseY, textRenderer, depth + 1)
            }

            return currentY
        }

        private fun renderNestedConfigurable(
            context: GuiGraphics, configurable: Configurable, startY: Int,
            mouseX: Int, mouseY: Int, textRenderer: Font, depth: Int, indent: Int
        ): Int {
            var currentY = startY
            val expandState = getConfigurableExpandState(configurable)
            val isExpanded = expandState.expanded || expandState.visibleFraction > 0f
            val isHovered = mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                mouseY >= currentY && mouseY < currentY + CONFIGURABLE_HEADER_HEIGHT

            val bgColor = when {
                isExpanded -> 0x50506080.toInt()
                isHovered -> 0x40405060.toInt()
                else -> 0x30304050.toInt()
            }
            context.fill(x + 4 + indent, currentY, x + panelWidth - 4, currentY + CONFIGURABLE_HEADER_HEIGHT, bgColor)

            context.drawString(textRenderer, configurable.name, x + 6 + indent, currentY + 2, 0xFFBBBBFF.toInt(), false)

            val indicator = if (expandState.expanded) "v" else ">"
            context.drawString(textRenderer, indicator, x + panelWidth - 8, currentY + 2, 0xFFAAAAAA.toInt(), false)

            currentY += CONFIGURABLE_HEADER_HEIGHT

            if (isExpanded && expandState.visibleFraction > 0f) {
                currentY = renderConfigurableSettings(context, configurable, currentY, mouseX, mouseY, textRenderer, depth + 1)
            }

            return currentY
        }

        private fun renderSettingValue(
            context: GuiGraphics, value: Value<*>, settingY: Int,
            mouseX: Int, mouseY: Int, textRenderer: Font, indent: Int
        ) {
            val isHovered = mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                mouseY >= settingY && mouseY < settingY + SETTING_HEIGHT

            val backgroundColor = if (isHovered) 0x50404060.toInt() else 0x40202030.toInt()
            context.fill(x + 4 + indent, settingY, x + panelWidth - 4, settingY + SETTING_HEIGHT, backgroundColor)

            val displayText = getSettingDisplayText(value)
            val nameColor = 0xFFCCCCCC.toInt()

            val maxNameWidth = panelWidth - 50 - indent
            var displayName = value.name
            if (textRenderer.width(displayName) > maxNameWidth) {
                while (textRenderer.width(displayName + "..") > maxNameWidth && displayName.isNotEmpty()) {
                    displayName = displayName.dropLast(1)
                }
                displayName += ".."
            }

            context.drawString(textRenderer, displayName, x + 6 + indent, settingY + 2, nameColor, false)

            val valueColor = getValueColor(value)
            context.drawString(textRenderer, displayText, x + panelWidth - textRenderer.width(displayText) - 8, settingY + 2, valueColor, false)
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
                is ClosedFloatingPointRange<*> -> "${String.format("%.1f", inner.start)}..${String.format("%.1f", inner.endInclusive)}"
                is IntRange -> "${inner.first}..${inner.last}"
                else -> inner.toString().take(8)
            }
        }

        private fun getValueColor(value: Value<*>): Int {
            return when (val inner = value.get()) {
                is Boolean -> if (inner) 0xFF00FF00.toInt() else 0xFFFF6666.toInt()
                is Number -> 0xFF66CCFF.toInt()
                is NamedChoice -> 0xFFFFCC66.toInt()
                is ClosedFloatingPointRange<*>, is IntRange -> 0xFF66CCFF.toInt()
                else -> 0xFFAAAAAA.toInt()
            }
        }

        fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (isMouseInHeader(mouseX, mouseY)) {
                return handleHeaderClick(mouseX, mouseY, button)
            }
            if (expanded) {
                return handleContentClick(mouseX, mouseY, button)
            }
            return false
        }

        private fun isMouseInHeader(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x && mouseX < x + panelWidth && mouseY >= y && mouseY < y + PANEL_HEADER_HEIGHT
        }

        @Suppress("UNUSED_PARAMETER")
        private fun handleHeaderClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
            when (button) {
                0 -> {
                    dragging = true
                    dragOffsetX = mouseX - x
                    dragOffsetY = mouseY - y
                    return true
                }
                1 -> {
                    expanded = !expanded
                    return true
                }
            }
            return false
        }

        private fun handleContentClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
            var currentY = y + PANEL_HEADER_HEIGHT

            for (module in modules) {
                val result = handleModuleAreaClick(module, mouseX, mouseY, button, currentY, 0)
                if (result.first) return true
                currentY = result.second
            }
            return false
        }

        private fun handleModuleAreaClick(
            module: ClientModule, mouseX: Double, mouseY: Double, button: Int, startY: Int, depth: Int
        ): Pair<Boolean, Int> {
            var currentY = startY
            val expandState = getModuleExpandState(module)

            // Check module row click
            if (mouseX >= x && mouseX < x + panelWidth && mouseY >= currentY && mouseY < currentY + MODULE_HEIGHT) {
                when (button) {
                    0 -> module.enabled = !module.enabled
                    1 -> expandState.expanded = !expandState.expanded
                }
                return true to currentY + MODULE_HEIGHT
            }
            currentY += MODULE_HEIGHT

            // Check settings if expanded
            if (expandState.visibleFraction > 0f) {
                val result = handleConfigurableClick(module, mouseX, mouseY, button, currentY, 1)
                if (result.first) return true to result.second
                currentY = result.second
            }

            return false to currentY
        }

        private fun handleConfigurableClick(
            configurable: Configurable, mouseX: Double, mouseY: Double, button: Int, startY: Int, depth: Int
        ): Pair<Boolean, Int> {
            var currentY = startY
            val indent = depth * INDENT_PER_LEVEL
            val values = getFilteredValues(configurable)

            for (value in values) {
                when (value) {
                    is ChoiceConfigurable<*> -> {
                        val expandState = getConfigurableExpandState(value)
                        if (mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                            mouseY >= currentY && mouseY < currentY + CONFIGURABLE_HEADER_HEIGHT) {
                            when (button) {
                                0 -> cycleChoiceConfigurable(value, false)
                                1 -> expandState.expanded = !expandState.expanded
                            }
                            return true to currentY
                        }
                        currentY += CONFIGURABLE_HEADER_HEIGHT

                        if (expandState.visibleFraction > 0f) {
                            val activeConfigurable = value.activeChoice as? Configurable
                            if (activeConfigurable != null) {
                                val result = handleConfigurableClick(activeConfigurable, mouseX, mouseY, button, currentY, depth + 1)
                                if (result.first) return result
                                currentY = result.second
                            }
                        }
                    }
                    is ToggleableConfigurable -> {
                        val expandState = getConfigurableExpandState(value)
                        if (mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                            mouseY >= currentY && mouseY < currentY + CONFIGURABLE_HEADER_HEIGHT) {
                            when (button) {
                                0 -> value.enabled = !value.enabled
                                1 -> expandState.expanded = !expandState.expanded
                            }
                            return true to currentY
                        }
                        currentY += CONFIGURABLE_HEADER_HEIGHT

                        if (expandState.visibleFraction > 0f) {
                            val result = handleConfigurableClick(value, mouseX, mouseY, button, currentY, depth + 1)
                            if (result.first) return result
                            currentY = result.second
                        }
                    }
                    is Configurable -> {
                        val expandState = getConfigurableExpandState(value)
                        if (mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                            mouseY >= currentY && mouseY < currentY + CONFIGURABLE_HEADER_HEIGHT) {
                            if (button == 1) {
                                expandState.expanded = !expandState.expanded
                            }
                            return true to currentY
                        }
                        currentY += CONFIGURABLE_HEADER_HEIGHT

                        if (expandState.visibleFraction > 0f) {
                            val result = handleConfigurableClick(value, mouseX, mouseY, button, currentY, depth + 1)
                            if (result.first) return result
                            currentY = result.second
                        }
                    }
                    else -> {
                        if (mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                            mouseY >= currentY && mouseY < currentY + SETTING_HEIGHT) {
                            handleSettingClick(value, button)
                            return true to currentY
                        }
                        currentY += SETTING_HEIGHT
                    }
                }
            }
            return false to currentY
        }

        private fun cycleChoiceConfigurable(choice: ChoiceConfigurable<*>, reverse: Boolean) {
            val choices = choice.choices.toList()
            val currentIndex = choices.indexOf(choice.activeChoice)
            val newIndex = if (reverse) {
                (currentIndex - 1 + choices.size) % choices.size
            } else {
                (currentIndex + 1) % choices.size
            }
            choice.setByString(choices[newIndex].choiceName)
        }

        private fun handleSettingClick(value: Value<*>, button: Int) {
            when (val inner = value.get()) {
                is Boolean -> {
                    @Suppress("UNCHECKED_CAST")
                    (value as Value<Boolean>).set(!inner)
                }
                is NamedChoice -> {
                    if (value is ChooseListValue<*>) {
                        cycleChoice(value, button == 1)
                    }
                }
            }
            if (value is RangedValue<*>) {
                val step = if (button == 1) -1 else 1
                adjustRangedValue(value, step)
            }
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

        private fun adjustRangedValue(value: RangedValue<*>, direction: Int) {
            when (val current = value.get()) {
                is Int -> {
                    @Suppress("UNCHECKED_CAST")
                    val intValue = value as RangedValue<Int>
                    val rangeStart = intValue.range.start as Int
                    val rangeEnd = intValue.range.endInclusive as Int
                    val newVal = (current + direction).coerceIn(rangeStart, rangeEnd)
                    intValue.set(newVal)
                }
                is Float -> {
                    @Suppress("UNCHECKED_CAST")
                    val floatValue = value as RangedValue<Float>
                    val rangeStart = floatValue.range.start as Float
                    val rangeEnd = floatValue.range.endInclusive as Float
                    val step = (rangeEnd - rangeStart) / 20f
                    val newVal = (current + step * direction).coerceIn(rangeStart, rangeEnd)
                    floatValue.set(newVal)
                }
                is Double -> {
                    @Suppress("UNCHECKED_CAST")
                    val doubleValue = value as RangedValue<Double>
                    val rangeStart = doubleValue.range.start as Double
                    val rangeEnd = doubleValue.range.endInclusive as Double
                    val step = (rangeEnd - rangeStart) / 20.0
                    val newVal = (current + step * direction).coerceIn(rangeStart, rangeEnd)
                    doubleValue.set(newVal)
                }
            }
        }

        fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
            if (!expanded) return false
            // Scroll handling for ranged values
            var currentY = y + PANEL_HEADER_HEIGHT
            for (module in modules) {
                val result = handleModuleScrollArea(module, mouseX, mouseY, amount, currentY)
                if (result.first) return true
                currentY = result.second
            }
            return false
        }

        private fun handleModuleScrollArea(
            module: ClientModule, mouseX: Double, mouseY: Double, amount: Double, startY: Int
        ): Pair<Boolean, Int> {
            var currentY = startY + MODULE_HEIGHT
            val expandState = getModuleExpandState(module)
            if (expandState.visibleFraction > 0f) {
                val result = handleConfigurableScroll(module, mouseX, mouseY, amount, currentY, 1)
                if (result.first) return result
                currentY = result.second
            }
            return false to currentY
        }

        private fun handleConfigurableScroll(
            configurable: Configurable, mouseX: Double, mouseY: Double, amount: Double, startY: Int, depth: Int
        ): Pair<Boolean, Int> {
            var currentY = startY
            val indent = depth * INDENT_PER_LEVEL
            val values = getFilteredValues(configurable)

            for (value in values) {
                when (value) {
                    is Configurable -> {
                        currentY += CONFIGURABLE_HEADER_HEIGHT
                        val expandState = getConfigurableExpandState(value)
                        if (expandState.visibleFraction > 0f) {
                            val result = handleConfigurableScroll(value, mouseX, mouseY, amount, currentY, depth + 1)
                            if (result.first) return result
                            currentY = result.second
                        }
                    }
                    else -> {
                        if (mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                            mouseY >= currentY && mouseY < currentY + SETTING_HEIGHT) {
                            if (value is RangedValue<*>) {
                                adjustRangedValue(value, if (amount > 0) 1 else -1)
                                return true to currentY
                            }
                        }
                        currentY += SETTING_HEIGHT
                    }
                }
            }
            return false to currentY
        }

        @Suppress("UnusedParameter")
        fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
            if (dragging && button == 0) {
                x = (mouseX - dragOffsetX).toInt()
                y = (mouseY - dragOffsetY).toInt()
                return true
            }
            return false
        }
    }
}
