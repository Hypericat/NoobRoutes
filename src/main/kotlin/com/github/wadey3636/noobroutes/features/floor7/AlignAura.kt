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
    private val recentClicks = mutableListOf<Int>()
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
        val rotations = currentFrames.map(it?.rotation ?? null);
        val solution = solutions.find(solution => !solution.some((value, index) => value === null ^ rotations[index] === null));
        if (!solution) return;
        for (var z of Object.entries(currentFrames).sort((a, b) => a[1] && b[1] && ((Player.getX() - b[1].entity.getX()) ** 2 + (Player.getY() + Player.getPlayer().func_70047_e() - b[1].entity.getY()) ** 2 + (Player.getZ() - b[1].entity.getZ()) ** 2) - ((Player.getX() - a[1].entity.getX()) ** 2 + (Player.getY() + Player.getPlayer().func_70047_e() - a[1].entity.getY()) ** 2 + (Player.getZ() - a[1].entity.getZ()) ** 2))) {
            var [index, frame] = z;
            if (!frame) continue;
            val entity = frame.entity;
            if ((Player.getX() - entity.getX()) ** 2 + (Player.getY() + Player.getPlayer().func_70047_e() - entity.getY()) ** 2 + (Player.getZ() - entity.getZ()) ** 2 > 25) continue;
            var clicksNeeded = (solution[index] - frame.rotation + 8) % 8;
            if (clicksNeeded <= 0) continue;
            val mcEntity = entity.getEntity();
            if (!mcEntity) continue;
            if (!inP3 && currentFrames.filter((frame, index) => frame && (solution[index] - frame.rotation + 8) % 8 > 0).length <= 1) --clicksNeeded;
            if (clicksNeeded > 0) recentClicks[index] = Date.now();
            for (var i = 0; i < clicksNeeded; ++i) {
            frame.rotation = (frame.rotation + 1) % 8;
            PacketUtils.sendPacket( C02PacketUseEntity(mcEntity,  Vec3(0.03125, 0, 0)));
            PacketUtils.sendPacket(C02PacketUseEntity(mcEntity, C02PacketUseEntity.Action.INTERACT));
        }
            break;
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