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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.world.entity.LivingEntity
import kotlin.random.Random

/**
 * Human-like targeting behavior to make KillAura less obvious.
 *
 * Features:
 * - Reaction time: Delay before first attack on a new target (visual processing + muscle response)
 * - Micro-corrections: Small random aim adjustments like humans naturally make
 */
internal object KillAuraHumanTargeting : ToggleableConfigurable(ModuleKillAura, "HumanTargeting", false) {

    // Reaction time simulation - delay before first attack on new target
    private val reactionTime by intRange("ReactionTime", 150..350, 0..1000, "ms")

    // Micro-corrections - small random aim adjustments
    private val microCorrections by floatRange("MicroCorrections", 0.1f..0.5f, 0f..2f, "°")

    // How often micro-corrections occur (% chance per tick)
    private val correctionChance by int("CorrectionChance", 30, 0..100, "%")

    // Internal state tracking
    private var lastTargetId: Int? = null
    private var targetAcquiredTime: Long = 0L
    private var currentReactionDelay: Int = reactionTime.random()

    /**
     * Check if we should delay attacking a target due to reaction time.
     * Returns true if we should wait (not attack yet).
     */
    fun shouldDelayAttack(target: LivingEntity): Boolean {
        if (!enabled) return false

        val targetId = target.id
        val now = System.currentTimeMillis()

        // New target detected
        if (targetId != lastTargetId) {
            lastTargetId = targetId
            targetAcquiredTime = now
            currentReactionDelay = reactionTime.random()

            debugParameter(ModuleKillAura, "HT_NewTarget") { target.scoreboardName }
            debugParameter(ModuleKillAura, "HT_ReactionDelay") { currentReactionDelay }
        }

        val timeSinceAcquired = now - targetAcquiredTime
        val shouldDelay = timeSinceAcquired < currentReactionDelay

        debugParameter(ModuleKillAura, "HT_TimeSinceAcquired") { timeSinceAcquired }
        debugParameter(ModuleKillAura, "HT_ShouldDelayAttack") { shouldDelay }

        return shouldDelay
    }

    /**
     * Apply micro-corrections to a rotation to make it look more human.
     * Humans constantly make tiny aim adjustments - perfectly still aim is unnatural.
     */
    fun applyMicroCorrections(rotation: Rotation): Rotation {
        if (!enabled) return rotation

        // Only apply corrections based on chance
        if (Random.nextInt(100) >= correctionChance) {
            return rotation
        }

        val correctionAmount = microCorrections.random()

        // Random direction for yaw correction
        val yawCorrection = (Random.nextFloat() - 0.5f) * 2f * correctionAmount
        // Random direction for pitch correction (usually smaller)
        val pitchCorrection = (Random.nextFloat() - 0.5f) * 2f * correctionAmount * 0.7f

        debugParameter(ModuleKillAura, "HT_YawCorrection") { yawCorrection }
        debugParameter(ModuleKillAura, "HT_PitchCorrection") { pitchCorrection }

        return Rotation(
            rotation.yaw + yawCorrection,
            (rotation.pitch + pitchCorrection).coerceIn(-90f, 90f)
        )
    }

    /**
     * Reset state when KillAura is disabled
     */
    fun reset() {
        lastTargetId = null
        targetAcquiredTime = 0L
        currentReactionDelay = reactionTime.random()
    }
}
