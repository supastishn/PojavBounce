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
package net.ccbluex.liquidbounce.api.services.auth

import net.ccbluex.liquidbounce.utils.client.logger
import kotlin.coroutines.suspendCoroutine

/**
 * OAuth Client - Simplified stub version for native GUI migration
 * 
 * This is a temporary stub implementation that removes the Netty HTTP server dependency.
 * OAuth functionality is disabled until a proper native implementation is created.
 */
object OAuthClient {

    private val logger = logger()

    /**
     * Stub implementation of OAuth authentication
     * 
     * TODO: Implement proper OAuth flow without Netty dependency
     * For now, this throws an exception to indicate OAuth is not available
     */
    suspend fun startAuth(redirectUri: String): String = suspendCoroutine { cont ->
        logger.warn("OAuth authentication is currently disabled due to native GUI migration")
        logger.warn("OAuth functionality will be restored in a future update")
        
        // For now, fail gracefully
        cont.resumeWith(Result.failure(
            UnsupportedOperationException("OAuth authentication is temporarily disabled during native GUI migration")
        ))
    }

    /**
     * Stub implementation of token renewal
     * 
     * TODO: Implement proper token renewal without Netty dependency
     */
    suspend fun renewToken(session: Any): Any = suspendCoroutine { cont ->
        logger.warn("OAuth token renewal is currently disabled due to native GUI migration")
        
        // For now, fail gracefully
        cont.resumeWith(Result.failure(
            UnsupportedOperationException("OAuth token renewal is temporarily disabled during native GUI migration")
        ))
    }

}