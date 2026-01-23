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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY

/**
 * AttributeSwap module
 *
 * Visibly switches between two hotbar slots before attacks to exploit
 * Minecraft's attribute application delay, then immediately reverts back.
 *
 * The swap is VISIBLE so you can see it happening in real-time.
 */
object ModuleAttributeSwap : ClientModule("AttributeSwap", ModuleCategories.COMBAT) {

    // Slot configuration - choose exactly 2 slots to swap between
    private val mainSlot by int("MainSlot", 0, 0..8, "slot")
    private val swapSlot by int("SwapSlot", 1, 0..8, "slot")

    // Timing - how long to hold the swapped item before reverting
    private val holdDuration by int("HoldDuration", 0, 0..10, "ticks")

    @Suppress("unused")
    private val attackHandler = sequenceHandler<AttackEntityEvent>(
        priority = FIRST_PRIORITY
    ) { event ->
        if (!enabled) return@sequenceHandler

        val currentSlot = player.inventory.selectedSlot

        // Only swap if we're currently on the main slot
        if (currentSlot != mainSlot) return@sequenceHandler

        // Visibly switch to swap slot
        player.inventory.selectedSlot = swapSlot

        // Ensure the carried item packet is sent with new slot
        interaction.ensureHasSentCarriedItem()

        // Hold for the specified duration
        if (holdDuration > 0) {
            waitTicks(holdDuration)
        }

        // Switch back to main slot
        player.inventory.selectedSlot = mainSlot
        interaction.ensureHasSentCarriedItem()
    }
}
