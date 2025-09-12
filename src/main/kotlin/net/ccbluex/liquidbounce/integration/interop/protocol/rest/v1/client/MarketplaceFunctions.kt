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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonObject

/**
 * Stubbed marketplace REST API functions for native GUI approach
 * 
 * These functions provide no-op implementations since the native GUI
 * does not require web server integration for marketplace operations.
 */

// Stub for RequestObject that doesn't exist in native approach
data class RequestObject(
    val queryParams: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap()
)

// Stub HTTP response functions
fun httpOk(data: Any): String = "OK"
fun httpForbidden(message: String): String = "Forbidden: $message"

/**
 * GET /api/v1/marketplace - Stubbed for native GUI
 */
fun getMarketplaceItems(requestObject: RequestObject): String {
    // Stubbed - marketplace operations handled through native GUI
    return httpOk("Marketplace integration requires web interface")
}

/**
 * POST /api/v1/marketplace/subscriptions - Stubbed for native GUI
 */
fun postMarketplaceSubscription(requestObject: RequestObject): String {
    // Stubbed - subscription operations handled through native GUI
    return httpOk("Subscription operations require web interface")
}

/**
 * DELETE /api/v1/marketplace/subscriptions/:id - Stubbed for native GUI
 */
fun deleteMarketplaceSubscription(requestObject: RequestObject): String {
    // Stubbed - subscription operations handled through native GUI
    return httpOk("Subscription operations require web interface")
}

/**
 * POST /api/v1/marketplace/items - Stubbed for native GUI
 */
fun postMarketplaceItem(requestObject: RequestObject): String {
    // Stubbed - item creation handled through native GUI
    return httpOk("Item creation requires web interface")
}

/**
 * GET /api/v1/marketplace/items/:id - Stubbed for native GUI
 */
fun getMarketplaceItem(requestObject: RequestObject): String {
    // Stubbed - item details handled through native GUI
    return httpOk("Item details require web interface")
}

/**
 * DELETE /api/v1/marketplace/items/:id - Stubbed for native GUI
 */
fun deleteMarketplaceItem(requestObject: RequestObject): String {
    // Stubbed - item deletion handled through native GUI
    return httpOk("Item deletion requires web interface")
}