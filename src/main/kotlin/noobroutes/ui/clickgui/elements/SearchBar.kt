package noobroutes.ui.clickgui.elements

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import noobroutes.Core
import noobroutes.features.ModuleManager.modules
import noobroutes.features.render.ClickGUIModule
import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.ColorPalette.moduleButtonColor
import noobroutes.ui.ColorPalette.titlePanelColor
import noobroutes.ui.clickgui.elements.Panel.Companion.BORDER_THICKNESS
import noobroutes.ui.clickgui.elements.Panel.Companion.BOTTOM_SEGMENT_HEIGHT
import noobroutes.ui.clickgui.elements.Panel.Companion.DOUBLE_BORDER_THICKNESS
import noobroutes.ui.clickgui.elements.Panel.Companion.HEIGHT
import noobroutes.ui.clickgui.elements.Panel.Companion.HIGHLIGHT_THICKNESS
import noobroutes.ui.clickgui.elements.Panel.Companion.HITBOX_HEIGHT
import noobroutes.ui.clickgui.elements.Panel.Companion.HITBOX_WIDTH
import noobroutes.ui.clickgui.elements.Panel.Companion.PANEL_RADIUS
import noobroutes.ui.clickgui.elements.Panel.Companion.WIDTH
import noobroutes.ui.util.MouseUtils.mouseX
import noobroutes.ui.util.MouseUtils.mouseY
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.CubicBezierAnimation
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.ui.util.elements.textElements.TextBoxElement
import noobroutes.utils.render.*
import noobroutes.utils.render.ColorUtil.brighter
import noobroutes.utils.render.ColorUtil.withAlpha
import noobroutes.utils.render.RenderUtils.loadBufferedImage
import noobroutes.utils.round
import org.lwjgl.input.Keyboard
import kotlin.math.floor

object SearchBar : UiElement(ClickGUIModule.searchBarX.value, ClickGUIModule.searchBarY.value) {
    private val searchIcon = DynamicTexture(loadBufferedImage("/assets/ui/searchIcon.png"))
    private const val SEARCH_ICON_BASE_SIZE = 32f
    private const val SEARCH_ICON_BASE_SIZE_HALF = SEARCH_ICON_BASE_SIZE * 0.5f
    private const val SEARCH_ICON_BASE_STARTING_POSITION = WIDTH * 0.9f
    private const val SEARCH_ICON_BASE_ENDING_POSITION = WIDTH * 0.1f
    private const val SEARCH_ICON_BASE_HEIGHT = HEIGHT * 0.4555f
    private const val SEARCH_ICON_HITBOX_STARTING_POSITION = SEARCH_ICON_BASE_STARTING_POSITION - SEARCH_ICON_BASE_SIZE_HALF
    private const val SEARCH_ICON_HITBOX_HEIGHT = SEARCH_ICON_BASE_HEIGHT - SEARCH_ICON_BASE_SIZE_HALF
    private const val SEARCH_UNDERLINE_BASE_X = SEARCH_ICON_HITBOX_STARTING_POSITION + SEARCH_ICON_BASE_SIZE
    private const val SEARCH_UNDERLINE_HEIGHT = 4f
    private const val SEARCH_UNDERLINE_Y = SEARCH_ICON_HITBOX_HEIGHT + SEARCH_ICON_BASE_SIZE - SEARCH_UNDERLINE_HEIGHT

    private val xIcon = DynamicTexture(loadBufferedImage("/assets/ui/xIcon.png"))
    private const val CLOSE_ICON_HITBOX_SIZE = 18f
    private const val CLOSE_ICON_HITBOX_X = SEARCH_ICON_BASE_STARTING_POSITION - CLOSE_ICON_HITBOX_SIZE * 0.5f
    private const val CLOSE_ICON_HITBOX_Y = HEIGHT * 0.4555f - CLOSE_ICON_HITBOX_SIZE * 0.5f

