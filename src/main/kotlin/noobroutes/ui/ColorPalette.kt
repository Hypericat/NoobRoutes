package noobroutes.ui

import noobroutes.font.FontType
import noobroutes.utils.render.Color

object ColorPalette {
    data class Palette(
        var text: Color,
        var subText: Color,
        var elementPrimary: Color,
        var elementSecondary: Color,
        var backgroundPrimary: Color,
        var backgroundSecondary: Color,
        var font: FontType
    )
    val defaultPalette = Palette(
        Color(205, 214, 244),
        Color(166, 173, 200),
        Color(75, 75, 204),
        Color(95, 95, 222),
        Color(51, 51, 95),
        Color(58, 58, 107),
        FontType.NUNITO
    )

    var currentColorPalette: Palette = defaultPalette

    inline val text get() = currentColorPalette.text
    inline val subText get() = currentColorPalette.subText
    inline val elementPrimary get() = currentColorPalette.elementPrimary
    inline val elementSecondary get() = currentColorPalette.elementSecondary
    inline val backgroundPrimary get() = currentColorPalette.backgroundPrimary
    inline val backgroundSecondary get() = currentColorPalette.backgroundSecondary
    inline val font get() = currentColorPalette.font


    const val TEXT_OFFSET = 9f
}