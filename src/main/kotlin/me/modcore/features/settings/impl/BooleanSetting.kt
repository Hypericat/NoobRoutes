package me.modcore.features.settings.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import me.modcore.features.settings.Saving
import me.modcore.features.settings.Setting

/**
 * A setting that represents a boolean.
 *
 * @author Aton, Stivais
 */
class BooleanSetting (
    name: String,
    override val default: Boolean = false,
    hidden: Boolean = false,
    description: String,
): Setting<Boolean>(name, hidden, description), Saving {

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