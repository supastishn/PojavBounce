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

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.ChooseListValue
import net.ccbluex.liquidbounce.config.types.MultiChooseListValue
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.util.ARGB
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Native Minecraft GUI implementation of ClickGUI
 * Features:
 * - Category panels with module lists
 * - Nested configurable support (accordions)
 * - Search bar for filtering modules
 * - Panel scrolling
 * - Persistent panel positions (saved across sessions)
 * - Slider controls for ranged values
 * - Inter font support
 */
class NativeClickGuiScreen : Screen("ClickGUI".asPlainText()) {

    private val panels = mutableListOf<CategoryPanel>()
    private var searchQuery = ""

    // Global scroll offset for viewing panels below the screen
    private var globalScrollOffset = 0
    private var maxGlobalScroll = 0

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
        private const val MULTI_CHOICE_ITEM_HEIGHT = 11
        private const val PANEL_SPACING = 10
        private const val PANEL_MARGIN = 10
        private const val SEARCH_BAR_HEIGHT = 20
        private const val UNBOUND_KEY_NAME = "None"
        private const val INDENT_PER_LEVEL = 6
        private const val MAX_PANEL_HEIGHT = 300
        private const val SLIDER_TRACK_HEIGHT = 4
        private const val SLIDER_THUMB_WIDTH = 6

        // Persistent state (survives screen close/reopen AND across sessions via file)
        private val savedPanelPositions = mutableMapOf<String, Pair<Int, Int>>()
        private val savedPanelExpanded = mutableMapOf<String, Boolean>()
        private val savedPanelScrollOffsets = mutableMapOf<String, Int>()
        private val savedModuleExpanded = mutableMapOf<String, Boolean>()
        private var savedGlobalScroll = 0
        private var stateLoaded = false

        // Gson for serialization
        private val gson = GsonBuilder().setPrettyPrinting().create()
        private val stateFile: File
            get() = File(ConfigSystem.rootFolder, "clickgui_state.json")

        /**
         * Load saved state from file
         */
        fun loadState() {
            if (stateLoaded) return
            stateLoaded = true

            try {
                if (!stateFile.exists()) return

                val json = stateFile.readText()
                val state = gson.fromJson(json, ClickGuiState::class.java) ?: return

                savedPanelPositions.clear()
                state.panelPositions.forEach { (k, v) -> savedPanelPositions[k] = v[0] to v[1] }

                savedPanelExpanded.clear()
                savedPanelExpanded.putAll(state.panelExpanded)

                savedPanelScrollOffsets.clear()
                savedPanelScrollOffsets.putAll(state.panelScrollOffsets)

                savedModuleExpanded.clear()
                savedModuleExpanded.putAll(state.moduleExpanded)

                savedGlobalScroll = state.globalScroll

                logger.info("Loaded ClickGUI state from file")
            } catch (e: Exception) {
                logger.error("Failed to load ClickGUI state", e)
            }
        }

        /**
         * Save state to file
         */
        fun saveState() {
            try {
                val state = ClickGuiState(
                    panelPositions = savedPanelPositions.mapValues { listOf(it.value.first, it.value.second) },
                    panelExpanded = savedPanelExpanded.toMap(),
                    panelScrollOffsets = savedPanelScrollOffsets.toMap(),
                    moduleExpanded = savedModuleExpanded.toMap(),
                    globalScroll = savedGlobalScroll
                )

                stateFile.writeText(gson.toJson(state))
                logger.debug("Saved ClickGUI state to file")
            } catch (e: Exception) {
                logger.error("Failed to save ClickGUI state", e)
            }
        }

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

    /**
     * Data class for serializing ClickGUI state
     */
    private data class ClickGuiState(
        val panelPositions: Map<String, List<Int>> = emptyMap(),
        val panelExpanded: Map<String, Boolean> = emptyMap(),
        val panelScrollOffsets: Map<String, Int> = emptyMap(),
        val moduleExpanded: Map<String, Boolean> = emptyMap(),
        val globalScroll: Int = 0
    )

    // Represents active slider being dragged
    private data class SliderDragState(
        val value: Value<*>,
        val thumbIndex: Int,  // 0 for single slider or left thumb, 1 for right thumb
        val sliderX: Int,
        val sliderWidth: Int
    )

    // Represents active text editing
    private data class TextEditState(
        val value: Value<String>,
        var text: String,
        var cursorPos: Int
    )

    // Active text being edited
    private var activeTextEdit: TextEditState? = null

