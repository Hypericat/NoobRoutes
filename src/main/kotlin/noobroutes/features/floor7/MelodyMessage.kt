package noobroutes.features.floor7

import net.minecraft.item.Item
import noobroutes.events.impl.MelodyOpenEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.S2FPacketSetSlotEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.StringSetting
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.sendCommand
import noobroutes.utils.skyblock.unformattedName
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.utils.Utils
import noobroutes.utils.skyblock.sendChatMessage
import org.lwjgl.input.Keyboard

object MelodyMessage: Module(
    name = "Melody Message",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "announces melody open and status in party chat"
) {
    private val message by StringSetting("message", "", description = "message to send")

    private var melodyWindowId = -1

    @SubscribeEvent
    fun onMelodyOpen(event: MelodyOpenEvent) {
        melodyWindowId = event.packet.windowId
    }

    @SubscribeEvent
    fun onS2F(event: S2FPacketSetSlotEvent) {
        if (event.packet.func_149175_c() != melodyWindowId) return
        val slots = listOf(16,25,34,43)
        val index = slots.indexOf(event.packet.func_149173_d())
        if (index == -1) return
        val item = event.packet.func_149174_e()
        val id = Item.getIdFromItem(item.item)
        if (id != 159 || item.metadata != 5) return
        sendChatMessage("/pc $message $index /4")
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        melodyWindowId = -1
    }
}