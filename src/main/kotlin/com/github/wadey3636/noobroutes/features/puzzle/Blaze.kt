package com.github.wadey3636.noobroutes.features.puzzle

import com.github.wadey3636.noobroutes.utils.PacketUtils
import com.github.wadey3636.noobroutes.utils.RotationUtils
import me.defnotstolen.events.impl.RoomEnterEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.Setting.Companion.withDependency
import me.defnotstolen.features.settings.impl.BooleanSetting
import me.defnotstolen.features.settings.impl.ColorSetting
import me.defnotstolen.utils.equalsOneOf
import me.defnotstolen.utils.noControlCodes
import me.defnotstolen.utils.render.Color
import me.defnotstolen.utils.render.Renderer
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent


object Blaze : Module(
    "Blaze",
    description = "Automatically completes the higher or lower puzzle",
    category = Category.PUZZLE
) {
    private var blazes = mutableListOf<EntityArmorStand>()
    private val blazeHealthRegex = Regex("^\\[Lv15] Blaze [\\d,]+/([\\d,]+)❤$")
    private var currentEtherwarpTarget: BlockPos? = null
    private var currentBlazeTarget: EntityArmorStand? = null
    private val silent by BooleanSetting("Silent", description = "Server Side Rotations")
    private val blazeHighlight by BooleanSetting("Highlight Target Blaze", description = "Highlights the blaze the module is targeting")
    private val blazeHighlightColor by ColorSetting("Blaze Color", Color.GREEN, description = "The color of the target blaze").withDependency { blazeHighlight }

    private val auraSecret by BooleanSetting("Aura Secret", description = "Secret Aura for the secret in blaze. (Have Secret Guide off for blaze if you use this)")




    private fun shootAt(yaw: Float, pitch: Float) {
        PacketUtils.c03ScheduleTask(cancel = true) {
            if (!silent) RotationUtils.setAngles(yaw, pitch)
            PacketUtils.sendPacket(C05PacketPlayerLook(yaw, pitch, mc.thePlayer.onGround))
            PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
        }
    }

    /**
     * Taken from Odin
     */
    private fun getBlaze() {
        val room = DungeonUtils.currentRoom ?: return
        if (!DungeonUtils.inDungeons || !room.data.name.equalsOneOf("Lower Blaze", "Higher Blaze")) return
        val hpMap = mutableMapOf<EntityArmorStand, Int>()
        blazes.clear()
        mc.theWorld?.loadedEntityList?.forEach { entity ->
            if (entity !is EntityArmorStand || entity in blazes) return@forEach
            val hp = blazeHealthRegex.find(entity.name.noControlCodes)?.groups?.get(1)?.value?.replace(",", "")
                ?.toIntOrNull() ?: return@forEach
            hpMap[entity] = hp
            blazes.add(entity)
        }
        if (room.data.name == "Lower Blaze") blazes.sortByDescending { hpMap[it] }
        else blazes.sortBy { hpMap[it] }
    }


    @SubscribeEvent
    fun onEnterRoom(event: RoomEnterEvent) {
        getBlaze()
    }
    @SubscribeEvent
    fun onTick(event: ClientTickEvent){
        if (event.phase != )
    }


    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        val room = DungeonUtils.currentRoom ?: return
        if (!room.data.name.equalsOneOf("Lower Blaze", "Higher Blaze")) return
        currentEtherwarpTarget?.let {
            if (blazeHighlight) Renderer.drawBlock(it, blazeHighlightColor)
        }

        currentBlazeTarget

    }


    private fun etherwarpToVec3(pos: Vec3) {
        val rot = RotationUtils.getYawAndPitch(pos, true)

        PacketUtils.c03ScheduleTask(1, cancel = true) {
            if (!silent) RotationUtils.setAngles(rot.first, rot.second)
            PacketUtils.sendPacket(C05PacketPlayerLook(rot.first, rot.second, mc.thePlayer.onGround))
            PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
        }
    }


}