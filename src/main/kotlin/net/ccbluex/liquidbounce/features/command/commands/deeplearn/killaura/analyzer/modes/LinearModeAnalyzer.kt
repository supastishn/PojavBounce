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

object LinearModeAnalyzer : KillAuraAnalyzer {

    override fun analyze(samples: List<CombatSample>): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult("LinearMode", emptyMap(), emptyMap(), 0f)
        }

        // Linear mode: direct rotation delta application
        val yawDeltas = samples.map { it.totalDelta.deltaYaw.toDouble() }
        val pitchDeltas = samples.map { it.totalDelta.deltaPitch.toDouble() }

        val avgYaw = yawDeltas.average()
        val avgPitch = pitchDeltas.average()

        // Speed recommendation: avg delta magnitude
        val avgSpeed = kotlin.math.sqrt(avgYaw * avgYaw + avgPitch * avgPitch)

        // Smoothness: low for linear, but if high variance recommend smoothing
        val yawVar = yawDeltas.map { (it - avgYaw) * (it - avgYaw) }.average()
        val pitchVar = pitchDeltas.map { (it - avgPitch) * (it - avgPitch) }.average()
        val variance = kotlin.math.sqrt(yawVar + pitchVar)

        val changes = mutableMapOf<String, SettingChange>()

        changes["speed"] = SettingChange(
            "Speed",
            "Current",
            "%.2f".format(avgSpeed),
            "Average rotation speed: ${"%.2f".format(avgSpeed)}°"
        )

        changes["smoothing"] = SettingChange(
            "Smoothing",
            "Current",
            when {
                variance > 5.0 -> "Enabled (high variance)"
                else -> "Minimal"
            },
            "Variance: ${"%.2f".format(variance)}"
        )

        val stats = mapOf(
            "avgYaw" to avgYaw,
            "avgPitch" to avgPitch,
            "avgSpeed" to avgSpeed,
            "variance" to variance.toDouble()
        )

        return AnalysisResult("LinearMode", changes, stats, 0.85f)
    }

    override fun apply(result: AnalysisResult) {
        // Manual application via ClickGUI
    }

    override fun report(result: AnalysisResult): String {
        if (result.changes.isEmpty()) {
            return "✗ Linear Mode: Insufficient data"
        }

        val speed = result.changes["speed"]
        val smoothing = result.changes["smoothing"]
        val variance = result.stats["variance"]?.let { "%.2f".format(it) } ?: "?"

        return buildString {
            append("§b╔ Linear Mode Configuration\n")
            if (speed != null) {
                append("§b║ Speed: §7${speed.newValue}\n")
            }
            if (smoothing != null) {
                append("§b║ Smoothing: §7${smoothing.newValue}\n")
            }
            append("§b║ Variance: §7${variance}°\n")
            append("§b╚ Direct delta application - good for low-variance combat")
        }
    }
}
