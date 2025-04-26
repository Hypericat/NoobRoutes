package noobroutes.features.settings.impl

import com.google.gson.JsonElement

/**
 * This setting is only designed to store values as a list, and shouldn't be rendered in the gui.
 *
 * @author Stivais
 */
class ListSetting<E, T : MutableCollection<E>>(
    name: String,
    override val default: T,
    description: String = "",
) : noobroutes.features.settings.Setting<T>(name, true, description),
    noobroutes.features.settings.Saving {

    override var value: T = default

    override fun write(): JsonElement {
        return gson.toJsonTree(value)
    }

    override fun read(element: JsonElement?) {
        element?.asJsonArray?.let {
            val temp = gson.fromJson<T>(it, object : com.google.gson.reflect.TypeToken<T>() {}.type)
            value.clear()
            value.addAll(temp)
        }
    }
}