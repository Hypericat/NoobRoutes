package com.github.wadey3636.noobroutes.utils

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import me.modcore.events.impl.MelodyOpenEvent
import me.modcore.events.impl.S08Event
import me.modcore.events.impl.S2FPacketSetSlotEvent
import me.modcore.events.impl.TerminalOpenedEvent
import me.modcore.utils.postAndCatch
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

object NettyS2DPacketInterceptor {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onNetworkEvent(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        val channel = event.manager.channel()
        val pipeline = channel.pipeline()
        pipeline.addAfter("fml:packet_handler", "Fuck_U_ChatTriggers", object : ChannelDuplexHandler() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                when (msg) {
                    is S2DPacketOpenWindow -> {
                        TerminalOpenedEvent().postAndCatch()
                        if (msg.windowTitle.unformattedText == "Click the button on time!") MelodyOpenEvent(msg).postAndCatch()
                    }
                    is S08PacketPlayerPosLook -> S08Event().postAndCatch()
                    is S2FPacketSetSlot -> S2FPacketSetSlotEvent(msg).postAndCatch()
                }
                super.channelRead(ctx, msg)
                }
            })
    }
}