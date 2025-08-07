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
        generateWalkingAnimation(distanceMoved)
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
    var lastYaw: Float = 0f
    var lastPitch: Float = 0f
    private var lastBodyYaw: Float = 0f

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
        GlStateManager.enableTexture2D()
        GlStateManager.enableAlpha()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateManager.disableLighting()


        try {
            val playerRenderer = renderManager.getEntityRenderObject<EntityPlayer>(player) as RenderPlayer
            val tempPlayer = EntityOtherPlayerMP(player.worldObj, player.gameProfile)
            tempPlayer.setPosition(0.0, 0.0, 0.0)

            for (i in 0..4) {
                tempPlayer.setCurrentItemOrArmor(i, player.getEquipmentInSlot(i))
            }
            tempPlayer.inventory.mainInventory[0] = mc.thePlayer.heldItem
            tempPlayer.inventory.currentItem = 0

            val targetBodyYaw = yaw
            val bodyYawDiff = MathHelper.wrapAngleTo180_float(targetBodyYaw - lastBodyYaw)
            val smoothedBodyYaw = lastBodyYaw + bodyYawDiff * 0.3f

            tempPlayer.prevRotationYaw = lastYaw
            tempPlayer.prevRotationYawHead = lastYaw


            tempPlayer.prevRotationPitch = lastPitch

            tempPlayer.rotationYaw = smoothedBodyYaw
            tempPlayer.renderYawOffset = smoothedBodyYaw
            tempPlayer.prevRenderYawOffset = lastBodyYaw
            tempPlayer.rotationYawHead = yaw
            tempPlayer.rotationPitch = pitch

            lastPitch = pitch
            lastYaw = yaw
            lastBodyYaw = smoothedBodyYaw

            animationGenerator.generateAnimationFromMovement(
                currentX, currentZ,
                prevX, prevZ,
                tempPlayer
            )

            playerRenderer.doRender(tempPlayer, 0.0, 0.0, 0.0, 0.0f, partialTicks)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
            GlStateManager.enableTexture2D()
            GlStateManager.disableBlend()
            GlStateManager.disableAlpha()
            GlStateManager.disableLighting()

            GlStateManager.popAttrib()
            GlStateManager.popMatrix()
        }
    }
}

