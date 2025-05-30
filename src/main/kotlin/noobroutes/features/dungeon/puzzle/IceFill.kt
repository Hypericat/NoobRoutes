package noobroutes.features.dungeon.puzzle

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.utils.Vec2
import noobroutes.utils.addVec
import noobroutes.utils.isAir
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.IceFillFloors
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.toVec3
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object IceFill: Module(
    "Ice Fill",
    category = Category.DUNGEON,
    description = "Does Icefill"
) {
    private var currentPatterns: ArrayList<Vec3> = ArrayList()

    private var representativeFloors: List<List<List<Int>>>
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val isr = this::class.java.getResourceAsStream("/icefillFloors.json")
        ?.let { InputStreamReader(it, StandardCharsets.UTF_8) }

    init {
        try {
            val text = isr?.readText()
            representativeFloors = gson.fromJson(text, object : TypeToken<List<List<List<Int>>>>() {}.type)
            isr?.close()
        } catch (e: Exception) {
            devMessage("Error loading ice fill floors", e.toString())
            representativeFloors = emptyList()
        }
    }

    @SubscribeEvent
    fun onRenderWorld() {
        if (currentPatterns.isEmpty() || DungeonUtils.currentRoomName != "Ice Fill") return

        Renderer.draw3DLine(currentPatterns, color = Color.GREEN, depth = true)
    }
    @SubscribeEvent
    fun onRoomEnter(event: RoomEnterEvent) = with (event.room) {
        if (this?.data?.name != "Ice Fill" || currentPatterns.isNotEmpty()) return

        scanAllFloors(getRealCoords(15, 70, 7).toVec3(), rotation)
    }

    private fun scanAllFloors(pos: Vec3, rotation: Rotations) {
        listOf(pos, pos.add(transformTo(Vec3i(5, 1, 0), rotation)), pos.add(transformTo(Vec3i(12, 2, 0), rotation))).forEachIndexed { floorIndex, startPosition ->
            val floorHeight = representativeFloors[floorIndex]
            val startTime = System.nanoTime()

            for (patternIndex in floorHeight.indices) {
                if (
                    isAir(BlockPos(startPosition).add(transform(floorHeight[patternIndex][0], floorHeight[patternIndex][1], rotation))) &&
                    !isAir(BlockPos(startPosition).add(transform(floorHeight[patternIndex][2], floorHeight[patternIndex][3], rotation)))
                ) {
                    modMessage("Section $floorIndex scan took ${(System.nanoTime() - startTime) / 1000000.0}ms pattern: $patternIndex")

                    (if (false) IceFillFloors.advanced[floorIndex][patternIndex] else IceFillFloors.IceFillFloors[floorIndex][patternIndex]).toMutableList().let {
                        currentPatterns.addAll(it.map { startPosition.addVec(x = 0.5, y = 0.1, z = 0.5).add(transformTo(it, rotation)) })
                    }
                    return@forEachIndexed
                }
            }
            modMessage("§cFailed to scan floor ${floorIndex + 1}")
        }
    }

    private fun transform(x: Int, z: Int, rotation: Rotations): Vec2 {
        return when (rotation) {
            Rotations.NORTH -> Vec2(z, -x) // east
            Rotations.WEST -> Vec2(-x, -z) // north
            Rotations.SOUTH -> Vec2(-z, x) // west
            Rotations.EAST -> Vec2(x, z) // south
            else -> Vec2(x, z)
        }
    }

    private fun transformTo(vec: Vec3i, rotation: Rotations): Vec3 = with(transform(vec.x, vec.z, rotation)) {
        Vec3(x.toDouble(), vec.y.toDouble(), z.toDouble())
    }

    fun reset() {
        currentPatterns.clear()
    }

    fun BlockPos.add(vec: Vec2): BlockPos = this.add(vec.x, 0, vec.z)
}