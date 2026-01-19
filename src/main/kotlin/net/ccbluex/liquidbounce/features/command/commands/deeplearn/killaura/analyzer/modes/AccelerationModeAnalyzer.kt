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
import kotlin.math.abs

object AccelerationModeAnalyzer : KillAuraAnalyzer {

    override fun analyze(samples: List<CombatSample>): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult("AccelerationMode", emptyMap(), emptyMap(), 0f)
        }

        val yawDeltas = samples.map { it.totalDelta.deltaYaw.toDouble() }
        val pitchDeltas = samples.map { it.totalDelta.deltaPitch.toDouble() }

        // Calculate accelerations (1st derivative)
        val yawAccelerations = mutableListOf<Double>()
        val pitchAccelerations = mutableListOf<Double>()

        for (i in 1 until yawDeltas.size) {
            yawAccelerations.add(yawDeltas[i] - yawDeltas[i - 1])
            pitchAccelerations.add(pitchDeltas[i] - pitchDeltas[i - 1])
        }

        val avgYawAccel = yawAccelerations.average()
        val avgPitchAccel = pitchAccelerations.average()
        val maxYawAccel = yawAccelerations.map { abs(it) }.maxOrNull() ?: 0.0
        val maxPitchAccel = pitchAccelerations.map { abs(it) }.maxOrNull() ?: 0.0

        // Jerk: 2nd derivative (rate of acceleration change)
        val jerks = mutableListOf<Double>()
        for (i in 1 until yawAccelerations.size) {
            val yawJerk = abs((yawAccelerations[i] - yawAccelerations[i - 1]))
            val pitchJerk = abs((pitchAccelerations[i] - pitchAccelerations[i - 1]))
            jerks.add((yawJerk + pitchJerk) / 2)
        }

        val avgJerk = if (jerks.isNotEmpty()) jerks.average() else 0.0

        // Recommend parameters based on acceleration patterns
        val maxAccel = maxOf(maxYawAccel, maxPitchAccel)

        val recommendedAcceleration = when {
            maxAccel > 10.0 -> 0.8   // High accel = allow more acceleration
            maxAccel > 5.0 -> 0.6    // Medium accel
            else -> 0.4               // Low accel = gentle acceleration
        }

        val recommendedDeceleration = recommendedAcceleration * 0.7  // Deceleration ~70% of acceleration

        val recommendedJerkLimit = when {
            avgJerk > 2.0 -> 0.5     // High jerk = strict limit
            avgJerk > 1.0 -> 0.7
            else -> 0.9               // Low jerk = can be lenient
        }

        val changes = mutableMapOf<String, SettingChange>()

        changes["acceleration"] = SettingChange(
            "Acceleration",
            "Current",
            "%.2f".format(recommendedAcceleration),
            "Max accel: ${"%.2f".format(maxAccel)}°/tick"
        )

        changes["deceleration"] = SettingChange(
            "Deceleration",
            "Current",
            "%.2f".format(recommendedDeceleration),
            "Decel/Accel ratio: 0.7"
        )

        changes["jerkLimit"] = SettingChange(
            "Jerk Limit",
            "Current",
            "%.2f".format(recommendedJerkLimit),
            "Avg jerk: ${"%.3f".format(avgJerk)}"
        )

        val stats = mapOf(
            "avgYawAccel" to avgYawAccel,
            "avgPitchAccel" to avgPitchAccel,
            "maxAccel" to maxAccel,
            "avgJerk" to avgJerk,
            "recommendedAcceleration" to recommendedAcceleration,
            "recommendedDeceleration" to recommendedDeceleration,
            "recommendedJerkLimit" to recommendedJerkLimit
        )

        return AnalysisResult("AccelerationMode", changes, stats, 0.90f)
    }

    override fun apply(result: AnalysisResult) {
        // Manual application via ClickGUI
    }

    override fun report(result: AnalysisResult): String {
        if (result.changes.isEmpty()) {
            return "✗ Acceleration Mode: Insufficient data"
        }

        val accel = result.changes["acceleration"]
        val decel = result.changes["deceleration"]
        val jerk = result.changes["jerkLimit"]
        val maxA = result.stats["maxAccel"]?.let { "%.2f".format(it) } ?: "?"
        val avgJ = result.stats["avgJerk"]?.let { "%.3f".format(it) } ?: "?"

        return buildString {
            append("§1╔ Acceleration Mode Configuration\n")
            if (accel != null) {
                append("§1║ Acceleration: §7${accel.newValue}\n")
            }
            if (decel != null) {
                append("§1║ Deceleration: §7${decel.newValue}\n")
            }
            if (jerk != null) {
                append("§1║ Jerk Limit: §7${jerk.newValue}\n")
            }
            append("§1║ Max Accel: §7${maxA}°/tick | Avg Jerk: §7${avgJ}\n")
            append("§1╚ Acceleration-based smoothing - handles high variance")
        }
    }
}
