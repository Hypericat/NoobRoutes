package noobroutes.features.floor7.autop3

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.features.floor7.autop3.rings.BlinkWaypoint
import noobroutes.utils.*
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.SyncData
import noobroutes.utils.json.syncdata.*
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage


data class RingBase(
    var coords: Vec3 = Vec3(mc.thePlayer?.posX ?: 0.0, mc.thePlayer?.posY ?: 0.0, mc.thePlayer?.posZ ?: 0.0),
    var yaw: Float,
    var term: Boolean,
    var leap: Boolean,
    var left: Boolean,
    var center: Boolean,
    var rotate: Boolean,
    var diameter: Float,
    var height: Float) {
    constructor() : this(Vec3(0.0, 0.0, 0.0), 0f, false, false, false, false, false, 1f, 1f)

    companion object {
        val diameterRegex = Regex("""d:(\d+)""")
        val heightRegex = Regex("""h:(\d+)""")
    }
}

abstract class Ring(
    val base: RingBase,
    val type: RingType
) {
    inline val ringName get() = type.ringName

    inline var coords: Vec3
        get() = base.coords
        set(value) {base.coords = value}
    inline var yaw: Float
        get() = base.yaw
        set(value) {base.yaw = value}
    inline var term: Boolean
        get() = base.term
        set(value) {
            if (value) {
                base.left = false
                base.leap = false
            }
            base.term = value
        }
    inline var leap: Boolean
        get() = base.leap
        set(value) {
            if (value) {
                base.term = false
                base.left = false
            }
            base.leap = value
        }
    inline var left: Boolean
        get() = base.left
        set(value) {
            if (value) {
                base.term = false
                base.leap = false
            }
            base.left = value
        }
    inline var center: Boolean
        get() = base.center
        set(value) {base.center = value}
    inline var rotate: Boolean
        get() = base.rotate
        set(value) {base.rotate = value}
    inline var diameter: Float
        get() = base.diameter
        set(value) {base.diameter = value}
    inline var height: Float
        get() = base.height
        set(value) {base.height = value}

    inline val isAwait: Boolean
        get() = (term || leap || left)


    var renderYawVector = false
    var triggered = false
    val internalRingData = mutableListOf<SyncData>()

    fun getAsJsonObject(): JsonObject{
        val obj = JsonObject().apply {
            addProperty("type", type.ringName)
            addProperty("coords", coords)
            addProperty("yaw", yaw)
            if (term) addProperty("term", true)
            if (leap) addProperty("leap", true)
            if (left) addProperty("left", true)
            if (center) addProperty("center", true)
            if (rotate) addProperty("rotate", true)
            if (diameter != 1f) addProperty("diameter", diameter)
            if (height != 1f) addProperty("height", height)
        }
        addPrimitiveRingData(obj)
        addRingData(obj)
        return obj
    }

    protected open fun addPrimitiveRingData(obj: JsonObject){
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


    fun renderRing(color: Color) {
        val offsetCoords = this.coords.add(0.0, 0.03, 0.0)
        RenderUtils.drawOutlinedAABB(offsetCoords.subtract(diameter * 0.5, 0.0, diameter * 0.5).toAABB(diameter, height, diameter), color, thickness = 3, depth = true)
        if (this.renderYawVector) Renderer.draw3DLine(
            listOf(
                this.coords.add(0.0, PlayerUtils.STAND_EYE_HEIGHT, 0.0),
                Vec3(this.yaw.xPart, 0.0, this.yaw.zPart).multiply(1.8).add(
                    this.coords.xCoord,
                    this.coords.yCoord + PlayerUtils.STAND_EYE_HEIGHT,
                    this.coords.zCoord
                )
            ),
            color
        )
    }

    open fun addRingData(obj: JsonObject) {}

    open fun doRing() {
        AutoP3MovementHandler.resetShit()
    }

    open fun runTriggeredLogic() {
        triggered = false
    }

    fun inRing(): Boolean {
        if (center || this is BlinkRing || this is BlinkWaypoint) return checkInBoundsWithSpecifiedHeight(0f) && mc.thePlayer.onGround
        return checkInBoundsWithSpecifiedHeight(height)
    }

    protected fun checkInBoundsWithSpecifiedHeight(heightToUse: Float): Boolean{
        return mc.thePlayer.positionVector.isVecInBounds(coords.xCoord - diameter * 0.5, coords.yCoord, coords.zCoord - diameter * 0.5, coords.xCoord + diameter * 0.5, coords.yCoord + heightToUse, coords.zCoord + diameter * 0.5)
    }

    protected fun center() {
        PlayerUtils.stopVelocity()
        mc.thePlayer.isSprinting = false
        PlayerUtils.unPressKeys()
        AutoP3MovementHandler.resetShit()

        Scheduler.schedulePostMoveEntityWithHeadingTask {
            PlayerUtils.setPosition(coords.xCoord, coords.zCoord)
            if (isAwait) await() else maybeDoRing()
        }
    }

    protected fun await(){
        PlayerUtils.stopVelocity()
        AutoP3MovementHandler.resetShit()
        AutoP3.waitingRing = this
    }

    fun run() {

        triggered = true

        if (rotate) {
            AutoP3.setBlinkRotation(yaw, 0f)
        }

        if (center) {
            center()
            if (isAwait) await()
            return
        }

        if (isAwait) {
            await()
            return
        }

        maybeDoRing()
    }

    fun maybeDoRing() {
        if (this !is BlinkRing) doRing()
        else AutoP3.setActiveBlink(this)
    }
}