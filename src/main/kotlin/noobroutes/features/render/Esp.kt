package noobroutes.features.render

import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.server.S13PacketDestroyEntities
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.getMobEntity
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import java.util.*

@DevOnly
object Esp : Module(
    name = "Starred Mob ESP",
    category = Category.RENDER,
    description = "Cheaters get banned!"
) {
    val depthCheck by BooleanSetting("Depth Check", false, description = "Disables depth check on starred mobs")
    private val drawBoxes by BooleanSetting("Draw Boxes", false, description = "Draws boxes around starred mobs")
    private val color by ColorSetting("Esp Color", Color.GREEN, true, description = "Disables depth check on starred mobs")

    private val starredMobs: MutableSet<Int> = mutableSetOf();
    private val scannedMobs: MutableSet<Int> = mutableSetOf();

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        starredMobs.clear();
        scannedMobs.clear();
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        starredMobs.clear();
        scannedMobs.clear();
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!DungeonUtils.inDungeons) return

        if (!drawBoxes) return

        val size = starredMobs.size;
        val list: List<Int> = starredMobs.toList();
        for (i in 0 until size) {
            val entityID = list[i];
            val entity = Minecraft.getMinecraft().theWorld.getEntityByID(entityID)
            if (entity == null) {
                starredMobs.remove(entityID)
                continue
            }

            val boundingBox: AxisAlignedBB = RenderUtils.getPartialEntityBoundingBox(entity, event.partialTicks)
            RenderUtils.renderBBOutline(boundingBox, event.partialTicks, color)
        }
    }

    private fun isValidEntity(entity: EntityArmorStand?): Boolean {
        if (entity == null) return false;
        //if (scannedMobs.contains(entity.entityId)) return false;
        if (!entity.hasCustomName() || !entity.customNameTag.contains("§6✯ ") || !entity.customNameTag.endsWith("§c❤") || !entity.alwaysRenderNameTag) {
            scannedMobs.add(entity.entityId)
            return false;
        }
        return true
    }

    @SubscribeEvent
    fun onReceivePacket(event: PacketEvent.Receive) {
        if (!DungeonUtils.inDungeons) return

        if (event.packet.javaClass == S13PacketDestroyEntities::class.java) {
            Arrays.stream((event.packet as S13PacketDestroyEntities).entityIDs).forEach { id: Int ->
                starredMobs.remove(id)
            }
        }
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent) {
        if (event.isEnd || !DungeonUtils.inDungeons) return
        for (armorStand in Minecraft.getMinecraft().theWorld.getEntities(EntityArmorStand::class.java) { obj: EntityArmorStand? -> isValidEntity(obj) }) {
            if (armorStand == null) return

            val e = getMobEntity(armorStand) ?: continue; // Entity may not be loaded
            starredMobs.add(e.entityId)
            //scannedMobs.add(armorStand.entityId)
        }

        // Check for shadow assassins
        for (player in Minecraft.getMinecraft().theWorld.getPlayers(Entity::class.java) { entity: Entity? -> entity!!.name.hashCode() == -0x277A5F7B}) { // Dont even ask
            starredMobs.add(player.entityId)
        }
    }

    private fun isStarred(id: Int): Boolean {
        return this.starredMobs.contains(id);
    }

    fun shouldCancelDepthCheck(id: Int) : Boolean {
        return depthCheck && this.enabled && isStarred(id);
    }

    // Todo
    // Fix missing mobs with the performance
    // Add fels
    // Disable depth check for entity armor and weapons too
}