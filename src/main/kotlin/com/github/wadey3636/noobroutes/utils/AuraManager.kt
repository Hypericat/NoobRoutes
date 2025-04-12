package com.github.wadey3636.noobroutes.utils

import com.github.wadey3636.noobroutes.utils.BlockUtils.collisionRayTrace
import me.defnotstolen.Core.logger
import me.defnotstolen.Core.mc
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.utils.skyblock.devMessage
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

object AuraManager {
    class EntityAura(val entity: Entity, val action: C02PacketUseEntity.Action)
    private val queuedBlocks: MutableList<BlockPos> = mutableListOf()
    private var clickBlockCooldown: Int = 0


    private val queuedEntityClicks = mutableListOf<EntityAura>()
    private var clickEntityCooldown = 0

    /**
     * Sends C08PacketPlayerBlockPlacement packet to a specified block.
     * Queues the packet if one has already been sent during the tick.
     *
     * @param pos the BlockPos of the block to be clicked.
     */
    fun auraBlock(pos: BlockPos){
        if (clickBlockCooldown > 0) {
            queuedBlocks.add(pos)
            return
        }
        clickBlock(pos)
    }

    /**
     * Sends C02PacketUseEntity
     *
     * @param entity Entity
     * @param action C02PacketUseEntity Action Type
      */
    fun auraEntity(entity: Entity, action: Action){
        if (clickEntityCooldown > 0) {
            queuedEntityClicks.add(EntityAura(entity, action))
            return
        }
        clickEntity(EntityAura(entity, action))
    }




    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load){
        queuedBlocks.clear()
        queuedEntityClicks.clear()
        clickBlockCooldown = 20
        clickEntityCooldown = 20
    }


    @SubscribeEvent
    fun auraQueue(event: ClientTickEvent){
        if (event.phase != TickEvent.Phase.START) return

        if (clickBlockCooldown > 0) clickBlockCooldown--
        if (clickEntityCooldown > 0) clickEntityCooldown--

        if (clickBlockCooldown == 0) {
            val block = queuedBlocks.firstOrNull() ?: return
            clickBlock(block, true)
        }
        if (clickEntityCooldown == 0) {
            val entity = queuedEntityClicks.firstOrNull() ?: return
            clickEntity(entity, true)
        }


    }

    @SubscribeEvent
    fun packetCooldown(event: PacketEvent.Send) {
        when (event.packet) {
            is C08PacketPlayerBlockPlacement -> clickBlockCooldown = 1
            is C02PacketUseEntity -> clickEntityCooldown = 1
        }

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
        devMessage("sent c08")
        if (removeFirst) queuedBlocks.removeFirst()
        //devMessage(System.currentTimeMillis())
    }

    /**
     * Modified from CGA
     * https://github.com/WompWatr/CatgirlAddons
     */
    private fun clickEntity(entityAura: EntityAura, removeFirst: Boolean = false){
        if (removeFirst) queuedEntityClicks.removeFirst()
        if (mc.thePlayer.getPositionEyes(0f).distanceTo(Vec3(entityAura.entity.posX, entityAura.entity.posY, entityAura.entity.posZ)) < 5) {
            if (entityAura.action == Action.INTERACT) {
                devMessage("entity packet sent")
                PacketUtils.sendPacket(C02PacketUseEntity(entityAura.entity, Action.INTERACT))
            } else if (entityAura.action == Action.INTERACT_AT) {
                val expandValue: Double = entityAura.entity.collisionBorderSize.toDouble()
                val eyePos = mc.thePlayer.getPositionEyes(0f)
                val movingObjectPosition = entityAura.entity.entityBoundingBox.expand(expandValue, expandValue, expandValue).calculateIntercept(eyePos, getEntityCenter(entityAura.entity)) ?: return
                PacketUtils.sendPacket(C02PacketUseEntity(entityAura.entity, Vec3(movingObjectPosition.hitVec.xCoord, movingObjectPosition.hitVec.yCoord, movingObjectPosition.hitVec.zCoord)))
                devMessage("entity packet sent")
            }
            return
        }
    }


    /**
     * Taken from cga
     * https://github.com/WompWatr/CatgirlAddons
     */
    private fun getEntityCenter(entity: Entity): Vec3 {
        val boundingBox = entity.entityBoundingBox
        val centerX = (boundingBox.minX + boundingBox.maxX) / 2
        val centerY = (boundingBox.minY + boundingBox.maxY) / 2
        val centerZ = (boundingBox.minZ + boundingBox.maxZ) / 2
        return Vec3(centerX, centerY, centerZ)
    }




}