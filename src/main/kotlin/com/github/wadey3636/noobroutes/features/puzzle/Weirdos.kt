package com.github.wadey3636.noobroutes.features.puzzle

import com.github.wadey3636.noobroutes.features.floor7.AutoP3.inBoss
import com.github.wadey3636.noobroutes.utils.AuraManager
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.ColorSetting
import me.defnotstolen.utils.addRotationCoords
import me.defnotstolen.utils.noControlCodes
import me.defnotstolen.utils.render.Color
import me.defnotstolen.utils.render.Renderer
import me.defnotstolen.utils.skyblock.PlayerUtils
import me.defnotstolen.utils.skyblock.devMessage
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils.currentRoomName
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils.inDungeons
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.BlockPos
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Based on odin
 */
object Weirdos : Module("Weirdos", description = "Solves weirdos puzzle", category = Category.PUZZLE) {
    private var correctPos: BlockPos? = null
    private var wrongPositions = CopyOnWriteArraySet<BlockPos>()
    private var clickedChest = false
    private val clickedWeirdos = mutableListOf<Int>()
    private val weirdoColor by ColorSetting("Color", Color.GREEN, description = "The color of a weirdo after clicked. (Completely cosmetic)")

init {
    onMessage(Regex("\\[NPC] (.+): (.+).?"), { enabled && inDungeons && !inBoss }) {
        str ->
        val (npc, message) = str.destructured
        onNPCMessage(npc, message)
    }


}

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || currentRoomName != "Three Weirdos" || clickedChest) return



        mc.theWorld.loadedEntityList
            .filter { it is EntityArmorStand && it.name.contains("CLICK") }
            .forEach { entity ->
                if (clickedWeirdos.contains(entity.entityId) || entity.positionVector.distanceTo(mc.thePlayer.positionVector) > 5) return@forEach
                AuraManager.auraEntity(entity, C02PacketUseEntity.Action.INTERACT_AT)
                clickedWeirdos.add(entity.entityId)
                return
            }

        correctPos?.let {
                AuraManager.auraBlock(it)
                clickedChest = true

        }


    }



    private fun onNPCMessage(npc: String, msg: String) {
        if (solutions.none { it.matches(msg) } && wrong.none { it.matches(msg) }) return
        val correctNPC = mc.theWorld?.loadedEntityList?.find { it is EntityArmorStand && it.name.noControlCodes == npc } ?: return
        val room = DungeonUtils.currentRoom ?: return
        val pos = BlockPos(correctNPC.posX - 0.5, 69.0, correctNPC.posZ - 0.5).addRotationCoords(room.rotation, -1, 0)

        if (solutions.any { it.matches(msg) }) {
            correctPos = pos
            PlayerUtils.playLoudSound("note.pling", 2f, 1f)
        } else wrongPositions.add(pos)
    }

    fun onRenderWorld(weirdosColor: Color, weirdosWrongColor: Color, weirdosStyle: Int) {
        if (DungeonUtils.currentRoomName != "Three Weirdos") return
        correctPos?.let { Renderer.drawStyledBlock(it, weirdosColor, weirdosStyle) }
        wrongPositions.forEach {
            Renderer.drawStyledBlock(it, weirdosWrongColor, weirdosStyle)
        }
    }

    @SubscribeEvent
    fun reset(event: WorldEvent.Load) {
        correctPos = null
        wrongPositions.clear()
        clickedChest = false
        clickedWeirdos.clear()

    }

    private val solutions = listOf(
        Regex("The reward is not in my chest!"),
        Regex("At least one of them is lying, and the reward is not in .+'s chest.?"),
        Regex("My chest doesn't have the reward. We are all telling the truth.?"),
        Regex("My chest has the reward and I'm telling the truth!"),
        Regex("The reward isn't in any of our chests.?"),
        Regex("Both of them are telling the truth. Also, .+ has the reward in their chest.?"),
    )

    private val wrong = listOf(
        Regex("One of us is telling the truth!"),
        Regex("They are both telling the truth. The reward isn't in .+'s chest."),
        Regex("We are all telling the truth!"),
        Regex(".+ is telling the truth and the reward is in his chest."),
        Regex("My chest doesn't have the reward. At least one of the others is telling the truth!"),
        Regex("One of the others is lying."),
        Regex("They are both telling the truth, the reward is in .+'s chest."),
        Regex("They are both lying, the reward is in my chest!"),
        Regex("The reward is in my chest."),
        Regex("The reward is not in my chest. They are both lying."),
        Regex(".+ is telling the truth."),
        Regex("My chest has the reward.")
    )
}