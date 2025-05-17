package noobroutes.utils.json.syncdata

import com.google.gson.JsonObject
import noobroutes.utils.json.SyncData

class SyncDouble(key: String, val getter: () -> Double, val setter: (Double) -> Unit) : SyncData(key) {
    override fun readFrom(obj: JsonObject) {
        if (obj.has(key)) setter(obj.get(key).asDouble)
    }

    override fun writeTo(obj: JsonObject) {
        obj.addProperty(key, getter())
    }
}