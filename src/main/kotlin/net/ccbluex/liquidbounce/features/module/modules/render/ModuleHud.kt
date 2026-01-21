/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isHidingNow
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.minimap.MinimapHudComponent
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.inGame
import net.minecraft.util.ARGB

/**
 * Module HUD
 *
 * Native Minecraft HUD overlay that displays:
 * - LiquidBounce watermark in top left (using Inter font)
 * - List of enabled modules in top right (using Inter font)
 */
object ModuleHud : ClientModule("HUD", ModuleCategories.RENDER, state = true, hide = true) {

    override val running
        get() = this.enabled && !isDestructed

    private val visible: Boolean
        get() = !isHidingNow && inGame

    override val baseKey: String
        get() = "liquidbounce.module.hud"

    init {
        tree(Blur)
    }

    object Blur : ToggleableConfigurable(ModuleHud, "Blur", enabled = false) {
        val alphaBlendRange by floatRange("AlphaBlendRange", 0.0F..0.75F, 0.0F..1.0F)
    }

    // Native HUD settings
    private val showWatermark by boolean("ShowWatermark", true)
    private val showArrayList by boolean("ShowArrayList", true)
    private val arrayListBackground by boolean("ArrayListBackground", true)
    private val useCustomFont by boolean("UseCustomFont", true)
    private val hudScale by float("Scale", 0.4f, 0.2f..1.0f)

    // Colors
    private val watermarkColor = Color4b(0, 255, 255, 255)  // Cyan
    private val arrayListColor = Color4b(0, 255, 0, 255)     // Green
    private val backgroundColor = Color4b(0, 0, 0, 120)      // Semi-transparent black

    val isBlurEffectActive
        get() = Blur.enabled && !(mc.options.hideGui && mc.screen == null)

    val themes = tree(Configurable("Themes"))

    val components = tree(Configurable("AdditionalComponents")).apply {
        tree(MinimapHudComponent)
    }

    fun updateThemes() {
        themes.inner.filterIsInstance<Configurable>().forEach {
            themes.drop(it)
        }
        for (theme in ThemeManager.themes) {
            themes.tree(theme.settings)
        }
        themes.initConfigurable()
        themes.walkKeyPath()
    }

    @Suppress("unused")
    private val overlayRenderHandler = handler<OverlayRenderEvent> { event ->
        // Only hide HUD when F1 is pressed (hideGui) AND no screen/menu is open.
        // This allows HUD to render when menus are open (e.g., pause menu via Esc).
        if (!running || !visible || (mc.options.hideGui && mc.screen == null)) {
            return@handler
        }

        val context = event.context
        val screenWidth = context.guiWidth()

        if (useCustomFont) {
            renderWithCustomFont(context, screenWidth)
        } else {
            renderWithMinecraftFont(context, screenWidth)
        }
    }

    private fun renderWithCustomFont(context: net.minecraft.client.gui.GuiGraphics, screenWidth: Int) {
        val fontRenderer = FontManager.FONT_RENDERER
        val scale = hudScale

        // Draw watermark in top left
        if (showWatermark) {
            val watermark = "${LiquidBounce.CLIENT_NAME} v${LiquidBounce.clientVersion}"
            val processedText = fontRenderer.process(watermark.asText(), watermarkColor)

            with(context) {
                fontRenderer.draw(processedText) {
                    x = 4f
                    y = 4f
                    this.scale = scale
                    shadow = true
                }
            }
        }

        // Draw enabled modules list in top right
        if (showArrayList) {
            val enabledModules = ModuleManager
                .filter { it.enabled && !it.hidden }
                .sortedByDescending { module ->
                    val text = fontRenderer.process(module.name.asText(), arrayListColor)
                    fontRenderer.getStringWidth(text, shadow = true)
                }

            var yOffset = 4f
            val lineHeight = fontRenderer.height * scale + 2

            for ((index, module) in enabledModules.withIndex()) {
                val processedText = fontRenderer.process(module.name.asText(), arrayListColor)
                val textWidth = fontRenderer.getStringWidth(processedText, shadow = true) * scale
                val xPos = screenWidth - textWidth - 4f

                // Background
                if (arrayListBackground) {
                    context.drawQuad(
                        xPos - 2f,
                        yOffset - 1f,
                        screenWidth.toFloat(),
                        yOffset + lineHeight - 1f,
                        fillColor = backgroundColor
                    )
                }

                // Rainbow-ish color based on index
                val hue = (index * 10f) / 360f
                val color = Color4b.ofHSB(hue, 1f, 1f)
                val coloredText = fontRenderer.process(module.name.asText(), color)

                // Module name
                with(context) {
                    fontRenderer.draw(coloredText) {
                        x = xPos
                        y = yOffset
                        this.scale = scale
                        shadow = true
                    }
                }

                yOffset += lineHeight
            }
        }
    }

    private fun renderWithMinecraftFont(context: net.minecraft.client.gui.GuiGraphics, screenWidth: Int) {
        val font = mc.font

        // Draw watermark in top left
        if (showWatermark) {
            val watermark = "${LiquidBounce.CLIENT_NAME} v${LiquidBounce.clientVersion}"
            context.drawString(font, watermark, 4, 4, 0xFF00FFFF.toInt(), true)
        }

        // Draw enabled modules list in top right
        if (showArrayList) {
            val enabledModules = ModuleManager
                .filter { it.enabled && !it.hidden }
                .sortedByDescending { font.width(it.name) }

            var yOffset = 4
            for ((index, module) in enabledModules.withIndex()) {
                val textWidth = font.width(module.name)
                val xPos = screenWidth - textWidth - 4

                // Background
                if (arrayListBackground) {
                    context.fill(
                        xPos - 2,
                        yOffset - 1,
                        screenWidth,
                        yOffset + font.lineHeight,
                        ARGB.color(120, 0, 0, 0)
                    )
                }

                // Rainbow-ish color based on index
                val hue = (index * 10f) / 360f
                val color = java.awt.Color.getHSBColor(hue, 1f, 1f)
                val argb = (0xFF shl 24) or (color.red shl 16) or (color.green shl 8) or color.blue

                // Module name
                context.drawString(font, module.name, xPos, yOffset, argb, true)
                yOffset += font.lineHeight + 1
            }
        }
    }

    fun reopen() {
        // No-op for native HUD
    }

    fun disableBlur() {
        Blur.enabled = false
    }

}
