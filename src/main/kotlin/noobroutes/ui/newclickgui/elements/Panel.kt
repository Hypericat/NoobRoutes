package noobroutes.ui.newclickgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.Core.logger
import noobroutes.features.Category
import noobroutes.features.ModuleManager.modules
import noobroutes.features.render.ClickGUIModule
import noobroutes.font.Font
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.ColorPalette.moduleButtonColor
import noobroutes.ui.ColorPalette.titlePanelColor
import noobroutes.ui.clickgui.Panel
import noobroutes.ui.clickgui.SearchBar.currentSearch
import noobroutes.ui.util.MouseUtils.mouseX
import noobroutes.ui.util.MouseUtils.mouseY
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.EaseInOut
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.utils.capitalizeFirst
import noobroutes.utils.render.ColorUtil.brighter
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.getTextWidth
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.stencilRoundedRectangle
import noobroutes.utils.render.text
import noobroutes.utils.round
import kotlin.math.floor

class Panel(val name: String, val category: Category) : UiElement(ClickGUIModule.panelX[category]!!.value, ClickGUIModule.panelY[category]!!.value) {
    companion object {
        const val WIDTH = 240f
        const val HEIGHT = 40f
        const val HALF_HEIGHT = HEIGHT * 0.5f
    }

    private var dragging = false

    var extended: Boolean = ClickGUIModule.panelExtended[category]!!.enabled

    private var length = 0f

    private var x2 = 0f
    private var y2 = 0f

    private var scrollTarget = 0f
    private var scrollOffset = 0f
    private val scrollAnimation = LinearAnimation<Float>(200)
    private val extendAnim = EaseInOut(250)

    init {
        for (module in modules.sortedByDescending { getTextWidth(it.name, 18f) }) {
            if (module.category != this@Panel.category) continue
            addChild(ModuleButton(0f, module))
        }
    }


    private inline val isHovered get() = isAreaHovered(0f, 0f, WIDTH, HEIGHT)

    private val isMouseOverExtended
        get() = extended && isAreaHovered(0f, 0f, WIDTH, length.coerceAtLeast(HEIGHT))

    private fun getTotalHeight(offset: Float): Float {
        var y = offset
        uiChildren.forEach { y += (it as ModuleButton).height }
        return y
    }



    override fun draw() {
        GlStateManager.pushMatrix()
        scrollOffset = scrollAnimation.get(scrollOffset, scrollTarget).round(0).toFloat()

        if (dragging) {
            updatePosition(floor(x2 + mouseX), floor(y2 + mouseY))
        }
        translate(x, y)

        roundedRectangle(
            0f, 0f, WIDTH, HEIGHT,
            titlePanelColor, titlePanelColor, titlePanelColor,
            0f, 10f, 10f, 0f, 0f, 0.5f
        )
        text(name, TEXT_OFFSET, HALF_HEIGHT, ColorPalette.textColor, 16f, Font.BOLD,TextAlign.Left)
        if (extended) roundedRectangle(0f, HEIGHT - 2, WIDTH, 2f, clickGUIColor.brighter(1.65f))
//floor(extendAnim.get(0f, getSettingHeight(), !extended))
        val offset = floor(extendAnim.get(0f, getTotalHeight(scrollOffset), !extended))
        stencilChildren(0f, HEIGHT, WIDTH, offset)
        var startY = scrollOffset + HEIGHT

        if (extended || extendAnim.isAnimating()) {
            if (uiChildren.isNotEmpty()) {
                for (button in uiChildren) {
                    button.visible = true
                    button.updatePosition(0f, startY)
                    startY += (button as ModuleButton).height
                }
                length = startY + 5f
            }
            roundedRectangle(
                0f, offset + HEIGHT, WIDTH, 10f, moduleButtonColor, moduleButtonColor, moduleButtonColor,
                0f, 0f, 0f, 10f, 10f, 0.5f
            )


        } else {
            uiChildren.forEach {
                it.visible = false
            }
            roundedRectangle(
                0f, offset + HEIGHT, WIDTH, 10f, titlePanelColor, titlePanelColor, titlePanelColor,
                0f, 0f, 0f, 10f, 10f, 0.5f
            )
        }

        GlStateManager.popMatrix()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (!isHovered) return false
        if (mouseButton == 0) {
            x2 = x - mouseX
            y2 = y - mouseY
            dragging = true
            return true
        }
        if (mouseButton == 1) {
            if (extendAnim.start()) extended = !extended
            return true
        }
        return false
    }

    override fun onScroll(amount: Int): Boolean {
        if (isMouseOverExtended && currentSearch.isBlank()) {
            scrollTarget = (scrollTarget + amount).coerceIn(-length + scrollOffset + 72f, 0f)
            scrollAnimation.start(true)
            return true
        }
        return false
    }


    override fun mouseReleased(): Boolean {
        dragging = false

        ClickGUIModule.panelX[category]!!.value = x
        ClickGUIModule.panelY[category]!!.value = y
        ClickGUIModule.panelExtended[category]!!.enabled = extended
        /*
        if (extended) {
            moduleButtons.filter { it.module.name.contains(currentSearch, true) }.reversed().forEach {
                it.mouseReleased(state)
            }
        }

 */     return false
    }



}