package noobroutes.features.floor7

import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.BossEventDispatcher
import noobroutes.events.EventDispatcher
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
    description = "The cleanest CoreClip™ in the west, east, north, and south."
) {

    private const val CORE_Y = 115.0
    private const val CORE_MIN_X = 52.0
    private const val CORE_MAX_X = 57.0

    private const val CORE_Z_EDGE1 = 53.7
    private const val CORE_Z_EDGE2 = 55.3

    private const val MAX_INSIDE_BLOCK = 0.0624

    private const val MAX_WALK_SPEED_FORWARD = 1.403
    private const val MAX_WALK_SPEED_BACKWARDS = 1.0
    private const val GROUND_DRAG = 0.546



    private var lastPlayerPos = Vec3(0.0, 0.0, 0.0)
    private var lastPlayerSpeed = Vec3(0.0, 0.0, 0.0)
    private var clipTo = 0.0
    private var skip = 0

    @SubscribeEvent
    fun onMotionPre(event: MotionUpdateEvent.Pre) {
        lastPlayerPos = Vec3(event.x, event.y, event.z)
        lastPlayerSpeed = Vec3(event.motionX, event.motionY, event.motionZ)
    }

    @SubscribeEvent
    fun afterMoveEntityWithHeading(event: MoveEntityWithHeadingEventPost) {
        if (mc.thePlayer == null || mc.thePlayer.isSneaking || !BossEventDispatcher.inF7Boss) return
        if (skip > 0) {
            skip--
            return
        }
        if (mc.thePlayer.posY != CORE_Y) return
        if (mc.thePlayer.posX !in CORE_MIN_X..CORE_MAX_X) return

        if (isClose(mc.thePlayer.posZ, CORE_Z_EDGE1)) { clipShit(CORE_Z_EDGE1 + MAX_INSIDE_BLOCK) }
        else if (isClose(mc.thePlayer.posZ, CORE_Z_EDGE2)){ clipShit(CORE_Z_EDGE2 - MAX_INSIDE_BLOCK) }
    }

    fun clipShit(spot: Double) {
        val dir = if (spot == CORE_Z_EDGE1 + MAX_INSIDE_BLOCK) 1 else -1
        skip = 2
        mc.thePlayer.setPosition(lastPlayerPos.xCoord, lastPlayerPos.yCoord, lastPlayerPos.zCoord)
        mc.thePlayer.setVelocity(0.0, lastPlayerSpeed.yCoord, MAX_WALK_SPEED_FORWARD * GROUND_DRAG * dir)

        Blocks.gold_block.setBlockBounds(-1f,-1f,-1f,-1f,-1f,-1f)
        Blocks.barrier.setBlockBounds(-1f,-1f,-1f,-1f,-1f,-1f)

        val prevRot = mc.thePlayer.rotationYaw
        mc.thePlayer.rotationYaw = if (dir == 1) 0f else 180f //make game think player is walking straight at door

        mc.thePlayer.moveEntityWithHeading(0f, 1f) //player must walk forward

        val maxWalkDistance = if (mc.thePlayer.moveForward == -1f) MAX_WALK_SPEED_BACKWARDS else MAX_WALK_SPEED_FORWARD

        if (mc.thePlayer.posZ + maxWalkDistance * dir in CORE_Z_EDGE1 - 0.05..CORE_Z_EDGE2 + 0.05) {
            mc.thePlayer.moveEntityWithHeading(0f, 1f) //walk again if he would still be in
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
        Blocks.barrier.setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f)
        if (event.packet.rotating) {
            PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.positionX, event.packet.positionY, z, event.packet.yaw, event.packet.pitch, event.packet.isOnGround))
        }
        else PacketUtils.sendPacket(C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, z, event.packet.isOnGround))
    }
}