package noobroutes.features.routes.autobloodrush.routes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.silent
import noobroutes.features.dungeon.autobloodrush.BloodRushRoute
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.ether
import noobroutes.utils.RotationUtils
import noobroutes.utils.SwapManager
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage

class BloodRushEtherwarp(pos: Vec3, var target: Vec3) : BloodRushRoute(name = "Etherwarp", pos) {
    companion object {
        fun loadFromJsonObject(jsonObject: JsonObject): BloodRushEtherwarp {
            return BloodRushEtherwarp(
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
        stopWalk()
        PlayerUtils.unSneak()
        AutoRouteUtils.setRotation(angles.first, angles.second, silent)
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