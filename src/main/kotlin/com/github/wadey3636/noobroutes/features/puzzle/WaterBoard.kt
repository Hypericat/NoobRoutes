package com.github.wadey3636.noobroutes.features.puzzle


import com.github.wadey3636.noobroutes.utils.AuraManager
import com.github.wadey3636.noobroutes.utils.ClientUtils
import com.github.wadey3636.noobroutes.utils.PacketUtils
import com.github.wadey3636.noobroutes.utils.RotationUtils
import com.github.wadey3636.noobroutes.utils.Utils.isClose
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.defnotstolen.events.impl.S08Event
import me.defnotstolen.events.impl.ServerTickEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.impl.render.ClickGUIModule.devMode
import me.defnotstolen.features.impl.render.ClickGUIModule.forceHypixel
import me.defnotstolen.utils.add
import me.defnotstolen.utils.render.Color
import me.defnotstolen.utils.render.Renderer
import me.defnotstolen.utils.skyblock.*
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils.currentRoom
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import me.defnotstolen.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import me.defnotstolen.utils.toBlockPos
import me.defnotstolen.utils.toVec3
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

//based on odin


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


    fun scan() = with (currentRoom) {

        if (DungeonUtils.currentRoomName != "Water Board" || patternIdentifier != -1) return@with
        val extendedSlots = WoolColor.entries.joinToString("") { if (it.isExtended) it.ordinal.toString() else "" }.takeIf { it.length == 3 } ?: return

        patternIdentifier = when {
            getBlockAt(currentRoom?.getRealCoords(14, 77, 27) ?: return) == Blocks.hardened_clay -> 0 // right block == clay
            getBlockAt(currentRoom?.getRealCoords(16, 78, 27) ?: return) == Blocks.emerald_block -> 1 // left block == emerald
            getBlockAt(currentRoom?.getRealCoords(14, 78, 27) ?: return) == Blocks.diamond_block -> 2 // right block == diamond
            getBlockAt(currentRoom?.getRealCoords(14, 78, 27) ?: return) == Blocks.quartz_block  -> 3 // right block == quartz
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
    }



    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        if (mc.isSingleplayer) return
        tickCounter++
    }

    @SubscribeEvent
    fun onNotServer(event: TickEvent.ClientTickEvent) {
        if (!mc.isSingleplayer) return
        if (event.phase != TickEvent.Phase.START) return
        tickCounter++
    }

    var waitingForS08 = false
    var warped = false
    var openedChest = false
    @SubscribeEvent
    fun onTick(event: TickEvent){
        if (event.phase != TickEvent.Phase.START || waitingForS08) return
        if (patternIdentifier == -1 || solutions.isEmpty() || DungeonUtils.currentRoomName != "Water Board") return
        val currRoom = currentRoom ?: return
        val solutionList = solutions
            .flatMap { (lever, times) -> times.drop(lever.i).map { Pair(lever, it) } }
            .sortedBy { (lever, time) -> time + if (lever == LeverBlock.WATER) 0.01 else 0.0 }

        val firstBlock = solutionList.firstOrNull()?.first?.relativePosition
        if (firstBlock != null) {
            val relativePlayerZ = currRoom.getRelativeCoords(mc.thePlayer.positionVector).zCoord
            if (mc.thePlayer.posY != 59.0) return
            val expectedZ = when (firstBlock.zCoord) {
                5.0, 10.0 -> 8.5
                15.0 -> 14.5
                20.0 -> 19.5
                else -> null
            }

            if (expectedZ != null && !isClose(relativePlayerZ, expectedZ.toDouble())) {
                devMessage("expectedZ: $expectedZ, relativePlayerZ: $relativePlayerZ")
                val warpTarget = BlockPos(15, 58, (expectedZ + 0.5).toInt())
                    devMessage(warpTarget)
                    etherwarpToTopBlock(warpTarget)
                    return
            }

            val next = solutionList.firstOrNull() ?: return
            val (lever, time) = next
            val timeInTicks = (time * 20).toInt()

            if ((openedWaterTicks == -1 && timeInTicks == 0) || (openedWaterTicks != -1 && tickCounter >= openedWaterTicks + timeInTicks)) {
                AuraManager.auraBlock(lever.leverPos.toBlockPos())
                if (lever == LeverBlock.WATER && openedWaterTicks == -1) {
                    openedWaterTicks = tickCounter
                }
                lever.i++
                return
            }
        }
        else {
            if (solutions.all { (lever, times) -> lever.i >= times.size } && !warped) {
                etherwarpToTopBlock(BlockPos(15, 58, 22))

                warped = true
            }

            if (
                getBlockAt(currRoom.getRealCoords(15, 56, 15)) == Blocks.air &&
                getBlockAt(currRoom.getRealCoords(15, 56, 16)) == Blocks.air &&
                getBlockAt(currRoom.getRealCoords(15, 56, 17)) == Blocks.air &&
                getBlockAt(currRoom.getRealCoords(15, 56, 18)) == Blocks.air &&
                getBlockAt(currRoom.getRealCoords(15, 56, 19)) == Blocks.air &&
                getBlockAt(currRoom.getRealCoords(15, 56, 22)) == Blocks.chest &&
                warped && !openedChest
            ) {
                AuraManager.auraBlock(currRoom.getRealCoords(15, 56, 22))
                devMessage("sent for chest")
                openedChest = true
            }
        }
    }

    @SubscribeEvent
    fun onS08(event: S08Event) {
        devMessage("waiting s08: $waitingForS08")
        ClientUtils.clientScheduleTask(1) { waitingForS08 = false }
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        warped = false
        openedChest = false
        reset()
    }

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        warped = false
        openedChest = false
        reset()
    }

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
            currentRoom?.let { getBlockAt(it.getRealCoords(relativePosition)) == Blocks.wool } == true
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
            get() = currentRoom?.getRealCoords(relativePosition) ?: Vec3(0.0, 0.0, 0.0)
    }


    private fun etherwarpToTopBlock(coords: BlockPos) {
        val currRoom = currentRoom ?: return
        waitingForS08 = true
        val target = currRoom.getRealCoords(coords).toVec3().add(0.5, 1.0, 0.5)
        if (devMode) {
            blocks.add(currRoom.getRealCoords(coords))
            blockRays.add(Pair(target, mc.thePlayer.positionVector.add(0.0, 1.54,0.0)))
        }


        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, true)
        RotationUtils.setAngleToVec3(target, true)
        ClientUtils.clientScheduleTask(1) {
            PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.keyCode))
            if (LocationUtils.currentArea.isArea(Island.SinglePlayer) && forceHypixel) {
                mc.thePlayer.setPosition(target.xCoord, target.yCoord, target.zCoord)
                mc.thePlayer.setVelocity(0.0, 0.0, 0.0)
                waitingForS08 = false
            }
        }
    }
    private val blockRays = mutableListOf<Pair<Vec3, Vec3>>()
    private val blocks = mutableListOf<BlockPos>()
    @SubscribeEvent
    fun renderWorld(event: RenderWorldLastEvent){
        blockRays.forEach { Renderer.draw3DLine(listOf(it.first, it.second), Color.BLUE) }
        blocks.forEach { Renderer.drawBlock(it, Color.GREEN) }
    }


}