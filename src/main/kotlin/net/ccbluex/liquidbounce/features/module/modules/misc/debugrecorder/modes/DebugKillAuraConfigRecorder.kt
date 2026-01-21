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

package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes

import net.ccbluex.liquidbounce.deeplearn.data.CombatSample
import net.ccbluex.liquidbounce.deeplearn.data.KillAuraConfigSample
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.lastPos
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Zombie
import java.util.UUID
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Advanced combat trainer that spawns buffed zombies moving at player speed.
 * Records comprehensive data for full KillAura autoconfig.
 *
 * NOTE: This mode disables attack cooldown for realistic high-CPS training.
 */
object DebugKillAuraConfigRecorder : ModuleDebugRecorder.DebugRecorderMode<KillAuraConfigSample>("KillAuraConfig") {

    private var isFirstRun = true
    private var target: LivingEntity? = null

    // Click tracking
    private var lastClickTime: Long = 0
    private val clickTimes = mutableListOf<Long>()
    private var attackAttempted = false
    private var attackSucceeded = false

    // Blocking tracking
    private var blockStartTime: Long? = null

    /**
     * Check if KillAuraConfig recorder is currently active
     */
    val isActive: Boolean
        get() = isSelected && ModuleDebugRecorder.running

    override fun enable() {
        isFirstRun = true
        lastClickTime = 0
        clickTimes.clear()
        attackAttempted = false
        attackSucceeded = false
        blockStartTime = null
        super.enable()
    }

    override fun disable() {
        val target = target ?: return
        world.removeEntity(target.id, Entity.RemovalReason.DISCARDED)
        super.disable()
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        var previous = RotationManager.currentRotation ?: player.rotation

        // Disable attack cooldown for training
        player.attackStrengthTicker = 1000

        // Track blocking state
        if (player.isUsingItem && player.useItem.item.toString().contains("sword")) {
            if (blockStartTime == null) {
                blockStartTime = System.currentTimeMillis()
            }
        } else {
            blockStartTime = null
        }

        target = spawnBuffedZombie()
        if (isFirstRun) {
            tickUntil { target == null }
            isFirstRun = false
            chat("✧ Starting advanced KillAura training (cooldown disabled)...")
        } else {
            tickUntil {
                val target = target ?: return@tickUntil true

                val next = RotationManager.currentRotation ?: player.rotation
                val current = RotationManager.previousRotation ?: player.lastRotation
                val previous = previous.apply {
                    previous = current
                }

                val distance = sqrt(player.squaredBoxedDistanceTo(target))

                // Calculate CPS
                val now = System.currentTimeMillis()
                clickTimes.removeAll { it < now - 1000 } // Keep only last second
                val currentCPS = clickTimes.size.toFloat()

                // Check raycast
                val eyes = player.eyePosition
                val raycastResult = raytraceBox(
                    eyes,
                    eyes.add(current.directionVector * distance),
                    target.box,
                    range = distance.toDouble(),
                    wallsRange = distance.toDouble()
                )

                // Check wall between
                val hasWall = world.clip(
                    net.minecraft.world.level.ClipContext(
                        eyes,
                        target.box.center,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        player
                    )
                ).type != net.minecraft.world.phys.HitResult.Type.MISS

                val blockDuration = blockStartTime?.let { now - it } ?: 0L

                recordPacket(
                    KillAuraConfigSample(
                        combatData = CombatSample(
                            currentVector = current.directionVector,
                            previousVector = previous.directionVector,
                            targetVector = Rotation.lookingAt(
                                point = target.box.center,
                                from = eyes
                            ).directionVector,
                            velocityDelta = current.rotationDeltaTo(next).toVec2f(),
                            playerDiff = player.position().subtract(player.lastPos),
                            targetDiff = target.position().subtract(target.lastPos),
                            age = target.tickCount,
                            hurtTime = target.hurtTime,
                            distance = distance.toFloat()
                        ),
                        clickTimestamp = now,
                        timeSinceLastClick = if (lastClickTime > 0) now - lastClickTime else 0,
                        currentCPS = currentCPS,
                        hasWallBetween = hasWall,
                        raycastHit = raycastResult != null,
                        actualRange = distance.toFloat(),
                        wasBlocking = player.isBlocking,
                        blockDuration = blockDuration,
                        attackAttempted = attackAttempted,
                        attackSucceeded = attackSucceeded,
                        availableTargets = world.entities().count {
                            it is LivingEntity && it != player && it.isAlive
                        },
                        targetHealth = target.health,
                        targetArmorValue = target.armorValue.toFloat()
                    )
                )

                // Reset attack flags
                attackAttempted = false
                attackSucceeded = false

                false
            }

            chat("✧ Recorded ${packets.size} comprehensive samples")
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ServerboundInteractPacket) {
            val targetEntity = target ?: return@handler

            if (packet.entityId == targetEntity.id) {
                // Track click
                val now = System.currentTimeMillis()
                lastClickTime = now
                clickTimes.add(now)

                attackAttempted = true
                attackSucceeded = targetEntity.hurtTime == 0 || targetEntity.hurtTime > 8

                world.removeEntity(targetEntity.id, Entity.RemovalReason.DISCARDED)
                target = null
                event.cancelEvent()
            }
        }
    }

    /**
     * Spawns a buffed zombie with player-like movement speed and maneuverability
     */
    private fun spawnBuffedZombie(): LivingEntity {
        val zombie = Zombie(EntityType.ZOMBIE, world)
        zombie.setUUID(UUID.randomUUID())

        val distance = Random.nextDouble() * 0.9 + 2.5

        // Spawn in player's view range
        val direction = Rotation(
            player.yRot + Random.nextDouble(-70.0, 70.0).toFloat(),
            Random.nextDouble(-25.0, 15.0).toFloat()
        ).directionVector * distance

        val position = player.eyePosition.add(direction)
        zombie.setPos(position)

        // Buff zombie to move like a player
        zombie.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue = 0.13 // Player speed
        zombie.getAttribute(Attributes.FOLLOW_RANGE)?.baseValue = 64.0
        zombie.getAttribute(Attributes.ATTACK_DAMAGE)?.baseValue = 1.0

        // Add speed and jump boost for better maneuverability
        zombie.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SPEED, 999999, 1, false, false))
        zombie.addEffect(MobEffectInstance(MobEffects.JUMP, 999999, 2, false, false))

        // Make zombie aggressive toward player
        zombie.setTarget(player)

        world.addEntity(zombie)

        // Play spawn sound
        world.playLocalSound(
            position.x,
            position.y,
            position.z,
            SoundEvents.ZOMBIE_AMBIENT,
            SoundSource.HOSTILE,
            0.8f,
            1.2f,
            false
        )

        return zombie
    }
}
