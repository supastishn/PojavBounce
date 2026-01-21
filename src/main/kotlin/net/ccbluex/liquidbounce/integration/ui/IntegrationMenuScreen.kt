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

package net.ccbluex.liquidbounce.integration.ui

import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB

class IntegrationMenuScreen : Screen(Component.literal("Integration Menu")) {

    private val fontRenderer: FontRenderer get() = FontManager.FONT_RENDERER
    private val fontScale = 0.22f

    companion object {
        private val COLOR_WHITE = Color4b(255, 255, 255, 255)
        private val COLOR_CYAN = Color4b(0, 255, 255, 255)
    }

    override fun init() {
        super.init()
        // Build buttons only for native screen implementations (skip browser-based screens)
        val nativeScreenTypes = listOf(
            VirtualScreenType.CLICK_GUI,
            VirtualScreenType.ALT_MANAGER,
            VirtualScreenType.PROXY_MANAGER,
            VirtualScreenType.SCRIPT_MANAGER,
            VirtualScreenType.THEME_MANAGER,
            VirtualScreenType.CUSTOMIZE
        )

        var y = 50
        for (type in nativeScreenTypes) {
            val name = type.routeName.replaceFirstChar { it.uppercase() }
            val button = Button.builder(Component.literal(name)) { onOpen(type) }
                .bounds(20, y, width - 40, 20)
                .build()
            this.addRenderableWidget(button)
            y += 24
        }
    }

    private fun onOpen(type: VirtualScreenType) {
        // Open native screen if available, else open the theme browser
        when (type) {
            VirtualScreenType.CLICK_GUI ->
                mc.setScreen(net.ccbluex.liquidbounce.integration.ui.clickgui.NativeClickGuiScreen())
            VirtualScreenType.ALT_MANAGER ->
                mc.setScreen(net.ccbluex.liquidbounce.integration.ui.altmanager.NativeAltManagerScreen(this))
            VirtualScreenType.PROXY_MANAGER ->
                mc.setScreen(net.ccbluex.liquidbounce.integration.ui.proxymanager.NativeProxyManagerScreen(this))
            VirtualScreenType.SCRIPT_MANAGER ->
                mc.setScreen(net.ccbluex.liquidbounce.integration.ui.scriptmanager.NativeScriptManagerScreen(this))
            VirtualScreenType.THEME_MANAGER ->
                mc.setScreen(net.ccbluex.liquidbounce.integration.ui.thememanager.NativeThemeManagerScreen(this))
            VirtualScreenType.CUSTOMIZE ->
                mc.setScreen(net.ccbluex.liquidbounce.integration.ui.customize.NativeCustomizeScreen(this))
            else ->
                mc.setScreen(net.ccbluex.liquidbounce.integration.VirtualDisplayScreen(type))
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // Background
        context.fill(0, 0, width, height, ARGB.color(200, 30, 25, 40))

        // Title with Inter font
        val titleText = "Integration Menu"
        drawText(context, titleText, width / 2f - getTextWidth(titleText) / 2, 15f, COLOR_CYAN)

        super.render(context, mouseX, mouseY, delta)
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
}
