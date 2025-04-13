package com.github.wadey3636.noobroutes.features.floor7

import com.github.wadey3636.noobroutes.features.Blink
import com.github.wadey3636.noobroutes.utils.AutoP3Utils
import com.github.wadey3636.noobroutes.utils.ClientUtils
import com.github.wadey3636.noobroutes.utils.PacketUtils
import com.github.wadey3636.noobroutes.utils.RotationUtils.getYawAndPitch
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.Setting.Companion.withDependency
import me.defnotstolen.features.settings.impl.BooleanSetting
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard

object Auto4: Module(
    name = "Auto (I)4",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "does 4th device"
) {
    private val silent by BooleanSetting("silent", true, description = "snaps only serverside")
    private val shit by BooleanSetting("code stuff", false, description = "off is wadey, on is noob").withDependency { silent && !speedyBoi }
    private val speedyBoi by BooleanSetting("speedyBoi", false, description = "does shit faster").withDependency { silent }


    private val devBlocks = listOf(
        BlockPos(64, 126, 50),
        BlockPos(66, 126, 50),
        BlockPos(68, 126, 50),
        BlockPos(64, 128, 50),
        BlockPos(66, 128, 50),
        BlockPos(68, 128, 50),
        BlockPos(64, 130, 50),
        BlockPos(66, 130, 50),
        BlockPos(68, 130, 50)
    )

    private var shotBlocks = mutableListOf<BlockPos>()

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (mc.thePlayer == null) return
        if (mc.thePlayer.getDistance(63.5, 127.0, 35.5) > 1.5 || mc.thePlayer.heldItem?.item != Items.bow) {
            shotBlocks = mutableListOf<BlockPos>()
            return
        }
        if (event.packet is S23PacketBlockChange && devBlocks.contains(event.packet.blockPosition) && event.packet.blockState.block == Blocks.emerald_block) shoot(event.packet.blockPosition)
        else if (event.packet is S22PacketMultiBlockChange) {
            event.packet.changedBlocks.forEach {block -> if (devBlocks.contains(block.pos) && block.blockState.block == Blocks.emerald_block) shoot(block.pos) }
        }

    }

    fun shoot(block: BlockPos) {
        val rotation = getRotation(block)
        if (!silent || Blink.cancelled < 1) {
            mc.thePlayer.rotationYaw = rotation.first
            mc.thePlayer.rotationPitch = rotation.second
            ClientUtils.clientScheduleTask(1) { PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem)) }
        }
        else if (speedyBoi) {
            PacketUtils.sendPacket(C05PacketPlayerLook(rotation.first, rotation.second, mc.thePlayer.onGround))
            Blink.cancelled--
            PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
        }
        else if (!shit){
            ClientUtils.clientScheduleTask {
                PacketUtils.sendPacket(C05PacketPlayerLook(rotation.first, rotation.second, mc.thePlayer.onGround))
                Blink.cancelled--
                PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
            }
        }
        else {
            PacketUtils.c03ScheduleTask(true) {
                PacketUtils.sendPacket(C05PacketPlayerLook(rotation.first, rotation.second, mc.thePlayer.onGround))
                PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
            }
        }
        shotBlocks.add(block)
    }

    fun getRotation(block: BlockPos): Pair<Float, Float> {
        if (mc.thePlayer.heldItem.displayName.contains("Terminator")) {
            return when (block.x) {
                64 -> getYawAndPitch(65.5, block.y.toDouble() + 1.1, block.z.toDouble() + 0.5)
                68 -> getYawAndPitch(67.5, block.y.toDouble() + 1.1, block.z.toDouble() + 0.5)
                else -> if (shotBlocks.any { it.y == block.y && it.x == 64 }) getYawAndPitch(67.5, block.y.toDouble() + 1.1, block.z.toDouble() + 0.5) else getYawAndPitch(65.5, block.y.toDouble() + 1.1, block.z.toDouble() + 0.5)
            }
        }
        return getYawAndPitch(block.x.toDouble() + 0.5, block.y.toDouble() + 1.1, block.z.toDouble() + 0.5)
    }

    @SubscribeEvent
    fun onKey(event: InputEvent) {
        if (shotBlocks.size in 1..8) AutoP3Utils.unPressKeys()
    }
}