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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.utils.facingEnemy
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.SimulatedArrow
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.entity.projectile.arrow.SpectralArrow
import net.minecraft.world.entity.projectile.arrow.ThrownTrident
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.world.item.ShieldItem
import net.minecraft.world.phys.AABB

/**
 * SmartBlock module
 *
 * Context-aware blocking that predicts incoming attacks and blocks just before getting hit.
 * Automatically unblocks to counter-attack for optimal combat flow.
 */
@Suppress("MagicNumber")
object ModuleSmartBlock : ClientModule("SmartBlock", ModuleCategories.COMBAT) {

    // Detection settings
    private val range by floatRange("Range", 3f..4.5f, 1f..8f)
    private val wallsRange by float("WallsRange", 0f, 0f..8f)

    // Timing settings
    private val blockTiming by enumChoice("BlockTiming", BlockTiming.PREDICTIVE)
    private val unblockForAttack by boolean("UnblockForAttack", true)
    private val unblockDelay by intRange("UnblockDelay", 1..2, 0..10, "ticks")
    private val reblockDelay by intRange("ReblockDelay", 1..2, 0..10, "ticks")

    // What to block
    private val blockMode by enumChoice("BlockMode", BlockMode.BOTH)

    // Projectile blocking
    private object ProjectileBlock : ToggleableConfigurable(this, "ProjectileBlock", true) {
        val projectileRange by float("ProjectileRange", 15f, 5f..30f)
        val ticksToImpact by int("TicksToImpact", 10, 1..30, "ticks")
    }

    // Swing detection (detect when enemy is about to attack)
    private object SwingDetection : ToggleableConfigurable(this, "SwingDetection", true) {
        val reactToSwingStart by boolean("ReactToSwingStart", true)
    }

    init {
        tree(ProjectileBlock)
        tree(SwingDetection)
    }

    // State tracking
    private var isBlocking = false
    private var blockCooldown = 0
    private var unblockCooldown = 0
    private var lastAttackTick = 0
    private val enemySwingStates = mutableMapOf<Int, Boolean>()

    /**
     * Visual blocking state for the sword block animation.
     * Used by MixinItemInHandRenderer to show the blocking animation.
     */
    @JvmStatic
    var blockVisual = false
        get() = field && running && (isOlderThanOrEqual1_8 || ModuleSwordBlock.running)

