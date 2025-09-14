package noobroutes.ui.util

import net.minecraft.client.renderer.GlStateManager
import noobroutes.Core.logger
import noobroutes.ui.util.shader.GaussianBlurShader
import noobroutes.utils.render.*


abstract class UiElement(var x: Float, var y: Float) {
    protected var parent: UiElement? = null
    protected val uiChildren = mutableListOf<UiElement>()
    var visible = true
    var enabled = true

    protected var xOrigin = 0f
    protected var yOrigin = 0f
    private var globalXScale = 1f
    private var globalYScale = 1f
    private var deltaX = 0f
    private var deltaY = 0f
    private var deltaScaleX = 1f
    private var deltaScaleY = 1f

    private var cachedEffectiveX = 0f
    private var cachedEffectiveY = 0f
    private var cachedEffectiveXScale = 1f
    private var cachedEffectiveYScale = 1f
    private var refreshEffectiveValues = true

    private data class StencilState(
        var active: Boolean = false,
        var x: Float = 0f,
        var y: Float = 0f,
        var width: Float = 0f,
        var height: Float = 0f,
        var radius: Float = 0f,
        var edgeSoftness: Float = 0f,
        var inverse: Boolean = false
    )

    private val stencilState = StencilState()
    private var childrenScissor: Scissor? = null

    protected open fun doDrawChildren(){
        for (i in uiChildren.indices) {
            uiChildren[i].doHandleDraw()
        }
    }

    open fun updateChildren(){}

    open fun updatePosition(x: Float, y: Float){
        this.x = x
        this.y = y
    }

    open fun doHandleDraw() {
        if (!visible) return

        draw()
        GlStateManager.pushMatrix()
        GlStateManager.translate(deltaX, deltaY, 0f)
        GlStateManager.scale(deltaScaleX, deltaScaleY, 1f)

        if (stencilState.active) {
            stencilRoundedRectangle(
                stencilState.x, stencilState.y, stencilState.width, stencilState.height,
                stencilState.radius, stencilState.edgeSoftness, stencilState.inverse
            )
        }
        childrenScissor?.let { scissor ->
            val scissorTest = scissor(
                scissor.x.toFloat() + getEffectiveX(),
                scissor.y.toFloat() + getEffectiveY(),
                scissor.w.toFloat() * getEffectiveXScale(),
                scissor.h.toFloat() * getEffectiveYScale()
            )
            doDrawChildren()
            resetScissor(scissorTest)
        } ?: doDrawChildren()

        if (stencilState.active) {
            popStencil()
            stencilState.active = false
        }
        GlStateManager.popMatrix()
    }

    fun handleScroll(amount: Int): Boolean {
        if (!enabled || !visible) return false

        for (i in uiChildren.indices) {
            if (uiChildren[i].handleScroll(amount)) return true
        }
        return onScroll(amount)
    }

    open fun handleMouseClicked(mouseButton: Int): Boolean{
        if (!enabled || !visible) return false

        for (i in uiChildren.indices) {
            if (uiChildren[i].handleMouseClicked(mouseButton)) {
                return true
            }
        }
        return mouseClicked(mouseButton)
    }

    fun handleMouseReleased(): Boolean {
        if (!enabled || !visible) return false

        for (i in uiChildren.indices) {
            if (uiChildren[i].handleMouseReleased()) return true
        }
        return mouseReleased()
    }

