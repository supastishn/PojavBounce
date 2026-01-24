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

package net.ccbluex.liquidbounce.features.account

import com.mojang.authlib.yggdrasil.YggdrasilEnvironment
import com.mojang.authlib.yggdrasil.YggdrasilUserApiService
import net.ccbluex.liquidbounce.authlib.account.AlteningAccount
import net.ccbluex.liquidbounce.authlib.account.CrackedAccount
import net.ccbluex.liquidbounce.authlib.account.MicrosoftAccount
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount
import net.ccbluex.liquidbounce.authlib.account.SessionAccount
import net.ccbluex.liquidbounce.authlib.yggdrasil.clientIdentifier
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AccountManagerAdditionResultEvent
import net.ccbluex.liquidbounce.event.events.AccountManagerLoginResultEvent
import net.ccbluex.liquidbounce.event.events.AccountManagerRemovalResultEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.with
import net.minecraft.client.multiplayer.ProfileKeyPairManager
import java.net.Proxy
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
object AccountManager : Configurable("Accounts"), EventListener {

    val accounts by list(name, mutableListOf<MinecraftAccount>(), ValueType.ACCOUNT)

    private var initialSession: SessionBundle

    private val loggingIn = AtomicBoolean(false)

    init {
        ConfigSystem.root(this)

        try {
            initialSession = SessionBundle(mc.user, mc.services.sessionService, mc.profileKeyPairManager)
            logger.info("Initial session saved: ${mc.user.name} (${mc.user.profileId})")
        } catch (e: Exception) {
            logger.error("Failed to save initial session", e)
            initialSession = SessionBundle(mc.user, null, ProfileKeyPairManager.EMPTY_KEY_MANAGER)
        }
    }

    fun loginAccount(id: Int) {
        if (!loggingIn.compareAndSet(false, true)) {
            EventManager.callEvent(AccountManagerLoginResultEvent(error = "Logging in already started!"))
            return
        }

        val account = accounts.getOrNull(id) ?: run {
            EventManager.callEvent(AccountManagerLoginResultEvent(error = "Account not found!"))
            return
        }
        loginDirectAccount(account)
        loggingIn.set(false)
    }

    fun loginDirectAccount(account: MinecraftAccount) = try {
        logger.info("Start logging in with username '${account.profile?.username}'")
        val (compatSession, service) = account.login()
        val session = SessionWithService(
            compatSession.username, compatSession.uuid, compatSession.token,
            Optional.empty(),
            Optional.of(clientIdentifier),
            AccountService.getService(account)
        )

        val profileKeys = runCatching {
            // In this case the environment doesn't matter, as it is only used for the profile key
            val environment = YggdrasilEnvironment.PROD.environment
            val userAuthenticationService = YggdrasilUserApiService(session.accessToken, Proxy.NO_PROXY, environment)
            ProfileKeyPairManager.create(userAuthenticationService, session, mc.gameDirectory.toPath())
        }.onFailure {
            logger.error("Failed to create profile keys for ${session.name} due to ${it.message}")
        }.getOrDefault(ProfileKeyPairManager.EMPTY_KEY_MANAGER)

        mc.user = session
        mc.services = mc.services.with(
            service.createMinecraftSessionService(),
            service.servicesKeySet,
            service.createProfileRepository(),
        )
        mc.profileKeyPairManager = profileKeys

        EventManager.callEvent(SessionEvent(session))
        EventManager.callEvent(AccountManagerLoginResultEvent(username = account.profile?.username))
    } catch (e: Exception) {
        logger.error("Failed to login into account", e)
        EventManager.callEvent(AccountManagerLoginResultEvent(error = e.message ?: "Unknown error"))
    }

    /**
     * Cracked account. This can only be used to join cracked servers and not premium servers.
     */
    fun newCrackedAccount(username: String, online: Boolean = false) {
        if (username.isEmpty()) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Username is empty!"))
            return
        }

