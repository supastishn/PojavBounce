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
package net.ccbluex.liquidbounce.integration.ui.proxymanager

import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.util.ARGB

/**
 * Native Proxy Manager screen with Inter font support
 * Features:
 * - List of saved proxies
 * - Add proxy (SOCKS5/HTTP)
 * - Select/enable proxy
 * - Remove proxy
 * - Shows current proxy status
 */
class NativeProxyManagerScreen(private val parent: Screen?) : Screen("Proxy Manager".asPlainText()) {

    private val fontRenderer: FontRenderer get() = FontManager.FONT_RENDERER
    private val fontScale = 0.22f

    private var scrollOffset = 0
    private var selectedIndex = -1
    private var proxyField: EditBox? = null
    private var statusMessage = ""
    private var statusColor = COLOR_WHITE

    companion object {
        private const val PROXY_ROW_HEIGHT = 28
        private const val LIST_TOP = 50
        private const val LIST_PADDING = 20

        private val COLOR_WHITE = Color4b(255, 255, 255, 255)
        private val COLOR_GREEN = Color4b(0, 255, 0, 255)
        private val COLOR_RED = Color4b(255, 102, 102, 255)
        private val COLOR_CYAN = Color4b(0, 255, 255, 255)
        private val COLOR_GRAY = Color4b(170, 170, 170, 255)
        private val COLOR_YELLOW = Color4b(255, 204, 102, 255)
        private val COLOR_PURPLE = Color4b(200, 150, 255, 255)
    }

