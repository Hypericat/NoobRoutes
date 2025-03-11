package me.defnotstolen.events.impl

import me.defnotstolen.utils.skyblock.dungeon.tiles.Room
import net.minecraftforge.fml.common.eventhandler.Event

data class RoomEnterEvent(val room: Room?) : Event()
