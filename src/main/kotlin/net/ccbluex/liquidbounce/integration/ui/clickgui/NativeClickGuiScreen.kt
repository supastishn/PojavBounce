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
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.util.ARGB
import kotlin.math.max
import kotlin.math.min

/**
 * Native Minecraft GUI implementation of ClickGUI
 * Features:
 * - Category panels with module lists
 * - Nested configurable support (accordions)
 * - Search bar for filtering modules
 * - Panel scrolling
 * - Persistent panel positions
 * - Slider controls for ranged values
 * - Inter font support
 */
class NativeClickGuiScreen : Screen("ClickGUI".asPlainText()) {

    private val panels = mutableListOf<CategoryPanel>()
    private var searchQuery = ""

    // Active slider drag state
    private var activeSliderDrag: SliderDragState? = null

    // Font rendering
    private val fontRenderer: FontRenderer get() = FontManager.FONT_RENDERER
    private val fontScale = 0.22f  // Scale for Inter font (43px base -> ~9px display)

    companion object {
        private const val PANEL_WIDTH = 140
        private const val PANEL_HEADER_HEIGHT = 16
        private const val MODULE_HEIGHT = 14
        private const val SETTING_HEIGHT = 12
        private const val SLIDER_HEIGHT = 18  // Taller for slider track
        private const val CONFIGURABLE_HEADER_HEIGHT = 11
        private const val PANEL_SPACING = 10
        private const val PANEL_MARGIN = 10
        private const val SEARCH_BAR_HEIGHT = 20
        private const val UNBOUND_KEY_NAME = "None"
        private const val INDENT_PER_LEVEL = 6
        private const val MAX_PANEL_HEIGHT = 300
        private const val SLIDER_TRACK_HEIGHT = 4
        private const val SLIDER_THUMB_WIDTH = 6

        // Persistent panel positions (survives screen close/reopen)
        private val savedPanelPositions = mutableMapOf<Category, Pair<Int, Int>>()
        private val savedPanelExpanded = mutableMapOf<Category, Boolean>()
        private val savedPanelScrollOffsets = mutableMapOf<Category, Int>()

        // Colors
        private val COLOR_WHITE = Color4b(255, 255, 255, 255)
        private val COLOR_GREEN = Color4b(0, 255, 0, 255)
        private val COLOR_RED = Color4b(255, 102, 102, 255)
        private val COLOR_GRAY = Color4b(170, 170, 170, 255)
        private val COLOR_DARK_GRAY = Color4b(136, 136, 136, 255)
        private val COLOR_LIGHT_GRAY = Color4b(204, 204, 204, 255)
        private val COLOR_CYAN = Color4b(102, 204, 255, 255)
        private val COLOR_PURPLE = Color4b(221, 170, 255, 255)
        private val COLOR_LIGHT_CYAN = Color4b(170, 221, 255, 255)
        private val COLOR_LIGHT_PURPLE = Color4b(187, 187, 255, 255)
        private val COLOR_LIGHT_GREEN = Color4b(136, 255, 136, 255)
        private val COLOR_YELLOW = Color4b(255, 204, 102, 255)
    }

    // Represents active slider being dragged
    private data class SliderDragState(
        val value: Value<*>,
        val thumbIndex: Int,  // 0 for single slider or left thumb, 1 for right thumb
        val sliderX: Int,
        val sliderWidth: Int
    )

    override fun init() {
        super.init()
        panels.clear()

        var xPos = PANEL_MARGIN
        var yPos = PANEL_MARGIN + SEARCH_BAR_HEIGHT + 5
        val maxPanelsPerRow = max(1, (width - PANEL_MARGIN * 2) / (PANEL_WIDTH + PANEL_SPACING))
        var panelCount = 0

        for (category in Category.entries) {
            val modules = ModuleManager.filter { it.category == category }
            if (modules.isEmpty()) continue

            if (panelCount >= maxPanelsPerRow) {
                xPos = PANEL_MARGIN
                yPos += MAX_PANEL_HEIGHT + 20
                panelCount = 0
            }

            val (savedX, savedY) = savedPanelPositions[category] ?: (xPos to yPos)
            val savedExpanded = savedPanelExpanded[category] ?: true
            val savedScroll = savedPanelScrollOffsets[category] ?: 0

            panels.add(CategoryPanel(category, modules, savedX, savedY, PANEL_WIDTH, savedExpanded, savedScroll))

            xPos += PANEL_WIDTH + PANEL_SPACING
            panelCount++
        }
    }

