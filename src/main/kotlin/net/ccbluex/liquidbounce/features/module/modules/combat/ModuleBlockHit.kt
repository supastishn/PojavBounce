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

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.utils.facingEnemy
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.world.InteractionHand

/**
 * BlockHit module
 *
 * Automatically blocks when an enemy is looking at you and within range.
 * Useful for defensive play - blocks incoming attacks reactively.
 */
object ModuleBlockHit : ClientModule("BlockHit", ModuleCategories.COMBAT) {

    private val range by floatRange("Range", 3f..4f, 1f..8f)
    private val wallsRange by float("WallsRange", 0f, 0f..8f)
    private val blockDuration by intRange("BlockDuration", 1..3, 1..20, "ticks")
    private val cooldown by int("Cooldown", 2, 0..20, "ticks")

    private var blockTicks = 0
    private var cooldownTicks = 0
    private var isBlocking = false

    @Suppress("unused")
    private val tickHandler = tickHandler {
        // Don't block while attacking
        if (mc.options.keyAttack.isPressedOnAny) {
            stopBlocking()
            return@tickHandler
        }

        if (cooldownTicks > 0) {
            cooldownTicks--
            return@tickHandler
        }

        val blockHand = getBlockableHand()
        if (blockHand == null) {
            stopBlocking()
            return@tickHandler
        }

        val enemies = world.getEntitiesBoxInRange(
            player.eyePosition,
            range.endInclusive.toDouble()
        ) {
            it != player && it.shouldBeAttacked()
        }

        val isInDanger = enemies.any { enemy ->
            val distance = player.distanceTo(enemy).toDouble()
            distance in range.start.toDouble()..range.endInclusive.toDouble() &&
                facingEnemy(
                    fromEntity = enemy,
                    toEntity = player,
                    rotation = enemy.rotation,
                    range = range.endInclusive.toDouble(),
                    wallsRange = wallsRange.toDouble()
                )
        }

        if (isInDanger) {
            if (!isBlocking) {
                startBlocking(blockHand)
                blockTicks = blockDuration.random()
            }
        }

        if (isBlocking) {
            blockTicks--
            if (blockTicks <= 0) {
                stopBlocking()
                cooldownTicks = cooldown
            }
        }
    }

    private fun getBlockableHand(): InteractionHand? {
        // Only support sword blocking (1.8 style)
        return if (player.mainHandItem.isSword) InteractionHand.MAIN_HAND else null
    }

    private fun startBlocking(hand: InteractionHand) {
        if (!player.mainHandItem.isSword) {
            return
        }

        // Use the item to start blocking (requires SwordBlock module for 1.9+ servers)
        val result = interaction.useItem(player, hand)
        if (result.consumesAction()) {
            player.swing(hand)
            isBlocking = true
        }
    }

    private fun stopBlocking() {
        if (isBlocking && player.isUsingItem) {
            interaction.releaseUsingItem(player)
        }
        isBlocking = false
    }

    override fun onDisabled() {
        stopBlocking()
        blockTicks = 0
        cooldownTicks = 0
    }
}
