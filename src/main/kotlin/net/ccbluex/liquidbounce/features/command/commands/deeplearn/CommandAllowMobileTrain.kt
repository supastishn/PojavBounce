/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * AllowMobileTrain Command
 *
 * Allows you to enable or disable training on mobile devices.
 */
object CommandAllowMobileTrain : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("allowMobileTrain")
            .alias("amt")
            .parameter(
                ParameterBuilder
                    .begin<Boolean>("enabled")
                    .optional()
                    .build()
            )
            .handler { command, args ->
                if (args.isEmpty()) {
                    showCurrentStatus(command)
                } else {
                    setTrainingAllowed(command, args[0] as Boolean)
                }
            }
            .build()
    }

    private fun showCurrentStatus(command: Command) {
        val currentStatus = DeepLearningEngine.isMobileTrainingAllowed
        val androidStatus = if (DeepLearningEngine.runningOnAndroid) "yes" else "no"
        
        chat(regular(command.result("status", 
            variable(if (currentStatus) "enabled" else "disabled"),
            variable(androidStatus)
        )))
    }

    private fun setTrainingAllowed(command: Command, enabled: Boolean) {
        DeepLearningEngine.isMobileTrainingAllowed = enabled
        
        val statusText = if (enabled) command.result("enabled") else command.result("disabled")
        chat(regular(command.result("changed", variable(statusText))))
        
        showAndroidWarningIfNeeded(command, enabled)
    }

    private fun showAndroidWarningIfNeeded(command: Command, enabled: Boolean) {
        if (DeepLearningEngine.runningOnAndroid && !enabled) {
            chat(regular(command.result("androidWarning")))
        }
    }
}
