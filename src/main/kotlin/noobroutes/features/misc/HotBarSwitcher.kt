package noobroutes.features.misc

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.config.DataManager
import noobroutes.events.impl.NettyPacketEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.WorldChangeEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.ActionSetting
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.DropdownSetting
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

@DevOnly
object HotBarSwitcher : Module(
    name = "Hotbar Switcher",
    Keyboard.KEY_NONE,
    category = Category.MISC,
    description = "automatically switches hotbar"
) {
    private val instant by BooleanSetting("Instant Swap", description = "swaps with 0 ms delay, also doesnt actually open inv")

    private val hotbar1Toggle by DropdownSetting("Hotbar 1")
    private val hotbar1Save by ActionSetting("Save Hotbar", description = "Saves current hotbar as hotbar 1", default = { saveHotbar(0) }).withDependency { hotbar1Toggle }
    private val hotbar1Keybind by KeybindSetting("Keybind 1", Keyboard.KEY_NONE, "Keybind to select hotbar 1").onPress { generateSwaps(0) }.withDependency { hotbar1Toggle }

    private val hotbar2Toggle by DropdownSetting("Hotbar 2")
    private val hotbar2Save by ActionSetting("Save Hotbar", description = "Saves current hotbar as hotbar 2", default = { saveHotbar(1) }).withDependency { hotbar2Toggle }
    private val hotbar2Keybind by KeybindSetting("Keybind 2", Keyboard.KEY_NONE, "Keybind to select hotbar 2").onPress { generateSwaps(1) }.withDependency { hotbar2Toggle }

    private val hotbar3Toggle by DropdownSetting("Hotbar 3")
    private val hotbar3Save by ActionSetting("Save Hotbar", description = "Saves current hotbar as hotbar 3", default = { saveHotbar(2) }).withDependency { hotbar3Toggle }
    private val hotbar3Keybind by KeybindSetting("Keybind 3", Keyboard.KEY_NONE, "Keybind to select hotbar 3").onPress { generateSwaps(2) }.withDependency { hotbar3Toggle }

    private val hotbar4Toggle by DropdownSetting("Hotbar 4")
    private val hotbar4Save by ActionSetting("Save Hotbar", description = "Saves current hotbar as hotbar 4", default = { saveHotbar(3) }).withDependency { hotbar4Toggle }
    private val hotbar4Keybind by KeybindSetting("Keybind 4", Keyboard.KEY_NONE, "Keybind to select hotbar 4").onPress { generateSwaps(3) }.withDependency { hotbar4Toggle }

    private val hotbar5Toggle by DropdownSetting("Hotbar 5")
    private val hotbar5Save by ActionSetting("Save Hotbar", description = "Saves current hotbar as hotbar 5", default = { saveHotbar(4) }).withDependency { hotbar5Toggle }
    private val hotbar5Keybind by KeybindSetting("Keybind 5", Keyboard.KEY_NONE, "Keybind to select hotbar 5").onPress { generateSwaps(4) }.withDependency { hotbar5Toggle }



    private data class Swap(val usedButton: Int, val slotId: Int)

    private data class SwappableItem(val uuidHash: Int?, val nameHash: Int, val slot: Int)

    private var currentSwapList = mutableListOf<Swap>()

    private var inGui = false

    private var hotbarMap = mutableMapOf<Int, Array<SwappableItem>>()

    init {
        loadHotbarsFromFile()
    }

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

    private fun saveHotbar(hotbarNumber: Int) {
        val hotbar = Array(8) { slot ->
            val itemStack = mc.thePlayer.inventory.getStackInSlot(slot) ?: return modMessage("no empty slots allowed in a saved hotbar")

            val uuid = itemStack.nullableUuid
            val name = itemStack.unformattedName

            SwappableItem(uuid.hashCode().takeIf { it != 0 }, name.hashCode(), slot)
        }
        hotbarMap[hotbarNumber] = hotbar
        modMessage("Hotbar Saved!")
        saveHotbarsToFile()
    }

    private fun generateSwaps(hotbarNumber: Int) {
        val hotbar = hotbarMap[hotbarNumber] ?: return modMessage("not found")

        if (hotbar.size != 8) return modMessage("Invalid Hotbar")

        if (mc.currentScreen != null || inGui) return modMessage("cant be in gui")

        val fakeInventory = mc.thePlayer.inventory.mainInventory.copyOf()

        val createdList = mutableListOf<Swap>()
        for (item in hotbar) {

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

    private fun swappableSlotToJsonObject(swappableItem: SwappableItem): JsonObject {
        val obj = JsonObject()
        obj.addProperty("uuidHash", swappableItem.uuidHash)
        obj.addProperty("nameHash", swappableItem.nameHash)
        obj.addProperty("slot", swappableItem.slot)
        return obj
    }

    private fun saveHotbarsToFile() {
        val jsonHotbars = JsonObject()
        for (hotbar in hotbarMap) {
            jsonHotbars.add(hotbar.key.toString(), generateHotbarJsonArray(hotbar.value))
        }
        DataManager.saveDataToFile("hotbar", jsonHotbars)
    }

    private fun loadHotbarsFromFile() {
        val jsonData = DataManager.loadDataFromFileObject("hotbar")
        for (jsonHotbar in jsonData) {
            val normalHotbar = generateJsonArrayHotbar(jsonHotbar.value.asJsonArray)
            hotbarMap[jsonHotbar.key.toInt()] = normalHotbar
        }
    }

    private fun generateHotbarJsonArray(hotbar: Array<SwappableItem>): JsonArray {
        val array = JsonArray()
        for (slot in hotbar) {
            array.add(swappableSlotToJsonObject(slot))
        }

        return array
    }

    private fun generateJsonArrayHotbar(jsonArray: JsonArray): Array<SwappableItem> {
        val list = mutableListOf<SwappableItem>()

        for (slot in jsonArray) {
            val obj = slot.asJsonObject
            val uuid = obj.get("uuidHash")?.asInt
            val nameHash = obj.get("nameHash").asInt
            val slot = obj.get("slot").asInt
            list.add(SwappableItem(uuid, nameHash, slot))
        }

        return list.toTypedArray()
    }



}