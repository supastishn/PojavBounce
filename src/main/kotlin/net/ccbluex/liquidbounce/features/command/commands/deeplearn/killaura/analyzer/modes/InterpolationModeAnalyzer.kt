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

        // Horizontal speed (percentage based, 1-100)
        val horizontalSpeed = when {
            maxVelocity > 2.0 -> 90..95   // Fast movement = high speed
            maxVelocity > 1.0 -> 80..90   // Medium movement
            else -> 70..85                 // Slow movement
        }

        // Vertical speed (percentage based, 1-100) - generally slower than horizontal
        val verticalSpeed = when {
            maxVelocity > 2.0 -> 25..35
            maxVelocity > 1.0 -> 20..30
            else -> 15..25
        }

        // Direction change factor based on variance
        val directionChangeFactor = when {
            variance > 10.0 -> 85..95    // High variance = need more factor
            variance > 5.0 -> 90..98
            else -> 95..100               // Low variance = full factor
        }

        // Midpoint based on movement patterns (0-1)
        val recommendedMidpoint = when {
            avgVelocity > 1.5 -> 0.40f
            avgVelocity > 0.8 -> 0.35f
            else -> 0.30f
        }

        val changes = mutableMapOf<String, SettingChange>()

        changes["horizontalSpeed"] = SettingChange(
            "HorizontalSpeed",
            "Current",
            "${horizontalSpeed.first}..${horizontalSpeed.last}%",
            "Velocity: ${"%.2f".format(avgVelocity)}"
        )

        changes["verticalSpeed"] = SettingChange(
            "VerticalSpeed",
            "Current",
            "${verticalSpeed.first}..${verticalSpeed.last}%",
            "Based on pitch requirements"
        )

        changes["directionChangeFactor"] = SettingChange(
            "DirectionChangeFactor",
            "Current",
            "${directionChangeFactor.first}..${directionChangeFactor.last}%",
            "Variance: ${"%.2f".format(variance)}°"
        )

        changes["midpoint"] = SettingChange(
            "Midpoint",
            "Current",
            "%.2f".format(recommendedMidpoint),
            "Avg velocity: ${"%.2f".format(avgVelocity)}"
        )

        val stats = mapOf(
            "avgVelocity" to avgVelocity,
            "maxVelocity" to maxVelocity,
            "variance" to variance,
            "horizontalSpeedStart" to horizontalSpeed.first.toDouble(),
            "horizontalSpeedEnd" to horizontalSpeed.last.toDouble(),
            "verticalSpeedStart" to verticalSpeed.first.toDouble(),
            "verticalSpeedEnd" to verticalSpeed.last.toDouble(),
            "directionChangeStart" to directionChangeFactor.first.toDouble(),
            "directionChangeEnd" to directionChangeFactor.last.toDouble(),
            "recommendedMidpoint" to recommendedMidpoint.toDouble()
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

        return buildString {
            append("§d╔ Interpolation Mode Configuration\n")
            if (hSpeed != null) {
                append("§d║ HorizontalSpeed: §7${hSpeed.newValue}\n")
            }
            if (vSpeed != null) {
                append("§d║ VerticalSpeed: §7${vSpeed.newValue}\n")
            }
            if (dcFactor != null) {
                append("§d║ DirectionChangeFactor: §7${dcFactor.newValue}\n")
            }
            if (midpoint != null) {
                append("§d║ Midpoint: §7${midpoint.newValue}\n")
            }
            append("§d║ Velocity: Avg=${avgVel}, Max=${maxVel}\n")
            append("§d╚ Bezier/Sigmoid interpolation - smooth curved paths")
        }
    }
}
