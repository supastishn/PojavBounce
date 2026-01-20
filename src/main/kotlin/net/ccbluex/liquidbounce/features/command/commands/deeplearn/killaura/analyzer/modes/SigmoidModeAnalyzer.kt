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

        // Sigmoid curve steepness based on variance (0-20 range)
        val recommendedSteepness = when {
            variance > 15.0 -> 15.0  // Steep curve for high variance
            variance > 10.0 -> 12.0
            variance > 5.0 -> 10.0   // Medium curve
            else -> 8.0              // Gentle curve
        }.coerceIn(0.0, 20.0)

        // Midpoint based on average rotation magnitude (0-1 range)
        val avgMagnitude = kotlin.math.sqrt(avgYaw * avgYaw + avgPitch * avgPitch)
        val recommendedMidpoint = when {
            avgMagnitude > 30.0 -> 0.4  // Higher midpoint for faster rotations
            avgMagnitude > 15.0 -> 0.35
            else -> 0.3                 // Lower midpoint for slower rotations
        }.coerceIn(0.0, 1.0)

        // Turn speed recommendations
        val avgSpeed = kotlin.math.sqrt(avgYaw * avgYaw + avgPitch * avgPitch).coerceIn(20.0, 180.0)
        val horizontalSpeed = (avgSpeed * 1.1).coerceIn(30.0, 180.0)
        val verticalSpeed = (avgSpeed * 0.9).coerceIn(20.0, 180.0)
        val speedVariance = (variance * 0.4).coerceIn(5.0, 25.0)

        val changes = mutableMapOf<String, SettingChange>()

        changes["steepness"] = SettingChange(
            "Steepness",
            "Current",
            "%.1f".format(recommendedSteepness),
            "Variance: ${"%.2f".format(variance)}° → Steepness: ${recommendedSteepness}"
        )

        changes["midpoint"] = SettingChange(
            "Midpoint",
            "Current",
            "%.2f".format(recommendedMidpoint),
            "Avg magnitude: ${"%.2f".format(avgMagnitude)}°"
        )

        changes["horizontalTurnSpeed"] = SettingChange(
            "HorizontalTurnSpeed",
            "Current",
            "${horizontalSpeed - speedVariance}..${horizontalSpeed}",
            "Based on avg yaw: ${"%.2f".format(avgYaw)}°"
        )

        changes["verticalTurnSpeed"] = SettingChange(
            "VerticalTurnSpeed",
            "Current",
            "${verticalSpeed - speedVariance}..${verticalSpeed}",
            "Based on avg pitch: ${"%.2f".format(avgPitch)}°"
        )

        val stats = mapOf(
            "variance" to variance,
            "recommendedSteepness" to recommendedSteepness,
            "recommendedMidpoint" to recommendedMidpoint,
            "horizontalSpeed" to horizontalSpeed,
            "verticalSpeed" to verticalSpeed,
            "speedVariance" to speedVariance
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
        val horizontalSpeed = result.stats["horizontalSpeed"]?.toFloat() ?: 60f
        val speedVariance = result.stats["speedVariance"]?.toFloat() ?: 10f
        val horizontalRange = (horizontalSpeed - speedVariance)..horizontalSpeed

        if (AutoConfigApplier.setFloatRangeValue("HorizontalTurnSpeed", horizontalRange)) {
            chat("§a  ✓ HorizontalTurnSpeed: ${"%.1f".format(horizontalRange.start)}..${"%.1f".format(horizontalRange.endInclusive)}")
        }

        // Apply vertical turn speed
        val verticalSpeed = result.stats["verticalSpeed"]?.toFloat() ?: 40f
        val verticalRange = (verticalSpeed - speedVariance)..verticalSpeed

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
