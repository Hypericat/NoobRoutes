package noobroutes.events.impl

import net.minecraftforge.fml.common.eventhandler.Event
import noobroutes.utils.skyblock.dungeonScanning.tiles.UniqueRoom


data class RoomEnterEvent(val room: UniqueRoom?) : Event()
