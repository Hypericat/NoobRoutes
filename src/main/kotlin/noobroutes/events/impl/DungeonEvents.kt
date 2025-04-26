package com.github.wadey3636.noobroutes.events.impl

import com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.Room
import net.minecraftforge.fml.common.eventhandler.Event

data class RoomEnterEvent(val room: Room?) : Event()
