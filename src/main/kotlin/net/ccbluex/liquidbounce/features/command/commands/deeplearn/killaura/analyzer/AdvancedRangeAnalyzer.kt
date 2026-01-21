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

/**
 * Analyzer for range settings based on wall detection and raycast data
 */
object AdvancedRangeAnalyzer {

    data class AdvancedRangeResult(
        val avgRange: Float,
        val maxRange: Float,
        val p90Range: Float,
        val wallHitPercentage: Float,
        val raycastSuccessRate: Float,
        val recommendedRange: Float,
        val recommendedWallRange: Float
    )

    fun analyze(samples: List<KillAuraConfigSample>): AdvancedRangeResult? {
        if (samples.isEmpty()) return null

        val ranges = samples.map { it.actualRange.toDouble() }
        val sortedRanges = ranges.sorted()

        val avgRange = ranges.average().toFloat()
        val maxRange = ranges.maxOrNull()?.toFloat() ?: 4.2f
        val p90Range = sortedRanges[(sortedRanges.size * 0.90).toInt().coerceIn(0, sortedRanges.size - 1)].toFloat()

        // Wall detection analysis
        val wallHitCount = samples.count { it.hasWallBetween }
        val wallHitPercentage = wallHitCount.toFloat() / samples.size

        // Raycast success analysis
        val raycastSuccessCount = samples.count { it.raycastHit }
        val raycastSuccessRate = raycastSuccessCount.toFloat() / samples.size

        // Calculate recommended ranges based on data
        // If wall hits are common, reduce wall range accordingly
        val recommendedRange = avgRange.coerceIn(1f, 6f)
        val recommendedWallRange = if (wallHitPercentage > 0.3f) {
            // Lots of wall hits - be more conservative with wall range
            (p90Range * (1 - wallHitPercentage)).coerceIn(0f, recommendedRange)
        } else {
            (p90Range * 0.8f).coerceIn(0f, recommendedRange)
        }

        return AdvancedRangeResult(
            avgRange = avgRange,
            maxRange = maxRange,
            p90Range = p90Range,
            wallHitPercentage = wallHitPercentage,
            raycastSuccessRate = raycastSuccessRate,
            recommendedRange = recommendedRange,
            recommendedWallRange = recommendedWallRange
        )
    }

    fun report(result: AdvancedRangeResult?): String {
        if (result == null) return "✗ Advanced Range: Insufficient data"

        return buildString {
            append("§e╔ Advanced Range Analysis\n")
            append("§e║ Combat Range: §7Avg ${result.avgRange.format()} / Max ${result.maxRange.format()}\n")
            append("§e║ P90 Range: §7${result.p90Range.format()}\n")
            append("§e║ Wall Hits: §7${"%.1f".format(result.wallHitPercentage * 100)}%\n")
            append("§e║ Raycast Success: §7${"%.1f".format(result.raycastSuccessRate * 100)}%\n")
            append("§e║ Recommended Range: §a${result.recommendedRange.format()}\n")
            append("§e║ Recommended WallRange: §a${result.recommendedWallRange.format()}\n")
            append("§e╚ Based on wall detection and raycast analysis")
        }
    }

    private fun Float.format() = "%.2f".format(this)
}
