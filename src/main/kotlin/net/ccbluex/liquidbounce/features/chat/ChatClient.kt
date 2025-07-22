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
package net.ccbluex.liquidbounce.features.chat

import net.ccbluex.liquidbounce.utils.client.logger

/**
 * Stub implementation of ChatClient for native GUI migration
 * 
 * This replaces the WebSocket-based chat functionality with no-op stubs
 * since the chat system requires WebSocket libraries that were removed.
 */
class ChatClient {
    
    private val logger = logger()
    
    val connected: Boolean = false
    val loggedIn: Boolean = false
    val isConnected: Boolean = false
    
    fun connect() {
        logger.info("ChatClient.connect() called - chat functionality is disabled during native GUI migration")
    }
    
    fun disconnect() {
        logger.info("ChatClient.disconnect() called - no-op for stub implementation")
    }
    
    fun sendMessage(message: String) {
        logger.info("ChatClient.sendMessage() called - chat functionality is disabled")
    }
    
    fun reconnect() {
        logger.info("ChatClient.reconnect() called - no-op for stub implementation")
    }
    
    suspend fun connectAsync() {
        logger.info("ChatClient.connectAsync() called - chat functionality is disabled")
    }
    
    fun sendPacket(packet: Any) {
        logger.info("ChatClient.sendPacket() called - chat functionality is disabled")
    }
}