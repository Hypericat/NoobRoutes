package noobroutes.features.move

import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.*
import noobroutes.utils.Utils.isClose
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.sendChatMessage
import org.lwjgl.input.Keyboard

object Doorless: Module(
    name = "Doorless",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "allows u to go through doors"
) {
    private val clipDistance by NumberSetting(name = "Clip Distance", description = "how far u clip", min = 0.0, max = 2, default = 0.5, increment = 0.1)
    private val faster by BooleanSetting("fast mode", default = false, description = "messes with packets (might ban)")
    private val allowChiseled by BooleanSetting("Chiseled Toggle", default = false, description = "Include chiseled stone bricks as door blocks")
    private val second by BooleanSetting("hclip", default = false, description = "hclips on second tick")
    private val clip2Distance by NumberSetting(name = "hclip distance", description = "how far u hclip", min = 0.0, max = 5.0, default = 4.0, increment = 0.1).withDependency { second }
    private val setSpeed by BooleanSetting(name = "Set Speed", description = "set the player to make speed first tick after clipping")


    private var doingShit = false
    private var clipped = false
    private var skip = false
    private var expectedX = 0.0
    private var expectedZ = 0.0
    private var dir = 0
    private var prevRot = Pair(0f, 0f)
    private var s08Pos = Pair(0.0, 0.0)

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
        devMessage(dir)
        doingShit = true
        event.isCanceled = true
        skip = true
        PacketUtils.sendPacket(C06PacketPlayerPosLook(x, y, z, getYaw(), getPitch(blockPosPlayer), event.packet.isOnGround))
        PlayerUtils.airClick()
        expectedX = blockPosPlayer.x + 0.5
        expectedZ = blockPosPlayer.z + 0.5
        if (!faster) prevRot = Pair(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)

        if (mc.isSingleplayer) {
            Scheduler.schedulePreTickTask(2) { sendChatMessage("/tp $expectedX ${blockPosPlayer.y + 2.0} $expectedZ") }
        }
    }

    @SubscribeEvent
    fun cancelC03(event: PacketEvent.Send) {
        if (!faster || !doingShit || event.packet !is C03PacketPlayer || clipped) return
        if (skip) {
            skip = false
            return
        }
        devMessage("cancelled packet")
        event.isCanceled = true
    }

    private fun getYaw(): Float {
        return when(dir) {
            0 -> 90f
            1 -> -90f
            2 -> -180f
            3 -> 0f
            else -> 0f
        }
    }

    private fun getPitch(orgPos: BlockPos): Float {
        return if (isAir(orgPos.add(0, 3, 0))) -79f else 0f
    }


    @SubscribeEvent
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || !doingShit || clipped) return
        s08Pos = Pair(event.packet.x, event.packet.z)
        devMessage(s08Pos)
        clipped = true
        AutoP3Utils.unPressKeys()
        if (!faster) {
            Scheduler.schedulePreTickTask { clip() }
            return
        }
        Scheduler.schedulePreTickTask(1) {
            val (dx, dz) = when (dir) {
                0 -> -1 to 0
                1 -> 1 to 0
                2 -> 0 to -1
                3 -> 0 to 1
                else -> return@schedulePreTickTask
            }
            clip2(dx, dz)
            if (!setSpeed) return@schedulePreTickTask
            val speed = Core.mc.thePlayer.aiMoveSpeed.toDouble()
            PlayerUtils.setMotion(
                dx * 2.806 * speed,
                dz * 2.806 * speed
            )
            PlayerUtils.unPressKeys()
            Scheduler.schedulePreTickTask { PlayerUtils.rePressKeys() }
        }
        event.isCanceled = true
        PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.x, event.packet.y, event.packet.z, event.packet.yaw, event.packet.pitch, false))
        devMessage("sent packet")
        clip()
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
            s08Pos.first + dx * clipDistance,
            69.0,
            s08Pos.second + dz * clipDistance
        )

        Blocks.coal_block.setBlockBounds(-1f,-1f,-1f,-1f,-1f,-1f)
        Blocks.stained_hardened_clay.setBlockBounds(-1f,-1f,-1f,-1f,-1f,-1f)
        Blocks.monster_egg.setBlockBounds(-1f, -1f, -1f, -1f, -1f, -1f)
        if (!faster) mc.thePlayer.rotationYaw = prevRot.first
        if (!faster) mc.thePlayer.rotationPitch = prevRot.second
        Scheduler.schedulePreTickTask { AutoP3Utils.rePressKeys() }
        Scheduler.schedulePreTickTask(15) {
            doingShit = false
            clipped = false
            Blocks.coal_block.setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f)
            Blocks.stained_hardened_clay.setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f)
            Blocks.monster_egg.setBlockBounds(0f, 0f, 0f, 1f, 1f, 1f)
        }
    }

    private fun clip2(dx: Int, dz: Int) {
        if (!second) return

        mc.thePlayer.setPosition(
            s08Pos.first + dx * (clip2Distance + clipDistance),
            69.0,
            s08Pos.second + dz * (clip2Distance + clipDistance)
        )

    }

    private fun isDoorBlock(blockPosition: BlockPos): Boolean {
        val state = mc.theWorld.getBlockState(blockPosition)
        val block = state.block
        val meta = block.getMetaFromState(state)
        return block == Blocks.coal_block || (block == Blocks.stained_hardened_clay && meta == 14) || (allowChiseled && block == Blocks.monster_egg && block.getMetaFromState(state) == 5)
    }
}