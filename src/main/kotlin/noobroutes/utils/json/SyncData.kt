package noobroutes.utils.json

import com.google.gson.JsonObject

abstract class SyncData(val key: String) {
    abstract fun readFrom(obj: JsonObject)
    abstract fun writeTo(obj: JsonObject)
}