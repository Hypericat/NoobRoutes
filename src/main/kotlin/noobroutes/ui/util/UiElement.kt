package noobroutes.ui.util

import net.minecraft.client.renderer.GlStateManager


abstract class UiElement(var x: Float, var y: Float) {
    var parent: UiElement? = null
    val uiChildren = mutableListOf<UiElement>()
    var xOrigin = 0f
    var yOrigin = 0f
    var globalXScale = 1f
    var globalYScale = 1f
    private var deltaX = 0f
    private var deltaY = 0f
    private var deltaScaleX = 1f
    private var deltaScaleY = 1f

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

    open fun draw() {
        uiChildren.forEach { it.draw() }
    }

    open fun mouseClicked(mouseButton: Int): Boolean {
        return uiChildren.any { it.mouseClicked(mouseButton) }
    }
    open fun mouseReleased(): Boolean {
        return uiChildren.any { it.mouseReleased() }
    }

    open fun mouseClickedAnywhere() {
        uiChildren.forEach { it.mouseClickedAnywhere() }
    }
    open fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        return uiChildren.any { it.keyTyped(typedChar, keyCode) }
    }

    protected fun addChildren(children: Collection<UiElement>) {
        children.map { it.parent = this }
        uiChildren.addAll(children)
    }

    protected fun addChild(child: UiElement) {
        child.parent = this
        uiChildren.add(child)
    }

    protected fun translate(x: Float, y: Float){
        deltaX = x
        deltaY = y
        updateChildrenTranslation()
        GlStateManager.translate(x, y, 1f)
    }
    protected fun scale(x: Float, y: Float){
        deltaScaleX = x
        deltaScaleY = y
        updateChildrenScale()
        GlStateManager.scale(x, y, 1f)
    }

    protected fun updateChildrenTranslation(){
        val xOrigin = getEffectiveX()
        val yOrigin = getEffectiveY()
        uiChildren.forEach {
            it.xOrigin = xOrigin
            it.yOrigin = yOrigin
            it.updateChildrenTranslation()
        }
    }

    protected fun updateChildrenScale(){
        val xScale = getEffectiveXScale()
        val yScale = getEffectiveYScale()
        uiChildren.forEach {
            it.globalXScale = xScale
            it.globalYScale = yScale
            it.updateChildrenScale()
        }
    }

    open fun getEffectiveX(): Float {
        return xOrigin + deltaX
    }
    open fun getEffectiveY(): Float {
        return yOrigin + deltaY
    }
    open fun getEffectiveXScale(): Float {
        return globalXScale * deltaScaleX
    }
    open fun getEffectiveYScale(): Float {
        return globalYScale * deltaScaleY
    }



    protected fun isAreaHovered(x: Float, y: Float, w: Float, h: Float): Boolean {
        return MouseUtils.isAreaHovered(getEffectiveXScale() * (x + getEffectiveX()), getEffectiveYScale() * (y + getEffectiveY()), w * getEffectiveXScale(), h * getEffectiveYScale())
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