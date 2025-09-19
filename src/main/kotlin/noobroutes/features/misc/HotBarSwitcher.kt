package noobroutes.features.misc

import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.isNotStart
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

@DevOnly
object HotBarSwitcher : Module(
    name = "Hotbar Switcher",
    Keyboard.KEY_NONE,
    category = Category.MISC,
    description = "automatically switches hotbar"
) {
    private val startRecording by KeybindSetting("Record Swaps", Keyboard.KEY_NONE, "Starts recording swaps on press. Stops after inv close").onPress { recording = true }
    private val swapKeybind by KeybindSetting("Swap Items", Keyboard.KEY_NONE, "swaps Items on Press").onPress { startSwapping() }

    private var recording = false
    private var hasOpenedInv = false
    private var recordedSwaps = mutableListOf<Swap>()

    private data class Swap(val usedButton: Int, val slotId: Int)

    private var swapList = mutableListOf<Swap>()

    private var currentSwapList = mutableListOf<Swap>()

    private val blinkPackets = mutableListOf<C03PacketPlayer>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.isNotStart || currentSwapList.isEmpty()) return
        val gui = mc.currentScreen

        if (gui !is GuiInventory) {
            currentSwapList.clear()
            for (c03 in blinkPackets) { PacketUtils.sendPacket(c03) }
            blinkPackets.clear()
            return
        }

        PlayerUtils.rePressKeys()

        val first = currentSwapList.removeFirst()

        mc.playerController.windowClick(gui.inventorySlots.windowId, first.slotId, first.usedButton, 2, mc.thePlayer)

        devMessage("slotId: ${first.slotId}, button: ${first.usedButton}")

        if (currentSwapList.isEmpty()) {
            mc.thePlayer.closeScreen()
            PlayerUtils.rePressKeys()
            for (c03 in blinkPackets) { PacketUtils.sendPacket(c03) }
            blinkPackets.clear()
        }
    }

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if (currentSwapList.isEmpty() || event.packet !is C03PacketPlayer) return
        event.isCanceled = true
        blinkPackets.add(event.packet)
    }

    private fun startSwapping() {
        currentSwapList = swapList.toMutableList() //this is needed
        mc.displayGuiScreen(GuiInventory(mc.thePlayer))
        PlayerUtils.rePressKeys()
        Mouse.setGrabbed(true)
    }

    @SubscribeEvent
    fun onInvOpenClose(event: GuiOpenEvent) {
        if (!recording) return
        if (event.gui is GuiInventory) hasOpenedInv = true
        if (event.gui == null && hasOpenedInv) { stopRecording() }
    }

    @SubscribeEvent
    fun onGuiRender(event: GuiScreenEvent.DrawScreenEvent.Pre) {
        if (event.gui is GuiInventory && currentSwapList.isNotEmpty()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onGuiClick(event: PacketEvent.Send) {
        if (!recording || !hasOpenedInv || event.packet !is C0EPacketClickWindow || event.packet.mode != 2 || event.packet.windowId != 0) return

        recordedSwaps.add(Swap(event.packet.usedButton, event.packet.slotId))
        modMessage("swapped ${event.packet.usedButton} with ${event.packet.slotId}")
    }

    private fun stopRecording() {
        modMessage("stopped recording")
        recording = false
        hasOpenedInv = false

        swapList = recordedSwaps.toMutableList() //this is needed
        recordedSwaps.clear()
    }
}