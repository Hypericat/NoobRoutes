package noobroutes.features.move

import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.isClose
import noobroutes.utils.getBlockAt
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.sendChatMessage
import noobroutes.utils.skyblock.sendCommand
import noobroutes.utils.toBlockPos
import org.lwjgl.input.Keyboard

object Doorless: Module(
    name = "Doorless",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "allows u to go through doors"
) {
    private val clipDistance by NumberSetting(name = "Clip Distance", description = "how far u clip", min = 0.0, max = 5.0, default = 4.0, increment = 0.1)

    private var doingShit = false
    private var clipped = false
    private var expectedX = 0.0
    private var expectedZ = 0.0
    private var dir = 0
    private var prevRot = Pair(0f, 0f)

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if ((event.packet !is C04PacketPlayerPosition && event.packet !is C06PacketPlayerPosLook) || doingShit) return
        if (mc.thePlayer.heldItem?.item != Items.ender_pearl) return
        val x = event.packet.positionX
        val y = event.packet.positionY
        val z = event.packet.positionZ
        if (y != 69.0 || x > 0 || z > 0 || x < -200 || z < -200) return
        val xDec = (x + 200) % 1
        val zDec = (z + 200) % 1
        val blockPosPlayer = Vec3(x,y,z).toBlockPos()

        val shouldDoShit = when {
            isClose(xDec, 0.3) && isDoorBlock(blockPosPlayer.subtract(Vec3i(1, 0, 0))) -> {
                dir = 0
                true
            }
            isClose(xDec, 0.7) && isDoorBlock(blockPosPlayer.add(Vec3i(1, 0, 0))) -> {
                dir = 1
                true
            }
            isClose(zDec, 0.3) && isDoorBlock(blockPosPlayer.subtract(Vec3i(0, 0, 1))) -> {
                dir = 2
                true
            }
            isClose(zDec, 0.7) && isDoorBlock(blockPosPlayer.add(Vec3i(0, 0, 1))) -> {
                dir = 3
                true
            }
            else -> false
        }
        if (!shouldDoShit) return

        doingShit = true
        event.isCanceled = true
        PacketUtils.sendPacket(C06PacketPlayerPosLook(x, y, z, 0f, -90f, event.packet.isOnGround))
        PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
        expectedX = blockPosPlayer.x + 0.5
        expectedZ = blockPosPlayer.z + 0.5
        prevRot = Pair(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)

        if (mc.isSingleplayer) {
            Scheduler.schedulePreTickTask(5) { sendChatMessage("/tp $expectedX ${blockPosPlayer.y + 2.0} $expectedZ") }
        }
    }

    @SubscribeEvent
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || !doingShit || clipped) return
        clipped = true
        AutoP3Utils.unPressKeys()
        Scheduler.schedulePreTickTask { clip() }
    }

    private fun clip() {
        val (dx, dz) = when (dir) {
            0 -> -1 to 0
            1 -> 1 to 0
            2 -> 0 to -1
            3 -> 0 to 1
            else -> return
        }
        mc.thePlayer.setPosition(
            mc.thePlayer.posX + dx * clipDistance,
            69.0,
            mc.thePlayer.posZ + dz * clipDistance
        )

        Blocks.coal_block.setBlockBounds(-1f,-1f,-1f,-1f,-1f,-1f)
        Blocks.stained_hardened_clay.setBlockBounds(-1f,-1f,-1f,-1f,-1f,-1f)
        mc.thePlayer.rotationYaw = prevRot.first
        mc.thePlayer.rotationPitch = prevRot.second
        Scheduler.schedulePreTickTask { AutoP3Utils.rePressKeys() }
        Scheduler.schedulePreTickTask(15) {
            doingShit = false
            clipped = false
            Blocks.coal_block.setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f)
            Blocks.stained_hardened_clay.setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f)
        }
    }

    private fun isDoorBlock(blockPosition: BlockPos): Boolean {
        val state = mc.theWorld.getBlockState(blockPosition)
        val block = state.block
        val meta = block.getMetaFromState(state)
        return block == Blocks.coal_block || (block == Blocks.stained_hardened_clay && meta == 14)
    }
}