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

package net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.deeplearn.data.KillAuraConfigSample
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraClicker
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraTargetTracker
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFailSwing
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Comprehensive analyzer and applier for ALL KillAura settings from KillAuraConfigSample data
 */
object KillAuraConfigApplier {

    data class ComprehensiveAnalysis(
        // Range settings
        val recommendedRange: Float,
        val recommendedWallRange: Float,
        val recommendedScanExtraRange: ClosedFloatingPointRange<Float>,

        // Click timing
        val avgCPS: Float,
        val cpsRange: IntRange,
        val clickPattern: String,
        val attackCooldownEnabled: Boolean,

        // Criticals analysis
        val critRate: Float,
        val usesCrits: Boolean,
        val critMode: String,

        // Sprint analysis
        val keepsSprint: Boolean,
        val sprintRate: Float,

        // Wall/Raycast
        val wallHitRate: Float,
        val raycastSuccessRate: Float,
        val raycastMode: String,
        val aimThroughWalls: Boolean,

        // Miss analysis
        val missRate: Float,
        val avgMissDistance: Float,
        val failSwingEnabled: Boolean,
        val failSwingRange: ClosedFloatingPointRange<Float>,

        // AutoBlock analysis
        val autoBlockEnabled: Boolean,
        val autoBlockOnScanRange: Boolean,
        val tickOffRange: IntRange,
        val tickOnRange: IntRange,
        val blockRate: Float,
        val autoBlockChance: Float,

        // Inventory
        val ignoreOpenInventory: Boolean,
        val simulateInventoryClosing: Boolean,

        // Rotation settings
        val rotationTiming: String,
        val avgRotationSpeed: Float,

        // Target settings
        val recommendedFOV: Float,
        val recommendedHurtTime: Int,
        val targetPriority: String,

        // NEW: Movement correction
        val movementCorrectionMode: String,
        val strafeAngleAvg: Float,
        val resetThreshold: Float,
        val ticksUntilReset: Int,

        // NEW: Aim point settings
        val preferredAimPoint: String,
        val aimPointJitterEnabled: Boolean,
        val aimPointStickyEnabled: Boolean,

        // NEW: Range exit ignoring
        val ignoreWhenExitingRange: Boolean,

        // NEW: Shield break ignoring
        val ignoreOnShieldBreak: Boolean,

        // NEW: Mace smash ignoring
        val ignoreOnMaceSmash: Boolean
    )

