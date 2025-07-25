package noobroutes.features.floor7.autop3

import com.google.gson.JsonObject
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.floor7.autop3.AutoP3.depth
import noobroutes.features.floor7.autop3.AutoP3.renderStyle
import noobroutes.features.floor7.autop3.AutoP3.silentLook
import noobroutes.utils.*
import noobroutes.utils.AutoP3Utils.ringColors
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.SyncData
import noobroutes.utils.json.syncdata.*
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import kotlin.math.pow
import kotlin.math.sin


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
    companion object {
        val diameterRegex = Regex("""d:(\d+)""")
        val heightRegex = Regex("""h:(\d+)""")
    }
}

abstract class Ring(
    val base: RingBase,
    val type: RingType
) {
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

    open fun generateRingFromArgs(args: Array<out String>): Ring? {
        return null
    }

    protected fun generateRingBaseFromArgs(args: Array<out String>): RingBase {
        val diameterString = args.firstOrNull { RingBase.diameterRegex.matches(it) }
        val diameter = diameterString?.let { RingBase.diameterRegex.find(it)?.groupValues?.get(1)?.toFloatOrNull() } ?: 1f
        val heightString = args.firstOrNull { RingBase.heightRegex.matches(it) }
        val height = heightString?.let { RingBase.heightRegex.find(it)?.groupValues?.get(1)?.toFloatOrNull() } ?: 1f
        return RingBase(
            mc.thePlayer.positionVector,
            MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
            args.any {it == "term"},
            args.any { it == "leap" },
            args.any {it == "left"},
            args.any {it == "center"},
            args.any {it == "rotate" || it == "look"},
            diameter,
            height
        )
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


    fun renderRing() {
        val offsetCoords = this.coords.add(0.0, 0.03, 0.0)
        RenderUtils.drawOutlinedAABB(offsetCoords.subtract(diameter * 0.5, 0.0, diameter * 0.5).toAABB(diameter, height, diameter), Color.GREEN, thickness = 3, depth = depth)
        if (this.renderYawVector) Renderer.draw3DLine(
            listOf(
                this.coords.add(0.0, PlayerUtils.STAND_EYE_HEIGHT, 0.0),
                Vec3(this.yaw.xPart, 0.0, this.yaw.zPart).multiply(1.8).add(
                    this.coords.xCoord,
                    this.coords.yCoord + PlayerUtils.STAND_EYE_HEIGHT,
                    this.coords.zCoord
                )
            ),
            Color.GREEN,
        )
    }

    fun drawCylinderWithRingArgs(drawCoords: Vec3, color: Color) {
        Renderer.drawCylinder(drawCoords, diameter * 0.5, diameter * 0.5, 0.01, AutoP3.ringSlices, 1, 90, 0, 0, color, depth = depth)
    }

    open fun addRingData(obj: JsonObject) {}

    open fun doRing() {
    }

    fun inRing(): Boolean {
        val ringAABB = AxisAlignedBB(coords.xCoord - diameter * 0.5, coords.yCoord, coords.zCoord - diameter * 0.5, coords.xCoord + diameter * 0.5, coords.yCoord + height, coords.zCoord + diameter * 0.5)
        return ringAABB.isVecInside(mc.thePlayer.positionVector)
    }

    fun run() {
        mc.thePlayer.isSprinting = false
        AutoP3Utils.unPressKeys()

        if (center) {
            PlayerUtils.stopVelocity()
            Scheduler.schedulePostMoveEntityWithHeadingTask {
                PlayerUtils.setPosition(coords.xCoord, coords.zCoord)
                if (!isAwait) doRing()
            }
        }

        if (rotate) {
            Blink.rotate = yaw
        }

        if (isAwait) {
            PlayerUtils.stopVelocity()
            AutoP3New.waitingRing = this
        }
    }

    open fun ringCheckY(): Boolean {
        return (coords.yCoord <= mc.thePlayer.posY && coords.yCoord + height > mc.thePlayer.posY && !center) || (center && coords.yCoord == mc.thePlayer.posY && mc.thePlayer.onGround)
    }

    fun doRingArgs() {
        if (rotate) {
            if (!silentLook) Scheduler.schedulePostTickTask { mc.thePlayer.rotationYaw = yaw }
            Blink.rotate = yaw
        }

        if (renderStyle == 3) {
            Scheduler.schedulePostTickTask { mc.thePlayer.rotationYaw = yaw }
            val javaRandom = java.util.Random()
            val gaussian = javaRandom.nextGaussian().toFloat()
            val scaled = gaussian * (15)
            Scheduler.schedulePostTickTask { mc.thePlayer.rotationPitch = scaled.coerceIn(-45f, 45f) + 10f }
        }

        if (center && (mc.thePlayer.onGround || System.currentTimeMillis() - Blink.lastBlink < 100)) {

            AutoP3Utils.unPressKeys()

            Scheduler.schedulePostMoveEntityWithHeadingTask {
                mc.thePlayer.setPosition(coords.xCoord, mc.thePlayer.posY, coords.zCoord)
                PlayerUtils.stopVelocity()
            }
            AutoP3.isAligned = true
        }
    }
}