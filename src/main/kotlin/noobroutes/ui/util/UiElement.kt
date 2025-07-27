package noobroutes.ui.util

import net.minecraft.client.renderer.GlStateManager


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
        drawChildren()
        GlStateManager.popMatrix()
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

    open fun draw() {}

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

    protected fun updateChildrenTranslation(){
        uiChildren.forEach {
            it.xOrigin = this.xOrigin + deltaX
            it.yOrigin = this.yOrigin + deltaY
        }
    }

    protected fun updateChildrenScale(){
        uiChildren.forEach {
            it.globalXScale = this.deltaScaleX * this.globalXScale
            it.globalYScale = this.deltaScaleY * this.globalYScale
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