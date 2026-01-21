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

import net.ccbluex.liquidbounce.deeplearn.data.KillAuraConfigSample
import net.ccbluex.liquidbounce.utils.client.chat

/**
 * Analyzer for click timing/CPS settings based on KillAuraConfigSample data
 */
object ClickTimingAnalyzer {

    data class ClickTimingResult(
        val avgCPS: Float,
        val minCPS: Float,
        val maxCPS: Float,
        val avgClickInterval: Long,
        val minClickInterval: Long,
        val maxClickInterval: Long,
        val hitSuccessRate: Float
    )

    fun analyze(samples: List<KillAuraConfigSample>): ClickTimingResult? {
        if (samples.isEmpty()) return null

        // Filter to only samples where attacks were attempted
        val attackSamples = samples.filter { it.attackAttempted }
        if (attackSamples.isEmpty()) return null

        val cpsList = samples.map { it.currentCPS }.filter { it > 0 }
        val clickIntervals = samples.map { it.timeSinceLastClick }.filter { it > 0 }

        val avgCPS = if (cpsList.isNotEmpty()) cpsList.average().toFloat() else 0f
        val minCPS = cpsList.minOrNull()?.toFloat() ?: 0f
        val maxCPS = cpsList.maxOrNull()?.toFloat() ?: 0f

        val avgInterval = if (clickIntervals.isNotEmpty()) clickIntervals.average().toLong() else 0L
        val minInterval = clickIntervals.minOrNull() ?: 0L
        val maxInterval = clickIntervals.maxOrNull() ?: 0L

        val successCount = attackSamples.count { it.attackSucceeded }
        val hitSuccessRate = if (attackSamples.isNotEmpty()) {
            successCount.toFloat() / attackSamples.size
        } else 0f

        return ClickTimingResult(
            avgCPS = avgCPS,
            minCPS = minCPS,
            maxCPS = maxCPS,
            avgClickInterval = avgInterval,
            minClickInterval = minInterval,
            maxClickInterval = maxInterval,
            hitSuccessRate = hitSuccessRate
        )
    }

    fun report(result: ClickTimingResult?): String {
        if (result == null) return "✗ Click Timing: Insufficient data"

        return buildString {
            append("§6╔ Click Timing Analysis\n")
            append("§6║ CPS: §7${result.minCPS.toInt()}-${result.maxCPS.toInt()} (avg: ${result.avgCPS.toInt()})\n")
            append("§6║ Click Interval: §7${result.minClickInterval}-${result.maxClickInterval}ms\n")
            append("§6║ Hit Success Rate: §7${"%.1f".format(result.hitSuccessRate * 100)}%\n")
            append("§6╚ Based on actual click patterns during training")
        }
    }
}
