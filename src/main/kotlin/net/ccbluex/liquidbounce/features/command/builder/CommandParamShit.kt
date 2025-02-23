package net.ccbluex.liquidbounce.features.command.builder

import net.ccbluex.liquidbounce.features.command.AutoCompletionProvider
import net.ccbluex.liquidbounce.features.command.ParameterValidationResult
import net.ccbluex.liquidbounce.features.command.ParameterVerifier
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
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
        class OptionalParamTail(
            val optionalParams: List<CommandParameter<ParameterParadigm.OptionalArgument<*>, Any?>>
        ) : ParamTail()

        class VarargParamTail(
            val varargParam: CommandParameter<ParameterParadigm.VarargArgument<*>, Any?>
        ) : ParamTail()
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
    fun parse(input: List<String>): ParameterValidationResult<T> {
        val contextText = "failed to parse parameter ${this.name}"

        val parsedInputs = input.map {
            when (val result = verifier.verifyAndParse(it)) {
                is ParameterValidationResult.Ok -> result.mappedResult
                is ParameterValidationResult.Error -> return result.into<T>().withContext(contextText)
            }
        }

        return paradigm.accept(parsedInputs).withContext(contextText)
    }
}

sealed class ParameterParadigm<in I, out T> {
    abstract fun accept(input: List<I>): ParameterValidationResult<T>

    class RequiredArgument<T : Any> : ParameterParadigm<T, T>() {
        override fun accept(input: List<T>): ParameterValidationResult<T> {
            val first = input.firstOrNull()

            return if (first != null) {
                ParameterValidationResult.Ok(first)
            } else {
                ParameterValidationResult.error("parameter is required")
            }
        }
    }

    class OptionalArgument<T : Any> : ParameterParadigm<T, T?>() {
        override fun accept(input: List<T>): ParameterValidationResult<T?> {
            return ParameterValidationResult.ok(input.firstOrNull())
        }
    }

    class VarargArgument<T : Any> : ParameterParadigm<T, List<T>>() {
        override fun accept(input: List<T>): ParameterValidationResult<List<T>> {
            return ParameterValidationResult.ok(input)
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
            null -> CommandParameterTemplate.ParamTail.OptionalParamTail(emptyList())
            is ParameterParadigm.VarargArgument<*> -> {
                val casted = first as CommandParameter<ParameterParadigm.VarargArgument<*>, Any?>

                CommandParameterTemplate.ParamTail.VarargParamTail(casted)
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

                CommandParameterTemplate.ParamTail.OptionalParamTail(converted)
            }
            is ParameterParadigm.RequiredArgument<*> -> error("Impossible to reach")
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
            ParameterValidationResult.Ok(txt)
        }
    }

    protected fun <T, P : ParameterParadigm<String, T>> playerNameParameter(
        name: String,
        paradigm: ParameterParadigm<String, T>,
    ): CommandParameter<String, T> {
        return parameter(name, paradigm, TODO()) { txt ->
            ParameterValidationResult.Ok(txt)
        }
    }

    protected fun <T, P : ParameterParadigm<Int, T>> intParameter(
        name: String,
        paradigm: ParameterParadigm<Int, T>,
        autocompleteWith: AutoCompletionProvider? = null,
    ): CommandParameter<Int, T> {
        return parameter(name, paradigm, autocompleteWith) { txt ->
            try {
                ParameterValidationResult.ok(txt.toInt())
            } catch (e: NumberFormatException) {
                ParameterValidationResult.error("'$txt' is not a valid integer")
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
                    ParameterValidationResult.ok(integer)
                } else {
                    ParameterValidationResult.error("The integer '$txt' must be positive")
                }
            } catch (e: NumberFormatException) {
                ParameterValidationResult.error("'$txt' is not a valid integer")
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

            if (mod == null) {
                ParameterValidationResult.error("Module '$txt' not found")
            } else {
                ParameterValidationResult.ok(mod)
            }
        }
    }

    protected fun <T, P : ParameterParadigm<InputUtil.Key, T>> inputKeyParameter(
        name: String,
        paradigm: ParameterParadigm<InputUtil.Key, T>,
        autocompleteWith: AutoCompletionProvider? = null,
    ): CommandParameter<InputUtil.Key, T> {
        return parameter(name, paradigm, autocompleteWith) { txt ->
            val inputKey = inputByNameOrNull(txt, acceptNoneKey = false)

            if (inputKey != null) {
                ParameterValidationResult.ok(inputKey)
            } else {
                ParameterValidationResult.error("Input key '$txt' was not found")
            }
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

                ParameterValidationResult.ok(Optional.ofNullable(processedKey))
            } else {
                ParameterValidationResult.error("Input key '$txt' was not found")
            }
        }
    }
}
