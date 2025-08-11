package noobroutes.features.render

import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityGiantZombie
import net.minecraft.network.play.server.S13PacketDestroyEntities
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.events.BossEventDispatcher
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.*
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.modMessage
import java.lang.reflect.Field
import java.util.*


object Esp : Module(
    name = "Mob ESP",
    category = Category.RENDER,
    description = "Cheaters get banned!"
) {
    private val drawBoxes by BooleanSetting("Draw Outline", true, description = "Draws outlines around starred mobs")
    private val drawFilledBox by BooleanSetting("Fill Box", false, description = "Draws filled boxes around starred mobs")

    private val showStarredMobs by BooleanSetting("Show Starred mobs", true, description = "ESPs starred mobs")
    private val outlineColor by ColorSetting("Star Mob Outline", Color.GREEN, true, description = "Color of starred mob outline").withDependency { drawBoxes && showStarredMobs }
    private val filledBoxColor by ColorSetting("Star Mob Fill", Color.CYAN, true, description = "Color of the fill").withDependency { drawFilledBox && showStarredMobs}
    private val onlyShowInCurrentRoom by BooleanSetting("Only Room Mobs", true, description = "Only ESPs mobs in the current room").withDependency { showStarredMobs }
    private val tracerCount by NumberSetting("Tracer Mobs Count", 0, min = 0, max = 20, increment = 1, description = "Draws tracers to remaining x starred mobs in the room. Use 0 to disable!").withDependency { showStarredMobs && onlyShowInCurrentRoom }

    private val drawBloodMobs by BooleanSetting("Show Blood Mobs", true, description = "ESPs blood mobs")
    private val bloodOutline by ColorSetting("Blood Mob Outline", Color.RED, true, description = "Color of blood mob outline").withDependency { drawBloodMobs && drawBoxes }
    private val bloodFill by ColorSetting("Blood Mob Fill", Color.ORANGE, true, description = "Color of blood mob fill").withDependency { drawBloodMobs && drawFilledBox }

    private val withers by BooleanSetting("Show Withers", true, description = "ESPs withers")
    private val witherOutline by ColorSetting("Wither Outline", Color.RED, true, description = "Color of wither outline").withDependency { withers && drawBoxes }
    private val witherFill by ColorSetting("Wither Fill", Color.ORANGE, true, description = "Color of wither fill").withDependency { withers && drawFilledBox }

    private val depthCheck by BooleanSetting("Disable Model Depth", false, description = "Disables depth check on starred mobs, you will need to disable patcher cull entities for this to work!")
    private val updateInterval by NumberSetting("Update Interval", 10, 1, 100, 1, description = "Update interval (in ticks) for starred mobs")

    private val fuckSkytils by BooleanSetting("Skytils Fix", true, description = "Disables skytils hide non-starred mobs feature which is bugged and hides all mobs. This is a temporary fix until skytils updates")

    private val starredMobs: MutableSet<Int> = mutableSetOf();
    private val bloodMobs: MutableSet<Int> = mutableSetOf();
    private val bloodNames: MutableSet<Int> = mutableSetOf();
    private var wither: Int = -1;
    private var witherDistance: Double = Double.MAX_VALUE;

    private var tick: Int = 0;

    init {
        addName("Revoker")
        addName("Psycho")
        addName("Reaper")
        addName("Cannibal")
        addName("Mute")
        addName("Ooze")
        addName("Putrid")
        addName("Freak")
        addName("Leech")
        addName("Tear")
        addName("Parasite")
        addName("Flamer")
        addName("Skull")
        addName("Mr. Dead")
        addName("Vader")
        addName("Frost")
        addName("Walker")
        addName("Wandering Soul")
        addName("Bonzo")
        addName("Scarf")
        addName("Livid")
        addName("Spirit Bear")
    }

    private fun addName(name: String) {
        bloodNames.add(name.hashCode());
    }


    override fun onEnable() {
        if (fuckSkytils) fuckSkytils(); // Skytils fix because of mod update it hides all nametags which makes some mobs not be esp
        super.onEnable()
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        starredMobs.clear();
        bloodMobs.clear();
        tick = 0;
    }


    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        starredMobs.clear();
        bloodMobs.clear();
        tick = 0;
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!drawBoxes || !DungeonUtils.inDungeons) return

        if (showStarredMobs && !BossEventDispatcher.inBoss && starredMobs.isNotEmpty()) {
            handleRender(starredMobs.toList(), outlineColor, filledBoxColor, starredMobs, event.partialTicks, starredMobs.size <= tracerCount)
        }

        if (drawBloodMobs && !BossEventDispatcher.inBoss && bloodMobs.isNotEmpty())
            handleRender(bloodMobs.toList(), bloodOutline, bloodFill, bloodMobs, event.partialTicks)

        if (withers && wither != -1 && BossEventDispatcher.inBoss) {
            val entity = Minecraft.getMinecraft().theWorld.getEntityByID(wither)
            if (entity == null) {
                wither = -1;
                return
            }

            val boundingBox: AxisAlignedBB = RenderUtils.getPartialEntityBoundingBox(entity, event.partialTicks)
            if (drawFilledBox)
                RenderUtils.drawFilledAABB(boundingBox, witherFill);
            if (drawBoxes)
                RenderUtils.drawOutlinedAABB(boundingBox, witherOutline, 1f)
        }
    }

    private fun handleRender(ids: List<Int>, outline: Color, fill: Color, parent: MutableSet<Int>, partialTicks: Float, tracer: Boolean = false) {
        for (i in 0 until ids.size) {
            val entityID = ids[i];
            val entity = Minecraft.getMinecraft().theWorld.getEntityByID(entityID)
            if (entity == null) {
                parent.remove(entityID)
                continue
            }

            val boundingBox: AxisAlignedBB = RenderUtils.getPartialEntityBoundingBox(entity, partialTicks)
            if (drawFilledBox)
                RenderUtils.drawFilledAABB(boundingBox, fill);
            if (drawBoxes)
                RenderUtils.drawOutlinedAABB(boundingBox, outline, 1f)
            if (tracer) {
                RenderUtils.drawLines(listOf(Minecraft.getMinecraft().thePlayer.getPositionEyes(partialTicks), boundingBox.middle), outlineColor, 1.0F, false)
            }
        }
    }

    private fun isValidEntity(entity: EntityArmorStand): Boolean {
        if (!entity.hasCustomName() || !entity.customNameTag.contains("§6✯ ") || !entity.customNameTag.endsWith("§c❤")) return false;
        if (onlyShowInCurrentRoom && DungeonUtils.currentRoom != null && !DungeonUtils.currentRoom!!.contains(entity.positionVector)) return false;
        return true;
    }


    @SubscribeEvent
    fun onReceivePacket(event: PacketEvent.Receive) {
        if (!DungeonUtils.inDungeons || BossEventDispatcher.inBoss) return

        if (event.packet.javaClass == S13PacketDestroyEntities::class.java) {
            Arrays.stream((event.packet as S13PacketDestroyEntities).entityIDs).forEach { id: Int ->
                starredMobs.remove(id)
            }
        }
    }


    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent) {
        if (event.isEnd || !DungeonUtils.inDungeons || Minecraft.getMinecraft().theWorld == null || Minecraft.getMinecraft().thePlayer == null) return;

        if (++tick % updateInterval != 0) {
            return
        }
        starredMobs.clear();
        bloodMobs.clear();
        wither = -1;
        witherDistance = Double.MAX_VALUE;

        for (e: Entity in Minecraft.getMinecraft().theWorld.getLoadedEntityList()) {
            if (showStarredMobs && !BossEventDispatcher.inBoss && e is EntityArmorStand) {
                if (!isValidEntity(e)) continue

                val mob = getMobEntity(e) ?: continue; // Entity may not be loaded
                starredMobs.add(mob.entityId)
                //scannedMobs.add(e.entityId)
                e.alwaysRenderNameTag = true;
                mob.isInvisible = false;
                continue
            }

            if (!BossEventDispatcher.inBoss && e is EntityOtherPlayerMP) {
                if (showStarredMobs && e.name.hashCode() == -0x277A5F7B && (DungeonUtils.currentRoom == null || DungeonUtils.currentRoom!!.contains(e.positionVector))) { // Shadow Assassin
                    starredMobs.add(e.entityId)
                    e.isInvisible = false;
                    continue
                }
                if (drawBloodMobs && bloodNames.contains(e.name.trim().hashCode())) {
                    bloodMobs.add(e.entityId)
                    e.isInvisible = false;
                    continue
                }
                continue
            }

            if (showStarredMobs && !BossEventDispatcher.inBoss && e is EntityEnderman) {
                if (e.name.hashCode() == -0x3BEF85AA)
                    e.isInvisible = false;
                continue
            }

            if (drawBloodMobs && !BossEventDispatcher.inBoss && e is EntityGiantZombie) {
                bloodMobs.add(e.entityId)
                e.isInvisible = false;
                continue
            }

            if (withers && BossEventDispatcher.inBoss && e is EntityWither && !e.isInvisible && getSBMaxHealth(e) > 400f) {
                if (wither == -1) {
                    wither = e.entityId;
                    continue
                }

                val dist: Double = e.getDistanceSqToEntity(Minecraft.getMinecraft().thePlayer);
                if (dist < witherDistance) {
                    witherDistance = dist;
                    wither = e.entityId;
                }
                continue
            }
        }
    }

    private fun isStarred(id: Int): Boolean {
        return this.starredMobs.contains(id);
    }

    private fun isBlood(id: Int): Boolean {
        return this.bloodMobs.contains(id);
    }

    // Stolen and modified from farm helper
    private fun fuckSkytils() {
        if (Utils.hasPackageInstalled("gg.skytils.skytilsmod")) {
            try {
                // Get the ConfigManager instance
                val skytilsClass = Class.forName("gg.skytils.skytilsmod.core.Config")

                val hideNonStarredNametags: Field = skytilsClass.getDeclaredField("hideNonStarredNametags")
                hideNonStarredNametags.isAccessible = true;
                if (hideNonStarredNametags.getBoolean(null)) {
                    modMessage(("Disabling Skytils Hide Non-Starred Nametags as it is required for esp to work!"));
                    hideNonStarredNametags.setBoolean(null, false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun shouldCancelDepthCheck(id: Int) : Boolean {
        return depthCheck && this.enabled && (isStarred(id) || isBlood(id));
    }
}