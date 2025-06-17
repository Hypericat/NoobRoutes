package noobroutes.utils.skyblock.dungeonscanning

import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import noobroutes.Core.mc
import noobroutes.utils.equalsOneOf
import noobroutes.utils.skyblock.dungeonscanning.tiles.Door
import noobroutes.utils.skyblock.dungeonscanning.tiles.DoorType
import noobroutes.utils.skyblock.dungeonscanning.tiles.Room
import noobroutes.utils.skyblock.dungeonscanning.tiles.RoomState
import noobroutes.utils.skyblock.dungeonscanning.tiles.RoomType
import noobroutes.utils.skyblock.dungeonscanning.tiles.UniqueRoom
import noobroutes.utils.skyblock.dungeonscanning.tiles.Unknown
import kotlin.collections.get
import kotlin.shr
import kotlin.text.compareTo
import kotlin.text.set

object MapUpdate {
    var roomAdded = false

    fun updateUniques() {
        val visited = BooleanArray(121)
        for (x in 0..10) {
            for (z in 0..10) {
                val index = z * 11 + x
                if (visited[index]) continue
                visited[index] = true

                val room = Dungeon.Info.dungeonList[index]
                if (room !is Room) continue

                val connected = getConnectedIndices(x, z)
                var unique = room.uniqueRoom
                if (unique == null || unique.name.startsWith("Unknown")) {
                    unique = connected.firstOrNull {
                        (Dungeon.Info.dungeonList[it.second * 11 + it.first] as? Room)?.uniqueRoom?.name?.startsWith("Unknown") == false
                    }?.let {
                        (Dungeon.Info.dungeonList[it.second * 11 + it.first] as? Room)?.uniqueRoom
                    } ?: unique
                }

                val finalUnique = unique ?: UniqueRoom(x, z, room)

                finalUnique.addTiles(connected)

                connected.forEach {
                    visited[it.second * 11 + it.first] = true
                }
            }
        }
        roomAdded = false
    }

    private fun getConnectedIndices(arrayX: Int, arrayY: Int): List<Pair<Int, Int>> {
        val tile = Dungeon.Info.dungeonList[arrayY * 11 + arrayX]
        if (tile !is Room) return emptyList()
        val directions = listOf(
            Pair(0, 1),
            Pair(1, 0),
            Pair(0, -1),
            Pair(-1, 0)
        )
        val connected = mutableListOf<Pair<Int, Int>>()
        val queue = mutableListOf(Pair(arrayX, arrayY))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (connected.contains(current)) continue
            connected.add(current)
            directions.forEach {
                val x = current.first + it.first
                val y = current.second + it.second
                if (x !in 0..10 || y !in 0..10) return@forEach
                if (Dungeon.Info.dungeonList[y * 11 + x] is Room) {
                    queue.add(Pair(x, y))
                }
            }
        }
        return connected
    }

}