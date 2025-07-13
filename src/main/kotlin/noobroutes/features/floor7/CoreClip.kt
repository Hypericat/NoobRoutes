package noobroutes.features.floor7

import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.BossEventDispatcher
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.isClose
import noobroutes.utils.Utils.lastPlayerPos
import noobroutes.utils.Utils.lastPlayerSpeed
import org.lwjgl.input.Keyboard
import kotlin.math.abs


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

    private var clipTo = 0.0
    private var skip = true

    @SubscribeEvent
    fun afterMoveEntityWithHeading(event: MoveEntityWithHeadingEvent.Post) {
        if (mc.thePlayer == null || !BossEventDispatcher.inF7Boss) return
        if (skip == true) {
            skip = false
            return
        }
        if (mc.thePlayer.posY != CORE_Y) return
        if (mc.thePlayer.posX !in CORE_MIN_X..CORE_MAX_X) return

        if (isClose(mc.thePlayer.posZ, CORE_Z_EDGE1)) { clipShit(CORE_Z_EDGE1 + MAX_INSIDE_BLOCK) }
        else if (isClose(mc.thePlayer.posZ, CORE_Z_EDGE2)){ clipShit(CORE_Z_EDGE2 - MAX_INSIDE_BLOCK) }
    }

    fun clipShit(spot: Double) {
        skip = true
        mc.thePlayer.setPosition(lastPlayerPos.xCoord, lastPlayerPos.yCoord, lastPlayerPos.zCoord)
        mc.thePlayer.setVelocity(lastPlayerSpeed.xCoord, lastPlayerSpeed.yCoord, lastPlayerSpeed.zCoord)

        Blocks.gold_block.setBlockBounds(-1f,-1f,-1f,-1f,-1f,-1f)
        Blocks.barrier.setBlockBounds(-1f,-1f,-1f,-1f,-1f,-1f)

        mc.thePlayer.moveEntityWithHeading(mc.thePlayer.moveStrafing, mc.thePlayer.moveForward)

        clipTo = spot
    }

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if (clipTo == 0.0 || event.packet !is C03PacketPlayer) return
        event.isCanceled = true
        val z = clipTo
        clipTo = 0.0

        Scheduler.schedulePreTickTask(3) {
            Blocks.gold_block.setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f)
            Blocks.barrier.setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f)
        }

        if (event.packet.rotating) {
            PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.positionX, event.packet.positionY, z, event.packet.yaw, event.packet.pitch, event.packet.isOnGround))
        }
        else PacketUtils.sendPacket(C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, z, event.packet.isOnGround))
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun noInWall(event: PacketEvent.Send) {
        if (event.packet !is C03PacketPlayer || !BossEventDispatcher.inF7Boss) return

        if (event.packet.positionY != CORE_Y) return
        if (event.packet.positionX !in CORE_MIN_X..CORE_MAX_X) return

        val noNoSpot1 = CORE_Z_EDGE1 + MAX_INSIDE_BLOCK
        val noNoSpot2 = CORE_Z_EDGE2 - MAX_INSIDE_BLOCK

        if (event.packet.positionZ !in CORE_Z_EDGE1 ..CORE_Z_EDGE2 ||
            event.packet.positionZ == noNoSpot1 ||
            event.packet.positionZ == noNoSpot2) return

        event.isCanceled = true

        val goatedZ = listOf(noNoSpot1, noNoSpot2).minBy { abs(it - event.packet.positionZ) }

        if (event.packet.rotating) {
            PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.positionX, event.packet.positionY, goatedZ, event.packet.yaw, event.packet.pitch, event.packet.isOnGround))
        }
        else {
            PacketUtils.sendPacket(C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, goatedZ, event.packet.isOnGround))
        }
    }
}