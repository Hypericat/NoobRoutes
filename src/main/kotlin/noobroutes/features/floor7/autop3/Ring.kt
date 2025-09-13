package noobroutes.features.floor7.autop3

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.rings.BlinkRing
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


    var isEditingRing = false
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

    fun renderRing(color: Color, renderMode: String) {
        if (renderMode.hashCode() == "BBG".hashCode()) {
            renderBBGRing(color)
            drawRingEditing(color)
            return
        }
        renderBoxRing(color)
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

    private fun renderCircularRing(color: Color) {
        val offsetCoords = this.coords.add(0.0, 0.03, 0.0)
        RenderUtils.drawCylinder(offsetCoords, diameter,)
        RenderUtils.drawOutlinedAABB(offsetCoords.subtract(diameter * 0.5, 0.0, diameter * 0.5).toAABB(diameter, height, diameter), color, thickness = 3, depth = true)
    }

    private fun renderBBGRing(color: Color) {
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

    private fun renderBoxRing(color: Color) {
        val offsetCoords = this.coords.add(0.0, 0.03, 0.0)
        RenderUtils.drawOutlinedAABB(offsetCoords.subtract(diameter * 0.5, 0.0, diameter * 0.5).toAABB(diameter, height, diameter), color, thickness = 3, depth = true)
    }

    protected open fun addRingData(obj: JsonObject) {}

    open fun doRing() {
        AutoP3MovementHandler.resetShit()
    }

    open fun runTriggeredLogic() {
        triggered = false
    }

    open fun inRing(pos: Vec3 = mc.thePlayer.positionVector): Boolean {
        if (center) return checkInBoundsWithSpecifiedHeight(pos,0f) && mc.thePlayer.onGround
        return checkInBoundsWithSpecifiedHeight(pos, height)
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
        if (this !is BlinkRing) doRing()
        else AutoP3.setActiveBlink(this)
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
        this.addSwitch("Left", {left}, {left= it})
        this.addSwitch("Term", {term}, {term = it})
        this.addSwitch("Leap", {leap}, {leap = it})
    }

    protected fun EditGuiBase.EditGuiBaseBuilder.addOnCloseAndOpen(){
        this.setOnOpen {
            isEditingRing = true
        }
        this.setOnClose {
            isEditingRing
        }
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
        return builder.build()
    }
}