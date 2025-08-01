package noobroutes.features.dungeon.puzzle

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister.Pack
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.events.impl.BlockChangeEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.render.ClickGUIModule
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.utils.*
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.Utils.isStart
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.IceFillFloors
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.Sys
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object IceFill: Module(
    "Auto Icefill",
    category = Category.DUNGEON,
    description = "Does Icefill"
) {

    private val simulate by BooleanSetting("Simulate", default = false, description = "Simulates Ice Fill in singleplayer").withDependency { ClickGUIModule.devMode }
    private val packetMode by BooleanSetting("Packet Mode", default = false, description = "Scary")

    private var pathing: Boolean = false;
    private var stairTicks: Int = -1;
    private val startBlockPosRoom: BlockPos = BlockPos(0, 69, -8)

    private var currentPatterns: ArrayList<Vec3> = ArrayList()
    private var representativeFloors: List<List<List<Int>>>
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val isr = this::class.java.getResourceAsStream("/icefillFloors.json")
        ?.let { InputStreamReader(it, StandardCharsets.UTF_8) }

    init {
        try {
            val text = isr?.readText()
            representativeFloors = gson.fromJson(text, object : TypeToken<List<List<List<Int>>>>() {}.type)
            System.err.println("No error loading ice fill floors")
            isr?.close()
        } catch (e: Exception) {
            System.err.println("Error loading ice fill floors")
            System.err.println(e.toString())
            representativeFloors = emptyList()
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (currentPatterns.isEmpty() || DungeonUtils.currentRoomName != "Ice Fill") return

        Renderer.draw3DLine(currentPatterns, color = Color.GREEN, depth = true)
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        reset();
    }

    @SubscribeEvent
    fun onRoomEnter(event: RoomEnterEvent) {
        if (event.room == null) {
            return
        }
        if (event.room.name != "Ice Fill" || currentPatterns.isNotEmpty()) return

        scanAllFloors(event.room.getRealCoords(startBlockPosRoom.up()).toVec3(), event.room.rotation)
        pathing = false;
    }

    private fun scanAllFloors(pos: Vec3, rotation: Rotations) {
        listOf(pos, pos.add(transformTo(Vec3i(5, 1, 0), rotation)), pos.add(transformTo(Vec3i(12, 2, 0), rotation))).forEachIndexed { floorIndex, startPosition ->
            val floorHeight = representativeFloors[floorIndex]
            val startTime = System.nanoTime()

            for (patternIndex in floorHeight.indices) {
                if (
                    isAir(BlockPos(startPosition).add(transform(floorHeight[patternIndex][0].toDouble(), floorHeight[patternIndex][1].toDouble(), rotation))) &&
                    !isAir(BlockPos(startPosition).add(transform(floorHeight[patternIndex][2].toDouble(), floorHeight[patternIndex][3].toDouble(), rotation)))
                ) {
                    modMessage("Section $floorIndex scan took ${(System.nanoTime() - startTime) / 1000000.0}ms pattern: $patternIndex")

                    (IceFillFloors.IceFillFloors[floorIndex][patternIndex]).toMutableList().let {
                        currentPatterns.addAll(it.map { startPosition.addVec(x = 0.5, y = 0.1, z = 0.5).add(transformTo(it, rotation)) })
                    }
                    return@forEachIndexed
                }
            }
            modMessage("§cFailed to scan floor $floorIndex")
        }
    }

    private fun transform(x: Double, z: Double, rotation: Rotations): Vec2 {
        return when (rotation) {
            Rotations.NORTH -> Vec2(z, -x) // east
            Rotations.WEST -> Vec2(-x, -z) // north
            Rotations.SOUTH -> Vec2(-z, x) // west
            Rotations.EAST -> Vec2(x, z) // south
            else -> Vec2(x, z)
        }
    }

    private fun transformTo(vec: Vec3i, rotation: Rotations): Vec3 = with(transform(vec.x.toDouble(), vec.z.toDouble(), rotation)) {
        Vec3(x, vec.y.toDouble(), z)
    }

    private fun reset() {
        currentPatterns.clear()
        pathing = false;
    }

    fun BlockPos.add(vec: Vec2): BlockPos = this.add(vec.x, 0.0, vec.z)

    @SubscribeEvent
    fun onSetBlock(event: BlockChangeEvent) {
        if (DungeonUtils.currentRoom == null || DungeonUtils.currentRoom!!.name != "Ice Fill") return
        val room: UniqueRoom = DungeonUtils.currentRoom!!;
        val newBlock: IBlockState = event.update
        if (newBlock.block != Blocks.packed_ice || event.pos != Minecraft.getMinecraft().thePlayer.positionVector.toBlockPos().down()) return; // Check that the block is under the player

        if (!pathing) {
            if (event.pos != room.getRealCoords(startBlockPosRoom)) return; // Check for start block
            pathing = true;
        }
        handleNextIndex(event.pos)
    }

    private fun handleNextIndex(lastPos: BlockPos) {
        stairTicks = -1;
        if (currentPatterns.isEmpty() || !Minecraft.getMinecraft().thePlayer.onGround) return
        var index: Int = currentPatterns.indexOfFirst { it.toBlockPos().down() == lastPos }
        if (index == -1) {
            pathing = false; // Invalid block
            return
        }

        if (++index >= currentPatterns.size) {
            // Index out of bounds reached end
            pathing = false
            return
        }

        PlayerUtils.unPressKeys();
        PlayerUtils.stopVelocity();

        val newPos: Vec3 = currentPatterns[index];

        if (newPos.toBlockPos().y - 1 != lastPos.y) {
            stairTicks = 2;
            return
        }

        Minecraft.getMinecraft().thePlayer.setPosition(newPos.xCoord, Minecraft.getMinecraft().thePlayer.posY, newPos.zCoord)
        if (!packetMode) return

        PacketUtils.sendPacket(C04PacketPlayerPosition(newPos.xCoord, Minecraft.getMinecraft().thePlayer.posY, newPos.zCoord, Minecraft.getMinecraft().thePlayer.onGround))
        Scheduler.scheduleC03Task(0, true) {  }
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.isStart && pathing && Minecraft.getMinecraft().thePlayer != null) {
            if (PlayerUtils.movementKeysPressed && stairTicks <= 0) {
                pathing = false; // Disable
                return
            }

            if (stairTicks-- > 0 ) {
                if (DungeonUtils.currentRoom != null) {
                    val vel: Vec2 = transform(1.0, 0.0, DungeonUtils.currentRoom!!.rotation);
                    Minecraft.getMinecraft().thePlayer.setVelocity(vel.x, Minecraft.getMinecraft().thePlayer.motionY, vel.z)
                }
            } else {
                PlayerUtils.stopVelocity();
            }
        }


        if (event.isEnd || !simulate || Minecraft.getMinecraft().thePlayer == null || !Minecraft.getMinecraft().isSingleplayer) return
            val pos: BlockPos = Minecraft.getMinecraft().thePlayer.positionVector.toBlockPos().down();
            if (Minecraft.getMinecraft().theWorld.getBlockState(pos).block == Blocks.ice) {
                Scheduler.schedulePreTickTask((Math.random() * 3).toInt() + 8) {
                    if (Minecraft.getMinecraft().theWorld.getBlockState(pos).block == Blocks.ice)
                        setClientSideBlockPacket(pos, Blocks.packed_ice.defaultState)
                }
            }
    }

}

// TODO
// make pathing be able to start from anywhere (adjust fail safe)
// stop velocity / keys upon walk over ice block client side dont wait for packet