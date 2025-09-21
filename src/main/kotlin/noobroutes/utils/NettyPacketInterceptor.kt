package noobroutes.utils

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import noobroutes.events.impl.*

object NettyPacketInterceptor {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onNetworkEvent(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        val channel = event.manager.channel()
        val pipeline = channel.pipeline()
        pipeline.addAfter("fml:packet_handler", "noobroutes:Fuck_U_ChatTriggers", object : ChannelDuplexHandler() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                if (msg is Packet<*>) {
                    if (NettyPacketEvent(msg).postAndCatch()) {
                        PacketUtils.cancelNettyPacket(msg)
                    }
                }
                super.channelRead(ctx, msg)
            }
        }
        )
    }
}