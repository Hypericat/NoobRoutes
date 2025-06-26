package noobroutes.utils.json

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.utils.MutableVec3

object JsonUtils {

    fun JsonArray.add(blockPos: BlockPos) {
        this.add(JsonPrimitive("${blockPos.x}, ${blockPos.y}, ${blockPos.z}"))
    }
    fun JsonArray.add(vec3: Vec3) {
        this.add(JsonPrimitive("${vec3.xCoord}, ${vec3.yCoord}, ${vec3.zCoord}"))
    }
    fun JsonArray.add(mutableVec3: MutableVec3) {
        this.add(JsonPrimitive("${mutableVec3.x}, ${mutableVec3.y}, ${mutableVec3.z}"))
    }


    fun JsonObject.addProperty(property: String, blockPos: BlockPos) {
        this.addProperty(property, "${blockPos.x}, ${blockPos.y}, ${blockPos.z}")
    }
    fun JsonObject.addProperty(property: String, vec3: Vec3) {
        this.addProperty(property, "${vec3.xCoord}, ${vec3.yCoord}, ${vec3.zCoord}")
    }
    fun JsonObject.addProperty(property: String, mutableVec3: MutableVec3) {
        this.addProperty(property, "${mutableVec3.x}, ${mutableVec3.y}, ${mutableVec3.z}")
    }

    val JsonElement.asMutableVec3: MutableVec3
        get() {
            if (this.isJsonPrimitive) {
                val positionArray = this.asString.split(", ")
                return MutableVec3(positionArray[0].toDouble(), positionArray[1].toDouble(), positionArray[2].toDouble())
            } else {
                val obj = this.asJsonObject
                return MutableVec3(obj.get("x")?.asDouble ?: 0.0, obj.get("y")?.asDouble ?: 0.0, obj.get("z")?.asDouble ?: 0.0)
            }
        }

    val JsonElement.asVec3: Vec3
        get() {
            if (this.isJsonPrimitive) {
                val positionArray = this.asString.split(", ")
                return Vec3(positionArray[0].toDouble(), positionArray[1].toDouble(), positionArray[2].toDouble())
            } else {
                val obj = this.asJsonObject
                return Vec3(obj.get("x")?.asDouble ?: 0.0, obj.get("y")?.asDouble ?: 0.0, obj.get("z")?.asDouble ?: 0.0)
            }
        }

    val JsonElement.asBlockPos: BlockPos
        get() {
            if (this.isJsonPrimitive) {
                val positionArray = this.asString.split(", ")
                return BlockPos(positionArray[0].toInt(), positionArray[1].toInt(), positionArray[2].toInt())
            } else {
                val obj = this.asJsonObject
                return BlockPos(obj.get("x")?.asDouble ?: 0.0, obj.get("y")?.asDouble ?: 0.0, obj.get("z")?.asDouble ?: 0.0)
            }
        }




}