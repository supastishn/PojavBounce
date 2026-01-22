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
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.utils.client.chat

/**
 * Logs Command
 *
 * Allows managing debug-recorder logs.
 */
object CommandLogs : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("logs")
            .subcommand(
                CommandBuilder
                    .begin("delete")
                    .handler {
                        val logsFolder = ConfigSystem.rootFolder.resolve("debug-recorder")

                        if (!logsFolder.exists()) {
                            chat("§cNo logs folder found.")
                            return@handler
                        }

                        var deletedCount = 0
                        logsFolder.listFiles()?.forEach { subfolder ->
                            if (subfolder.isDirectory) {
                                subfolder.listFiles()?.forEach { file ->
                                    if (file.delete()) deletedCount++
                                }
                            }
                        }

                        chat("§aDeleted §f$deletedCount §alogs.")
                    }
                    .build()
            )
            .build()
    }

}
