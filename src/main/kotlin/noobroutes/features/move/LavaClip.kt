package noobroutes.features.move


import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.NotPersistent
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.Scheduler
import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.text
import org.lwjgl.input.Keyboard

@NotPersistent
object LavaClip: Module(
    name = "Lava Clip",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "clips u down when entering lava"
)  {
    private val lavaDistance by NumberSetting(name = "Lava Clip distance", description = "how far to clip u", min = 10f, max = 50f, default = 30f)

    private var cancelS12 = false
    var ringClip: Double? = null

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (mc.thePlayer == null) return
        if (event.phase != TickEvent.Phase.END) return

        if (mc.thePlayer.isInLava) {
            cancelS12 = true
            val clipDistance = if (ringClip != null) ringClip else lavaDistance.toDouble()
            if (clipDistance == null) return
            Scheduler.schedulePreTickTask {mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - clipDistance, mc.thePlayer.posZ)}
        }
    }

    @SubscribeEvent
    fun onOverlay(event: RenderGameOverlayEvent.Post) {
        val clipDistance = if (ringClip != null) ringClip else lavaDistance.toDouble()
        if (clipDistance == null) return
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        val resolution = ScaledResolution(mc)
        text("Lava CLipping ${clipDistance.toInt()}", resolution.scaledWidth / 2, resolution.scaledHeight / 2.5, Color.RED, 13, align = TextAlign.Middle)
    }

    @SubscribeEvent
    fun onS12(event: PacketEvent.Receive) {
        if (!cancelS12 || event.packet !is S12PacketEntityVelocity || (event.packet.motionY != 28000 && event.packet.motionY != 26000) || event.packet.entityID != mc.thePlayer.entityId) return
        event.isCanceled = true
        cancelS12 = false
        toggle()
    }

    override fun onDisable() {
        ringClip = null
        super.onDisable()
    }
}