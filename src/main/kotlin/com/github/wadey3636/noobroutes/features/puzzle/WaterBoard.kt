package com.github.wadey3636.noobroutes.features.puzzle


import com.github.wadey3636.noobroutes.utils.AuraManager
import com.github.wadey3636.noobroutes.utils.ClientUtils
import com.github.wadey3636.noobroutes.utils.Utils
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.events.impl.ServerTickEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.utils.equal
import me.defnotstolen.utils.render.Color
import me.defnotstolen.utils.render.RenderUtils
import me.defnotstolen.utils.skyblock.devMessage
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import me.defnotstolen.utils.skyblock.dungeon.Puzzle
import me.defnotstolen.utils.skyblock.dungeon.tiles.Room
import me.defnotstolen.utils.skyblock.getBlockAt
import me.defnotstolen.utils.skyblock.modMessage
import me.defnotstolen.utils.toBlockPos
import me.defnotstolen.utils.toVec3
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.PlayerUseItemEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


object WaterBoard : Module("WaterBoard", Keyboard.KEY_NONE, Category.PUZZLE, description = "Automatic Waterboard Solver") {
    private var waterSolutions: JsonObject

    init {
        val isr = WaterBoard::class.java.getResourceAsStream("/waterSolutions.json")?.let { InputStreamReader(it, StandardCharsets.UTF_8) }
        waterSolutions = JsonParser().parse(isr).asJsonObject
        execute(500) {
        if (enabled) scan()
        }
    }

    private var solutions = HashMap<LeverBlock, Array<Double>>()
    private var patternIdentifier = -1
    private var openedWaterTicks = -1
    private var tickCounter = 0


    fun scan() = with (DungeonUtils.currentRoom) {

        //devMessage(this?.data?.name)
        if (this?.data?.name != "Water Board" || patternIdentifier != -1) return@with
        val extendedSlots = WoolColor.entries.joinToString("") { if (it.isExtended) it.ordinal.toString() else "" }.takeIf { it.length == 3 } ?: return

        patternIdentifier = when {
            getBlockAt(getRealCoords(14, 77, 27)) == Blocks.hardened_clay -> 0 // right block == clay
            getBlockAt(getRealCoords(16, 78, 27)) == Blocks.emerald_block -> 1 // left block == emerald
            getBlockAt(getRealCoords(14, 78, 27)) == Blocks.diamond_block -> 2 // right block == diamond
            getBlockAt(getRealCoords(14, 78, 27)) == Blocks.quartz_block  -> 3 // right block == quartz
            else -> return@with modMessage("§cFailed to get Water Board pattern. Was the puzzle already started?")
        }

        devMessage("$patternIdentifier || ${WoolColor.entries.filter { it.isExtended }.joinToString(", ") { it.name.lowercase() }}")

        solutions.clear()
        waterSolutions[true.toString()].asJsonObject[patternIdentifier.toString()].asJsonObject[extendedSlots].asJsonObject.entrySet().forEach { entry ->
            solutions[
                when (entry.key) {
                    "diamond_block" -> LeverBlock.DIAMOND
                    "emerald_block" -> LeverBlock.EMERALD
                    "hardened_clay" -> LeverBlock.CLAY
                    "quartz_block"  -> LeverBlock.QUARTZ
                    "gold_block"    -> LeverBlock.GOLD
                    "coal_block"    -> LeverBlock.COAL
                    "water"         -> LeverBlock.WATER
                    else -> LeverBlock.NONE
                }
            ] = entry.value.asJsonArray.map { it.asDouble }.toTypedArray()
        }
        //devMessage(solutions.isEmpty())
    }



    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        tickCounter++
    }
    @SubscribeEvent
    fun waterInteract(event: PacketEvent.Receive) {
        if (solutions.isEmpty() || event.packet !is S08PacketPlayerPosLook) return
        LeverBlock.entries.find { it.leverPos.equal(Vec3(event.packet.x, event.packet.y, event.packet.z)) }?.let {
            if (it == LeverBlock.WATER && openedWaterTicks == -1) openedWaterTicks = tickCounter
            it.i++
        }
    }

    var waitingForS08 = false
    @SubscribeEvent
    fun onTick(event: TickEvent){
        if (event.phase != TickEvent.Phase.START || waitingForS08) return
        devMessage("not checked board")
        if (patternIdentifier == -1 || solutions.isEmpty() || DungeonUtils.currentRoomName != "Water Board") return
        devMessage("in board")
        val solutionList = solutions
            .flatMap { (lever, times) -> times.drop(lever.i).map { Pair(lever, it) } }
            .sortedBy { (lever, time) -> time + if (lever == LeverBlock.WATER) 0.01 else 0.0 }

        val firstBlock = solutionList.firstOrNull()?.first?.relativePosition ?: return
        val relativePlayerVec = DungeonUtils.currentRoom?.getRelativeCoords(mc.thePlayer.positionVector) ?: return
        if (relativePlayerVec.yCoord != 59.0) return
        devMessage("starting etherwarp shit")
        val expectedX = when (firstBlock.xCoord) {
            5.0, 10.0 -> 9.0
            15.0 -> 15.0
            20.0 -> 20.0
            else -> null
        }

        if (expectedX != null && relativePlayerVec.xCoord != expectedX) {
            val warpTarget = DungeonUtils.currentRoom?.getRealCoords(15, 59, expectedX.toInt())?.toVec3()
            warpTarget?.let {
                Utils.etherwarpToBlock(it)
                waitingForS08 = true
            }
        }

        solutions.entries
            .flatMap { (lever, times) -> times.filter { it <= 0.0 }.map { lever } }
            .firstOrNull()
            ?.let { AuraManager.auraBlock(it.leverPos.toBlockPos()) }
    }

    @SubscribeEvent
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet is S08PacketPlayerPosLook) ClientUtils.clientScheduleTask { waitingForS08 = false }
    }



    fun reset() {
        LeverBlock.entries.forEach { it.i = 0 }
        patternIdentifier = -1
        solutions.clear()
        openedWaterTicks = -1
        tickCounter = 0
    }

    private enum class WoolColor(val relativePosition: BlockPos) {
        PURPLE(BlockPos(15, 56, 19)),
        ORANGE(BlockPos(15, 56, 18)),
        BLUE(BlockPos(15, 56, 17)),
        GREEN(BlockPos(15, 56, 16)),
        RED(BlockPos(15, 56, 15));

        inline val isExtended: Boolean get() =
            DungeonUtils.currentRoom?.let { getBlockAt(it.getRealCoords(relativePosition)) == Blocks.wool } == true
    }
    private enum class LeverBlock(val relativePosition: Vec3, var i: Int = 0) {
        QUARTZ(Vec3(20.0, 61.0, 20.0)),
        GOLD(Vec3(20.0, 61.0, 15.0)),
        COAL(Vec3(20.0, 61.0, 10.0)),
        DIAMOND(Vec3(10.0, 61.0, 20.0)),
        EMERALD(Vec3(10.0, 61.0, 15.0)),
        CLAY(Vec3(10.0, 61.0, 10.0)),
        WATER(Vec3(15.0, 60.0, 5.0)),
        NONE(Vec3(0.0, 0.0, 0.0));

        inline val leverPos: Vec3
            get() = DungeonUtils.currentRoom?.getRealCoords(relativePosition) ?: Vec3(0.0, 0.0, 0.0)
    }
}