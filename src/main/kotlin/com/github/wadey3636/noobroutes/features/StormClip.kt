package com.github.wadey3636.noobroutes.features

import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.NumberSetting
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard

object StormClip: Module(
    name = "Storm Clip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "clips u down when entering f7/m7 boss"
)  {
    private val clipDistance by NumberSetting(name = "Storm Clip distance", description = "how far to clip u", min = 30f, max = 80f, default = 40f)

    private var goIn: Int? = null
    private var has = false

    @SubscribeEvent
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || has) return
        if (event.packet.x == 73.5 && event.packet.y == 221.5 && event.packet.z == 14.5) goIn = 2
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (goIn == null) return
        if (event.phase != TickEvent.Phase.END) return
        goIn = goIn!! - 1
        if (goIn != 0) return
        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - clipDistance, mc.thePlayer.posZ)
        goIn = null
        has = true
    }

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        has = false
    }
}