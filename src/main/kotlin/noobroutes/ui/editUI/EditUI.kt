package noobroutes.ui.editUI

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Vec3
import noobroutes.Core
import noobroutes.config.Config
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.rings.*
import noobroutes.features.render.ClickGUIModule
import noobroutes.ui.Screen
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.ui.editUI.elements.ElementCheckBox
import noobroutes.ui.editUI.elements.ElementSlider
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.ui.util.MouseUtils.mouseX
import noobroutes.ui.util.MouseUtils.mouseY
import noobroutes.utils.Vec2
import noobroutes.utils.render.*
import kotlin.math.floor


/*
    Yes, I know the code is shit. No, I am not going to fix it.
    Ui's are such a pain in the ass that I cannot be bothered to improve the code.
    Even, if I easily improve it, if it works, it works.

    Fuck
    U
    I
    s

    They are the bane of my existence.

    Here is some copy and pasted ai slop:

    Why UIs Are a Pain to Make

        User Interfaces (UIs) are essential. They are the bridge between complex systems and human beings,
    turning raw logic and data into tangible, interactive experiences. Yet, despite their importance — or perhaps because of it — building UIs
    is a notoriously frustrating, tedious, and error-prone process. Whether you’re working in Java Swing, HTML/CSS,
    or some janky custom GUI framework for a Minecraft mod, making UIs often feels less like engineering
    and more like pixel-wrangling purgatory. Here’s why.

    1. Design is Subjective, But Code is Not
        In back-end programming, code either works or it doesn’t.
    A function returns the right value, or it doesn't.
    But in UI development, perfection is subjective.
    Is that button the right size? Does that spacing feel right?
    Is the color accessible enough? Design is fuzzy, nuanced, and deeply human — but you’re stuck expressing
    it through brittle, rigid code. That mismatch breeds frustration. You’re constantly second-guessing pixel values,
    trying to match mockups, and tweaking things for hours to move a label five pixels to the left.

    2. You’re Fighting the Framework
        It doesn't matter if it’s React, Flutter, JavaFX, Forge's scaled resolution mess,
    or some HTML canvas hack — every UI framework has quirks. Layout systems break under edge cases.
    Responsiveness is an afterthought. And good luck mixing dynamic behavior with nested, animated,
    or interactive elements without descending into spaghetti. The higher-level the abstraction,
    the more you’re at the mercy of magic state handling and performance trade-offs. The lower-level the framework,
    the more you’re reinventing wheels — and dealing with input events, bounding boxes, and redrawing by hand.

    3. State Management is a Nightmare
        UIs are inherently stateful. Every interaction — hovering, clicking, typing — can change what
    the user sees or does. Managing that state is hard. What’s visible? What’s enabled?
    What if an element should animate in? How do you prevent race conditions or stale renders?
    And don’t even bring up “reactive programming” unless you enjoy debugging a chain of cascading updates and ghost states.

    4. Testing is Basically Manual QA
        Back-end code can be unit-tested. Front-end code? Good luck.
    Most UI testing is either brittle (pixel-diff screenshots, anyone?), slow (Selenium-level slow),
    or incomplete (did you test dragging that modal while resizing the window?).
    So you end up spending hours manually clicking around to make sure that one CSS change didn’t break the tooltip
    on hover inside a nested scroll container inside a tab.

    5. Everyone Has an Opinion
        Unlike infrastructure code that lives deep in the bowels of the system, UIs are visible.
    To everyone. That means everyone has feedback: designers, product managers, users, your dog.
    You’ll be told to change the font, adjust the padding, animate the button, make it pop more,
    or align it to an imaginary axis. And none of it’s wrong — but implementing that feedback
    in a fragile UI codebase is an exercise in pain.

    6. Cross-Device and Resolution Hell
        UIs don’t run in a vacuum. They have to scale across devices, resolutions, aspect ratios,
    DPI settings, and accessibility modes. That beautiful centered layout you made?
    Now it’s overflowing on ultra-wide screens, broken on mobile, and blurry on high-DPI monitors.
    Minecraft mod UIs are especially cursed: you have to deal with scaled resolutions, mouse coordinates
    that don’t map to your draw area, and GUI elements that behave differently depending on internal states you have zero control over.

    Conclusion
        UIs are deceptively difficult. They look simple on the surface,
    but underneath lies a tangled mess of layout constraints,
    event handlers, visual glitches, and state transitions — all
    made worse by subjective design requirements and platform inconsistencies.
    It’s not that UI developers are bad at their jobs; it’s that the job itself is unreasonably hard.
    So next time your GUI lags, buttons misalign, or scrollbars vanish for no reason,
    don’t blame the dev. Blame the UI gods. They’re cruel and they never sleep.
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
            is BlinkRing -> {
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
            }

            is InstaRing -> {
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
        GlStateManager.pushMatrix()
        translate(0f, 0f, 200f)

        scale(1f / scaleFactor, 1f / scaleFactor, 1f)
        if (dragging) {
            originX = floor(x2 + mouseX)
            originY = floor(y2 + mouseY)
        }
        roundedRectangle(mc.displayWidth * 0.5 - 75, mc.displayHeight * 0.9f - 40, 150f, 80f, ColorUtil.buttonColor, 15f)
        text("Reset", mc.displayWidth * 0.5, mc.displayHeight * 0.9f, Color.WHITE.darkerIf(isResetHovered), 26f, align = TextAlign.Middle)
        //roundedRectangle(ORIGIN_X + 200f, ORIGIN_Y + 10f, 200f, 5f, Color.WHITE, 2f)
        roundedRectangle(originX, originY, 600, 70, ColorUtil.titlePanelColor,  ColorUtil.titlePanelColor, Color.TRANSPARENT, 0, 20f, 20f, 0f, 0f, 0f)
        roundedRectangle(originX, originY, 600, currentY, ColorUtil.buttonColor, radius = 20)
        text(ring.type, originX + X_ALIGNMENT_LEFT - 10, originY + 37.5, Color.WHITE, size = 30)
        elements.forEach { it.draw(originX + it.x, originY + it.y) }
        ring.coords = Vec3(ringX, ringY, ringZ)
        scale(scaleFactor, scaleFactor, 1f)
        GlStateManager.popMatrix()
    }


}