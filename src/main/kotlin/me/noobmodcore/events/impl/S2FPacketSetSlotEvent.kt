package me.noobmodcore.events.impl


import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.Event
class S2FPacketSetSlotEvent(val packet: S2FPacketSetSlot) : Event() {
}