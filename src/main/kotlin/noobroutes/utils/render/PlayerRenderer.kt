package noobroutes.utils.render

import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RenderPlayer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import noobroutes.Core.mc
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
        currentX: Double, currentY: Double, currentZ: Double,
        prevX: Double, prevY: Double, prevZ: Double,
        partialTicks: Float,
        tempPlayer: EntityOtherPlayerMP,
        tickCount: Int = mc.theWorld.totalWorldTime.toInt()

    ) {
        val deltaX = currentX - prevX
        val deltaZ = currentZ - prevZ
        val distanceMoved = sqrt(deltaX * deltaX + deltaZ * deltaZ).toFloat()

        // Generate walking animation
        generateWalkingAnimation(distanceMoved, partialTicks, tempPlayer, tickCount)

        // Generate idle animations
        generateIdleAnimations(tempPlayer, tickCount, partialTicks)

        // Copy to entity
        applyAnimationToEntity(tempPlayer, partialTicks)
    }
    private fun generateWalkingAnimation(
        distanceMoved: Float,
        partialTicks: Float,
        tempPlayer: EntityOtherPlayerMP,
        tickCount: Int
    ) {
        // Minecraft's walking animation calculation
        val walkingSpeed = 0.6f
        val isMoving = distanceMoved > 0.001f

        if (isMoving) {
            // Advance limb swing based on distance moved
            // Minecraft uses distanceWalkedModified which accumulates
            lastLimbSwing += distanceMoved * walkingSpeed

            // Increase swing amount when moving
            lastLimbSwingAmount = min(lastLimbSwingAmount + 0.15f, 1.0f)
        } else {
            // Decrease swing amount when not moving
            lastLimbSwingAmount = max(lastLimbSwingAmount - 0.15f, 0.0f)
        }

        // Clamp limb swing amount
        lastLimbSwingAmount = MathHelper.clamp_float(lastLimbSwingAmount, 0.0f, 1.0f)
    }

    private fun generateIdleAnimations(
        tempPlayer: EntityOtherPlayerMP,
        tickCount: Int,
        partialTicks: Float
    ) {
        // Generate subtle idle movements using sine waves
        val time = (tickCount + partialTicks) * 0.1f

        // Subtle head bob
        val headBob = sin(time * 0.5f) * 0.5f
        tempPlayer.rotationPitch += headBob

        // Breathing-like animation (could affect model scaling if supported)
        val breathingCycle = sin(time * 0.3f) * 0.02f + 1.0f
    }

    private fun generateArmSwingAnimation(tickCount: Int, partialTicks: Float): Float {
        val swingDuration = 6 // ticks for full swing

        if (isSwinging) {
            val swingTicks = tickCount - swingStartTick
            if (swingTicks >= swingDuration) {
                isSwinging = false
                return 0f
            }

            // Smooth swing progress (0 to 1 and back to 0)
            val progress = swingTicks.toFloat() / swingDuration.toFloat()
            return sin(progress * PI.toFloat()) // Sine wave for smooth swing
        }

        return 0f
    }

    private fun applyAnimationToEntity(tempPlayer: EntityOtherPlayerMP, partialTicks: Float) {
        // Apply walking animation
        tempPlayer.limbSwing = lastLimbSwing
        tempPlayer.limbSwingAmount = lastLimbSwingAmount
        tempPlayer.prevLimbSwingAmount = max(0f, lastLimbSwingAmount - 0.15f)

        // Apply arm swing
        val swingProgress = generateArmSwingAnimation(mc.theWorld.totalWorldTime.toInt(), partialTicks)
        tempPlayer.swingProgress = swingProgress
        tempPlayer.prevSwingProgress = max(0f, swingProgress - 0.1f)
        tempPlayer.isSwingInProgress = swingProgress > 0f

        // Set other animation-related properties
        tempPlayer.onGround = true // Assume on ground for walking animation
        tempPlayer.ticksExisted = mc.theWorld.totalWorldTime.toInt()
    }

    // Trigger arm swing manually
    fun triggerArmSwing() {
        isSwinging = true
        swingStartTick = mc.theWorld.totalWorldTime.toInt()
    }
}

