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

import net.ccbluex.liquidbounce.deeplearn.data.CombatSample
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import kotlin.math.abs
import kotlin.math.sqrt

object RotationAnalyzer : KillAuraAnalyzer {

    override fun analyze(samples: List<CombatSample>): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult("Rotation", emptyMap(), emptyMap(), 0f)
        }

        val yawDeltas = samples.map { it.totalDelta.deltaYaw.toDouble() }
        val pitchDeltas = samples.map { it.totalDelta.deltaPitch.toDouble() }

        val avgYawDelta = yawDeltas.average()
        val avgPitchDelta = pitchDeltas.average()
        val maxYawDelta = yawDeltas.maxOrNull() ?: 0.0
        val maxPitchDelta = pitchDeltas.maxOrNull() ?: 0.0

        // Calculate variance for smoothness needs
        val yawVariance = if (yawDeltas.isNotEmpty()) {
            yawDeltas.map { (it - avgYawDelta) * (it - avgYawDelta) }.average()
        } else 0.0
        val pitchVariance = if (pitchDeltas.isNotEmpty()) {
            pitchDeltas.map { (it - avgPitchDelta) * (it - avgPitchDelta) }.average()
        } else 0.0

        val totalVariance = sqrt(yawVariance + pitchVariance)

        // Calculate acceleration (2nd derivative approximation)
        val yawAccelerations = mutableListOf<Double>()
        val pitchAccelerations = mutableListOf<Double>()
        for (i in 1 until yawDeltas.size - 1) {
            yawAccelerations.add((yawDeltas[i + 1] - yawDeltas[i]) - (yawDeltas[i] - yawDeltas[i - 1]))
            pitchAccelerations.add((pitchDeltas[i + 1] - pitchDeltas[i]) - (pitchDeltas[i] - pitchDeltas[i - 1]))
        }

        val avgYawAccel = if (yawAccelerations.isNotEmpty()) yawAccelerations.map { abs(it) }.average() else 0.0
        val avgPitchAccel = if (pitchAccelerations.isNotEmpty()) pitchAccelerations.map { abs(it) }.average() else 0.0

        val stats = mapOf(
            "avgYawDelta" to avgYawDelta,
            "avgPitchDelta" to avgPitchDelta,
            "maxYawDelta" to maxYawDelta,
            "maxPitchDelta" to maxPitchDelta,
            "totalVariance" to totalVariance,
            "avgYawAccel" to avgYawAccel,
            "avgPitchAccel" to avgPitchAccel
        )

        return AnalysisResult(
            "Rotation",
            emptyMap(), // Changes applied per-mode
            stats,
            0.85f
        )
    }

    override fun apply(result: AnalysisResult) {
        // Per-mode application handled by command
    }

    override fun report(result: AnalysisResult): String {
        if (result.stats.isEmpty()) {
            return "✗ Rotation: Insufficient data"
        }

        val avgYaw = result.stats["avgYawDelta"]?.let { "%.2f".format(it) } ?: "?"
        val avgPitch = result.stats["avgPitchDelta"]?.let { "%.2f".format(it) } ?: "?"
        val variance = result.stats["totalVariance"]?.let { "%.2f".format(it) } ?: "?"
        val varianceValue = result.stats["totalVariance"] ?: 0.0
        val yawAccel = result.stats["avgYawAccel"]?.let { "%.3f".format(it) } ?: "?"
        val pitchAccel = result.stats["avgPitchAccel"]?.let { "%.3f".format(it) } ?: "?"

        return "✓ Rotation Analysis\n" +
            "  • Avg Yaw Delta: ${avgYaw}° | Pitch Delta: ${avgPitch}°\n" +
            "  • Total Variance: $variance (smoothness factor)\n" +
            "  • Avg Acceleration: Yaw=${yawAccel}, Pitch=${pitchAccel}\n" +
            "  • Recommendation: " +
            when {
                varianceValue > 10.0 -> "High variance - try Acceleration mode"
                varianceValue > 5.0 -> "Medium variance - Sigmoid or Interpolation recommended"
                else -> "Low variance - Linear mode suitable"
            }
    }
}
