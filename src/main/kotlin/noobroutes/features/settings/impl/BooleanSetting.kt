package noobroutes.features.settings.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

/**
 * A setting that represents a boolean.
 *
 * @author Aton, Stivais
 */
class BooleanSetting (
    name: String,
    override val default: Boolean = false,
    hidden: Boolean = false,
    description: String = "",
): noobroutes.features.settings.Setting<Boolean>(name, hidden, description),
    noobroutes.features.settings.Saving {

    override var value: Boolean = default

    var enabled: Boolean by this::value

    override fun write(): JsonElement {
        return JsonPrimitive(enabled)
    }

    override fun read(element: JsonElement?) {
        if (element?.asBoolean != enabled) {
            enabled = !enabled
        }
    }
}