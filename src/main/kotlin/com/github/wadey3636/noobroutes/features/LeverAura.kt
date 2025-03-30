package com.github.wadey3636.noobroutes.features

import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.NumberSetting
import me.defnotstolen.utils.distanceSquaredTo
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import kotlin.math.pow

data class Lever (val coords: Vec3, var lastClick: Long)

object LeverAura: Module(
    name = "Lever Aura",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "does levers (duh)"
) {
    private val range by NumberSetting(name = "range", description = "how much reach the aura should have", min = 5f, max = 6.5f, default = 6f)
    private val cooldown by NumberSetting(name = "cooldown", description = "how long to wait beetween presses (miliseconds)", min = 50, max = 10000, default = 1000)


    private val levers = listOf<Lever>(
        Lever(Vec3(106.0, 124.0, 113.0), System.currentTimeMillis()),
        Lever(Vec3(94.0, 124.0, 113.0), System.currentTimeMillis()),
        Lever(Vec3(23.0, 132.0, 138.0), System.currentTimeMillis()),
        Lever(Vec3(27.0, 124.0, 127.0), System.currentTimeMillis()),
        Lever(Vec3(2.0, 122.0, 55.0), System.currentTimeMillis()),
        Lever(Vec3(14.0, 122.0, 55.0), System.currentTimeMillis()),
        Lever(Vec3(84.0, 121.0, 34.0), System.currentTimeMillis()),
        Lever(Vec3(86.0, 128.0, 46.0), System.currentTimeMillis()),
        Lever(Vec3(62.0, 133.0, 142.0), System.currentTimeMillis()),
        Lever(Vec3(62.0, 136.0, 142.0), System.currentTimeMillis()),
        Lever(Vec3(60.0, 135.0, 142.0), System.currentTimeMillis()),
        Lever(Vec3(60.0, 134.0, 142.0), System.currentTimeMillis()),
        Lever(Vec3(58.0, 136.0, 142.0), System.currentTimeMillis()),
        Lever(Vec3(58.0, 133.0, 142.0), System.currentTimeMillis())
    )

    @SubscribeEvent
    fun doShit(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !AutoP3.inBoss) return
        levers.forEach { lever ->
            if (mc.thePlayer.distanceSquaredTo(lever.coords.subtract(Vec3(0.0, mc.thePlayer.eyeHeight.toDouble(), 0.0))) > range.pow(2)) return
            if (System.currentTimeMillis() - lever.lastClick < cooldown) return

        }
    }
}