package com.github.wadey3636.noobroutes.features.floor7

import com.github.wadey3636.noobroutes.utils.PacketUtils
import com.github.wadey3636.noobroutes.utils.Utils.getEntitiesOfType
import me.defnotstolen.features.Module
import me.defnotstolen.utils.skyblock.Island
import me.defnotstolen.utils.skyblock.LocationUtils
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import com.github.wadey3636.noobroutes.utils.Utils.getID
import me.defnotstolen.events.impl.ChatPacketEvent
import me.defnotstolen.features.Category
import net.minecraft.network.play.client.C02PacketUseEntity
import kotlin.math.pow

/**
 * Translated from AlignAura
 *
 * when translated into kotlin this code is a warcrime lmaooooo
 * @author soshimee
 */

object AlignAura : Module("Align Aura", description = "Does Arrow Align Device", category = Category.FLOOR7) {
    private var inP3 = false;
    private val solutions = listOf(
        listOf(7, 7, 7, 7, null, 1, null, null, null, null, 1, 3, 3, 3, 3, null, null, null, null, 1, null, 7, 7, 7, 1),
        listOf(null, null, null, null, null, 1, null, 1, null, 1, 1, null, 1, null, 1, 1, null, 1, null, 1, null, null, null, null, null),
        listOf(5, 3, 3, 3, null, 5, null, null, null, null, 7, 7, null, null, null, 1, null, null, null, null, 1, 3, 3, 3, null),
        listOf(null, null, null, null, null, null, 1, null, 1, null, 7, 1, 7, 1, 3, 1, null, 1, null, 1, null, null, null, null, null),
        listOf(null, null, 7, 7, 5, null, 7, 1, null, 5, null, null, null, null, null, null, 7, 5, null, 1, null, null, 7, 7, 1),
        listOf(7, 7, null, null, null, 1, null, null, null, null, 1, 3, 3, 3, 3, null, null, null, null, 1, null, null, null, 7, 1),
        listOf(5, 3, 3, 3, 3, 5, null, null, null, 1, 7, 7, null, null, 1, null, null, null, null, 1, null, 7, 7, 7, 1),
        listOf(7, 7, null, null, null, 1, null, null, null, null, 1, 3, null, 7, 5, null, null, null, null, 5, null, null, null, 3, 3),
        listOf(null, null, null, null, null, 1, 3, 3, 3, 3, null, null, null, null, 1, 7, 7, 7, 7, 1, null, null, null, null, null)
    )
    private val deviceStandLocation = Vec3(0.0, 120.0, 77.0)
    private val deviceCorner = Vec3(-2.0, 120.0, 75.0)
    private val recentClicks = mutableListOf<Long>()
    private var currentFrames: MutableList<Pair<EntityItemFrame, Int>>? = null;

    private fun getCurrentFrames(): MutableList<Pair<EntityItemFrame, Int>> {
        val entities = mc.theWorld.getEntitiesOfType<EntityItemFrame>()
        val frames = mutableMapOf<BlockPos , Pair<EntityItemFrame, Int>>()

        for (entity in entities) {
            val pos = BlockPos(entity.posX, entity.posY, entity.posZ)
            val mcItem = entity.displayedItem ?: continue
            if (mcItem.getID() != 262) continue;
            val rotation = entity.rotation
            frames[pos] = Pair(entity, rotation);
        }
        val x = deviceCorner.xCoord
        val y0 = deviceCorner.yCoord
        val z0 = deviceCorner.zCoord
        val array = mutableListOf<Pair<EntityItemFrame, Int>?>()
        val currentFrames1 = currentFrames
        for (dz in 0 until 5) {
            for (dy in 0 until 5) {
            val index = dy + dz * 5;
            if (currentFrames1 != null && System.currentTimeMillis() - recentClicks[index] < 1000) {
                array.add(currentFrames1[index]);
                continue;
            }
            val y = y0 + dy;
            val z = z0 + dz;
            val pos = BlockPos(x, y, z)
            if (pos in frames) {
                array.add(frames[pos]);
                continue;
            }
            array.add(null);
        }
        }

        return array;
    }

    fun tick(event: ClientTickEvent) {
        if (!LocationUtils.currentArea.isArea(Island.Dungeon) || event.phase != TickEvent.Phase.START) return;

        if (deviceStandLocation.distanceTo(mc.thePlayer.positionVector) > 10) {
            currentFrames = null;
            return;
        }
        currentFrames = getCurrentFrames();
        val rotations = currentFrames?.map { it.first.rotation }
        //val solution = solutions.find(!solution.some((value, index) => value === null ^ rotations[index] === null));

        val solution = solutions.find { solution ->
            solution.indices.all { index ->
                (solution[index] == null) xor (rotations?.get(index) == null)
            }
        } ?: return

        val sortedFrames = currentFrames!!.withIndex().sortedByDescending { (_, frame) ->
            frame.let {
                mc.thePlayer.positionVector.add(Vec3(0.0, mc.thePlayer.eyeHeight.toDouble(), 0.0)).squareDistanceTo(it.first.positionVector)
            }
        }

        for ((index, frame) in sortedFrames) {
            val entity = frame
            if (mc.thePlayer.getDistanceSqToEntity(entity.first) > 25) continue
            if (solution[index] == null) continue
            var clicksNeeded = (solution[index]!! - frame.first.rotation + 8) % 8
            if (clicksNeeded <= 0) continue
            if (!inP3 && currentFrames!!.count { frame ->
                (solution[currentFrames!!.indexOf(frame)]!! - frame.first.rotation + 8) % 8 > 0
            } <= 1) {
                clicksNeeded--
            }
            if (clicksNeeded > 0) recentClicks[index] = System.currentTimeMillis() //Wadey pls check wtf im supposed to do here
            for (i in 0 until clicksNeeded) {
                frame.first.setItemRotation((frame.first.rotation + 1) % 8)
                PacketUtils.sendPacket( C02PacketUseEntity(entity.first,  Vec3(0.03125, 0.0, 0.0)));
                PacketUtils.sendPacket(C02PacketUseEntity(entity.first, C02PacketUseEntity.Action.INTERACT));
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: ChatPacketEvent){
        if (event.message === "[BOSS] Goldor: Who dares trespass into my domain?") inP3 = true;
        else if (event.message === "The Core entrance is opening!") inP3 = false;
    }
    @SubscribeEvent
    fun worldUnload(event: WorldEvent.Unload){
        inP3 = false;
    }




}