package noobroutes.utils.json.syncdata

import com.google.gson.JsonObject
import noobroutes.utils.json.SyncData

class SyncInt(key: String, val getter: () -> Int, val setter: (Int) -> Unit) : SyncData(key) {
    override fun readFrom(obj: JsonObject) {
        if (obj.has(key)) setter(obj.get(key).asInt)
    }

    override fun writeTo(obj: JsonObject) {
        obj.addProperty(key, getter())
    }
}