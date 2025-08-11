package noobroutes.features.floor7

import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.BossEventDispatcher.inF7Boss
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.AuraManager
import noobroutes.utils.Utils.isNotStart
import noobroutes.utils.toVec3
import org.lwjgl.input.Keyboard


object LeverAura: Module(
    name = "Lever Aura",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "does levers (duh)"
) {
    class Lever (val coords: BlockPos, var lastClick: Long)

    private val range by NumberSetting(name = "range", description = "how much reach the aura should have", min = 5f, max = 6.5f, default = 6f, increment = 0.1f)
    private val cooldown by NumberSetting(name = "cooldown", description = "how long to wait between presses", min = 0.1, max = 20, default = 10, unit = "s", increment = 0.1)


    private val levers = listOf(
        Lever(BlockPos(106, 124, 113), System.currentTimeMillis()),
        Lever(BlockPos(94, 124, 113), System.currentTimeMillis()),
        Lever(BlockPos(23, 132, 138), System.currentTimeMillis()),
        Lever(BlockPos(27, 124, 127), System.currentTimeMillis()),
        Lever(BlockPos(2, 122, 55), System.currentTimeMillis()),
        Lever(BlockPos(14, 122, 55), System.currentTimeMillis()),
        Lever(BlockPos(84, 121, 34), System.currentTimeMillis()),
        Lever(BlockPos(86, 128, 46), System.currentTimeMillis()),
        Lever(BlockPos(62, 133, 142), System.currentTimeMillis()),
        Lever(BlockPos(62, 136, 142), System.currentTimeMillis()),
        Lever(BlockPos(60, 135, 142), System.currentTimeMillis()),
        Lever(BlockPos(60, 134, 142), System.currentTimeMillis()),
        Lever(BlockPos(58, 136, 142), System.currentTimeMillis()),
        Lever(BlockPos(58, 133, 142), System.currentTimeMillis())
    )

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.isNotStart || !inF7Boss) return
        val eyePos = mc.thePlayer.getPositionEyes(0f)

        for (lever in levers) {
            if (eyePos.distanceTo(lever.coords.toVec3()) > range) continue
            if (System.currentTimeMillis() - lever.lastClick < cooldown * 1000) continue
            AuraManager.clickBlock(AuraManager.BlockAura(lever.coords, false) {})
            lever.lastClick = System.currentTimeMillis()
            return
        }
    }
}