// Usage in your render function
class MovementRenderer {
    private val animationGenerator = AnimationGenerator()

    fun renderPlayerAt(
        currentX: Double, currentY: Double, currentZ: Double,
        prevX: Double, prevY: Double, prevZ: Double,
        partialTicks: Float
    ) {
        val renderManager = mc.renderManager
        val player = mc.thePlayer

        GlStateManager.pushAttrib()
        GlStateManager.pushMatrix()

        val renderX = currentX - renderManager.viewerPosX
        val renderY = currentY - renderManager.viewerPosY
        val renderZ = currentZ - renderManager.viewerPosZ

        GlStateManager.translate(renderX, renderY, renderZ)
        GlStateManager.enableLighting()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.8f)

        try {
            val playerRenderer = renderManager.getEntityRenderObject<EntityPlayer>(player) as RenderPlayer
            val tempPlayer = EntityOtherPlayerMP(player.worldObj, player.gameProfile)
            tempPlayer.setPosition(0.0, 0.0, 0.0)

            // Copy basic appearance
            for (i in 0..4) {
                tempPlayer.setCurrentItemOrArmor(i, player.getEquipmentInSlot(i))
            }
            tempPlayer.rotationYaw = player.rotationYaw
            tempPlayer.rotationYawHead = player.rotationYawHead
            tempPlayer.rotationPitch = player.rotationPitch

            // Generate animation from movement
            animationGenerator.generateAnimationFromMovement(
                currentX, currentY, currentZ,
                prevX, prevY, prevZ,
                partialTicks,
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

// Alternative: Simpler approach using just math functions
fun generateSimpleWalkingAnimation(
    distanceMoved: Float,
    worldTime: Long,
    partialTicks: Float,
    tempPlayer: EntityOtherPlayerMP
) {
    val time = (worldTime + partialTicks) * 0.1f
    val isMoving = distanceMoved > 0.001f

    if (isMoving) {
        // Walking animation using sine waves
        tempPlayer.limbSwing = time * 1.5f // Speed of leg swing
        tempPlayer.limbSwingAmount = min(1.0f, distanceMoved * 10f) // Intensity based on speed

        // Add some arm swing when walking
        tempPlayer.swingProgress = abs(sin(time * 2f)) * 0.3f
        tempPlayer.isSwingInProgress = tempPlayer.swingProgress > 0.1f
    } else {
        // Idle animation
        tempPlayer.limbSwing = time * 0.2f // Slow idle movement
        tempPlayer.limbSwingAmount = 0.1f // Minimal movement
        tempPlayer.swingProgress = 0f
        tempPlayer.isSwingInProgress = false
    }

    // Set previous values for smooth interpolation
    tempPlayer.prevLimbSwingAmount = tempPlayer.limbSwingAmount * 0.9f
    tempPlayer.prevSwingProgress = tempPlayer.swingProgress * 0.9f
}

// Even simpler: Just use time-based animation
fun generateTimeBasedAnimation(tempPlayer: EntityOtherPlayerMP, partialTicks: Float) {
    val time = (System.currentTimeMillis() / 50f + partialTicks) // 50ms per tick

    // Simple walking cycle
    tempPlayer.limbSwing = time * 0.6667f // Default walking speed
    tempPlayer.limbSwingAmount = 0.8f // Moderate swing amount
    tempPlayer.prevLimbSwingAmount = 0.7f

    // Subtle arm movement
    tempPlayer.swingProgress = (sin(time * 2f) + 1f) * 0.1f // 0 to 0.2
    tempPlayer.prevSwingProgress = tempPlayer.swingProgress * 0.9f

    tempPlayer.onGround = true
    tempPlayer.ticksExisted = (time).toInt()
}

}