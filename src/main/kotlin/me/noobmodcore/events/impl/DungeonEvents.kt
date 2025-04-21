package me.noobmodcore.events.impl

import me.noobmodcore.utils.skyblock.dungeon.tiles.Room
import net.minecraftforge.fml.common.eventhandler.Event

data class RoomEnterEvent(val room: Room?) : Event()
