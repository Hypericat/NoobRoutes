package noobroutes.features.misc

import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.events.impl.MovePlayerEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.Simulation
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.settings.NotPersistent
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import java.util.Stack



@NotPersistent
object TickControl : Module("Tick Control", category = Category.MISC, description = "Makes the game tick on keypress instead of every 50ms.") {

    private data class PlayerState(val pos: Vec3, val velocity: Vec3, val ground: Boolean, val p3State: AutoP3MovementHandler.AutoP3MovementState, val lavaState: Simulation.SimulationLavaState)

    private val rewindTick by KeybindSetting("Rewind Tick", key = Keyboard.KEY_LEFT, description = "").onPress {
        if (!enabled) return@onPress modMessage("Tick Control Must Be Enabled")
        if (stateStack.isEmpty()) return@onPress modMessage("Can't Rewind Further")
        val state = stateStack.pop()
        PlayerUtils.setPosition(state.pos)


        mc.thePlayer.onGround = state.ground
        PlayerUtils.setMotionVector(state.velocity)
        AutoP3MovementHandler.setState(state.p3State)
        Simulation.setSimulationLavaState(state.lavaState)
        AutoP3.handleRings(mc.thePlayer.positionVector)
    }

    private val advanceTick by KeybindSetting("Advance Tick", key = Keyboard.KEY_RIGHT, description = "Triggers a game tick").onPress {
        if (!enabled) return@onPress modMessage("Tick Control Must Be Enabled")
        canTick = true
    }
    private var canTick = false
    private val stateStack = Stack<PlayerState>()

    override fun onEnable() {
        if (!mc.isSingleplayer) return
        super.onEnable()
    }

    override fun onDisable() {
        stateStack.clear()
        super.onDisable()
    }

    /*
If this breaks with hclip it has something to do with event priority in the scheduler. idk.
 */

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMoveEntityWithHeadingPre(event: MoveEntityWithHeadingEvent.Pre) {
        if (!mc.isSingleplayer) {
            toggle()
            return
        }
        if (canTick) {
            stateStack.add(
                PlayerState(
                    mc.thePlayer.positionVector,
                    PlayerUtils.getMotionVector(),
                    mc.thePlayer.onGround,
                    AutoP3MovementHandler.getState(),
                    Simulation.getSimulationLavaState()
                )
            )
            return
        }
        event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMoveEntityWithHeadingPost(event: MoveEntityWithHeadingEvent.Post) {
        if (canTick) {
            canTick = false
            return
        }
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onMovePlayer(event: MovePlayerEvent) {
        if (canTick) return
        event.isCanceled = true
    }

}