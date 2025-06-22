package noobroutes.utils.json

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.utils.MutableVec3

object JsonUtils {
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
    fun JsonObject.addProperty(property: String, mutableVec3: MutableVec3) {
        this.add(property, JsonObject().apply {
            addProperty("x", mutableVec3.x)
            addProperty("y", mutableVec3.y)
            addProperty("z", mutableVec3.z)
        })
    }

    val JsonElement.asMutableVec3: MutableVec3
        get() {
            val obj = this.asJsonObject
            return MutableVec3(obj.get("x")?.asDouble ?: 0.0, obj.get("y")?.asDouble ?: 0.0, obj.get("z")?.asDouble ?: 0.0)
        }

    val JsonElement.asVec3: Vec3
        get() {
            val obj = this.asJsonObject
            return Vec3(obj.get("x")?.asDouble ?: 0.0, obj.get("y")?.asDouble ?: 0.0, obj.get("z")?.asDouble ?: 0.0)
        }

    val JsonElement.asBlockPos: BlockPos
        get() {
            val obj = this.asJsonObject
            return BlockPos(obj.get("x")?.asInt ?: 0, obj.get("y")?.asInt ?: 0, obj.get("z")?.asInt ?: 0)
        }




}