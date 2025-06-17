package noobroutes.utils.skyblock.dungeonScanning

import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.mc
import noobroutes.events.impl.RoomEnterEvent

import noobroutes.utils.Utils.isEnd
import noobroutes.utils.postAndCatch
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeonScanning.tiles.Puzzle
import noobroutes.utils.skyblock.dungeonScanning.tiles.Tile
import noobroutes.utils.skyblock.dungeonScanning.tiles.UniqueRoom
import noobroutes.utils.skyblock.dungeonScanning.tiles.Unknown

object Dungeon {
    var lastRoom: UniqueRoom? = null

    var currentRoom: UniqueRoom? = null
        private set



    @SubscribeEvent
    fun onRoomEnter(event: RoomEnterEvent) {
        devMessage("${event.room?.name}, ${event.room?.rotation}")
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.isEnd || !DungeonUtils.inDungeons) return
        val room = ScanUtils.getRoomFromPos(mc.thePlayer.position)
        if (lastRoom?.name != room?.data?.name) {
            currentRoom = room?.uniqueRoom
            RoomEnterEvent(room?.uniqueRoom).postAndCatch()
        }
        lastRoom = room?.uniqueRoom
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