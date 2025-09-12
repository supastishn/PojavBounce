/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace.item

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.utils.client.regular

/**
 * Create marketplace item
 */
fun marketplaceCreateItemCommand() = CommandBuilder
    .begin("create")
    .parameter(
        ParameterBuilder
            .begin<String>("name")
            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
            .required()
            .build()
    )
    .parameter(
        ParameterBuilder.Companion.enumChoice<MarketplaceItemType>("type")
            .required()
            .build()
    )
    .parameter(
        ParameterBuilder
            .begin<String>("description")
            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
            .vararg()
            .required()
            .build()
    )
    .suspendHandler { command, args ->
        val name = args[0] as String
        val type = args[1] as MarketplaceItemType
        val description = (args[2] as Array<String>).joinToString(" ")
        
        // Stubbed for native GUI - marketplace operations handled through web interface
        throw CommandException(regular("Marketplace item creation requires web interface access"))
    }
    .build()