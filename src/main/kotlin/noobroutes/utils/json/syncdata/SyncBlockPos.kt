package noobroutes.utils.json.syncdata

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.json.SyncData

class SyncBlockPos(
    key: String,
    val getter: () -> BlockPos,
    val setter: (BlockPos) -> Unit) : SyncData(key) {


    override fun writeTo(obj: JsonObject) {
        obj.addProperty(key, getter())
    }

    override fun readFrom(obj: JsonObject) {
        if (obj.has(key)) setter(obj.get(key).asBlockPos)
    }



}