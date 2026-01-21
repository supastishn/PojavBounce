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

object SigmoidModeAnalyzer : KillAuraAnalyzer {

    override fun analyze(samples: List<CombatSample>): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult("SigmoidMode", emptyMap(), emptyMap(), 0f)
        }

        // Sigmoid: easing curve smoothing
        val yawDeltas = samples.map { it.totalDelta.deltaYaw.toDouble() }
        val pitchDeltas = samples.map { it.totalDelta.deltaPitch.toDouble() }

        val avgYaw = kotlin.math.abs(yawDeltas.average())
        val avgPitch = kotlin.math.abs(pitchDeltas.average())
        val variance = kotlin.math.sqrt(
            yawDeltas.map { (it - avgYaw) * (it - avgYaw) }.average() +
            pitchDeltas.map { (it - avgPitch) * (it - avgPitch) }.average()
        )

        // Sigmoid curve steepness: continuous mapping based on variance (0-20 range)
        // Higher variance = steeper curve (faster transition)
        // Map variance to steepness: 0 variance -> 5, 20 variance -> 18
        val varianceNormalized = (variance / 20.0).coerceIn(0.0, 1.0)
        val recommendedSteepness = (5.0 + (varianceNormalized * 13.0)).coerceIn(0.0, 20.0)

        // Midpoint based on average rotation magnitude (0-1 range)
        // Higher rotation = higher midpoint (curve transitions later)
        // Map avgMagnitude to midpoint: 0° -> 0.25, 60° -> 0.45
        val avgMagnitude = kotlin.math.sqrt(avgYaw * avgYaw + avgPitch * avgPitch)
        val magnitudeNormalized = (avgMagnitude / 60.0).coerceIn(0.0, 1.0)
        val recommendedMidpoint = (0.25 + (magnitudeNormalized * 0.20)).coerceIn(0.0, 1.0)

        // Turn speed based on actual velocity per tick
        val yawSpeeds = samples.map { kotlin.math.abs(it.velocityDelta.x.toDouble()) }
        val pitchSpeeds = samples.map { kotlin.math.abs(it.velocityDelta.y.toDouble()) }

        // Use P60 and P90 for ranges, exact values from training
        val sortedYaw = yawSpeeds.sorted()
        val sortedPitch = pitchSpeeds.sorted()

        val yawP60 = sortedYaw[(sortedYaw.size * 0.60).toInt().coerceIn(0, sortedYaw.size - 1)]
        val yawP90 = sortedYaw[(sortedYaw.size * 0.90).toInt().coerceIn(0, sortedYaw.size - 1)]
        val pitchP60 = sortedPitch[(sortedPitch.size * 0.60).toInt().coerceIn(0, sortedPitch.size - 1)]
        val pitchP90 = sortedPitch[(sortedPitch.size * 0.90).toInt().coerceIn(0, sortedPitch.size - 1)]

        // IMPORTANT: Sigmoid mode multiplies TurnSpeed by sigmoid curve (0-1 factor)
        // Calculate average sigmoid factor dynamically based on recommended steepness/midpoint
        // instead of using a hardcoded 0.6
        //
        // Sigmoid formula: sigmoid = 1 / (1 + exp(-steepness * (scaledDiff - midpoint)))
        // Average input t ≈ 0.3 (36° on 120° typical rotation, from scaledDifference = rotationDiff / 120f)
        val avgScaledDiff = 0.3
        val avgSigmoidFactor = (1.0 / (1.0 + kotlin.math.exp(-recommendedSteepness * (avgScaledDiff - recommendedMidpoint))))
            .coerceIn(0.3, 0.9)

        val horizontalSpeedStart = (yawP60 / avgSigmoidFactor).coerceIn(0.1, 180.0).toFloat()
        val horizontalSpeed = (yawP90 / avgSigmoidFactor).coerceIn(horizontalSpeedStart.toDouble() + 0.5, 180.0).toFloat()
        val verticalSpeedStart = (pitchP60 / avgSigmoidFactor).coerceIn(0.1, 180.0).toFloat()
        val verticalSpeed = (pitchP90 / avgSigmoidFactor).coerceIn(verticalSpeedStart.toDouble() + 0.5, 180.0).toFloat()

        val changes = mutableMapOf<String, SettingChange>()

        changes["steepness"] = SettingChange(
            "Steepness",
            "Current",
            "%.1f".format(recommendedSteepness),
            "Variance: ${"%.2f".format(variance)}° → continuous steepness"
        )

        changes["midpoint"] = SettingChange(
            "Midpoint",
            "Current",
            "%.2f".format(recommendedMidpoint),
            "Avg magnitude: ${"%.2f".format(avgMagnitude)}° → continuous midpoint"
        )

        changes["horizontalTurnSpeed"] = SettingChange(
            "HorizontalTurnSpeed",
            "Current",
            "${"%.1f".format(horizontalSpeedStart)}..${"%.1f".format(horizontalSpeed)}",
            "From P60-P90 yaw: ${"%.2f".format(yawP60)}-${"%.2f".format(yawP90)}°/tick ÷ ${"%.2f".format(avgSigmoidFactor)} sigmoid factor"
        )

        changes["verticalTurnSpeed"] = SettingChange(
            "VerticalTurnSpeed",
            "Current",
            "${"%.1f".format(verticalSpeedStart)}..${"%.1f".format(verticalSpeed)}",
            "From P60-P90 pitch: ${"%.2f".format(pitchP60)}-${"%.2f".format(pitchP90)}°/tick ÷ ${"%.2f".format(avgSigmoidFactor)} sigmoid factor"
        )

        val stats = mapOf(
            "variance" to variance,
            "recommendedSteepness" to recommendedSteepness,
            "recommendedMidpoint" to recommendedMidpoint,
            "avgSigmoidFactor" to avgSigmoidFactor,
            "horizontalSpeedStart" to horizontalSpeedStart.toDouble(),
            "horizontalSpeed" to horizontalSpeed.toDouble(),
            "verticalSpeedStart" to verticalSpeedStart.toDouble(),
            "verticalSpeed" to verticalSpeed.toDouble(),
            "yawP60" to yawP60,
            "yawP90" to yawP90,
            "pitchP60" to pitchP60,
            "pitchP90" to pitchP90,
            "avgMagnitude" to avgMagnitude
        )

        return AnalysisResult("SigmoidMode", changes, stats, 0.80f)
    }

    override fun apply(result: AnalysisResult) {
        if (result.stats.isEmpty()) return

        // Set rotation mode to Sigmoid
        if (!AutoConfigApplier.setRotationMode("Sigmoid")) {
            chat("§c✗ Failed to set Sigmoid mode")
            return
        }

        // Apply steepness
        val steepness = result.stats["recommendedSteepness"]?.toFloat() ?: 10f
        if (AutoConfigApplier.setFloatValue("Steepness", steepness)) {
            chat("§a  ✓ Steepness: ${"%.1f".format(steepness)}")
        }

        // Apply midpoint
        val midpoint = result.stats["recommendedMidpoint"]?.toFloat() ?: 0.3f
        if (AutoConfigApplier.setFloatValue("Midpoint", midpoint)) {
            chat("§a  ✓ Midpoint: ${"%.2f".format(midpoint)}")
        }

        // Apply horizontal turn speed
        val horizontalSpeedStart = result.stats["horizontalSpeedStart"]?.toFloat() ?: 30f
        val horizontalSpeed = result.stats["horizontalSpeed"]?.toFloat() ?: 60f
        val horizontalRange = horizontalSpeedStart..horizontalSpeed

        if (AutoConfigApplier.setFloatRangeValue("HorizontalTurnSpeed", horizontalRange)) {
            chat("§a  ✓ HorizontalTurnSpeed: ${"%.1f".format(horizontalRange.start)}..${"%.1f".format(horizontalRange.endInclusive)}")
        }

        // Apply vertical turn speed
        val verticalSpeedStart = result.stats["verticalSpeedStart"]?.toFloat() ?: 20f
        val verticalSpeed = result.stats["verticalSpeed"]?.toFloat() ?: 40f
        val verticalRange = verticalSpeedStart..verticalSpeed

        if (AutoConfigApplier.setFloatRangeValue("VerticalTurnSpeed", verticalRange)) {
            chat("§a  ✓ VerticalTurnSpeed: ${"%.1f".format(verticalRange.start)}..${"%.1f".format(verticalRange.endInclusive)}")
        }
    }

    override fun report(result: AnalysisResult): String {
        if (result.changes.isEmpty()) {
            return "✗ Sigmoid Mode: Insufficient data"
        }

        val steepness = result.changes["steepness"]
        val midpoint = result.changes["midpoint"]
        val hSpeed = result.changes["horizontalTurnSpeed"]
        val vSpeed = result.changes["verticalTurnSpeed"]
        val variance = result.stats["variance"]?.let { "%.2f".format(it) } ?: "?"

        return buildString {
            append("§5╔ Sigmoid Mode Configuration\n")
            if (steepness != null) {
                append("§5║ Steepness: §7${steepness.newValue}\n")
            }
            if (midpoint != null) {
                append("§5║ Midpoint: §7${midpoint.newValue}\n")
            }
            if (hSpeed != null) {
                append("§5║ HorizontalTurnSpeed: §7${hSpeed.newValue}\n")
            }
            if (vSpeed != null) {
                append("§5║ VerticalTurnSpeed: §7${vSpeed.newValue}\n")
            }
            append("§5║ Variance: §7${variance}°\n")
            append("§5╚ Sigmoid easing - smooth acceleration curves")
        }
    }
}
