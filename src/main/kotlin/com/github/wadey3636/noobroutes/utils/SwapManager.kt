package com.github.wadey3636.noobroutes.utils

import com.github.wadey3636.noobroutes.utils.Utils.ID
import me.modcore.Core.mc
import me.modcore.events.impl.PacketEvent
import me.modcore.utils.skyblock.modMessage
import me.modcore.utils.skyblock.skyblockID
import me.modcore.utils.skyblock.unformattedName
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent



/**
 * Taken from CGA
 */
object SwapManager {
    private var recentlySwapped = false



    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END) {
            recentlySwapped = false
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Send){
        if (event.packet is C09PacketHeldItemChange) recentlySwapped = true
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
                        if (recentlySwapped) {
                            modMessage("yo somethings wrong $itemName")
                            return SwapState.TOO_FAST
                        }
                        recentlySwapped = true
                        mc.thePlayer.inventory.currentItem = i
                        return SwapState.SWAPPED
                    } else {
                        return SwapState.ALREADY_HELD
                    }
                }
            }
        }
        modMessage("$name not found.")
        return SwapState.UNKNOWN
    }

    fun swapFromSBId(vararg skyblockID: String): SwapState {
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
                        recentlySwapped = true
                        mc.thePlayer.inventory.currentItem = i
                        return SwapState.SWAPPED
                    } else {
                        return SwapState.ALREADY_HELD
                    }
                }
            }
        }
        modMessage("$skyblockID not found.")
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
                        recentlySwapped = true
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
            recentlySwapped = true
            mc.thePlayer.inventory.currentItem = slot
            return SwapState.SWAPPED
        } else return SwapState.ALREADY_HELD
    }

    enum class SwapState{
        SWAPPED, ALREADY_HELD, TOO_FAST, UNKNOWN
    }

}