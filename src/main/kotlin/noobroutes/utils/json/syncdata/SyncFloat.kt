package noobroutes.utils.json.syncdata

import com.google.gson.JsonObject
import noobroutes.utils.json.SyncData

class SyncFloat(key: String, val getter: () -> Float, val setter: (Float) -> Unit) : SyncData(key) {

    override fun readFrom(obj: JsonObject) {
        if (obj.has(key)) setter(obj.get(key).asFloat)
    }

    override fun writeTo(obj: JsonObject) {
        obj.addProperty(key, getter())

    }


}