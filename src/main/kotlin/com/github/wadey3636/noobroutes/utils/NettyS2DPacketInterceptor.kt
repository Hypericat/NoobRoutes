package com.github.wadey3636.noobroutes.utils

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import me.defnotstolen.Core.logger
import me.defnotstolen.events.impl.TerminalOpenedEvent
import me.defnotstolen.utils.postAndCatch
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S2DPacketOpenWindow
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
                    if (msg is S2DPacketOpenWindow) {
                        TerminalOpenedEvent().postAndCatch()
                    }
                    super.channelRead(ctx, msg)
                }
            })
    }
}