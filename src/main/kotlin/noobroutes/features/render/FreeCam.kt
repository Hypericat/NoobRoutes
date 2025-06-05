package noobroutes.features.render

import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.Entity
import net.minecraft.util.MathHelper
import net.minecraft.util.MovementInput
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.utils.*
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.Utils.isStart
import noobroutes.utils.add
import noobroutes.utils.multiply
import noobroutes.utils.skyblock.devMessage
import kotlin.math.pow
import kotlin.math.sign

/**
 * Credit to FME
 */
object FreeCam : Module("Free Cam", description = "FME free cam", category = Category.RENDER) {
    private val freeCamSpectatorMovement by BooleanSetting("Spectator Movement", description = "Moving forward and backward in free cam mode changes y level.")
    var looking: MovingObjectPosition? = null
    private var forwardVelocity: Double = 0.0
    private var leftVelocity: Double = 0.0
    private var upVelocity: Double = 0.0
    private var lastTime = System.nanoTime()
    private var oldNoClip = false
    private var lookVec: MutableVec3 = MutableVec3(0.0, 0.0, 0.0)
    private var oldCameraType: Int = 0
    private var shouldOverride = false
    private var oldInput: MovementInput = MovementInput()
    private val freeCamPosition: EntityPosition = EntityPosition(0.0, 0.0, 0.0, 0f, 0f)
    private val playerPosition: EntityPosition = EntityPosition(0.0, 0.0, 0.0, 0f, 0f)
    private var renderingEntities = false

    override fun onEnable() {
        oldCameraType = mc.gameSettings.thirdPersonView
        oldInput = mc.thePlayer.movementInput
        mc.thePlayer.movementInput = MovementInput()
        mc.gameSettings.thirdPersonView = 0
        val viewEntity = mc.renderViewEntity
        val pos = viewEntity.getPositionEyes(1f)
        lookVec = RotationUtils.yawAndPitchVector(viewEntity.rotationYaw, viewEntity.rotationPitch).toMutableVec3()
        freeCamPosition.x = pos.xCoord + lookVec.x * -1.5
        freeCamPosition.y = pos.yCoord + lookVec.y * -1.5
        freeCamPosition.z = pos.zCoord + lookVec.z * -1.5
        freeCamPosition.pitch = viewEntity.rotationPitch
        freeCamPosition.yaw = viewEntity.rotationYaw
        lastTime = System.nanoTime()
        super.onEnable()
    }

    override fun onDisable() {
        super.onDisable()
        mc.gameSettings.thirdPersonView = oldCameraType
        mc.thePlayer.movementInput = oldInput
        shouldOverride = false
        forwardVelocity = 0.0
        leftVelocity = 0.0
        upVelocity = 0.0
        mc.renderGlobal.loadRenderers()
    }


    @SubscribeEvent
    fun onRenderTick(event: RenderTickEvent){
        if (event.isEnd) return
        val currTime = System.nanoTime()
        val frameTime = ((currTime - lastTime).toFloat() / 1.0E9f)
        lastTime = currTime
        val input = oldInput
        val forwardImpulse = input.moveForward
        val leftImpulse = input.moveStrafe
        val upImpulse = if (input.jump) 1 else 0 + if (input.sneak) -1 else 0
        forwardVelocity = calculateVelocity(forwardVelocity, forwardImpulse.toDouble(), frameTime.toDouble())
        leftVelocity = calculateVelocity(leftVelocity, leftImpulse.toDouble(), frameTime.toDouble())
        upVelocity = calculateVelocity(upVelocity, upImpulse.toDouble(), frameTime.toDouble())
        val forward =
            if (freeCamSpectatorMovement) lookVec else MutableVec3(
                lookVec.x,
                0.0,
                lookVec.z
            ).normalize()
        val left = MutableVec3(lookVec.z, 0.0, -lookVec.x).normalize()

        val moveDelta: MutableVec3 = forward.scale(forwardVelocity)
            .add(left.scale(leftVelocity))
            .add(0.0, upVelocity, 0.0)
            .scale(frameTime)
        val speed: Double = moveDelta.length / frameTime
        if (speed > 35.0) {
            val factor = 35.0 / speed
            forwardVelocity *= factor
            leftVelocity *= factor
            upVelocity *= factor
            moveDelta.scale(factor)
        }
       freeCamPosition.add(moveDelta)
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent){
        if (event.isStart) {
            //I am pretty sure this is to freeze your game while you are taking a screenshot. IDK why tho.
            while (mc.gameSettings.keyBindScreenshot.isPressed) { }
            oldInput.updatePlayerMoveState()
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload){
        onDisable()
    }

    @SubscribeEvent
    fun onClick(event: MouseEvent) {
        event.isCanceled = true
    }

    fun onBeforeRenderWorld(){
        if (!enabled) return
        shouldOverride = true
        val renderEntity = mc.renderViewEntity ?: return
        playerPosition.copyFromEntity(renderEntity, true)
        freeCamPosition.copyToEntity(renderEntity, false)
        oldNoClip = renderEntity.noClip
        renderEntity.noClip = true
    }

    fun onAfterRenderWorld(){
        if (shouldOverride) {
            looking = mc.objectMouseOver
            val cameraEntity = mc.renderViewEntity ?: return
            playerPosition.copyToEntity(cameraEntity, true)
            shouldOverride = false
            cameraEntity.noClip = oldNoClip
        }
    }

    fun onBeforeRenderEntity(entity: Entity){
        if (mc.renderViewEntity == entity && shouldOverride) {
            playerPosition.copyToEntity(entity, true)
        }
    }

    fun onAfterRenderEntity(entity: Entity){
        if (mc.renderViewEntity == entity && shouldOverride) {
            freeCamPosition.copyToEntity(entity, false)
        }
    }

    fun onBeforeRenderEntities(){
        renderingEntities = true
        if (shouldOverride) {
            mc.gameSettings.thirdPersonView = 1
        }
    }

    fun onAfterRenderEntities(){
        renderingEntities = false
        if (shouldOverride) mc.gameSettings.thirdPersonView = 0
    }

    fun shouldOverrideSpectator(player: AbstractClientPlayer): Boolean {
        return player == mc.renderViewEntity && shouldOverride && !renderingEntities

    }

    fun setAngles(yaw: Float, pitch: Float) {
        freeCamPosition.yaw += yaw * 0.15f
        freeCamPosition.pitch = MathHelper.clamp_float((freeCamPosition.pitch - pitch * 0.15f), -90f, 90f)
        lookVec = RotationUtils.yawAndPitchVector(freeCamPosition.yaw, freeCamPosition.pitch).toMutableVec3()
    }



    private fun calculateVelocity(velocity: Double, impulse: Double, frameTime: Double): Double {
        if (impulse == 0.0) return velocity * 0.05.pow(frameTime)
        var newVelocity = 20 * impulse * frameTime
        if (sign(impulse) == sign(velocity)) newVelocity += velocity
        return newVelocity
    }



}