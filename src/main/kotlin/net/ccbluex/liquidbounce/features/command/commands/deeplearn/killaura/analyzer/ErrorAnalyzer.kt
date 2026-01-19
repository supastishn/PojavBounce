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

object ErrorAnalyzer : KillAuraAnalyzer {

    override fun analyze(samples: List<CombatSample>): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult("Error", emptyMap(), emptyMap(), 0f)
        }

        // ===== ROTATION ERROR ANALYSIS =====
        val yawDeltas = samples.map { it.totalDelta.deltaYaw.toDouble() }
        val pitchDeltas = samples.map { it.totalDelta.deltaPitch.toDouble() }

        // Calculate rotation jitter/variance
        val avgYaw = yawDeltas.average()
        val avgPitch = pitchDeltas.average()
        val yawVariance = yawDeltas.map { (it - avgYaw) * (it - avgYaw) }.average()
        val pitchVariance = pitchDeltas.map { (it - avgPitch) * (it - avgPitch) }.average()
        val totalRotationError = sqrt(yawVariance + pitchVariance)

        // Calculate rotation consistency (low consistency = high error)
        val yawStdDev = sqrt(yawVariance)
        val pitchStdDev = sqrt(pitchVariance)

        // ===== PREDICTION ERROR ANALYSIS =====
        val velocityDeltas = samples.map { sample ->
            abs(sample.velocityDelta.x.toDouble()) + abs(sample.velocityDelta.y.toDouble())
        }

        val avgVelocityError = velocityDeltas.average()
        val maxVelocityError = velocityDeltas.maxOrNull() ?: 0.0
        val velocityErrorVariance = velocityDeltas.map { (it - avgVelocityError) * (it - avgVelocityError) }.average()

        // ===== ACCELERATION ERROR (2ND DERIVATIVE) =====
        val yawAccelerations = mutableListOf<Double>()
        val pitchAccelerations = mutableListOf<Double>()

        for (i in 1 until yawDeltas.size - 1) {
            yawAccelerations.add(abs((yawDeltas[i + 1] - yawDeltas[i]) - (yawDeltas[i] - yawDeltas[i - 1])))
            pitchAccelerations.add(abs((pitchDeltas[i + 1] - pitchDeltas[i]) - (pitchDeltas[i] - pitchDeltas[i - 1])))
        }

        val avgAccelError = if (yawAccelerations.isNotEmpty()) {
            (yawAccelerations.average() + pitchAccelerations.average()) / 2
        } else 0.0

        // ===== HIT SUCCESS ANALYSIS =====
        // Estimate based on hurt time consistency
        val hurtTimes = samples.map { it.hurtTime }
        val targetHit = hurtTimes.count { it > 0 }
        val hitRate = if (samples.isNotEmpty()) targetHit.toDouble() / samples.size else 0.0

        // ===== CONSTANT/SYSTEMATIC ERROR =====
        // If rotation deltas are consistently in one direction = systematic error
        val yawBias = yawDeltas.count { it > avgYaw }.toDouble() / yawDeltas.size
        val pitchBias = pitchDeltas.count { it > avgPitch }.toDouble() / pitchDeltas.size
        val systematicError = abs(yawBias - 0.5) + abs(pitchBias - 0.5) // 1.0 = perfect bias, 0.0 = balanced

        // ===== RECOMMENDATIONS =====
        val changes = mutableMapOf<String, SettingChange>()

        // Primary recommendation: which rotation mode is needed
        val recommendedRotationMode = when {
            avgAccelError > 0.5 -> "Acceleration" // High acceleration error = need acceleration smoothing
            totalRotationError > 15.0 -> "Sigmoid" // High variance = sigmoid smoothing
            totalRotationError > 8.0 -> "Interpolation" // Medium variance = interpolation
            else -> "Linear" // Low variance = linear sufficient
        }

        changes["rotationMode"] = SettingChange(
            "Rotation Mode",
            "Current",
            recommendedRotationMode,
            "Error analysis: Rotation=${totalRotationError.toFloat()}°, Accel=${avgAccelError.toFloat()}"
        )

        // Secondary recommendations per mode
        changes["accelerationRecommended"] = SettingChange(
            "Use Acceleration Mode",
            "Current",
            (avgAccelError > 0.3),
            "Acceleration error detected: ${"%.3f".format(avgAccelError)}"
        )

        val stats = mapOf(
            "totalRotationError" to totalRotationError,
            "yawStdDev" to yawStdDev.toDouble(),
            "pitchStdDev" to pitchStdDev.toDouble(),
            "avgVelocityError" to avgVelocityError,
            "velocityErrorVariance" to velocityErrorVariance,
            "avgAccelError" to avgAccelError,
            "hitRate" to hitRate,
            "systematicError" to systematicError
        )

        return AnalysisResult(
            "Error",
            changes,
            stats,
            0.90f
        )
    }

    override fun apply(result: AnalysisResult) {
        // Rotation mode application needs manual CLI or complex reflection
        // Report recommendations in chat
    }

    override fun report(result: AnalysisResult): String {
        if (result.stats.isEmpty()) {
            return "✗ Error Analysis: Insufficient data"
        }

        val rotError = result.stats["totalRotationError"]?.let { "%.2f".format(it) } ?: "?"
        val yawStd = result.stats["yawStdDev"]?.let { "%.2f".format(it) } ?: "?"
        val pitchStd = result.stats["pitchStdDev"]?.let { "%.2f".format(it) } ?: "?"
        val velError = result.stats["avgVelocityError"]?.let { "%.3f".format(it) } ?: "?"
        val accelError = result.stats["avgAccelError"]?.let { "%.4f".format(it) } ?: "?"
        val hitRate = result.stats["hitRate"]?.let { "%.1f%%".format(it * 100) } ?: "?"
        val sysError = result.stats["systematicError"]?.let { "%.2f".format(it) } ?: "?"

        val modeRec = result.changes["rotationMode"]
        val accelRec = result.changes["accelerationRecommended"]

        return buildString {
            append("§e✓ Error Analysis Report\n")
            append("§7━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("§6Rotation Errors:\n")
            append("  • Total: ${rotError}° (jitter/variance)\n")
            append("  • Yaw StdDev: ${yawStd}° | Pitch StdDev: ${pitchStd}°\n")
            append("\n§6Prediction Errors:\n")
            append("  • Velocity Error: ${velError}\n")
            append("  • Systematic Bias: ${sysError} (0.0=balanced, 1.0=perfect bias)\n")
            append("\n§6Acceleration Errors:\n")
            append("  • Avg Acceleration Error: ${accelError}\n")
            append("\n§6Combat Effectiveness:\n")
            append("  • Hit Rate: ${hitRate}\n")
            append("\n§a━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            append("§eRecommendations:\n")
            if (modeRec != null) {
                append("  §a✓ Rotation Mode: ${modeRec.newValue}\n")
                append("    Reason: ${modeRec.reasoning}\n")
            }
            if (accelRec != null) {
                append("  §a✓ Acceleration Mode Needed: ${accelRec.newValue}\n")
                append("    Reason: ${accelRec.reasoning}\n")
            }
            append("\n§7Apply these in ClickGUI → KillAura → Rotations → AngleSmooth")
        }
    }
}
