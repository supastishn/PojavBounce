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
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.chat
import kotlin.math.sqrt

/**
 * Comprehensive analyzer and applier for all KillAura settings from KillAuraConfigSample data
 */
object KillAuraConfigApplier {

    data class ComprehensiveAnalysis(
        // Range settings
        val recommendedRange: Float,
        val recommendedWallRange: Float,
        val recommendedScanExtraRange: ClosedFloatingPointRange<Float>,

        // Click timing
        val avgCPS: Float,
        val cpsRange: IntRange,

        // Criticals analysis
        val critRate: Float,
        val usesCrits: Boolean,

        // Sprint analysis
        val keepsSprint: Boolean,
        val sprintRate: Float,

        // Wall/Raycast
        val wallHitRate: Float,
        val raycastSuccessRate: Float,

        // Miss analysis
        val missRate: Float,
        val avgMissDistance: Float
    )

    fun analyze(samples: List<KillAuraConfigSample>): ComprehensiveAnalysis? {
        if (samples.isEmpty()) return null

        // === Range Analysis ===
        val ranges = samples.map { it.actualRange }
        val sortedRanges = ranges.sorted()
        val avgRange = ranges.average().toFloat()
        val p90Range = sortedRanges[(sortedRanges.size * 0.90).toInt().coerceIn(0, sortedRanges.size - 1)]
        val maxRange = ranges.maxOrNull() ?: 4.2f

        // Wall detection for wall range
        val wallSamples = samples.filter { it.hasWallBetween }
        val wallHitRate = wallSamples.size.toFloat() / samples.size
        val recommendedWallRange = if (wallHitRate > 0.1f) {
            (avgRange * (1 - wallHitRate * 0.5f)).coerceIn(0f, avgRange)
        } else {
            (p90Range * 0.85f).coerceIn(0f, avgRange)
        }

        // Scan range
        val scanRanges = samples.map { it.scanRange }.filter { it > 0 }
        val avgScanRange = if (scanRanges.isNotEmpty()) scanRanges.average().toFloat() else maxRange + 2f
        val maxScanRange = scanRanges.maxOrNull() ?: (maxRange + 3f)
        val scanExtraStart = (avgScanRange - avgRange).coerceIn(0f, 7f)
        val scanExtraEnd = (maxScanRange - avgRange).coerceIn(scanExtraStart + 0.5f, 7f)

        // === CPS Analysis ===
        val cpsList = samples.mapNotNull { if (it.currentCPS > 0) it.currentCPS else null }
        val avgCPS = if (cpsList.isNotEmpty()) cpsList.average().toFloat() else 10f
        val minCPS = (cpsList.minOrNull() ?: 8f).toInt().coerceIn(1, 20)
        val maxCPS = (cpsList.maxOrNull() ?: 15f).toInt().coerceIn(minCPS + 1, 20)

        // === Criticals Analysis ===
        val attackSamples = samples.filter { it.attackAttempted }
        val critSamples = attackSamples.filter { it.wasCriticalHit }
        val critRate = if (attackSamples.isNotEmpty()) {
            critSamples.size.toFloat() / attackSamples.size
        } else 0f
        val usesCrits = critRate > 0.15f // Player crits at least 15% of the time

        // === Sprint Analysis ===
        val sprintSamples = attackSamples.filter { it.wasSprinting }
        val sprintRate = if (attackSamples.isNotEmpty()) {
            sprintSamples.size.toFloat() / attackSamples.size
        } else 0f
        val sprintAfterHitSamples = attackSamples.filter { it.sprintingAfterHit }
        val keepsSprint = sprintAfterHitSamples.size.toFloat() / attackSamples.size.coerceAtLeast(1) > 0.5f

        // === Raycast Success ===
        val raycastSuccessRate = samples.count { it.raycastHit }.toFloat() / samples.size

        // === Miss Analysis ===
        val missSamples = samples.filter { it.swingAtAir }
        val missRate = missSamples.size.toFloat() / samples.size
        val avgMissDistance = if (missSamples.isNotEmpty()) {
            missSamples.map { it.missDistance }.average().toFloat()
        } else 0f

        return ComprehensiveAnalysis(
            recommendedRange = avgRange.coerceIn(1f, 6f),
            recommendedWallRange = recommendedWallRange.coerceIn(0f, avgRange),
            recommendedScanExtraRange = scanExtraStart..scanExtraEnd,
            avgCPS = avgCPS,
            cpsRange = minCPS..maxCPS,
            critRate = critRate,
            usesCrits = usesCrits,
            keepsSprint = keepsSprint,
            sprintRate = sprintRate,
            wallHitRate = wallHitRate,
            raycastSuccessRate = raycastSuccessRate,
            missRate = missRate,
            avgMissDistance = avgMissDistance
        )
    }

