package noobroutes.features.dungeon.autobloodrush


import com.google.gson.JsonObject
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.nodeColor
import noobroutes.utils.AutoP3Utils.walking
import noobroutes.utils.middle
import noobroutes.utils.offset
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import kotlin.math.abs
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.editMode
import noobroutes.utils.add

abstract class BloodRushRoute(val name: String, var pos: Vec3) {
    var triggered = false

    open fun reset(){
        triggered = false
    }
    fun stopWalk(){
        walking = false
        PlayerUtils.unPressKeys()
    }

    fun renderIndex(room: Room, index: Int) {
        Renderer.drawStringInWorld(index.toString(), room.getRealCoords(pos).add(0.0, 0.6, 0.0), nodeColor)
    }

    fun render(room: Room) {
        RenderUtils.drawOutlinedAABB(AxisAlignedBB.fromBounds(-0.5, 0.0, -0.5, 0.5, 0.4, 0.5).offset(room.getRealCoords(pos)), color = nodeColor)
    }

    fun inNode(room: Room): Boolean{
        val realCoords = room.getRealCoords(pos)
        val inNode = realCoords.distanceToPlayer <= 0.5
        if (inNode && !triggered) {
            triggered = true
            return true
        } else if (!inNode && triggered) {
            reset()
            return false
        }
        return false
    }

    abstract fun runTick(room: Room)
    abstract fun runMotion(room: Room, event: MotionUpdateEvent.Pre)
    abstract fun getAsJsonObject(): JsonObject



}