package noobroutes.features.move

import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.utils.PacketUtils
import noobroutes.utils.Utils
import noobroutes.utils.Utils.lastPlayerPos
import noobroutes.utils.Utils.lastPlayerSpeed
import noobroutes.utils.toBlockPos
import org.lwjgl.input.Keyboard

object BarPhase: Module(
    name = "Bar Phase",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "fucking take a guess"
) {

    private var skip = false
    private var xToChange = 0.0
    private var zToChange = 0.0

    private const val COLLIDED_WITH_IRON_BAR_DECIMAL_LOW = 0.1375
    private const val COLLIDED_WITH_IRON_BAR_DECIMAL_HIGH = 0.8625


    @SubscribeEvent
    fun doShit(event: MoveEntityWithHeadingEvent) {
        if (mc.thePlayer == null || !mc.thePlayer.onGround || mc.thePlayer.isSneaking) return
        if (skip) {
            skip = false
            return
        }
        val playerPosVector = mc.thePlayer.positionVector.toBlockPos()


        if (
            mc.theWorld.getBlockState(playerPosVector.up()).block != Blocks.iron_bars &&
            (mc.theWorld.getBlockState(playerPosVector).block != Blocks.iron_bars || mc.theWorld.isAirBlock(playerPosVector.up(2)))
        ) return

        val decX = getDecimal(mc.thePlayer.posX)
        val decZ = getDecimal(mc.thePlayer.posZ)


        var xOffset = 0
        var zOffset = 0

        when {
            Utils.isClose(decX, COLLIDED_WITH_IRON_BAR_DECIMAL_LOW) -> xOffset++
            Utils.isClose(decX, COLLIDED_WITH_IRON_BAR_DECIMAL_HIGH) -> xOffset--
            Utils.isClose(decZ, COLLIDED_WITH_IRON_BAR_DECIMAL_LOW) -> zOffset++
            Utils.isClose(decZ, COLLIDED_WITH_IRON_BAR_DECIMAL_HIGH) -> zOffset--
            else -> return
        }

        skip = true

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

        for ((i, block) in blocks.withIndex()) {
            mc.theWorld.setBlockState(positions[i], block)
        }
    }

    private fun getDecimal(number: Double): Double {
        var decimal = number - number.toInt()
        if (decimal < 0) decimal = 1 + decimal
        return decimal
    }

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