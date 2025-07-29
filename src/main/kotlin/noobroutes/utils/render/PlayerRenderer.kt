package noobroutes.utils.render

import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RenderPlayer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import noobroutes.Core.mc
import noobroutes.utils.RotationUtils
import noobroutes.utils.skyblock.PlayerUtils
import org.lwjgl.opengl.GL11
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class AnimationGenerator {
    private var lastLimbSwing = 0f
    private var lastLimbSwingAmount = 0f
    private var lastSwingProgress = 0f
    private var lastTickCount = 0
    private var isSwinging = false
    private var swingStartTick = 0

    fun generateAnimationFromMovement(
        currentX: Double, currentZ: Double,
        prevX: Double, prevZ: Double,
        tempPlayer: EntityOtherPlayerMP

    ) {
        val deltaX = currentX - prevX
        val deltaZ = currentZ - prevZ
        val distanceMoved = sqrt(deltaX * deltaX + deltaZ * deltaZ).toFloat()

        // Generate walking animation
        generateWalkingAnimation(distanceMoved)

        // Copy to entity
        applyAnimationToEntity(tempPlayer)
    }
    private fun generateWalkingAnimation(
        distanceMoved: Float
    ) {
        val walkingSpeed = 0.6f
        val isMoving = distanceMoved > 0.001f

        if (isMoving) {
            lastLimbSwing += distanceMoved * walkingSpeed
            lastLimbSwingAmount = min(lastLimbSwingAmount + 0.15f, 1.0f)
        } else {
            lastLimbSwingAmount = max(lastLimbSwingAmount - 0.15f, 0.0f)
        }

        lastLimbSwingAmount = MathHelper.clamp_float(lastLimbSwingAmount, 0.0f, 1.0f)
    }

    private fun generateArmSwingAnimation(tickCount: Int): Float {
        val swingDuration = 6

        if (isSwinging) {
            val swingTicks = tickCount - swingStartTick
            if (swingTicks >= swingDuration) {
                isSwinging = false
                return 0f
            }

            val progress = swingTicks.toFloat() / swingDuration.toFloat()
            return sin(progress * PI.toFloat())
        }

        return 0f
    }

    private fun applyAnimationToEntity(tempPlayer: EntityOtherPlayerMP) {
        tempPlayer.limbSwing = lastLimbSwing
        tempPlayer.limbSwingAmount = lastLimbSwingAmount
        tempPlayer.prevLimbSwingAmount = max(0f, lastLimbSwingAmount - 0.15f)

        val swingProgress = generateArmSwingAnimation(mc.theWorld.totalWorldTime.toInt())
        tempPlayer.swingProgress = swingProgress
        tempPlayer.prevSwingProgress = max(0f, swingProgress - 0.1f)
        tempPlayer.isSwingInProgress = swingProgress > 0f

        tempPlayer.onGround = true
        tempPlayer.ticksExisted = mc.theWorld.totalWorldTime.toInt()
    }

    fun triggerArmSwing() {
        isSwinging = true
        swingStartTick = mc.theWorld.totalWorldTime.toInt()
    }
}

class MovementRenderer {
    private val animationGenerator = AnimationGenerator()


    fun renderPlayerAt(
        currentX: Double, currentY: Double, currentZ: Double,
        prevX: Double, prevY: Double, prevZ: Double,
        partialTicks: Float
    ) {

        val (yaw, pitch) = RotationUtils.getYawAndPitchOrigin(prevX, prevY, prevZ, currentX, currentY + PlayerUtils.STAND_EYE_HEIGHT, currentZ)

        val xDiff = currentX - prevX
        val yDiff = currentY - prevY
        val zDiff = currentZ - prevZ

        val xPos = prevX + xDiff * partialTicks
        val yPos = prevY + yDiff * partialTicks
        val zPos = prevZ + zDiff * partialTicks

        val renderManager = mc.renderManager
        val player = mc.thePlayer

        GlStateManager.pushAttrib()
        GlStateManager.pushMatrix()

        val renderX = xPos - renderManager.viewerPosX
        val renderY = yPos - renderManager.viewerPosY
        val renderZ = zPos - renderManager.viewerPosZ

        GlStateManager.translate(renderX, renderY, renderZ)
        GlStateManager.enableLighting()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.8f)

        try {
            val playerRenderer = renderManager.getEntityRenderObject<EntityPlayer>(player) as RenderPlayer
            val tempPlayer = EntityOtherPlayerMP(player.worldObj, player.gameProfile)
            tempPlayer.setPosition(0.0, 0.0, 0.0)

            for (i in 0..4) {
                tempPlayer.setCurrentItemOrArmor(i, player.getEquipmentInSlot(i))
            }
            tempPlayer.rotationYaw = yaw
            tempPlayer.rotationYawHead = yaw
            tempPlayer.rotationPitch = pitch

            animationGenerator.generateAnimationFromMovement(
                currentX, currentZ,
                prevX, prevZ,
                tempPlayer
            )

            playerRenderer.doRender(tempPlayer, 0.0, 0.0, 0.0, 0.0f, partialTicks)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        GlStateManager.popMatrix()
        GlStateManager.popAttrib()
    }
}

fun generateSimpleWalkingAnimation(
    distanceMoved: Float,
    worldTime: Long,
    partialTicks: Float,
    tempPlayer: EntityOtherPlayerMP
) {
    val time = (worldTime + partialTicks) * 0.1f
    val isMoving = distanceMoved > 0.001f

    if (isMoving) {

        tempPlayer.limbSwing = time * 1.5f // Speed of leg swing
        tempPlayer.limbSwingAmount = min(1.0f, distanceMoved * 10f) // Intensity based on speed


        tempPlayer.swingProgress = abs(sin(time * 2f)) * 0.3f
        tempPlayer.isSwingInProgress = tempPlayer.swingProgress > 0.1f
    } else {

        tempPlayer.limbSwing = time * 0.2f
        tempPlayer.limbSwingAmount = 0.1f
        tempPlayer.swingProgress = 0f
        tempPlayer.isSwingInProgress = false
    }


    tempPlayer.prevLimbSwingAmount = tempPlayer.limbSwingAmount * 0.9f
    tempPlayer.prevSwingProgress = tempPlayer.swingProgress * 0.9f
}

fun generateTimeBasedAnimation(tempPlayer: EntityOtherPlayerMP, partialTicks: Float) {
    val time = (System.currentTimeMillis() / 50f + partialTicks) // 50ms per tick

    tempPlayer.limbSwing = time * 0.6667f // Default walking speed
    tempPlayer.limbSwingAmount = 0.8f // Moderate swing amount
    tempPlayer.prevLimbSwingAmount = 0.7f


    tempPlayer.swingProgress = (sin(time * 2f) + 1f) * 0.1f // 0 to 0.2
    tempPlayer.prevSwingProgress = tempPlayer.swingProgress * 0.9f

    tempPlayer.onGround = true
    tempPlayer.ticksExisted = (time).toInt()
}
