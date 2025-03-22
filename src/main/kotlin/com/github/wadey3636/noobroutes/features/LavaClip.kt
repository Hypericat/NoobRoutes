package com.github.wadey3636.noobroutes.features


import com.github.wadey3636.noobroutes.utils.ClientUtils
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.NumberSetting
import me.defnotstolen.utils.render.Color
import me.defnotstolen.utils.render.TextAlign
import me.defnotstolen.utils.render.text
import me.defnotstolen.utils.runOnMCThread
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard

object LavaClip: Module(
    name = "Lava Clip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "clips u down when entering lava"
)  {
    private val lavaDistance by NumberSetting(name = "Lava Clip distance", description = "how far to clip u", min = 10f, max = 50f, default = 30f)

    private var cancelS12 = false

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (mc.thePlayer == null) return
        if (event.phase != TickEvent.Phase.START) return

        if (mc.thePlayer.isInLava) ClientUtils.clientScheduleTask {
            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - lavaDistance, mc.thePlayer.posZ)
            cancelS12 = true
            }
    }

    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent) {
        val resolution = ScaledResolution(mc)
        text("Lava CLipping $lavaDistance", resolution.scaledWidth / 2, resolution.scaledHeight / 2.5, Color.RED, 13, align = TextAlign.Middle)
    }

    @SubscribeEvent
    fun onS12(event: PacketEvent.Receive) {
        if (!cancelS12 || event.packet !is S12PacketEntityVelocity) return
        event.isCanceled = true
        cancelS12 = false
        toggle()
        onDisable()
    }
}