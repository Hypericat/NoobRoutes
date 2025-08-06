package noobroutes.utils

import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.utils.Utils.ID
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.skyblockID
import noobroutes.utils.skyblock.unformattedName


/**
 * Taken from CGA
 */
object SwapManager {
    var lastSwap = 0L
    inline val recentlySwapped get() = System.currentTimeMillis() - lastSwap < 50L



    @SubscribeEvent
    fun onPacket(event: PacketEvent.Send){
        if (event.packet is C09PacketHeldItemChange) lastSwap = System.currentTimeMillis()
    }



    fun getItemSlot(item: String, ignoreCase: Boolean = true): Int? =
        mc.thePlayer?.inventory?.mainInventory?.indexOfFirst { it?.unformattedName?.contains(item, ignoreCase) == true }.takeIf { it != -1 }

    fun swapFromName(name: String): SwapState {
        for (i in 0..8) {
            val stack: ItemStack? = mc.thePlayer.inventory.getStackInSlot(i)
            val itemName = stack?.displayName
            if (itemName != null) {
                if (itemName.contains(name, ignoreCase = true)) {
                    if (mc.thePlayer.inventory.currentItem != i) {
                        if (System.currentTimeMillis() - lastSwap < 50L) devMessage("old 0 tick swap")
                        if (recentlySwapped) {
                            modMessage("yo somethings wrong $itemName")
                            return SwapState.TOO_FAST
                        }
                        lastSwap = System.currentTimeMillis()
                        mc.thePlayer.inventory.currentItem = i
                        return SwapState.SWAPPED
                    } else {
                        return SwapState.ALREADY_HELD
                    }
                }
            }
        }
        if (AutoP3.renderStyle == "cgy" && name == "TNT") modMessage("Unable to find Infinityboom TNT in your hotbar", "§0[§6Yharim§0]§7 ")
        else modMessage("$name not found.")
        return SwapState.UNKNOWN
    }

    fun swapFromSBId(vararg skyblockID: String): SwapState {
        devMessage("swapped: ${System.currentTimeMillis()}")
        for (i in 0..8) {
            val stack: ItemStack? = mc.thePlayer.inventory.getStackInSlot(i)
            val itemName = stack?.skyblockID
            if (itemName != null) {
                if (skyblockID.any { it == itemName }) {
                    if (mc.thePlayer.inventory.currentItem != i) {
                        if (System.currentTimeMillis() - lastSwap < 50L) devMessage("old 0 tick swap")
                        if (recentlySwapped) {
                            modMessage("yo somethings wrong $itemName")
                            return SwapState.TOO_FAST
                        }
                        lastSwap = System.currentTimeMillis()
                        mc.thePlayer.inventory.currentItem = i
                        return SwapState.SWAPPED
                    } else {
                        return SwapState.ALREADY_HELD
                    }
                }
            }
        }
        modMessage("${skyblockID.first()} not found.")
        return SwapState.UNKNOWN
    }


    fun swapFromSBId(bitch: Boolean, vararg skyblockID: String): SwapState {
        if (bitch) devMessage("swapped: ${System.currentTimeMillis()}")
        for (i in 0..8) {
            val stack: ItemStack? = mc.thePlayer.inventory.getStackInSlot(i)
            val itemName = stack?.skyblockID
            if (itemName != null) {
                if (skyblockID.any { it == itemName }) {
                    if (mc.thePlayer.inventory.currentItem != i) {
                        if (recentlySwapped) {
                            modMessage("yo somethings wrong $itemName")
                            return SwapState.TOO_FAST
                        }
                        lastSwap = System.currentTimeMillis()
                        mc.thePlayer.inventory.currentItem = i
                        return SwapState.SWAPPED
                    } else {
                        return SwapState.ALREADY_HELD
                    }
                }
            }
        }
        if (bitch) modMessage("${skyblockID.first()} not found.")
        return SwapState.UNKNOWN
    }



    fun swapFromId(id: Int): SwapState {
        for (i in 0..8) {
            val stack: ItemStack? = mc.thePlayer.inventory.getStackInSlot(i)
            val itemName = stack?.ID
            if (itemName != null) {
                if (itemName == id) {
                    if (mc.thePlayer.inventory.currentItem != i) {
                        if (recentlySwapped) {
                            modMessage("yo somethings wrong $itemName")
                            return SwapState.TOO_FAST
                        }
                        lastSwap = System.currentTimeMillis()
                        mc.thePlayer.inventory.currentItem = i
                        return SwapState.SWAPPED
                    } else {
                        return SwapState.ALREADY_HELD
                    }
                }
            }
        }
        modMessage("$id not found.")
        return SwapState.UNKNOWN
    }

    fun swapToSlot(slot: Int): SwapState {
        if (mc.thePlayer.inventory.currentItem != slot) {
            if (recentlySwapped) {
                modMessage("u swapping too faaaast")
                return SwapState.TOO_FAST
            }
            lastSwap = System.currentTimeMillis()
            mc.thePlayer.inventory.currentItem = slot
            return SwapState.SWAPPED
        } else return SwapState.ALREADY_HELD
    }

    enum class SwapState{
        SWAPPED, ALREADY_HELD, TOO_FAST, UNKNOWN
    }

}