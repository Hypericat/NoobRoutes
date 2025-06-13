package noobroutes.features.dungeon.puzzle

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.events.impl.ServerTickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.utils.*
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoordsOdin
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object WaterBoard : Module("WaterBoard", Keyboard.KEY_NONE, Category.DUNGEON, description = "Automatic Waterboard Solver") {
    private var waterSolutions: JsonObject

    init {
        val isr = WaterBoard::class.java.getResourceAsStream("/waterSolutions.json")?.let { InputStreamReader(it, StandardCharsets.UTF_8) }
        waterSolutions = JsonParser().parse(isr).asJsonObject
        execute(500) {
            if (enabled) scan(true)
        }
    }

    private var solutions = HashMap<LeverBlock, Array<Double>>()
    private var patternIdentifier = -1
    private var openedWaterTicks = -1
    private var tickCounter = 0
    private var c08Delay = System.currentTimeMillis()

    fun scan(optimized: Boolean) = with (DungeonUtils.currentRoom) {
        if (this?.data?.name != "Water Board" || patternIdentifier != -1) return@with
        val extendedSlots = WoolColor.entries.joinToString("") { if (it.isExtended) it.ordinal.toString() else "" }.takeIf { it.length == 3 } ?: return

        patternIdentifier = when {
            getBlockAt(getRealCoordsOdin(14, 77, 27)) == Blocks.hardened_clay -> 0 // right block == clay
            getBlockAt(getRealCoordsOdin(16, 78, 27)) == Blocks.emerald_block -> 1 // left block == emerald
            getBlockAt(getRealCoordsOdin(14, 78, 27)) == Blocks.diamond_block -> 2 // right block == diamond
            getBlockAt(getRealCoordsOdin(14, 78, 27)) == Blocks.quartz_block  -> 3 // right block == quartz
            else -> return@with modMessage("§cFailed to get Water Board pattern. Was the puzzle already started?")
        }

        modMessage("$patternIdentifier || ${WoolColor.entries.filter { it.isExtended }.joinToString(", ") { it.name.lowercase() }}")

        solutions.clear()
        waterSolutions[optimized.toString()].asJsonObject[patternIdentifier.toString()].asJsonObject[extendedSlots].asJsonObject.entrySet().forEach { entry ->
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
    }

    private var awaitingS08 = false

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || awaitingS08 || didChest) return
        if (patternIdentifier == -1 || solutions.isEmpty() || DungeonUtils.currentRoomName != "Water Board" || mc.thePlayer.posY != 59.0) return
        val room = DungeonUtils.currentRoom ?: return
        val solutionList = solutions
            .flatMap { (lever, times) -> times.drop(lever.i).map { Pair(lever, it) } }
            .sortedBy { (lever, time) -> time + if (lever == LeverBlock.WATER) 0.01 else 0.0 }
        val first = solutionList.firstOrNull()
        if (first == null) {
            doChest()
            return
        }
        val (firstLever, time) = first
        val relativeFirst = firstLever.relativePosition
        val expectedZRelative = when (relativeFirst.zCoord) {
            -5.0, 0.0 -> relativeFirst.zCoord.toInt()
            5.0, 10.0 -> 6
            else -> return
        }
        val etherwarpBlock = room.getRealCoords(0, 58, expectedZRelative)
        if (mc.thePlayer.positionVector.subtract(Vec3(0.0,1.0,0.0)).toBlockPos() != etherwarpBlock) {
            if (System.currentTimeMillis() - c08Delay < 200) return
            val realSpot = Vec3(etherwarpBlock.x + 0.5, etherwarpBlock.y + 1.1, etherwarpBlock.z + 0.5)
            AutoRouteUtils.etherwarpToVec3(realSpot, true)
            awaitingS08 = true
            Scheduler.schedulePreTickTask { awaitingS08 = false }
            return
        }

        val timeRemaining = openedWaterTicks + (time * 20).toInt() - tickCounter
        if ((firstLever != LeverBlock.WATER && timeRemaining <= 0) || (firstLever == LeverBlock.WATER && openedWaterTicks == -1) || (firstLever == LeverBlock.WATER && timeRemaining <= 0)) {
            if (firstLever == LeverBlock.WATER && openedWaterTicks == -1) openedWaterTicks = tickCounter
            firstLever.i++
            AuraManager.auraBlock(firstLever.leverPos.toBlockPos())
            c08Delay = System.currentTimeMillis()
        }
    }

    private var doChest = false
    private var didChest = false

    fun doChest() {
        val room = DungeonUtils.currentRoom ?: return
        val aboveChest = room.getRealCoords(0, 58, -7)
        if (mc.thePlayer.positionVector.subtract(Vec3(0.0,1.0,0.0)).toBlockPos() != aboveChest) {
            if (System.currentTimeMillis() - c08Delay < 200) return
            val realSpot = Vec3(aboveChest.x + 0.5, aboveChest.y + 1.1, aboveChest.z + 0.5)
            AutoRouteUtils.etherwarpToVec3(realSpot)
            awaitingS08 = true
            Scheduler.schedulePreTickTask { awaitingS08 = false }
            return
        }
        val chest = room.getRealCoords(BlockPos(0 ,56, -7))
        if ((doors.map{room.getRealCoords(it)}.any{!isAir(it)}) || mc.theWorld.getBlockState(chest).block != Blocks.chest) return
        AuraManager.auraBlock(chest)
        didChest = true
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        if (!mc.isSingleplayer) tickCounter++
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (mc.isSingleplayer) tickCounter++
    }

    @SubscribeEvent
    fun onRoomEnter(event: RoomEnterEvent) {
        reset()
    }

    fun reset() {
        LeverBlock.entries.forEach { it.i = 0 }
        patternIdentifier = -1
        solutions.clear()
        openedWaterTicks = -1
        tickCounter = 0
        doChest = false
        didChest = false
    }

    @SubscribeEvent
    fun worldLoad(event: WorldEvent.Load) {
        reset()
    }
    @SubscribeEvent
    fun worldUnload(event: WorldEvent.Unload) {
        reset()
    }

    val doors = setOf<BlockPos>(
        BlockPos(0, 56, -4),
        BlockPos(0, 56, -3),
        BlockPos(0, 56, -2),
        BlockPos(0, 56, -1),
        BlockPos(0, 56, 0)
    )

    private enum class WoolColor(val relativePosition: BlockPos) { //translated
        PURPLE(BlockPos(0, 56, -4)),
        ORANGE(BlockPos(0, 56, -3)),
        BLUE(BlockPos(0, 56, -2)),
        GREEN(BlockPos(0, 56, -1)),
        RED(BlockPos(0, 56, 0));

        inline val isExtended: Boolean get() =
            DungeonUtils.currentRoom?.let { getBlockAt(it.getRealCoords(relativePosition)) == Blocks.wool } == true
    }

    private enum class LeverBlock(val relativePosition: Vec3, var i: Int = 0) { //changed to new pos
        QUARTZ(Vec3(-5.0, 61.0, -5.0)),
        GOLD(Vec3(-5.0, 61.0, 0.0)),
        COAL(Vec3(-5.0, 61.0, 5.0)),
        DIAMOND(Vec3(5.0, 61.0, -5.0)),
        EMERALD(Vec3(5.0, 61.0, 0.0)),
        CLAY(Vec3(5.0, 61.0, 5.0)),
        WATER(Vec3(0.0, 60.0, 10.0)),
        NONE(Vec3(0.0, 0.0, 0.0));

        inline val leverPos: Vec3
            get() = DungeonUtils.currentRoom?.getRealCoords(relativePosition) ?: Vec3(0.0, 0.0, 0.0)
    }
}