package noobroutes.features.floor7.autop3

import com.google.gson.JsonObject
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3.depth
import noobroutes.features.floor7.autop3.AutoP3.renderStyle
import noobroutes.features.floor7.autop3.AutoP3.silentLook
import noobroutes.utils.AutoP3Utils.ringColors
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.add
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.SyncData
import noobroutes.utils.json.syncdata.*
import noobroutes.utils.multiply
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.toAABB
import kotlin.math.pow
import kotlin.math.sin

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
    var rotate: Boolean,
    var diameter: Float,
    var height: Float
) {
    var renderYawVector = false
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

    fun renderRing() {
        val offsetCoords = this.coords.add(0.0, 0.03, 0.0)
        val topOffset = (0.48 * sin(System.currentTimeMillis().toDouble()/300)) + 0.5
        val bottomOffset = 1 - topOffset
        when (renderStyle) {
            0 -> {
                drawCylinderWithRingArgs(offsetCoords.add(0.0, topOffset * height, 0.0), Color.GREEN) //moving ring 1
                drawCylinderWithRingArgs(offsetCoords.add(0.0, bottomOffset * height, 0.0), Color.GREEN) //moving ring 2
                drawCylinderWithRingArgs(offsetCoords, Color.DARK_GRAY) //bottom static
                drawCylinderWithRingArgs(offsetCoords.add(0.0, 0.5 * height, 0.0), Color.GREEN) // middle static
                drawCylinderWithRingArgs(offsetCoords.add(0.0, height.toDouble(), 0.0), Color.DARK_GRAY) //bottom static
            }
            1 -> drawCylinderWithRingArgs(offsetCoords, Color.GREEN)
            2 -> RenderUtils.drawOutlinedAABB(offsetCoords.subtract(diameter * 0.5, 0.0, diameter * 0.5).toAABB(diameter, height, diameter), Color.GREEN, thickness = 3, depth = depth)
            3 -> Renderer.drawCylinder(this.coords, diameter * 0.5, diameter * 0.5, -0.01, 24, 1, 90, 0, 0, ringColors.getOrDefault(this.type, Color(255, 0, 255)), depth = depth)
        }
        if (this.renderYawVector) Renderer.draw3DLine(listOf(
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

    private fun drawCylinderWithRingArgs(drawCoords: Vec3, color: Color) {
        Renderer.drawCylinder(drawCoords, diameter * 0.5, diameter * 0.5, 0.01, AutoP3.ringSlices, 1, 90, 0, 0, color, depth = depth)
    }

    private fun loadInternalRingData(obj: JsonObject){
        internalRingData.forEach { it.readFrom(obj) }
    }


    open fun addRingData(obj: JsonObject) {}

    open fun doRing() {
    }

    fun inRing(playerCoords: Vec3): Boolean {
        return (coords.xCoord - playerCoords.xCoord).pow(2) + (coords.zCoord - playerCoords.zCoord).pow(2) <= (diameter * 0.5).pow(2)
    }

    open fun ringCheckY(): Boolean {
        return (coords.yCoord <= mc.thePlayer.posY && coords.yCoord + height > mc.thePlayer.posY && !center) || center && coords.yCoord == mc.thePlayer.posY && mc.thePlayer.onGround
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
            mc.thePlayer.setPosition(coords.xCoord, mc.thePlayer.posY, coords.zCoord)
            PacketUtils.sendPacket(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.onGround))
            AutoP3.isAligned = true
        }
    }
}