    fun analyze(samples: List<KillAuraConfigSample>): ComprehensiveAnalysis? {
        if (samples.isEmpty()) return null

        // === Range Analysis ===
        val ranges = samples.map { it.actualRange }
        val sortedRanges = ranges.sorted()
        val avgRange = ranges.average().toFloat()
        val p90Range = sortedRanges[(sortedRanges.size * 0.90).toInt().coerceIn(0, sortedRanges.size - 1)]
        val maxRange = ranges.maxOrNull() ?: 4.2f

        // Wall detection for wall range
        val wallSamples = samples.filter { it.hasWallBetween }
        val wallHitRate = wallSamples.size.toFloat() / samples.size
        val recommendedWallRange = if (wallHitRate > 0.1f) {
            (avgRange * (1 - wallHitRate * 0.5f)).coerceIn(0f, avgRange)
        } else {
            (p90Range * 0.85f).coerceIn(0f, avgRange)
        }

        // Scan range
        val scanRanges = samples.map { it.scanRange }.filter { it > 0 }
        val avgScanRange = if (scanRanges.isNotEmpty()) scanRanges.average().toFloat() else maxRange + 2f
        val maxScanRange = scanRanges.maxOrNull() ?: (maxRange + 3f)
        val scanExtraStart = (avgScanRange - avgRange).coerceIn(0f, 7f)
        val scanExtraEnd = (maxScanRange - avgRange).coerceIn(scanExtraStart + 0.5f, 7f)

        // === CPS Analysis ===
        val cpsList = samples.mapNotNull { if (it.currentCPS > 0) it.currentCPS else null }
        val avgCPS = if (cpsList.isNotEmpty()) cpsList.average().toFloat() else 10f
        val minCPS = (cpsList.minOrNull() ?: 8f).toInt().coerceIn(1, 20)
        val maxCPS = (cpsList.maxOrNull() ?: 15f).toInt().coerceIn(minCPS + 1, 20)

        // Click pattern analysis
        val clickIntervals = samples.filter { it.clickIntervalMs > 0 }.map { it.clickIntervalMs }
        val clickPattern = analyzeClickPattern(clickIntervals)

        // Attack cooldown analysis
        val cooldownSamples = samples.filter { it.attackCooldownProgress < 1.0f }
        val attackCooldownEnabled = cooldownSamples.size.toFloat() / samples.size < 0.3f // Uses cooldown if <30% attacks before ready

        // === Criticals Analysis ===
        val attackSamples = samples.filter { it.attackAttempted }
        val critSamples = attackSamples.filter { it.wasCriticalHit }
        val critRate = if (attackSamples.isNotEmpty()) {
            critSamples.size.toFloat() / attackSamples.size
        } else 0f
        val usesCrits = critRate > 0.15f
        val critMode = when {
            critRate > 0.5f -> "Always"
            critRate > 0.2f -> "Smart"
            else -> "None"
        }

        // === Sprint Analysis ===
        val sprintSamples = attackSamples.filter { it.wasSprinting }
        val sprintRate = if (attackSamples.isNotEmpty()) {
            sprintSamples.size.toFloat() / attackSamples.size
        } else 0f
        val sprintAfterHitSamples = attackSamples.filter { it.sprintingAfterHit }
        val keepsSprint = sprintAfterHitSamples.size.toFloat() / attackSamples.size.coerceAtLeast(1) > 0.5f

        // === Raycast Analysis ===
        val raycastSuccessRate = samples.count { it.raycastHit }.toFloat() / samples.size
        val wallHitSuccesses = samples.count { it.targetThroughWall && it.raycastHit }
        val aimThroughWalls = wallHitSuccesses.toFloat() / samples.size.coerceAtLeast(1) > 0.1f

        // Raycast mode from data
        val raycastModes = samples.map { it.raycastModeUsed }.groupingBy { it }.eachCount()
        val raycastMode = raycastModes.maxByOrNull { it.value }?.key ?: "All"

        // === Miss/FailSwing Analysis ===
        val missSamples = samples.filter { it.swingAtAir || it.swingWhileMiss }
        val missRate = missSamples.size.toFloat() / samples.size
        val avgMissDistance = if (missSamples.isNotEmpty()) {
            missSamples.map { it.missDistance.coerceAtLeast(it.failSwingRange) }.filter { it > 0 }.average().toFloat()
        } else 0f

        // FailSwing: enabled if player swings at air frequently (>5% of time)
        val failSwingEnabled = missRate > 0.05f
        val failSwingRanges = missSamples.map { it.failSwingRange }.filter { it > 0 }
        val failSwingStart = if (failSwingRanges.isNotEmpty()) {
            (failSwingRanges.average() - 0.5).coerceIn(0.0, 10.0).toFloat()
        } else 2.5f
        val failSwingEnd = if (failSwingRanges.isNotEmpty()) {
            failSwingRanges.maxOrNull()?.coerceIn(failSwingStart + 0.5f, 10f) ?: 3.5f
        } else 3.5f

        // === AutoBlock Analysis ===
        val blockingSamples = samples.filter { it.wasBlocking || it.wasBlockingBeforeHit || it.blockingAfterHit }
        val blockRate = blockingSamples.size.toFloat() / samples.size
        val autoBlockEnabled = blockRate > 0.1f // Blocks at least 10% of time

        // Check if player blocks on scan range (outside attack range)
        val scanRangeBlockSamples = samples.filter { it.blockedOnScanRange }
        val autoBlockOnScanRange = scanRangeBlockSamples.size.toFloat() / samples.size.coerceAtLeast(1) > 0.05f

        // Calculate tick off/on ranges
        val ticksBeforeHit = attackSamples.filter { it.ticksUnblockedBeforeHit > 0 }.map { it.ticksUnblockedBeforeHit }
        val tickOffMin = if (ticksBeforeHit.isNotEmpty()) ticksBeforeHit.minOrNull()?.coerceIn(0, 5) ?: 0 else 0
        val tickOffMax = if (ticksBeforeHit.isNotEmpty()) ticksBeforeHit.maxOrNull()?.coerceIn(tickOffMin, 5) ?: 0 else 0

        val ticksAfterHit = attackSamples.filter { it.ticksBlockedBeforeHit > 0 }.map { it.ticksBlockedBeforeHit }
        val tickOnMin = if (ticksAfterHit.isNotEmpty()) ticksAfterHit.minOrNull()?.coerceIn(0, 5) ?: 0 else 0
        val tickOnMax = if (ticksAfterHit.isNotEmpty()) ticksAfterHit.maxOrNull()?.coerceIn(tickOnMin, 5) ?: 0 else 0

        // AutoBlock chance (how often does the player block when they could)
        val autoBlockChance = if (blockingSamples.isNotEmpty()) {
            (blockRate * 100).coerceIn(10f, 100f)
        } else 100f

        // === Inventory Analysis ===
        val inventoryAttacks = attackSamples.filter { it.attackedWhileInventoryOpen }
        val ignoreOpenInventory = inventoryAttacks.size.toFloat() / attackSamples.size.coerceAtLeast(1) > 0.1f

        // Simulate inventory closing - if attacking while inventory is open
        val simulateInventoryClosing = ignoreOpenInventory && inventoryAttacks.isNotEmpty()

        // === Rotation Analysis ===
        val rotationDeltas = samples.filter { it.rotationDeltaYaw != 0f || it.rotationDeltaPitch != 0f }
        val avgRotationSpeed = if (rotationDeltas.isNotEmpty()) {
            rotationDeltas.map { sqrt(it.rotationDeltaYaw * it.rotationDeltaYaw + it.rotationDeltaPitch * it.rotationDeltaPitch) }
                .average().toFloat()
        } else 30f

        // Rotation timing analysis
        val instantRotations = samples.count { it.timeToReachTargetRotation <= 1 }
        val rotationTiming = when {
            instantRotations.toFloat() / samples.size > 0.7f -> "OnTick" // Mostly instant rotations
            avgRotationSpeed > 45f -> "Snap" // Fast rotations
            else -> "Normal"
        }

        // === Target Selection Analysis ===
        val fovAngles = samples.filter { it.targetInFOV }.map { it.combatData.distance }
        val targetInFovRate = samples.count { it.targetInFOV }.toFloat() / samples.size
        val recommendedFOV = when {
            targetInFovRate > 0.95f -> 180f // Attacks targets all around
            targetInFovRate > 0.8f -> 120f
            targetInFovRate > 0.6f -> 90f
            else -> 60f
        }

        // HurtTime analysis
        val hurtTimes = samples.map { it.combatData.hurtTime }.filter { it >= 0 }
        val maxHurtTime = if (hurtTimes.isNotEmpty()) hurtTimes.maxOrNull()?.coerceIn(0, 10) ?: 10 else 10

        // Target priority analysis (based on what targets were selected)
        val targetPriority = analyzeTargetPriority(samples)

        // === NEW: Movement Correction Analysis ===
        val strafeAngles = samples.filter { it.strafeAngle > 0 }.map { it.strafeAngle }
        val strafeAngleAvg = if (strafeAngles.isNotEmpty()) strafeAngles.average().toFloat() else 0f
        val movementAlignedRate = samples.count { it.movementAlignedWithRotation }.toFloat() / samples.size
        val movementCorrectionMode = when {
            movementAlignedRate > 0.9f -> "Off" // Movement already aligned
            strafeAngleAvg > 30f -> "Silent" // High strafe, use silent correction
            else -> "Off"
        }

        // Rotation reset analysis
        val resetOccurrences = samples.count { it.rotationResetOccurred }
        val avgTicksSinceTarget = samples.filter { it.ticksSinceLastTarget > 0 }.map { it.ticksSinceLastTarget }.average()
        val resetThreshold = samples.filter { it.rotationDiffFromPlayer > 0 }
            .map { it.rotationDiffFromPlayer }
            .maxOrNull() ?: 2f
        val ticksUntilReset = if (avgTicksSinceTarget.isFinite()) avgTicksSinceTarget.toInt().coerceIn(1, 30) else 5

        // === NEW: Aim Point Analysis ===
        val headAimRate = samples.count { it.aimedAtHead }.toFloat() / samples.size
        val bodyAimRate = samples.count { it.aimedAtBody }.toFloat() / samples.size
        val feetAimRate = samples.count { it.aimedAtFeet }.toFloat() / samples.size

        val preferredAimPoint = when {
            headAimRate > 0.6f -> "Head"
            feetAimRate > 0.4f -> "Feet"
            else -> "Body"
        }

        val avgJitter = samples.filter { it.aimPointJitter > 0 }.map { it.aimPointJitter }.average()
        val aimPointJitterEnabled = avgJitter > 0.05 && avgJitter.isFinite()

        val stickyRate = samples.count { it.aimPointSticky }.toFloat() / samples.size
        val aimPointStickyEnabled = stickyRate > 0.5f

        // === NEW: Range Exit Analysis ===
        val exitingSamples = samples.filter { it.exitingRange }
        val attackedWhileExiting = exitingSamples.count { it.attackedWhileExiting }
        val ignoreWhenExitingRange = attackedWhileExiting.toFloat() / exitingSamples.size.coerceAtLeast(1) > 0.3f

        // === NEW: Shield Break Analysis ===
        val shieldSamples = samples.filter { it.targetBlockingWithShield }
        val attackedShielding = shieldSamples.count { it.attackedShieldingTarget }
        val ignoreOnShieldBreak = attackedShielding.toFloat() / shieldSamples.size.coerceAtLeast(1) > 0.5f

        // === NEW: Mace Smash Analysis ===
        val maceSamples = samples.filter { it.usingMace && it.maceSmashPossible }
        val ignoreOnMaceSmash = maceSamples.size.toFloat() / samples.size > 0.1f

        return ComprehensiveAnalysis(
            recommendedRange = avgRange.coerceIn(1f, 6f),
            recommendedWallRange = recommendedWallRange.coerceIn(0f, avgRange),
            recommendedScanExtraRange = scanExtraStart..scanExtraEnd,
            avgCPS = avgCPS,
            cpsRange = minCPS..maxCPS,
            clickPattern = clickPattern,
            attackCooldownEnabled = attackCooldownEnabled,
            critRate = critRate,
            usesCrits = usesCrits,
            critMode = critMode,
            keepsSprint = keepsSprint,
            sprintRate = sprintRate,
            wallHitRate = wallHitRate,
            raycastSuccessRate = raycastSuccessRate,
            raycastMode = raycastMode,
            aimThroughWalls = aimThroughWalls,
            missRate = missRate,
            avgMissDistance = avgMissDistance,
            failSwingEnabled = failSwingEnabled,
            failSwingRange = failSwingStart..failSwingEnd,
            autoBlockEnabled = autoBlockEnabled,
            autoBlockOnScanRange = autoBlockOnScanRange,
            tickOffRange = tickOffMin..tickOffMax,
            tickOnRange = tickOnMin..tickOnMax,
            blockRate = blockRate,
            autoBlockChance = autoBlockChance,
            ignoreOpenInventory = ignoreOpenInventory,
            simulateInventoryClosing = simulateInventoryClosing,
            rotationTiming = rotationTiming,
            avgRotationSpeed = avgRotationSpeed,
            recommendedFOV = recommendedFOV,
            recommendedHurtTime = maxHurtTime,
            targetPriority = targetPriority,
            // New fields
            movementCorrectionMode = movementCorrectionMode,
            strafeAngleAvg = strafeAngleAvg,
            resetThreshold = resetThreshold,
            ticksUntilReset = ticksUntilReset,
            preferredAimPoint = preferredAimPoint,
            aimPointJitterEnabled = aimPointJitterEnabled,
            aimPointStickyEnabled = aimPointStickyEnabled,
            ignoreWhenExitingRange = ignoreWhenExitingRange,
            ignoreOnShieldBreak = ignoreOnShieldBreak,
            ignoreOnMaceSmash = ignoreOnMaceSmash
        )
    }

