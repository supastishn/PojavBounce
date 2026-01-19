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

object InterpolationModeAnalyzer : KillAuraAnalyzer {

    override fun analyze(samples: List<CombatSample>): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult("InterpolationMode", emptyMap(), emptyMap(), 0f)
        }

        // Interpolation: spline-based smoothing
        val velocityDeltas = samples.map { sample ->
            kotlin.math.sqrt(
                sample.velocityDelta.x * sample.velocityDelta.x +
                sample.velocityDelta.y * sample.velocityDelta.y
            ).toDouble()
        }

        val avgVelocity = velocityDeltas.average()
        val maxVelocity = velocityDeltas.maxOrNull() ?: 0.0

        // Spline tension: how tight the curve is
        // High velocity = need less tension, low velocity = can use more tension
        val recommendedTension = when {
            maxVelocity > 2.0 -> 0.3   // Loose spline for fast movement
            maxVelocity > 1.0 -> 0.5   // Medium tension
            else -> 0.7                 // Tight spline for slow movement
        }

        // Knot density: how many control points for smoothness
        val recommendedKnots = when {
            samples.size > 500 -> 20  // Dense knots for lots of data
            samples.size > 200 -> 15
            samples.size > 100 -> 10
            else -> 5
        }

        val changes = mutableMapOf<String, SettingChange>()

        changes["tension"] = SettingChange(
            "Spline Tension",
            "Current",
            "%.1f".format(recommendedTension),
            "Velocity: ${"%.2f".format(avgVelocity)} → Tension: ${recommendedTension}"
        )

        changes["knots"] = SettingChange(
            "Knot Density",
            "Current",
            "$recommendedKnots",
            "Samples: ${samples.size} → Knots: $recommendedKnots"
        )

        val stats = mapOf(
            "avgVelocity" to avgVelocity,
            "maxVelocity" to maxVelocity,
            "recommendedTension" to recommendedTension,
            "recommendedKnots" to recommendedKnots.toDouble()
        )

        return AnalysisResult("InterpolationMode", changes, stats, 0.75f)
    }

    override fun apply(result: AnalysisResult) {
        // Manual application via ClickGUI
    }

    override fun report(result: AnalysisResult): String {
        if (result.changes.isEmpty()) {
            return "✗ Interpolation Mode: Insufficient data"
        }

        val tension = result.changes["tension"]
        val knots = result.changes["knots"]
        val avgVel = result.stats["avgVelocity"]?.let { "%.2f".format(it) } ?: "?"
        val maxVel = result.stats["maxVelocity"]?.let { "%.2f".format(it) } ?: "?"

        return buildString {
            append("§d╔ Interpolation Mode Configuration\n")
            if (tension != null) {
                append("§d║ Spline Tension: §7${tension.newValue}\n")
            }
            if (knots != null) {
                append("§d║ Knot Density: §7${knots.newValue}\n")
            }
            append("§d║ Velocity: Avg=${avgVel}, Max=${maxVel}\n")
            append("§d╚ Spline interpolation - smooth curved paths")
        }
    }
}
