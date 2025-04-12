package com.github.wadey3636.noobroutes.utils

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import me.defnotstolen.events.impl.S08Event
import me.defnotstolen.events.impl.TerminalOpenedEvent
import me.defnotstolen.utils.postAndCatch
import net.minecraft.network.play.server.S08PacketPlayerPosLook
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
                when (msg) {
                    is S2DPacketOpenWindow -> TerminalOpenedEvent().postAndCatch()
                    is S08PacketPlayerPosLook -> S08Event().postAndCatch()
                }
                super.channelRead(ctx, msg)
                }
            })
    }
}