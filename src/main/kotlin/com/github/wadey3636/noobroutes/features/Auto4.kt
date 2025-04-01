package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.PacketUtils
import com.github.wadey3636.noobroutes.utils.Utils
import me.defnotstolen.Core
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.utils.skyblock.modMessage
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object Auto4: Module(
    name = "Auto (I)4",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "does 4th device"
) {

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

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (mc.thePlayer == null) return
        if (mc.thePlayer.getDistance(63.5, 127.0, 35.5) > 1.5 || mc.thePlayer.heldItem.item != Items.bow) return
        if (event.packet is S23PacketBlockChange && devBlocks.contains(event.packet.blockPosition) && event.packet.blockState.block == Blocks.emerald_block) shoot(event.packet.blockPosition)
        else if (event.packet is S22PacketMultiBlockChange) {
            event.packet.changedBlocks.forEach {block -> if (devBlocks.contains(block.pos) && block.blockState.block == Blocks.emerald_block) shoot(block.pos) }
        }

    }

    fun shoot(block: BlockPos) {
        if (Blink.cancelled ==  0) return
        val rotation = Utils.getYawAndPitch(block.x.toDouble(), block.y.toDouble() + 1.1, block.z.toDouble())
        PacketUtils.sendPacket(C05PacketPlayerLook(rotation.first, rotation.second, mc.thePlayer.onGround))
        PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
    }
}