package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.features.command.AutoCompletionProvider
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.ParameterVerifier
import net.ccbluex.liquidbounce.features.command.builder.CommandParameterTemplate.ParamTail.*
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.input.inputByNameOrNull
import net.ccbluex.liquidbounce.utils.kotlin.subList
import net.minecraft.client.util.InputUtil
import java.util.*
import kotlin.collections.ArrayList

class CommandParameterTemplate(
    val requiredParams: List<CommandParameter<ParameterParadigm.RequiredArgument<*>, Any?>>,
    val optionalTail: ParamTail
) {
    sealed class ParamTail {
        class OptionalParamsTail(
            val optionalParams: List<CommandParameter<ParameterParadigm.OptionalArgument<*>, Any?>>
        ) : ParamTail()

        class VarargParamTail(
            val varargParam: CommandParameter<ParameterParadigm.VarargArgument<*>, Any?>
        ) : ParamTail()

        class TextParamTail(
            val textParam: CommandParameter<ParameterParadigm.TextArgument<*>, Any?>
        ) : ParamTail()
    }

    fun parseParams(params: LexedParameters): CommandParameters {
        val required = this.requiredParams.mapIndexed { idx, declaredParam ->
            declaredParam.parse(params.withOffset(idx))
        }

        val optionalTail = when (this.optionalTail) {
            is OptionalParamsTail -> {
                this.optionalTail.optionalParams.mapIndexed { idx, declaredParam ->
                    declaredParam.parse(params.withOffset(this.requiredParams.size + idx))
                }
            }
            is VarargParamTail -> this.optionalTail.varargParam.parse(params.withOffset(this.requiredParams.size))
            is TextParamTail -> this.optionalTail.textParam.parse(params.withOffset(this.requiredParams.size))
        }

        return CommandParameters(required + optionalTail)
    }
}

class CommandParameters(private val params: List<Any?>) {
    operator fun <T> get(parameter: CommandParameter<*, T>): T {
        return params[parameter.paramIndex] as T
    }
}

class CommandParameter<I, out T>(
    val name: String,
    val paramIndex: Int,
    private val verifier: ParameterVerifier<I>,
    private val autocompleteWith: AutoCompletionProvider?,
    val paradigm: ParameterParadigm<I, T>
) {
    fun parse(input: LexedParameters): T {
        val contextText = "failed to parse parameter ${this.name}"

        try {
            return paradigm.accept(this.verifier, input)
        } catch (e: CommandException) {
            throw CommandException(contextText.asText(), e)
        }
    }
}

sealed class ParameterParadigm<in I, out T> {
    abstract fun accept(verifier: ParameterVerifier<I>, input: LexedParameters): T

    class RequiredArgument<T : Any> : ParameterParadigm<T, T>() {
        override fun accept(verifier: ParameterVerifier<T>, input: LexedParameters): T {
            val first = input.asTokens().firstOrNull()

            return if (first != null) {
                verifier.verifyAndParse(first)
            } else {
                throw CommandException("parameter is required".asText())
            }
        }
    }

    class OptionalArgument<T : Any> : ParameterParadigm<T, T?>() {
        override fun accept(verifier: ParameterVerifier<T>, input: LexedParameters): T? {
            val param =  input.asTokens().firstOrNull()

            return if (param != null) {
                verifier.verifyAndParse(param)
            } else {
                null
            }
        }
    }

    class VarargArgument<T : Any> : ParameterParadigm<T, List<T>>() {
        override fun accept(verifier: ParameterVerifier<T>, input: LexedParameters): List<T> {
            return input.asTokens().map { verifier.verifyAndParse(it) }
        }
    }

    class TextArgument<T: Any> : ParameterParadigm<T, T>() {
        override fun accept(
            verifier: ParameterVerifier<T>,
            input: LexedParameters
        ): T {
            return verifier.verifyAndParse(input.text)
        }

    }
}

abstract class ParameterContainer {
    private val parameters = ArrayList<CommandParameter<*, Any?>>()

    private val innerTemplate = lazy { intoTemplate() }

    val template by innerTemplate

    protected fun <T : Any> required(): ParameterParadigm.RequiredArgument<T> =
        ParameterParadigm.RequiredArgument()

    protected fun <T : Any> optional(): ParameterParadigm.OptionalArgument<T> =
        ParameterParadigm.OptionalArgument()

    protected fun <T : Any> vararg(): ParameterParadigm.VarargArgument<T> =
        ParameterParadigm.VarargArgument()

