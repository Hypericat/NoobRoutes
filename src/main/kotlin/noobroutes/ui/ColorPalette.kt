package noobroutes.ui

import noobroutes.utils.render.Color

object ColorPalette {
    data class Palette(
        val text: Color,
        val subText: Color,
        val primary: Color,
        val secondary: Color,
        val background: Color,
        val backgroundSecondary: Color
    )
    private val defaultPalette = Palette(
        Color(205, 214, 244),
        Color(166, 173, 200),
        Color(75, 75, 204),
        Color(95, 95, 222),
        Color(51, 51, 95),
        Color(58, 58, 107),
    )

    var currentColorPalette: Palette = defaultPalette

    inline val text get() = currentColorPalette.text
    inline val subText get() = currentColorPalette.subText
    inline val primary get() = currentColorPalette.primary
    inline val secondary get() = currentColorPalette.secondary
    inline val background get() = currentColorPalette.background
    inline val backgroundSecondary get() = currentColorPalette.backgroundSecondary




}