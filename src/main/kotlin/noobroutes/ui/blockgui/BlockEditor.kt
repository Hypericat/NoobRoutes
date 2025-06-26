package noobroutes.ui.blockgui

import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.render.Color
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text
import kotlin.math.floor

object BlockEditor {

    var scrollOffset = 0
    var originX = 500f
    var originY = 200f

    const val WIDTH = 600f
    const val HEIGHT = 600f



    var x2 = 0f
    var y2 = 0f
    var dragging = false
    fun mouseClicked(): Boolean {
        if (isAreaHovered(originX, originY, WIDTH, 70f)) {
            x2 = originX - MouseUtils.mouseX
            y2 = originY - MouseUtils.mouseY
            dragging = true
            return true
        }
        return false
    }
    fun draw() {
        if (dragging) {
            originX = floor(x2 + MouseUtils.mouseX)
            originY = floor(y2 + MouseUtils.mouseY)
        }
        roundedRectangle(originX, originY, 600, 70, ColorUtil.titlePanelColor,  ColorUtil.titlePanelColor, Color.TRANSPARENT, 0, 20f, 20f, 0f, 0f, 0f)
        roundedRectangle(originX, originY, 600, HEIGHT, ColorUtil.buttonColor, radius = 20)
        text("Block State Editor", originX + 20, originY + 37.5, Color.WHITE, size = 30)
    }

}