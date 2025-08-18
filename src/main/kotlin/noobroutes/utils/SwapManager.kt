package noobroutes.utils

import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.WorldChangeEvent
import noobroutes.mixin.accessors.C08Accessor
import noobroutes.utils.Utils.ID
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.skyblockID
import noobroutes.utils.skyblock.unformattedName


/**
 * No longer modified CGA code
 * Thank you CGA for ratting Lux
 */
object SwapManager {
    var serverSlot = -1

    private var dontSwap = true
    private var hasSwapped = false

    private inline val serverItem: ItemStack
        get() = mc.thePlayer.inventory.mainInventory[serverSlot]

    @SubscribeEvent
    fun adjustServer(event: PacketEvent.Send) {
        if (event.packet !is C09PacketHeldItemChange) return

        if (event.packet.slotId == serverSlot) {
            event.isCanceled = true
            return
        }

        if (dontSwap) {
            event.isCanceled = true
            mc.thePlayer.inventory.currentItem = serverSlot
            devMessage("prevented 0 tick")
            return
        }

        hasSwapped = true
        serverSlot = event.packet.slotId
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldChangeEvent) {
        serverSlot = -1
    }

    @SubscribeEvent
    fun onC08(event: PacketEvent.Send) {
        if (event.packet !is C08PacketPlayerBlockPlacement) return
        if (hasSwapped) dontSwap = true

        if (event.packet.stack == null && event.packet.position == BlockPos(-1, -1, -1)) event.isCanceled = true
    }

    fun onPreRunTick(){
        dontSwap = false
        hasSwapped = false
    }
/*
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTickEnd(event: TickEvent.ClientTickEvent) {
        if (event.isEnd) dontSwap = false
    }

 */

    enum class SwapState{
        SWAPPED, ALREADY_HELD, TOO_FAST, UNKNOWN
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
            if (dontSwap) {
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
                dontSwap -> {
                    modMessage("Tried to 0 tick swap ${stack.displayName ?: stack.skyblockID}")
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

    fun performSwap(slot: Int) {
        if (slot !in 0..8) return modMessage("not a good index to swap to")
        //if (dontSwap) return modMessage("tried to 0 tick swap")
        if (mc.thePlayer.inventory.currentItem == slot) return

        mc.thePlayer.inventory.currentItem = slot
        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot))
    }
}