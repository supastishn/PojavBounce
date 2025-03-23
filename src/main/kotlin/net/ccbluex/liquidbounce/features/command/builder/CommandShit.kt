package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.features.command.*
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.text.Text
import kotlin.jvm.Throws
import kotlin.jvm.optionals.getOrNull

typealias PositiveInt = Int

typealias CommandReference = () -> CommandTemplate

sealed class CommandTemplate(
    val name: String,
    val aliases: Array<String> = emptyArray(),
    val parent: CommandReference? = null,
    val requireIngame: Boolean = false
): ParameterContainer() {
    val canonicalName: String
        get() {
            val parent = this.parent?.invoke() ?: return this.name

            return "${parent.canonicalName} ${this.name}"
        }
    val usageInfo: List<String> by lazy { usage() }

    fun failCommand(key: String, vararg args: Any, cause: Throwable? = null): Nothing {
        val translationText =  TODO() //translation("$translationBaseKey.result.$key", args = args)

        throw CommandException(translationText, usageInfo = TODO(), cause = cause)
    }

    fun chatCommand(key: String, vararg args: Any, metadata: MessageMetadata) {
        val translationText: Text = TODO()// translation("$translationBaseKey.result.$key", args = args)

        chat(translationText, metadata = metadata)
    }
    fun chatCommand(key: String, vararg args: Any) {
        val translationText: Text =  TODO() //translation("$translationBaseKey.result.$key", args = args)

        chat(translationText, command = TODO())
    }

    /**
     * Checks if the [name] refers to the name or an alias of this command.
     *
     * For example, A command with the name `username` and the aliases [`ign`, `whoami`] would accept all the given
     * strings.
     */
    fun isValidAlias(name: String): Boolean {
        if (this.name.equals(name, true)) {
            return true
        }

        return this.aliases.any { it.equals(name, true) }
    }

    private fun usage(): List<String> {
        TODO()
    }


    @Throws(CommandException::class)
    abstract fun executeRaw(lexedParameters: LexedParameters)
}

abstract class ExecutableCommandTemplate(
    name: String,
    aliases: Array<String> = emptyArray(),
    parent: CommandReference? = null,
    requireIngame: Boolean = false,
): CommandTemplate(name = name, aliases = aliases, parent = parent, requireIngame = requireIngame) {
    @Throws(CommandException::class)
    abstract fun execute(params: CommandParameters)

    final override fun executeRaw(lexedParameters: LexedParameters) {
        val params = this.template.parseParams(lexedParameters)

        this.execute(params)
    }
}

private class BindCommand: ExecutableCommandTemplate("bind") {
    private val targetModule = moduleParameter("module", required())
    private val targetKey = inputKeyOrNoneParameter("key", required())

    override fun execute(params: CommandParameters) {
        val module = params[targetModule]
        val key = params[targetKey].getOrNull()

        val translationKey = if (key != null) {
            module.bind.bind(key)

            "moduleBound"
        } else {
            module.bind.unbind()

            "moduleUnbound"
        }

        chatCommand(
            translationKey, variable(module.name), variable(module.bind.keyName),
            metadata = MessageMetadata(id = "Bind#${module.name}")
        )

        ModuleClickGui.reloadView()
    }
}

private class ChatCommand: ExecutableCommandTemplate("chat") {
    private val targetModule = text("text")

    override fun execute(params: CommandParameters) {
        val module = params[targetModule]

        chat(module)
    }
}

private object FriendsCommand: HubCommand("friend", subcommands = arrayOf(AddSubcommand)) {

    private object AddSubcommand: ExecutableCommandTemplate("add", parent = { FriendsCommand }) {
        private val nameParam = playerNameParameter("name", required())
        private val aliasParam = stringParameter("alias", optional())

        override fun execute(params: CommandParameters) {
            val friend = FriendManager.Friend(params[nameParam], params[aliasParam])

            if (!FriendManager.friends.add(friend)) {
                failCommand("alreadyFriends", variable(friend.name))
            }

            if (friend.alias == null) {
                chatCommand("success", variable(friend.name))
            } else {
                chatCommand( "successAlias", variable(friend.name), variable(friend.alias!!) )
            }
        }

    }
}

abstract class HubCommand(
    name: String,
    aliases: Array<String> = emptyArray(),
    parent: CommandReference? = null,
    val subcommands: Array<CommandTemplate>
): CommandTemplate(name, aliases, parent = parent) {

    final override fun executeRaw(lexedParameters: LexedParameters) {
        // There was no subcommand specified. That's bad.
        if (lexedParameters.size == 0) {
            throw CommandException(
                translation("liquidbounce.commandManager.invalidUsage", this.canonicalName),
                usageInfo = this.usageInfo
            )
        }

        val subcommandName = lexedParameters[0]

        val subcommand = subcommands.find { it.isValidAlias(subcommandName) } ?: throw CommandException(
            translation(
                "liquidbounce.commandManager.unknownCommand",
                subcommandName
            ),
            usageInfo = this.usageInfo
        )

        subcommand.executeRaw(lexedParameters.withOffset(1))
    }
}
