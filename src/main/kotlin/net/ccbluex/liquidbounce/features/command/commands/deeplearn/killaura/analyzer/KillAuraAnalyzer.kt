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

interface KillAuraAnalyzer {
    fun analyze(samples: List<CombatSample>): AnalysisResult
    fun apply(result: AnalysisResult)
    fun report(result: AnalysisResult): String
}

data class AnalysisResult(
    val category: String,
    val changes: Map<String, SettingChange> = emptyMap(),
    val stats: Map<String, Double> = emptyMap(),
    val confidence: Float = 1f
)

data class SettingChange(
    val settingName: String,
    val oldValue: Any?,
    val newValue: Any?,
    val reasoning: String
)
