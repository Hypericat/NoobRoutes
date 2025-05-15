package noobroutes.utils.adapters

import com.google.gson.*
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.Vec3
import noobroutes.features.floor7.Ring
import noobroutes.features.floor7.RingTypes
import noobroutes.utils.LookVec
import java.lang.reflect.Type

class RingAdapter : JsonSerializer<Ring>, JsonDeserializer<Ring> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Ring {
        val obj = json.asJsonObject
        val type = RingTypes.valueOf(obj.get("type").asString)

        val coordsElem = obj.get("coords")
        val coords: Vec3 = when {
            coordsElem.isJsonObject -> {
                val o = coordsElem.asJsonObject
                val x = o.get("x")?.asDouble
                    ?: o.get("field_72450_a")?.asDouble
                    ?: o.get("field_72450_a_old")?.asDouble  // if you have other variants
                    ?: 0.0
                val y = o.get("y")?.asDouble
                    ?: o.get("field_72448_b")?.asDouble
                    ?: 0.0
                val z = o.get("z")?.asDouble
                    ?: o.get("field_72449_c")?.asDouble
                    ?: 0.0
                Vec3(x, y, z)
            }
            coordsElem.isJsonArray -> {
                val arr = coordsElem.asJsonArray
                Vec3(
                    arr?.get(0)?.asDouble ?: 0.0,
                    arr?.get(1)?.asDouble ?: 0.0,
                    arr?.get(2)?.asDouble ?: 0.0
                 )
            }
            else -> Vec3(0.0, 0.0, 0.0)
        }


        val dirElem = obj.get("direction") ?: obj.get("directions")
        val direction: LookVec = if (dirElem != null && dirElem.isJsonObject) {
            val o = dirElem.asJsonObject
            val yaw   = o.get("yaw")?.asFloat ?: o.get("field_yaw")?.asFloat ?: 0f
            val pitch = o.get("pitch")?.asFloat ?: o.get("field_pitch")?.asFloat ?: 0f
            LookVec(yaw, pitch)
        } else {
            LookVec(0f, 0f)
        }

        val walk = obj.has("walk")
        val look = obj.has("look")
        val center = obj.has("center")
        val misc = obj.get("misc")?.asDouble ?: obj.get("endY")?.asDouble ?: 0.0
        val blinks = mutableListOf<C03PacketPlayer.C04PacketPlayerPosition>()
        if (obj.has("blinkPackets") || obj.has("blink_packets")) {
            val arr = obj.getAsJsonArray(if (obj.has("blinkPackets")) "blinkPackets" else "blink_packets")
            arr.forEach { el ->
                val p = el.asJsonObject
                val x = p.get("x")?.asDouble ?: p.get("field_149479_a")?.asDouble ?: 0.0
                val y = p.get("y")?.asDouble ?: p.get("field_149477_b")?.asDouble ?: 0.0
                val z = p.get("z")?.asDouble ?: p.get("field_149478_c")?.asDouble ?: 0.0
                val g = p.get("onGround")?.asBoolean ?: p.get("field_149474_g")?.asBoolean ?: false
                blinks.add(C03PacketPlayer.C04PacketPlayerPosition(x, y, z, g))
            }
        }
        return Ring(type, coords, direction, walk, look, center, false, blinks, misc)

    }

    override fun serialize(
        src: Ring,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonObject().apply {
            addProperty("type", src.type.name)
            add("coords", JsonArray().apply {
                addProperty("x", src.coords.xCoord)
                addProperty("y", src.coords.yCoord)
                addProperty("z", src.coords.zCoord)
            })
            add("direction", JsonArray().apply {
                addProperty("yaw", src.direction.yaw)
                addProperty("pitch", src.direction.pitch)
            })
            if (src.walk) add("walk", JsonPrimitive(true))
            if (src.look) add("look", JsonPrimitive(true))
            if (src.center) add("center", JsonPrimitive(true))
            if (src.misc != 0.0) add("misc", JsonPrimitive(src.misc))
            if (src.blinkPackets.isNotEmpty()) add("blink_packets", JsonArray().apply {
                for (packet in src.blinkPackets) {
                    add(JsonObject().apply {
                        addProperty("x", packet.positionX)
                        addProperty("y", packet.positionY)
                        addProperty("z", packet.positionZ)
                        addProperty("isOnGround", packet.isOnGround)
                    })
                }
            })
        }
    }
}