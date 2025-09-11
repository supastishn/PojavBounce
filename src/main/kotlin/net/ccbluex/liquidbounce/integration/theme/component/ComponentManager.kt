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
 *
 *
 */

package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ComponentsUpdate
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.components.minimap.MinimapComponent

object ComponentManager {

    val nativeComponents = listOf(MinimapComponent)

    val components: List<Component>
        get() = nativeComponents + ThemeManager.activeTheme.components

    @JvmStatic
    fun isTweakEnabled(tweak: ComponentTweak) = ModuleHud.running && !HideAppearance.isHidingNow &&
        components.any { component ->
            component.enabled // Remove tweaks check for native GUI approach
        }

    @JvmStatic
    fun getComponentWithTweak(tweak: ComponentTweak): Component? {
        if (!ModuleHud.running || HideAppearance.isHidingNow) {
            return null
        }

        return components.find { component ->
            component.enabled // Remove tweaks check for native GUI approach
        }
    }

    fun getComponents(id: String?): List<Component> {
        if (id == null) {
            return components
        }

        val themeResult = ThemeManager.themes()
        if (themeResult.isFailure) {
            return emptyList()
        }
        val theme = themeResult.getOrNull()?.find { it.metadata.id == id } ?: return emptyList()
        return theme.components
    }

    fun updateComponents() {
        // Might be necessary later on.
        // EventManager.callEvent(ComponentsUpdate(null, components))
        EventManager.callEvent(ComponentsUpdate(ThemeManager.activeTheme.components))
    }

}
