package noobroutes.utils

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraft.util.IChatComponent
import noobroutes.Core.mc

class FakeContainer(
    val windowId: Int,
    val guiId: String,
    val windowTitle: IChatComponent,
    val slotCount: Int
) {
    enum class Type {
        Container,
        None
    }

    companion object {
        val ALLOWED_GUI_IDS = listOf("minecraft:chest")

        var onSlotUpdate: (
            container: FakeContainer,
            slot: Int,
            itemStack: ItemStack?) -> Unit =
            { container, slot, itemStack ->

            }
        fun fromPacket(packet: S2DPacketOpenWindow): FakeContainer {
            return FakeContainer(packet.windowId, packet.guiId, packet.windowTitle, packet.slotCount)
        }

    }

    var transactionId: Short = 0
    val unformattedTitle get() = windowTitle.unformattedText.noControlCodes
    val slots: Array<ItemStack?> = arrayOfNulls(slotCount)

    var lastClick = System.currentTimeMillis()
    var click_delay = 350L

    var type: Type = if (guiId in ALLOWED_GUI_IDS && slots.isNotEmpty()) {
        Type.Container
    } else {
        Type.None
    }



    fun handleItemsPacket(packet: S30PacketWindowItems): Boolean {
        val windowID = packet.func_148911_c()
        if (windowID != this.windowId) return false
        val items = packet.itemStacks
        for (i in items.indices) {
            this.setSlot(windowID, i, items[i])
        }
        return true
    }

    fun getNextTransactionID(): Short {
        val screen = mc.currentScreen
        if (screen is GuiContainer && screen.inventorySlots.windowId == this.windowId) {
            transactionId = screen.inventorySlots.getNextTransactionID(mc.thePlayer.inventory)
        } else {
            transactionId++
        }
        return transactionId
    }


    fun handleSetSlotPacket(packet: S2FPacketSetSlot): Pair<Int, ItemStack>? {
        return this.setSlot(packet.func_149175_c(), packet.func_149173_d(), packet.func_149174_e())
    }

    fun setSlot(windowId: Int, slot: Int, itemStack: ItemStack): Pair<Int, ItemStack>? {
        if (this.windowId != windowId || this.slotCount - 1 < slot) return null
        this.slots[slot] = itemStack
        onSlotUpdate(this, slot, itemStack)
        return Pair(slot, itemStack)
    }
    fun sendWindowClick(slot: Int, button: Int, mode: Int, itemStack: ItemStack): Boolean {
        if (System.currentTimeMillis() - this.lastClick < click_delay) return false
        if (this.slots.size - 1 < slot) return false
        PacketUtils.sendPacket(C0EPacketClickWindow(this.windowId, slot, button, mode, itemStack, this.getNextTransactionID()))
        return true
    }

    fun leftClickWindow(slot: Int): Boolean {
        val inventory = mc.thePlayer.inventory
        val held = inventory.itemStack
        val result = slots[slot]?.let { sendWindowClick(slot, 0, 0, it) }
        if (result != true) return false
        inventory.itemStack = slots[slot]
        slots[slot] = held

        return true
    }

    fun closeWindow(){

    }



}