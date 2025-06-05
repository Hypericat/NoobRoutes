package noobroutes.features.dungeon.maplobotomizer

import net.minecraft.util.MathHelper
import net.minecraft.util.MovementInput
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent
import noobroutes.Core.mc
import noobroutes.utils.EntityPosition
import noobroutes.utils.RotationUtils
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.add
import noobroutes.utils.length
import noobroutes.utils.multiply
import kotlin.math.pow
import kotlin.math.sign

/**
 * Credit to FME
 */
object FreeCam {
    var freeCam = false
    var freeCamX: Double = 0.0
    var freeCamY: Double = 0.0
    var freeCamZ: Double = 0.0
    var freeCamYaw: Float = 0f
    var freeCamPitch: Float = 0f
    var forwardVelocity: Double = 0.0
    var leftVelocity: Double = 0.0
    var upVelocity: Double = 0.0
    var lastTime = System.nanoTime()
    private var oldNoClip = false
    private var lookVec: Vec3 = Vec3(0.0, 0.0, 0.0)
    private var oldCameraType: Int = 0
    private var shouldOverride = false
    private var oldInput: MovementInput = MovementInput()
    private val freeCamPosition: EntityPosition = EntityPosition(0.0, 0.0, 0.0, 0f, 0f)
    private val playerPosition: EntityPosition = EntityPosition(0.0, 0.0, 0.0, 0f, 0f)

    fun enable(){
        freeCam = true
        oldCameraType = mc.gameSettings.thirdPersonView
        oldInput = mc.thePlayer.movementInput
        mc.thePlayer.movementInput = MovementInput()
        mc.gameSettings.thirdPersonView = 0
        val viewEntity = mc.renderViewEntity
        val pos = viewEntity.getPositionEyes(1f)
        lookVec = RotationUtils.yawAndPitchVector(viewEntity.rotationYaw, viewEntity.rotationPitch)
        freeCamPosition.x = pos.xCoord + lookVec.xCoord * -1.5
        freeCamPosition.y = pos.yCoord + lookVec.yCoord * -1.5
        freeCamPosition.z = pos.zCoord + lookVec.zCoord * -1.5
        freeCamPosition.pitch = viewEntity.rotationPitch
        freeCamPosition.yaw = viewEntity.rotationYaw
        lastTime = System.nanoTime()
    }
    fun disable(){
        freeCam = false
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
        if (!freeCam || event.isEnd || !MapLobotomizer.enabled) return
        val currTime = System.nanoTime()
        val frameTime = ((currTime - lastTime).toFloat() / 1.0E9f).toDouble()
        lastTime = currTime
        val input = oldInput
        val forwardImpulse = input.moveForward.toDouble()
        val leftImpulse = input.moveStrafe.toDouble()
        val upImpulse = if (input.jump) 1.0 else 0.0 + if (input.sneak) -1.0 else 0.0
        forwardVelocity = calculateVelocity(forwardVelocity, forwardImpulse, frameTime)
        leftVelocity = calculateVelocity(leftVelocity, leftImpulse, frameTime)
        upVelocity = calculateVelocity(upVelocity, upImpulse, frameTime)
        val forward =
            if (MapLobotomizer.freeCamSpectatorMovement) lookVec else (Vec3(
                lookVec.xCoord,
                0.0,
                lookVec.zCoord
            )).normalize()
        val left = (Vec3(lookVec.zCoord, 0.0, -lookVec.xCoord)).normalize()

        var moveDelta: Vec3 = forward.multiply(forwardVelocity)
            .add(left.multiply(leftVelocity))
            .add(0.0, upVelocity, 0.0)
            .multiply(frameTime)
        val speed: Double = moveDelta.length / frameTime
        if (speed > 35.0) {
            val factor = 35.0 / speed
            forwardVelocity *= factor
            leftVelocity *= factor
            upVelocity *= factor
            moveDelta = moveDelta.multiply(factor)
        }
        var entityPosition: EntityPosition = freeCamPosition
        entityPosition.x = (entityPosition.x + moveDelta.xCoord)
        entityPosition = freeCamPosition
        entityPosition.y = (entityPosition.y + moveDelta.yCoord)
        entityPosition = freeCamPosition
        entityPosition.z = (entityPosition.z + moveDelta.zCoord)
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload){
        freeCam = false
    }

    @SubscribeEvent
    fun onClick(event: MouseEvent) {
        if (freeCam) event.isCanceled = true
    }

    fun onBeforeRenderWorld(){
        if (!freeCam) return
        shouldOverride = true
        val renderEntity = mc.renderViewEntity
        playerPosition.copyFromEntity(renderEntity, true)
        freeCamPosition.copyToEntity(renderEntity, false)
        oldNoClip = renderEntity.noClip
        renderEntity.noClip = true
    }

    fun onAfterRenderWorld(){

    }

    fun setAngles(yaw: Float, pitch: Float) {
        var p = pitch
        var y = yaw
        with(freeCamPosition) {
            p -= pitch * 0.15f
            y += yaw * 0.15f
            p = MathHelper.clamp_float(p, -90f, 90f)
            lookVec = RotationUtils.yawAndPitchVector(y, p)
        }
    }



    private fun calculateVelocity(velocity: Double, impulse: Double, frameTime: Double): Double {
        if (impulse == 0.0) return velocity * 0.05.pow(frameTime)
        var newVelocity = 20 * impulse * frameTime
        if (sign(impulse) == sign(velocity)) newVelocity += velocity
        return newVelocity
    }



}