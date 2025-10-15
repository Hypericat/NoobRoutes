package noobroutes.features.settings.impl

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import noobroutes.features.settings.Saving
import noobroutes.features.settings.Setting

class DynamicListSetting(
    name: String,
    override val default: String? = null,
    hidden: Boolean = false,
    description: String,
) : Setting<String?>(name, hidden, description), Saving {

    override var value: String? = default
    val options = mutableListOf<String>()

    var text: String? by this::value

    override fun write(): JsonElement {
        val obj = JsonObject()
        val jsonArray = JsonArray()
        for (option in options) {
            jsonArray.add(JsonPrimitive(option))
        }
        obj.addProperty("value", value)
        obj.add("options", jsonArray)
        return obj
    }

    override fun read(element: JsonElement?) {
        if (element == null) return
        val obj = element.asJsonObject ?: return
        val optionArray = obj.get("options")?.asJsonArray ?: JsonArray()
        for (option in optionArray) {
            val string = option.asString ?: continue
            options.add(string)
        }
        value = obj.get("value")?.asString
    }
}