package noobroutes.events.impl

import net.minecraft.network.Packet
import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

@Cancelable
class NettyPacketEvent(val packet: Packet<*>) : Event()