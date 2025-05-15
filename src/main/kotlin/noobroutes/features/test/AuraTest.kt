package noobroutes.features.test

import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.AuraManager
import noobroutes.utils.Utils.isStart
import noobroutes.utils.toVec3

object AuraTest : Module("Aura Test", category = Category.MISC, description = "") {
    val blockAuraList = mutableListOf<BlockPos>()
    private val reach by NumberSetting("Reach", 4.5, 1, 6, 0.1, description = "Reach")
    private val cooldown by NumberSetting("Cooldown", 500, 50, 10000, 50, description = "")

    private var lastClick = 0L
    @SubscribeEvent
    fun testAura(event: TickEvent.ClientTickEvent){
        if (!event.isStart || System.currentTimeMillis() - lastClick < cooldown || mc.thePlayer == null) return
        blockAuraList.forEach { block  ->
            if (mc.thePlayer.getPositionEyes(0f).distanceTo(block.toVec3()) <= reach) {
                AuraManager.auraBlock(block)
                lastClick = System.currentTimeMillis()
            }
        }


    }
}