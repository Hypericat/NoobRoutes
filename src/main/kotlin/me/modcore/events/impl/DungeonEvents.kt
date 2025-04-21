package me.modcore.events.impl

import me.modcore.utils.skyblock.dungeon.tiles.Room
import net.minecraftforge.fml.common.eventhandler.Event

data class RoomEnterEvent(val room: Room?) : Event()
