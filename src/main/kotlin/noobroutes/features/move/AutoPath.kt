package noobroutes.features.move

import net.minecraft.client.Minecraft
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.misc.EWPathfinderModule
import noobroutes.features.render.FreeCam
import noobroutes.features.routes.DynamicRoute
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.*
import noobroutes.utils.*
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DoorPositions
import noobroutes.utils.skyblock.dungeon.DoorPositions.getDoorSpots
import noobroutes.utils.skyblock.dungeon.DoorPositions.getRoomDoors
import noobroutes.utils.skyblock.dungeon.DoorPositions.oneByOneDoors
import noobroutes.utils.skyblock.dungeon.DoorPositions.oneByOneSpots
import noobroutes.utils.skyblock.dungeon.Dungeon
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import org.lwjgl.input.Keyboard


object AutoPath: Module(
    name = "Auto Path",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "Automatically PathFinds to doors"
) {

    private val resetPos by BooleanSetting("Align to node", true, false, "Moves the player to the center of the node after pathing.")
    private val selectionMode by SelectorSetting("Selection Mode", "Rotation", arrayListOf("Rotation", "Alt Key"), false,"Either use Alt Key or Angle to Select Door")
    private val altKey by KeybindSetting("Alt Key", Keyboard.KEY_NONE, "Alt Key keybind", false).withDependency { this.selectionMode == 1}
    private val pathKey by KeybindSetting("Path Key", Keyboard.KEY_NONE, "Path to selected door", false).withDependency { this.selectionMode == 0}
    private val doorNumberColor by ColorSetting("Door Number Color", description = "I wonder what this could possibly mean", default = Color.ORANGE)
    private val selectedDoorColor by ColorSetting("Selected Door Color", description = "I wonder what this could possibly mean", default = Color.GREEN).withDependency { this.selectionMode == 0 }

    private const val DOOR_POS_BITMASK : Int = 0b111.inv();
    private const val MIN_DOT_THRESHOLD : Double = 0.95;

    private var validDoors: MutableList<BlockPos> = mutableListOf();
    private var validBlocks: MutableList<Pair<BlockPos, BlockPos>> = mutableListOf();

    private var validDoorLookIndex: Int = -1;

    private var validKeys: HashSet<Int>? = null;

    fun onInitKeys() {
        validKeys = HashSet()

        // Add all key IDs
        for (i in 0x02..0x09) {
            validKeys!!.add(i)
        }
        validKeys!!.add(0x0B)
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.isEnd) return
        updateLookDoorIndex();
    }

    private fun getDoorColor(pos: BlockPos, default: Color) : Color {
        if (DoorPositions.isWitherDoor(pos)) return Color.GRAY
        if (DoorPositions.isBloodDoor(pos)) return Color.RED
        return default;
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        validDoors.forEachIndexed { index, pos ->
            Renderer.drawStringInWorld(index.toString(), pos.toVec3().add(0.5, 2.0, 0.5), if (index == validDoorLookIndex && selectionMode == 0) selectedDoorColor else getDoorColor(pos, doorNumberColor), scale = 0.1f)
        }

        validBlocks.forEachIndexed { index, it ->
            Renderer.drawBlock(it.first, getDoorColor(validDoors[index], Color.GREEN), 3, 1, 0)
            Renderer.drawBlock(it.second, DynamicRoute.dynColor, 3, 1, 0)
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        validDoors.clear();
        validBlocks.clear();
    }

    private fun getClosestPointInBox(eye: Vec3, box: AxisAlignedBB): Vec3 {
        val x = eye.xCoord.coerceIn(box.minX, box.maxX)
        val y = eye.yCoord.coerceIn(box.minY, box.maxY)
        val z = eye.zCoord.coerceIn(box.minZ, box.maxZ)
        return Vec3(x, y, z)
    }

    private fun updateLookDoorIndex() {
        validDoorLookIndex = -1;
        if (validDoors.isEmpty()) return;

        val lookVec: Vec3 = Minecraft.getMinecraft().thePlayer.getLook(1.0f).normalize();
        val eyePos: Vec3 = Minecraft.getMinecraft().thePlayer.getPositionEyes(1.0f);
        var bestDot: Double = Double.MIN_VALUE;

        val box: AxisAlignedBB = AxisAlignedBB(BlockPos.ORIGIN, BlockPos.ORIGIN).expand(1.0, 1.0, 1.0); // Using a box so we can look anywhere inside the door frame (needed at close ranges)

        for (i in 0..< validDoors.size) {
            val doorPos = validDoors[i].add(0.5, 2.0, 0.5);

            val dot: Double = lookVec.dotProduct(getClosestPointInBox(eyePos, box.offset(doorPos.toVec3())).subtract(eyePos).normalize())
            if (dot > bestDot && dot > MIN_DOT_THRESHOLD) {
                bestDot = dot;
                validDoorLookIndex = i;
            }
        }
    }

    private fun roundToNearest(value: Int): Int {
        return (value + 4) and DOOR_POS_BITMASK;
    }

    private fun updateDoors() {
        val room = DungeonUtils.currentRoom ?: return

        validDoors.clear();
        validBlocks.clear();

        for (doorPosition in if (room.name == "Entrance") oneByOneDoors.map { room.getRealCoords(it) } else getRoomDoors(room).map { room.getRealCoords(it) }) {
            if (!Dungeon.Info.dungeonList.any {it.x == doorPosition.x && it.z == doorPosition.z}) continue;
            validDoors.add(doorPosition);
        }

        if (validDoors.isEmpty()) return;

        for (pair in if (room.name == "Entrance") {
            oneByOneSpots.map { it.key to Pair(room.getRealCoords(it.value.first), room.getRealCoords(it.value.second)) }
        } else {
            getDoorSpots(room).map { it.key to Pair(room.getRealCoords(it.value.first), room.getRealCoords(it.value.second)) }
        }) {
            val px = roundToNearest(pair.second.first.x)
            val pz = roundToNearest(pair.second.first.z)

            if (!Dungeon.Info.dungeonList.any {
                    roundToNearest(it.x) == px && roundToNearest(it.z) == pz
                }) continue

            validBlocks.add(pair.second)
        }
    }

    @SubscribeEvent
    fun onRoomEnterEvent(event: RoomEnterEvent) {
        updateDoors();
    }

    @SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        if (!mc.thePlayer.onGround || FreeCam.enabled || !Keyboard.getEventKeyState()) return
        if (selectionMode == 0) {
            if (Keyboard.getEventKey() == pathKey.key) {
                pathToDoor(validDoorLookIndex)
            }
            return;
        }

        if (selectionMode != 1 || !Keyboard.isKeyDown(altKey.key)) return

        var key = Keyboard.getEventKey()
        if (!validKeys!!.contains(key)) return

        if (key == Keyboard.KEY_0) key = 0x01;
        pathToDoor(--key)
    }

    private fun resetPos(pos: BlockPos) {
        if (!resetPos || !Minecraft.getMinecraft().thePlayer.onGround) return;
        if (DynamicRoute.getInNodesWithoutUpdate().any {it.isValid(Minecraft.getMinecraft().thePlayer.positionVector, it.target)}) return; // Player is in a valid node

        Minecraft.getMinecraft().thePlayer.setPosition(pos.x.toDouble() + 0.5, Minecraft.getMinecraft().thePlayer.posY, pos.z.toDouble() + 0.5)
        devMessage("Reset pos!")
    }

    private fun pathToDoor(key: Int) {
        if (key >= validBlocks.size || key < 0) {
            devMessage("Invalid index for pathing!")
            return
        }

        PlayerUtils.unPressKeys();
        PlayerUtils.stopVelocity();
        EWPathfinderModule.execute(validBlocks[key].second, true, if (resetPos) Runnable { resetPos(BlockPos(Minecraft.getMinecraft().thePlayer.positionVector)); } else null)
    }

    fun shouldCancelKey(keyCode: Int) : Boolean {
        return this.enabled && selectionMode == 1 && altKey.key != Keyboard.KEY_NONE && Keyboard.isKeyDown(altKey.key) && validKeys!!.contains(keyCode)
    }
}