package noobroutes.features.dungeon.autobloodrush.routes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.ExpectedS08
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.clipS08
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.silent
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.thrown
import noobroutes.features.dungeon.autobloodrush.BloodRushRoute
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.utils.*
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeonScanning.tiles.Room
import noobroutes.utils.skyblock.dungeonScanning.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.sendChatMessage

class DoorRoute(pos: Vec3) : BloodRushRoute("Door", pos) {
    companion object {
        fun loadFromJsonObject(jsonObject: JsonObject): DoorRoute {
            return DoorRoute(jsonObject.get("pos").asVec3)
        }
    }


    var cancelMotion = false
    override fun runTick(room: UniqueRoom) {
        if (System.currentTimeMillis() - thrown < 10000)  {
            cancelMotion = true
            return modMessage("doing shit")
        }
        val x = PlayerUtils.posX
        val y = PlayerUtils.posY
        val z = PlayerUtils.posZ
        if (y != 69.0 || x > 0 || z > 0 || x < -200 || z < -200) {
            cancelMotion = true
            return
        }
        val xDec = (x + 200) % 1
        val zDec = (z + 200) % 1
        if (xDec != 0.5 || zDec != 0.5) {
            cancelMotion = true
            return
        }
        val dir = getDir()

        val isOpen = isAir(AutoBloodRush.getClosestDoorToPlayer(room)?.pos ?: return)

        /*val isOpen = when (dir) {
            0 -> isAir(mc.thePlayer.positionVector.toBlockPos().add(1, 0, 0))
            1 -> isAir(mc.thePlayer.positionVector.toBlockPos().add(-1, 0, 0))
            2 -> isAir(mc.thePlayer.positionVector.toBlockPos().add(0, 0, 1))
            3 -> isAir(mc.thePlayer.positionVector.toBlockPos().add(0, 0, -1))
            else -> false
        }*/
        if (isOpen) {
            devMessage("4")
            val dx = if (dir == 0) 1 else if (dir == 1) -1 else 0
            val dz = if (dir == 2) 1 else if (dir == 3) -1 else 0
            AutoRouteUtils.etherwarpToVec3(mc.thePlayer.positionVector.add(dx * 4.0, 0.0, dz * 4.0), silent)
            cancelMotion = true
            return
        }
        PlayerUtils.unSneak()
        val state = SwapManager.swapFromName("pearl")
        if (state == SwapManager.SwapState.UNKNOWN) {
            cancelMotion = true
            return
        }
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            cancelMotion = true
            return
        }

        val yaw = when (dir) {
            0 -> 14.8f
            1 -> -167.6f
            2 -> 101.8f
            3 -> -81.9f
            else -> {
                cancelMotion = true
                return
            }
        }
        val pitch = when (dir) {
            0 -> 65.6f
            1 -> 67.2f
            2 -> 66.9f
            3 -> 67.6f
            else -> {
                cancelMotion = true
                return
            }
        }
        if (!silent) RotationUtils.setAngles(yaw, pitch)
        thrown = System.currentTimeMillis()
        devMessage(yaw)
        val expectedX = x + if (dir == 3) 1 else if (dir == 2) -1 else 0
        val expectedZ = z + if (dir == 0) 1 else if (dir == 1) -1 else 0
        clipS08 = ExpectedS08(expectedX, expectedZ, dir)
        if (mc.isSingleplayer) {
            Scheduler.schedulePreTickTask(2) { sendChatMessage("/tp $expectedX 70 $expectedZ") }
        }
    }

    fun getDir(): Int {
        return when {
            mc.theWorld.getTileEntity(mc.thePlayer.positionVector.toBlockPos().add(0,1,1)) != null -> 0
            mc.theWorld.getTileEntity(mc.thePlayer.positionVector.toBlockPos().add(0,1,-1)) != null -> 1
            mc.theWorld.getTileEntity(mc.thePlayer.positionVector.toBlockPos().add(-1,1,0)) != null -> 2
            mc.theWorld.getTileEntity(mc.thePlayer.positionVector.toBlockPos().add(1,1,0)) != null -> 3
            else -> 69420
        }
    }

    override fun runMotion(
        room: UniqueRoom,
        event: MotionUpdateEvent.Pre
    ) {
        if (cancelMotion) {
            cancelMotion = false
            return
        }
        val dir = getDir()
        val yaw = when (dir) {
            0 -> 14.8f
            1 -> -167.6f
            2 -> 101.8f
            3 -> -81.9f
            else -> return
        }
        val pitch = when (dir) {
            0 -> 65.6f
            1 -> 67.2f
            2 -> 66.9f
            3 -> 67.6f
            else -> return
        }
        event.yaw = yaw
        event.pitch = pitch
        if (mc.thePlayer.isSneaking || !mc.thePlayer.heldItem.displayName.contains("pearl", true)) {
            AutoRouteUtils.setRotation(yaw + offset, pitch)
            Scheduler.schedulePreTickTask {
                AutoBloodRush.autoBrUnsneakRegistered = true
            }
            return
        }
        AutoBloodRush.autoBrUnsneakRegistered = true
    }

    override fun getAsJsonObject(): JsonObject {
        val obj = JsonObject()
        obj.addProperty("name", name)
        obj.addProperty("pos", pos)
        return obj
    }


}