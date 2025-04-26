package com.github.wadey3636.noobroutes.utils.adapters

import com.github.wadey3636.noobroutes.features.floor7.Ring
import com.github.wadey3636.noobroutes.features.floor7.RingTypes
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.github.wadey3636.noobroutes.utils.LookVec
import net.minecraft.util.Vec3


class RingsMapTypeAdapter : TypeAdapter<MutableMap<String, MutableList<Ring>>>() {
    override fun write(out: JsonWriter, rings: MutableMap<String, MutableList<Ring>>?) {
        if (rings == null) {
            out.nullValue()
            return
        }

        out.beginObject()
        for ((routeName, ringList) in rings) {
            out.name(routeName)
            out.beginArray()
            for (ring in ringList) {
                out.beginObject()

                // Write ring type
                out.name("type")
                out.value(ring.type.name)

                // Write coordinates
                out.name("coords")
                out.beginObject()
                out.name("x").value(ring.coords.xCoord)
                out.name("y").value(ring.coords.yCoord)
                out.name("z").value(ring.coords.zCoord)
                out.endObject()

                // Write direction
                out.name("direction")
                out.beginObject()
                out.name("yaw").value(ring.direction.yaw.toDouble())
                out.name("pitch").value(ring.direction.pitch.toDouble())
                out.endObject()

                // Write boolean flags
                out.name("walk").value(ring.walk)
                out.name("look").value(ring.look)
                out.name("center").value(ring.center)

                out.endObject()
            }
            out.endArray()
        }
        out.endObject()
    }

    override fun read(reader: JsonReader): MutableMap<String, MutableList<Ring>>? {
        val rings = mutableMapOf<String, MutableList<Ring>>()

        reader.beginObject()
        while (reader.hasNext()) {
            val routeName = reader.nextName()
            val ringList = mutableListOf<Ring>()

            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()

                var type = RingTypes.WALK
                var x = 0.0
                var y = 0.0
                var z = 0.0
                var yaw = 0f
                var pitch = 0f
                var walk = false
                var look = false
                var center = false

                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "type" -> type = RingTypes.valueOf(reader.nextString())
                        "coords" -> {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "x" -> x = reader.nextDouble()
                                    "y" -> y = reader.nextDouble()
                                    "z" -> z = reader.nextDouble()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                        }
                        "direction" -> {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "yaw" -> yaw = reader.nextDouble().toFloat()
                                    "pitch" -> pitch = reader.nextDouble().toFloat()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                        }
                        "walk" -> walk = reader.nextBoolean()
                        "look" -> look = reader.nextBoolean()
                        "center" -> center = reader.nextBoolean()
                        else -> reader.skipValue()
                    }
                }

                ringList.add(
                    Ring(
                    type = type,
                    coords = Vec3(x, y, z),
                    direction = LookVec(yaw, pitch),
                    walk = walk,
                    look = look,
                    center = center
                )
                )

                reader.endObject()
            }
            reader.endArray()

            rings[routeName] = ringList
        }
        reader.endObject()

        return rings
    }
}