    override fun removed() {
        for (panel in panels) {
            savedPanelPositions[panel.category] = panel.x to panel.y
            savedPanelExpanded[panel.category] = panel.expanded
            savedPanelScrollOffsets[panel.category] = panel.scrollOffset
        }
        super.removed()
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, ARGB.color(180, 20, 20, 20))

        renderSearchBar(context, mouseX, mouseY)

        for (panel in panels) {
            panel.render(context, mouseX, mouseY, delta, fontRenderer, fontScale, searchQuery)
        }

        super.render(context, mouseX, mouseY, delta)
    }

    // Helper method to draw text using Inter font
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

    private fun renderSearchBar(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val barX = PANEL_MARGIN
        val barY = PANEL_MARGIN
        val barWidth = width - PANEL_MARGIN * 2

        context.fill(barX, barY, barX + barWidth, barY + SEARCH_BAR_HEIGHT, 0xC8323232.toInt())

        context.fill(barX, barY, barX + barWidth, barY + 1, 0xFF505050.toInt())
        context.fill(barX, barY + SEARCH_BAR_HEIGHT - 1, barX + barWidth, barY + SEARCH_BAR_HEIGHT, 0xFF505050.toInt())
        context.fill(barX, barY, barX + 1, barY + SEARCH_BAR_HEIGHT, 0xFF505050.toInt())
        context.fill(barX + barWidth - 1, barY, barX + barWidth, barY + SEARCH_BAR_HEIGHT, 0xFF505050.toInt())

        val label = "Search: "
        drawText(context, label, barX + 4f, barY + 5f, COLOR_DARK_GRAY)

        val displayText = if (searchQuery.isEmpty()) "(type to filter modules)" else searchQuery
        val textColor = if (searchQuery.isEmpty()) Color4b(102, 102, 102, 255) else COLOR_WHITE
        drawText(context, displayText, barX + 4f + getTextWidth(label), barY + 5f, textColor)

        if (searchQuery.isNotEmpty() || (System.currentTimeMillis() / 500) % 2 == 0L) {
            val cursorX = barX + 4f + getTextWidth(label) + getTextWidth(searchQuery)
            drawText(context, "_", cursorX, barY + 5f, COLOR_WHITE)
        }
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        val chr = event.codepoint().toChar()
        if (chr.isLetterOrDigit() || chr == ' ' || chr == '_' || chr == '-') {
            searchQuery += chr
            return true
        }
        return super.charTyped(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key == 259 && searchQuery.isNotEmpty()) {
            searchQuery = searchQuery.dropLast(1)
            return true
        }
        if (event.key == 256 && searchQuery.isNotEmpty()) {
            searchQuery = ""
            return true
        }
        return super.keyPressed(event)
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
        // Handle slider drag
        activeSliderDrag?.let { drag ->
            updateSliderValue(drag, click.x)
            return true
        }

        for (panel in panels) {
            if (panel.mouseDragged(click.x, click.y, click.button(), offsetX, offsetY)) {
                return true
            }
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        activeSliderDrag = null
        for (panel in panels) {
            panel.dragging = false
        }
        return super.mouseReleased(click)
    }

    private fun updateSliderValue(drag: SliderDragState, mouseX: Double) {
        val fraction = ((mouseX - drag.sliderX) / drag.sliderWidth).coerceIn(0.0, 1.0)

        when (val inner = drag.value.get()) {
            is Int -> {
                @Suppress("UNCHECKED_CAST")
                val ranged = drag.value as RangedValue<Int>
                val start = ranged.range.start as Int
                val end = ranged.range.endInclusive as Int
                val newVal = (start + (end - start) * fraction).toInt()
                ranged.set(newVal)
            }
            is Float -> {
                @Suppress("UNCHECKED_CAST")
                val ranged = drag.value as RangedValue<Float>
                val start = ranged.range.start as Float
                val end = ranged.range.endInclusive as Float
                val newVal = (start + (end - start) * fraction).toFloat()
                ranged.set(newVal)
            }
            is Double -> {
                @Suppress("UNCHECKED_CAST")
                val ranged = drag.value as RangedValue<Double>
                val start = ranged.range.start as Double
                val end = ranged.range.endInclusive as Double
                val newVal = start + (end - start) * fraction
                ranged.set(newVal)
            }
            is ClosedFloatingPointRange<*> -> {
                @Suppress("UNCHECKED_CAST")
                val ranged = drag.value as RangedValue<ClosedFloatingPointRange<Float>>
                val rangeStart = ranged.range.start as ClosedFloatingPointRange<Float>
                val rangeEnd = ranged.range.endInclusive as ClosedFloatingPointRange<Float>
                val start = rangeStart.start
                val end = rangeEnd.endInclusive
                val currentRange = inner as ClosedFloatingPointRange<Float>
                val newVal = (start + (end - start) * fraction).toFloat()
                if (drag.thumbIndex == 0) {
                    // Left thumb - adjust start
                    val newStart = newVal.coerceAtMost(currentRange.endInclusive)
                    ranged.set(newStart..currentRange.endInclusive)
                } else {
                    // Right thumb - adjust end
                    val newEnd = newVal.coerceAtLeast(currentRange.start)
                    ranged.set(currentRange.start..newEnd)
                }
            }
            is IntRange -> {
                @Suppress("UNCHECKED_CAST")
                val ranged = drag.value as RangedValue<IntRange>
                val rangeStart = ranged.range.start as IntRange
                val rangeEnd = ranged.range.endInclusive as IntRange
                val start = rangeStart.first
                val end = rangeEnd.last
                val newVal = (start + (end - start) * fraction).toInt()
                if (drag.thumbIndex == 0) {
                    val newStart = newVal.coerceAtMost(inner.last)
                    ranged.set(newStart..inner.last)
                } else {
                    val newEnd = newVal.coerceAtLeast(inner.first)
                    ranged.set(inner.first..newEnd)
                }
            }
        }
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
    override fun shouldCloseOnEsc() = searchQuery.isEmpty()

    private class ExpandState {
        var expanded = false
    }

    private inner class CategoryPanel(
        val category: Category,
        private val allModules: List<ClientModule>,
        var x: Int,
        var y: Int,
        private val panelWidth: Int,
        initialExpanded: Boolean = true,
        initialScroll: Int = 0
    ) {
        var expanded = initialExpanded
        var dragging = false
        var scrollOffset = initialScroll
        private var dragOffsetX = 0.0
        private var dragOffsetY = 0.0

        private val moduleExpandStates = mutableMapOf<ClientModule, ExpandState>()
        private val configurableExpandStates = mutableMapOf<Any, ExpandState>()

        private fun getModuleExpandState(module: ClientModule): ExpandState {
            return moduleExpandStates.getOrPut(module) { ExpandState() }
        }

        private fun getConfigurableExpandState(configurable: Any): ExpandState {
            return configurableExpandStates.getOrPut(configurable) { ExpandState() }
        }

        private fun getFilteredModules(searchQuery: String): List<ClientModule> {
            if (searchQuery.isEmpty()) return allModules
            val query = searchQuery.lowercase()
            return allModules.filter { module ->
                module.name.lowercase().contains(query) ||
                module.aliases.any { it.lowercase().contains(query) }
            }
        }

        private fun isSliderValue(value: Value<*>): Boolean {
            if (value !is RangedValue<*>) return false
            val inner = value.get()
            return inner is Number || inner is ClosedFloatingPointRange<*> || inner is IntRange
        }

        private fun isDualSliderValue(value: Value<*>): Boolean {
            if (value !is RangedValue<*>) return false
            val inner = value.get()
            return inner is ClosedFloatingPointRange<*> || inner is IntRange
        }

        private fun getValueHeight(value: Value<*>): Int {
            return if (isSliderValue(value)) SLIDER_HEIGHT else SETTING_HEIGHT
        }

        fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float, fr: FontRenderer, scale: Float, searchQuery: String) {
            val modules = getFilteredModules(searchQuery)
            if (modules.isEmpty() && searchQuery.isNotEmpty()) {
                return
            }

            val contentHeight = if (expanded) calculateTotalContentHeight(modules) else 0
            val visibleContentHeight = min(contentHeight, MAX_PANEL_HEIGHT)
            val totalPanelHeight = PANEL_HEADER_HEIGHT + visibleContentHeight

            val maxScroll = max(0, contentHeight - MAX_PANEL_HEIGHT)
            scrollOffset = scrollOffset.coerceIn(0, maxScroll)

            context.fill(x, y, x + panelWidth, y + totalPanelHeight, 0xB41E1E1E.toInt())

            val headerColor = 0xC8323232.toInt()
            context.fill(x, y, x + panelWidth, y + PANEL_HEADER_HEIGHT, headerColor)

            val displayName = if (modules.size != allModules.size) {
                "${category.choiceName} (${modules.size})"
            } else {
                category.choiceName
            }
            drawPanelText(context, fr, scale, displayName, x + 4f, y + 3f, COLOR_WHITE)

            val indicator = if (expanded) "v" else ">"
            drawPanelText(context, fr, scale, indicator, x + panelWidth - 12f, y + 3f, COLOR_WHITE)

            if (expanded && modules.isNotEmpty()) {
                val contentY = y + PANEL_HEADER_HEIGHT
                context.enableScissor(x, contentY, x + panelWidth, contentY + visibleContentHeight)

                var currentY = contentY - scrollOffset
                for (module in modules) {
                    currentY = renderModuleWithSettings(context, module, currentY, mouseX, mouseY, fr, scale, 0)
                }

                context.disableScissor()

                if (contentHeight > MAX_PANEL_HEIGHT) {
                    val scrollBarHeight = (visibleContentHeight.toFloat() / contentHeight * visibleContentHeight).toInt().coerceAtLeast(10)
                    val scrollBarY = contentY + (scrollOffset.toFloat() / maxScroll * (visibleContentHeight - scrollBarHeight)).toInt()
                    context.fill(x + panelWidth - 3, scrollBarY, x + panelWidth - 1, scrollBarY + scrollBarHeight, 0x80FFFFFF.toInt())
                }
            }
        }

        private fun drawPanelText(context: GuiGraphics, fr: FontRenderer, scale: Float, text: String, x: Float, y: Float, color: Color4b, shadow: Boolean = true) {
            val processedText = fr.process(text.asText(), color)
            context.pose().pushMatrix()
            context.pose().translate(x, y)
            context.pose().scale(scale, scale)
            with(context) {
                fr.draw(processedText, 0f, 0f, shadow = shadow)
            }
            context.pose().popMatrix()
        }

        private fun getTextWidth(fr: FontRenderer, scale: Float, text: String): Float {
            val processedText = fr.process(text.asText(), COLOR_WHITE)
            return fr.getStringWidth(processedText, shadow = true) * scale
        }

        private fun calculateTotalContentHeight(modules: List<ClientModule> = allModules): Int {
            var height = 0
            for (module in modules) {
                height += MODULE_HEIGHT
                val expandState = getModuleExpandState(module)
                if (expandState.expanded) {
                    height += calculateSettingsHeight(module, 0)
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
                    if (subExpandState.expanded) {
                        height += calculateSettingsHeight(value, depth + 1)
                    }
                } else {
                    height += getValueHeight(value)
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
            mouseX: Int, mouseY: Int, fr: FontRenderer, scale: Float, depth: Int
        ): Int {
            var currentY = startY
            val expandState = getModuleExpandState(module)

            renderModuleRow(context, module, currentY, mouseX, mouseY, fr, scale, expandState)
            currentY += MODULE_HEIGHT

            if (expandState.expanded) {
                currentY = renderConfigurableSettings(context, module, currentY, mouseX, mouseY, fr, scale, 1)
            }

            return currentY
        }

        private fun renderModuleRow(
            context: GuiGraphics, module: ClientModule, moduleY: Int,
            mouseX: Int, mouseY: Int, fr: FontRenderer, scale: Float, expandState: ExpandState
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

            val color = if (module.enabled) COLOR_GREEN else COLOR_WHITE
            drawPanelText(context, fr, scale, module.name, x + 6f, moduleY + 2f, color)

            val settings = getFilteredValues(module)
            if (settings.isNotEmpty()) {
                val indicator = if (isExpanded) "-" else "+"
                drawPanelText(context, fr, scale, indicator, x + panelWidth - 12f, moduleY + 2f, COLOR_GRAY)
            } else {
                val bindText = module.bind.keyName
                if (bindText.isNotEmpty() && bindText != UNBOUND_KEY_NAME) {
                    val displayText = "[$bindText]"
                    val textW = getTextWidth(fr, scale, displayText)
                    drawPanelText(context, fr, scale, displayText, x + panelWidth - textW - 6f, moduleY + 2f, COLOR_DARK_GRAY)
                }
            }
        }

        private fun renderConfigurableSettings(
            context: GuiGraphics, configurable: Configurable, startY: Int,
            mouseX: Int, mouseY: Int, fr: FontRenderer, scale: Float, depth: Int
        ): Int {
            var currentY = startY
            val indent = depth * INDENT_PER_LEVEL
            val values = getFilteredValues(configurable)

            for (value in values) {
                when (value) {
                    is ChoiceConfigurable<*> -> {
                        currentY = renderChoiceConfigurable(context, value, currentY, mouseX, mouseY, fr, scale, depth, indent)
                    }
                    is ToggleableConfigurable -> {
                        currentY = renderToggleableConfigurable(context, value, currentY, mouseX, mouseY, fr, scale, depth, indent)
                    }
                    is Configurable -> {
                        currentY = renderNestedConfigurable(context, value, currentY, mouseX, mouseY, fr, scale, depth, indent)
                    }
                    else -> {
                        if (isSliderValue(value)) {
                            renderSliderValue(context, value, currentY, mouseX, mouseY, fr, scale, indent)
                            currentY += SLIDER_HEIGHT
                        } else {
                            renderSettingValue(context, value, currentY, mouseX, mouseY, fr, scale, indent)
                            currentY += SETTING_HEIGHT
                        }
                    }
                }
            }
            return currentY
        }

        private fun renderChoiceConfigurable(
            context: GuiGraphics, choice: ChoiceConfigurable<*>, startY: Int,
            mouseX: Int, mouseY: Int, fr: FontRenderer, scale: Float, depth: Int, indent: Int
        ): Int {
            var currentY = startY
            val expandState = getConfigurableExpandState(choice)
            val isExpanded = expandState.expanded
            val isHovered = mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                mouseY >= currentY && mouseY < currentY + CONFIGURABLE_HEADER_HEIGHT

            val bgColor = when {
                isExpanded -> 0x60506080.toInt()
                isHovered -> 0x50405060.toInt()
                else -> 0x40303050.toInt()
            }
            context.fill(x + 4 + indent, currentY, x + panelWidth - 4, currentY + CONFIGURABLE_HEADER_HEIGHT, bgColor)

            val displayName = choice.name
            val activeChoice = choice.activeChoice.name
            drawPanelText(context, fr, scale, displayName, x + 6f + indent, currentY + 1f, COLOR_PURPLE, false)

            val choiceText = "[$activeChoice]"
            val choiceTextWidth = getTextWidth(fr, scale, choiceText)
            drawPanelText(context, fr, scale, choiceText, x + panelWidth - choiceTextWidth - 16f, currentY + 1f, COLOR_LIGHT_CYAN, false)

            val indicator = if (expandState.expanded) "v" else ">"
            drawPanelText(context, fr, scale, indicator, x + panelWidth - 8f, currentY + 1f, COLOR_GRAY, false)

            currentY += CONFIGURABLE_HEADER_HEIGHT

            if (isExpanded) {
                val activeConfigurable = choice.activeChoice as? Configurable
                if (activeConfigurable != null) {
                    currentY = renderConfigurableSettings(context, activeConfigurable, currentY, mouseX, mouseY, fr, scale, depth + 1)
                }
            }

            return currentY
        }

        private fun renderToggleableConfigurable(
            context: GuiGraphics, toggleable: ToggleableConfigurable, startY: Int,
            mouseX: Int, mouseY: Int, fr: FontRenderer, scale: Float, depth: Int, indent: Int
        ): Int {
            var currentY = startY
            val expandState = getConfigurableExpandState(toggleable)
            val isExpanded = expandState.expanded
            val isHovered = mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                mouseY >= currentY && mouseY < currentY + CONFIGURABLE_HEADER_HEIGHT

            val bgColor = when {
                toggleable.enabled && isExpanded -> 0x60508060.toInt()
                toggleable.enabled -> 0x50406050.toInt()
                isExpanded -> 0x50505060.toInt()
                isHovered -> 0x40404050.toInt()
                else -> 0x30303040.toInt()
            }
            context.fill(x + 4 + indent, currentY, x + panelWidth - 4, currentY + CONFIGURABLE_HEADER_HEIGHT, bgColor)

            val nameColor = if (toggleable.enabled) COLOR_LIGHT_GREEN else COLOR_LIGHT_GRAY
            drawPanelText(context, fr, scale, toggleable.name, x + 6f + indent, currentY + 1f, nameColor, false)

            val statusText = if (toggleable.enabled) "ON" else "OFF"
            val statusColor = if (toggleable.enabled) COLOR_GREEN else COLOR_RED
            val statusWidth = getTextWidth(fr, scale, statusText)
            drawPanelText(context, fr, scale, statusText, x + panelWidth - statusWidth - 20f, currentY + 1f, statusColor, false)

            val indicator = if (expandState.expanded) "v" else ">"
            drawPanelText(context, fr, scale, indicator, x + panelWidth - 8f, currentY + 1f, COLOR_GRAY, false)

            currentY += CONFIGURABLE_HEADER_HEIGHT

            if (isExpanded) {
                currentY = renderConfigurableSettings(context, toggleable, currentY, mouseX, mouseY, fr, scale, depth + 1)
            }

            return currentY
        }

        private fun renderNestedConfigurable(
            context: GuiGraphics, configurable: Configurable, startY: Int,
            mouseX: Int, mouseY: Int, fr: FontRenderer, scale: Float, depth: Int, indent: Int
        ): Int {
            var currentY = startY
            val expandState = getConfigurableExpandState(configurable)
            val isExpanded = expandState.expanded
            val isHovered = mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                mouseY >= currentY && mouseY < currentY + CONFIGURABLE_HEADER_HEIGHT

            val bgColor = when {
                isExpanded -> 0x50506080.toInt()
                isHovered -> 0x40405060.toInt()
                else -> 0x30304050.toInt()
            }
            context.fill(x + 4 + indent, currentY, x + panelWidth - 4, currentY + CONFIGURABLE_HEADER_HEIGHT, bgColor)

            drawPanelText(context, fr, scale, configurable.name, x + 6f + indent, currentY + 1f, COLOR_LIGHT_PURPLE, false)

            val indicator = if (expandState.expanded) "v" else ">"
            drawPanelText(context, fr, scale, indicator, x + panelWidth - 8f, currentY + 1f, COLOR_GRAY, false)

            currentY += CONFIGURABLE_HEADER_HEIGHT

            if (isExpanded) {
                currentY = renderConfigurableSettings(context, configurable, currentY, mouseX, mouseY, fr, scale, depth + 1)
            }

            return currentY
        }

        private fun renderSliderValue(
            context: GuiGraphics, value: Value<*>, settingY: Int,
            mouseX: Int, mouseY: Int, fr: FontRenderer, scale: Float, indent: Int
        ) {
            val isHovered = mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                mouseY >= settingY && mouseY < settingY + SLIDER_HEIGHT

            val backgroundColor = if (isHovered) 0x50404060.toInt() else 0x40202030.toInt()
            context.fill(x + 4 + indent, settingY, x + panelWidth - 4, settingY + SLIDER_HEIGHT, backgroundColor)

            // Draw name
            val displayText = getSettingDisplayText(value)
            val displayName = value.name.take(12) + if (value.name.length > 12) ".." else ""
            drawPanelText(context, fr, scale, displayName, x + 6f + indent, settingY + 1f, COLOR_LIGHT_GRAY, false)

            // Draw value text
            val valueWidth = getTextWidth(fr, scale, displayText)
            drawPanelText(context, fr, scale, displayText, x + panelWidth - valueWidth - 8f, settingY + 1f, COLOR_CYAN, false)

            // Draw slider track
            val sliderX = x + 6 + indent
            val sliderWidth = panelWidth - 14 - indent
            val sliderY = settingY + 12
            val sliderEndX = sliderX + sliderWidth

            // Track background
            context.fill(sliderX, sliderY, sliderEndX, sliderY + SLIDER_TRACK_HEIGHT, 0xFF404040.toInt())

            // Filled portion and thumb(s)
            if (isDualSliderValue(value)) {
                renderDualSlider(context, value, sliderX, sliderY, sliderWidth)
            } else {
                renderSingleSlider(context, value, sliderX, sliderY, sliderWidth)
            }
        }

        private fun renderSingleSlider(context: GuiGraphics, value: Value<*>, sliderX: Int, sliderY: Int, sliderWidth: Int) {
            val ranged = value as RangedValue<*>
            val fraction = when (val inner = ranged.get()) {
                is Int -> {
                    val start = ranged.range.start as Int
                    val end = ranged.range.endInclusive as Int
                    if (end == start) 0f else (inner - start).toFloat() / (end - start)
                }
                is Float -> {
                    val start = ranged.range.start as Float
                    val end = ranged.range.endInclusive as Float
                    if (end == start) 0f else (inner - start) / (end - start)
                }
                is Double -> {
                    val start = ranged.range.start as Double
                    val end = ranged.range.endInclusive as Double
                    if (end == start) 0f else ((inner - start) / (end - start)).toFloat()
                }
                else -> 0f
            }

            val filledWidth = (sliderWidth * fraction).toInt()
            context.fill(sliderX, sliderY, sliderX + filledWidth, sliderY + SLIDER_TRACK_HEIGHT, 0xFF4488CC.toInt())

            // Thumb
            val thumbX = sliderX + filledWidth - SLIDER_THUMB_WIDTH / 2
            context.fill(thumbX, sliderY - 1, thumbX + SLIDER_THUMB_WIDTH, sliderY + SLIDER_TRACK_HEIGHT + 1, 0xFFFFFFFF.toInt())
        }

        private fun renderDualSlider(context: GuiGraphics, value: Value<*>, sliderX: Int, sliderY: Int, sliderWidth: Int) {
            val ranged = value as RangedValue<*>
            val inner = ranged.get()

            val (startFraction, endFraction) = when (inner) {
                is ClosedFloatingPointRange<*> -> {
                    val range = inner as ClosedFloatingPointRange<Float>
                    val rangeStart = ranged.range.start as ClosedFloatingPointRange<*>
                    val rangeEnd = ranged.range.endInclusive as ClosedFloatingPointRange<*>
                    val min = (rangeStart.start as Float)
                    val max = (rangeEnd.endInclusive as Float)
                    if (max == min) {
                        0f to 1f
                    } else {
                        val s = (range.start - min) / (max - min)
                        val e = (range.endInclusive - min) / (max - min)
                        s to e
                    }
                }
                is IntRange -> {
                    val rangeStart = ranged.range.start as IntRange
                    val rangeEnd = ranged.range.endInclusive as IntRange
                    val min = rangeStart.first
                    val max = rangeEnd.last
                    if (max == min) {
                        0f to 1f
                    } else {
                        val s = (inner.first - min).toFloat() / (max - min)
                        val e = (inner.last - min).toFloat() / (max - min)
                        s to e
                    }
                }
                else -> 0f to 1f
            }

            val startX = sliderX + (sliderWidth * startFraction).toInt()
            val endX = sliderX + (sliderWidth * endFraction).toInt()

            // Filled portion between thumbs
            context.fill(startX, sliderY, endX, sliderY + SLIDER_TRACK_HEIGHT, 0xFF4488CC.toInt())

            // Left thumb
            val leftThumbX = startX - SLIDER_THUMB_WIDTH / 2
            context.fill(leftThumbX, sliderY - 1, leftThumbX + SLIDER_THUMB_WIDTH, sliderY + SLIDER_TRACK_HEIGHT + 1, 0xFF88CCFF.toInt())

            // Right thumb
            val rightThumbX = endX - SLIDER_THUMB_WIDTH / 2
            context.fill(rightThumbX, sliderY - 1, rightThumbX + SLIDER_THUMB_WIDTH, sliderY + SLIDER_TRACK_HEIGHT + 1, 0xFFFFCC88.toInt())
        }

        private fun renderSettingValue(
            context: GuiGraphics, value: Value<*>, settingY: Int,
            mouseX: Int, mouseY: Int, fr: FontRenderer, scale: Float, indent: Int
        ) {
            val isHovered = mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                mouseY >= settingY && mouseY < settingY + SETTING_HEIGHT

            val backgroundColor = if (isHovered) 0x50404060.toInt() else 0x40202030.toInt()
            context.fill(x + 4 + indent, settingY, x + panelWidth - 4, settingY + SETTING_HEIGHT, backgroundColor)

            val displayText = getSettingDisplayText(value)
            val displayName = value.name.take(12) + if (value.name.length > 12) ".." else ""

            drawPanelText(context, fr, scale, displayName, x + 6f + indent, settingY + 1f, COLOR_LIGHT_GRAY, false)

            val valueColor = getValueColorC4b(value)
            val valueWidth = getTextWidth(fr, scale, displayText)
            drawPanelText(context, fr, scale, displayText, x + panelWidth - valueWidth - 8f, settingY + 1f, valueColor, false)
        }

        private fun getSettingDisplayText(value: Value<*>): String {
            return when (val inner = value.get()) {
                is Boolean -> if (inner) "ON" else "OFF"
                is Int -> inner.toString()
                is Float -> String.format("%.1f", inner)
                is Double -> String.format("%.2f", inner)
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

        private fun getValueColorC4b(value: Value<*>): Color4b {
            return when (val inner = value.get()) {
                is Boolean -> if (inner) COLOR_GREEN else COLOR_RED
                is Number -> COLOR_CYAN
                is NamedChoice -> COLOR_YELLOW
                is ClosedFloatingPointRange<*>, is IntRange -> COLOR_CYAN
                else -> COLOR_GRAY
            }
        }

        fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (isMouseInHeader(mouseX, mouseY)) {
                return handleHeaderClick(mouseX, mouseY, button)
            }
            if (expanded && isMouseInContent(mouseX, mouseY)) {
                return handleContentClick(mouseX, mouseY, button)
            }
            return false
        }

        private fun isMouseInHeader(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x && mouseX < x + panelWidth && mouseY >= y && mouseY < y + PANEL_HEADER_HEIGHT
        }

        private fun isMouseInContent(mouseX: Double, mouseY: Double): Boolean {
            val contentHeight = min(calculateTotalContentHeight(), MAX_PANEL_HEIGHT)
            return mouseX >= x && mouseX < x + panelWidth &&
                mouseY >= y + PANEL_HEADER_HEIGHT && mouseY < y + PANEL_HEADER_HEIGHT + contentHeight
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
            val modules = getFilteredModules(searchQuery)
            var currentY = y + PANEL_HEADER_HEIGHT - scrollOffset

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

            if (mouseX >= x && mouseX < x + panelWidth && mouseY >= currentY && mouseY < currentY + MODULE_HEIGHT) {
                when (button) {
                    0 -> module.enabled = !module.enabled
                    1 -> expandState.expanded = !expandState.expanded
                }
                return true to currentY + MODULE_HEIGHT
            }
            currentY += MODULE_HEIGHT

            if (expandState.expanded) {
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

                        if (expandState.expanded) {
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

                        if (expandState.expanded) {
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

                        if (expandState.expanded) {
                            val result = handleConfigurableClick(value, mouseX, mouseY, button, currentY, depth + 1)
                            if (result.first) return result
                            currentY = result.second
                        }
                    }
                    else -> {
                        val valueHeight = getValueHeight(value)
                        if (mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                            mouseY >= currentY && mouseY < currentY + valueHeight) {
                            if (isSliderValue(value)) {
                                // Start slider drag
                                val sliderX = x + 6 + indent
                                val sliderWidth = panelWidth - 14 - indent

                                // Determine which thumb (for dual slider)
                                val thumbIndex = if (isDualSliderValue(value)) {
                                    // Check which thumb is closer
                                    val ranged = value as RangedValue<*>
                                    val inner = ranged.get()
                                    val (startFrac, endFrac) = when (inner) {
                                        is ClosedFloatingPointRange<*> -> {
                                            val range = inner as ClosedFloatingPointRange<Float>
                                            val rangeStart = ranged.range.start as ClosedFloatingPointRange<*>
                                            val rangeEnd = ranged.range.endInclusive as ClosedFloatingPointRange<*>
                                            val min = (rangeStart.start as Float)
                                            val max = (rangeEnd.endInclusive as Float)
                                            if (max == min) 0f to 1f
                                            else (range.start - min) / (max - min) to (range.endInclusive - min) / (max - min)
                                        }
                                        is IntRange -> {
                                            val rangeStart = ranged.range.start as IntRange
                                            val rangeEnd = ranged.range.endInclusive as IntRange
                                            val min = rangeStart.first
                                            val max = rangeEnd.last
                                            if (max == min) 0f to 1f
                                            else (inner.first - min).toFloat() / (max - min) to (inner.last - min).toFloat() / (max - min)
                                        }
                                        else -> 0f to 1f
                                    }

                                    val startThumbX = sliderX + (sliderWidth * startFrac)
                                    val endThumbX = sliderX + (sliderWidth * endFrac)
                                    if (kotlin.math.abs(mouseX - startThumbX) < kotlin.math.abs(mouseX - endThumbX)) 0 else 1
                                } else {
                                    0
                                }

                                activeSliderDrag = SliderDragState(value, thumbIndex, sliderX, sliderWidth)
                                updateSliderValue(activeSliderDrag!!, mouseX)
                                return true to currentY
                            } else {
                                handleSettingClick(value, button)
                                return true to currentY
                            }
                        }
                        currentY += valueHeight
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

        fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
            if (!expanded) return false

            val contentHeight = min(calculateTotalContentHeight(), MAX_PANEL_HEIGHT)
            if (mouseX >= x && mouseX < x + panelWidth &&
                mouseY >= y && mouseY < y + PANEL_HEADER_HEIGHT + contentHeight) {

                val totalContentHeight = calculateTotalContentHeight()
                if (totalContentHeight > MAX_PANEL_HEIGHT) {
                    val scrollAmount = (amount * 20).toInt()
                    val maxScroll = totalContentHeight - MAX_PANEL_HEIGHT
                    scrollOffset = (scrollOffset - scrollAmount).coerceIn(0, maxScroll)
                    return true
                }
            }

            return false
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
