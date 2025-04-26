package com.github.wadey3636.noobroutes.events.impl

import net.minecraft.network.Packet
import net.minecraftforge.fml.common.eventhandler.Event

class NettyPacketEvent(val event: Packet<*>) : Event()