    open fun doHandleKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!enabled || !visible) return false

        for (i in uiChildren.indices) {
            if (uiChildren[i].doHandleKeyTyped(typedChar, keyCode)) return true
        }
        return keyTyped(typedChar, keyCode)
    }

    protected open fun onScroll(amount: Int): Boolean = false
    protected open fun draw() {}
    protected open fun mouseClicked(mouseButton: Int): Boolean = false
    protected open fun mouseReleased(): Boolean = false
    protected open fun keyTyped(typedChar: Char, keyCode: Int): Boolean = false

    protected fun addChildren(vararg children: UiElement) {
        children.forEach {
            it.parent = this
        }
        uiChildren.addAll(children)
    }

    protected fun addChildren(children: Collection<UiElement>) {
        children.forEach {
            it.parent = this
        }
        uiChildren.addAll(children)
    }

    protected fun addChild(child: UiElement) {
        child.parent = this
        uiChildren.add(child)
    }

    protected fun blurRoundedRectangle(x: Number, y: Number, w: Number, h: Number, topL: Number, topR: Number, botL: Number, botR: Number, edgeSoftness: Number){
        val effX = getEffectiveX()
        val effY = getEffectiveY()

        GlStateManager.translate(-effX, -effY, -1f)
        stencil {
            roundedRectangle(effX + x.toFloat(),effY + y.toFloat(), w, h, Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT, 0f, topL, topR, botL, botR, edgeSoftness)
        }
        GaussianBlurShader.blurredBackground(effX + x.toFloat(), effY + y.toFloat(), w.toFloat(), h.toFloat(), 4f)
        popStencil()
        GlStateManager.translate(effX, effY, 1f)

    }


    protected fun translate(x: Float, y: Float){
        deltaX = x * globalXScale
        deltaY = y * globalYScale
        refreshEffectiveValues = true
        updateChildrenTranslation()
        GlStateManager.translate(x, y, 0f)
    }
    protected fun scale(x: Float, y: Float){
        this.deltaScaleX = x
        this.deltaScaleY = y
        refreshEffectiveValues = true
        updateChildrenScale()
        GlStateManager.scale(x, y, 1f)
    }

    protected fun scissorChildren(x: Float, y: Float, w: Float, h: Float) {
        childrenScissor = Scissor(x, y, w, h, 0)
    }

    protected fun stencilChildren(x: Float, y: Float, w: Float, h: Float, radius: Number = 0f, edgeSoftness: Number = 0.5f, inverse: Boolean = false){
        stencilState.apply {
            active = true
            this.x = x
            this.y = y
            width = w
            height = h
            this.radius = radius.toFloat()
            this.edgeSoftness = edgeSoftness.toFloat()
            this.inverse = inverse
        }
    }

    protected fun updateChildrenTranslation(){
        val newXOrigin = xOrigin + deltaX
        val newYOrigin = yOrigin + deltaY

        for (i in uiChildren.indices) {
            val child = uiChildren[i]
            child.xOrigin = newXOrigin
            child.yOrigin = newYOrigin
            child.refreshEffectiveValues = true
            child.updateChildrenTranslation()
        }
    }

    fun setGlobalScale(x: Float, y: Float){
        globalXScale = x
        globalYScale = y
    }

    protected fun updateChildrenScale(){
        val newXScale = deltaScaleX * globalXScale
        val newYScale = deltaScaleY * globalYScale

        for (i in uiChildren.indices) {
            val child = uiChildren[i]
            child.globalXScale = newXScale
            child.globalYScale = newYScale
            child.refreshEffectiveValues = true
            child.updateChildrenScale()
        }
    }

    private fun updateEffectiveValues() {
        if (refreshEffectiveValues) {
            cachedEffectiveX = xOrigin + deltaX
            cachedEffectiveY = yOrigin + deltaY
            cachedEffectiveXScale = deltaScaleX * globalXScale
            cachedEffectiveYScale = deltaScaleY * globalYScale
            refreshEffectiveValues = false
        }
    }

    open fun getEffectiveX(): Float {
        updateEffectiveValues()
        return cachedEffectiveX
    }
    open fun getEffectiveY(): Float {
        updateEffectiveValues()
        return cachedEffectiveY
    }
    open fun getEffectiveXScale(): Float {
        updateEffectiveValues()
        return cachedEffectiveXScale
    }
    open fun getEffectiveYScale(): Float {
        updateEffectiveValues()
        return cachedEffectiveYScale
    }


    protected fun debugMouse(w: Float, h: Float){
        logger.info("New Box, xOrigin: $xOrigin, yOrigin: $yOrigin")
        logger.info("x: ${getEffectiveX() + x * getEffectiveXScale()}, y: ${getEffectiveY() + y * getEffectiveYScale()}, w: ${w * getEffectiveXScale()}, h: ${h * getEffectiveYScale()}| mouseX: ${MouseUtils.mouseX}, mouseY: ${MouseUtils.mouseY}")
    }

    protected fun isAreaHovered(x: Float, y: Float, w: Float, h: Float): Boolean {
        return MouseUtils.isAreaHovered(
            getEffectiveX() + x * getEffectiveXScale(),
            getEffectiveY() + y * getEffectiveYScale(),
            w * getEffectiveXScale(),
            h * getEffectiveYScale()
        )
    }

    /**
     * Returns how far the mouse X position is within the given horizontal bounds as a percentage [0.0, 1.0].
     *
     * @param x The starting x-coordinate of the bounds.
     * @param w The width of the bounds.
     * @param invert If true, returns the inverted percentage (1.0 = left, 0.0 = right).
     */
    protected fun getMouseXPercentageInBounds(x: Float, w: Float, invert: Boolean = false): Float {
        val relative = ((MouseUtils.mouseX - (x + getEffectiveX()) * getEffectiveXScale()) / (w * getEffectiveXScale())).coerceIn(0f, 1f)
        return if (invert) 1f - relative else relative
    }

    protected fun getMouseYPercentageInBounds(y: Float, h: Float, invert: Boolean = false): Float {
        val relative = ((MouseUtils.mouseY - (y + getEffectiveY()) * getEffectiveYScale()) / (h * getEffectiveYScale())).coerceIn(0f, 1f)
        return if (invert) 1f - relative else relative
    }
}