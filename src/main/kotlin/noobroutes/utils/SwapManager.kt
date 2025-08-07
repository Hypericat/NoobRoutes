package noobroutes.utils

import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.utils.Utils.ID
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.skyblockID
import noobroutes.utils.skyblock.unformattedName


/**
 * Modified CGA code
 * Thank you CGA for ratting Lux
 */
object SwapManager {
    var recentlySwapped = false

    enum class SwapState{
        SWAPPED, ALREADY_HELD, TOO_FAST, UNKNOWN
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Send){
        if (event.packet is C09PacketHeldItemChange) recentlySwapped = true
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTickEnd(event: TickEvent.ClientTickEvent){
        if (event.isEnd) recentlySwapped = false
    }

    fun getItemSlot(item: String, ignoreCase: Boolean = true): Int? =
        mc.thePlayer?.inventory?.mainInventory?.indexOfFirst { it?.unformattedName?.contains(item, ignoreCase) == true }.takeIf { it != -1 }


    fun swapFromName(name: String): SwapState {
        return findAndSwapItem { stack ->
            stack.displayName?.contains(name, ignoreCase = true) == true
        } ?: run {
            modMessage("$name not found.")
            SwapState.UNKNOWN
        }
    }

    fun swapFromSBId(vararg skyblockID: String, silent: Boolean = false): SwapState {
        if (!silent) devMessage("swapped: ${System.currentTimeMillis()}")

        return findAndSwapItem { stack ->
            stack.skyblockID.let { itemId -> skyblockID.contains(itemId) }
        } ?: run {
            if (!silent) modMessage("${skyblockID.first()} not found.")
            SwapState.UNKNOWN
        }
    }

    fun swapFromId(id: Int): SwapState {
        return findAndSwapItem { stack ->
            stack.ID == id
        } ?: run {
            modMessage("$id not found.")
            SwapState.UNKNOWN
        }
    }


    fun swapToSlot(slot: Int): SwapState {
        return if (mc.thePlayer.inventory.currentItem != slot) {
            if (recentlySwapped) {
                modMessage("Error Swapping")
                SwapState.TOO_FAST
            } else {
                performSwap(slot)
                SwapState.SWAPPED
            }
        } else {
            SwapState.ALREADY_HELD
        }
    }

    private fun findAndSwapItem(predicate: (ItemStack) -> Boolean): SwapState? {
        for (i in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i) ?: continue
            if (!predicate(stack)) continue
            return when {
                mc.thePlayer.inventory.currentItem == i -> SwapState.ALREADY_HELD
                recentlySwapped -> {
                    modMessage("tried to swap too fast ${stack.displayName ?: stack.skyblockID}")
                    SwapState.TOO_FAST
                }
                else -> {
                    performSwap(i)
                    SwapState.SWAPPED
                }
            }

        }
        return null
    }

    private fun performSwap(slot: Int) {
        recentlySwapped = true
        mc.thePlayer.inventory.currentItem = slot
    }
}