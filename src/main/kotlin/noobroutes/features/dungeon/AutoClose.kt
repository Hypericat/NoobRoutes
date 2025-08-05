package noobroutes.features.dungeon

import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Module
import noobroutes.utils.PacketUtils.sendPacket
import noobroutes.utils.Utils.isNotEnd
import noobroutes.utils.skyblock.dungeon.DungeonUtils

/**
 * Credit to Soshimee
 * Taken from Secret Guide
 * control c control v
 *
 * one could say I took inspiration
 */
object AutoClose : Module("Auto Close", description = "Taken from Secret Guide") {
    private var closeId: Int? = null

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.isNotEnd || closeId == null) return
        sendPacket(C0DPacketCloseWindow(closeId!!))
        closeId = null
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.Receive) {
        if (event.packet !is S2DPacketOpenWindow) return
        if (!DungeonUtils.inDungeons && !SecretAura.enableOutsideOfDungeons) return
        val packet = event.packet
        if (packet.guiId != "minecraft:chest") return
        if (
            (packet.windowTitle.formattedText == "Chest§r" && packet.slotCount == 27) || (packet.windowTitle.formattedText == "Large Chest§r" && packet.slotCount == 54)
        ) {
            closeId = packet.windowId
            event.isCanceled = true
        }
    }

}