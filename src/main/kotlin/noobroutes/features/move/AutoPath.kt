package noobroutes.features.move

import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.getDoorSpots
import noobroutes.features.dungeon.autobloodrush.AutoBloodRush.oneByOneSpots
import noobroutes.features.misc.EWPathfinderModule
import noobroutes.features.render.FreeCam
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoordsOdin
import org.lwjgl.input.Keyboard

object AutoPath: Module(
    name = "Auto Path",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "Automatically PathFinds to doors"
) {

    private val useAltKeys by BooleanSetting("Use Alt Key", true, false, "Use Alt Key to select door")
    private val altKey by KeybindSetting("Alt Key", Keyboard.KEY_SLASH, "Alt Key keybind", false).withDependency { this.useAltKeys }


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

        val doorSpots = if (room.data.name == "Entrance") oneByOneSpots.map { it.key to Pair(room.getRealCoordsOdin(it.value.first), room.getRealCoordsOdin(it.value.second)) } else
            getDoorSpots(room).map { it.key to Pair(room.getRealCoordsOdin(it.value.first), room.getRealCoordsOdin(it.value.second)) }

        if (key >= doorSpots.size || key < 0) {
            devMessage("Invalid index for pathing!")
            return
        }

        EWPathfinderModule.execute(doorSpots[key].second.second, true)
    }

    fun shouldCancelKey(keyCode: Int) : Boolean {
        return this.enabled && useAltKeys && altKey.key != Keyboard.KEY_NONE && Keyboard.isKeyDown(altKey.key) && validKeys!!.contains(keyCode)
    }
}