    private const val CLOSE_ICON_SIZE = 20f
    private const val CLOSE_ICON_X = SEARCH_ICON_BASE_STARTING_POSITION - CLOSE_ICON_SIZE * 0.5f
    private const val CLOSE_ICON_Y = HEIGHT * 0.4555f - CLOSE_ICON_SIZE * 0.5f
    private inline val isHoveredCloseIcon
        get() =
            searchExtended && searchTextBox.elementValue.isNotBlank() && isAreaHovered(
                CLOSE_ICON_HITBOX_X,
                CLOSE_ICON_HITBOX_Y,
                CLOSE_ICON_HITBOX_SIZE,
                CLOSE_ICON_HITBOX_SIZE
            )


    private var x2 = 0f
    private var y2 = 0f
    private var dragging = false
    private var scrollTarget = 0f
    private var scrollOffset = 0f
    private val scrollAnimation = LinearAnimation<Float>(200)
    private inline val isHovered
        get() = isAreaHovered(-BORDER_THICKNESS, -BORDER_THICKNESS, HITBOX_WIDTH, HITBOX_HEIGHT)
    private inline val isMouseOverExtended
        get() = extended && isAreaHovered(-BORDER_THICKNESS, -BORDER_THICKNESS, HITBOX_WIDTH, length.coerceAtLeast(HEIGHT) + DOUBLE_BORDER_THICKNESS)
    private inline val isHoveredSearchIcon
        get() =
            !searchExtended && !searchExtendedAnimation.isAnimating() && isAreaHovered(
                SEARCH_ICON_HITBOX_STARTING_POSITION,
                SEARCH_ICON_HITBOX_HEIGHT,
                SEARCH_ICON_BASE_SIZE,
                SEARCH_ICON_BASE_SIZE
            )
    private val searchIconDrawX get() =
        searchExtendedAnimation.get(SEARCH_ICON_BASE_STARTING_POSITION, SEARCH_ICON_BASE_ENDING_POSITION, !searchExtended)
    private val searchIconSizeMultiplier get() =
        searchExtendedAnimation.get(1f, 0.8f, !searchExtended)

    private inline val extended get() = moduleButtons.isNotEmpty()
    private var length = 0f

    private var searchExtended = false
    private val searchExtendedAnimation = CubicBezierAnimation(200, 0.4, 0, 0.2, 1)

    private val moduleButtons = mutableListOf<ModuleButton>()

    private inline val bottomSegmentColor get() = if (extended) moduleButtonColor else titlePanelColor
    private val separatorColor = clickGUIColor.brighter(1.65f)

    private val searchTextBox = TextBoxElement(
        "",
        0f,
        0f,
        50f,
        20f,
        12f,
        TextAlign.Left,
        0f,
        0f,
        Color.TRANSPARENT,
        19,
        TextBoxElement.TextBoxType.NO_BOX,
        0f,
        "",
        "Search"
    ).apply {
        visible = false
    }

    init {
        addChild(searchTextBox)
    }

    private fun getTotalHeight(offset: Float): Float {
        var y = offset
        for (i in moduleButtons.indices) {
            y += moduleButtons[i].getHeight()
        }
        return y
    }