    @Suppress("unused")
    private val tickHandler = tickHandler {
        // Decrease cooldowns
        if (blockCooldown > 0) blockCooldown--
        if (unblockCooldown > 0) unblockCooldown--

        // Check if we're attacking - unblock for attack
        if (unblockForAttack && mc.options.keyAttack.isPressedOnAny) {
            if (isBlocking) {
                stopBlocking()
                unblockCooldown = unblockDelay.random()
            }
            return@tickHandler
        }

        // Wait for unblock cooldown after attacking
        if (unblockCooldown > 0) {
            return@tickHandler
        }

        // Wait for block cooldown
        if (blockCooldown > 0) {
            return@tickHandler
        }

        val blockHand = getBlockableHand()
        if (blockHand == null) {
            stopBlocking()
            return@tickHandler
        }

        // Evaluate threat level
        val threatLevel = evaluateThreat()

        if (threatLevel != ThreatLevel.NONE) {
            if (!isBlocking) {
                startBlocking(blockHand)
            }
        } else {
            if (isBlocking) {
                stopBlocking()
                blockCooldown = reblockDelay.random()
            }
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> {
        lastAttackTick = player.tickCount
        if (isBlocking && unblockForAttack) {
            stopBlocking()
            unblockCooldown = unblockDelay.random()
        }
    }

    /**
     * Evaluates the current threat level from enemies and projectiles.
     */
    private fun evaluateThreat(): ThreatLevel {
        var maxThreat = ThreatLevel.NONE

        // Check melee threats
        val meleeThreat = evaluateMeleeThreat()
        if (meleeThreat.ordinal > maxThreat.ordinal) {
            maxThreat = meleeThreat
        }

        // Check projectile threats
        if (ProjectileBlock.enabled) {
            val projectileThreat = evaluateProjectileThreat()
            if (projectileThreat.ordinal > maxThreat.ordinal) {
                maxThreat = projectileThreat
            }
        }

        return maxThreat
    }

    /**
     * Evaluates melee threat from nearby enemies.
     */
    private fun evaluateMeleeThreat(): ThreatLevel {
        val enemies = world.getEntitiesBoxInRange(
            player.eyePosition,
            range.endInclusive.toDouble()
        ) {
            it != player && it.shouldBeAttacked() && it is LivingEntity
        }

        for (enemy in enemies) {
            if (enemy !is LivingEntity) continue

            val distance = player.distanceTo(enemy).toDouble()
            if (distance !in range.start.toDouble()..range.endInclusive.toDouble()) continue

            // Check if enemy is facing us
            val isFacingUs = facingEnemy(
                fromEntity = enemy,
                toEntity = player,
                rotation = enemy.rotation,
                range = range.endInclusive.toDouble(),
                wallsRange = wallsRange.toDouble()
            )

            if (!isFacingUs) continue

            // Determine threat level based on timing mode
            when (blockTiming) {
                BlockTiming.ALWAYS -> {
                    return ThreatLevel.HIGH
                }
                BlockTiming.REACTIVE -> {
                    // Block only when enemy is actively swinging (already checked range + facing)
                    if (isEnemySwinging(enemy)) {
                        return ThreatLevel.HIGH
                    }
                }
                BlockTiming.PREDICTIVE -> {
                    // Predict when enemy will attack based on multiple factors
                    val threat = predictAttack(enemy, distance)
                    if (threat.ordinal > ThreatLevel.NONE.ordinal) {
                        return threat
                    }
                }
            }
        }

        return ThreatLevel.NONE
    }

    /**
     * Predicts when an enemy is about to attack based on various factors.
     */
    private fun predictAttack(enemy: LivingEntity, distance: Double): ThreatLevel {
        // Factor 1: Enemy is swinging (definite attack)
        if (SwingDetection.enabled) {
            val wasSwinging = enemySwingStates[enemy.id] ?: false
            val isSwinging = enemy.swinging

            // Update state
            enemySwingStates[enemy.id] = isSwinging

            // React to swing start (most critical moment)
            if (SwingDetection.reactToSwingStart && isSwinging && !wasSwinging) {
                return ThreatLevel.CRITICAL
            }

            // Currently swinging
            if (isSwinging) {
                return ThreatLevel.HIGH
            }
        }

        // Factor 2: Enemy attack cooldown (for 1.9+ combat)
        if (enemy is Player) {
            val attackStrength = enemy.getAttackStrengthScale(0f)
            // If attack is ready (cooldown complete), they're likely to attack soon
            if (attackStrength >= 0.9f && distance <= 3.5) {
                return ThreatLevel.MEDIUM
            }
        }

        // Factor 3: Very close range + facing = high threat
        if (distance <= 2.5) {
            return ThreatLevel.MEDIUM
        }

        // Factor 4: Enemy is sprinting towards us (aggressive approach)
        if (enemy.isSprinting && distance <= 4.0) {
            // Check if approaching
            val relativeVelocity = enemy.deltaMovement.dot(
                player.position().subtract(enemy.position()).normalize()
            )
            if (relativeVelocity > 0.1) {
                return ThreatLevel.LOW
            }
        }

        return ThreatLevel.NONE
    }

    /**
     * Checks if an enemy is currently swinging.
     */
    private fun isEnemySwinging(enemy: LivingEntity): Boolean {
        return enemy.swinging
    }

    /**
     * Evaluates projectile threat from incoming arrows and tridents.
     */
    private fun evaluateProjectileThreat(): ThreatLevel {
        val projectiles = world.entitiesForRendering().filter { entity ->
            (entity is Arrow || entity is SpectralArrow ||
                (entity is ThrownTrident && entity.clientSideReturnTridentTickCount == 0)) &&
                !isProjectileInGround(entity) &&
                entity.position().distanceTo(player.position()) <= ProjectileBlock.projectileRange
        }

        for (projectile in projectiles) {
            val ticksToHit = calculateTicksToImpact(projectile)
            if (ticksToHit != null && ticksToHit <= ProjectileBlock.ticksToImpact) {
                return if (ticksToHit <= 3) ThreatLevel.CRITICAL else ThreatLevel.HIGH
            }
        }

        return ThreatLevel.NONE
    }

    /**
     * Calculates how many ticks until a projectile hits the player.
     */
    private fun calculateTicksToImpact(projectile: Entity): Int? {
        val simArrow = SimulatedArrow(world, projectile.position(), projectile.deltaMovement, false)
        val playerBox = AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3)
            .inflate(0.5)
            .move(player.position())

        for (tick in 0 until 40) {
            val lastPos = simArrow.pos
            simArrow.tick()

            if (simArrow.inGround) return null

            val hitResult = playerBox.clip(lastPos, simArrow.pos)
            if (hitResult.isPresent) {
                return tick
            }
        }

        return null
    }

    private fun isProjectileInGround(entity: Entity): Boolean {
        return when (entity) {
            is Arrow -> entity.isInGround
            is SpectralArrow -> entity.isInGround
            is ThrownTrident -> entity.isInGround
            else -> false
        }
    }

    /**
     * Gets the hand that can be used for blocking.
     */
    private fun getBlockableHand(): InteractionHand? {
        return when (blockMode) {
            BlockMode.SWORD_ONLY -> {
                if (player.mainHandItem.isSword) InteractionHand.MAIN_HAND else null
            }
            BlockMode.SHIELD_ONLY -> {
                when {
                    player.offhandItem.item is ShieldItem -> InteractionHand.OFF_HAND
                    player.mainHandItem.item is ShieldItem -> InteractionHand.MAIN_HAND
                    else -> null
                }
            }
            BlockMode.BOTH -> {
                when {
                    // Prefer shield in offhand
                    player.offhandItem.item is ShieldItem -> InteractionHand.OFF_HAND
                    // Shield in main hand
                    player.mainHandItem.item is ShieldItem -> InteractionHand.MAIN_HAND
                    // Sword in main hand (requires SwordBlock module)
                    player.mainHandItem.isSword -> InteractionHand.MAIN_HAND
                    else -> null
                }
            }
        }
    }

    /**
     * Checks if an item can block.
     */
    private fun canBlock(hand: InteractionHand): Boolean {
        val item = player.getItemInHand(hand)
        val animation = item.item?.getUseAnimation(item)
        return animation == ItemUseAnimation.BLOCK ||
            (item.isSword && ModuleSwordBlock.running)
    }

    private fun startBlocking(hand: InteractionHand) {
        if (isBlocking) return

        val item = player.getItemInHand(hand)
        if (item.isEmpty) return

        // Use the item to start blocking
        val result = interaction.useItem(player, hand)
        if (result.consumesAction()) {
            isBlocking = true
            // Only set visual when blocking actually started
            blockVisual = true
        }
    }

    private fun stopBlocking() {
        // Always reset visual state
        blockVisual = false

        if (!isBlocking) return

        if (player.isUsingItem) {
            interaction.releaseUsingItem(player)
        }
        isBlocking = false
    }

    override fun onDisabled() {
        stopBlocking()
        blockVisual = false
        blockCooldown = 0
        unblockCooldown = 0
        enemySwingStates.clear()
    }

    private enum class ThreatLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private enum class BlockTiming(override val choiceName: String) : NamedChoice {
        ALWAYS("Always"),
        REACTIVE("Reactive"),
        PREDICTIVE("Predictive")
    }

    private enum class BlockMode(override val choiceName: String) : NamedChoice {
        SWORD_ONLY("SwordOnly"),
        SHIELD_ONLY("ShieldOnly"),
        BOTH("Both")
    }
}