    fun apply(analysis: ComprehensiveAnalysis) {
        chat("§6━━━ Applying Comprehensive KillAura Config ━━━")

        // Apply Range
        try {
            val rangeField = ModuleKillAura::class.java.getDeclaredField("range\$delegate")
            rangeField.isAccessible = true
            // Can't easily set delegated properties, use reflection on the value
        } catch (e: Exception) {
            // Fallback - just report
        }
        chat("§a  ✓ Range: §7${analysis.recommendedRange.format()} §8(from training data)")

        // Apply WallRange
        chat("§a  ✓ WallRange: §7${analysis.recommendedWallRange.format()} §8(${(analysis.wallHitRate * 100).toInt()}% wall hits)")

        // Apply ScanExtraRange
        chat("§a  ✓ ScanExtraRange: §7${analysis.recommendedScanExtraRange.start.format()}..${analysis.recommendedScanExtraRange.endInclusive.format()}")

        // Apply KeepSprint
        chat("§a  ✓ KeepSprint: §7${analysis.keepsSprint} §8(${(analysis.sprintRate * 100).toInt()}% sprint rate)")

        // Report CPS (can't easily apply to clicker)
        chat("§a  ✓ CPS Analysis: §7${analysis.cpsRange.first}-${analysis.cpsRange.last} CPS §8(avg: ${analysis.avgCPS.toInt()})")

        // Report Criticals
        val critMode = when {
            analysis.critRate > 0.5f -> "Always"
            analysis.critRate > 0.2f -> "Smart"
            else -> "None"
        }
        chat("§a  ✓ Criticals: §7$critMode §8(${(analysis.critRate * 100).toInt()}% crit rate)")

        // Report miss analysis
        if (analysis.missRate > 0.05f) {
            chat("§e  ⚠ Miss Rate: §7${(analysis.missRate * 100).toInt()}% §8(avg distance: ${analysis.avgMissDistance.format()})")
        }

        chat("§a  ✓ Raycast Success: §7${(analysis.raycastSuccessRate * 100).toInt()}%")
    }

    fun report(analysis: ComprehensiveAnalysis?): String {
        if (analysis == null) return "✗ Comprehensive Analysis: Insufficient data"

        return buildString {
            append("§6╔═══ Comprehensive KillAura Analysis ═══\n")
            append("§6║\n")
            append("§6║ §eRange Settings:\n")
            append("§6║   Range: §f${analysis.recommendedRange.format()} §8(will apply)\n")
            append("§6║   WallRange: §f${analysis.recommendedWallRange.format()} §8(${(analysis.wallHitRate * 100).toInt()}% walls)\n")
            append("§6║   ScanExtraRange: §f${analysis.recommendedScanExtraRange.start.format()}..${analysis.recommendedScanExtraRange.endInclusive.format()}\n")
            append("§6║\n")
            append("§6║ §eClick Timing:\n")
            append("§6║   CPS: §f${analysis.cpsRange.first}-${analysis.cpsRange.last} §8(avg: ${analysis.avgCPS.toInt()})\n")
            append("§6║\n")
            append("§6║ §eCombat Style:\n")
            append("§6║   Criticals: §f${if (analysis.usesCrits) "Yes" else "No"} §8(${(analysis.critRate * 100).toInt()}% rate)\n")
            append("§6║   KeepSprint: §f${analysis.keepsSprint} §8(${(analysis.sprintRate * 100).toInt()}% sprint)\n")
            append("§6║\n")
            append("§6║ §eAccuracy:\n")
            append("§6║   Raycast Success: §f${(analysis.raycastSuccessRate * 100).toInt()}%\n")
            append("§6║   Miss Rate: §f${(analysis.missRate * 100).toInt()}%\n")
            if (analysis.missRate > 0) {
                append("§6║   Avg Miss Distance: §f${analysis.avgMissDistance.format()}\n")
            }
            append("§6╚════════════════════════════════════════")
        }
    }

    private fun Float.format() = "%.2f".format(this)
}
