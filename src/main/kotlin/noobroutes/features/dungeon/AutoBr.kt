package noobroutes.features.dungeon

import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.impl.BloodRoomSpawnEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.misc.EWPathfinderModule
import noobroutes.features.move.Zpew
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.AutoP3Utils.walking
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.Utils.isStart
import noobroutes.utils.Vec2i
import noobroutes.utils.isAir
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.routes.RouteUtils.setRotation
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.Dungeon
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.skyblockID
import noobroutes.utils.toBlockPos
import noobroutes.utils.toVec3
import org.lwjgl.input.Keyboard

object AutoBr: Module(
    name = "Auto Br",
    Keyboard.KEY_NONE,
    category = Category.DUNGEON,
    description = "Bloodrushes very fast (does it outside of map)"
) {
    private val testKey by KeybindSetting("test br", Keyboard.KEY_NONE, "tests").onPress { testAutoBr() }
    private val testPearls by KeybindSetting("test pearls", Keyboard.KEY_NONE, "tests").onPress { testAutoPearl() }
    private val pearlThrowAmount by NumberSetting("pearl amount", 6, 4, 16, description = "how many pearls shall be yeeted")

    private var expectedCoords: BlockPos? = null
    private var expectedCoords2: BlockPos? = null

    private val MORT_COORDS = Vec3(0.0, 69.0, 11.0)

    private val BLOOD_DOOR_BLOCK_REL = BlockPos(-2, 73, 16)
    private val REDSTONE_BLOCK_REL = BlockPos(0, 99, 15)

    private val CLIP_0_REL = Vec3(-0.0, 100.0, 15.5)

    private val CLIP_1_REL = Vec3(-2.0, 74.0, 15.7376)
    private val CLIP_2_REL = Vec3(-3.0, 74.0, 14.0) //-2.8 14.2

    private val MIDDLE = Vec2i(-104, -104)

    private var waitingForSpawn = false

    fun reset() {
        waitingForSpawn = false
        expectedCoords = null
        expectedCoords2 = null
    }

    fun testAutoPearl() {
        //wadey pwetty pls
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        reset()
    }

    fun testAutoBr() {
        reset()
        if (Dungeon.currentRoom?.name != "Entrance" || (mc.thePlayer.posY != 98.0 && mc.thePlayer.posY != 99.0) || !isAir(mc.thePlayer.positionVector.toBlockPos().up())) return
        val realRedstoneCoords = Dungeon.Info.uniqueRooms.find { it.name == "Blood" && it.rotation != Rotations.NONE }?.getRealCoords(REDSTONE_BLOCK_REL)
        if (realRedstoneCoords == null) {
            val y = getHighestNonAirBlockY(MIDDLE.x, MIDDLE.z)
            if (y == -1) return
            EWPathfinderModule.execute(BlockPos(MIDDLE.x, y, MIDDLE.z), true)
            waitingForSpawn = true
            return
        }
        EWPathfinderModule.execute(realRedstoneCoords, true)
        expectedCoords = REDSTONE_BLOCK_REL.up()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!event.isStart || mc.thePlayer == null || !mc.thePlayer.onGround || Dungeon.currentRoom?.name != "Blood") return
        if (Dungeon.currentRoom!!.getRelativeCoords(mc.thePlayer.positionVector.toBlockPos()) != expectedCoords) return

        val stepPos = Dungeon.currentRoom!!.getRealCoords(CLIP_0_REL)
        mc.thePlayer.setPosition(stepPos.xCoord, stepPos.yCoord, stepPos.zCoord)

        val spot = Dungeon.currentRoom!!.getRealCoords(BLOOD_DOOR_BLOCK_REL).toVec3(0.5, 1.0, 0.5)
        RouteUtils.etherwarpToVec3(spot, true)

        Zpew

        expectedCoords2 = spot.toBlockPos()
        expectedCoords = null
    }

    @SubscribeEvent
    fun doClipShit(event: TickEvent.ClientTickEvent) {
        if (!event.isStart || mc.thePlayer == null || !mc.thePlayer.onGround || expectedCoords2 == null) return
        if (mc.thePlayer.positionVector.toBlockPos() != expectedCoords2) return
        expectedCoords2 = null
        val bloodRoom = Dungeon.Info.uniqueRooms.find { it.name == "Blood" } ?: return modMessage("smth went very wrong")

        Scheduler.schedulePreTickTask {
            val relPos = bloodRoom.getRealCoords(CLIP_1_REL)
            mc.thePlayer.setPosition(relPos.xCoord, relPos.yCoord, relPos.zCoord)
        }
        Scheduler.schedulePreTickTask(1) {
            val relPos2 = bloodRoom.getRealCoords(CLIP_2_REL)
            mc.thePlayer.setPosition(relPos2.xCoord, relPos2.yCoord, relPos2.zCoord)
        }
    }

    private fun getHighestNonAirBlockY(x: Int, z: Int): Int {
        for (y in mc.theWorld.actualHeight - 1 downTo 0) {
            if (!isAir(BlockPos(x, y, z))) return y
        }
        return -1
    }

    @SubscribeEvent
    fun onBloodSpawn(event: BloodRoomSpawnEvent) {
        if (waitingForSpawn && mc.theWorld.isBlockLoaded(event.room.getRealCoords(REDSTONE_BLOCK_REL))) {
            EWPathfinderModule.execute(event.room.getRealCoords(REDSTONE_BLOCK_REL), true)
            expectedCoords = REDSTONE_BLOCK_REL.up()
            waitingForSpawn = false
        }
    }
}