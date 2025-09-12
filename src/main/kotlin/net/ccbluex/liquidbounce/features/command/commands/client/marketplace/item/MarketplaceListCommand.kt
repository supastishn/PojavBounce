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
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.utils.client.*

/**
 * List marketplace items
 */
@Suppress("CognitiveComplexMethod")
fun marketplaceListCommand() = CommandBuilder
    .begin("list")
    .parameter(
        ParameterBuilder.Companion.enumChoice<MarketplaceItemType>("type") { it.isListable }
            .required()
            .build()
    )
    .parameter(
        ParameterBuilder
            .begin<Int>("page")
            .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
            .optional()
            .build()
    )
    .parameter(
        ParameterBuilder
            .begin<Boolean>("featured")
            .verifiedBy(ParameterBuilder.BOOLEAN_VALIDATOR)
            .optional()
            .build()
    )
    .suspendHandler { command, args ->
        val type = args[0] as MarketplaceItemType
        val page = args.getOrNull(1) as Int? ?: 1
        val featured = args.getOrNull(2) as Boolean? ?: false
        
        // Stubbed for native GUI - marketplace operations handled through web interface
        throw CommandException(regular("Marketplace item listing requires web interface access"))
    }
    .build()