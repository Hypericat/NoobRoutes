package noobroutes.features.move

import net.minecraft.client.renderer.EnumFaceDirection
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.MoveEntityWithHeadingEventPost
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils
import noobroutes.utils.toBlockPos
import org.lwjgl.input.Keyboard

object BarPhase: Module(
    name = "Bar Phase",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "fucking take a guess"
) {

    private var lastPlayerPos = Vec3(0.0, 0.0, 0.0)
    private var lastPlayerSpeed = Vec3(0.0, 0.0, 0.0)

    @SubscribeEvent
    fun onMotionPost(event: MotionUpdateEvent.Pre) {
        lastPlayerPos = Vec3(event.x, event.y, event.z)
        lastPlayerSpeed = Vec3(event.motionX, event.motionY, event.motionZ)
    }

    @SubscribeEvent
    fun doShit(event: MoveEntityWithHeadingEventPost) {
        if (mc.thePlayer == null || !mc.thePlayer.onGround || mc.thePlayer.isSneaking) return
        val playerPosVector = mc.thePlayer.positionVector.toBlockPos()
        if (mc.theWorld.getBlockState(playerPosVector).block != Blocks.iron_bars && mc.theWorld.getBlockState(playerPosVector.up()).block != Blocks.iron_bars) return

        val decX = getDecimal(mc.thePlayer.posX)
        val decZ = getDecimal(mc.thePlayer.posZ)


        var xOffset = 0
        var zOffset = 0

        val dir = when {
            Utils.isClose(decX, 0.1375) -> {
                EnumFaceDirection.EAST
                xOffset++
            }
            Utils.isClose(decX, 0.8625) -> {
                EnumFaceDirection.WEST
                xOffset--
            }
            Utils.isClose(decZ, 0.1375) -> {
                EnumFaceDirection.SOUTH
                zOffset++
            }
            Utils.isClose(decZ, 0.8625) -> {
                EnumFaceDirection.NORTH
                zOffset--
            }
            else -> null
        } ?: return

        val positions = listOf(
           playerPosVector,
           playerPosVector.up(),
           playerPosVector.add(zOffset, 0, -xOffset),
           playerPosVector.add(zOffset, 1, -xOffset),
           playerPosVector.add(-zOffset, 0, xOffset),
           playerPosVector.add(-zOffset, 1, xOffset),
        )

        val blocks = positions.map {
            mc.theWorld.getBlockState(it)
        }

        positions.forEach {
            if (mc.theWorld.getBlockState(it).block == Blocks.iron_bars) mc.theWorld.setBlockToAir(it)
        }

        changePos(mc.thePlayer.posX + xOffset * 0.0625, mc.thePlayer.posZ + zOffset * 0.0625)

        mc.thePlayer.setPosition(lastPlayerPos.xCoord, lastPlayerPos.yCoord, lastPlayerPos.zCoord)
        mc.thePlayer.setVelocity(lastPlayerSpeed.xCoord, lastPlayerSpeed.yCoord, lastPlayerSpeed.zCoord)

        mc.thePlayer.moveEntityWithHeading(mc.thePlayer.moveStrafing, mc.thePlayer.moveForward)

        Scheduler.scheduleC03Task(1) {
            for ((i, block) in blocks.withIndex()) {
                mc.theWorld.setBlockState(positions[i], block)
            }
        }
    }

    private fun getDecimal(number: Double): Double {
        var decimal = number - number.toInt()
        if (decimal < 0) decimal = 1 + decimal
        return decimal
    }

    private var xToChange = 0.0
    private var zToChange = 0.0

    private fun changePos(x: Double, z: Double) {
        xToChange = x
        zToChange = z
    }

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if ((xToChange == 0.0 && zToChange == 0.0) || event.packet !is C03PacketPlayer) return
        event.isCanceled = true
        val x = xToChange
        val z = zToChange
        xToChange = 0.0
        zToChange = 0.0
        if (event.packet.rotating) {
            PacketUtils.sendPacket(C06PacketPlayerPosLook(x, event.packet.positionY, z, event.packet.yaw, event.packet.pitch, event.packet.isOnGround))
        }
        else PacketUtils.sendPacket(C04PacketPlayerPosition(x, event.packet.positionY, z, event.packet.isOnGround))
    }
}