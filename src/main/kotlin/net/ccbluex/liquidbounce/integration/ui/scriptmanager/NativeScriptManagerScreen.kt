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
package net.ccbluex.liquidbounce.integration.ui.scriptmanager

import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.script.PolyglotScript
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.util.ARGB
import java.io.File

/**
 * Native Script Manager screen with Inter font support
 * Features:
 * - List of loaded scripts
 * - Reload all scripts
 * - Enable/disable individual scripts
 * - Shows script language and file info
 */
class NativeScriptManagerScreen(private val parent: Screen?) : Screen("Script Manager".asPlainText()) {

    private val fontRenderer: FontRenderer get() = FontManager.FONT_RENDERER
    private val fontScale = 0.22f

    private var scrollOffset = 0
    private var selectedIndex = -1
    private var statusMessage = ""
    private var statusColor = COLOR_WHITE

    companion object {
        private const val SCRIPT_ROW_HEIGHT = 32
        private const val LIST_TOP = 50
        private const val LIST_PADDING = 20

        private val COLOR_WHITE = Color4b(255, 255, 255, 255)
        private val COLOR_GREEN = Color4b(0, 255, 0, 255)
        private val COLOR_RED = Color4b(255, 102, 102, 255)
        private val COLOR_CYAN = Color4b(0, 255, 255, 255)
        private val COLOR_GRAY = Color4b(170, 170, 170, 255)
        private val COLOR_YELLOW = Color4b(255, 204, 102, 255)
        private val COLOR_ORANGE = Color4b(255, 165, 0, 255)
        private val COLOR_PURPLE = Color4b(200, 150, 255, 255)
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

        // Reload all scripts button
        addRenderableWidget(
            Button.builder("Reload All".asPlainText()) {
                try {
                    ScriptManager.reload()
                    statusMessage = "Scripts reloaded successfully"
                    statusColor = COLOR_GREEN
                } catch (e: Exception) {
                    statusMessage = "Failed to reload: ${e.message}"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Enable button
        addRenderableWidget(
            Button.builder("Enable".asPlainText()) {
                val scripts = ScriptManager.scripts.toList()
                if (selectedIndex >= 0 && selectedIndex < scripts.size) {
                    val script = scripts[selectedIndex]
                    try {
                        script.enable()
                        statusMessage = "Script enabled"
                        statusColor = COLOR_GREEN
                    } catch (e: Exception) {
                        statusMessage = "Failed: ${e.message}"
                        statusColor = COLOR_RED
                    }
                } else {
                    statusMessage = "Select a script first!"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Disable button
        addRenderableWidget(
            Button.builder("Disable".asPlainText()) {
                val scripts = ScriptManager.scripts.toList()
                if (selectedIndex >= 0 && selectedIndex < scripts.size) {
                    val script = scripts[selectedIndex]
                    try {
                        script.disable()
                        statusMessage = "Script disabled"
                        statusColor = COLOR_YELLOW
                    } catch (e: Exception) {
                        statusMessage = "Failed: ${e.message}"
                        statusColor = COLOR_RED
                    }
                } else {
                    statusMessage = "Select a script first!"
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
        context.fill(0, 0, width, height, ARGB.color(200, 25, 20, 35))

        // Title
        drawText(context, "Script Manager", width / 2f - getTextWidth("Script Manager") / 2, 10f, COLOR_CYAN)

        // Script count
        val countText = "Loaded Scripts: ${ScriptManager.scripts.size}"
        drawText(context, countText, width / 2f - getTextWidth(countText) / 2, 25f, COLOR_GRAY)

        // Scripts directory info
        val dirText = "Directory: ${ScriptManager.root.absolutePath}"
        drawText(context, dirText, width / 2f - getTextWidth(dirText) / 2, 37f, COLOR_GRAY)

        // Script list
        renderScriptList(context, mouseX, mouseY)

        // Status message
        if (statusMessage.isNotEmpty()) {
            drawText(context, statusMessage, width / 2f - getTextWidth(statusMessage) / 2, height - 55f, statusColor)
        }

        super.render(context, mouseX, mouseY, delta)
    }

    private fun renderScriptList(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val listX = LIST_PADDING
        val listWidth = width - LIST_PADDING * 2
        val listHeight = height - LIST_TOP - 70
        val listBottom = LIST_TOP + listHeight

        // List background
        context.fill(listX, LIST_TOP, listX + listWidth, listBottom, ARGB.color(150, 35, 30, 50))

        // Border
        context.fill(listX, LIST_TOP, listX + listWidth, LIST_TOP + 1, ARGB.color(255, 70, 60, 90))
        context.fill(listX, listBottom - 1, listX + listWidth, listBottom, ARGB.color(255, 70, 60, 90))
        context.fill(listX, LIST_TOP, listX + 1, listBottom, ARGB.color(255, 70, 60, 90))
        context.fill(listX + listWidth - 1, LIST_TOP, listX + listWidth, listBottom, ARGB.color(255, 70, 60, 90))

        // Enable scissor for clipping
        context.enableScissor(listX + 1, LIST_TOP + 1, listX + listWidth - 1, listBottom - 1)

        val scripts = ScriptManager.scripts.toList()
        var yPos = LIST_TOP + 2 - scrollOffset

        for ((index, script) in scripts.withIndex()) {
            if (yPos + SCRIPT_ROW_HEIGHT > LIST_TOP && yPos < listBottom) {
                renderScriptRow(context, script, index, listX + 2, yPos, listWidth - 4, mouseX, mouseY)
            }
            yPos += SCRIPT_ROW_HEIGHT
        }

        if (scripts.isEmpty()) {
            val noScripts = "No scripts loaded"
            drawText(context, noScripts, listX + listWidth / 2f - getTextWidth(noScripts) / 2, LIST_TOP + listHeight / 2f - 10, COLOR_GRAY)
            val hint = "Place scripts in: ${ScriptManager.root.name}/"
            drawText(context, hint, listX + listWidth / 2f - getTextWidth(hint) / 2, LIST_TOP + listHeight / 2f + 5, COLOR_GRAY)
        }

        context.disableScissor()

        // Scroll bar
        if (scripts.size * SCRIPT_ROW_HEIGHT > listHeight) {
            val maxScroll = scripts.size * SCRIPT_ROW_HEIGHT - listHeight
            val scrollBarHeight = (listHeight.toFloat() / (scripts.size * SCRIPT_ROW_HEIGHT) * listHeight).toInt().coerceAtLeast(20)
            val scrollBarY = LIST_TOP + (scrollOffset.toFloat() / maxScroll * (listHeight - scrollBarHeight)).toInt()
            context.fill(listX + listWidth - 5, scrollBarY, listX + listWidth - 2, scrollBarY + scrollBarHeight, ARGB.color(180, 120, 100, 140))
        }
    }

    private fun renderScriptRow(context: GuiGraphics, script: PolyglotScript, index: Int, x: Int, y: Int, w: Int, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + SCRIPT_ROW_HEIGHT
        val isSelected = index == selectedIndex
        // Use scriptName initialization as a proxy for whether script is loaded
        val isLoaded = runCatching { script.scriptName }.isSuccess

        // Row background
        val bgColor = when {
            isLoaded && isSelected -> ARGB.color(200, 60, 100, 80)
            isLoaded -> ARGB.color(150, 50, 80, 60)
            isSelected -> ARGB.color(200, 80, 70, 100)
            isHovered -> ARGB.color(150, 60, 55, 80)
            else -> ARGB.color(100, 45, 40, 65)
        }
        context.fill(x, y, x + w, y + SCRIPT_ROW_HEIGHT - 2, bgColor)

        // Script name (use public scriptName if available, else file name)
        val displayName = runCatching { script.scriptName }.getOrElse { script.file.nameWithoutExtension }
        val nameColor = when {
            isLoaded -> COLOR_GREEN
            isSelected -> COLOR_WHITE
            isHovered -> COLOR_WHITE
            else -> COLOR_GRAY
        }
        drawText(context, displayName, x + 8f, y + 3f, nameColor)

        // Language badge
        val langColor = when (script.language.lowercase()) {
            "js", "javascript" -> COLOR_YELLOW
            "python", "py" -> COLOR_CYAN
            "ruby", "rb" -> COLOR_RED
            else -> COLOR_PURPLE
        }
        drawText(context, "[${script.language}]", x + w - getTextWidth("[${script.language}]") - 8, y + 3f, langColor)

        // Script info (version and authors if available)
        val versionText = runCatching { "v${script.scriptVersion}" }.getOrElse { "Unknown" }
        val authorsText = runCatching { script.scriptAuthors.joinToString(", ") }.getOrElse { "" }
        val infoText = if (authorsText.isNotEmpty()) "$versionText by $authorsText" else versionText
        drawText(context, infoText, x + 8f, y + 16f, COLOR_GRAY)

        // File path
        val pathText = script.file.parentFile?.name ?: ""
        if (pathText.isNotEmpty()) {
            drawText(context, "/$pathText/", x + w - getTextWidth("/$pathText/") - 8, y + 16f, COLOR_GRAY)
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
        val listX = LIST_PADDING
        val listWidth = width - LIST_PADDING * 2
        val listHeight = height - LIST_TOP - 70
        val listBottom = LIST_TOP + listHeight

        // Check if clicked in script list
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= LIST_TOP && mouseY < listBottom) {
            val scripts = ScriptManager.scripts.toList()
            var yPos = LIST_TOP + 2 - scrollOffset

            for ((index, _) in scripts.withIndex()) {
                if (mouseY >= yPos && mouseY < yPos + SCRIPT_ROW_HEIGHT) {
                    selectedIndex = index
                    return true
                }
                yPos += SCRIPT_ROW_HEIGHT
            }
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val listHeight = height - LIST_TOP - 70
        val totalHeight = ScriptManager.scripts.size * SCRIPT_ROW_HEIGHT

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
