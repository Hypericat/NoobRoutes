package noobroutes.utils.json.syncdata

import com.google.gson.JsonObject
import noobroutes.utils.json.SyncData

class SyncString(key: String, val getter: () -> String, val setter: (String) -> Unit) : SyncData(key) {
    override fun readFrom(obj: JsonObject) {
        if (obj.has(key)) setter(obj.get(key).asString)
    }

    override fun writeTo(obj: JsonObject) {
        obj.addProperty(key, getter())
    }
}