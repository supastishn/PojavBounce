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
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.isInventoryOpen
import net.ccbluex.liquidbounce.utils.item.isMace
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
import net.minecraft.world.entity.player.Player
import java.util.UUID
import kotlin.math.abs
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
    private var wasCrit = false
    private var wasSprintingBeforeHit = false
    private var sprintingAfterHit = false

    // Blocking tracking
    private var blockStartTime: Long? = null
    private var wasBlockingBeforeHit = false
    private var blockingAfterHit = false
    private var ticksBlocking = 0
    private var ticksNotBlocking = 0
    private var blockedOnScanRange = false

    // FailSwing tracking
    private var lastSwingWasMiss = false
    private var missSwingRange = 0f

    // Inventory tracking
    private var wasInventoryOpen = false
    private var attackedWhileInventoryOpen = false

    // Rotation tracking
    private var lastRotationDeltaYaw = 0f
    private var lastRotationDeltaPitch = 0f

    // Movement correction tracking
    private var lastAimPointY = 0f
    private var aimPointYHistory = mutableListOf<Float>()
    private var ticksSinceLastTarget = 0
    private var lastServerRotation: Rotation? = null

    // Range exit tracking
    private var exitingRange = false
    private var attackedWhileExiting = false

    // Shield tracking
    private var targetHadShield = false
    private var attackedShieldingTarget = false

    // Mace tracking
    private var usingMace = false
    private var maceSmashPossible = false

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
        wasCrit = false
        wasSprintingBeforeHit = false
        sprintingAfterHit = false
        blockStartTime = null
        wasBlockingBeforeHit = false
        blockingAfterHit = false
        ticksBlocking = 0
        ticksNotBlocking = 0
        blockedOnScanRange = false
        lastSwingWasMiss = false
        missSwingRange = 0f
        wasInventoryOpen = false
        attackedWhileInventoryOpen = false
        lastRotationDeltaYaw = 0f
        lastRotationDeltaPitch = 0f
        lastAimPointY = 0f
        aimPointYHistory.clear()
        ticksSinceLastTarget = 0
        lastServerRotation = null
        exitingRange = false
        attackedWhileExiting = false
        targetHadShield = false
        attackedShieldingTarget = false
        usingMace = false
        maceSmashPossible = false
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
        val isCurrentlyBlocking = player.isUsingItem && player.useItem.item.toString().contains("sword")
        if (isCurrentlyBlocking) {
            if (blockStartTime == null) {
                blockStartTime = System.currentTimeMillis()
            }
            ticksBlocking++
            ticksNotBlocking = 0
        } else {
            blockStartTime = null
            ticksNotBlocking++
            ticksBlocking = 0
        }

        // Track inventory state
        wasInventoryOpen = isInventoryOpen

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

                // Calculate scan range (furthest entity we could target)
                val allTargets = world.entities.getAll()
                    .filterIsInstance<LivingEntity>()
                    .filter { it.id != player.id && !it.isRemoved }
                val scanRange = allTargets.maxOfOrNull { sqrt(player.squaredBoxedDistanceTo(it)).toFloat() } ?: 0f
                val closestDist = allTargets.minOfOrNull { sqrt(player.squaredBoxedDistanceTo(it)).toFloat() } ?: distance.toFloat()

                // Check if target is in FOV (roughly 110 degrees)
                val lookVec = current.directionVector
                val toTarget = target.position().subtract(player.eyePosition).normalize()
                val dot = lookVec.dot(toTarget)
                val targetInFOV = dot > 0.4 // ~66 degree half-angle

                // Check if this would be a miss (swing at air)
                val raycastHit = raycastResult != null
                val swingAtAir = !raycastHit && attackAttempted
                val missDistance = if (swingAtAir) distance.toFloat() else 0f

                // Calculate rotation deltas
                val rotationDelta = current.rotationDeltaTo(next)
                lastRotationDeltaYaw = rotationDelta.deltaYaw
                lastRotationDeltaPitch = rotationDelta.deltaPitch

                // Calculate target rotation for time estimation
                val targetRotation = Rotation.lookingAt(point = target.box.center, from = eyes)
                val rotationDiff = current.rotationDeltaTo(targetRotation)
                val estimatedTicks = (kotlin.math.max(
                    kotlin.math.abs(rotationDiff.deltaYaw),
                    kotlin.math.abs(rotationDiff.deltaPitch)
                ) / 30f).toInt().coerceAtLeast(1) // Rough estimate at 30°/tick

                // Check if blocking on scan range (outside attack range but within scan)
                val inScanRange = distance > 4.2 && distance <= scanRange
                if (isCurrentlyBlocking && inScanRange) {
                    blockedOnScanRange = true
                }

                // Calculate click interval
                val clickInterval = if (lastClickTime > 0) now - lastClickTime else 0L

                // === NEW DATA CALCULATIONS ===

                // Movement correction data
                val playerYaw = player.yRot
                val movementYaw = if (player.deltaMovement.horizontalDistanceSqr() > 0.01) {
                    kotlin.math.atan2(player.deltaMovement.z, player.deltaMovement.x).toFloat() * 180f / kotlin.math.PI.toFloat() - 90f
                } else playerYaw
                val strafeAngle = abs(playerYaw - movementYaw)
                val movementAligned = strafeAngle < 15f

                // Rotation reset tracking
                val serverRotation = RotationManager.serverRotation
                val rotationResetOccurred = if (lastServerRotation != null) {
                    val diff = current.rotationDeltaTo(player.rotation)
                    abs(diff.deltaYaw) + abs(diff.deltaPitch) > 45f
                } else false
                lastServerRotation = serverRotation

                if (target != null) {
                    ticksSinceLastTarget = 0
                } else {
                    ticksSinceLastTarget++
                }

                val rotationDiffFromPlayer = current.rotationDeltaTo(player.rotation).let {
                    abs(it.deltaYaw) + abs(it.deltaPitch)
                }

                // Aim point analysis
                val targetBox = target.box
                val aimPointY = raycastResult?.vec?.y ?: targetBox.center.y
                val aimPointYRelative = ((aimPointY - targetBox.minY) / (targetBox.maxY - targetBox.minY)).toFloat()

                aimPointYHistory.add(aimPointYRelative)
                if (aimPointYHistory.size > 10) aimPointYHistory.removeAt(0)

                val aimPointVariance = if (aimPointYHistory.size > 1) {
                    val avg = aimPointYHistory.average()
                    aimPointYHistory.map { (it - avg) * (it - avg) }.average().toFloat()
                } else 0f

                val aimedAtHead = aimPointYRelative > 0.75f
                val aimedAtBody = aimPointYRelative in 0.3f..0.75f
                val aimedAtFeet = aimPointYRelative < 0.3f

                val aimPointJitter = if (aimPointYHistory.size >= 2) {
                    abs(aimPointYHistory.last() - aimPointYHistory[aimPointYHistory.size - 2])
                } else 0f

                val aimPointSticky = aimPointVariance < 0.01f && aimPointYHistory.size >= 5

                // Range exit prediction
                val avgRange = 4.2f // Standard KillAura range
                exitingRange = distance > avgRange * 0.9 && player.deltaMovement.horizontalDistanceSqr() > 0.01
                if (exitingRange && attackAttempted) {
                    attackedWhileExiting = true
                }

                // Shield detection
                targetHadShield = target is Player && target.wouldBlockHit
                if (targetHadShield && attackAttempted) {
                    attackedShieldingTarget = true
                }

                // Mace detection
                usingMace = player.mainHandItem.item is net.minecraft.world.item.MaceItem ||
                            player.offhandItem.item is net.minecraft.world.item.MaceItem
                maceSmashPossible = usingMace && player.fallDistance > 1.5f

                recordPacket(
                    KillAuraConfigSample(
                        combatData = CombatSample(
                            currentVector = current.directionVector,
                            previousVector = previous.directionVector,
                            targetVector = targetRotation.directionVector,
                            velocityDelta = rotationDelta.toVec2f(),
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
                        raycastHit = raycastHit,
                        actualRange = distance.toFloat(),
                        wasBlocking = player.isBlocking,
                        blockDuration = blockDuration,
                        attackAttempted = attackAttempted,
                        attackSucceeded = attackSucceeded,
                        availableTargets = allTargets.count(),
                        targetHealth = target.health,
                        targetArmorValue = target.armorValue.toFloat(),
                        // Criticals data
                        wasFalling = player.fallDistance > 0,
                        fallDistance = player.fallDistance,
                        onGround = player.onGround(),
                        wasCriticalHit = wasCrit,
                        // Sprint data
                        wasSprinting = player.isSprinting,
                        sprintingAfterHit = sprintingAfterHit,
                        // Scan/Target data
                        scanRange = scanRange,
                        closestTargetDistance = closestDist,
                        targetInFOV = targetInFOV,
                        // Miss/FailSwing data
                        swingAtAir = swingAtAir,
                        missDistance = missDistance,
                        // AutoBlock data
                        wasBlockingBeforeHit = wasBlockingBeforeHit,
                        blockingAfterHit = blockingAfterHit,
                        ticksBlockedBeforeHit = ticksBlocking,
                        ticksUnblockedBeforeHit = ticksNotBlocking,
                        blockedOnScanRange = blockedOnScanRange,
                        // FailSwing data
                        failSwingRange = missSwingRange,
                        swingWhileMiss = lastSwingWasMiss,
                        // Raycast/Targeting data
                        raycastModeUsed = "All",
                        targetThroughWall = hasWall && raycastHit,
                        // Timing data
                        tickCount = player.tickCount,
                        attackCooldownProgress = player.getAttackStrengthScale(0.5f),
                        clickIntervalMs = clickInterval,
                        // Rotation timing data
                        rotationDeltaYaw = lastRotationDeltaYaw,
                        rotationDeltaPitch = lastRotationDeltaPitch,
                        timeToReachTargetRotation = estimatedTicks,
                        // Inventory state
                        inventoryOpen = wasInventoryOpen,
                        attackedWhileInventoryOpen = attackedWhileInventoryOpen,
                        // Movement correction
                        movementYaw = movementYaw,
                        strafeAngle = strafeAngle,
                        movementAlignedWithRotation = movementAligned,
                        // Rotation reset
                        rotationResetOccurred = rotationResetOccurred,
                        ticksSinceLastTarget = ticksSinceLastTarget,
                        rotationDiffFromPlayer = rotationDiffFromPlayer,
                        // Aim point
                        aimPointYRelative = aimPointYRelative,
                        aimedAtHead = aimedAtHead,
                        aimedAtBody = aimedAtBody,
                        aimedAtFeet = aimedAtFeet,
                        aimPointJitter = aimPointJitter,
                        aimPointSticky = aimPointSticky,
                        // Range exit
                        exitingRange = exitingRange,
                        attackedWhileExiting = attackedWhileExiting,
                        // Shield
                        targetBlockingWithShield = targetHadShield,
                        attackedShieldingTarget = attackedShieldingTarget,
                        // Mace
                        usingMace = usingMace,
                        maceSmashPossible = maceSmashPossible
                    )
                )

                // Reset attack flags
                attackAttempted = false
                attackSucceeded = false
                wasCrit = false
                sprintingAfterHit = false
                wasBlockingBeforeHit = false
                blockingAfterHit = false
                blockedOnScanRange = false
                lastSwingWasMiss = false
                attackedWhileInventoryOpen = false
                exitingRange = false
                attackedWhileExiting = false
                targetHadShield = false
                attackedShieldingTarget = false

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

                // Track blocking state before hit
                wasBlockingBeforeHit = player.isBlocking

                // Track sprint state before hit
                wasSprintingBeforeHit = player.isSprinting

                attackAttempted = true
                attackSucceeded = targetEntity.hurtTime == 0 || targetEntity.hurtTime > 8

                // Track if this was a critical hit (falling and not on ground)
                wasCrit = player.fallDistance > 0 && !player.onGround()

                // Track sprint after hit (same tick for now, will be different next tick)
                sprintingAfterHit = player.isSprinting

                // Track blocking after hit
                blockingAfterHit = player.isBlocking

                // Track if attacked while inventory open
                if (wasInventoryOpen) {
                    attackedWhileInventoryOpen = true
                }

                // Track miss swing data
                val distance = sqrt(player.squaredBoxedDistanceTo(targetEntity))
                val eyes = player.eyePosition
                val raycastResult = raytraceBox(
                    eyes,
                    targetEntity.box,
                    range = distance.toDouble(),
                    wallsRange = distance.toDouble()
                )
                if (raycastResult == null) {
                    lastSwingWasMiss = true
                    missSwingRange = distance.toFloat()
                }

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
