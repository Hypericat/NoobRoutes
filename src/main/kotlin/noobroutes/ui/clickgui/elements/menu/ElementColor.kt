package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.texture.DynamicTexture
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.font.Font
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.ClickGUI.TEXTOFFSET
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.ModuleButton
import noobroutes.ui.clickgui.util.HoverHandler
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.ui.util.MouseUtils.mouseX
import noobroutes.ui.util.MouseUtils.mouseY
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.animations.impl.EaseInOut
import noobroutes.utils.ColorUtil.brighter
import noobroutes.utils.ColorUtil.darker
import noobroutes.utils.ColorUtil.hsbMax
import noobroutes.utils.ColorUtil.withAlpha
import noobroutes.utils.Utils.COLOR_NORMALIZER
import noobroutes.utils.equalsOneOf
import noobroutes.utils.render.*
import noobroutes.utils.render.RenderUtils.bind
import noobroutes.utils.render.RenderUtils.loadBufferedImage
import org.lwjgl.input.Keyboard
import kotlin.math.floor

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [Element]
 */
class ElementColor(parent: ModuleButton, setting: ColorSetting) :
    Element<ColorSetting>(parent, setting, ElementType.COLOR) {

    private val anim = EaseInOut(200)
    var dragging: Int? = null

    inline val color: Color
        get() = setting.value

    private val hover = HoverHandler(0, 150)
    private val hueGradiant = DynamicTexture(loadBufferedImage("/assets/ui/HueGradient.png"))

    private var hexString = "#FFFFFFFF"
    private var stringBefore = hexString
    private val colorAnim = ColorAnimation(100)
    private var listeningForString = false

    // TODO: MAKE A BETTER DESIGN (FUNCTION IS ALL HERE P MUCH)
    override fun draw() {
        h = floor(anim.get(36f, if (setting.allowAlpha) 285f else 255f, !extended))

        hover.handle(x + w - 41, y, 31.5f, 19f)

        roundedRectangle(x, y, w, h, elementBackground)
        text(name, x + TEXTOFFSET, y + 18f, textColor, 12f, Font.REGULAR)
        roundedRectangle(x + w - 40f, y + 9, 31f, 19f, color.brighter(1 + hover.percent() / 500f), 5f)
        rectangleOutline(x + w - 40f, y + 9, 31f, 19f, color.darker().withAlpha(1f), 5f, 1.5f)

        if (!extended && !anim.isAnimating()) return
        stencilRoundedRectangle(x + 2, y, w - 4, h, 0f)
        // SATURATION AND BRIGHTNESS

        drawHSBBox(x + 10f, y + 38f, w - 20f, 170f, color.hsbMax())

        val sbPointer = Pair((x + 10f + setting.saturation * 220), (y + 38f + (1 - setting.brightness) * 170))
        circle(sbPointer.first, sbPointer.second, 9f, Color.TRANSPARENT, color.darker(.5f).withAlpha(1f), 3f)

        // HUE

        drawDynamicTexture(hueGradiant, x + 10f, y + 214f, w - 20f, 15f)
        rectangleOutline(x + 10f, y + 214f, w - 20f, 15f, buttonColor, 1f, 2.5f)

        val hue = x + 10f + setting.hue * 221f to y + 221f
        circle(hue.first, hue.second, 9f, color.hsbMax(), color.hsbMax().darker(.5f), 2f)

        // ALPHA
        if (setting.allowAlpha) {
            gradientRect(x + 10f, y + 235f, w - 20f, 15f, Color.TRANSPARENT, color.withAlpha(1f), 5f, GradientDirection.Right, Color.DARK_GRAY, 2.5f)

            val alpha = Pair((x + 10f + setting.alpha * 220f), y + 243f)
            circle(alpha.first, alpha.second, 9f, Color.WHITE.withAlpha(setting.alpha), Color.GRAY, 2f)
        }

        when (dragging) {
            0 -> {
                setting.saturation = (mouseX - (x + 10f)) / 220f
                setting.brightness = -((mouseY - (y + 38f)) - 170f) / 170f
            }
            1 -> setting.hue = (mouseX - (x + 10f)) / (w - 20f)
            2 -> setting.alpha = (mouseX - (x + 10f)) / (w - 20f)
        }

        if (dragging != null) {
            hexString = "#${color.hex}"
            stringBefore = hexString
        }

        val stringWidth = getTextWidth(hexString, 12f)
        roundedRectangle(x + w * 0.5 - stringWidth * 0.5 - 12, y + 260, stringWidth + 24, 22f, buttonColor, 5f)
        text(hexString, x + w * 0.5, y + 271, Color.WHITE, 12f, Font.REGULAR, TextAlign.Middle, TextPos.Middle)

        if (listeningForString || colorAnim.isAnimating()) {
            val color = colorAnim.get(clickGUIColor, buttonColor, listeningForString)
            rectangleOutline(x + w * 0.5 - stringWidth * 0.5 - 13 , y + 259, stringWidth + 25f, 23f, color, 5f,2f)
        }
        popStencil()
        Color.WHITE.bind()
    }

    private fun completeHexString() {
        if (colorAnim.isAnimating()) return
        if (colorAnim.start()) listeningForString = false
        if (hexString.isEmpty()) return
        val stringWithoutHash = hexString.substring(1)
        if (stringWithoutHash.length.equalsOneOf(6, 8)) {
            try {
                val alpha = if (stringWithoutHash.length == 8) stringWithoutHash.substring(6).toInt(16) * COLOR_NORMALIZER else 1f
                val red = stringWithoutHash.substring(0, 2).toInt(16)
                val green = stringWithoutHash.substring(2, 4).toInt(16)
                val blue = stringWithoutHash.substring(4, 6).toInt(16)
                setting.value = Color(red, green, blue, alpha)
                stringBefore = hexString
            } catch (_: Exception) {
                hexString = stringBefore
                return
            }
        } else hexString = stringBefore
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (listeningForString) {
            when (keyCode) {
                Keyboard.KEY_ESCAPE, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_RETURN -> completeHexString()
                Keyboard.KEY_BACK -> hexString = hexString.dropLast(1)
                !in ElementTextField.keyBlackList -> hexString += typedChar.toString()
            }
            hexString = hexString.uppercase()
            return true
        }
        return false
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton == 0) {
            if (isHovered) {
                if (anim.start()) extended = !extended
                return true
            }
            if (!extended) return false

            dragging = when {
                isAreaHovered(x + 10f, y + 38f, w - 20f, 170f) -> 0 // sat & brightness
                isAreaHovered(x + 10f, y + 214f, w - 20f, 15f) -> 1 // hue
                isAreaHovered(x + 10f, y + 235f, w - 20f, 15f) && setting.allowAlpha -> 2 // alpha
                else -> null
            }

            if (isAreaHovered(x + 10f, y + 255f, w, y + 278f)) {
                if (!colorAnim.isAnimating()) {
                    if (listeningForString) completeHexString()
                    else listeningForString = true
                }
            } else if (listeningForString) listeningForString = false

        } else if (mouseButton == 1) {
            if (isHovered) {
                if (anim.start()) extended = !extended
                return true
            }
        }
        return false
    }

    override fun mouseReleased(state: Int) {
        dragging = null
    }

    override val isHovered: Boolean
        get() = isAreaHovered(x + w - 41, y + 9, 31.5f, 19f)
}