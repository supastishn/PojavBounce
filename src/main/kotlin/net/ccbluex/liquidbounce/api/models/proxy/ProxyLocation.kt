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
package net.ccbluex.liquidbounce.api.models.proxy

import com.google.gson.annotations.SerializedName

data class ProxySubscription(
    @SerializedName("subscription_id")
    val subscriptionId: String,
    @SerializedName("valid_until")
    val validUntil: String,
    val status: Byte, // 0 is unbanned, 1 or higher is banned for a specific reason
    val plans: List<ProxyPlan>,
    val credentials: ProxyCredentials
)

data class ProxyPlan(
    val level: Byte,
    val name: String,
    val active: Boolean
)

data class ProxyCredentials(
    val username: String,
    val password: String
)

data class ProxyLocation(
    val name: String,
    val location: String, // human-readable location
    val region: String, // 2-letter region code
    @SerializedName("country_code")
    val country: String, // 2-letter country code
    @SerializedName("ping_server")
    val pingServer: String? = null
)
