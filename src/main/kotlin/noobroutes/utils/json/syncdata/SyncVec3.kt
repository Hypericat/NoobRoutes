package noobroutes.utils.json.syncdata

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.json.SyncData

class SyncVec3(key: String, val getter: () -> Vec3, val setter: (Vec3) -> Unit) : SyncData(key) {

    override fun readFrom(obj: JsonObject) {
        if (obj.has(key)) setter(obj.get(key).asVec3)
    }

    override fun writeTo(obj: JsonObject) {
        obj.addProperty(key, getter())

    }


}