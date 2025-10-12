package noobroutes.commands

import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.toCenteredVec3


object TpCommand {
    val posMap: HashMap<Int, BlockPos> = hashMapOf();

    fun initMap() {
        posMap["tps1".hashCode()] = BlockPos(100, 116, 42)
        posMap["tps2".hashCode()] = BlockPos(101, 115, 132)
        posMap["tps3".hashCode()] = BlockPos(10, 115, 129)
        posMap["tps4".hashCode()] = BlockPos(11, 115, 38)
        posMap["tpp1".hashCode()] = BlockPos(73, 221, 16)
        posMap["tpp2".hashCode()] = BlockPos(73, 165, 37)
        posMap["tpp3".hashCode()] = BlockPos(100, 116, 42)
        posMap["tpp4".hashCode()] = BlockPos(54, 64, 112)
        posMap["tpp5".hashCode()] = BlockPos(54, 5, 76)
        posMap["tpee3".hashCode()] = BlockPos(2, 109, 98)
        posMap["tpee2".hashCode()] = BlockPos(58, 109, 130)
        posMap["tpss".hashCode()] = BlockPos(107, 120, 94)
        posMap["tpcore".hashCode()] = BlockPos(54, 115, 51)
    }

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.Send) {
        if (!Minecraft.getMinecraft().isSingleplayer || event.packet !is C01PacketChatMessage) return;
        val chatMessage = event.packet.message.lowercase()
        if (chatMessage.length > 7 || !chatMessage.startsWith("/tp")) return;
        val pos = posMap.get(chatMessage.substring(1).hashCode())?.toCenteredVec3() ?: return
        Minecraft.getMinecraft().thePlayer.setPosition(pos.xCoord, pos.yCoord, pos.zCoord)
        Minecraft.getMinecraft().netHandler.addToSendQueue (C01PacketChatMessage("/tp ${pos.xCoord} ${pos.yCoord} ${pos.zCoord}"))
        event.isCanceled = true;
        modMessage("Teleporting to $pos")
    }

}