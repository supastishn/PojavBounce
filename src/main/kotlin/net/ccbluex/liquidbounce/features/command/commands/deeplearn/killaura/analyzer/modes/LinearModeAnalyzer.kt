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
        // Use actual velocity data (rotation per tick) instead of totalDelta
        val yawSpeeds = samples.map { kotlin.math.abs(it.velocityDelta.x.toDouble()) }
        val pitchSpeeds = samples.map { kotlin.math.abs(it.velocityDelta.y.toDouble()) }

        // Sort for percentile calculations
        val sortedYaw = yawSpeeds.sorted()
        val sortedPitch = pitchSpeeds.sorted()

        // Use P70 (higher) and P95 (max viable) for more aggressive ranges
        val yawP70 = sortedYaw[(sortedYaw.size * 0.70).toInt().coerceIn(0, sortedYaw.size - 1)]
        val yawP95 = sortedYaw[(sortedYaw.size * 0.95).toInt().coerceIn(0, sortedYaw.size - 1)]
        val pitchP70 = sortedPitch[(sortedPitch.size * 0.70).toInt().coerceIn(0, sortedPitch.size - 1)]
        val pitchP95 = sortedPitch[(sortedPitch.size * 0.95).toInt().coerceIn(0, sortedPitch.size - 1)]

        // Also calculate average for reference
        val avgYawSpeed = yawSpeeds.average()
        val avgPitchSpeed = pitchSpeeds.average()

        // Calculate variance
        val yawVar = yawSpeeds.map { (it - avgYawSpeed) * (it - avgYawSpeed) }.average()
        val pitchVar = pitchSpeeds.map { (it - avgPitchSpeed) * (it - avgPitchSpeed) }.average()
        val variance = kotlin.math.sqrt(yawVar + pitchVar)

        // Horizontal turn speed: from P70 to P95, with minimum viable speed
        // Scale up if values are too low (combat data might be from slow precise aiming)
        val horizontalSpeedStart = (yawP70 * 1.5).coerceIn(15.0, 180.0).toFloat()
        val horizontalSpeedEnd = (yawP95 * 1.8).coerceIn(horizontalSpeedStart.toDouble() + 10.0, 180.0).toFloat()

        // Vertical turn speed: from P70 to P95, with minimum viable speed
        val verticalSpeedStart = (pitchP70 * 1.5).coerceIn(12.0, 180.0).toFloat()
        val verticalSpeedEnd = (pitchP95 * 1.8).coerceIn(verticalSpeedStart.toDouble() + 10.0, 180.0).toFloat()

        val changes = mutableMapOf<String, SettingChange>()

        changes["horizontalTurnSpeed"] = SettingChange(
            "HorizontalTurnSpeed",
            "Current",
            "${"%1f".format(horizontalSpeedStart)}..${"%.1f".format(horizontalSpeedEnd)}",
            "From P70-P95 yaw speeds: ${"%.2f".format(yawP70)}-${"%.2f".format(yawP95)}°/tick (1.5x-1.8x scaled)"
        )

        changes["verticalTurnSpeed"] = SettingChange(
            "VerticalTurnSpeed",
            "Current",
            "${"%.1f".format(verticalSpeedStart)}..${"%.1f".format(verticalSpeedEnd)}",
            "From P70-P95 pitch speeds: ${"%.2f".format(pitchP70)}-${"%.2f".format(pitchP95)}°/tick (1.5x-1.8x scaled)"
        )

        val stats = mapOf(
            "avgYawSpeed" to avgYawSpeed,
            "avgPitchSpeed" to avgPitchSpeed,
            "yawP70" to yawP70,
            "yawP95" to yawP95,
            "pitchP70" to pitchP70,
            "pitchP95" to pitchP95,
            "variance" to variance,
            "horizontalSpeedStart" to horizontalSpeedStart.toDouble(),
            "horizontalSpeedEnd" to horizontalSpeedEnd.toDouble(),
            "verticalSpeedStart" to verticalSpeedStart.toDouble(),
            "verticalSpeedEnd" to verticalSpeedEnd.toDouble()
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
        val hStart = result.stats["horizontalSpeedStart"]?.toFloat() ?: 30f
        val hEnd = result.stats["horizontalSpeedEnd"]?.toFloat() ?: 60f
        val horizontalRange = hStart..hEnd

        if (AutoConfigApplier.setFloatRangeValue("HorizontalTurnSpeed", horizontalRange)) {
            chat("§a  ✓ HorizontalTurnSpeed: ${"%.1f".format(hStart)}..${"%.1f".format(hEnd)}")
        }

        // Apply vertical turn speed
        val vStart = result.stats["verticalSpeedStart"]?.toFloat() ?: 20f
        val vEnd = result.stats["verticalSpeedEnd"]?.toFloat() ?: 40f
        val verticalRange = vStart..vEnd

        if (AutoConfigApplier.setFloatRangeValue("VerticalTurnSpeed", verticalRange)) {
            chat("§a  ✓ VerticalTurnSpeed: ${"%.1f".format(vStart)}..${"%.1f".format(vEnd)}")
        }
    }

    override fun report(result: AnalysisResult): String {
        if (result.changes.isEmpty()) {
            return "✗ Linear Mode: Insufficient data"
        }

        val hSpeed = result.changes["horizontalTurnSpeed"]
        val vSpeed = result.changes["verticalTurnSpeed"]
        val variance = result.stats["variance"]?.let { "%.2f".format(it) } ?: "?"
        val avgYaw = result.stats["avgYawSpeed"]?.let { "%.2f".format(it) } ?: "?"
        val avgPitch = result.stats["avgPitchSpeed"]?.let { "%.2f".format(it) } ?: "?"

        return buildString {
            append("§b╔ Linear Mode Configuration\n")
            if (hSpeed != null) {
                append("§b║ HorizontalTurnSpeed: §7${hSpeed.newValue}\n")
                append("§b║   ${hSpeed.reasoning}\n")
            }
            if (vSpeed != null) {
                append("§b║ VerticalTurnSpeed: §7${vSpeed.newValue}\n")
                append("§b║   ${vSpeed.reasoning}\n")
            }
            append("§b║ Avg speeds: Yaw=${avgYaw}°/tick, Pitch=${avgPitch}°/tick\n")
            append("§b║ Variance: §7${variance}°\n")
            append("§b╚ Direct delta application - good for low-variance combat")
        }
    }
}
