package noobroutes.events.impl

import net.minecraftforge.fml.common.eventhandler.Event
import noobroutes.utils.skyblock.dungeonscanning.tiles.Room


data class RoomEnterEvent(val room: noobroutes.utils.skyblock.dungeon.tiles.Room?) : Event()

data class RoomEnterEventFMap(val room: Room?) : Event()