    override fun doHandleDraw() {
        GlStateManager.pushMatrix()
        translate(x, y)
        scrollOffset = scrollAnimation.get(scrollOffset, scrollTarget).round(0).toFloat()
        val offset = floor(getTotalHeight(scrollOffset))
        if (dragging) {
            updatePosition(floor(x2 + mouseX), floor(y2 + mouseY))
        }

        if (extended) {
            blurRoundedRectangle(-BORDER_THICKNESS, -BORDER_THICKNESS, WIDTH + DOUBLE_BORDER_THICKNESS, offset + HEIGHT + BOTTOM_SEGMENT_HEIGHT + DOUBLE_BORDER_THICKNESS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, 0.5f)
            roundedRectangle(
                0f, 0f, WIDTH, HEIGHT,
                titlePanelColor, Color.TRANSPARENT, Color.TRANSPARENT,
                0f, PANEL_RADIUS, PANEL_RADIUS, 0f, 0f, 0.5f
            )
            rectangleOutline(
                -BORDER_THICKNESS,
                -BORDER_THICKNESS,
                WIDTH + DOUBLE_BORDER_THICKNESS,
                offset + HEIGHT + BOTTOM_SEGMENT_HEIGHT + DOUBLE_BORDER_THICKNESS,
                titlePanelColor,
                10f,
                3f
            )


            roundedRectangle(
                0f, offset + HEIGHT, WIDTH, BOTTOM_SEGMENT_HEIGHT, bottomSegmentColor, bottomSegmentColor, bottomSegmentColor,
                0f, 0f, 0f, PANEL_RADIUS, PANEL_RADIUS, 0.5f
            )
            roundedRectangle(-BORDER_THICKNESS, HEIGHT - HIGHLIGHT_THICKNESS, WIDTH + DOUBLE_BORDER_THICKNESS, HIGHLIGHT_THICKNESS, separatorColor)
        } else {
            blurRoundedRectangle(-BORDER_THICKNESS, -BORDER_THICKNESS, WIDTH + DOUBLE_BORDER_THICKNESS, offset + HEIGHT + DOUBLE_BORDER_THICKNESS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, 0.5f)
            roundedRectangle(-BORDER_THICKNESS, -BORDER_THICKNESS, WIDTH + DOUBLE_BORDER_THICKNESS, HEIGHT + BORDER_THICKNESS, titlePanelColor, 10f)
        }

        if (isHoveredSearchIcon) {
            roundedRectangle(
                SEARCH_ICON_HITBOX_STARTING_POSITION,
                SEARCH_ICON_HITBOX_HEIGHT,
                SEARCH_ICON_BASE_SIZE,
                SEARCH_ICON_BASE_SIZE,
                Color.WHITE.withAlpha(0.4f),
                8f
            )
        }
        drawDynamicTexture(
            searchIcon,
            searchIconDrawX - SEARCH_ICON_BASE_SIZE_HALF * searchIconSizeMultiplier,
            SEARCH_ICON_BASE_HEIGHT - SEARCH_ICON_BASE_SIZE_HALF * searchIconSizeMultiplier,
            SEARCH_ICON_BASE_SIZE * searchIconSizeMultiplier,
            SEARCH_ICON_BASE_SIZE * searchIconSizeMultiplier
        )
        if (searchExtended || searchExtendedAnimation.isAnimating()) {
            val extendedMultiplier = searchExtendedAnimation.get(
                0f,
                1f,
                !searchExtended)

            val width =
                (SEARCH_ICON_BASE_STARTING_POSITION - SEARCH_ICON_BASE_ENDING_POSITION + SEARCH_ICON_BASE_SIZE) * extendedMultiplier
////
            roundedRectangle(
                SEARCH_UNDERLINE_BASE_X - width,
                SEARCH_UNDERLINE_Y,
                width,
                SEARCH_UNDERLINE_HEIGHT,
                Color.WHITE.withAlpha(0.4f),
                1.70f
            )
            if (extendedMultiplier < 0.25) {
                GlStateManager.popMatrix()
                return
            }
            searchTextBox.visible = true
            GlStateManager.scale(extendedMultiplier, 1f, 1f)
            //searchTextBox.setGlobalScale(getEffectiveXScale() * extendedMultiplier, 1f)
            searchTextBox.updatePosition((SEARCH_UNDERLINE_BASE_X - width + SEARCH_ICON_BASE_SIZE) / extendedMultiplier, SEARCH_ICON_BASE_HEIGHT - 10f)
            searchTextBox.doHandleDraw()
            GlStateManager.scale(1 / extendedMultiplier, 1f, 1f)

            if (searchTextBox.elementValue.isNotBlank()) {
                if (isHoveredCloseIcon) {
                    roundedRectangle(
                        CLOSE_ICON_HITBOX_X,
                        CLOSE_ICON_HITBOX_Y,
                        CLOSE_ICON_HITBOX_SIZE,
                        CLOSE_ICON_HITBOX_SIZE,
                        Color.WHITE.withAlpha(0.4f),
                        5f
                    )
                }
                drawDynamicTexture(xIcon, CLOSE_ICON_X, CLOSE_ICON_Y, CLOSE_ICON_SIZE, CLOSE_ICON_SIZE)
            }


        } else {
            searchTextBox.visible = false
        }
        var startY = scrollOffset + HEIGHT
        stencilRoundedRectangle(0f, HEIGHT, WIDTH, offset + BOTTOM_SEGMENT_HEIGHT)
        if (moduleButtons.isNotEmpty()) {
            for (button in moduleButtons) {
                button.visible = true
                button.updatePosition(0f, startY)
                button.doHandleDraw()
                startY += button.getHeight()
            }
            length = startY + 5f
        }
        popStencil()
        GlStateManager.popMatrix()
    }