    override fun init() {
        super.init()
        panels.clear()

        // Load saved state from file on first open
        loadState()

        // Reserve space for scrollbar on the right (always visible)
        val scrollbarWidth = 10
        val availableWidth = width - PANEL_MARGIN * 2 - scrollbarWidth

        var xPos = PANEL_MARGIN
        var yPos = PANEL_MARGIN + SEARCH_BAR_HEIGHT + 5
        val maxPanelsPerRow = max(1, availableWidth / (PANEL_WIDTH + PANEL_SPACING))
        var panelCount = 0
        var rowCount = 1

        for (category in ModuleCategories.entries) {
            val modules = ModuleManager.filter { it.category == category }
            if (modules.isEmpty()) continue

            if (panelCount >= maxPanelsPerRow) {
                xPos = PANEL_MARGIN
                yPos += MAX_PANEL_HEIGHT + 20
                panelCount = 0
                rowCount++
            }

            // Use saved position if available, otherwise use calculated position
            val categoryKey = category.choiceName
            val savedPos = savedPanelPositions[categoryKey]
            val panelX = savedPos?.first ?: xPos
            val panelY = savedPos?.second ?: yPos
            val savedExpanded = savedPanelExpanded[categoryKey] ?: true
            val savedScroll = savedPanelScrollOffsets[categoryKey] ?: 0

            panels.add(CategoryPanel(category, modules, panelX, panelY, PANEL_WIDTH, savedExpanded, savedScroll))

            xPos += PANEL_WIDTH + PANEL_SPACING
            panelCount++
        }

        // Calculate total content height based on all rows
        val totalContentHeight = PANEL_MARGIN + SEARCH_BAR_HEIGHT + 5 + (rowCount * (MAX_PANEL_HEIGHT + 20)) - 20 + PANEL_MARGIN

        // Always allow scrolling if content exceeds viewport
        maxGlobalScroll = max(0, totalContentHeight - height)
        globalScrollOffset = savedGlobalScroll.coerceIn(0, max(0, maxGlobalScroll))
    }

    override fun removed() {
        for (panel in panels) {
            val categoryKey = panel.category.choiceName
            savedPanelPositions[categoryKey] = panel.x to panel.y
            savedPanelExpanded[categoryKey] = panel.expanded
            savedPanelScrollOffsets[categoryKey] = panel.scrollOffset

            // Save module expanded states
            panel.saveModuleExpandedStates()
        }
        savedGlobalScroll = globalScrollOffset

        // Save state to file
        saveState()

        super.removed()
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, ARGB.color(180, 20, 20, 20))

        renderSearchBar(context, mouseX, mouseY)

        // Render panels with global scroll offset applied
        for (panel in panels) {
            panel.render(context, mouseX, mouseY, delta, fontRenderer, fontScale, searchQuery, globalScrollOffset)
        }

        // Always render scrollbar (visible indicator that scrolling is possible)
        val scrollbarX = width - 8
        val scrollbarTop = PANEL_MARGIN + SEARCH_BAR_HEIGHT + 5
        val scrollbarHeight = height - scrollbarTop - PANEL_MARGIN

        // Scrollbar track background (always visible)
        context.fill(scrollbarX - 1, scrollbarTop, scrollbarX + 7, scrollbarTop + scrollbarHeight, 0x60000000.toInt())

        // Scrollbar thumb
        if (maxGlobalScroll > 0) {
            val thumbHeight = max(20, (scrollbarHeight.toFloat() * scrollbarHeight / (scrollbarHeight + maxGlobalScroll)).toInt())
            val thumbY = scrollbarTop + ((scrollbarHeight - thumbHeight).toFloat() * globalScrollOffset / maxGlobalScroll).toInt()
            context.fill(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFFAAAAAA.toInt())
        } else {
            // Even when no scroll needed, show full-size thumb to indicate position
            context.fill(scrollbarX, scrollbarTop, scrollbarX + 6, scrollbarTop + scrollbarHeight, 0x80888888.toInt())
        }

