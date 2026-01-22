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

package net.ccbluex.liquidbounce.features.command.commands.deeplearn

import net.ccbluex.liquidbounce.deeplearn.data.CombatSample
import net.ccbluex.liquidbounce.deeplearn.data.KillAuraConfigSample
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.*
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.modes.*
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCombatRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugCombatTrainerRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.DebugKillAuraConfigRecorder
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

object CommandKillAuraAutoConfig : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("killaura")
            .hub()
            .subcommand(autoConfigCommand())
            .build()
    }

    private fun autoConfigCommand(): Command {
        return CommandBuilder
            .begin("autoconfig")
            .parameter(
                ParameterBuilder
                    .begin<String>("mode")
                    .optional()
                    .build()
            )
            .handler {
                val requestedMode = args.getOrNull(0) as String?

                if (requestedMode == null) {
                    throw CommandException("§cUsage: .killaura autoconfig [Linear|Sigmoid|Interpolation|Acceleration]".asText())
                }

                val validModes = listOf("Linear", "Sigmoid", "Interpolation", "Acceleration")
                if (requestedMode.lowercase() !in validModes.map { it.lowercase() }) {
                    throw CommandException("§cInvalid mode. Valid modes: ${validModes.joinToString(", ")}".asText())
                }

                chat("§7Loading combat samples...")

                // Try to load advanced KillAuraConfig samples first
                val (advancedSamples, advancedTime) = measureTimedValue {
                    KillAuraConfigSample.parse(DebugKillAuraConfigRecorder.folder)
                }

                // Also load basic combat samples
                val (basicSamples, basicTime) = measureTimedValue {
                    CombatSample.parse(
                        DebugCombatRecorder.folder,
                        DebugCombatTrainerRecorder.folder
                    )
                }

                // Combine combat data from both sources
                val allCombatSamples = basicSamples + advancedSamples.map { it.combatData }

                if (allCombatSamples.isEmpty() && advancedSamples.isEmpty()) {
                    throw CommandException("§cNo combat samples found. Use DebugRecorder to collect data first.".asText())
                }

                chat("§a✓ Loaded ${basicSamples.size} basic + ${advancedSamples.size} advanced samples")

                analyzeAndApplyForMode(allCombatSamples, advancedSamples, requestedMode)
            }
            .build()
    }

    private fun analyzeAndApplyForMode(
        samples: List<CombatSample>,
        advancedSamples: List<KillAuraConfigSample>,
        mode: String
    ) {
        val baseAnalyzers = listOf(
            ErrorAnalyzer,
            RangeAnalyzer,
            RotationAnalyzer
        )

        // Add mode-specific analyzer
        val modeAnalyzer = when (mode.lowercase()) {
            "linear" -> LinearModeAnalyzer
            "sigmoid" -> SigmoidModeAnalyzer
            "interpolation" -> InterpolationModeAnalyzer
            "acceleration" -> AccelerationModeAnalyzer
            else -> AccelerationModeAnalyzer // Default fallback
        }

        val allAnalyzers = baseAnalyzers + modeAnalyzer

        val results = mutableListOf<AnalysisResult>()

        // Run analysis and apply synchronously on main thread
        val analysisTime = measureTime {
            for (analyzer in allAnalyzers) {
                // Use analyzeAdvanced for mode analyzers if advanced samples available
                // This allows filtering by raycastHit (cursor on enemy)
                val result = if (advancedSamples.isNotEmpty() && analyzer == modeAnalyzer) {
                    analyzer.analyzeAdvanced(advancedSamples)
                } else {
                    analyzer.analyze(samples)
                }
                results.add(result)
                analyzer.apply(result)
            }
        }

        chat("§a✓ Analysis complete in ${analysisTime.toString(DurationUnit.MILLISECONDS, decimals = 0)}ms")
        chat("")
        chat("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        chat("§eKillAura AutoConfig: §b$mode §eMode")
        chat("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        chat("")
        chat("§7Analysis for mode: §b$mode")
        chat("")

        for (result in results) {
            when (result.category) {
                "Error" -> chat(ErrorAnalyzer.report(result))
                "Range" -> chat(RangeAnalyzer.report(result))
                "Rotation" -> chat(RotationAnalyzer.report(result))
                "LinearMode" -> chat(LinearModeAnalyzer.report(result))
                "SigmoidMode" -> chat(SigmoidModeAnalyzer.report(result))
                "InterpolationMode" -> chat(InterpolationModeAnalyzer.report(result))
                "AccelerationMode" -> chat(AccelerationModeAnalyzer.report(result))
            }
            chat("")
        }

        // Advanced analysis from KillAuraConfig data if available
        if (advancedSamples.isNotEmpty()) {
            chat("§6━━━━━ Advanced Analysis ━━━━━")
            chat("")

            // Comprehensive analysis and apply
            val comprehensiveResult = KillAuraConfigApplier.analyze(advancedSamples)
            chat(KillAuraConfigApplier.report(comprehensiveResult))
            chat("")

            // Apply comprehensive settings
            if (comprehensiveResult != null) {
                KillAuraConfigApplier.apply(comprehensiveResult)
                chat("")
            }
        }

        chat("§6Mode-Specific Tuning for §b$mode§6:")
        chat("  §a✓ Rotation mode and settings have been applied automatically!")
        if (advancedSamples.isNotEmpty()) {
            chat("  §a✓ Range, WallRange, ScanExtraRange applied!")
            chat("  §a✓ CPS, KeepSprint, FailSwing, AutoBlock applied!")
            chat("  §a✓ IgnoreOpenInventory setting applied!")
        } else {
            chat("  §8• Tip: Use DebugRecorder → KillAuraConfig mode for comprehensive config")
        }
        chat("")
        chat("§a✓ AutoConfig finished! All settings applied.")
    }
}
