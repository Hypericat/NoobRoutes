package com.github.wadey3636.noobroutes.utils

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import me.defnotstolen.Core.logger
import me.defnotstolen.events.impl.TerminalOpenedEvent
import me.defnotstolen.utils.postAndCatch
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

object NettyS2DPacketInterceptor {
    @SubscribeEvent
    fun onNetworkEvent(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        val channel = event.manager.channel()
        val pipeline = channel.pipeline()
        logger.info("Current pipeline handlers: ${pipeline.names()}")
        if (!pipeline.names().contains("MyPacketInterceptor")) {
            pipeline.addFirst("MyPacketInterceptor", object : MessageToMessageDecoder<Packet<*>>() {
                @Throws(Exception::class)
                override fun decode(ctx: ChannelHandlerContext, packet: Packet<*>, out: MutableList<Any>) {
                    if (packet is S2DPacketOpenWindow) {
                       ClientUtils.clientScheduleTask { TerminalOpenedEvent().postAndCatch() }
                    }
                    out.add(packet)
                }
            })
            logger.info("Added MyPacketInterceptor at the beginning of the pipeline")
        }
    }
}