        super.render(context, mouseX, mouseY, delta)
    }

    // Helper method to draw text using Inter font
    private fun drawText(context: GuiGraphics, text: String, x: Float, y: Float, color: Color4b, shadow: Boolean = true) {
        val processedText = fontRenderer.process(text.asText(), color)
        with(context) {
            fontRenderer.draw(processedText) {
                this.x = x
                this.y = y
                this.scale = fontScale
                this.shadow = shadow
            }
        }
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

        // Handle text editing first
        activeTextEdit?.let { edit ->
            if (chr.isLetterOrDigit() || chr == ' ' || chr == '_' || chr == '-' || chr == '.' || chr == '@' || chr == '!' || chr == '#' || chr == '$' || chr == '%' || chr == '^' || chr == '&' || chr == '*' || chr == '(' || chr == ')') {
                edit.text = edit.text.substring(0, edit.cursorPos) + chr + edit.text.substring(edit.cursorPos)
                edit.cursorPos++
                edit.value.set(edit.text)
                return true
            }
            return true
        }

        if (chr.isLetterOrDigit() || chr == ' ' || chr == '_' || chr == '-') {
            searchQuery += chr
            return true
        }
        return super.charTyped(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        // Handle text editing first
        activeTextEdit?.let { edit ->
            when (event.key) {
                259 -> { // Backspace
                    if (edit.cursorPos > 0) {
                        edit.text = edit.text.substring(0, edit.cursorPos - 1) + edit.text.substring(edit.cursorPos)
                        edit.cursorPos--
                        edit.value.set(edit.text)
                    }
                    return true
                }
                261 -> { // Delete
                    if (edit.cursorPos < edit.text.length) {
                        edit.text = edit.text.substring(0, edit.cursorPos) + edit.text.substring(edit.cursorPos + 1)
                        edit.value.set(edit.text)
                    }
                    return true
                }
                263 -> { // Left arrow
                    if (edit.cursorPos > 0) edit.cursorPos--
                    return true
                }
                262 -> { // Right arrow
                    if (edit.cursorPos < edit.text.length) edit.cursorPos++
                    return true
                }
                256, 257 -> { // Escape or Enter - finish editing
                    activeTextEdit = null
                    return true
                }
                268 -> { // Home
                    edit.cursorPos = 0
                    return true
                }
                269 -> { // End
                    edit.cursorPos = edit.text.length
                    return true
                }
            }
            return true
        }

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
        // Clear text edit if clicking elsewhere (unless clicking on the same text field)
        val wasTextEditing = activeTextEdit != null

        // Adjust mouse Y for global scroll
        val adjustedY = click.y + globalScrollOffset

        for (panel in panels) {
            if (panel.mouseClicked(click.x, adjustedY, click.button())) {
                return true
            }
        }

        // If we were editing and didn't click a new setting, clear the edit
        if (wasTextEditing && activeTextEdit != null) {
            activeTextEdit = null
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        // Handle slider drag
        activeSliderDrag?.let { drag ->
            updateSliderValue(drag, click.x)
            return true
        }

        // Adjust mouse Y for global scroll
        val adjustedY = click.y + globalScrollOffset

        for (panel in panels) {
            if (panel.mouseDragged(click.x, adjustedY, click.button(), offsetX, offsetY)) {
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
                // For floatRange, ranged.range.start and endInclusive are Float values
                val start = ranged.range.start as Float
                val end = ranged.range.endInclusive as Float
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
                // For intRange, ranged.range.start and endInclusive are Int values
                val start = ranged.range.start as Int
                val end = ranged.range.endInclusive as Int
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
        // Adjust mouse Y for global scroll
        val adjustedMouseY = mouseY + globalScrollOffset

        // First try panel-specific scrolling (only if panel needs internal scroll)
        for (panel in panels) {
            if (panel.mouseScrolled(mouseX, adjustedMouseY, vertical)) {
                return true
            }
        }

        // Global scroll - always handle if there's overflow content OR if we're scrolled
        if (maxGlobalScroll > 0 || globalScrollOffset > 0) {
            globalScrollOffset = (globalScrollOffset - (vertical * 30).toInt()).coerceIn(0, max(0, maxGlobalScroll))
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun isPauseScreen() = false
    override fun shouldCloseOnEsc() = searchQuery.isEmpty()

    private class ExpandState {
        var expanded = false
    }

    private inner class CategoryPanel(
        val category: ModuleCategory,
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

        init {
            // Load saved module expanded states
            for (module in allModules) {
                val key = "${category.choiceName}:${module.name}"
                savedModuleExpanded[key]?.let { expanded ->
                    moduleExpandStates[module] = ExpandState().apply { this.expanded = expanded }
                }
            }
        }

        fun saveModuleExpandedStates() {
            for ((module, state) in moduleExpandStates) {
                val key = "${category.choiceName}:${module.name}"
                savedModuleExpanded[key] = state.expanded
            }
        }

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
            return when {
                value is MultiChooseListValue<*> -> CONFIGURABLE_HEADER_HEIGHT + value.choices.size * MULTI_CHOICE_ITEM_HEIGHT
                isSliderValue(value) -> SLIDER_HEIGHT
                else -> SETTING_HEIGHT
            }
        }

        fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float, fr: FontRenderer, scale: Float, searchQuery: String, globalScrollOffset: Int = 0) {
            val modules = getFilteredModules(searchQuery)
            if (modules.isEmpty() && searchQuery.isNotEmpty()) {
                return
            }

            // Apply global scroll offset to panel position
            val renderY = y - globalScrollOffset
            val adjustedMouseY = mouseY + globalScrollOffset

            val contentHeight = if (expanded) calculateTotalContentHeight(modules) else 0
            val visibleContentHeight = min(contentHeight, MAX_PANEL_HEIGHT)
            val totalPanelHeight = PANEL_HEADER_HEIGHT + visibleContentHeight

            val maxScroll = max(0, contentHeight - MAX_PANEL_HEIGHT)
            scrollOffset = scrollOffset.coerceIn(0, maxScroll)

            context.fill(x, renderY, x + panelWidth, renderY + totalPanelHeight, 0xB41E1E1E.toInt())

            val headerColor = 0xC8323232.toInt()
            context.fill(x, renderY, x + panelWidth, renderY + PANEL_HEADER_HEIGHT, headerColor)

            val displayName = if (modules.size != allModules.size) {
                "${category.choiceName} (${modules.size})"
            } else {
                category.choiceName
            }
            drawPanelText(context, fr, scale, displayName, x + 4f, renderY + 3f, COLOR_WHITE)

            val indicator = if (expanded) "v" else ">"
            drawPanelText(context, fr, scale, indicator, x + panelWidth - 12f, renderY + 3f, COLOR_WHITE)

            if (expanded && modules.isNotEmpty()) {
                val contentY = renderY + PANEL_HEADER_HEIGHT
                context.enableScissor(x, contentY, x + panelWidth, contentY + visibleContentHeight)

                var currentY = contentY - scrollOffset
                for (module in modules) {
                    currentY = renderModuleWithSettings(context, module, currentY, mouseX, adjustedMouseY, fr, scale, 0)
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
            with(context) {
                fr.draw(processedText) {
                    this.x = x
                    this.y = y
                    this.scale = scale
                    this.shadow = shadow
                }
            }
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
                    is MultiChooseListValue<*> -> {
                        currentY = renderMultiChooseValue(context, value, currentY, mouseX, mouseY, fr, scale, indent)
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

        private fun renderMultiChooseValue(
            context: GuiGraphics, value: MultiChooseListValue<*>, startY: Int,
            mouseX: Int, mouseY: Int, fr: FontRenderer, scale: Float, indent: Int
        ): Int {
            var currentY = startY

            // Draw header with name
            val isHeaderHovered = mouseX >= x + indent && mouseX < x + panelWidth - 4 &&
                mouseY >= currentY && mouseY < currentY + CONFIGURABLE_HEADER_HEIGHT

            val headerBgColor = if (isHeaderHovered) 0x50405060.toInt() else 0x40303050.toInt()
            context.fill(x + 4 + indent, currentY, x + panelWidth - 4, currentY + CONFIGURABLE_HEADER_HEIGHT, headerBgColor)

            drawPanelText(context, fr, scale, value.name, x + 6f + indent, currentY + 1f, COLOR_LIGHT_PURPLE, false)

            // Show count of selected items
            val selectedCount = value.get().size
            val totalCount = value.choices.size
            val countText = "[$selectedCount/$totalCount]"
            val countWidth = getTextWidth(fr, scale, countText)
            drawPanelText(context, fr, scale, countText, x + panelWidth - countWidth - 8f, currentY + 1f, COLOR_GRAY, false)

            currentY += CONFIGURABLE_HEADER_HEIGHT

            // Draw each choice as a toggleable item
            val selected = value.get()
            for (choice in value.choices) {
                val isSelected = choice in selected
                val isChoiceHovered = mouseX >= x + indent + INDENT_PER_LEVEL && mouseX < x + panelWidth - 4 &&
                    mouseY >= currentY && mouseY < currentY + MULTI_CHOICE_ITEM_HEIGHT

                val choiceBgColor = when {
                    isSelected && isChoiceHovered -> 0x60508060.toInt()
                    isSelected -> 0x50406050.toInt()
                    isChoiceHovered -> 0x40404050.toInt()
                    else -> 0x30303040.toInt()
                }
                context.fill(x + 4 + indent + INDENT_PER_LEVEL, currentY, x + panelWidth - 4, currentY + MULTI_CHOICE_ITEM_HEIGHT, choiceBgColor)

                // Checkbox indicator
                val checkboxText = if (isSelected) "[x]" else "[ ]"
                val checkboxColor = if (isSelected) COLOR_GREEN else COLOR_GRAY
                drawPanelText(context, fr, scale, checkboxText, x + 6f + indent + INDENT_PER_LEVEL, currentY + 1f, checkboxColor, false)

                // Choice name
                val choiceName = choice.choiceName
                val nameColor = if (isSelected) COLOR_LIGHT_GREEN else COLOR_LIGHT_GRAY
                drawPanelText(context, fr, scale, choiceName, x + 24f + indent + INDENT_PER_LEVEL, currentY + 1f, nameColor, false)

                currentY += MULTI_CHOICE_ITEM_HEIGHT
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
                    @Suppress("UNCHECKED_CAST")
                    val range = inner as ClosedFloatingPointRange<Float>
                    // For floatRange, ranged.range.start and endInclusive are Float values
                    val min = ranged.range.start as Float
                    val max = ranged.range.endInclusive as Float
                    if (max == min) {
                        0f to 1f
                    } else {
                        val s = (range.start - min) / (max - min)
                        val e = (range.endInclusive - min) / (max - min)
                        s to e
                    }
                }
                is IntRange -> {
                    // For intRange, ranged.range.start and endInclusive are Int values
                    val min = ranged.range.start as Int
                    val max = ranged.range.endInclusive as Int
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

            // Check if this value is currently being edited
            val isEditing = activeTextEdit?.value === value

            val backgroundColor = when {
                isEditing -> 0x60606080.toInt()
                isHovered -> 0x50404060.toInt()
                else -> 0x40202030.toInt()
            }
            context.fill(x + 4 + indent, settingY, x + panelWidth - 4, settingY + SETTING_HEIGHT, backgroundColor)

            val displayName = value.name.take(12) + if (value.name.length > 12) ".." else ""
            drawPanelText(context, fr, scale, displayName, x + 6f + indent, settingY + 1f, COLOR_LIGHT_GRAY, false)

            // For text being edited, show with cursor
            if (isEditing) {
                val edit = activeTextEdit!!
                val beforeCursor = edit.text.substring(0, edit.cursorPos)
                val afterCursor = edit.text.substring(edit.cursorPos)
                val cursorVisible = (System.currentTimeMillis() / 500) % 2 == 0L
                val displayText = beforeCursor + (if (cursorVisible) "|" else "") + afterCursor
                val valueWidth = getTextWidth(fr, scale, displayText)
                drawPanelText(context, fr, scale, displayText, x + panelWidth - valueWidth - 8f, settingY + 1f, COLOR_WHITE, false)
            } else {
                val displayText = getSettingDisplayText(value)
                val valueColor = getValueColorC4b(value)
                val valueWidth = getTextWidth(fr, scale, displayText)
                drawPanelText(context, fr, scale, displayText, x + panelWidth - valueWidth - 8f, settingY + 1f, valueColor, false)
            }
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
                    is MultiChooseListValue<*> -> {
                        // Skip the header row
                        currentY += CONFIGURABLE_HEADER_HEIGHT

                        // Check each choice item
                        for (choice in value.choices) {
                            if (mouseX >= x + indent + INDENT_PER_LEVEL && mouseX < x + panelWidth - 4 &&
                                mouseY >= currentY && mouseY < currentY + MULTI_CHOICE_ITEM_HEIGHT) {
                                if (button == 0) {
                                    @Suppress("UNCHECKED_CAST")
                                    (value as MultiChooseListValue<NamedChoice>).toggle(choice as NamedChoice)
                                }
                                return true to currentY
                            }
                            currentY += MULTI_CHOICE_ITEM_HEIGHT
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
                                            // ranged.range.start and endInclusive are Float values (min/max bounds)
                                            val min = ranged.range.start as Float
                                            val max = ranged.range.endInclusive as Float
                                            if (max == min) 0f to 1f
                                            else (range.start - min) / (max - min) to (range.endInclusive - min) / (max - min)
                                        }
                                        is IntRange -> {
                                            // ranged.range.start and endInclusive are Int values (min/max bounds)
                                            val min = ranged.range.start as Int
                                            val max = ranged.range.endInclusive as Int
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
                is String -> {
                    // Start text editing
                    @Suppress("UNCHECKED_CAST")
                    activeTextEdit = TextEditState(value as Value<String>, inner, inner.length)
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
                // Only consume scroll if this panel actually needs internal scrolling
                if (totalContentHeight > MAX_PANEL_HEIGHT) {
                    val scrollAmount = (amount * 20).toInt()
                    val maxScroll = totalContentHeight - MAX_PANEL_HEIGHT
                    val oldOffset = scrollOffset
                    scrollOffset = (scrollOffset - scrollAmount).coerceIn(0, maxScroll)
                    // Only consume the event if we actually scrolled
                    if (scrollOffset != oldOffset) {
                        return true
                    }
                }
            }

            // Don't consume scroll event - let global scroll handle it
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
