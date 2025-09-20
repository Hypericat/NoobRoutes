package noobroutes.events.impl

import net.minecraft.network.Packet
import net.minecraftforge.fml.common.eventhandler.Event

class NettyPacketEvent(val packet: Packet<*>) : Event()