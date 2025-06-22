package noobroutes.ui.editUI

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Vec3
import noobroutes.Core
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.rings.*
import noobroutes.features.render.ClickGUIModule
import noobroutes.ui.Screen
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.editUI.elements.ElementCheckBox
import noobroutes.ui.editUI.elements.ElementSlider
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.render.*

object EditUI : Screen() {

    var ring: Ring? = null
    const val ORIGIN_X = 100f
    const val ORIGIN_Y = 100f
    const val X_ALIGNMENT_LEFT = ORIGIN_X + 30f
    const val X_ALIGNMENT_RIGHT = ORIGIN_X + 300f
    const val WIDTH = 600f

    var ringY = 0.0
    var backgroundHeight = 490f

    fun openUI(ring: Ring) {
        this.ring = ring
        ring.renderYawVector = true
        ringY = ring.coords.yCoord
        backgroundHeight = 460f
        elements.add(
            ElementSlider(
                "Yaw",
                min = 0.0, max = 360.0,
                unit = "°",
                increment = 0.1,
                { ring.yaw.toDouble() + 180 }, { ring.yaw = it.toFloat() - 180 },
                X_ALIGNMENT_LEFT, ORIGIN_Y + 105,
                550f,
                104f,
                4
            )
        )
        elements.add(
            ElementSlider(
                "Ring Y",
                min = 0.0, max = 255.0,
                unit = "",
                increment = 0.1,
                { ringY }, { ringY = it },
                X_ALIGNMENT_LEFT, ORIGIN_Y + 180,
                550f,
                104f,
                1
            )
        )
        //
        elements.add(
            ElementCheckBox(
                X_ALIGNMENT_LEFT,
                ORIGIN_Y + 255f,
                250f, 50f,
                { ring.center = it },
                { ring.center },
                "Center"
            )
        )
        elements.add(
            ElementCheckBox(
                X_ALIGNMENT_RIGHT,
                ORIGIN_Y + 255f,
                250f, 50f,
                { ring.rotate = it },
                { ring.rotate },
                "Rotate"
            )
        )
        elements.add(
            ElementCheckBox(
                X_ALIGNMENT_LEFT,
                ORIGIN_Y + 305f,
                250f, 50f,
                { ring.left = it },
                { ring.left },
                "Left"
            )
        )

        elements.add(
            ElementCheckBox(
                X_ALIGNMENT_RIGHT,
                ORIGIN_Y + 305f,
                250f, 50f,
                { ring.term = it },
                { ring.term },
                "Term"
            )
        )
        elements.add(
            ElementCheckBox(
                X_ALIGNMENT_LEFT,
                ORIGIN_Y + 360f,
                250f, 50f,
                { ring.leap = it },
                { ring.leap },
                "Leap"
            )
        )
        when (ring) {
            is BlinkRing -> {
                elements.add(
                    ElementCheckBox(
                        X_ALIGNMENT_RIGHT,
                        ORIGIN_Y + 360f,
                        250f, 50f,
                        { ring.walk = it },
                        { ring.walk },
                        "Walk"
                    )
                )
            }

            is ClampRing -> {
                elements.add(
                    ElementCheckBox(
                        X_ALIGNMENT_RIGHT,
                        ORIGIN_Y + 360f,
                        250f, 50f,
                        { ring.walk = it },
                        { ring.walk },
                        "Walk"
                    )
                )
            }

            is HClipRing -> {
                elements.add(
                    ElementCheckBox(
                        X_ALIGNMENT_RIGHT,
                        ORIGIN_Y + 360f,
                        250f, 50f,
                        { ring.walk = it },
                        { ring.walk },
                        "Walk"
                    )
                )
            }

            is InstaRing -> {
                elements.add(
                    ElementCheckBox(
                        X_ALIGNMENT_RIGHT,
                        ORIGIN_Y + 360f,
                        250f, 50f,
                        { ring.walk = it },
                        { ring.walk },
                        "Walk"
                    )
                )
            }

            is JumpRing -> {
                elements.add(
                    ElementCheckBox(
                        X_ALIGNMENT_RIGHT,
                        ORIGIN_Y + 360f,
                        250f, 50f,
                        { ring.walk = it },
                        { ring.walk },
                        "Walk"
                    )
                )
            }

            is LavaClipRing -> {
                elements.add(
                    ElementSlider(
                        "Distance",
                        min = 0.0, max = 60.0,
                        unit = "",
                        increment = 1.0,
                        { ring.length }, { ring.length = it },
                        X_ALIGNMENT_LEFT, ORIGIN_Y + 415,
                        550f,
                        104f,
                        0
                    )
                )
                backgroundHeight = 515f
            }

            is MotionRing -> {
                elements.add(
                    ElementCheckBox(
                        X_ALIGNMENT_RIGHT,
                        ORIGIN_Y + 360f,
                        250f, 50f,
                        { ring.far = it },
                        { ring.far },
                        "Walk"
                    )
                )
                elements.add(
                    ElementSlider(
                        "Scale",
                        min = 0.01, max = 1.0,
                        unit = "",
                        increment = 0.05,
                        { ring.scale.toDouble() }, { ring.scale = it.toFloat() },
                        X_ALIGNMENT_LEFT, ORIGIN_Y + 415,
                        550f,
                        104f,
                        2
                    )
                )
                backgroundHeight = 515f
            }
            is SpedRing -> {
                backgroundHeight = 515f
                elements.add(
                    ElementSlider(
                        "Length",
                        min = 1.0, max = 30.0,
                        unit = "",
                        increment = 0.05,
                        { ring.length.toDouble() }, { ring.length = it.toInt() },
                        X_ALIGNMENT_LEFT, ORIGIN_Y + 415,
                        550f,
                        104f,
                        0
                    )
                )
            }
        }
        Core.display = EditUI
    }

    //270f
    val elements = mutableListOf<Element<*>>()


    override fun initGui() {
        if (OpenGlHelper.shadersSupported && mc.renderViewEntity is EntityPlayer && ClickGUIModule.blur) {
            mc.entityRenderer.stopUseShader()
            mc.entityRenderer.loadShader(ResourceLocation("shaders/post/blur.json"))
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        elements.forEach { it.keyTyped(typedChar, keyCode) }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        elements.forEach { it.mouseReleased() }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        elements.forEach { it.mouseClickedAnywhere(mouseButton) }
        if (isAreaHovered(ORIGIN_X, ORIGIN_Y, WIDTH, backgroundHeight)) {
            elements.forEach { it.mouseClicked() }
        }
    }

    override fun onGuiClosed() {
        mc.entityRenderer.stopUseShader()
        ring?.renderYawVector = false
        ring = null
        elements.clear()
        AutoP3.saveRings()
    }

    override fun draw() {
        val ring = this.ring ?: return
        GlStateManager.pushMatrix()
        translate(0f, 0f, 200f)
        scale(1f / scaleFactor, 1f / scaleFactor, 1f)
        roundedRectangle(ORIGIN_X, ORIGIN_Y, 600, backgroundHeight, ColorUtil.buttonColor, radius = 20)
        text(ring.type, X_ALIGNMENT_LEFT, ORIGIN_Y + 50, Color.WHITE, size = 38)
        elements.forEach { it.draw() }
        ring.coords = Vec3(ring.coords.xCoord, ringY, ring.coords.zCoord)
        scale(scaleFactor, scaleFactor, 1f)
        GlStateManager.popMatrix()
    }


}