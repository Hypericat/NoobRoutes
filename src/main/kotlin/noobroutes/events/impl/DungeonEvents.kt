package noobroutes.events.impl

import net.minecraftforge.fml.common.eventhandler.Event
import noobroutes.utils.skyblock.dungeon.tiles.Room

data class RoomEnterEvent(val room: Room?) : Event()
