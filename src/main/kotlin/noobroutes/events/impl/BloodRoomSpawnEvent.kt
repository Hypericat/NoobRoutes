package noobroutes.events.impl

import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.Event
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom

class BloodRoomSpawnEvent(val room: UniqueRoom) : Event() {
}