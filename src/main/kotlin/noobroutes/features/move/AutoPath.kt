package noobroutes.features.move

import net.minecraft.client.Minecraft
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.getDoorSpots
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.getRoomDoors
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.oneByOneDoors
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.oneByOneSpots
import noobroutes.features.misc.EWPathfinderModule
import noobroutes.features.render.FreeCam
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.add
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeonScanning.DungeonUtils
import noobroutes.utils.skyblock.dungeonScanning.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.modMessage

import noobroutes.utils.toVec3
import org.lwjgl.input.Keyboard


object AutoPath: Module(
    name = "Auto Path",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "Automatically PathFinds to doors"
) {

    private val resetPos by BooleanSetting("Align to node", true, false, "Moves the player to the center of the node after pathing.")
    private val useAltKeys by BooleanSetting("Use Alt Key", true, false, "Use Alt Key to select door")
    private val altKey by KeybindSetting("Alt Key", Keyboard.KEY_PERIOD, "Alt Key keybind", false).withDependency { this.useAltKeys }
    private val doorNumberColor by ColorSetting("Door Number Color", description = "I wonder what this could possibly mean", default = Color.GREEN)



    private var validKeys: HashSet<Int>? = null;
    private var resetBlockPos: BlockPos? = null

    fun onInitKeys() {
        validKeys = HashSet()

        // Add all key IDs
        for (i in 0x02..0x09) {
            validKeys!!.add(i)
        }
        validKeys!!.add(0x0B)
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.isEnd || resetBlockPos == null) return
        if (!resetPos) {
            resetBlockPos = null
            return
        }
        if (!DynamicRoute.isInNode()) {
            Minecraft.getMinecraft().thePlayer.setPosition(resetBlockPos!!.x.toDouble() + 0.5, Minecraft.getMinecraft().thePlayer.posY, resetBlockPos!!.z.toDouble() + 0.5)
            resetBlockPos = null
        }
    }

    private fun getColor(pos: BlockPos) : Color {
        if (AutoBloodRush.isWitherDoor(pos)) return Color.GRAY
        if (AutoBloodRush.isBloodDoor(pos)) return Color.RED
        return doorNumberColor
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (AutoBloodRush.enabled && AutoBloodRush.editMode) return

        val room = DungeonUtils.currentRoom ?: return

        val doorPositions = if (room.name == "Entrance") oneByOneDoors.map { room.getRealCoords(it) } else getRoomDoors(room).map { room.getRealCoords(it) }
        doorPositions.forEachIndexed { index, pos ->
            Renderer.drawStringInWorld(index.toString(), pos.toVec3().add(0.5, 2.0, 0.5), getColor(pos), scale = 0.1f)
        }

        val doorSpots = if (room.name == "Entrance") oneByOneSpots.map { it.key to Pair(room.getRealCoords(it.value.first), room.getRealCoords(it.value.second)) } else
            getDoorSpots(room).map { it.key to Pair(room.getRealCoords(it.value.first), room.getRealCoords(it.value.second)) }

        doorSpots.forEach {
            Renderer.drawBlock(it.second.first, Color.RED, 3, 1, 0)
            Renderer.drawBlock(it.second.second, Color.BLUE, 3, 1, 0)
        }
    }



    @SubscribeEvent
    fun onKeyInput(event: KeyInputEvent) {
        if (!mc.thePlayer.onGround || FreeCam.enabled || !Keyboard.getEventKeyState()) return
        if (!useAltKeys || !Keyboard.isKeyDown(altKey.key)) return

        var key = Keyboard.getEventKey()
        if (!validKeys!!.contains(key)) return

        if (key == Keyboard.KEY_0) key = 0x01;

        pathToDoor(--key)
    }

    private fun pathToDoor(key: Int) {
        val room = DungeonUtils.currentRoom ?: return

        val doorSpots = if (room.name == "Entrance") oneByOneSpots.map { it.key to Pair(room.getRealCoords(it.value.first), room.getRealCoords(it.value.second)) } else
            getDoorSpots(room).map { it.key to Pair(room.getRealCoords(it.value.first), room.getRealCoords(it.value.second)) }

        if (key >= doorSpots.size || key < 0) {
            devMessage("Invalid index for pathing!")
            return
        }

        EWPathfinderModule.execute(doorSpots[key].second.second, true)
        if (resetPos) resetBlockPos = BlockPos(Minecraft.getMinecraft().thePlayer.positionVector);
    }

    fun shouldCancelKey(keyCode: Int) : Boolean {
        return this.enabled && useAltKeys && altKey.key != Keyboard.KEY_NONE && Keyboard.isKeyDown(altKey.key) && validKeys!!.contains(keyCode)
    }
}