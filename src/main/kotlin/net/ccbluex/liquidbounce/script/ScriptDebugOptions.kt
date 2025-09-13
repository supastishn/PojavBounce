package net.ccbluex.liquidbounce.script

<<<<<<< HEAD
=======
import net.ccbluex.liquidbounce.config.types.NamedChoice

>>>>>>> upstream/nextgen
data class ScriptDebugOptions(
    val enabled: Boolean = false,
    val protocol: DebugProtocol = DebugProtocol.INSPECT,
    val suspendOnStart: Boolean = false,
    val inspectInternals: Boolean = false,
    val port: Int = 4242
)

<<<<<<< HEAD
enum class DebugProtocol {
    DAP,
    INSPECT
=======
enum class DebugProtocol(override val choiceName: String) : NamedChoice {
    DAP("DAP"),
    INSPECT("INSPECT"),
>>>>>>> upstream/nextgen
}
