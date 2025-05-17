package noobroutes.utils.json.syncdata

import com.google.gson.JsonObject
import noobroutes.utils.json.SyncData

class SyncLong(key: String, val getter: () -> Long, val setter: (Long) -> Unit) : SyncData(key) {
    override fun readFrom(obj: JsonObject) {
        if (obj.has(key)) setter(obj.get(key).asLong)
    }

    override fun writeTo(obj: JsonObject) {
        obj.addProperty(key, getter())
    }
}