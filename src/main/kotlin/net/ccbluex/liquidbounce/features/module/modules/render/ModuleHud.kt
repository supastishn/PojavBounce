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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isDestructed
import net.ccbluex.liquidbounce.features.misc.HideAppearance.isHidingNow
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.minimap.MinimapHudComponent
import net.ccbluex.liquidbounce.utils.client.inGame
import net.minecraft.util.ARGB

/**
 * Module HUD
 *
 * Native Minecraft HUD overlay that displays:
 * - LiquidBounce watermark in top left
 * - List of enabled modules in top right
 */
object ModuleHud : ClientModule("HUD", Category.RENDER, state = false, hide = true) {

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

    val isBlurEffectActive
        get() = Blur.enabled && !(mc.options.hideGui && mc.screen == null)

    val themes = tree(Configurable("Themes"))

    val components = tree(Configurable("AdditionalComponents")).apply {
        tree(MinimapHudComponent)
    }

    fun updateThemes() {
        themes.inner.clear()
        for (theme in ThemeManager.themes) {
            themes.tree(theme.settings)
        }
        themes.initConfigurable()
        themes.walkKeyPath()
    }

    @Suppress("unused")
    private val overlayRenderHandler = handler<OverlayRenderEvent> { event ->
        if (!running || !visible || mc.options.hideGui) {
            return@handler
        }

        val context = event.context
        val font = mc.font
        val screenWidth = context.guiWidth()

        // Draw watermark in top left
        if (showWatermark) {
            val watermark = "${LiquidBounce.CLIENT_NAME} v${LiquidBounce.clientVersion}"
            context.drawString(font, watermark, 4, 4, 0x00FFFF, true)
        }

        // Draw enabled modules list in top right
        if (showArrayList) {
            val enabledModules = ModuleManager
                .filter { it.enabled && !it.hide }
                .sortedByDescending { font.width(it.name) }

            var yOffset = 4
            for (module in enabledModules) {
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

                // Module name
                context.drawString(font, module.name, xPos, yOffset, 0x00FF00, true)
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
