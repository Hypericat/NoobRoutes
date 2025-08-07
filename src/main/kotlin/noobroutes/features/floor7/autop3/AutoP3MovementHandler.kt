package noobroutes.features.floor7.autop3

import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import noobroutes.Core.mc
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.events.impl.S08Event
import noobroutes.utils.Utils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard

object AutoP3MovementHandler {
    private var airTicks = 0
    private var motionTicks = -1
    private var jumping = false
    private var direction: Float? = null
    private var scale = 1f

    private var lastSpeed = 0.0

    const val DEFAULT_SPEED = 2.806
    private const val JUMP_SPEED = 6.0075
    private const val SPRINT_MULTIPLIER = 1.3

    private const val DRAG = 0.9063338661881611
    private const val PUSH = 0.036901383361851

    private const val TICK1 = 6.16
    private const val TICK2 = 3.98

    @SubscribeEvent //stolen from sy? (its just so good)
    fun handleWalking(event: MoveEntityWithHeadingEvent.Post) {
        if (mc.thePlayer == null) return
        if (mc.thePlayer.onGround) {
            airTicks = 0
        }
        else airTicks++

        val dir = direction ?: return

        if (mc.thePlayer.isInWater || mc.thePlayer.isInLava || motionTicks != -1) return
        val speed = PlayerUtils.getPlayerWalkSpeed().toDouble()

        if (airTicks == 0) {
            var speedMultiplier = DEFAULT_SPEED
            if (jumping) {
                jumping = false
                speedMultiplier = JUMP_SPEED
            }
            mc.thePlayer.motionX = Utils.xPart(dir) * speed * speedMultiplier
            mc.thePlayer.motionZ = Utils.zPart(dir) * speed * speedMultiplier
            return
        }

        var movementFactor = 0.02 * SPRINT_MULTIPLIER;

        if (mc.thePlayer.onGround || (airTicks == 1 && mc.thePlayer.motionY < 0 && AutoP3.walkBoost != "none")) {
            movementFactor = speed;
            if (AutoP3.walkBoost == "big")
                movementFactor *= SPRINT_MULTIPLIER;
        }

        mc.thePlayer.motionX += movementFactor * Utils.xPart(dir)
        mc.thePlayer.motionZ += movementFactor * Utils.zPart(dir)
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun doMotioning(event: MoveEntityWithHeadingEvent.Post) {
        if (direction == null || motionTicks == -1) return;

        doMotionTick();
        motionTicks++

        if (mc.thePlayer.onGround && motionTicks > 1) {
            setVelocity(DEFAULT_SPEED)
            motionTicks = -1
        }
    }

    private fun doMotionTick() {
        if (motionTicks == 0 && mc.thePlayer.onGround) {
            setVelocity(TICK1 * scale)
            return;
        }

        if (motionTicks == 0) {
            motionTicks = -2 //fuck u hyper
            direction = null
            modMessage("herp im midair") //yes herp
            return;
        }

        if (motionTicks == 1) {
            setVelocity(TICK2 * scale)
            lastSpeed = TICK2 * PlayerUtils.getPlayerWalkSpeed() //wrong but no fix rn
            return
        }

        lastSpeed *= DRAG
        lastSpeed += PUSH
        setSpeed(lastSpeed * scale)
    }

    private fun setSpeed(speed: Double) {
        val dir = direction ?: return
        mc.thePlayer.motionX = Utils.xPart(dir) * speed
        mc.thePlayer.motionZ = Utils.zPart(dir) * speed
    }

    fun setVelocity(velo: Double) {
        val dir = direction ?: return
        mc.thePlayer.motionX = velo * PlayerUtils.getPlayerWalkSpeed() * Utils.xPart(dir)
        mc.thePlayer.motionZ = velo * PlayerUtils.getPlayerWalkSpeed() * Utils.zPart(dir)
    }

    @SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        if (!Keyboard.getEventKeyState()) return
        val keyCode = Keyboard.getEventKey()
        if (!PlayerUtils.keyBindings.map { it.keyCode }.contains(keyCode) && keyCode != mc.gameSettings.keyBindSneak.keyCode) return

        AutoP3.waitedTicks = 0

        if (!AutoP3.x_y0uMode) AutoP3.cancelled = 0

        if (direction == null) return

        PlayerUtils.stopVelocity()
        resetShit()
    }

    @SubscribeEvent
    fun onS08(event: S08Event) {
        resetShit()
    }

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        resetShit()
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        resetShit()
    }

    fun resetShit() {
        motionTicks = -1
        jumping = false
        direction = null
    }


    fun setMotionTicks(ticks: Int) {
        motionTicks = ticks
    }

    fun setJumpingTrue() {
        jumping = true
    }

    fun setDirection(dir: Float?) {
        direction = dir
    }

    fun setScale(value: Float) {
        scale = value
    }
}