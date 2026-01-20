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

package net.ccbluex.liquidbounce.integration.theme.component

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.utils.render.Alignment

/**
 * Type alias for component factory function
 */
private typealias ComponentFactory =
    (String, Boolean, Alignment, Array<HudComponentTweak>, Array<JsonObject>) -> HudComponent

/**
 * Registry for native component factories loaded from themes component JSON.
 */
object NativeComponentRegistry {

    private val factories: MutableMap<String, ComponentFactory> = mutableMapOf()

    fun register(
        name: String,
        create: ComponentFactory
    ) {
        factories[name] = create
    }

    fun create(
        name: String,
        enabled: Boolean,
        alignment: Alignment,
        tweaks: Array<HudComponentTweak>,
        values: Array<JsonObject>
    ): HudComponent? {
        val factory = factories[name] ?: return null
        return factory(name, enabled, alignment, tweaks, values)
    }

}
