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

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.lastPos
import net.ccbluex.liquidbounce.utils.entity.ping
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * SmartHit module
 *
 * Intelligently times attacks for optimal combo potential and damage output.
 * Analyzes target distance, velocity, hurt time, and player position to determine
 * the best moment to strike.
 *
 * Credits to Raven versions, Augustus, and Vape for some of these ideas.
 */
@Suppress("MagicNumber")
object ModuleSmartHit : ClientModule("SmartHit", ModuleCategories.COMBAT) {

    // Attack timing settings
    private val attackableHurtTime by intRange("AttackableHurtTime", 0..0, 0..10)
    private val notAboveRange by float("NotAboveRange", 2.7f, 0f..8f, "blocks")
    private val notAbovePredRange by float("NotAbovePredictedRange", 2.8f, 0f..8f, "blocks")

    // Health thresholds for panic hitting
    private val notBelowOwnHealth by float("NotBelowOwnHealth", 5f, 0f..20f)
    private val notBelowEnemyHealth by float("NotBelowEnemyHealth", 5f, 0f..20f)

    // Edge detection
    private object EdgeDetection : ToggleableConfigurable(this, "EdgeDetection", false) {
        val edgeLimit by float("EdgeLimit", 1f, 0f..8f, "blocks")
    }

    // Prediction settings
    private val predictClientMovement by int("PredictClientMovement", 5, 0..10, "ticks")
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, 0f..2f)

    // Knockback simulation
    private object KnockbackSimulation : ToggleableConfigurable(this, "SimulateKnockback", true) {
        val horizontalKnockback by floatRange("HorizontalKnockback", 0.88f..1f, 0f..4f)
        val verticalKnockback by floatRange("VerticalKnockback", 0.4f..0.5f, 0f..2f)
    }

    // Debug output
    private val debug by boolean("Debug", false)

    init {
        tree(EdgeDetection)
        tree(KnockbackSimulation)
    }

    // State tracking
    private var simHurtTime = 0
    private var lastHitCrit = false
    private var hitOnTheWay = false

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        val target = event.entity

        lastHitCrit = player.fallDistance > 0

        hitOnTheWay = player.squaredBoxedDistanceTo(target) < 9f &&
            (target as? LivingEntity)?.hurtTime in attackableHurtTime
    }

    /**
     * Determines whether an attack should be executed on the target.
     *
     * This analyzes various factors including:
     * - Target hurt time and distance
     * - Player position and predicted movement
     * - Whether target can hit back
     * - Edge proximity for both players
     */
    fun shouldHit(target: Entity): Boolean {
        if (target !is LivingEntity) return true

        // Latency affects many calculations
        val playerPing = player.ping
        val targetPing = (target as? Player)?.ping ?: 0
        val combinedPing = playerPing + targetPing
        val combinedPingMult = combinedPing.toFloat() / 100f

        // Calculate distances from both perspectives
        val distance = sqrt(player.squaredBoxedDistanceTo(target))
        val targetDistance = sqrt(target.squaredBoxedDistanceTo(player))

        // Simulate player movement
        val simInput = SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(
            DirectionalInput(player.input)
        )
        val simPlayer = SimulatedPlayer.fromClientPlayer(simInput)
        simHurtTime = player.hurtTime

        repeat(predictClientMovement + 1) {
            simPlayer.tick()
            if (simHurtTime > 0) --simHurtTime
        }

        // Ground state checks
        val trueGround = player.onGround() && player.onGroundTicks > 1 && simPlayer.onGround

        // Critical hit potential
        val falling = player.fallDistance > 0 || simPlayer.fallDistance > 0

        val targetHittable = target.hurtTime in attackableHurtTime

        if (target.hurtTime <= attackableHurtTime.last) lastHitCrit = false

        if (!targetHittable) hitOnTheWay = false

        // Check if target is looking at us
        val rotDiff = rotationDifference(
            toRotation(player.eyePosition, target),
            target.rotation
        )

        val rotHittable = rotDiff < 22f + (10f * combinedPingMult) &&
            !target.box.contains(player.eyePosition)

        val targetHitLikely = rotHittable && !target.isUsingItem && targetDistance < 3.08f

        val simulatedDistance = simulateDistance(simPlayer, target, KnockbackSimulation.enabled && targetHitLikely)

        // Calculate optimal hurt time thresholds
        val baseHurtTime = 3f / (1f + sqrt(distance.toFloat()) - (rotDiff / 180f))
        val optimalHurtTime = max(baseHurtTime.toInt(), attackableHurtTime.last + 1)
        val hurtTimeNoEscape = (2 * distance * 8).toInt() / 10

        val groundHit = trueGround && if (targetHitLikely) {
            target.hurtTime !in 2..optimalHurtTime
        } else {
            targetHittable && !hitOnTheWay
        }

        val fallingHit = falling && if (targetHitLikely) {
            target.hurtTime !in (attackableHurtTime.last + 1)..optimalHurtTime
        } else {
            targetHittable && (!hitOnTheWay || !lastHitCrit)
        }
        val airHit = fallingHit || (target.hurtTime in 4..5 && targetHitLikely)

        val shouldHit = when {
            groundHit || airHit -> true

            (distance > notAboveRange || simulatedDistance > notAbovePredRange) &&
                player.hurtTime !in hurtTimeNoEscape..8 && targetHitLikely -> true

            // Target can't reach us
            targetDistance > 3.05f && targetHittable -> true

            // Panic hitting when low health
            player.health < notBelowOwnHealth -> true

            // Finish off low health target
            target.health < notBelowEnemyHealth -> true

            // Near edge - spam hit to reduce knockback
            EdgeDetection.enabled && player.isCloseToEdge(
                DirectionalInput(player.input),
                EdgeDetection.edgeLimit.toDouble()
            ) -> true

            else -> false
        }

        if (debug) {
            debugParameter(this, "ShouldHit") { shouldHit.toString() }
            debugParameter(this, "Distance") { "%.2f".format(distance) }
            debugParameter(this, "TargetDistance") { "%.2f".format(targetDistance) }
            debugParameter(this, "PredictedDistance") { "%.2f".format(simulatedDistance) }
            debugParameter(this, "RotationDiff") { "%.1f".format(rotDiff) }
            debugParameter(this, "TargetHurtTime") { target.hurtTime.toString() }
            debugParameter(this, "PlayerHurtTime") { player.hurtTime.toString() }
            debugParameter(this, "OnGround") { trueGround.toString() }
            debugParameter(this, "Falling") { falling.toString() }
            debugParameter(this, "HitOnTheWay") { hitOnTheWay.toString() }

            if (debug) {
                chat(
                    "(SmartHit) hit=$shouldHit dist=${"%.2f".format(distance)} " +
                        "predDist=${"%.2f".format(simulatedDistance)} rot=${"%.1f".format(rotDiff)} " +
                        "tHurt=${target.hurtTime} pHurt=${player.hurtTime}"
                )
            }
        }

        return shouldHit
    }

    private fun simulateDistance(
        simPlayer: SimulatedPlayer,
        target: Entity,
        simulateKnockback: Boolean
    ): Double {
        val prediction = target.position().subtract(target.lastPos)
            .scale(predictEnemyPosition.toDouble())
        val targetBox = target.box.move(prediction)

        if (simulateKnockback && simHurtTime <= 0) {
            simulateOwnKnockback(simPlayer, target)
        }

        return getDistanceToBox(simPlayer.pos, targetBox)
    }

    private fun simulateOwnKnockback(simPlayer: SimulatedPlayer, target: Entity) {
        val knockbackModifier = KnockbackSimulation.horizontalKnockback.random()

        // Calculate knockback direction based on target rotation
        val knockbackX = -Mth.sin(target.yRot * (PI.toFloat() / 180f)) * knockbackModifier * 0.5f
        val knockbackY = KnockbackSimulation.verticalKnockback.random()
        val knockbackZ = Mth.cos(target.yRot * (PI.toFloat() / 180f)) * knockbackModifier * 0.5f

        // Apply knockback to simulated velocity
        simPlayer.velocity = simPlayer.velocity.add(
            knockbackX.toDouble(),
            knockbackY.toDouble(),
            knockbackZ.toDouble()
        )

        if (debug) {
            chat("(SmartHit) KB: x=${"%.3f".format(knockbackX)} y=${"%.3f".format(knockbackY)} " +
                "z=${"%.3f".format(knockbackZ)} mod=${"%.3f".format(knockbackModifier)}")
        }

        simHurtTime = 10
    }

    private fun getDistanceToBox(pos: Vec3, box: AABB): Double {
        val nearestX = pos.x.coerceIn(box.minX, box.maxX)
        val nearestY = pos.y.coerceIn(box.minY, box.maxY)
        val nearestZ = pos.z.coerceIn(box.minZ, box.maxZ)

        val dx = pos.x - nearestX
        val dy = pos.y - nearestY
        val dz = pos.z - nearestZ

        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun toRotation(from: Vec3, target: Entity): Rotation {
        val diffX = target.x - from.x
        val diffY = (target.y + target.eyeHeight / 2.0) - from.y
        val diffZ = target.z - from.z

        val horizontalDistance = sqrt(diffX * diffX + diffZ * diffZ)

        val yaw = (Mth.atan2(diffZ, diffX) * (180.0 / PI) - 90.0).toFloat()
        val pitch = (-(Mth.atan2(diffY, horizontalDistance)) * (180.0 / PI)).toFloat()

        return Rotation(yaw, pitch)
    }

    private fun rotationDifference(a: Rotation, b: Rotation): Float {
        val yawDiff = abs(Mth.wrapDegrees(a.yaw - b.yaw))
        val pitchDiff = abs(a.pitch - b.pitch)
        return sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff)
    }

    private val Entity.rotation: Rotation
        get() = Rotation(this.yRot, this.xRot)

    private val LocalPlayer.onGroundTicks: Int
        get() = (this as? net.ccbluex.liquidbounce.interfaces.LocalPlayerAddition)
            ?.`liquid_bounce$getOnGroundTicks`() ?: 0
}
