package com.github.wadey3636.noobroutes.features.misc

import com.github.wadey3636.noobroutes.utils.AuraManager
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.NumberSetting
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

object AuraTest : Module("Aura Test", category = Category.MISC, description = "") {

    private val reach by NumberSetting("Reach", 4.5, 1, 6, 0.1, description = "Reach")
    private val cooldown by NumberSetting("Cooldown", 500, 50, 10000, 50, description = "")

    private var lastClick = 0L
    @SubscribeEvent
    fun testAura(event: ClientTickEvent){
        if (event.phase != TickEvent.Phase.START || System.currentTimeMillis() - lastClick < cooldown || mc.thePlayer == null) return
        if (mc.thePlayer.getPositionEyes(0f).distanceTo(Vec3(-9.0, 84.0, -87.0)) <= reach) {

            AuraManager.auraBlock(BlockPos(-9, 84, -87))
            lastClick = System.currentTimeMillis()
        }


    }





}