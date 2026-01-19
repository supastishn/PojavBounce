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
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.KillAuraAnalyzer
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.SettingChange

object SigmoidModeAnalyzer : KillAuraAnalyzer {

    override fun analyze(samples: List<CombatSample>): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult("SigmoidMode", emptyMap(), emptyMap(), 0f)
        }

        // Sigmoid: easing curve smoothing
        val yawDeltas = samples.map { it.totalDelta.deltaYaw.toDouble() }
        val pitchDeltas = samples.map { it.totalDelta.deltaPitch.toDouble() }

        val avgYaw = yawDeltas.average()
        val avgPitch = pitchDeltas.average()
        val variance = kotlin.math.sqrt(
            yawDeltas.map { (it - avgYaw) * (it - avgYaw) }.average() +
            pitchDeltas.map { (it - avgPitch) * (it - avgPitch) }.average()
        )

        // Sigmoid curve steepness based on variance
        val recommendedSteepness = when {
            variance > 10.0 -> 2.0  // Steep curve for high variance
            variance > 5.0 -> 1.5   // Medium curve
            else -> 1.0              // Gentle curve
        }

        // Min/max speed thresholds
        val minSpeed = kotlin.math.abs(avgYaw) + kotlin.math.abs(avgPitch)
        val maxSpeed = minSpeed * 2.5  // Allow up to 2.5x for peaks

        val changes = mutableMapOf<String, SettingChange>()

        changes["steepness"] = SettingChange(
            "Curve Steepness",
            "Current",
            "%.1f".format(recommendedSteepness),
            "Variance: ${"%.2f".format(variance)}° → Steepness: ${recommendedSteepness}"
        )

        changes["minSpeed"] = SettingChange(
            "Min Speed",
            "Current",
            "%.2f".format(minSpeed),
            "Minimum rotation threshold"
        )

        changes["maxSpeed"] = SettingChange(
            "Max Speed",
            "Current",
            "%.2f".format(maxSpeed),
            "Maximum rotation threshold"
        )

        val stats = mapOf(
            "variance" to variance,
            "recommendedSteepness" to recommendedSteepness,
            "minSpeed" to minSpeed,
            "maxSpeed" to maxSpeed
        )

        return AnalysisResult("SigmoidMode", changes, stats, 0.80f)
    }

    override fun apply(result: AnalysisResult) {
        // Manual application via ClickGUI
    }

    override fun report(result: AnalysisResult): String {
        if (result.changes.isEmpty()) {
            return "✗ Sigmoid Mode: Insufficient data"
        }

        val steepness = result.changes["steepness"]
        val minSpeed = result.changes["minSpeed"]
        val maxSpeed = result.changes["maxSpeed"]
        val variance = result.stats["variance"]?.let { "%.2f".format(it) } ?: "?"

        return buildString {
            append("§5╔ Sigmoid Mode Configuration\n")
            if (steepness != null) {
                append("§5║ Curve Steepness: §7${steepness.newValue}\n")
            }
            if (minSpeed != null) {
                append("§5║ Min Speed: §7${minSpeed.newValue}\n")
            }
            if (maxSpeed != null) {
                append("§5║ Max Speed: §7${maxSpeed.newValue}\n")
            }
            append("§5║ Variance: §7${variance}°\n")
            append("§5╚ Sigmoid easing - smooth acceleration curves")
        }
    }
}
