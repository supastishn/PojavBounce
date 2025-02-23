package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.features.command.*
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.input.inputByNameOrNull
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import java.util.Optional
import kotlin.jvm.Throws
import kotlin.jvm.optionals.getOrNull

typealias PositiveInt = Int

typealias CommandReference = () -> CommandTemplate


abstract class CommandTemplate(
    val name: String,
    val aliases: Array<String> = emptyArray(),
    val parent: () -> CommandTemplate? = { null },
    val requireIngame: Boolean = false
): ParameterContainer() {
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

    @Throws(CommandException::class)
    abstract fun execute(params: CommandParameters)
}

private class BindCommand: CommandTemplate("bind") {
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

private object FriendsCommand: HubCommand("friend", subcommands = arrayOf(AddSubcommand)) {

    private object AddSubcommand: CommandTemplate("add", parent = { FriendsCommand }) {
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
    parent: CommandTemplate? = null,
    val subcommands: Array<CommandTemplate>
): CommandTemplate(name, aliases) {
    final override fun execute(params: CommandParameters) {
        TODO("Not yet implemented")
    }
}
