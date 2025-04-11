package com.github.wadey3636.noobroutes.features.floor7

import com.github.wadey3636.noobroutes.utils.AuraManager
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.NumberSetting
import me.defnotstolen.utils.skyblock.devMessage
import me.defnotstolen.utils.toVec3
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard



object LeverAura: Module(
    name = "Lever Aura",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "does levers (duh)"
) {
    class Lever (val coords: BlockPos, var lastClick: Long)
    private val range by NumberSetting(name = "range", description = "how much reach the aura should have", min = 5f, max = 6.5f, default = 6f, increment = 0.1f)
    private val cooldown by NumberSetting(name = "cooldown", description = "how long to wait beetween presses (miliseconds)", min = 50, max = 10000, default = 1000)


    private val levers = listOf<Lever>(
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
        //Lever(BlockPos(210, 62, 226), System.currentTimeMillis())
    )

    @SubscribeEvent
    fun doShit(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !AutoP3.inBoss) return
        val eyePos = mc.thePlayer.getPositionEyes(0f)
        levers.forEach { lever ->
            if (eyePos.distanceTo(lever.coords.toVec3()) > range) return@forEach
            if (System.currentTimeMillis() - lever.lastClick < cooldown) return@forEach
            AuraManager.auraBlock(lever.coords)
            lever.lastClick = System.currentTimeMillis()
            return
        }
    }
}