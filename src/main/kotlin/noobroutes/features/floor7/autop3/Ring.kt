package noobroutes.features.floor7.autop3

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.features.floor7.autop3.rings.StopRing
import noobroutes.features.misc.TimerHud
import noobroutes.features.render.FreeCam
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.utils.*
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.SyncData
import noobroutes.utils.json.syncdata.*
import noobroutes.utils.render.Color
import noobroutes.utils.render.ColorUtil.withAlpha
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import java.util.EnumSet
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin


data class RingBase(
    var coords: Vec3 = Vec3(mc.thePlayer?.posX ?: 0.0, mc.thePlayer?.posY ?: 0.0, mc.thePlayer?.posZ ?: 0.0),
    var yaw: Float,
    var await: EnumSet<RingAwait>,
    var center: Boolean,
    var rotate: Boolean,
    var stopWatch: Boolean,
    var diameter: Float,
    var height: Float) {
    constructor() : this(Vec3(0.0, 0.0, 0.0), 0f, EnumSet.noneOf<RingAwait>(RingAwait::class.java), false, false, false, 1f, 1f)

    companion object {
        val diameterRegex = Regex("""d:(\d+)""")
        val heightRegex = Regex("""h:(\d+)""")
    }
}

abstract class Ring(
    val base: RingBase,
    val type: RingType
) {

    //"Simple Ring", "Ring"
    companion object {
        val OCTAGON_HASHCODE = "Octagon".hashCode()
        val BBG_HASHCODE: Int = "BBG".hashCode()
        val BOX_HASHCODE = "Box".hashCode()
        val SIMPLE_RING_HASHCODE = "Simple Ring".hashCode()
        val RING_HASHCODE = "Ring".hashCode()
        const val ONE_THREE_HUNDREDTH = 1 / 300.0
    }

    inline val ringName get() = type.ringName

    inline var coords: Vec3
        get() = base.coords
        set(value) {base.coords = value}
    inline var yaw: Float
        get() = base.yaw
        set(value) {base.yaw = value}
    inline var ringAwaits
        get() = base.await
        set(value) {base.await = value}
    inline var center: Boolean
        get() = base.center
        set(value) {base.center = value}
    inline var rotate: Boolean
        get() = base.rotate
        set(value) {base.rotate = value}
    inline var stopWatch: Boolean
        get() = base.stopWatch
        set(value) {base.stopWatch = value}
    inline var diameter: Float
        get() = base.diameter
        set(value) {base.diameter = value}
    inline var height: Float
        get() = base.height
        set(value) {base.height = value}

    inline val isAwait: Boolean
        get() = ringAwaits.isNotEmpty()


    var isEditingRing = false
    var triggered = false
    val internalRingData = mutableListOf<SyncData>()

    fun getAsJsonObject(): JsonObject{
        val obj = JsonObject().apply {
            addProperty("type", type.ringName)
            addProperty("coords", coords)
            addProperty("yaw", yaw)
            for (await in ringAwaits) {
                addProperty(await.name, true)
            }


            if (center) addProperty("center", true)
            if (rotate) addProperty("rotate", true)
            if (stopWatch) addProperty("stopwatch", true)
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

    open fun renderRing(color: Color, secondaryColor: Color, renderMode: String) {
        when(renderMode.hashCode()) {
            BBG_HASHCODE -> renderBBGRing(color)
            BOX_HASHCODE -> renderBoxRing(color)
            RING_HASHCODE -> renderCircularRing(color, secondaryColor)
            SIMPLE_RING_HASHCODE -> renderSimpleCircularRing(color)
            OCTAGON_HASHCODE -> renderOctagonRing(color)
        }
        drawRingEditing(color)
    }

    protected open fun drawRingEditing(color: Color){
        if (this.isEditingRing) Renderer.draw3DLine(
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

    //quad thing might be interesting
    protected fun renderOctagonRing(color: Color) {
        val offsetCoords = this.coords.add(0.0, 0.03, 0.0)
        val r = diameter * 0.6f
        RenderUtils.drawDisc(offsetCoords,  r,8, 90, 0, 22.5, color, true, 4f)
        RenderUtils.drawFilledDisc(offsetCoords,  r * 0.8f,8, 90, 0, 22.5, color, color.withAlpha(0.333f), true, 3f)
    }

    protected fun renderCircularRing(color1: Color, color2: Color) {
        val offsetCoords = this.coords.add(0.0, 0.03, 0.0)
        val r = diameter * 0.6f
        RenderUtils.drawDisc(offsetCoords.add(0.0, (0.45 * sin(System.currentTimeMillis().toDouble() * ONE_THREE_HUNDREDTH)) + 0.528 , 0.0), r, 24, 90, 0, 0, color1, true, 4f)
        RenderUtils.drawDisc(offsetCoords.add(0.0, (-0.45 * sin(System.currentTimeMillis().toDouble() * ONE_THREE_HUNDREDTH)) + 0.528 , 0.0), r, 24, 90, 0, 0, color1, true, 4f)
        RenderUtils.drawDisc(offsetCoords.add(0.0, 0.503, 0.0), r, 24, 90, 0, 0, color1, true, 4f)
        RenderUtils.drawDisc(offsetCoords.add(0.0, 0.03, 0.0), r, 24, 90, 0, 0, color2, true, 4f)
        RenderUtils.drawDisc(offsetCoords.add(0.0, 1.03, 0.0), r, 24, 90, 0, 0, color2, true, 4f)
    }

    protected fun renderSimpleCircularRing(color: Color) {
        val offsetCoords = this.coords.add(0.0, 0.03, 0.0)
        RenderUtils.drawDisc(offsetCoords,  diameter * 0.6f, 24, 90, 0, 0, color, true, 4f)
    }

    protected fun renderBBGRing(color: Color) {
        val offsetCoords = this.coords.add(0.0, 0.03, 0.0)
        var dDia = diameter.toDouble();
        var offset = offsetCoords.subtract(diameter * 0.5, 0.0, diameter * 0.5);

        val vertices = listOf(offset, offset.add(dDia, 0.0, 0.0), offset.add(dDia, 0.0, dDia), offset.add(0.0, 0.0, dDia), offset);
        RenderUtils.drawLines(vertices, color, 6f, true)
        RenderUtils.drawFilledVertices(vertices.subList(0, 4), color.withAlpha(0.333f), 6f, true)

        dDia *= 0.8;
        offset = offsetCoords.subtract(dDia * 0.5, -0.02, dDia * 0.5)
        RenderUtils.drawLines(listOf(offset, offset.add(dDia, 0.0, 0.0), offset.add(dDia, 0.0, dDia), offset.add(0.0, 0.0, dDia), offset), color, 4f, true)

        Renderer.drawStringInWorld(this.ringName, this.coords.add(Vec3(0.0, 0.3, 0.0)), Color.DARK_GRAY, depth = false, shadow = true, scale = 0.022f)
    }

    protected fun renderBoxRing(color: Color) {
        val offsetCoords = this.coords.add(0.0, 0.03, 0.0)
        RenderUtils.drawOutlinedAABB(offsetCoords.subtract(diameter * 0.5, 0.0, diameter * 0.5).toAABB(diameter, height, diameter), color, thickness = 3, depth = true)
    }

    protected open fun addRingData(obj: JsonObject) {}

    open fun doRing() {
        if (stopWatch && this !is BlinkRing && this !is StopRing) TimerHud.toggle()
        AutoP3MovementHandler.resetShit()
    }

    open fun runTriggeredLogic() {
        triggered = false
    }

    protected open fun meetsGroundRequirements(): Boolean = !center || mc.thePlayer.onGround

    protected open fun getRingHeight(): Float = if (center) 0f else height

     fun intersectedWithRing(oldPos: Vec3, newPos: Vec3): Boolean {
        if (!meetsGroundRequirements()) return false

        val direction = newPos - oldPos

        val height = getRingHeight()

        if (direction.length < 1e-8f) {
            return checkInBoundsWithSpecifiedHeight(newPos, height)
        }

        val halfWidth = diameter * 0.5
        val min = this.coords.subtract(halfWidth, 0.0, halfWidth)
        val max = this.coords.add(halfWidth, height.toDouble(), halfWidth)

        var tMin = 0.0
        var tMax = 1.0

        for (axis in 0..2) {
            if (abs(direction[axis]) < 1e-8f) {
                if (oldPos[axis] < min[axis] || oldPos[axis] > max[axis]) {
                    return false
                }
            } else {
                var t1 = (min[axis] - oldPos[axis]) / direction[axis]
                var t2 = (max[axis] - oldPos[axis]) / direction[axis]

                if (t1 > t2) {
                    val temp = t1
                    t1 = t2
                    t2 = temp
                }

                tMin = max(tMin, t1)
                tMax = min(tMax, t2)

                if (tMin > tMax) {
                    return false
                }
            }
        }

        return true
    }

    open fun inRing(pos: Vec3 = mc.thePlayer.positionVector): Boolean {
        return meetsGroundRequirements() && checkInBoundsWithSpecifiedHeight(pos, getRingHeight())
    }

    protected fun checkInBoundsWithSpecifiedHeight(pos: Vec3, heightToUse: Float): Boolean{
        return pos.isVecInBounds(coords.xCoord - diameter * 0.5, coords.yCoord, coords.zCoord - diameter * 0.5, coords.xCoord + diameter * 0.5, coords.yCoord + heightToUse, coords.zCoord + diameter * 0.5)
    }

    protected fun center() {
        PlayerUtils.stopVelocity()
        mc.thePlayer.isSprinting = false
        if (!FreeCam.enabled) PlayerUtils.unPressKeys()
        AutoP3MovementHandler.resetShit()

        Scheduler.scheduleHighestPostMoveEntityWithHeadingTask {
            PlayerUtils.setPosition(coords.xCoord, coords.zCoord)
            if (isAwait) await() else maybeDoRing()
        }
    }

    protected fun await(){
        PlayerUtils.stopVelocity()
        AutoP3MovementHandler.resetShit()
        AutoP3.waitingRing = this
    }


    open fun run() {
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
        if (this !is BlinkRing) {
            doRing()
            return
        }
        AutoP3.setActiveBlink(this)
    }

    protected fun EditGuiBase.EditGuiBaseBuilder.addXYZ(includeY: Boolean) {
        this.addSlider(
            "Ring X",
            coords.xCoord - 3,
            coords.xCoord + 3,
            0.1,
            1,
            { coords.xCoord },
            {
                coords = Vec3(it, coords.yCoord, coords.zCoord)
            }
        )
        if (includeY) this.addSlider(
            "Ring Y",
            coords.yCoord - 3,
            coords.yCoord + 3,
            0.1,
            1,
            { coords.yCoord },
            {
                coords = Vec3(coords.xCoord, it, coords.zCoord)
            }
        )
        this.addSlider(
            "Ring Z",
            coords.zCoord - 3,
            coords.zCoord + 3,
            0.1,
            1,
            { coords.zCoord },
            {
                coords = Vec3(coords.xCoord, coords.yCoord, it)
            }
        )
    }
    protected fun EditGuiBase.EditGuiBaseBuilder.addYaw(){
        this.addSlider("Yaw", yaw - 5.0, yaw + 5.0, 0.1, 2, {yaw.toDouble()}, {yaw = it.toFloat()})
    }

    protected fun EditGuiBase.EditGuiBaseBuilder.addDiameterAndHeight(includeHeight: Boolean) {
        this.addSlider("Diameter", min = 0.0, max = 3.0, 0.1, 2, {diameter.toDouble()}, {diameter = it.toFloat()})
        if (includeHeight) this.addSlider("Height", min = 0.0, max = 3.0, 0.1, 2, {height.toDouble()}, {height = it.toFloat()})
    }

    protected fun EditGuiBase.EditGuiBaseBuilder.addArgs(){
        this.addSwitch("Center", {center}, {center = it})
        this.addSwitch("Rotate", {rotate}, {rotate = it})
        this.addSwitch("Stopwatch", {stopWatch}, {stopWatch = it})
    }

    protected fun EditGuiBase.EditGuiBaseBuilder.addOnCloseAndOpen(){
        this.setOnOpen {
            isEditingRing = true
        }
        this.setOnClose {
            AutoP3.saveRings()
            isEditingRing = false
        }
    }

    protected fun EditGuiBase.EditGuiBaseBuilder.addAwait() {

        //this.addSelector("Await", RingAwait.getOptionsList(), { ringAwaits.getIndex() }, {ringAwaits = RingAwait[it]})
    }

    protected open val includeY = true
    protected open val includeHeight = true

    open fun extraArgs(builder: EditGuiBase.EditGuiBaseBuilder) {
    }

    open fun getEditGuiBase(): EditGuiBase {
        val builder = EditGuiBase.EditGuiBaseBuilder()
        builder.addYaw()
        builder.addXYZ(includeY)
        builder.addDiameterAndHeight(includeHeight)
        builder.addArgs()
        extraArgs(builder)
        builder.setName(ringName)
        builder.addOnCloseAndOpen()
        builder.addAwait()
        return builder.build()
    }
}