package noobroutes.utils.skyblock.dungeonscanning

import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.mc
import noobroutes.events.impl.RoomEnterEventFMap
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.Vec2i
import noobroutes.utils.postAndCatch
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeonscanning.DungeonScan.roomSize
import noobroutes.utils.skyblock.dungeonscanning.DungeonScan.startX
import noobroutes.utils.skyblock.dungeonscanning.DungeonScan.startZ
import noobroutes.utils.skyblock.dungeonscanning.tiles.Puzzle
import noobroutes.utils.skyblock.dungeonscanning.tiles.Room
import noobroutes.utils.skyblock.dungeonscanning.tiles.Tile
import noobroutes.utils.skyblock.dungeonscanning.tiles.UniqueRoom
import noobroutes.utils.skyblock.dungeonscanning.tiles.Unknown

object Dungeon {
    var lastRoom: Room? = null

    var currentRoom: Room? = null
        private set



    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent){
        val room = currentRoom?.uniqueRoom ?: return

        room.tiles.forEach { tile ->
            val y = room.getTopLayerOfRoom(tile.second)
            val x = (tile.second.x - startX) * (roomSize shl 1)
            val z = (tile.second.z - startZ) * (roomSize shl 1)
            devMessage(BlockPos(tile.second.x, 70, tile.second.z))
            //Renderer.drawBlock(BlockPos(x, 70, z), Color.GREEN)
        }
    }

    @SubscribeEvent
    fun onRoomEnter(eventFMap: RoomEnterEventFMap) {
        devMessage(eventFMap.room?.data?.name)
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.isEnd || !DungeonUtils.inDungeons) return
        val room = ScanUtils.getRoomFromPos(mc.thePlayer.position)
        if (lastRoom?.data?.name != room?.data?.name) {
            currentRoom = room?.uniqueRoom?.mainRoom
            RoomEnterEventFMap(room?.uniqueRoom?.mainRoom).postAndCatch()
        }
        lastRoom = room
        if (DungeonScan.shouldScan) DungeonScan.scan()
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        reset()
    }

    fun reset() {
        Info.reset()
        DungeonScan.hasScanned = false
    }


    object Info {
        val dungeonList = Array<Tile>(121) { Unknown(0, 0) }
        val uniqueRooms = mutableSetOf<UniqueRoom>()
        var roomCount = 0
        val puzzles = mutableMapOf<Puzzle, Boolean>()
        var trapType = ""
        var witherDoors = 0

        fun reset() {
            dungeonList.fill(Unknown(0, 0))
            uniqueRooms.clear()
            roomCount = 0
            puzzles.clear()
            trapType = ""
            witherDoors = 0
        }

    }

}