    fun onGuiClosed(){
        closeTextBox()
    }

    private fun closeTextBox(){
        searchTextBox.insertionCursor = 0
        searchTextBox.listening = false
        searchTextBox.selectionStart = 0
        searchTextBox.selectionEnd = 0
        searchTextBox.elementValue = ""
        searchExtended = false
        searchExtendedAnimation.start(true)
        uiChildren.removeAll { it in moduleButtons }
        moduleButtons.clear()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        //if (moduleButtons.any { it.handleMouseClicked(mouseButton) }) return true
        if (mouseButton != 0) return false
        if (searchExtended && searchTextBox.elementValue.isBlank()) {
            searchExtendedAnimation.start(bypass = true)
            closeTextBox()
        }

        if (!isHovered) return false
        if (isHoveredSearchIcon) {
            if (searchExtendedAnimation.start()) {
                searchExtended = true
                searchTextBox.insertionCursor = 0
                searchTextBox.listening = true
                searchTextBox.selectionStart = 0
                searchTextBox.selectionEnd = 0
            }
            return true
        }
        if (isHoveredCloseIcon) {
            closeTextBox()
        }


        x2 = x - mouseX
        y2 = y - mouseY
        dragging = true
        return true
    }
//
    private val escapeKeys = listOf(Keyboard.KEY_ESCAPE, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_RETURN)

    override fun doHandleKeyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!enabled || !visible) return false
        if (moduleButtons.any { it.doHandleKeyTyped(typedChar, keyCode)}) return true

        if(!searchTextBox.doHandleKeyTyped(typedChar, keyCode)) return false
        if (searchTextBox.elementValue.isBlank() && escapeKeys.contains(keyCode)) {
            closeTextBox()
            return true
        }

        for (module in modules.sortedByDescending { getTextWidth(it.name, 18f) }) {
            if (module.devOnly && !Core.DEV_MODE) {
                if (module.enabled) module.onDisable()
                module.keybinding?.key = Keyboard.KEY_NONE
                module.notPersistent = true
                continue
            }
            if (!module.name.lowercase().contains(searchTextBox.elementValue, true)) {
                val moduleButton = moduleButtons.firstOrNull {it.module.name == module.name} ?: continue
                moduleButtons.remove(moduleButton)
                uiChildren.remove(moduleButton)
                continue
            }
            if (moduleButtons.none { it.module.name == module.name } ) {
                val moduleButton = ModuleButton(0f, module)
                moduleButtons.add(moduleButton)
                uiChildren.add(moduleButton)
            }
        }
        return true
    }

    override fun onScroll(amount: Int): Boolean {
        if (isMouseOverExtended) {
            scrollTarget = (scrollTarget + amount).coerceIn(-length + scrollOffset + 72f, 0f)
            scrollAnimation.start(true)
            return true
        }
        return false
    }


    override fun mouseReleased(): Boolean {
        if (moduleButtons.any { it.handleMouseReleased() }) return true

        dragging = false
        ClickGUIModule.searchBarX.value = x
        ClickGUIModule.searchBarY.value = y
        return false
    }

}