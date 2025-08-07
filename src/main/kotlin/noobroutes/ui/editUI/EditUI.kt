package noobroutes.ui.editUI

import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import noobroutes.Core
import noobroutes.config.Config
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.rings.*
import noobroutes.features.render.ClickGUIModule
import noobroutes.ui.ColorPalette
import noobroutes.ui.Screen
import noobroutes.ui.editUI.elements.ElementCheckBox
import noobroutes.ui.editUI.elements.ElementSlider
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.ui.util.MouseUtils.mouseX
import noobroutes.ui.util.MouseUtils.mouseY
import noobroutes.ui.util.shader.GaussianBlurShader
import noobroutes.utils.render.*
import noobroutes.utils.render.ColorUtil.darkerIf
import kotlin.math.floor


/**
 * TODO Remake this
 * the code is shit.
 */


object EditUI : Screen() {

    var ring: Ring? = null
    var originX = 100f
    var originY = 200f
    const val X_ALIGNMENT_LEFT = 30f
    const val X_ALIGNMENT_RIGHT = 300f
    const val WIDTH = 600f
    var ringX = 0.0
    var ringY = 0.0
    var ringZ = 0.0
    var currentY = 490f

    val isResetHovered get() = isAreaHovered(mc.displayWidth * 0.5f - 75f, mc.displayHeight * 0.9f - 40f, 150f, 80f)

    //hyper if you want to complain about this function go ahead and recode this ui. I cannot be asked.
    fun openUI(ring: Ring) {
        this.ring = ring
        ring.renderYawVector = true
        ringY = ring.coords.yCoord
        ringX = ring.coords.xCoord
        ringZ = ring.coords.zCoord
        currentY = 105f
        elements.add(
            ElementSlider(
                "Yaw",
                min = ring.yaw - 5.0, max = ring.yaw + 5.0,
                unit = "°",
                increment = 0.1,
                { ring.yaw.toDouble()}, { ring.yaw = MathHelper.wrapAngleTo180_float(it.toFloat()) },
                X_ALIGNMENT_LEFT, currentY,
                550f,
                104f,
                2
            )
        )
        currentY += 75f
        elements.add(
            ElementSlider(
                "Ring X",
                min = ringX - 3, max = ringX + 3,
                unit = "",
                increment = 0.1,
                { ringX }, { ringX = it },
                X_ALIGNMENT_LEFT, currentY,
                550f,
                104f,
                1
            )
        )
        currentY += 75f
        if (ring !is BlinkRing) {
            elements.add(
                ElementSlider(
                    "Ring Y",
                    min = ringY - 3, max = ringY + 3,
                    unit = "",
                    increment = 0.1,
                    { ringY }, { ringY = it },
                    X_ALIGNMENT_LEFT, currentY,
                    550f,
                    104f,
                    1
                )
            )
            currentY += 75f
        }
        elements.add(
            ElementSlider(
                "Ring Z",
                min = ringZ - 3, max = ringZ + 3,
                unit = "",
                increment = 0.1,
                { ringZ }, { ringZ = it },
                X_ALIGNMENT_LEFT, currentY,
                550f,
                104f,
                1
            )
        )
        currentY += 75f
        elements.add(
            ElementSlider(
                "Diameter",
                min = 0.0, max = 3.0,
                unit = "",
                increment = 0.1,
                { ring.diameter.toDouble() }, { ring.diameter = it.toFloat() },
                X_ALIGNMENT_LEFT, currentY,
                550f,
                104f,
                2
            )
        )
        currentY += 75f

        if (ring !is BlinkRing && ring !is MotionRing && ring !is JumpRing) {
            elements.add(
                ElementSlider(
                    "Height",
                    min = 0.0, max = 3.0,
                    unit = "",
                    increment = 0.1,
                    { ring.height.toDouble() }, { ring.height = it.toFloat() },
                    X_ALIGNMENT_LEFT, currentY,
                    550f,
                    104f,
                    2
                )
            )
            currentY += 75f
        }
        elements.add(
            ElementCheckBox(
                X_ALIGNMENT_LEFT,
                currentY,
                250f, 50f,
                { ring.center = it },
                { ring.center },
                "Center"
            )
        )
        elements.add(
            ElementCheckBox(
                X_ALIGNMENT_RIGHT,
                currentY,
                250f, 50f,
                { ring.rotate = it },
                { ring.rotate },
                "Rotate"
            )
        )
        currentY += 50f
        elements.add(
            ElementCheckBox(
                X_ALIGNMENT_LEFT,
                currentY,
                250f, 50f,
                { ring.left = it },
                { ring.left },
                "Left"
            )
        )
        elements.add(
            ElementCheckBox(
                X_ALIGNMENT_RIGHT,
                currentY,
                250f, 50f,
                { ring.term = it },
                { ring.term },
                "Term"
            )
        )
        currentY += 50f
        elements.add(
            ElementCheckBox(
                X_ALIGNMENT_LEFT,
                currentY,
                250f, 50f,
                { ring.leap = it },
                { ring.leap },
                "Leap"
            )
        )
        when (ring) {
            is ClampRing -> {
                elements.add(
                    ElementCheckBox(
                        X_ALIGNMENT_RIGHT,
                        currentY,
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
                        currentY,
                        250f, 50f,
                        { ring.walk = it },
                        { ring.walk },
                        "Walk"
                    )
                )

                currentY += 50
                elements.add(
                    ElementCheckBox(
                        X_ALIGNMENT_LEFT,
                        currentY,
                        250f, 50f,
                        { ring.insta = it },
                        { ring.insta },
                        "Insta"
                    )
                )
            }

            is JumpRing -> {
                elements.add(
                    ElementCheckBox(
                        X_ALIGNMENT_RIGHT,
                        currentY,
                        250f, 50f,
                        { ring.walk = it },
                        { ring.walk },
                        "Walk"
                    )
                )
            }

            is LavaClipRing -> {
                currentY += 75f
                elements.add(
                    ElementSlider(
                        "Distance",
                        min = 10.0, max = 70.0,
                        unit = "",
                        increment = 1.0,
                        { ring.length }, { ring.length = it },
                        X_ALIGNMENT_LEFT, currentY,
                        550f,
                        104f,
                        0
                    )
                )
            }

            is MotionRing -> {
                elements.add(
                    ElementCheckBox(
                        X_ALIGNMENT_RIGHT,
                        currentY,
                        250f, 50f,
                        { ring.far = it },
                        { ring.far },
                        "Walk"
                    )
                )
                currentY += 75f
                elements.add(
                    ElementSlider(
                        "Scale",
                        min = 0.85, max = 1.0,
                        unit = "",
                        increment = 0.01,
                        { ring.scale.toDouble() }, { ring.scale = it.toFloat() },
                        X_ALIGNMENT_LEFT, currentY,
                        550f,
                        104f,
                        2
                    )
                )


            }
            is SpedRing -> {
                currentY += 75f
                elements.add(
                    ElementSlider(
                        "Length",
                        min = 1.0, max = 30.0,
                        unit = "",
                        increment = 0.05,
                        { ring.length.toDouble() }, { ring.length = it.toInt() },
                        X_ALIGNMENT_LEFT, currentY,
                        550f,
                        104f,
                        0
                    )
                )
            }
        }
        Core.display = EditUI
        currentY += 75f
    }

