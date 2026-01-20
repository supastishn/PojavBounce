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
package net.ccbluex.liquidbounce.integration.ui.thememanager

import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.integration.theme.Theme
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
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
 * Native Theme Manager screen with Inter font support
 * Features:
 * - List of available themes
 * - Select/activate theme
 * - Toggle shader background
 * - Shows theme info (name, author, version)
 */
class NativeThemeManagerScreen(private val parent: Screen?) : Screen("Theme Manager".asPlainText()) {

    private val fontRenderer: FontRenderer get() = FontManager.FONT_RENDERER
    private val fontScale = 0.22f

    private var scrollOffset = 0
    private var selectedIndex = -1
    private var statusMessage = ""
    private var statusColor = COLOR_WHITE

    companion object {
        private const val THEME_ROW_HEIGHT = 36
        private const val LIST_TOP = 50
        private const val LIST_PADDING = 20

        private val COLOR_WHITE = Color4b(255, 255, 255, 255)
        private val COLOR_GREEN = Color4b(0, 255, 0, 255)
        private val COLOR_RED = Color4b(255, 102, 102, 255)
        private val COLOR_CYAN = Color4b(0, 255, 255, 255)
        private val COLOR_GRAY = Color4b(170, 170, 170, 255)
        private val COLOR_YELLOW = Color4b(255, 204, 102, 255)
        private val COLOR_PURPLE = Color4b(200, 150, 255, 255)
        private val COLOR_GOLD = Color4b(255, 215, 0, 255)
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

        // Select theme button
        addRenderableWidget(
            Button.builder("Select".asPlainText()) {
                val themes = ThemeManager.themes
                if (selectedIndex >= 0 && selectedIndex < themes.size) {
                    val theme = themes[selectedIndex]
                    ThemeManager.theme = theme
                    statusMessage = "Theme selected: ${theme.metadata.name}"
                    statusColor = COLOR_GREEN
                } else {
                    statusMessage = "Select a theme first!"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Reload themes button
        addRenderableWidget(
            Button.builder("Reload".asPlainText()) {
                try {
                    runBlocking {
                        ThemeManager.load()
                    }
                    statusMessage = "Themes reloaded"
                    statusColor = COLOR_GREEN
                } catch (e: Exception) {
                    statusMessage = "Failed: ${e.message}"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Toggle shader button
        addRenderableWidget(
            Button.builder("Toggle Shader".asPlainText()) {
                ThemeManager.shaderEnabled = !ThemeManager.shaderEnabled
                val status = if (ThemeManager.shaderEnabled) "enabled" else "disabled"
                statusMessage = "Shader $status"
                statusColor = if (ThemeManager.shaderEnabled) COLOR_GREEN else COLOR_YELLOW
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
        context.fill(0, 0, width, height, ARGB.color(200, 30, 25, 40))

        // Title
        drawText(context, "Theme Manager", width / 2f - getTextWidth("Theme Manager") / 2, 10f, COLOR_CYAN)

        // Current theme info
        val currentTheme = ThemeManager.theme
        val themeInfo = if (currentTheme != null) {
            "Current: ${currentTheme.metadata.name}"
        } else {
            "No theme active"
        }
        drawText(context, themeInfo, width / 2f - getTextWidth(themeInfo) / 2, 25f, COLOR_GRAY)

        // Shader status
        val shaderStatus = "Shader: ${if (ThemeManager.shaderEnabled) "ON" else "OFF"}"
        val shaderColor = if (ThemeManager.shaderEnabled) COLOR_GREEN else COLOR_GRAY
        drawText(context, shaderStatus, width / 2f - getTextWidth(shaderStatus) / 2, 37f, shaderColor)

        // Theme list
        renderThemeList(context, mouseX, mouseY)

        // Status message
        if (statusMessage.isNotEmpty()) {
            drawText(context, statusMessage, width / 2f - getTextWidth(statusMessage) / 2, height - 55f, statusColor)
        }

        super.render(context, mouseX, mouseY, delta)
    }

    private fun renderThemeList(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val listX = LIST_PADDING
        val listWidth = width - LIST_PADDING * 2
        val listHeight = height - LIST_TOP - 70
        val listBottom = LIST_TOP + listHeight

        // List background
        context.fill(listX, LIST_TOP, listX + listWidth, listBottom, ARGB.color(150, 40, 35, 55))

        // Border
        context.fill(listX, LIST_TOP, listX + listWidth, LIST_TOP + 1, ARGB.color(255, 80, 70, 100))
        context.fill(listX, listBottom - 1, listX + listWidth, listBottom, ARGB.color(255, 80, 70, 100))
        context.fill(listX, LIST_TOP, listX + 1, listBottom, ARGB.color(255, 80, 70, 100))
        context.fill(listX + listWidth - 1, LIST_TOP, listX + listWidth, listBottom, ARGB.color(255, 80, 70, 100))

        // Enable scissor for clipping
        context.enableScissor(listX + 1, LIST_TOP + 1, listX + listWidth - 1, listBottom - 1)

        val themes = ThemeManager.themes
        var yPos = LIST_TOP + 2 - scrollOffset

        for ((index, theme) in themes.withIndex()) {
            if (yPos + THEME_ROW_HEIGHT > LIST_TOP && yPos < listBottom) {
                renderThemeRow(context, theme, index, listX + 2, yPos, listWidth - 4, mouseX, mouseY)
            }
            yPos += THEME_ROW_HEIGHT
        }

        if (themes.isEmpty()) {
            val noThemes = "No themes available"
            drawText(context, noThemes, listX + listWidth / 2f - getTextWidth(noThemes) / 2, LIST_TOP + listHeight / 2f - 10, COLOR_GRAY)
            val hint = "Place themes in: themes/"
            drawText(context, hint, listX + listWidth / 2f - getTextWidth(hint) / 2, LIST_TOP + listHeight / 2f + 5, COLOR_GRAY)
        }

        context.disableScissor()

        // Scroll bar
        if (themes.size * THEME_ROW_HEIGHT > listHeight) {
            val maxScroll = themes.size * THEME_ROW_HEIGHT - listHeight
            val scrollBarHeight = (listHeight.toFloat() / (themes.size * THEME_ROW_HEIGHT) * listHeight).toInt().coerceAtLeast(20)
            val scrollBarY = LIST_TOP + (scrollOffset.toFloat() / maxScroll * (listHeight - scrollBarHeight)).toInt()
            context.fill(listX + listWidth - 5, scrollBarY, listX + listWidth - 2, scrollBarY + scrollBarHeight, ARGB.color(180, 130, 100, 150))
        }
    }

    private fun renderThemeRow(context: GuiGraphics, theme: Theme, index: Int, x: Int, y: Int, w: Int, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + THEME_ROW_HEIGHT
        val isSelected = index == selectedIndex
        val isActive = theme == ThemeManager.theme

        // Row background
        val bgColor = when {
            isActive && isSelected -> ARGB.color(200, 80, 100, 80)
            isActive -> ARGB.color(150, 60, 80, 60)
            isSelected -> ARGB.color(200, 90, 70, 110)
            isHovered -> ARGB.color(150, 70, 60, 90)
            else -> ARGB.color(100, 50, 45, 70)
        }
        context.fill(x, y, x + w, y + THEME_ROW_HEIGHT - 2, bgColor)

        val metadata = theme.metadata

        // Theme name
        val nameColor = when {
            isActive -> COLOR_GREEN
            isSelected -> COLOR_WHITE
            isHovered -> COLOR_WHITE
            else -> COLOR_GRAY
        }
        drawText(context, metadata.name, x + 8f, y + 3f, nameColor)

        // Origin badge
        val originText = when (theme.origin) {
            Theme.Origin.RESOURCE -> "[Built-in]"
            Theme.Origin.LOCAL -> "[Local]"
            Theme.Origin.MARKETPLACE -> "[Marketplace]"
            else -> "[Unknown]"
        }
        val originColor = when (theme.origin) {
            Theme.Origin.RESOURCE -> COLOR_CYAN
            Theme.Origin.LOCAL -> COLOR_YELLOW
            Theme.Origin.MARKETPLACE -> COLOR_GOLD
            else -> COLOR_GRAY
        }
        drawText(context, originText, x + w - getTextWidth(originText) - 8, y + 3f, originColor)

        // Author
        val authorText = "by ${metadata.authors.joinToString(", ")}"
        drawText(context, authorText, x + 8f, y + 14f, COLOR_GRAY)

        // Version
        val versionText = "v${metadata.version}"
        drawText(context, versionText, x + 8f, y + 25f, COLOR_GRAY)

        // Active indicator
        if (isActive) {
            drawText(context, "[ACTIVE]", x + w - getTextWidth("[ACTIVE]") - 8, y + 25f, COLOR_GREEN)
        }

        // Theme ID (for debugging/identification)
        val idText = "ID: ${metadata.id}"
        drawText(context, idText, x + w / 2f - getTextWidth(idText) / 2, y + 14f, COLOR_GRAY)
    }

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

    override fun mouseClicked(click: net.minecraft.client.input.MouseButtonEvent, doubled: Boolean): Boolean {
        val mouseX = click.x.toInt()
        val mouseY = click.y.toInt()
        val listX = LIST_PADDING
        val listWidth = width - LIST_PADDING * 2
        val listHeight = height - LIST_TOP - 70
        val listBottom = LIST_TOP + listHeight

        // Check if clicked in theme list
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= LIST_TOP && mouseY < listBottom) {
            val themes = ThemeManager.themes
            var yPos = LIST_TOP + 2 - scrollOffset

            for ((index, _) in themes.withIndex()) {
                if (mouseY >= yPos && mouseY < yPos + THEME_ROW_HEIGHT) {
                    selectedIndex = index

                    // Double click to select
                    if (doubled) {
                        ThemeManager.theme = themes[index]
                        statusMessage = "Theme selected: ${themes[index].metadata.name}"
                        statusColor = COLOR_GREEN
                    }
                    return true
                }
                yPos += THEME_ROW_HEIGHT
            }
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val listHeight = height - LIST_TOP - 70
        val totalHeight = ThemeManager.themes.size * THEME_ROW_HEIGHT

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
