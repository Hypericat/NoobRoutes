package noobroutes.features.dungeon.autobloodrush


import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.utils.AutoP3Utils.walking
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import kotlin.math.abs

abstract class BloodRushRoute(val name: String, var pos: Vec3) {
    var triggered = false

    open fun reset(){
        triggered = false
    }
    fun stopWalk(){
        walking = false
        PlayerUtils.unPressKeys()
    }

    fun inNode(): Boolean{
        val inNode = abs(PlayerUtils.posX - pos.xCoord) < 0.001 &&
                abs(PlayerUtils.posZ - pos.zCoord) < 0.001 &&
                PlayerUtils.posY >= pos.yCoord - 0.01 && PlayerUtils.posY <= pos.yCoord + 0.5
        if (inNode && !triggered) {
            triggered = true
            return true
        } else if (!inNode && triggered) {
            reset()
            return false
        }
        return false
    }

    abstract fun convertToReal(room: Room)
    abstract fun runTick(room: Room)
    abstract fun runMotion(room: Room, event: MotionUpdateEvent.Pre)
    abstract fun getAsJsonObject(): JsonObject
    abstract fun loadFromJsonObject(jsonObject: JsonObject)



}