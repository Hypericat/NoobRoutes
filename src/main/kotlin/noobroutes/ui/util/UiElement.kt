package noobroutes.ui.util

import net.minecraft.client.renderer.GlStateManager
import noobroutes.Core.logger
import noobroutes.ui.util.elements.NumberBoxElement
import noobroutes.ui.util.shader.GaussianBlurShader
import noobroutes.utils.render.Color
import noobroutes.utils.render.Scissor
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.resetScissor
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.scissor
import noobroutes.utils.render.stencil
import noobroutes.utils.render.stencilRoundedRectangle


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

    protected fun drawChildren(){
        uiChildren.forEach { it.handleDraw() }
    }

    open fun updateChildren(){}

    open fun updatePosition(x: Float, y: Float){
        val deltaX = x - this.x
        val deltaY = y - this.y
        this.x = x
        this.y = y
        uiChildren.forEach {
            it.updatePosition(it.x + deltaX, it.y + deltaY)
        }
    }

    fun handleDraw(){
        if (!visible) return
        draw()
        GlStateManager.pushMatrix()
        GlStateManager.translate(deltaX, deltaY, 1f)
        GlStateManager.scale(deltaScaleX, deltaScaleY, 1f)
        if (stencilChildren) {
            stencilRoundedRectangle(stencilX, stencilY, stencilWidth, stencilHeight, stencilRadius, stencilEdgeSoftness, stencilInverse)
        }
        val scissor = childrenScissor

        if (scissor != null) {
            val scissorTest = scissor(scissor.x.toFloat() + getEffectiveX(), scissor.y.toFloat() + getEffectiveY(), scissor.w.toFloat() * getEffectiveXScale(), scissor.h.toFloat() * getEffectiveYScale())
            drawChildren()
            resetScissor(scissorTest)
        } else drawChildren()

        if (stencilChildren) {
            popStencil()
            stencilChildren = false
        }
        GlStateManager.popMatrix()
    }

    fun handleScroll(amount: Int): Boolean {
        if (!enabled || !visible) return false
        if (uiChildren.any { it.handleScroll(amount) }) return true
        return onScroll(amount)
    }

    fun handleMouseClicked(mouseButton: Int): Boolean{
        if (!enabled || !visible) return false
        if (uiChildren.any { it.handleMouseClicked(mouseButton) }) return true
        return mouseClicked(mouseButton)
    }

    fun handleMouseReleased(): Boolean {
        if (!enabled || !visible) return false
        if (uiChildren.any { it.handleMouseReleased() }) return true
        return mouseReleased()
    }

    fun handleKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!enabled || !visible) return false
        if (uiChildren.any { it.handleKeyTyped(typedChar, keyCode) }) return true
        return keyTyped(typedChar, keyCode)
    }

    protected open fun onScroll(amount: Int): Boolean {
        return false
    }

    protected open fun draw() {}

    protected open fun mouseClicked(mouseButton: Int): Boolean {
        return false
    }

    protected open fun mouseReleased(): Boolean {
        return false
    }

    protected open fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        return false
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
        //GlStateManager.pushMatrix()
        GlStateManager.translate(-effX, -effY, -1f)
        stencil {roundedRectangle(effX + x.toFloat(),effY + y.toFloat(), w, h, Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT, 0f, topL, topR, botL, botR, edgeSoftness)}
        GaussianBlurShader.blurredBackground(effX + x.toFloat(), effY + y.toFloat(), w.toFloat(), h.toFloat(), 8f)
        popStencil()
        GlStateManager.translate(effX, effY, 1f)
        //GlStateManager.popMatrix()
    }


    protected fun translate(x: Float, y: Float){
        deltaX = x * globalXScale
        deltaY = y * globalYScale
        updateChildrenTranslation()
        GlStateManager.translate(x, y, 1f)
    }
    protected fun scale(x: Float, y: Float){
        this.deltaScaleX = x
        this.deltaScaleY = y
        updateChildrenScale()
        GlStateManager.scale(x, y, 1f)
    }
    private var stencilChildren = false
    private var stencilX = 0f
    private var stencilY = 0f
    private var stencilWidth = 0f
    private var stencilHeight = 0f
    private var stencilRadius = 0f
    private var stencilEdgeSoftness = 0f
    private var stencilInverse = false

    private var childrenScissor: Scissor? = null

    protected fun scissorChildren(x: Float, y: Float, w: Float, h: Float) {
        childrenScissor = Scissor(x, y, w, h, 0)
    }

    protected fun stencilChildren(x: Float, y: Float, w: Float, h: Float, radius: Number = 0f, edgeSoftness: Number = 0.5f, inverse: Boolean = false){
        stencilChildren = true
        stencilX = x
        stencilY = y
        stencilWidth = w
        stencilHeight = h
        stencilRadius = radius.toFloat()
        stencilEdgeSoftness = edgeSoftness.toFloat()
        stencilInverse = inverse
    }

    protected fun updateChildrenTranslation(){
        uiChildren.forEach {
            it.xOrigin = this.xOrigin + deltaX
            it.yOrigin = this.yOrigin + deltaY
            it.updateChildrenTranslation()
        }
    }

    protected fun updateChildrenScale(){
        uiChildren.forEach {
            it.globalXScale = this.deltaScaleX * this.globalXScale
            it.globalYScale = this.deltaScaleY * this.globalYScale
            it.updateChildrenScale()
        }
    }

    open fun getEffectiveX(): Float {
        return this.xOrigin + deltaX
    }
    open fun getEffectiveY(): Float {
        return this.yOrigin + this.deltaY
    }
    open fun getEffectiveXScale(): Float {
        return this.deltaScaleX * globalXScale
    }
    open fun getEffectiveYScale(): Float {
        return this.deltaScaleY * globalYScale
    }


    private fun debugMouse(w: Float, h: Float){
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