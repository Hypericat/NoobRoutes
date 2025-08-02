package noobroutes.features.floor7

import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C0CPacketInput
import net.minecraft.network.play.server.S1BPacketEntityAttach
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.BossEventDispatcher.inBoss
import noobroutes.events.impl.ChatPacketEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.utils.PacketUtils
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard

object InstaMid: Module (
    name = "Insta Mid",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "teleports u back to where u were before necron picked u up (ideally mid)"
) {
    private val forceSneak by BooleanSetting("Force Sneak", description = "Makes the player unable to unsneak while insta mid is active")
    private var cancelling = false
    private var sent = false

    private var forceSneakingActive = false

    private val MID_AABB = AxisAlignedBB(46.5, 64.0, 68.5, 63.5, 100.0, 84.5)

    //based on cga (pretty much stolen)
    @SubscribeEvent
    fun onSend(event: PacketEvent.Send)  {
        if (!cancelling || (event.packet !is C03PacketPlayer && event.packet !is C0CPacketInput)) return
        event.isCanceled = true
        if (mc.thePlayer.isRiding) return
        if (!sent) {
            cancelling = false
            sent = true
            PacketUtils.sendPacket(C06PacketPlayerPosLook(54.0, 65.0, 76.0, 0F, 0F, false))
            forceSneakingActive = false
            PlayerUtils.resyncSneak()
        }

    }
    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent){
        if (forceSneak && forceSneakingActive) PlayerUtils.sneak()
    }

    @SubscribeEvent
    fun onS1B(event: PacketEvent.Receive) {
        if (event.packet !is S1BPacketEntityAttach || event.packet.entityId != mc.thePlayer.entityId || event.packet.vehicleEntityId < 0 || !inBoss || !MID_AABB.isVecInside(mc.thePlayer.positionVector)) return
        cancelling = true
        sent = false
        modMessage("instamid")
    }

    @SubscribeEvent
    fun onChat(event: ChatPacketEvent) {

        if (event.message == "[BOSS] Necron: You went further than any human before, congratulations.") {
            forceSneakingActive = true
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, true)
        }
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) { //incase leave during pickup stage
        forceSneakingActive = false
        sent = false
        cancelling = false
        forceSneakingActive = false
    }
}