    private fun analyzeClickPattern(intervals: List<Long>): String {
        if (intervals.size < 5) return "Stabilized"

        val avg = intervals.average()
        val variance = intervals.map { (it - avg) * (it - avg) }.average()
        val stdDev = sqrt(variance)
        val coefficientOfVariation = if (avg > 0) stdDev / avg else 0.0

        // Check for double clicks (very short intervals followed by normal)
        val shortIntervals = intervals.count { it < 30 }
        val hasDoubleClicks = shortIntervals.toFloat() / intervals.size > 0.2f

        // Check for bursts (clusters of fast clicks)
        val burstCount = intervals.windowed(3).count { window ->
            window.all { it < avg * 0.6 }
        }
        val hasBursts = burstCount.toFloat() / intervals.size > 0.1f

        return when {
            hasDoubleClicks -> "DoubleClick"
            hasBursts -> "Butterfly"
            coefficientOfVariation < 0.15 -> "Stabilized" // Very consistent
            coefficientOfVariation < 0.25 -> "Efficient"
            coefficientOfVariation > 0.4 -> "NormalDistribution" // High variance
            else -> "Stabilized"
        }
    }

    private fun analyzeTargetPriority(samples: List<KillAuraConfigSample>): String {
        // Analyze what type of targets are being selected
        val closestSelected = samples.count { it.closestTargetDistance <= it.actualRange * 1.1f }
        val closestRate = closestSelected.toFloat() / samples.size

        val lowHealthSelected = samples.count { it.targetHealth < 10f }
        val lowHealthRate = lowHealthSelected.toFloat() / samples.size

        return when {
            closestRate > 0.8f -> "Distance"
            lowHealthRate > 0.6f -> "Health"
            else -> "Distance" // Default
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun apply(analysis: ComprehensiveAnalysis) {
        chat("§6━━━━━ Applying Comprehensive KillAura Config ━━━━━")

        // === Apply Range Settings ===
        chat("§e▸ Range Settings:")
        applyFloatValue(ModuleKillAura, "range", analysis.recommendedRange)
        chat("§a  ✓ Range: §f${analysis.recommendedRange.format()}")

        applyFloatValue(ModuleKillAura, "wallRange", analysis.recommendedWallRange)
        chat("§a  ✓ WallRange: §f${analysis.recommendedWallRange.format()} §8(${(analysis.wallHitRate * 100).toInt()}% wall hits)")

        applyFloatRangeValue(ModuleKillAura, "scanExtraRange", analysis.recommendedScanExtraRange)
        chat("§a  ✓ ScanExtraRange: §f${analysis.recommendedScanExtraRange.start.format()}..${analysis.recommendedScanExtraRange.endInclusive.format()}")

        // === Apply Raycast Mode ===
        chat("§e▸ Raycast:")
        applyEnumValue(ModuleKillAura, "raycast", analysis.raycastMode)
        chat("§a  ✓ Raycast: §f${analysis.raycastMode}")

        // === Apply Combat Style ===
        chat("§e▸ Combat Style:")
        applyBooleanValue(ModuleKillAura, "keepSprint", analysis.keepsSprint)
        chat("§a  ✓ KeepSprint: §f${analysis.keepsSprint} §8(${(analysis.sprintRate * 100).toInt()}% sprint rate)")

        // Apply Criticals mode
        applyEnumValue(ModuleKillAura, "criticals", analysis.critMode)
        chat("§a  ✓ Criticals: §f${analysis.critMode} §8(${(analysis.critRate * 100).toInt()}% crit rate)")

        // === Apply CPS Settings ===
        chat("§e▸ Click Timing:")
        applyIntRangeValue(KillAuraClicker, "cps", analysis.cpsRange)
        chat("§a  ✓ CPS: §f${analysis.cpsRange.first}-${analysis.cpsRange.last} §8(avg: ${analysis.avgCPS.toInt()})")

        // Apply click pattern/technique
        applyEnumValue(KillAuraClicker, "technique", analysis.clickPattern)
        chat("§a  ✓ Technique: §f${analysis.clickPattern}")

        // Apply attack cooldown
        applyBooleanValue(KillAuraClicker, "attackCooldown", analysis.attackCooldownEnabled)
        chat("§a  ✓ AttackCooldown: §f${analysis.attackCooldownEnabled}")

        // === Apply Rotation Settings ===
        chat("§e▸ Rotations:")
        applyEnumValue(KillAuraRotationsConfigurable, "rotationTiming", analysis.rotationTiming)
        chat("§a  ✓ RotationTiming: §f${analysis.rotationTiming}")

        applyBooleanValue(KillAuraRotationsConfigurable, "throughWalls", analysis.aimThroughWalls)
        chat("§a  ✓ ThroughWalls: §f${analysis.aimThroughWalls}")

        // === Apply Target Settings ===
        chat("§e▸ Target:")
        applyFloatValue(KillAuraTargetTracker, "fov", analysis.recommendedFOV)
        chat("§a  ✓ FOV: §f${analysis.recommendedFOV.toInt()}°")

        applyIntValue(KillAuraTargetTracker, "hurtTime", analysis.recommendedHurtTime)
        chat("§a  ✓ HurtTime: §f${analysis.recommendedHurtTime}")

        // === Apply FailSwing Settings ===
        chat("§e▸ FailSwing:")
        if (analysis.failSwingEnabled) {
            KillAuraFailSwing.enabled = true
            applyFloatRangeValue(KillAuraFailSwing, "additionalRange", analysis.failSwingRange)
            chat("§a  ✓ FailSwing: §fEnabled")
            chat("§a  ✓ AdditionalRange: §f${analysis.failSwingRange.start.format()}..${analysis.failSwingRange.endInclusive.format()}")
        } else {
            KillAuraFailSwing.enabled = false
            chat("§7  ✓ FailSwing: §fDisabled §8(low miss rate)")
        }

        // === Apply AutoBlock Settings ===
        chat("§e▸ AutoBlock:")
        if (analysis.autoBlockEnabled) {
            KillAuraAutoBlock.enabled = true
            applyBooleanValue(KillAuraAutoBlock, "onScanRange", analysis.autoBlockOnScanRange)
            applyIntRangeValue(KillAuraAutoBlock, "tickOff", analysis.tickOffRange)
            applyIntRangeValue(KillAuraAutoBlock, "tickOn", analysis.tickOnRange)
            applyFloatValue(KillAuraAutoBlock, "chance", analysis.autoBlockChance)
            chat("§a  ✓ AutoBlock: §fEnabled §8(${(analysis.blockRate * 100).toInt()}% block rate)")
            chat("§a  ✓ OnScanRange: §f${analysis.autoBlockOnScanRange}")
            chat("§a  ✓ TickOff: §f${analysis.tickOffRange.first}..${analysis.tickOffRange.last}")
            chat("§a  ✓ TickOn: §f${analysis.tickOnRange.first}..${analysis.tickOnRange.last}")
            chat("§a  ✓ Chance: §f${analysis.autoBlockChance.toInt()}%")
        } else {
            KillAuraAutoBlock.enabled = false
            chat("§7  ✓ AutoBlock: §fDisabled §8(rarely blocks)")
        }

        // === Apply Inventory Settings ===
        chat("§e▸ Inventory:")
        applyBooleanValue(ModuleKillAura, "ignoreOpenInventory", analysis.ignoreOpenInventory)
        chat("§a  ✓ IgnoreOpenInventory: §f${analysis.ignoreOpenInventory}")

        applyBooleanValue(ModuleKillAura, "simulateInventoryClosing", analysis.simulateInventoryClosing)
        chat("§a  ✓ SimulateInventoryClosing: §f${analysis.simulateInventoryClosing}")

        // === Apply Movement Correction ===
        chat("§e▸ Movement Correction:")
        applyEnumValue(KillAuraRotationsConfigurable, "movementCorrection", analysis.movementCorrectionMode)
        chat("§a  ✓ MovementCorrection: §f${analysis.movementCorrectionMode}")

        // === Apply Rotation Reset Settings ===
        chat("§e▸ Rotation Reset:")
        applyFloatValue(KillAuraRotationsConfigurable, "resetThreshold", analysis.resetThreshold)
        chat("§a  ✓ ResetThreshold: §f${analysis.resetThreshold.format()}°")

        applyIntValue(KillAuraRotationsConfigurable, "ticksUntilReset", analysis.ticksUntilReset)
        chat("§a  ✓ TicksUntilReset: §f${analysis.ticksUntilReset}")

        // === Apply ItemCooldown Settings ===
        chat("§e▸ Item Cooldown:")
        val itemCooldown = KillAuraClicker.containedValues.find { it.name.equals("itemCooldown", true) }
        if (itemCooldown != null) {
            applyBooleanValue(itemCooldown as Configurable, "ignoreWhenExitingRange", analysis.ignoreWhenExitingRange)
            chat("§a  ✓ IgnoreWhenExitingRange: §f${analysis.ignoreWhenExitingRange}")

            applyBooleanValue(itemCooldown, "ignoreOnShieldBreak", analysis.ignoreOnShieldBreak)
            chat("§a  ✓ IgnoreOnShieldBreak: §f${analysis.ignoreOnShieldBreak}")

            applyBooleanValue(itemCooldown, "ignoreOnMaceSmash", analysis.ignoreOnMaceSmash)
            chat("§a  ✓ IgnoreOnMaceSmash: §f${analysis.ignoreOnMaceSmash}")
        }

        // === Report Accuracy ===
        chat("§e▸ Accuracy Stats:")
        chat("§7  Raycast Success: §f${(analysis.raycastSuccessRate * 100).toInt()}%")
        chat("§7  Miss Rate: §f${(analysis.missRate * 100).toInt()}%")
        if (analysis.missRate > 0) {
            chat("§7  Avg Miss Distance: §f${analysis.avgMissDistance.format()}")
        }
        chat("§7  Avg Rotation Speed: §f${analysis.avgRotationSpeed.format()}°/tick")

        chat("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        chat("§a✓ All settings applied successfully!")
    }

    fun report(analysis: ComprehensiveAnalysis?): String {
        if (analysis == null) return "✗ Comprehensive Analysis: Insufficient data"

        return buildString {
            append("§6╔═══ Comprehensive KillAura Analysis ═══\n")
            append("§6║\n")
            append("§6║ §eRange Settings: §8(will apply)\n")
            append("§6║   Range: §f${analysis.recommendedRange.format()}\n")
            append("§6║   WallRange: §f${analysis.recommendedWallRange.format()} §8(${(analysis.wallHitRate * 100).toInt()}% walls)\n")
            append("§6║   ScanExtraRange: §f${analysis.recommendedScanExtraRange.start.format()}..${analysis.recommendedScanExtraRange.endInclusive.format()}\n")
            append("§6║   Raycast: §f${analysis.raycastMode}\n")
            append("§6║\n")
            append("§6║ §eClick Timing: §8(will apply)\n")
            append("§6║   CPS: §f${analysis.cpsRange.first}-${analysis.cpsRange.last} §8(avg: ${analysis.avgCPS.toInt()})\n")
            append("§6║   Technique: §f${analysis.clickPattern}\n")
            append("§6║   AttackCooldown: §f${analysis.attackCooldownEnabled}\n")
            append("§6║\n")
            append("§6║ §eCombat Style: §8(will apply)\n")
            append("§6║   Criticals: §f${analysis.critMode} §8(${(analysis.critRate * 100).toInt()}% rate)\n")
            append("§6║   KeepSprint: §f${analysis.keepsSprint} §8(${(analysis.sprintRate * 100).toInt()}% sprint)\n")
            append("§6║\n")
            append("§6║ §eRotations: §8(will apply)\n")
            append("§6║   Timing: §f${analysis.rotationTiming}\n")
            append("§6║   ThroughWalls: §f${analysis.aimThroughWalls}\n")
            append("§6║   Avg Speed: §f${analysis.avgRotationSpeed.format()}°/tick\n")
            append("§6║\n")
            append("§6║ §eTarget: §8(will apply)\n")
            append("§6║   FOV: §f${analysis.recommendedFOV.toInt()}°\n")
            append("§6║   HurtTime: §f${analysis.recommendedHurtTime}\n")
            append("§6║   Priority: §f${analysis.targetPriority}\n")
            append("§6║\n")
            append("§6║ §eFailSwing: §8(will apply)\n")
            append("§6║   Enabled: §f${analysis.failSwingEnabled}\n")
            if (analysis.failSwingEnabled) {
                append("§6║   AdditionalRange: §f${analysis.failSwingRange.start.format()}..${analysis.failSwingRange.endInclusive.format()}\n")
            }
            append("§6║\n")
            append("§6║ §eAutoBlock: §8(will apply)\n")
            append("§6║   Enabled: §f${analysis.autoBlockEnabled} §8(${(analysis.blockRate * 100).toInt()}% rate)\n")
            if (analysis.autoBlockEnabled) {
                append("§6║   OnScanRange: §f${analysis.autoBlockOnScanRange}\n")
                append("§6║   TickOff: §f${analysis.tickOffRange.first}..${analysis.tickOffRange.last}\n")
                append("§6║   TickOn: §f${analysis.tickOnRange.first}..${analysis.tickOnRange.last}\n")
                append("§6║   Chance: §f${analysis.autoBlockChance.toInt()}%\n")
            }
            append("§6║\n")
            append("§6║ §eInventory: §8(will apply)\n")
            append("§6║   IgnoreOpenInventory: §f${analysis.ignoreOpenInventory}\n")
            append("§6║   SimulateInventoryClosing: §f${analysis.simulateInventoryClosing}\n")
            append("§6║\n")
            append("§6║ §eAccuracy:\n")
            append("§6║   Raycast Success: §f${(analysis.raycastSuccessRate * 100).toInt()}%\n")
            append("§6║   Miss Rate: §f${(analysis.missRate * 100).toInt()}%\n")
            if (analysis.missRate > 0) {
                append("§6║   Avg Miss Distance: §f${analysis.avgMissDistance.format()}\n")
            }
            append("§6╚════════════════════════════════════════")
        }
    }

    // === Helper functions to apply values ===

    @Suppress("UNCHECKED_CAST")
    private fun applyFloatValue(configurable: Configurable, name: String, value: Float): Boolean {
        return try {
            val valueObj = configurable.containedValues.find {
                it.name.equals(name, ignoreCase = true)
            } as? Value<Float>

            if (valueObj != null) {
                valueObj.set(value)
                true
            } else {
                logger.warn("Float value $name not found in ${configurable.name}")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to set $name to $value", e)
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyIntValue(configurable: Configurable, name: String, value: Int): Boolean {
        return try {
            val valueObj = configurable.containedValues.find {
                it.name.equals(name, ignoreCase = true)
            } as? Value<Int>

            if (valueObj != null) {
                valueObj.set(value)
                true
            } else {
                logger.warn("Int value $name not found in ${configurable.name}")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to set $name to $value", e)
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyBooleanValue(configurable: Configurable, name: String, value: Boolean): Boolean {
        return try {
            val valueObj = configurable.containedValues.find {
                it.name.equals(name, ignoreCase = true)
            } as? Value<Boolean>

            if (valueObj != null) {
                valueObj.set(value)
                true
            } else {
                logger.warn("Boolean value $name not found in ${configurable.name}")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to set $name to $value", e)
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyFloatRangeValue(configurable: Configurable, name: String, range: ClosedFloatingPointRange<Float>): Boolean {
        return try {
            val valueObj = configurable.containedValues.find {
                it.name.equals(name, ignoreCase = true)
            } as? Value<ClosedFloatingPointRange<Float>>

            if (valueObj != null) {
                valueObj.set(range)
                true
            } else {
                logger.warn("Float range value $name not found in ${configurable.name}")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to set $name to $range", e)
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyIntRangeValue(configurable: Configurable, name: String, range: IntRange): Boolean {
        return try {
            val valueObj = configurable.containedValues.find {
                it.name.equals(name, ignoreCase = true)
            } as? Value<IntRange>

            if (valueObj != null) {
                valueObj.set(range)
                true
            } else {
                logger.warn("Int range value $name not found in ${configurable.name}")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to set $name to $range", e)
            false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyEnumValue(configurable: Configurable, name: String, value: String): Boolean {
        return try {
            val valueObj = configurable.containedValues.find {
                it.name.equals(name, ignoreCase = true)
            } as? Value<NamedChoice>

            if (valueObj != null) {
                // Find the enum value that matches the string
                val currentValue = valueObj.get()
                val enumClass = currentValue::class.java
                val enumConstants = enumClass.enumConstants as? Array<out NamedChoice>

                val matchingConstant = enumConstants?.find {
                    it.choiceName.equals(value, ignoreCase = true)
                }

                if (matchingConstant != null) {
                    (valueObj as Value<Any>).set(matchingConstant)
                    true
                } else {
                    logger.warn("Enum value $value not found for $name in ${configurable.name}")
                    false
                }
            } else {
                logger.warn("Enum value $name not found in ${configurable.name}")
                false
            }
        } catch (e: Exception) {
            logger.error("Failed to set $name to $value", e)
            false
        }
    }

    private fun Float.format() = "%.2f".format(this)
}