    override fun init() {
        super.init()

        val buttonWidth = 80
        val buttonHeight = 20
        val buttonY = height - 30
        val spacing = 5

        // Proxy field for adding new proxies
        proxyField = EditBox(font, width / 2 - 150, height - 60, 300, 20, "Proxy".asPlainText())
        proxyField!!.setMaxLength(200)
        proxyField!!.setHint("host:port or user:pass@host:port".asPlainText())
        addWidget(proxyField!!)

        // Calculate button positions
        val totalWidth = buttonWidth * 5 + spacing * 4
        var xPos = width / 2 - totalWidth / 2

        // Add proxy button
        addRenderableWidget(
            Button.builder("Add".asPlainText()) {
                val proxyString = proxyField?.value ?: ""
                if (proxyString.isNotEmpty()) {
                    try {
                        val proxy = Proxy.parse(proxyString)
                        ProxyManager.validateProxy(proxy)
                        proxyField?.value = ""
                        statusMessage = "Adding proxy (validating)..."
                        statusColor = COLOR_YELLOW
                    } catch (e: Exception) {
                        statusMessage = "Invalid proxy format!"
                        statusColor = COLOR_RED
                    }
                } else {
                    statusMessage = "Enter a proxy first!"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Enable button
        addRenderableWidget(
            Button.builder("Enable".asPlainText()) {
                if (selectedIndex >= 0 && selectedIndex < ProxyManager.proxies.size) {
                    ProxyManager.proxy = ProxyManager.proxies[selectedIndex]
                    statusMessage = "Proxy enabled"
                    statusColor = COLOR_GREEN
                } else {
                    statusMessage = "Select a proxy first!"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Disable button
        addRenderableWidget(
            Button.builder("Disable".asPlainText()) {
                ProxyManager.proxy = Proxy.NONE
                statusMessage = "Proxy disabled"
                statusColor = COLOR_YELLOW
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Remove button
        addRenderableWidget(
            Button.builder("Remove".asPlainText()) {
                if (selectedIndex >= 0 && selectedIndex < ProxyManager.proxies.size) {
                    val proxy = ProxyManager.proxies[selectedIndex]
                    ProxyManager.proxies.remove(proxy)
                    if (ProxyManager.proxy == proxy) {
                        ProxyManager.proxy = Proxy.NONE
                    }
                    statusMessage = "Proxy removed"
                    statusColor = COLOR_GREEN
                    selectedIndex = -1
                } else {
                    statusMessage = "Select a proxy first!"
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
        context.fill(0, 0, width, height, ARGB.color(200, 20, 30, 20))

        // Title
        drawText(context, "Proxy Manager", width / 2f - getTextWidth("Proxy Manager") / 2, 10f, COLOR_CYAN)

        // Current proxy status
        val currentProxy = ProxyManager.currentProxy
        val statusText = if (currentProxy != null) {
            "Active: ${currentProxy.host}:${currentProxy.port} (${currentProxy.type})"
        } else {
            "Status: No proxy active"
        }
        val statusTextColor = if (currentProxy != null) COLOR_GREEN else COLOR_GRAY
        drawText(context, statusText, width / 2f - getTextWidth(statusText) / 2, 25f, statusTextColor)

        // Proxy list
        renderProxyList(context, mouseX, mouseY)

        // Status message
        if (statusMessage.isNotEmpty()) {
            drawText(context, statusMessage, width / 2f - getTextWidth(statusMessage) / 2, height - 85f, statusColor)
        }

        // Render proxy field
        proxyField?.render(context, mouseX, mouseY, delta)

        super.render(context, mouseX, mouseY, delta)
    }

    private fun renderProxyList(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val listX = LIST_PADDING
        val listWidth = width - LIST_PADDING * 2
        val listHeight = height - LIST_TOP - 100
        val listBottom = LIST_TOP + listHeight

        // List background
        context.fill(listX, LIST_TOP, listX + listWidth, listBottom, ARGB.color(150, 30, 40, 30))

        // Border
        context.fill(listX, LIST_TOP, listX + listWidth, LIST_TOP + 1, ARGB.color(255, 60, 80, 60))
        context.fill(listX, listBottom - 1, listX + listWidth, listBottom, ARGB.color(255, 60, 80, 60))
        context.fill(listX, LIST_TOP, listX + 1, listBottom, ARGB.color(255, 60, 80, 60))
        context.fill(listX + listWidth - 1, LIST_TOP, listX + listWidth, listBottom, ARGB.color(255, 60, 80, 60))

        // Enable scissor for clipping
        context.enableScissor(listX + 1, LIST_TOP + 1, listX + listWidth - 1, listBottom - 1)

        val proxies = ProxyManager.proxies
        var yPos = LIST_TOP + 2 - scrollOffset

        for ((index, proxy) in proxies.withIndex()) {
            if (yPos + PROXY_ROW_HEIGHT > LIST_TOP && yPos < listBottom) {
                renderProxyRow(context, proxy, index, listX + 2, yPos, listWidth - 4, mouseX, mouseY)
            }
            yPos += PROXY_ROW_HEIGHT
        }

        if (proxies.isEmpty()) {
            val noProxies = "No proxies added"
            drawText(context, noProxies, listX + listWidth / 2f - getTextWidth(noProxies) / 2, LIST_TOP + listHeight / 2f - 5, COLOR_GRAY)
        }

        context.disableScissor()

        // Scroll bar
        if (proxies.size * PROXY_ROW_HEIGHT > listHeight) {
            val maxScroll = proxies.size * PROXY_ROW_HEIGHT - listHeight
            val scrollBarHeight = (listHeight.toFloat() / (proxies.size * PROXY_ROW_HEIGHT) * listHeight).toInt().coerceAtLeast(20)
            val scrollBarY = LIST_TOP + (scrollOffset.toFloat() / maxScroll * (listHeight - scrollBarHeight)).toInt()
            context.fill(listX + listWidth - 5, scrollBarY, listX + listWidth - 2, scrollBarY + scrollBarHeight, ARGB.color(180, 100, 120, 100))
        }
    }

    private fun renderProxyRow(context: GuiGraphics, proxy: Proxy, index: Int, x: Int, y: Int, w: Int, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + PROXY_ROW_HEIGHT
        val isSelected = index == selectedIndex
        val isActive = proxy == ProxyManager.proxy

        // Row background
        val bgColor = when {
            isActive -> ARGB.color(200, 50, 100, 50)
            isSelected -> ARGB.color(200, 70, 100, 70)
            isHovered -> ARGB.color(150, 50, 70, 50)
            else -> ARGB.color(100, 40, 55, 40)
        }
        context.fill(x, y, x + w, y + PROXY_ROW_HEIGHT - 2, bgColor)

        // Proxy info
        val hostPort = "${proxy.host}:${proxy.port}"
        val proxyType = proxy.type?.name ?: "SOCKS5"

        val nameColor = when {
            isActive -> COLOR_GREEN
            isSelected -> COLOR_WHITE
            isHovered -> COLOR_WHITE
            else -> COLOR_GRAY
        }
        drawText(context, hostPort, x + 8f, y + 3f, nameColor)

        // Type badge
        val typeColor = when (proxy.type) {
            Proxy.Type.HTTP -> COLOR_YELLOW
            Proxy.Type.SOCKS5 -> COLOR_PURPLE
            else -> COLOR_GRAY
        }
        drawText(context, "[$proxyType]", x + w - getTextWidth("[$proxyType]") - 8, y + 3f, typeColor)

        // Credentials indicator
        if (proxy.credentials != null) {
            drawText(context, "Auth", x + 8f, y + 14f, COLOR_CYAN)
        }

        // IP info if available
        proxy.ipInfo?.let { ipInfo ->
            val location = "${ipInfo.country ?: "?"}"
            drawText(context, location, x + 50f, y + 14f, COLOR_GRAY)
        }

        // Active indicator
        if (isActive) {
            drawText(context, "[ACTIVE]", x + w - getTextWidth("[ACTIVE]") - 8, y + 14f, COLOR_GREEN)
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
        val listHeight = height - LIST_TOP - 100
        val listBottom = LIST_TOP + listHeight

        // Check if clicked in proxy list
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= LIST_TOP && mouseY < listBottom) {
            val proxies = ProxyManager.proxies
            var yPos = LIST_TOP + 2 - scrollOffset

            for ((index, _) in proxies.withIndex()) {
                if (mouseY >= yPos && mouseY < yPos + PROXY_ROW_HEIGHT) {
                    selectedIndex = index

                    // Double click to enable
                    if (doubled) {
                        ProxyManager.proxy = proxies[index]
                        statusMessage = "Proxy enabled"
                        statusColor = COLOR_GREEN
                    }
                    return true
                }
                yPos += PROXY_ROW_HEIGHT
            }
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val listHeight = height - LIST_TOP - 100
        val totalHeight = ProxyManager.proxies.size * PROXY_ROW_HEIGHT

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
