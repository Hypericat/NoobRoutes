package noobroutes.features.dungeon.autobloodrush.routes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.silent
import noobroutes.features.dungeon.autobloodrush.BloodRushRoute
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.ether
import noobroutes.utils.RotationUtils
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.RotationUtils.setAngles
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.skyblockID

class Etherwarp(pos: Vec3, var target: Vec3) : BloodRushRoute(name = "Etherwarp", pos) {
    companion object {
        fun loadFromJsonObject(jsonObject: JsonObject): Etherwarp {
            return Etherwarp(
                jsonObject.get("pos").asVec3,
                jsonObject.get("target").asVec3
            )
        }
    }


    override fun runTick(room: UniqueRoom) {
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target), true)
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        if (!silent) setAngles(angles.first, angles.second)
        stopWalk()
        PlayerUtils.sneak()
    }

    override fun runMotion(room: UniqueRoom, event: MotionUpdateEvent.Pre) {
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target), true)
        event.yaw = angles.first
        event.pitch = angles.second
        if (!mc.thePlayer.isSneaking || mc.thePlayer.heldItem.skyblockID != "ASPECT_OF_THE_VOID") {
            AutoRouteUtils.setRotation(angles.first + offset, angles.second)
            Scheduler.schedulePreTickTask {
                ether()
            }
            return
        }
        ether()
    }

    override fun getAsJsonObject(): JsonObject {
        val obj = JsonObject()
        obj.addProperty("name", name)
        obj.addProperty("pos", pos)
        obj.addProperty("target", target)
        return obj
    }
}