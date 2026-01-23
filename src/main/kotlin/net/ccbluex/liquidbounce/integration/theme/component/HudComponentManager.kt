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

package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ComponentsUpdateEvent
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.backends.minecraftgui.MinecraftGuiBrowserBackend
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.ArrayListComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.WatermarkComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.minimap.MinimapHudComponent
import net.ccbluex.liquidbounce.utils.render.Alignment

object HudComponentManager {

    // Default native components always available
    private val defaultNativeComponents = listOf(MinimapHudComponent)

    // Additional components for when browser isn't available (Android)
    private val fallbackNativeComponents: List<HudComponent> by lazy {
        if (BrowserBackendManager.browserBackend is MinecraftGuiBrowserBackend) {
            listOf(
                ArrayListComponent("ArrayList", true, Alignment.TOP_RIGHT, emptyArray()),
                WatermarkComponent("Watermark", true, Alignment.TOP_LEFT, emptyArray())
            )
        } else {
            emptyList()
        }
    }

    val nativeComponents: List<HudComponent>
        get() = defaultNativeComponents + fallbackNativeComponents

    val components: List<HudComponent>
        get() = nativeComponents + ThemeManager.theme.components

    @JvmStatic
    fun isTweakEnabled(tweak: HudComponentTweak) = ModuleHud.running && !HideAppearance.isHidingNow &&
        components.any { component ->
            component.enabled && component.tweaks.contains(tweak)
        }

    @JvmStatic
    fun getComponentWithTweak(tweak: HudComponentTweak): HudComponent? {
        if (!ModuleHud.running || HideAppearance.isHidingNow) {
            return null
        }

        return components.find { component ->
            component.enabled && component.tweaks.contains(tweak)
        }
    }

    fun getComponents(id: String?): List<HudComponent> {
        if (id == null) {
            return components
        }

        val theme = ThemeManager.themes.find { it.metadata.id == id } ?: return emptyList()
        return theme.components
    }

    fun updateComponents() {
        // Might be necessary later on.
        // EventManager.callEvent(ComponentsUpdate(null, components))
        EventManager.callEvent(ComponentsUpdateEvent(ThemeManager.theme.metadata.id, ThemeManager.theme.components))
    }

}
