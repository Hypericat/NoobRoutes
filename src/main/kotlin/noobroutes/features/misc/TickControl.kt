package noobroutes.features.misc

import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.events.impl.MovePlayerEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.Simulation
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.settings.NotPersistent
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import java.util.Stack



@NotPersistent
object TickControl : Module("Tick Control", category = Category.MISC, description = "Makes the game tick on keypress instead of every 50ms.") {

    private data class PlayerState(val pos: Vec3, val velocity: Vec3, val ground: Boolean, val p3State: AutoP3MovementHandler.AutoP3MovementState, val lavaState: Simulation.SimulationLavaState)

    private val rewindTick by KeybindSetting("Rewind Tick", key = Keyboard.KEY_LEFT, description = "").onPress {
        rewindTick();
    }

    private val advanceTick by KeybindSetting("Advance Tick", key = Keyboard.KEY_RIGHT, description = "Triggers a game tick").onPress {
        advanceTick();
    }

    private var canTick = false
    private var tick = 0;
    private val stateStack = Stack<PlayerState>()

    override val canToggle: () -> Boolean
        get() = {mc.isSingleplayer}

    override fun onDisable() {
        stateStack.clear()
        super.onDisable()
    }

    fun tick(count: Int) {
        if (tick != 0) return modMessage("Current ticking already in progress!")
        tick = count;
    }

    private fun advanceTick() {
        if (!enabled)  {
            tick = 0;
            return modMessage("Tick Control Must Be Enabled")
        }
        if (!mc.isSingleplayer) {
            tick = 0;
            return modMessage("Must be in Single Player")
        }

        canTick = true
    }

    private fun rewindTick() {
        if (!enabled)  {
            tick = 0;
            return modMessage("Tick Control Must Be Enabled")
        }

        if (!mc.isSingleplayer) {
            tick = 0;
            return modMessage("Must be in Single Player")
        }

        if (stateStack.isEmpty()) {
            tick = 0;
            return modMessage("Can't Rewind Further")
        }

        val state = stateStack.pop()
        PlayerUtils.setPosition(state.pos)


        mc.thePlayer.onGround = state.ground
        PlayerUtils.setMotionVector(state.velocity)
        AutoP3MovementHandler.setState(state.p3State)
        Simulation.setSimulationLavaState(state.lavaState)
        AutoP3.handleRings(mc.thePlayer.positionVector)
    }

    /*
If this breaks with hclip it has something to do with event priority in the scheduler. idk.
 */

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.isEnd || tick == 0) return
        if (tick > 0) {
            tick--;
            advanceTick();
            return
        }

        tick++;
        rewindTick();
        return
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMoveEntityWithHeadingPre(event: MoveEntityWithHeadingEvent.Pre) {
        if (!mc.isSingleplayer) {
            disable()
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