package noobroutes.features.misc

import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.impl.NettyPacketEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.WorldChangeEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.PacketUtils
import noobroutes.utils.Utils.isNotStart
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.nullableUuid
import noobroutes.utils.skyblock.unformattedName
import noobroutes.utils.skyblock.uuid
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

@DevOnly
object HotBarSwitcher : Module(
    name = "Hotbar Switcher",
    Keyboard.KEY_NONE,
    category = Category.MISC,
    description = "automatically switches hotbar"
) {
    private val startRecording by KeybindSetting("Record Swaps", Keyboard.KEY_NONE, "Starts recording swaps on press. Stops after inv close").onPress { saveHotbar() }
    private val swapKeybind by KeybindSetting("Swap Items", Keyboard.KEY_NONE, "swaps Items on Press").onPress { generateSwaps(savedHotbar) }
    private val instant by BooleanSetting("Instant Swap", description = "swaps with 0 ms delay, also doesnt actually open inv")

    private data class Swap(val usedButton: Int, val slotId: Int)

    private data class SwappableItem(val uuidHash: Int?, val nameHash: Int, val slot: Int)

    private var savedHotbar = arrayOfNulls<SwappableItem>(8)

    private var currentSwapList = mutableListOf<Swap>()

    private var inGui = false

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.isNotStart || currentSwapList.isEmpty()) return
        val gui = mc.currentScreen

        if (gui !is GuiInventory) {
            currentSwapList.clear()
            return
        }

        val first = currentSwapList.removeFirst()

        mc.playerController.windowClick(gui.inventorySlots.windowId, first.slotId, first.usedButton, 2, mc.thePlayer)

        if (currentSwapList.isEmpty()) {
            mc.thePlayer.closeScreen()
            PlayerUtils.rePressKeys()
        }
    }

    private fun openInv() {
        mc.displayGuiScreen(GuiInventory(mc.thePlayer))
    }

    private fun saveHotbar() {
        val hotbar = arrayOfNulls<SwappableItem>(8)
        for (slot in 0..7) { //no menu
            val itemStack = mc.thePlayer.inventory.getStackInSlot(slot) ?: return modMessage("no empty slots allowed in a saved hotbar")
            val uuid = itemStack.nullableUuid
            val name = itemStack.unformattedName
            hotbar[slot] = SwappableItem(uuid.hashCode().takeIf { it != 0 }, name.hashCode(), slot)
        }
        savedHotbar = hotbar
        modMessage("Hotbar Saved!")
    }

    private fun generateSwaps(hotbar: Array<SwappableItem?>) {
        if (mc.currentScreen != null || inGui) return
        if (hotbar.size != 8) return modMessage("Invalid Hotbar")

        val fakeInventory = mc.thePlayer.inventory.mainInventory.copyOf()

        val createdList = mutableListOf<Swap>()
        for (item in hotbar) {
            if (item == null) return modMessage("Invalid Hotbar")

            var currentSlot = getItemSlotFromUUIDHashInCustomArray(item.uuidHash, fakeInventory) ?: getItemSlotFromNameHashInCustomArray(item.nameHash, fakeInventory) ?: return modMessage("couldn't find an item")

            if (currentSlot == item.slot) continue

            if (item.slot !in 0..7) return modMessage("broken hotbar safe")

            fakeInventory.swap(item.slot, currentSlot)

            if (currentSlot in 0..8) currentSlot += 36

            val currentSwap = Swap(item.slot, currentSlot)
            createdList.add(currentSwap)
        }

        if (createdList.isEmpty()) return modMessage("already selected")

        if (instant)  {
            instantSwap(createdList)
            return
        }

        currentSwapList = createdList
        openInv()
    }

    private fun instantSwap(createdList: MutableList<Swap>) {
        var actionNumber: Short = 1

        while (createdList.isNotEmpty()) {
            val first = createdList.removeFirst()

            PacketUtils.sendPacket(C0EPacketClickWindow(0, first.slotId, first.usedButton, 2, null, actionNumber))
            //devMessage("slotId: ${first.slotId}, button: ${first.usedButton}")

            val lowerSlot = if (first.slotId > 35) first.slotId - 36 else first.slotId
            mc.thePlayer.inventory.mainInventory.swap(lowerSlot, first.usedButton)

            actionNumber++
        }
        PacketUtils.sendPacket(C0DPacketCloseWindow(0))

    }

    @SubscribeEvent
    fun onGuiServer(event: NettyPacketEvent) {
        if (event.packet is S2DPacketOpenWindow) inGui = true
        if (event.packet is S2EPacketCloseWindow) inGui = false
    }

    @SubscribeEvent
    fun onWorld(event: WorldChangeEvent) {
        inGui = false
    }

    @SubscribeEvent
    fun onClient(event: PacketEvent.Send) {
        if (event.packet is C0DPacketCloseWindow) inGui = false
        if (event.packet is C0EPacketClickWindow) inGui = true
    }

    //@SubscribeEvent
    fun onInv(event: PacketEvent.Send) {
        if (event.packet is C0EPacketClickWindow) devMessage("slotId: ${event.packet.slotId}, button: ${event.packet.usedButton}, mode: ${event.packet.mode}, item: ${event.packet.clickedItem}")
        if (event.packet is C0DPacketCloseWindow) devMessage(System.currentTimeMillis(), "c0d: ")
    }

    private fun getItemSlotFromUUIDHashInCustomArray(uuidHash: Int?, customArray: Array<ItemStack>): Int? {
        if (uuidHash == null) return null
        return customArray.indexOfLast { it.uuid.hashCode() == uuidHash }.takeIf { it != -1 }
    }

    private fun getItemSlotFromNameHashInCustomArray(nameHash: Int, customArray: Array<ItemStack>): Int? =
        customArray.indexOfLast { it.unformattedName.hashCode() == nameHash }.takeIf { it != -1 }

    private fun <T> Array<T>.swap(i: Int, j: Int) {
        val tempVal = this[i]
        this[i] = this[j]
        this[j] = tempVal
    }
}