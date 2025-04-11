package com.github.wadey3636.noobroutes.utils

import com.github.wadey3636.noobroutes.utils.BlockUtils.collisionRayTrace
import me.defnotstolen.Core.logger
import me.defnotstolen.Core.mc
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.utils.skyblock.devMessage
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

object AuraManager {
    private val queuedBlocks: MutableList<BlockPos> = mutableListOf()
    private var clickCooldown: Int = 0

    fun auraBlock(pos: BlockPos){
        if (clickCooldown > 0) {
            queuedBlocks.add(pos)
            return
        }
        clickBlock(pos)


    }

    @SubscribeEvent
    fun auraQueue(event: ClientTickEvent){
        if (event.phase != TickEvent.Phase.START) return

        if (clickCooldown > 0) clickCooldown--

        val block = queuedBlocks.firstOrNull() ?: return
        clickBlock(block, true)

    }

    @SubscribeEvent
    fun packetCooldown(event: PacketEvent.Send){
        if (event.packet is C08PacketPlayerBlockPlacement) clickCooldown = 1
    }


    private fun clickBlock(block: BlockPos, removeFirst: Boolean = false){
        val aabb = BlockUtils.getAABB(block)
        if (aabb == null) {
            devMessage("Invalid Aura Block")
            logger.info("Invalid Aura Block")
            if (removeFirst) queuedBlocks.removeFirst()
            return

        }
        val centerPos = Vec3(block).addVector(
            (aabb.minX + aabb.maxX) / 2,
            (aabb.minY + aabb.maxY) / 2,
            (aabb.minZ + aabb.maxZ) / 2
        )
        val movingObjectPosition: MovingObjectPosition = collisionRayTrace(block, aabb, mc.thePlayer.getPositionEyes(0f), centerPos) ?: return
        PacketUtils.sendPacket(
            C08PacketPlayerBlockPlacement(
                block,
                movingObjectPosition.sideHit.index,
                mc.thePlayer.heldItem,
                movingObjectPosition.hitVec.xCoord.toFloat(),
                movingObjectPosition.hitVec.yCoord.toFloat(),
                movingObjectPosition.hitVec.zCoord.toFloat()
            )
        )
        if (removeFirst) queuedBlocks.removeFirst()
        devMessage(System.currentTimeMillis())
    }



}