    val elements = mutableListOf<Element<*>>()

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        elements.forEach { it.keyTyped(typedChar, keyCode) }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        elements.forEach { it.mouseReleased() }
        dragging = false
    }

    var dragging = false
    var x2 = 0f
    var y2 = 0f

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        elements.forEach { it.mouseClickedAnywhere(mouseButton) }

        if (isResetHovered) {
            originX = 100f
            originY = 200f
        }

        if (isAreaHovered(originX, originY, WIDTH, 70f)) {
            x2 = originX - MouseUtils.mouseX
            y2 = originY - MouseUtils.mouseY
            dragging = true
        }

        if (isAreaHovered(originX, originY, WIDTH, currentY)) {
            elements.forEach { it.mouseClicked() }
        }
    }

    override fun onGuiClosed() {
        mc.entityRenderer.stopUseShader()
        ring?.renderYawVector = false
        ring = null
        elements.clear()
        AutoP3.saveRings()
        Config.save()
    }

    override fun draw() {
        val ring = this.ring ?: return
        scaleUI()
        if (ClickGUIModule.blur) GaussianBlurShader.captureBackground()
        if (dragging) {
            originX = floor(x2 + mouseX)
            originY = floor(y2 + mouseY)
        }
        roundedRectangle(mc.displayWidth * 0.5 - 75, mc.displayHeight * 0.9f - 40, 150f, 80f, ColorPalette.buttonColor, 15f)
        text("Reset", mc.displayWidth * 0.5, mc.displayHeight * 0.9f, Color.WHITE.darkerIf(isResetHovered), 26f, align = TextAlign.Middle)
        //roundedRectangle(ORIGIN_X + 200f, ORIGIN_Y + 10f, 200f, 5f, Color.WHITE, 2f)
        if (ClickGUIModule.blur) blurRoundedRectangle(originX, originY, 600, currentY, 20f, 20f, 20f, 20f, 0.5f)
        roundedRectangle(originX, originY, 600, 70, ColorPalette.titlePanelColor,  ColorPalette.titlePanelColor, Color.TRANSPARENT, 0, 20f, 20f, 0f, 0f, 0f)
        roundedRectangle(originX, originY, 600, currentY, ColorPalette.buttonColor, radius = 20)
        text(ring.ringName, originX + X_ALIGNMENT_LEFT - 10, originY + 37.5, Color.WHITE, size = 30)
        elements.forEach { it.draw(originX + it.x, originY + it.y) }
        ring.coords = Vec3(ringX, ringY, ringZ)
        if (ClickGUIModule.blur) GaussianBlurShader.cleanup()
        resetScale()
    }


}