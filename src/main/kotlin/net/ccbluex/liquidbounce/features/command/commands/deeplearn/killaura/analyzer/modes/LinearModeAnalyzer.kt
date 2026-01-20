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

        // Speed recommendation: avg delta magnitude clamped to valid range
        val avgSpeed = kotlin.math.sqrt(avgYaw * avgYaw + avgPitch * avgPitch)
            .coerceIn(10.0, 180.0)

        // Smoothness: low for linear, but if high variance recommend smoothing
        val yawVar = yawDeltas.map { (it - avgYaw) * (it - avgYaw) }.average()
        val pitchVar = pitchDeltas.map { (it - avgPitch) * (it - avgPitch) }.average()
        val variance = kotlin.math.sqrt(yawVar + pitchVar)

        // Calculate recommended turn speeds based on analysis
        val horizontalSpeed = (avgSpeed * 1.2).coerceIn(30.0, 180.0).toFloat()
        val verticalSpeed = (avgSpeed * 0.8).coerceIn(20.0, 180.0).toFloat()

        // Add some variance to the ranges based on observed variance
        val speedVariance = (variance * 0.5).coerceIn(5.0, 30.0).toFloat()

        val changes = mutableMapOf<String, SettingChange>()

        changes["horizontalTurnSpeed"] = SettingChange(
            "HorizontalTurnSpeed",
            "Current",
            "${horizontalSpeed - speedVariance}..${horizontalSpeed}",
            "Based on avg yaw delta: ${"%.2f".format(avgYaw)}°"
        )

        changes["verticalTurnSpeed"] = SettingChange(
            "VerticalTurnSpeed",
            "Current",
            "${verticalSpeed - speedVariance}..${verticalSpeed}",
            "Based on avg pitch delta: ${"%.2f".format(avgPitch)}°"
        )

        val stats = mapOf(
            "avgYaw" to avgYaw,
            "avgPitch" to avgPitch,
            "avgSpeed" to avgSpeed,
            "variance" to variance,
            "horizontalSpeed" to horizontalSpeed.toDouble(),
            "verticalSpeed" to verticalSpeed.toDouble(),
            "speedVariance" to speedVariance.toDouble()
        )

        return AnalysisResult("LinearMode", changes, stats, 0.85f)
    }

    override fun apply(result: AnalysisResult) {
        if (result.stats.isEmpty()) return

        // Set rotation mode to Linear
        if (!AutoConfigApplier.setRotationMode("Linear")) {
            chat("§c✗ Failed to set Linear mode")
            return
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
            return "✗ Linear Mode: Insufficient data"
        }

        val hSpeed = result.changes["horizontalTurnSpeed"]
        val vSpeed = result.changes["verticalTurnSpeed"]
        val variance = result.stats["variance"]?.let { "%.2f".format(it) } ?: "?"

        return buildString {
            append("§b╔ Linear Mode Configuration\n")
            if (hSpeed != null) {
                append("§b║ HorizontalTurnSpeed: §7${hSpeed.newValue}\n")
            }
            if (vSpeed != null) {
                append("§b║ VerticalTurnSpeed: §7${vSpeed.newValue}\n")
            }
            append("§b║ Variance: §7${variance}°\n")
            append("§b╚ Direct delta application - good for low-variance combat")
        }
    }
}
