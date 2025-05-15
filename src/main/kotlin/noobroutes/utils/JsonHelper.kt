package noobroutes.utils

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3

object JsonHelper {
    fun JsonObject.addProperty(property: String, blockPos: BlockPos) {
        this.add(property, JsonObject().apply {
            addProperty("x", blockPos.x)
            addProperty("y", blockPos.y)
            addProperty("z", blockPos.z)
        })
    }
    fun JsonObject.addProperty(property: String, vec3: Vec3) {
        this.add(property, JsonObject().apply {
            addProperty("x", vec3.xCoord)
            addProperty("y", vec3.yCoord)
            addProperty("z", vec3.zCoord)
        })
    }


}