package noobroutes.features.dungeon.autobloodrush.routes

import com.google.gson.JsonObject
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.ExpectedS08
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.clipS08
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.direction
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.getDir
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
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.sendChatMessage

class DoorRoute(pos: Vec3) : BloodRushRoute("Door", pos) {


    companion object {
        fun loadFromJsonObject(jsonObject: JsonObject): DoorRoute {
            return DoorRoute(jsonObject.get("pos").asVec3)
        }
    }



    override fun runTick(room: Room) {
        if (System.currentTimeMillis() - thrown < 10000) return modMessage("doing shit")
        PlayerUtils.unSneak()
        val state = SwapManager.swapFromName("pearl")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        val x = PlayerUtils.posX
        val y = PlayerUtils.posY
        val z = PlayerUtils.posZ
        if (y != 69.0 || x > 0 || z > 0 || x < -200 || z < -200) return
        val xDec = (x + 200) % 1
        val zDec = (z + 200) % 1
        if (xDec != 0.5 || zDec != 0.5) return
        val dir = getDir()
        val yaw = when (dir) {
            0 -> 10f
            1 -> -170f
            2 -> 100f
            3 -> -80f
            else -> return
        }
        if (!silent) RotationUtils.setAngles(yaw, 68f)
        thrown = System.currentTimeMillis()
        devMessage(yaw)
        val expectedX = x + if (dir == 3) 1 else if (dir == 2) -1 else 0
        val expectedZ = z + if (dir == 0) 1 else if (dir == 1) -1 else 0
        clipS08 = ExpectedS08(expectedX, expectedZ, dir)
        if (mc.isSingleplayer) {
            Scheduler.schedulePreTickTask(2) { sendChatMessage("/tp $expectedX 70 $expectedZ") }
        }
    }



    override fun runMotion(
        room: Room,
        event: MotionUpdateEvent.Pre
    ) {
        val dir = getDir()
        val yaw = when (dir) {
            0 -> 10f
            1 -> -170f
            2 -> 100f
            3 -> -80f
            else -> return
        }
        event.yaw = yaw
        event.pitch = 68f
        if (mc.thePlayer.isSneaking || !mc.thePlayer.heldItem.displayName.contains("pearl", true)) {
            AutoRouteUtils.setRotation(yaw + offset, 68f)
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



    private fun doorDirection(room: Room): Rotations {
        val realCoord = room.getRealCoords(pos).toBlockPos()
        return when {
            isDoorBlock(realCoord.add(-1, 0, 0)) -> Rotations.EAST
            isDoorBlock(realCoord.add(1, 0, 0)) -> Rotations.WEST
            isDoorBlock(realCoord.add(0, 0, 1)) -> Rotations.NORTH
            isDoorBlock(realCoord.add(0, 0, -1)) -> Rotations.SOUTH
            else -> Rotations.NONE
        }
    }




    private fun getYaw(): Float {
        return when(direction) {
            Rotations.EAST  -> 90f
            Rotations.WEST  -> -90f
            Rotations.SOUTH  -> -180f
            Rotations.NORTH ->  0f
            else -> 0f
        }
    }

    private fun getPitch(orgPos: BlockPos): Float {
        return if (isAir(orgPos.add(0, 3, 0))) -79f else 0f
    }


    private fun isDoorBlock(blockPosition: BlockPos): Boolean {
        val state = mc.theWorld.getBlockState(blockPosition)
        val block = state.block
        val meta = block.getMetaFromState(state)
        return block == Blocks.coal_block || (block == Blocks.stained_hardened_clay && meta == 14)
    }


}