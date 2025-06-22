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



/*
    Yes, I know the code is shit. No, I am not going to fix it.
    Ui's are such a pain in the ass that I cannot be bothered to improve the code.
    Even, if I easily improve it, if it works, it works.

    Fuck
    U
    I
    s

    They are the bane of my existence.

    Here is some copy and pasted chatGPT:

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
    const val ORIGIN_X = 100f
    const val ORIGIN_Y = 100f
    const val X_ALIGNMENT_LEFT = ORIGIN_X + 30f
    const val X_ALIGNMENT_RIGHT = ORIGIN_X + 300f
    const val WIDTH = 600f

    var ringY = 0.0
    var backgroundHeight = 490f


    //hyper if you want to complain about this function go ahead and recode this ui. I cannot be asked.
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