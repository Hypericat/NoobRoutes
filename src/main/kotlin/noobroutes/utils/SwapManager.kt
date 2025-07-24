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
import noobroutes.utils.Utils.isStart
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.skyblockID
import noobroutes.utils.skyblock.unformattedName


/**
 * Taken from CGA
 */
object SwapManager {
    var recentlySwapped = false



    @SubscribeEvent
    fun onPacket(event: PacketEvent.Send){
        if (event.packet is C09PacketHeldItemChange) recentlySwapped = true
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTickEnd(event: TickEvent.ClientTickEvent){
        if (event.isEnd) recentlySwapped = false
    }



    /**
     * Retrieves the index of the first inventory slot containing an item with a name that matches the specified string.
     *
     * @param item The substring to search for in the unformatted names of items in the inventory.
     * @param ignoreCase A flag indicating whether the search should ignore case differences. Default is true.
     * @return The index of the first matching inventory slot, or null if no match is found.
     */
    fun getItemSlot(item: String, ignoreCase: Boolean = true): Int? =
        mc.thePlayer?.inventory?.mainInventory?.indexOfFirst { it?.unformattedName?.contains(item, ignoreCase) == true }.takeIf { it != -1 }


    /**
     * Attempts to swap the player's current inventory slot to the one holding an item
     * with a display name that matches the specified name. If a matching item is found,
     * and the swap is performed, or it determines the swap's status otherwise.
     *
     * @param name The name to search for in the display names of items in the player's inventory.
     * @return A `SwapState` indicating the result of the operation:
     * - `SwapState.SWAPPED` if the item was found and the slot was successfully swapped.
     * - `SwapState.ALREADY_HELD` if the item is already in the currently held inventory slot.
     * - `SwapState.TOO_FAST` if the swap attempt was made too quickly after a previous swap.
     * - `SwapState.UNKNOWN` if no matching item was found in the inventory.
     */
    fun swapFromName(name: String): SwapState {
        for (i in 0..8) {
            val stack: ItemStack? = mc.thePlayer.inventory.getStackInSlot(i)
            val itemName = stack?.displayName
            if (itemName != null) {
                if (itemName.contains(name, ignoreCase = true)) {
                    if (mc.thePlayer.inventory.currentItem != i) {
                        if (recentlySwapped) {
                            modMessage("Swapped too fast $itemName")
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
        if (AutoP3.renderStyle == 3 && name == "TNT") modMessage("Unable to find Infinityboom TNT in your hotbar", "§0[§6Yharim§0]§7 ")
        else modMessage("$name not found.")
        return SwapState.UNKNOWN
    }

    /**
     * Attempts to swap the player's held item in their inventory to the first slot
     * containing an item with a matching Skyblock ID from the provided list.
     *
     * @param skyblockID Vararg parameter containing one or more Skyblock IDs to search for in the player's inventory.
     * @return A [SwapState] indicating the result of the swap attempt. Possible values are:
     * - [SwapState.SWAPPED]: The item with the matching Skyblock ID was successfully swapped to the active slot.
     * - [SwapState.ALREADY_HELD]: The item with the matching Skyblock ID was already in the active slot.
     * - [SwapState.TOO_FAST]: A previous swap was performed too recently, preventing a new swap.
     * - [SwapState.UNKNOWN]: No item with a matching Skyblock ID was found in the player's inventory.
     */
    fun swapFromSBId(vararg skyblockID: String): SwapState {
        devMessage("swapped: ${System.currentTimeMillis()}")
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
        modMessage("${skyblockID.first()} not found.")
        return SwapState.UNKNOWN
    }

    /**
     * Attempts to swap the player's held item in their inventory to the first slot
     * containing an item with a matching Skyblock ID from the provided list.
     *
     * @param skyblockID Vararg parameter containing one or more Skyblock IDs to search for in the player's inventory.
     * @param bitch Stop bitching in the chat ffs
     * @return A [SwapState] indicating the result of the swap attempt. Possible values are:
     * - [SwapState.SWAPPED]: The item with the matching Skyblock ID was successfully swapped to the active slot.
     * - [SwapState.ALREADY_HELD]: The item with the matching Skyblock ID was already in the active slot.
     * - [SwapState.TOO_FAST]: A previous swap was performed too recently, preventing a new swap.
     * - [SwapState.UNKNOWN]: No item with a matching Skyblock ID was found in the player's inventory.
     */
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
                        recentlySwapped = true
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


    /**
     * Attempts to swap the currently held inventory item to an item with the given ID
     * in the player's inventory. Ensures that swaps are not performed too frequently.
     *
     * @param id The ID of the item to swap to.
     * @return A `SwapState` enum indicating the result of the swap attempt:
     * - `SWAPPED` if the item was successfully swapped.
     * - `ALREADY_HELD` if the item is already in the currently held slot.
     * - `TOO_FAST` if a swap was attempted too soon after a previous swap.
     * - `UNKNOWN` if the item ID could not be found in the inventory.
     */
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



    /**
     * Attempts to switch the currently held inventory slot to the given slot.
     * Returns the state of the swap attempt.
     *
     * @param slot The target inventory slot to switch to.
     * @return A [SwapState] indicating the result of the swap. Possible values are:
     * - [SwapState.SWAPPED]: The slot was successfully switched to.
     * - [SwapState.ALREADY_HELD]: The specified slot is already selected.
     * - [SwapState.TOO_FAST]: The swap attempt was made too quickly after the previous swap.
     */
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