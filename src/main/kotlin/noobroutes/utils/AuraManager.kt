package noobroutes.utils

import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.utils.BlockUtils.collisionRayTrace

object AuraManager {
    class EntityAura(val entity: Entity, val action: Action)
    class BlockAura(val pos: BlockPos, val force: Boolean, val callback: () -> Unit)

    private val queuedBlocks: MutableList<BlockAura> = mutableListOf()
    private var clickBlockCooldown: Int = 0


    private val queuedEntityClicks = mutableListOf<EntityAura>()
    private var clickEntityCooldown = 0

    fun auraBlock(pos: BlockPos, force: Boolean = false, callback: () -> Unit = {}) {
        val blockAura = BlockAura(pos, force, callback)
        if (clickBlockCooldown > 0) {
            queuedBlocks.add(blockAura)
        } else {
            clickBlock(blockAura)
        }
    }

    fun auraBlock(x: Int, y: Int, z: Int, force: Boolean = false, callback: () -> Unit = {}) {
        auraBlock(BlockPos(x, y, z), force, callback)
    }

    fun auraEntity(entity: Entity, action: Action) {
        val entityAura = EntityAura(entity, action)
        if (clickEntityCooldown > 0) {
            queuedEntityClicks.add(entityAura)
        } else {
            clickEntity(entityAura)
        }
    }


    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        queuedBlocks.clear()
        queuedEntityClicks.clear()
        clickBlockCooldown = 20
        clickEntityCooldown = 20
    }


    @SubscribeEvent
    fun auraQueue(event: ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END) {
            if (clickBlockCooldown > 0) clickBlockCooldown--
            if (clickEntityCooldown > 0) clickEntityCooldown--
            return
        }

        if (event.phase != TickEvent.Phase.START) return

        if (clickBlockCooldown == 0) {
            queuedBlocks.firstOrNull()?.let { block ->
                clickBlock(block, true)
            }
        }
        if (clickEntityCooldown == 0) {
            queuedEntityClicks.firstOrNull()?.let { entity ->
                clickEntity(entity, true)
            }
        }
    }

    @SubscribeEvent
    fun packetCooldown(event: PacketEvent.Send) {
        when (event.packet) {
            is C08PacketPlayerBlockPlacement -> clickBlockCooldown = 1
            is C02PacketUseEntity -> clickEntityCooldown = 1
        }
    }


    fun clickBlock(aura: BlockAura, removeFirst: Boolean = false) {
        var aabb = BlockUtils.getAABB(aura.pos)
        if (aura.force && aabb == null) {
            aabb = AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            if (isAir(aura.pos)) aabb = null
        }
        if (aabb == null) {
            aura.callback()
            if (removeFirst) queuedBlocks.removeFirst()
            return

        }
        val centerPos = Vec3(aura.pos).addVector(
            (aabb.minX + aabb.maxX) * 0.5,
            (aabb.minY + aabb.maxY) * 0.5,
            (aabb.minZ + aabb.maxZ) * 0.5
        )
        val movingObjectPosition: MovingObjectPosition =
            collisionRayTrace(aura.pos, aabb, mc.thePlayer.getPositionEyes(0f), centerPos) ?: return
        PacketUtils.sendPacket(
            C08PacketPlayerBlockPlacement(
                aura.pos,
                movingObjectPosition.sideHit.index,
                mc.thePlayer.heldItem,
                movingObjectPosition.hitVec.xCoord.toFloat(),
                movingObjectPosition.hitVec.yCoord.toFloat(),
                movingObjectPosition.hitVec.zCoord.toFloat()
            )
        )
        if (removeFirst) queuedBlocks.removeFirst()
    }

    /**
     * Modified from CGA
     * https://github.com/WompWatr/CatgirlAddons
     */
    fun clickEntity(entityAura: EntityAura, removeFirst: Boolean = false) {
        //if (isZeroTickSwapping()) return
        if (removeFirst) queuedEntityClicks.removeFirst()
        if (mc.thePlayer.getPositionEyes(0f)
                .distanceTo(Vec3(entityAura.entity.posX, entityAura.entity.posY, entityAura.entity.posZ)) < 5
        ) {
            if (entityAura.action == Action.INTERACT) {
                PacketUtils.sendPacket(C02PacketUseEntity(entityAura.entity, Action.INTERACT))
            } else if (entityAura.action == Action.INTERACT_AT) {
                val expandValue: Double = entityAura.entity.collisionBorderSize.toDouble()
                val eyePos = mc.thePlayer.getPositionEyes(0f)
                val movingObjectPosition =
                    entityAura.entity.entityBoundingBox.expand(expandValue, expandValue, expandValue)
                        .calculateIntercept(eyePos, getEntityCenter(entityAura.entity)) ?: return
                PacketUtils.sendPacket(
                    C02PacketUseEntity(
                        entityAura.entity,
                        Vec3(
                            movingObjectPosition.hitVec.xCoord,
                            movingObjectPosition.hitVec.yCoord,
                            movingObjectPosition.hitVec.zCoord
                        )
                    )
                )
            }
            return
        }
    }


    /**
     * Taken from cga
     * https://github.com/WompWatr/CatgirlAddons
     * */
    private fun getEntityCenter(entity: Entity): Vec3 {
        val boundingBox = entity.entityBoundingBox
        val centerX = (boundingBox.minX + boundingBox.maxX) * 0.5
        val centerY = (boundingBox.minY + boundingBox.maxY) * 0.5
        val centerZ = (boundingBox.minZ + boundingBox.maxZ) * 0.5
        return Vec3(centerX, centerY, centerZ)
    }
}