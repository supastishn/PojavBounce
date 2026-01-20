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
package net.ccbluex.liquidbounce.integration.ui.hud

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager
import net.ccbluex.liquidbounce.integration.theme.component.components.NativeHudComponent
import net.ccbluex.liquidbounce.utils.client.mc

/**
 * Native HUD renderer - replaces browser-based HUD with pure Minecraft rendering
 * This renders the arraylist, watermark, and other HUD elements
 */
object NativeHudRenderer : EventListener {

    @Suppress("unused")
    private val margin = 4

    @Suppress("unused")
    private val moduleSpacing = 2

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        // Only hide HUD when F1 is pressed (hideGui) AND no screen/menu is open.
        // This allows HUD to render when menus are open (e.g., pause menu via Esc).
        if (mc.options.hideGui && mc.screen == null) {
            return@handler
        }

        val context = event.context

        // Render all native components. Web-based components are rendered separately by the browser.
        for (component in HudComponentManager.components) {
            if (component.enabled && component is NativeHudComponent) {
                component.render(context)
            }
        }
    }

}
