package net.ccbluex.liquidbounce.integration.theme.component

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.utils.render.Alignment

/**
 * Registry for native component factories loaded from themes component JSON.
 */
object NativeComponentRegistry {

    private val factories: MutableMap<String, (String, Boolean, Alignment, Array<ComponentTweak>, Array<JsonObject>) -> Component> = mutableMapOf()

    fun register(name: String, create: (String, Boolean, Alignment, Array<ComponentTweak>, Array<JsonObject>) -> Component) {
        factories[name] = create
    }

    fun create(name: String, enabled: Boolean, alignment: Alignment, tweaks: Array<ComponentTweak>, values: Array<JsonObject>) : Component? {
        val factory = factories[name] ?: return null
        return factory(name, enabled, alignment, tweaks, values)
    }

}