    fun intoTemplate(): CommandParameterTemplate {
        val requiredParams = this.parameters
            .takeWhile { it.paradigm is ParameterParadigm.RequiredArgument<*> }
            .map { it as CommandParameter<ParameterParadigm.RequiredArgument<*>, Any?> }

        val remainingArgs = this.parameters.subList(requiredParams.size)
        val first = remainingArgs.firstOrNull()

        // Check the first argument after the required arguments.
        val tail = when (first?.paradigm) {
            null -> OptionalParamsTail(emptyList())
            is ParameterParadigm.VarargArgument<*> -> {
                val casted = first as CommandParameter<ParameterParadigm.VarargArgument<*>, Any?>

                VarargParamTail(casted)
            }

            is ParameterParadigm.OptionalArgument<*> -> {
                val converted = remainingArgs.map {
                    if (it.paradigm !is ParameterParadigm.OptionalArgument<*>) {
                        error(
                            "There may not be other argument types than" +
                                "optional types after an optional type as specified."
                        )
                    }

                    it as CommandParameter<ParameterParadigm.OptionalArgument<*>, Any?>
                }

                OptionalParamsTail(converted)
            }
            is ParameterParadigm.RequiredArgument<*> -> error("Impossible to reach")
            is ParameterParadigm.TextArgument<*> -> {
                val casted = first as CommandParameter<ParameterParadigm.TextArgument<*>, Any?>

                TextParamTail(casted)
            }
        }

        return CommandParameterTemplate(requiredParams, tail)
    }

    protected fun <I, T, P : ParameterParadigm<I, T>> parameter(
        name: String,
        paradigm: ParameterParadigm<I, T>,
        autocompleteWith: AutoCompletionProvider? = null,
        verifier: ParameterVerifier<I>
    ): CommandParameter<I, T> {
        check(!this.innerTemplate.isInitialized()) { "Parameter was added after the parameter template was generated." }

        val parameter = CommandParameter(name, this.parameters.size, verifier, autocompleteWith, paradigm)

        this.parameters.add(parameter)

        return parameter
    }

    protected fun <T, P : ParameterParadigm<String, T>> stringParameter(
        name: String,
        paradigm: ParameterParadigm<String, T>,
        autocompleteWith: AutoCompletionProvider? = null,
    ): CommandParameter<String, T> {
        return parameter(name, paradigm, autocompleteWith) { txt ->
            txt
        }
    }

    protected fun <T, P : ParameterParadigm<String, T>> playerNameParameter(
        name: String,
        paradigm: ParameterParadigm<String, T>,
    ): CommandParameter<String, T> {
        return parameter(name, paradigm, TODO()) { txt ->
            txt
        }
    }

    protected fun <T, P : ParameterParadigm<Int, T>> intParameter(
        name: String,
        paradigm: ParameterParadigm<Int, T>,
        autocompleteWith: AutoCompletionProvider? = null,
    ): CommandParameter<Int, T> {
        return parameter(name, paradigm, autocompleteWith) { txt ->
            try {
                txt.toInt()
            } catch (ignored: NumberFormatException) {
                throw CommandException("'$txt' is not a valid integer".asText())
            }
        }
    }

    protected fun <T, P : ParameterParadigm<PositiveInt, T>> positiveIntParameter(
        name: String,
        paradigm: ParameterParadigm<PositiveInt, T>,
        autocompleteWith: AutoCompletionProvider? = null,
    ): CommandParameter<PositiveInt, T> {
        return parameter(name, paradigm, autocompleteWith) { txt ->
            try {
                val integer = txt.toInt()

                if (integer >= 0) {
                    integer
                } else {
                    throw CommandException("The integer '$txt' must be positive".asText())
                }
            } catch (ignored: NumberFormatException) {
                throw CommandException("'$txt' is not a valid integer".asText())
            }
        }
    }

    protected fun <T, P : ParameterParadigm<ClientModule, T>> moduleParameter(
        name: String,
        paradigm: ParameterParadigm<ClientModule, T>,
        autocompleteWith: AutoCompletionProvider? = null,
    ): CommandParameter<ClientModule, T> {
        return parameter(name, paradigm, autocompleteWith) { txt ->
            val mod = ModuleManager.find { it.name.equals(txt, true) }

            mod ?: throw CommandException("Module '$txt' not found".asText())
        }
    }

    protected fun <T, P : ParameterParadigm<InputUtil.Key, T>> inputKeyParameter(
        name: String,
        paradigm: ParameterParadigm<InputUtil.Key, T>,
        autocompleteWith: AutoCompletionProvider? = null,
    ): CommandParameter<InputUtil.Key, T> {
        return parameter(name, paradigm, autocompleteWith) { txt ->
            val inputKey = inputByNameOrNull(txt, acceptNoneKey = false)

            inputKey ?: throw CommandException("Input key '$txt' was not found".asText())
        }
    }

    protected fun <T, P : ParameterParadigm<Optional<InputUtil.Key>, T>> inputKeyOrNoneParameter(
        name: String,
        paradigm: ParameterParadigm<Optional<InputUtil.Key>, T>,
        autocompleteWith: AutoCompletionProvider? = null,
    ): CommandParameter<Optional<InputUtil.Key>, T> {
        return parameter(name, paradigm, autocompleteWith) { txt ->
            val inputKey = inputByNameOrNull(txt, acceptNoneKey = true)

            if (inputKey != null) {
                val processedKey = inputKey.takeUnless { inputKey == InputUtil.UNKNOWN_KEY }

                Optional.ofNullable(processedKey)
            } else {
                throw CommandException("Input key '$txt' was not found".asText())
            }
        }
    }

    protected fun text(name: String): CommandParameter<String, String> {
        return parameter(name, ParameterParadigm.TextArgument(), null) { txt -> txt }
    }
}
