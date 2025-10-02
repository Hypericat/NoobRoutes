package noobroutes.features.dungeon.puzzle

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
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
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.*
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.IceFillFloors
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import noobroutes.utils.skyblock.modMessage
import scala.reflect.runtime.Settings.IntSetting
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object IceFill: Module(
    "Auto Icefill",
    category = Category.DUNGEON,
    description = "Does Icefill"
) {

    private val packetMode by BooleanSetting("Packet Mode", default = false, description = "Scary")
    private val fastMode by BooleanSetting("Fast Mode", default = true, description = "Doesn't wait for server")
    private val fastModeDelay by NumberSetting("Fast Delay", default = 3, min = 1, max = 20, increment = 1, description = "Delay between blocks.").withDependency { fastMode }

    private val simulate by BooleanSetting("Simulate", default = false, description = "Simulates Ice Fill in singleplayer")
    private val ping by NumberSetting("Simulation Tick Delay", default = 0, description = "Adds x ticks of delay to simulation", min = 0, max = 50).withDependency { simulate }

    private var canceledKeys: List<KeyBinding>? = null;

    private var pathing: Boolean = false;
    private var stairTicks: Int = -1;
    private val startBlockPosRoom: BlockPos = BlockPos(0, 69, 8)
    private var tickCount = 0;

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

        val settings: GameSettings = Minecraft.getMinecraft().gameSettings;
        val list: MutableList<KeyBinding> = mutableListOf();
        list.add(settings.keyBindSneak)
        list.add(settings.keyBindJump)
        list.add(settings.keyBindForward)
        list.add(settings.keyBindBack)
        list.add(settings.keyBindRight)
        list.add(settings.keyBindLeft)
        list.add(settings.keyBindAttack)
        list.add(settings.keyBindUseItem)
        list.add(settings.keyBindSprint)
        canceledKeys = list;
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
        pathing = false;

        if (event.room == null || event.room.name != "Ice Fill" || currentPatterns.isNotEmpty()) return
        scanAllFloors(event.room.getRealCoords(startBlockPosRoom.up()).toVec3(), event.room.rotation)
    }

    override fun onEnable() {
        super.onEnable()
        this.pathing = false;
    }

    override fun onDisable() {
        super.onDisable()
        this.pathing = false;
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
                    //modMessage("Section $floorIndex scan took ${(System.nanoTime() - startTime) / 1000000.0}ms pattern: $patternIndex")

                    (IceFillFloors.IceFillFloors[floorIndex][patternIndex]).toMutableList().let {
                        currentPatterns.addAll(it.map { startPosition.addVec(x = 0.5, y = 0.1, z = 0.5).add(transformTo(it, rotation)) })
                    }
                    return@forEachIndexed
                }
            }
            modMessage("§cFailed to scan floor $floorIndex")
            //modMessage("§c" + "startpos : " + startPosition)
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

    fun path(lastPos: BlockPos) : Boolean {
        if (!Minecraft.getMinecraft().thePlayer.onGround) return false;

        if (!pathing) {
            modMessage("Solving Ice Fill!")
        }

        pathing = true;
        handleNextIndex(lastPos)
        return true;
    }

    @SubscribeEvent
    fun onSetBlock(event: BlockChangeEvent) {
        if (DungeonUtils.currentRoom?.name != "Ice Fill" || (fastMode && pathing)) return // pathing allows it to start pathing, because onTick check would be expensive

        val newBlock: IBlockState = event.update
        if (newBlock.block != Blocks.packed_ice || event.pos != Minecraft.getMinecraft().thePlayer.positionVector.toBlockPos().down()) return; // Check that the block is under the player

        if (!path(event.pos)) {
            Scheduler.schedulePreTickTask(1) {
                onSetBlock(event) // Schedule for next tick
            }
        }
    }

    private fun handleNextIndex(lastPos: BlockPos) {
        stairTicks = -1;
        if (currentPatterns.isEmpty()) return

        var index: Int = currentPatterns.indexOfFirst { it.toBlockPos().down() == lastPos }
        if (index == -1) {
            modMessage("Stopped Ice Fill, out of bounds!")
            pathing = false; // Invalid block
            return
        }

        if (++index >= currentPatterns.size) {
            // Index out of bounds reached end
            modMessage("Stopped Ice Fill, reached end!")
            pathing = false;
            return
        }

        PlayerUtils.unPressKeys();
        PlayerUtils.stopVelocity();

        val newPos: Vec3 = currentPatterns[index].subtract(0.0, 1.0, 0.0);

        if (newPos.toBlockPos().y != lastPos.y) {
            stairTicks = 2;
            return
        }

        if (Minecraft.getMinecraft().theWorld.getBlockState(newPos.toBlockPos()).block == Blocks.packed_ice) {
            this.pathing = false;
            modMessage("Stopped Ice Fill, next block filled!")
            return
        }

        Minecraft.getMinecraft().thePlayer.setPosition(newPos.xCoord, Minecraft.getMinecraft().thePlayer.posY, newPos.zCoord)
        if (!packetMode) return

        PacketUtils.sendPacket(C04PacketPlayerPosition(newPos.xCoord, Minecraft.getMinecraft().thePlayer.posY, newPos.zCoord, Minecraft.getMinecraft().thePlayer.onGround))
        Scheduler.scheduleC03Task(0, true) {  }
    }


    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (Minecraft.getMinecraft().thePlayer == null) return
        val playerBlock: BlockPos = Minecraft.getMinecraft().thePlayer.positionVector.toBlockPos().down();

        if (event.isEnd) {
            if (simulate && Minecraft.getMinecraft().isSingleplayer) simulate();
            return
        }

        // Event.start
        if (pathing) {
            if (fastMode) {
                if (++tickCount >= fastModeDelay) {
                    if (path(playerBlock)) {
                        tickCount = 0;
                    }
                }
            }

            handleVelocity()
            return
        }

        if (DungeonUtils.currentRoom != null && DungeonUtils.currentRoom!!.name == "Ice Fill" &&
            Minecraft.getMinecraft().theWorld.getBlockState(playerBlock).block == Blocks.ice &&
            playerBlock == DungeonUtils.currentRoom!!.getRealCoords(startBlockPosRoom)) {

            PlayerUtils.unPressKeys();
            PlayerUtils.stopVelocity()
        }
    }

    private fun handleVelocity() {
        if (stairTicks-- == 0) { // Stop velocity after going up stair
            PlayerUtils.stopVelocity()
            return
        }

        if (stairTicks <= -1 || DungeonUtils.currentRoom == null) return
        val vel: Vec2 = transform(1.0, 0.0, DungeonUtils.currentRoom!!.rotation);
        Minecraft.getMinecraft().thePlayer.setVelocity(vel.x, Minecraft.getMinecraft().thePlayer.motionY, vel.z)
    }

    private fun simulate() {
        val pos: BlockPos = Minecraft.getMinecraft().thePlayer.positionVector.toBlockPos().down();
        if (Minecraft.getMinecraft().theWorld.getBlockState(pos).block != Blocks.ice) return

        if (ping < 1) {
            setClientSideBlockPacket(pos, Blocks.packed_ice.defaultState)
            return
        }

        Scheduler.schedulePreTickTask(ping) {
            if (Minecraft.getMinecraft().theWorld.getBlockState(pos).block == Blocks.ice)
                setClientSideBlockPacket(pos, Blocks.packed_ice.defaultState)
        }
    }

    fun shouldCancelKey(keycode: Int) : Boolean {
        if (canceledKeys!!.any { it.keyCode == keycode })
            return this.enabled && Minecraft.getMinecraft().thePlayer != null && pathing && DungeonUtils.currentRoom != null && DungeonUtils.currentRoom!!.name == "Ice Fill";
        return false;
    }
}



// TODO
// clean up code