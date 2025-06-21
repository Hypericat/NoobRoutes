package noobroutes.features.floor7

import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.PacketUtils
import noobroutes.utils.skyblock.devMessage
import org.lwjgl.input.Keyboard

object StormClip: Module(
    name = "Storm Clip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "clips u down when entering f7/m7 boss"
)  {
    private val clipDistance by NumberSetting(name = "Storm Clip distance", description = "how far to clip u", min = 30f, max = 80f, default = 40f)

    private var has = false

    @SubscribeEvent
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || has) return
        devMessage("so8 pos: x: ${event.packet.x}, y: ${event.packet.y}, z: ${event.packet.z}")
        if (event.packet.x == 73.5 && event.packet.y == 221.5 && event.packet.z == 14.5) {
            event.isCanceled = true
            PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.x, event.packet.y, event.packet.z, event.packet.yaw, event.packet.pitch, false))
            mc.thePlayer.setPosition(73.5, 221.5 - clipDistance, 14.5)
            has = true
        }
    }

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        has = false
    }
}