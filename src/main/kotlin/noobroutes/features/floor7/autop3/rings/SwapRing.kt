package noobroutes.features.floor7.autop3.rings

import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.SwapManager
import noobroutes.utils.getArg
import noobroutes.utils.skyblock.modMessage

class SwapRing(
    ringBase: RingBase = RingBase(),
    var bySBId: Boolean = false,
    var item: String = ""
) : Ring(ringBase, RingType.SWAP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            val criteriaArg = args.getArg(2, "Need swap criteria: Id, Name") ?: return null
            val bySBId = when (criteriaArg.lowercase()) {
                "name" -> false
                "id" -> true
                else -> {
                    modMessage("Need swap criteria: Id, Name")
                    return null
                }
            }
            val searchString = args.getArg(3, "Need item name/SBid") ?: return null


            return SwapRing(generateRingBaseFromArgs(args), bySBId, searchString)
        }

    }

    init {
        addBoolean("bySBiD", {bySBId}, {bySBId = it})
        addString("item", {item}, {item = it})
    }

    override fun doRing() {
        if (bySBId) {
            SwapManager.swapFromSBId(item)
            return
        }
        SwapManager.swapFromName(item)
    }
}