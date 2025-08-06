package noobroutes.features.settings.impl

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive


/**
 * Setting that lets you pick between an array of strings.
 *
 * I changed it to return strings, even though it was less efficient, it just makes the code more readable/
 * @author Aton, Stivais
 */
class SelectorSetting(
    name: String,
    defaultSelected: String,
    var options: ArrayList<String>,
    hidden: Boolean = false,
    description: String,
) : noobroutes.features.settings.Setting<String>(name, hidden, description),
    noobroutes.features.settings.Saving {

    override val default: String = defaultSelected

    override var value: String
        get() = options[index]
        set(value) {
            index = optionIndex(value)
        }

    var index: Int = optionIndex(defaultSelected)
        set(value) {
            field = if (value > options.size - 1) 0 else if (value < 0) options.size - 1 else value
        }

    var selected: String
        get() = options[index]
        set(value) {
            index = optionIndex(value)
        }

    override fun write(): JsonElement {
        return JsonPrimitive(selected)
    }

    override fun read(element: JsonElement?) {
        element?.asString?.let {
            selected = it
        }
    }

    private fun optionIndex(string: String): Int {
        return options.map { it.lowercase() }.indexOf(string.lowercase()).coerceIn(0, options.size - 1)
    }
}