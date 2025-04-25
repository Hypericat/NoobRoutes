package com.github.wadey3636.noobroutes.features.floor7

import com.github.wadey3636.noobroutes.utils.Scheduler
import me.noobmodcore.events.impl.PacketEvent
import me.noobmodcore.features.Category
import me.noobmodcore.features.Module
import me.noobmodcore.features.settings.impl.NumberSetting
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object StormClip: Module(
    name = "Storm Clip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "clips u down when entering f7/m7 boss"
)  {
    private val clipDistance by NumberSetting(name = "Storm Clip distance", description = "how far to clip u", min = 30f, max = 80f, default = 40f)
    private val delayTicks by NumberSetting(name = "Storm Clip delay", description = "testing option", min = 0, max = 3, default = 1)

    private var has = false

    @SubscribeEvent
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || has ) return
        if (event.packet.x == 73.5 && event.packet.y == 221.5 && event.packet.z == 14.5) Scheduler.schedulePreTickTask(
            delayTicks
        ) {
            mc.thePlayer.setPosition(73.5, 221.5 - clipDistance, 14.5)
            has = true
        }
    }

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        has = false
    }
}