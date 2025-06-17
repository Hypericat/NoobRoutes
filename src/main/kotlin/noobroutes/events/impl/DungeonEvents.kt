package noobroutes.events.impl

import net.minecraftforge.fml.common.eventhandler.Event
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom


data class RoomEnterEvent(val room: UniqueRoom?) : Event()
