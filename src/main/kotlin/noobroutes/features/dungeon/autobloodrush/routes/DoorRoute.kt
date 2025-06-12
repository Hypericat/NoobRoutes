package noobroutes.features.dungeon.autobloodrush.routes

import com.google.gson.JsonObject
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.direction
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.silent
import noobroutes.features.dungeon.autobloodrush.BloodRushRoute
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.utils.RotationUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.isAir
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.sendChatMessage

import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.doingShit
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.expectedX
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.expectedZ
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.lastAttempt
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.skip
import noobroutes.utils.toBlockPos

class DoorRoute(pos: Vec3) : BloodRushRoute("Door", pos) {


    companion object {
        fun loadFromJsonObject(jsonObject: JsonObject): DoorRoute {
            return DoorRoute(jsonObject.get("pos").asVec3)
        }
    }



    override fun runTick(room: Room) {
        if (doingShit) return modMessage("doing shit")
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

        val blockPosPlayer = Vec3(x,y,z).toBlockPos()
        direction = doorDirection(room)
        devMessage(direction)
        lastAttempt = mc.thePlayer.positionVector
        skip = true
        val angles = Pair(getYaw(), getPitch(mc.thePlayer.positionVector.toBlockPos()))
        if (!silent) RotationUtils.setAngles(angles.first, angles.second)
        expectedX = blockPosPlayer.x + 0.5
        expectedZ = blockPosPlayer.z + 0.5
        if (mc.isSingleplayer) {
            Scheduler.schedulePreTickTask(2) { sendChatMessage("/tp $expectedX ${blockPosPlayer.y + 2.0} $expectedZ") }
        }
        PlayerUtils.unSneak()

        if (state == SwapManager.SwapState.SWAPPED || mc.thePlayer.isSneaking) {
            AutoRouteUtils.setRotation(angles.first, angles.second)
            Scheduler.schedulePreTickTask {
                AutoRouteUtils.aotvTarget = null
                AutoRouteUtils.unsneakRegistered = true
            }
            return
        }
        AutoRouteUtils.aotvTarget = null
        AutoRouteUtils.unsneakRegistered = true
    }

    override fun runMotion(
        room: Room,
        event: MotionUpdateEvent.Pre
    ) {
        val angles = Pair(getYaw(), getPitch(mc.thePlayer.positionVector.toBlockPos()))
        event.yaw = angles.first
        event.pitch = angles.second
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