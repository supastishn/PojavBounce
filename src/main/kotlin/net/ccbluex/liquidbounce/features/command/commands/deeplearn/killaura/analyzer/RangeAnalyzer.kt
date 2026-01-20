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
import kotlin.math.max
import kotlin.math.min

object RangeAnalyzer : KillAuraAnalyzer {

    override fun analyze(samples: List<CombatSample>): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult("Range", emptyMap(), emptyMap(), 0f)
        }

        val distances = samples.map { it.distance.toDouble() }

        val avgDistance = distances.average()
        val maxDistance = distances.maxOrNull() ?: 4.2
        val minDistance = distances.minOrNull() ?: 1.0
        val p75Distance = distances.sorted().let { sorted ->
            sorted[((sorted.size * 0.75).toInt()).coerceIn(0, sorted.size - 1)]
        }

        // Calculate recommended values
        val recommendedRange = avgDistance.toFloat().coerceIn(1f, 8f)
        val recommendedWallRange = (p75Distance * 0.7).toFloat().coerceIn(0f, min(recommendedRange, 8f))
        val recommendedScanRange = (maxDistance * 0.5).toFloat().coerceIn(0f, 7f)

        val changes = mutableMapOf<String, SettingChange>()

        changes["range"] = SettingChange(
            "Range",
            ModuleKillAura.range,
            recommendedRange,
            "Average combat distance: ${"%.2f".format(avgDistance)}m"
        )

        changes["wallRange"] = SettingChange(
            "WallRange",
            ModuleKillAura.wallRange,
            recommendedWallRange,
            "75th percentile distance: ${"%.2f".format(p75Distance)}m"
        )

        val stats = mapOf(
            "avgDistance" to avgDistance,
            "maxDistance" to maxDistance,
            "minDistance" to minDistance,
            "p75Distance" to p75Distance
        )

        return AnalysisResult(
            "Range",
            changes,
            stats,
            0.95f
        )
    }

    override fun apply(result: AnalysisResult) {
        // Range settings are NOT automatically applied because:
        // 1. WallRange requires raycast/wall collision data we don't have
        // 2. Range is server-dependent and may cause issues if set incorrectly
        // User should manually adjust these based on recommendations in report
    }

    override fun report(result: AnalysisResult): String {
        if (result.changes.isEmpty()) {
            return "✗ Range: Insufficient data"
        }

        val rangeChange = result.changes["range"]
        val wallRangeChange = result.changes["wallRange"]

        val avgDist = result.stats["avgDistance"]?.let { "%.2f".format(it) } ?: "?"

        return buildString {
            append("§e✓ Range Analysis §7(recommendations only - not auto-applied)\n")
            append("§7  Average combat distance: ${avgDist}m\n")
            if (rangeChange != null) {
                append("§7  • Suggested Range: §f${rangeChange.newValue} §7(${rangeChange.reasoning})\n")
            }
            if (wallRangeChange != null) {
                append("§7  • Suggested WallRange: §f${wallRangeChange.newValue} §7(${wallRangeChange.reasoning})\n")
            }
            append("§8  Note: Manually adjust in ClickGUI → KillAura")
        }
    }
}
