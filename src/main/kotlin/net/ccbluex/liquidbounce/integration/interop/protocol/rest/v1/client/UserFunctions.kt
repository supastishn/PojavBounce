@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import io.netty.handler.codec.http.FullHttpResponse
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.services.auth.OAuthClient.startAuth
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.UserLoggedInEvent
import net.ccbluex.liquidbounce.event.events.UserLoggedOutEvent
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk
import net.ccbluex.netty.http.util.httpUnauthorized
import net.minecraft.util.Util

// GET /api/v1/client/user
@Suppress("UNUSED_PARAMETER")
fun getUser(requestObject: RequestObject): FullHttpResponse {
    val clientAccount = ClientAccountManager.clientAccount

    if (clientAccount == ClientAccount.EMPTY_ACCOUNT) {
        return httpUnauthorized("Not logged in")
    }

    val userInformation = if (clientAccount.userInformation == null) {
        runBlocking {
            clientAccount.updateInfo()
            clientAccount.userInformation
        }
    } else {
        clientAccount.userInformation
    }

    return httpOk(interopGson.toJsonTree(userInformation))
}

// POST /api/v2/client/user/login
@Suppress("UNUSED_PARAMETER")
fun loginUser(requestObject: RequestObject): FullHttpResponse {
    val clientAccount = ClientAccountManager.clientAccount

    if (clientAccount != ClientAccount.EMPTY_ACCOUNT) {
        return httpOk(emptyJsonObject())
    }

    withScope {
        val account = startAuth { Util.getOperatingSystem().open(it) }
        ClientAccountManager.clientAccount = account
        ConfigSystem.storeConfigurable(ClientAccountManager)
        EventManager.callEvent(UserLoggedInEvent())
    }

    return httpOk(emptyJsonObject())
}

// POST /api/v2/client/user/logout
@Suppress("UNUSED_PARAMETER")
fun logoutUser(requestObject: RequestObject): FullHttpResponse {
    val clientAccount = ClientAccountManager.clientAccount
    if (clientAccount == ClientAccount.EMPTY_ACCOUNT) {
        return httpOk(emptyJsonObject())
    }

    ClientAccountManager.clientAccount = ClientAccount.EMPTY_ACCOUNT
    ConfigSystem.storeConfigurable(ClientAccountManager)
    EventManager.callEvent(UserLoggedOutEvent())
    return httpOk(emptyJsonObject())
}
