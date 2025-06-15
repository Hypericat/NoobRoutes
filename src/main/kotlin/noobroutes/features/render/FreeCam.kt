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
import noobroutes.Core.logger
import noobroutes.events.impl.ClickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.NotPersistent
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.utils.*
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.Utils.isStart
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import kotlin.math.pow
import kotlin.math.sign

/**
 * Credit to FME
 */
@NotPersistent
object FreeCam : Module("Free Cam", description = "FME free cam", category = Category.RENDER) {
    val instantSlow by BooleanSetting("Instant Slow", description = "Sets the drag coefficient to 0 while no movement keys are pressed")

    var looking: MovingObjectPosition? = null
    private var speedVector = Vec3(0.0,0.0,0.0)
    private var oldPos = Vec3(0.0,0.0,0.0)
    private var pos = Vec3(0.0,0.0,0.0)
    private var lastTick = System.currentTimeMillis()
    private var oldNoClip = false
    private var lookVec: MutableVec3 = MutableVec3(0.0, 0.0, 0.0)
    private var oldCameraType: Int = 0
    private var shouldOverride = false
    private var oldInput: MovementInput = MovementInput()
    private var freeCamPosition: EntityPosition = EntityPosition(0.0, 0.0, 0.0, 0f, 0f)
    private val playerPosition: EntityPosition = EntityPosition(0.0, 0.0, 0.0, 0f, 0f)
    private var renderingEntities = false
    private var scrollWheelMultiplier = 2.0


    override fun onEnable() {
        speedVector = Vec3(0.0, 0.0, 0.0)
        oldCameraType = mc.gameSettings.thirdPersonView
        oldInput = mc.thePlayer.movementInput
        mc.thePlayer.movementInput = MovementInput()
        mc.gameSettings.thirdPersonView = 0
        val viewEntity = mc.renderViewEntity
        val eyePos = viewEntity.getPositionEyes(1f)
        lookVec = RotationUtils.yawAndPitchVector(viewEntity.rotationYaw, viewEntity.rotationPitch).toMutableVec3()
        val camPos = eyePos.subtract(Vec3(lookVec.x, lookVec.y, lookVec.z).multiply(1.5))
        pos = camPos
        oldPos = camPos
        freeCamPosition.x = camPos.xCoord
        freeCamPosition.y = camPos.yCoord
        freeCamPosition.z = camPos.zCoord
        freeCamPosition.pitch = viewEntity.rotationPitch
        freeCamPosition.yaw = viewEntity.rotationYaw
        scrollWheelMultiplier = 2.0
        super.onEnable()
    }

    override fun onDisable() {
        mc.gameSettings.thirdPersonView = oldCameraType
        mc.thePlayer.movementInput = oldInput
        shouldOverride = false
        speedVector = Vec3(0.0,0.0,0.0)
        mc.renderGlobal.loadRenderers()
        super.onDisable()
    }

    var lastTime = System.currentTimeMillis()
    @SubscribeEvent
    fun onRenderTick(event: RenderTickEvent){
        if (event.isEnd) return
        val partialTicks = event.renderTickTime
        val interpPos = oldPos.add(pos.subtract(oldPos).multiply(partialTicks))
        freeCamPosition.x = interpPos.xCoord
        freeCamPosition.y = interpPos.yCoord
        freeCamPosition.z = interpPos.zCoord
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastTime).toDouble() / 10000.0
        lastTime = currentTime
        val input = oldInput
        val yImpulse = (if (input.jump) 1 else 0) + (if (input.sneak) -1 else 0)
        val xImpulse = ((freeCamPosition.yaw.xPart * input.moveForward.sign) + (if (input.moveStrafe == 0f) 0.0 else (freeCamPosition.yaw + -90 * input.moveStrafe.sign).xPart)) * (if (input.sneak) 0.3 else 1.0)
        val zImpulse = ((freeCamPosition.yaw.zPart * input.moveForward.sign) + (if (input.moveStrafe == 0f) 0.0 else (freeCamPosition.yaw + -90 * input.moveStrafe.sign).zPart)) * (if (input.sneak) 0.3 else 1.0)

        val dragFactor = if (instantSlow && input.moveStrafe == 0f && input.moveForward == 0f) 0.0 else 0.4.pow(deltaTime * 20.0)
        val xSpeed = speedVector.xCoord * dragFactor + xImpulse * deltaTime * scrollWheelMultiplier
        val ySpeed = yImpulse * deltaTime * scrollWheelMultiplier * 50
        val zSpeed = speedVector.zCoord * dragFactor + zImpulse * deltaTime * scrollWheelMultiplier
        speedVector = Vec3(xSpeed, ySpeed, zSpeed)
        oldPos = pos
        pos = pos.add(speedVector)
    }

    @SubscribeEvent
    fun onMouseEvent(event: MouseEvent) {
        if (event.dwheel != 0) {
            event.isCanceled = true
            scrollWheelMultiplier *= if (event.dwheel.sign == 1) 1.1 else 0.9
            scrollWheelMultiplier = scrollWheelMultiplier.coerceIn(0.1..10.0)
        }
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
    fun onClick(event: ClickEvent.All) {
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
        freeCamPosition.yaw = MathHelper.wrapAngleTo180_float(((yaw * 0.15f) + freeCamPosition.yaw))
        freeCamPosition.pitch = MathHelper.clamp_float((freeCamPosition.pitch - pitch * 0.15f), -90f, 90f)
        lookVec = RotationUtils.yawAndPitchVector(freeCamPosition.yaw, freeCamPosition.pitch).toMutableVec3()

    }
}