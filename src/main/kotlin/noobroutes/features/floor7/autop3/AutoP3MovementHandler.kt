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
    private var motionTicks = -1 //write setter
    private var jumping = false //write setter
    private var direction: Float? = null //write setter
    private var scale = 1f //write setter

    private var lastSpeed = 0.0

    const val DEFAULT_SPEED = 2.806
    private const val JUMP_SPEED = 6.0075
    private const val SPRINT_MULTIPLIER = 1.3

    private const val DRAG = 0.9063338661881611
    private const val PUSH = 0.036901383361851
    private const val TICK1 = 3.08
    private const val TICK2 = 1.99

    @SubscribeEvent //stolen from sy? (its just so good)
    fun handleWalking(event: MoveEntityWithHeadingEvent.Post) {
        if (mc.thePlayer?.onGround ?: return) {
            airTicks = 0
        }
        else airTicks++

        val dir = direction ?: return Keyboard.enableRepeatEvents(true)
        Keyboard.enableRepeatEvents(false)

        if (mc.thePlayer.isInWater || mc.thePlayer.isInLava || motionTicks != -1) return
        val speed = mc.thePlayer.aiMoveSpeed.toDouble()

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

        if (mc.thePlayer.onGround || (airTicks == 1 && mc.thePlayer.motionY < 0 && AutoP3.walkBoost != 0)) {
            movementFactor = speed;
            if (AutoP3.walkBoost == 2)
                movementFactor *= SPRINT_MULTIPLIER;
        }

        mc.thePlayer.motionX += movementFactor * Utils.xPart(dir)
        mc.thePlayer.motionZ += movementFactor * Utils.zPart(dir)
    }

    @SubscribeEvent(priority = EventPriority.LOW) //go after walking
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
            mc.thePlayer.jump();
            setVelocity(TICK1 * scale)
            return;
        }

        if (motionTicks == 0) {
            motionTicks = -1;
            modMessage("herp im midair") //yes herp
            return;
        }

        if (motionTicks == 1) {
            setVelocity(TICK2 * scale);
            lastSpeed = TICK2;
            return;
        }

        lastSpeed *= DRAG
        lastSpeed += PUSH
        setVelocity(lastSpeed * scale)
    }

    fun setVelocity(velo: Double) {
        val dir = direction ?: return
        mc.thePlayer.motionX = velo * mc.thePlayer.aiMoveSpeed * Utils.xPart(dir)
        mc.thePlayer.motionZ = velo * mc.thePlayer.aiMoveSpeed * Utils.zPart(dir)
    }

    @SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        if (direction == null) return
        val keyCode = Keyboard.getEventKey()
        if (!PlayerUtils.keyBindings.map { it.keyCode }.contains(keyCode)) return
        if (!Keyboard.getEventKeyState()) return

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