        if (username.length > 16) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Username is too long!"))
            return
        }

        // Check if account already exists
        if (accounts.any { it.profile?.username.equals(username, true) }) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Account already exists!"))
            return
        }

        // Create new cracked account
        accounts += CrackedAccount(username, online).also { it.refresh() }

        // Store configurable
        ConfigSystem.store(this@AccountManager)

        EventManager.callEvent(AccountManagerAdditionResultEvent(username = username))
    }

    fun loginCrackedAccount(username: String, online: Boolean = false) {
        if (username.isEmpty()) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Username is empty!"))
            return
        }

        if (username.length > 16) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Username is too long!"))
            return
        }

        val account = CrackedAccount(username, online).also { it.refresh() }
        loginDirectAccount(account)
    }

    fun loginSessionAccount(token: String) {
        val account = SessionAccount(token).also { it.refresh() }
        loginDirectAccount(account)
    }

    /**
     * Cache microsoft login server
     */
    private var activeUrl: String? = null

    fun newMicrosoftAccount(url: (String) -> Unit) {
        // Prevents you from starting multiple login attempts
        val activeUrl = activeUrl
        if (activeUrl != null) {
            url(activeUrl)
            return
        }

        runCatching {
            newMicrosoftAccount(url = {
                this.activeUrl = it

                url(it)
            }, success = { account ->
                val profile = account.profile
                if (profile == null) {
                    logger.error("Failed to get profile")
                    EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
                    return@newMicrosoftAccount
                }

                EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.username))
                this.activeUrl = null
            }, error = { errorString ->
                logger.error("Failed to create new account: $errorString")

                EventManager.callEvent(AccountManagerAdditionResultEvent(error = errorString))
                this.activeUrl = null
            })
        }.onFailure {
            logger.error("Failed to create new account", it)

            EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
            this.activeUrl = null
        }
    }

    /**
     * Create a new Microsoft Account using the OAuth2 flow which opens a browser window to authenticate the user
     */
    private fun newMicrosoftAccount(url: (String) -> Unit, success: (account: MicrosoftAccount) -> Unit,
                                    error: (error: String) -> Unit) {
        MicrosoftAccount.buildFromOpenBrowser(object : MicrosoftAccount.OAuthHandler {

            /**
             * Called when the user has cancelled the authentication process or the thread has been interrupted
             */
            override fun authError(error: String) {
                // Oh no, something went wrong. Callback with error.
                logger.error("Failed to login: $error")
                error(error)
            }

            /**
             * Called when the user has completed authentication
             */
            override fun authResult(account: MicrosoftAccount) {
                // Yay, it worked! Callback with account.
                logger.info("Logged in as new account ${account.profile?.username}")

                val existingAccount = accounts.find {
                    it.type == account.type && it.profile?.username == account.profile?.username
                }

                if (existingAccount != null) {
                    // Replace existing account
                    accounts[accounts.indexOf(existingAccount)] = account
                } else {
                    // Add account to list of accounts
                    accounts += account
                }

                runCatching {
                    success(account)
                }.onFailure {
                    logger.error("Internal error", it)
                }

                // Store configurable
                ConfigSystem.store(this@AccountManager)
            }

            /**
             * Called when the server has prepared the user for authentication
             */
            override fun openUrl(url: String) {
                url(url)
            }

        })
    }

    fun newAlteningAccount(accountToken: String) = runCatching {
        accounts += AlteningAccount.fromToken(accountToken).apply {
            val profile = this.profile

            if (profile == null) {
                EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
                return@runCatching
            }

            EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.username))
        }

        // Store configurable
        ConfigSystem.store(this@AccountManager)
    }.onFailure {
        logger.error("Failed to login into altening account (for add-process)", it)
        EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
    }

    fun generateAlteningAccount(apiToken: String) = runCatching {
        if (apiToken.isEmpty()) {
            error("Altening API Token is empty!")
        }

        val account = AlteningAccount.generateAccount(apiToken)
        accounts += account

        // Store configurable
        ConfigSystem.store(this@AccountManager)

        account
    }.onFailure {
        logger.error("Failed to generate altening account", it)
        EventManager.callEvent(AccountManagerAdditionResultEvent(error = it.message ?: "Unknown error"))
    }.onSuccess {
        val profile = it.profile

        if (profile == null) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
            return@onSuccess
        }

        EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.username))
    }

    fun restoreInitial() {
        val initialSession = initialSession
        mc.user = initialSession.session
        mc.services = mc.services.with(
            initialSession.sessionService ?: mc.services.sessionService
        )
        mc.profileKeyPairManager = initialSession.profileKeys

        EventManager.callEvent(SessionEvent(mc.user))
        EventManager.callEvent(AccountManagerLoginResultEvent(username = mc.user.name))
    }

    /**
     * Login with a random cracked account using a fully randomized username.
     */
    fun loginRandomCrackedAccount() {
        val randomUsername = generateRandomUsername()
        loginCrackedAccount(randomUsername)
    }

    /**
     * Generates a fully random Minecraft-style username (3-16 characters).
     * Uses a mix of adjectives, nouns, and random suffixes for variety.
     */
    private fun generateRandomUsername(): String {
        val adjectives = listOf(
            "Cool", "Fast", "Swift", "Dark", "Light", "Fire", "Ice", "Storm", "Shadow", "Bright",
            "Wild", "Brave", "Silent", "Loud", "Crazy", "Lucky", "Happy", "Angry", "Sneaky", "Epic",
            "Ultra", "Mega", "Super", "Hyper", "Toxic", "Neon", "Cyber", "Pixel", "Astro", "Turbo",
            "Blazing", "Frozen", "Golden", "Silver", "Crystal", "Phantom", "Stealth", "Mystic", "Chaos", "Zen"
        )

        val nouns = listOf(
            "Wolf", "Fox", "Bear", "Lion", "Tiger", "Eagle", "Hawk", "Dragon", "Phoenix", "Ninja",
            "Knight", "Mage", "Wizard", "Hunter", "Slayer", "King", "Queen", "Lord", "Ace", "Pro",
            "Gamer", "Sniper", "Tank", "Healer", "Rogue", "Archer", "Blade", "Sword", "Arrow", "Shield",
            "Storm", "Thunder", "Flame", "Frost", "Star", "Moon", "Sun", "Nova", "Comet", "Vortex"
        )

        val styles = listOf(
            // Style 1: Adjective + Noun (e.g., "CoolWolf")
            { "${adjectives.random()}${nouns.random()}" },
            // Style 2: Noun + Numbers (e.g., "Dragon847")
            { "${nouns.random()}${(10..999).random()}" },
            // Style 3: Adjective + Noun + Numbers (e.g., "FastFox42")
            { "${adjectives.random()}${nouns.random()}${(1..99).random()}" },
            // Style 4: x + Noun + x (e.g., "xDragonx")
            { "x${nouns.random()}x" },
            // Style 5: Noun + Underscore + Noun (e.g., "Wolf_King")
            { "${nouns.random()}_${nouns.random()}" },
            // Style 6: Double letters prefix (e.g., "xxNinjax")
            { "xx${nouns.random()}x" },
            // Style 7: The + Noun + Numbers (e.g., "TheWolf99")
            { "The${nouns.random()}${(1..99).random()}" },
            // Style 8: iNoun or iiNoun (e.g., "iWizard", "iiMage")
            { "${"i".repeat((1..2).random())}${nouns.random()}" },
            // Style 9: Random alphanumeric (e.g., "Kx7mP2nQ")
            { (1..(8..12).random()).map { "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString("") },
            // Style 10: Noun + OG/YT/TTV suffix (e.g., "DragonOG")
            { "${nouns.random()}${listOf("OG", "YT", "TTV", "HD", "HQ", "PvP").random()}" }
        )

        var username = styles.random()()

        // Ensure username is within valid length (3-16 characters)
        if (username.length > 16) {
            username = username.take(16)
        }
        if (username.length < 3) {
            username = username + (100..999).random()
        }

        return username
    }

    fun favoriteAccount(id: Int) {
        val account = accounts.getOrNull(id) ?: error("Account not found!")
        account.favorite()
        ConfigSystem.store(this@AccountManager)
    }

    fun unfavoriteAccount(id: Int) {
        val account = accounts.getOrNull(id) ?: error("Account not found!")
        account.unfavorite()
        ConfigSystem.store(this@AccountManager)
    }

    fun swapAccounts(index1: Int, index2: Int) {
        val account1 = accounts.getOrNull(index1) ?: error("Account not found!")
        val account2 = accounts.getOrNull(index2) ?: error("Account not found!")
        accounts[index1] = account2
        accounts[index2] = account1
        ConfigSystem.store(this@AccountManager)
    }

    fun orderAccounts(order: List<Int>) {
        order.map { index -> accounts[index] }
            .forEachIndexed { index, serverInfo ->
                accounts[index] = serverInfo
            }

        ConfigSystem.store(this@AccountManager)
    }

    fun removeAccount(id: Int): MinecraftAccount {
        val account = accounts.removeAt(id).apply { ConfigSystem.store(this@AccountManager) }
        EventManager.callEvent(AccountManagerRemovalResultEvent(account.profile?.username))
        return account
    }

    fun newSessionAccount(token: String) {
        if (token.isEmpty()) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Token is empty!"))
            return
        }

        // Create a new cracked account
        val account = SessionAccount(token)
        try {
            account.refresh()
        } catch (exception: Exception) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = exception.message ?: "Unknown error"))
            return
        }

        val profile = account.profile

        if (profile == null) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Failed to get profile"))
            return
        }

        // Check if an account already exists
        if (accounts.any { it.profile?.username.equals(profile.username, true) }) {
            EventManager.callEvent(AccountManagerAdditionResultEvent(error = "Account already exists!"))
            return
        }

        // Store configurable
        accounts += account
        ConfigSystem.store(this@AccountManager)
        EventManager.callEvent(AccountManagerAdditionResultEvent(username = profile.username))
    }

}
