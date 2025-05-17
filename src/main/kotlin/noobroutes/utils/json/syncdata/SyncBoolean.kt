package noobroutes.utils.json.syncdata


import com.google.gson.JsonObject
import noobroutes.utils.json.SyncData

class SyncBoolean(
    key: String,
    val getter: () -> Boolean,
    val setter: (Boolean) -> Unit
) : SyncData(key) {

    override fun writeTo(obj: JsonObject) {
        if (getter()) obj.addProperty(key, true)
    }

    override fun readFrom(obj: JsonObject) {
        if (obj.has(key)) setter(obj.get(key).asBoolean)
    }


}