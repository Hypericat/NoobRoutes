package noobroutes.features.dungeon.autoroute

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute.depth
import noobroutes.utils.AutoP3Utils.walking
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import kotlin.math.ceil
import kotlin.math.floor

abstract class Node(
    val name: String,
    val priority: Int,
    var pos: Vec3,
    var awaitSecrets: Int = 0,
    var maybeSecret: Boolean = false,
    var delay: Long = 0,
    var center: Boolean = false,
    var stop: Boolean = false,
    var chain: Boolean = false
) {
    enum class RunStatus {
        NotExecuted,
        Unfinished,
        Complete
    }

    var runStatus = RunStatus.NotExecuted

    open fun awaitMotion(event: MotionUpdateEvent.Pre, room: Room) {}
    open fun motion(event: MotionUpdateEvent.Pre, room: Room) {}
    open fun awaitTick(room: Room) {}
    open fun tick(room: Room) {}
    abstract fun render(room: Room)
    abstract fun nodeAddInfo(obj: JsonObject)

    fun stopWalk(){
        walking = false
        PlayerUtils.unPressKeys()
    }

    fun getAsJsonObject(): JsonObject{
        val obj = JsonObject().apply {
            addProperty("name", name)
            addProperty("position", pos)
            if (awaitSecrets > 0) addProperty("awaitSecret", awaitSecrets)
            if (maybeSecret) addProperty("maybeSecret", true)
            if (delay > 0) addProperty("delay", delay)
            if (center) addProperty("center", true)
            if (stop) addProperty("stop", true)
            if (chain) addProperty("chain", true)
        }
        nodeAddInfo(obj)
        return obj
    }




    fun calcFloorPos(c: Double, v: Double): Double{
        return if (c < 0) {
            ceil(c) - v / 10
        } else {
            return floor(c) + v / 10
        }
    }

    internal fun drawNode(room: Room, color: Color) {
        Renderer.drawCylinder(room.getRealCoords(pos.add(Vec3(0.0, 0.03, 0.0))), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, color, depth = depth)
    }

    fun center(){
        if (mc.thePlayer.posZ < 0 || mc.thePlayer.posZ > 0) mc.thePlayer.setPosition(
            calcFloorPos(mc.thePlayer.posX, 5.0),
            mc.thePlayer.posY,
            calcFloorPos(mc.thePlayer.posZ, 5.0)
        )
    }

}
