package noobroutes.features.floor7.autop3

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3.fuckingLook
import noobroutes.features.floor7.autop3.AutoP3.silentLook
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.SyncData
import noobroutes.utils.json.syncdata.SyncBlockPos
import noobroutes.utils.json.syncdata.SyncBoolean
import noobroutes.utils.json.syncdata.SyncDouble
import noobroutes.utils.json.syncdata.SyncFloat
import noobroutes.utils.json.syncdata.SyncInt
import noobroutes.utils.json.syncdata.SyncLong
import noobroutes.utils.json.syncdata.SyncString
import noobroutes.utils.json.syncdata.SyncVec3

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RingType(val name: String, val aliases: Array<String> = [])

abstract class Ring(
    var coords: Vec3 = Vec3(mc.thePlayer?.posX ?: 0.0, mc.thePlayer?.posY ?: 0.0, mc.thePlayer?.posZ ?: 0.0),
    var yaw: Float,
    var term: Boolean,
    var leap: Boolean,
    var left: Boolean,
    var center: Boolean,
    var rotate: Boolean
) {
    var triggered = false
    val type = javaClass.getAnnotation(RingType::class.java)?.name ?: "Unknown"
    val internalRingData = mutableListOf<SyncData>()

    fun getAsJsonObject(): JsonObject{
        val obj = JsonObject().apply {
            addProperty("type", type)
            addProperty("coords", coords)
            addProperty("yaw", yaw)
            if (term) addProperty("term", true)
            if (leap) addProperty("leap", true)
            if (left) addProperty("left", true)
            if (center) addProperty("center", true)
            if (rotate) addProperty("rotate", true)
        }
        addPrimitiveRingData(obj)
        addRingData(obj)
        return obj
    }

    internal open fun addPrimitiveRingData(obj: JsonObject){
        internalRingData.forEach { it.writeTo(obj) }
    }


    open fun loadRingData(obj: JsonObject){
        internalRingData.forEach { it.readFrom(obj) }
    }
    protected fun addBoolean(name: String, getter: () -> Boolean, setter: (Boolean) -> Unit){
        internalRingData.add(SyncBoolean(name, getter, setter))
    }

    protected fun addBlockPos(name: String, getter: () -> BlockPos, setter: (BlockPos) -> Unit) {
        internalRingData.add(SyncBlockPos(name, getter, setter))
    }

    protected fun addVec3(name: String, getter: () -> Vec3, setter: (Vec3) -> Unit) {
        internalRingData.add(SyncVec3(name, getter, setter))
    }
    protected fun addString(name: String, getter: () -> String, setter: (String) -> Unit) {
        internalRingData.add(SyncString(name, getter, setter))
    }
    protected fun addDouble(name: String, getter: () -> Double, setter: (Double) -> Unit) {
        internalRingData.add(SyncDouble(name, getter, setter))
    }
    protected fun addFloat(name: String, getter: () -> Float, setter: (Float) -> Unit) {
        internalRingData.add(SyncFloat(name, getter, setter))
    }
    protected fun addInt(name: String, getter: () -> Int, setter: (Int) -> Unit) {
        internalRingData.add(SyncInt(name, getter, setter))
    }
    protected fun addLong(name: String, getter: () -> Long, setter: (Long) -> Unit) {
        internalRingData.add(SyncLong(name, getter, setter))
    }


    private fun loadInternalRingData(obj: JsonObject){
        internalRingData.forEach { it.readFrom(obj) }
    }


    open fun addRingData(obj: JsonObject) {}

    open fun doRing() {
    }

    fun doRingArgs() {
        if (center && !mc.thePlayer.onGround) return
        if (rotate) {
            if (!silentLook) mc.thePlayer.rotationYaw = yaw
            Blink.rotate = yaw
        }
        if (fuckingLook) {
            mc.thePlayer.rotationYaw = yaw
        }
        if (center) {
            mc.thePlayer.setPosition(coords.xCoord, mc.thePlayer.posY, coords.zCoord)
            Blink.rotSkip = true
        }
    }

    fun run() {
        if (center && !mc.thePlayer.onGround) {
            triggered = false
            return
        }
        doRing()
    }
}