package noobroutes.utils.skyblock.dungeonScanning

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S3EPacketTeams
import net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.utils.*
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeonScanning.tiles.UniqueRoom


class DungeonInstance(val floor: Floor) {

    val inBoss: Boolean get() = getBoss()
    var dungeonTeammates: ArrayList<DungeonPlayer> = ArrayList(5)
    var dungeonTeammatesNoSelf: ArrayList<DungeonPlayer> = ArrayList(4)
    var leapTeammates: ArrayList<DungeonPlayer> = ArrayList(4)
    var puzzles = listOf<Puzzle>()
    var currentRoom: UniqueRoom? = null
        private set

    fun enterDungeonRoom(room: UniqueRoom?){
        currentRoom = room
    }


    private fun getBoss(): Boolean {
        return when (floor.floorNumber) {
            1 -> PlayerUtils.posX > -71 && PlayerUtils.posZ > -39
            in 2..4 -> PlayerUtils.posX > -39 && PlayerUtils.posZ > -39
            in 5..6 -> PlayerUtils.posX > -39 && PlayerUtils.posZ > -7
            7 -> PlayerUtils.posX > -7 && PlayerUtils.posZ > -7
            else -> false
        }
    }


    fun onPacket(event: PacketEvent.Receive) {
        when (event.packet) {
            is S38PacketPlayerListItem -> handleTabListPacket(event.packet)
            is S3EPacketTeams -> handleScoreboardPacket(event.packet)
            is S47PacketPlayerListHeaderFooter -> handleHeaderFooterPacket(event.packet)
        }
    }

    fun onWorldLoad() {
        dungeonTeammates = ArrayList()
        dungeonTeammatesNoSelf = ArrayList()
        leapTeammates = ArrayList()
        puzzles = emptyList()
        Blessing.entries.forEach { it.current = 0 }
    }

    fun onEntityJoin(event: EntityJoinWorldEvent) {
        val teammate = dungeonTeammatesNoSelf.find { it.name == event.entity.name } ?: return
        teammate.entity = event.entity as? EntityPlayer ?: return
    }



    private fun handleHeaderFooterPacket(packet: S47PacketPlayerListHeaderFooter) {
        Blessing.entries.forEach { blessing ->
            blessing.regex.find(packet.footer.unformattedText.noControlCodes)?.let { match -> blessing.current =
                romanToInt(match.groupValues[1])
            }
        }
    }

    private fun handleScoreboardPacket(packet: S3EPacketTeams) {
        if (packet.action != 2) return
    }

    private fun handleTabListPacket(packet: S38PacketPlayerListItem) {
        if (!packet.action.equalsOneOf(S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME, S38PacketPlayerListItem.Action.ADD_PLAYER)) return


        updateDungeonTeammates(packet.entries)
        runOnMCThread {
            puzzles =
                DungeonUtils.getDungeonPuzzles(
                    getTabList
                ) // transfer to packet based
        }
    }

    private fun updateDungeonTeammates(tabList: List<S38PacketPlayerListItem.AddPlayerData>) {
        dungeonTeammates =
            DungeonUtils.getDungeonTeammates(
                dungeonTeammates,
                tabList
            )
        dungeonTeammatesNoSelf = ArrayList(dungeonTeammates.filter { it.entity != mc.thePlayer })
    }
}