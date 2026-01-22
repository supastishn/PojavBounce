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

package net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.modes

import net.ccbluex.liquidbounce.deeplearn.data.CombatSample
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.AnalysisResult
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.AutoConfigApplier
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.KillAuraAnalyzer
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.SettingChange
import net.ccbluex.liquidbounce.utils.client.chat

object InterpolationModeAnalyzer : KillAuraAnalyzer {

    override fun analyze(samples: List<CombatSample>): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult("InterpolationMode", emptyMap(), emptyMap(), 0f)
        }

        // Interpolation: spline-based smoothing with Bezier/Sigmoid combination
        val yawDeltas = samples.map { it.totalDelta.deltaYaw.toDouble() }
        val pitchDeltas = samples.map { it.totalDelta.deltaPitch.toDouble() }

        val velocityDeltas = samples.map { sample ->
            kotlin.math.sqrt(
                sample.velocityDelta.x * sample.velocityDelta.x +
                sample.velocityDelta.y * sample.velocityDelta.y
            ).toDouble()
        }

        val avgVelocity = velocityDeltas.average()
        val maxVelocity = velocityDeltas.maxOrNull() ?: 0.0

        val avgYaw = kotlin.math.abs(yawDeltas.average())
        val avgPitch = kotlin.math.abs(pitchDeltas.average())

        // Calculate variance for direction change factor
        val yawVar = yawDeltas.map { (it - avgYaw) * (it - avgYaw) }.average()
        val pitchVar = pitchDeltas.map { (it - avgPitch) * (it - avgPitch) }.average()
        val variance = kotlin.math.sqrt(yawVar + pitchVar)

        // CRITICAL FIX: Calculate what % of remaining distance was covered each tick
        // InterpolationAngleSmooth expects percentage (1-100%) of remaining distance per tick
        // NOT degrees/tick divided by 180!
        //
        // IMPORTANT: Filter out samples where:
        // 1. remaining < 5° (cursor already on target - would skew toward slow values)
        // 2. moved < 0.1° (no significant movement)
        // 3. remaining > 90° (extreme flicks - likely target switching, not normal aiming)
        val minRemaining = 5f   // Skip "already on target" samples
        val maxRemaining = 90f  // Skip extreme flicks
        val minMoved = 0.1f     // Skip no-movement samples

        val yawPercentages = samples.mapNotNull { sample ->
            val remaining = kotlin.math.abs(sample.totalDelta.deltaYaw)
            val moved = kotlin.math.abs(sample.velocityDelta.x)
            // Only include active targeting samples (not already on target, not extreme flicks)
            if (remaining in minRemaining..maxRemaining && moved > minMoved) {
                (moved / remaining * 100.0)
            } else null
        }
        val pitchPercentages = samples.mapNotNull { sample ->
            val remaining = kotlin.math.abs(sample.totalDelta.deltaPitch)
            val moved = kotlin.math.abs(sample.velocityDelta.y)
            if (remaining in minRemaining..maxRemaining && moved > minMoved) {
                (moved / remaining * 100.0)
            } else null
        }

        // Use percentiles from the calculated percentages
        val sortedYawPct = yawPercentages.sorted()
        val sortedPitchPct = pitchPercentages.sorted()

        // Use P65 as center (not P50) to skip slow "already locked on" samples
        // P50 gets skewed by intentionally slow movements when tracking
        val yawP65 = if (sortedYawPct.isNotEmpty())
            sortedYawPct[(sortedYawPct.size * 0.65).toInt().coerceIn(0, sortedYawPct.size - 1)]
        else 35.0
        val pitchP65 = if (sortedPitchPct.isNotEmpty())
            sortedPitchPct[(sortedPitchPct.size * 0.65).toInt().coerceIn(0, sortedPitchPct.size - 1)]
        else 25.0

        // Calculate standard deviation to understand the actual variance in the data
        val yawStdDev = if (yawPercentages.size > 1) {
            val mean = yawPercentages.average()
            kotlin.math.sqrt(yawPercentages.map { (it - mean) * (it - mean) }.average())
        } else 5.0
        val pitchStdDev = if (pitchPercentages.size > 1) {
            val mean = pitchPercentages.average()
            kotlin.math.sqrt(pitchPercentages.map { (it - mean) * (it - mean) }.average())
        } else 5.0

        // Clamp variation to max 10% (or 10 percentage points) for consistency
        val maxVariation = 10.0
        val yawVariation = kotlin.math.min(yawStdDev * 0.5, maxVariation)  // Half stddev, max 10
        val pitchVariation = kotlin.math.min(pitchStdDev * 0.5, maxVariation)

        // Create narrow range centered on P65
        val horizontalSpeedStart = (yawP65 - yawVariation).coerceIn(1.0, 100.0).toInt()
        val horizontalSpeedEnd = (yawP65 + yawVariation).coerceIn(1.0, 100.0).toInt()
            .coerceAtLeast(horizontalSpeedStart + 3) // Ensure minimum 3% range

        val verticalSpeedStart = (pitchP65 - pitchVariation).coerceIn(1.0, 100.0).toInt()
        val verticalSpeedEnd = (pitchP65 + pitchVariation).coerceIn(1.0, 100.0).toInt()
            .coerceAtLeast(verticalSpeedStart + 3)

        // Direction change factor - use actual variance from data
        // Low variance = high factor (consistent aim), high variance = lower factor
        val combinedStdDev = (yawStdDev + pitchStdDev) / 2.0
        val varianceNormalized = (combinedStdDev / 30.0).coerceIn(0.0, 1.0)  // Normalize: 30% stddev = max
        val dcFactorBase = (95 - (varianceNormalized * 20)).toInt().coerceIn(75, 95)
        val dcFactorEnd = (dcFactorBase + 5).coerceIn(dcFactorBase, 100)

        // Midpoint: controls Bezier vs Sigmoid transition
        // Based on how quickly the player typically acquires targets
        // Higher P65 % = faster acquisition = higher midpoint (more Bezier, less Sigmoid slowdown)
        val avgP65 = (yawP65 + pitchP65) / 2.0
        val recommendedMidpoint = when {
            avgP65 > 50 -> 0.45f  // Fast aimer - use Bezier longer
            avgP65 > 30 -> 0.35f  // Medium aimer - balanced
            avgP65 > 15 -> 0.28f  // Slow aimer - more Sigmoid precision
            else -> 0.22f         // Very slow - mostly Sigmoid
        }

        val changes = mutableMapOf<String, SettingChange>()

        changes["horizontalSpeed"] = SettingChange(
            "HorizontalSpeed",
            "Current",
            "${horizontalSpeedStart}..${horizontalSpeedEnd}%",
            "P65: ${"%.1f".format(yawP65)}% ± ${"%.1f".format(yawVariation)}%"
        )

        changes["verticalSpeed"] = SettingChange(
            "VerticalSpeed",
            "Current",
            "${verticalSpeedStart}..${verticalSpeedEnd}%",
            "P65: ${"%.1f".format(pitchP65)}% ± ${"%.1f".format(pitchVariation)}%"
        )

        changes["directionChangeFactor"] = SettingChange(
            "DirectionChangeFactor",
            "Current",
            "${dcFactorBase}..${dcFactorEnd}%",
            "StdDev: ${"%.1f".format(combinedStdDev)}% → factor: ${dcFactorBase}%"
        )

        changes["midpoint"] = SettingChange(
            "Midpoint",
            "Current",
            "%.2f".format(recommendedMidpoint),
            "Avg P65: ${"%.1f".format(avgP65)}% → ${if (avgP65 > 30) "faster" else "slower"} transition"
        )

        val stats = mapOf(
            "avgVelocity" to avgVelocity,
            "maxVelocity" to maxVelocity,
            "variance" to variance,
            "horizontalSpeedStart" to horizontalSpeedStart.toDouble(),
            "horizontalSpeedEnd" to horizontalSpeedEnd.toDouble(),
            "verticalSpeedStart" to verticalSpeedStart.toDouble(),
            "verticalSpeedEnd" to verticalSpeedEnd.toDouble(),
            "directionChangeStart" to dcFactorBase.toDouble(),
            "directionChangeEnd" to dcFactorEnd.toDouble(),
            "recommendedMidpoint" to recommendedMidpoint.toDouble(),
            "yawP65" to yawP65,
            "pitchP65" to pitchP65,
            "yawStdDev" to yawStdDev,
            "pitchStdDev" to pitchStdDev,
            "yawVariation" to yawVariation,
            "pitchVariation" to pitchVariation,
            // Debug stats
            "totalSamples" to samples.size.toDouble(),
            "validYawSamples" to yawPercentages.size.toDouble(),
            "validPitchSamples" to pitchPercentages.size.toDouble()
        )

        return AnalysisResult("InterpolationMode", changes, stats, 0.75f)
    }

    override fun apply(result: AnalysisResult) {
        if (result.stats.isEmpty()) return

        // Set rotation mode to Interpolation
        if (!AutoConfigApplier.setRotationMode("Interpolation")) {
            chat("§c✗ Failed to set Interpolation mode")
            return
        }

        // Apply horizontal speed (IntRange)
        val hStart = result.stats["horizontalSpeedStart"]?.toInt() ?: 80
        val hEnd = result.stats["horizontalSpeedEnd"]?.toInt() ?: 85
        if (AutoConfigApplier.setIntRangeValue("HorizontalSpeed", hStart..hEnd)) {
            chat("§a  ✓ HorizontalSpeed: ${hStart}..${hEnd}%")
        }

        // Apply vertical speed (IntRange)
        val vStart = result.stats["verticalSpeedStart"]?.toInt() ?: 20
        val vEnd = result.stats["verticalSpeedEnd"]?.toInt() ?: 25
        if (AutoConfigApplier.setIntRangeValue("VerticalSpeed", vStart..vEnd)) {
            chat("§a  ✓ VerticalSpeed: ${vStart}..${vEnd}%")
        }

        // Apply direction change factor (IntRange)
        val dcStart = result.stats["directionChangeStart"]?.toInt() ?: 95
        val dcEnd = result.stats["directionChangeEnd"]?.toInt() ?: 100
        if (AutoConfigApplier.setIntRangeValue("DirectionChangeFactor", dcStart..dcEnd)) {
            chat("§a  ✓ DirectionChangeFactor: ${dcStart}..${dcEnd}%")
        }

        // Apply midpoint
        val midpoint = result.stats["recommendedMidpoint"]?.toFloat() ?: 0.35f
        if (AutoConfigApplier.setFloatValue("Midpoint", midpoint)) {
            chat("§a  ✓ Midpoint: ${"%.2f".format(midpoint)}")
        }
    }

    override fun report(result: AnalysisResult): String {
        if (result.changes.isEmpty()) {
            return "✗ Interpolation Mode: Insufficient data"
        }

        val hSpeed = result.changes["horizontalSpeed"]
        val vSpeed = result.changes["verticalSpeed"]
        val dcFactor = result.changes["directionChangeFactor"]
        val midpoint = result.changes["midpoint"]
        val avgVel = result.stats["avgVelocity"]?.let { "%.2f".format(it) } ?: "?"
        val maxVel = result.stats["maxVelocity"]?.let { "%.2f".format(it) } ?: "?"
        val totalSamples = result.stats["totalSamples"]?.toInt() ?: 0
        val validYaw = result.stats["validYawSamples"]?.toInt() ?: 0
        val validPitch = result.stats["validPitchSamples"]?.toInt() ?: 0

        return buildString {
            append("§d╔ Interpolation Mode Configuration\n")
            if (hSpeed != null) {
                append("§d║ HorizontalSpeed: §7${hSpeed.newValue}\n")
                append("§d║   §8(${hSpeed.reasoning})\n")
            }
            if (vSpeed != null) {
                append("§d║ VerticalSpeed: §7${vSpeed.newValue}\n")
                append("§d║   §8(${vSpeed.reasoning})\n")
            }
            if (dcFactor != null) {
                append("§d║ DirectionChangeFactor: §7${dcFactor.newValue}\n")
            }
            if (midpoint != null) {
                append("§d║ Midpoint: §7${midpoint.newValue}\n")
            }
            append("§d║ Velocity: Avg=${avgVel}, Max=${maxVel}\n")
            append("§d║ Samples: ${totalSamples} total, ${validYaw} valid yaw, ${validPitch} valid pitch\n")
            append("§d╚ Bezier/Sigmoid interpolation - smooth curved paths")
        }
    }
}
