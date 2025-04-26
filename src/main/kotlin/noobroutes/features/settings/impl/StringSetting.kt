package com.github.wadey3636.noobroutes.features.settings.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive


/**
 * Setting that lets you type a string.
 * @author Aton, Stivais
 */
class StringSetting(
    name: String,
    override val default: String = "",
    var length: Int = 20,
    hidden: Boolean = false,
    description: String,
) : com.github.wadey3636.noobroutes.features.settings.Setting<String>(name, hidden, description),
    com.github.wadey3636.noobroutes.features.settings.Saving {

    override var value: String = default
        set(value) {
            field = if (value.length <= length) value else return
        }

    var text: String by this::value

    override fun write(): JsonElement {
        return JsonPrimitive(value)
    }

    override fun read(element: JsonElement?) {
        element?.asString?.let {
            value = it
        }
    }
}