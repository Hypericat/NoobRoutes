package noobroutes.features.floor7

import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.BossEventDispatcher
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.utils.BlockUtils.removeHitbox
import noobroutes.utils.BlockUtils.restoreHitbox
import noobroutes.utils.PacketUtils
import noobroutes.utils.isAir
import org.lwjgl.input.Keyboard
import kotlin.math.abs


object CoreClip: Module(
    name = "CoreClip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "The cleanest CoreClip™ in the west, east, north, and south."
) {
    private const val CORE_Y = 115.0

    private const val CORE_Z_EDGE1 = 53.7
    private const val CORE_Z_EDGE2 = 55.3

    private const val MAX_INSIDE_BLOCK = 0.0624

    private const val SLIGHTLY_IN1 = CORE_Z_EDGE1 + MAX_INSIDE_BLOCK
    private const val SLIGHTLY_IN2 = CORE_Z_EDGE2 - MAX_INSIDE_BLOCK

    private val MIDDLE_CORE_BLOCK = BlockPos(54, 115, 54) //this like is a const trust

    private var inDoorTicks = 0

    private inline val doorOpen: Boolean
         get() = isAir(MIDDLE_CORE_BLOCK)

    fun inDoor(posZ: Double): Boolean {
        return posZ in CORE_Z_EDGE1 ..CORE_Z_EDGE2 && posZ != SLIGHTLY_IN1 && posZ != SLIGHTLY_IN2
    }

    @SubscribeEvent()
    fun noInWall(event: PacketEvent.Send) {
        if (event.packet !is C03PacketPlayer || !BossEventDispatcher.inF7Boss || doorOpen) return

        if (abs(MIDDLE_CORE_BLOCK.z - mc.thePlayer.posZ) > 5 || mc.thePlayer.isSneaking || !mc.thePlayer.onGround) {
            Blocks.gold_block.restoreHitbox()
            Blocks.barrier.restoreHitbox()
        }
        else {
            Blocks.gold_block.removeHitbox()
            Blocks.barrier.removeHitbox()
        }

        if (!event.packet.isMoving) return

        if (inDoor(event.packet.positionZ)) inDoorTicks++ else inDoorTicks = 0

        if (event.packet.positionY != CORE_Y || !inDoor(event.packet.positionZ)) return

        event.isCanceled = true

        val goatedZ = listOf(SLIGHTLY_IN1, SLIGHTLY_IN2).minBy { abs(it - if (inDoorTicks == 1) mc.thePlayer.prevPosZ else event.packet.positionZ) }

        if (event.packet.rotating) {
            PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.positionX, event.packet.positionY, goatedZ, event.packet.yaw, event.packet.pitch, event.packet.isOnGround))
        }
        else {
            PacketUtils.sendPacket(C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, goatedZ, event.packet.isOnGround))
        }
    }
}