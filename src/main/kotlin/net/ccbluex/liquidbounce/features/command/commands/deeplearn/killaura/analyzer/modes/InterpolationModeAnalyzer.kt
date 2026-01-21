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
        // 1. remaining < 1° (avoid division by small numbers)
        // 2. moved < 0.1° (no significant movement, would add 0% and skew percentiles down)
        val yawPercentages = samples.mapNotNull { sample ->
            val remaining = kotlin.math.abs(sample.totalDelta.deltaYaw)
            val moved = kotlin.math.abs(sample.velocityDelta.x)
            // Filter out samples with no significant movement or small remaining distance
            if (remaining > 1f && moved > 0.1f) (moved / remaining * 100.0) else null
        }
        val pitchPercentages = samples.mapNotNull { sample ->
            val remaining = kotlin.math.abs(sample.totalDelta.deltaPitch)
            val moved = kotlin.math.abs(sample.velocityDelta.y)
            if (remaining > 1f && moved > 0.1f) (moved / remaining * 100.0) else null
        }

        // Use percentiles from the calculated percentages
        val sortedYawPct = yawPercentages.sorted()
        val sortedPitchPct = pitchPercentages.sorted()

        // P25 and P75 for ranges - these are now actual percentages of remaining distance
        val yawP25 = if (sortedYawPct.isNotEmpty())
            sortedYawPct[(sortedYawPct.size * 0.25).toInt().coerceIn(0, sortedYawPct.size - 1)]
        else 30.0
        val yawP75 = if (sortedYawPct.isNotEmpty())
            sortedYawPct[(sortedYawPct.size * 0.75).toInt().coerceIn(0, sortedYawPct.size - 1)]
        else 60.0
        val pitchP25 = if (sortedPitchPct.isNotEmpty())
            sortedPitchPct[(sortedPitchPct.size * 0.25).toInt().coerceIn(0, sortedPitchPct.size - 1)]
        else 20.0
        val pitchP75 = if (sortedPitchPct.isNotEmpty())
            sortedPitchPct[(sortedPitchPct.size * 0.75).toInt().coerceIn(0, sortedPitchPct.size - 1)]
        else 40.0

        // These are now correctly scaled percentages (1-100%)
        val horizontalSpeedStart = yawP25.coerceIn(1.0, 100.0).toInt()
        val horizontalSpeedEnd = yawP75.coerceIn(1.0, 100.0).toInt()
            .coerceAtLeast(horizontalSpeedStart + 5) // Ensure range

        val verticalSpeedStart = pitchP25.coerceIn(1.0, 100.0).toInt()
        val verticalSpeedEnd = pitchP75.coerceIn(1.0, 100.0).toInt()
            .coerceAtLeast(verticalSpeedStart + 5)

        // Direction change factor based on variance - continuous calculation
        // High variance = lower factor (more correction needed)
        val varianceNormalized = (variance / 50.0).coerceIn(0.0, 1.0) // Normalize to 0-1
        val dcFactorBase = (100 - (varianceNormalized * 30)).toInt().coerceIn(70, 100)
        val dcFactorEnd = (dcFactorBase + 5).coerceIn(dcFactorBase, 100)

        // Midpoint based on average rotation magnitude - continuous
        val avgRotationMag = kotlin.math.sqrt(avgYaw * avgYaw + avgPitch * avgPitch)
        val recommendedMidpoint = (0.25 + (avgRotationMag / 180.0) * 0.25).coerceIn(0.2, 0.5).toFloat()

        val changes = mutableMapOf<String, SettingChange>()

        changes["horizontalSpeed"] = SettingChange(
            "HorizontalSpeed",
            "Current",
            "${horizontalSpeedStart}..${horizontalSpeedEnd}%",
            "From P25-P75 yaw %/tick: ${"%.1f".format(yawP25)}-${"%.1f".format(yawP75)}%"
        )

        changes["verticalSpeed"] = SettingChange(
            "VerticalSpeed",
            "Current",
            "${verticalSpeedStart}..${verticalSpeedEnd}%",
            "From P25-P75 pitch %/tick: ${"%.1f".format(pitchP25)}-${"%.1f".format(pitchP75)}%"
        )

        changes["directionChangeFactor"] = SettingChange(
            "DirectionChangeFactor",
            "Current",
            "${dcFactorBase}..${dcFactorEnd}%",
            "Variance: ${"%.2f".format(variance)}° → factor: ${dcFactorBase}%"
        )

        changes["midpoint"] = SettingChange(
            "Midpoint",
            "Current",
            "%.2f".format(recommendedMidpoint),
            "Avg rotation: ${"%.2f".format(avgRotationMag)}°"
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
            "yawP25" to yawP25,
            "yawP75" to yawP75,
            "pitchP25" to pitchP25,
            "pitchP75" to pitchP75,
            "avgRotationMag" to avgRotationMag,
            // Debug stats to diagnose calculation issues
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
