package noobroutes.features.floor7

import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.MoveEntityWithHeadingEventPost
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.utils.PacketUtils
import noobroutes.utils.Utils.isClose
import noobroutes.utils.skyblock.devMessage
import org.lwjgl.input.Keyboard


object CoreClip: Module(
    name = "CoreClip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "clips u through the gold blocks into core"
) {

    private var lastPlayerPos = Vec3(0.0, 0.0, 0.0)
    private var lastPlayerSpeed = Vec3(0.0, 0.0, 0.0)
    private var clipTo = 0.0

    @SubscribeEvent
    fun onMotionPost(event: MotionUpdateEvent.Pre) {
        lastPlayerPos = Vec3(event.x, event.y, event.z)
        lastPlayerSpeed = Vec3(event.motionX, event.motionY, event.motionZ)
    }

    @SubscribeEvent
    fun onMotionPre(event: MoveEntityWithHeadingEventPost) {
        if (mc.thePlayer == null || mc.thePlayer.isSneaking) return
        if (mc.thePlayer.posY != 115.0) return
        if (mc.thePlayer.posX !in 52.0..57.0) return

        if (isClose(mc.thePlayer.posZ, 53.7)) { clipShit(53.7624) }
        else if (isClose(mc.thePlayer.posZ, 55.3)){ clipShit(55.2376) }
    }

    fun clipShit(spot: Double) {
        val dir = if (spot == 53.7624) 1 else -1
        mc.thePlayer.setPosition(lastPlayerPos.xCoord, lastPlayerPos.yCoord, lastPlayerPos.zCoord)
        mc.thePlayer.setVelocity(0.0, lastPlayerSpeed.yCoord, 1.403 * 0.546 * dir)

        Blocks.gold_block.setBlockBounds(-1f,-1f,-1f,-1f,-1f,-1f)

        val prevRot = mc.thePlayer.rotationYaw
        mc.thePlayer.rotationYaw = if (spot == 53.7624) 0f else 180f //make game think player is walking straight at door

        mc.thePlayer.moveEntityWithHeading(0f, 1f) //player must walk forward

        if (mc.thePlayer.posZ + 1.4 * dir in 53.65..55.35) {
            mc.thePlayer.moveEntityWithHeading(0f, 1f)
            devMessage("far")
        }

        mc.thePlayer.rotationYaw = prevRot

        clipTo = spot
    }

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if (clipTo == 0.0 || event.packet !is C03PacketPlayer) return
        event.isCanceled = true
        val z = clipTo
        clipTo = 0.0
        Blocks.gold_block.setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f)
        if (event.packet.rotating) {
            PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.positionX, event.packet.positionY, z, event.packet.yaw, event.packet.pitch, event.packet.isOnGround))
        }
        else PacketUtils.sendPacket(C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, z, event.packet.isOnGround))
    }
}