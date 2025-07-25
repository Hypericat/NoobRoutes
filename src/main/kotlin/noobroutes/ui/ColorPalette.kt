package noobroutes.ui

import noobroutes.features.render.ClickGUIModule
import noobroutes.font.FontType
import noobroutes.utils.render.Color

object ColorPalette {
    data class Palette(
        var text: Color,
        var clickGUIColor: Color,
        var elementBackground: Color,
        var titlePanelColor: Color,
        var buttonColor: Color,
        var moduleButtonColor: Color,
        var font: FontType
    )
    val defaultPalette = Palette(
        Color(239, 239, 239),
        ClickGUIModule.color,
        Color(0, 0, 0, 0.43f),
        Color(12, 12, 12,  0.6f),
        Color(28, 28, 28, 0.35f),
        Color(26, 26, 26, 0.42f),
        FontType.NUNITO
    )





    var currentColorPalette: Palette = defaultPalette


    inline val textColor get() = currentColorPalette.text
    inline val clickGUIColor get() = currentColorPalette.clickGUIColor
    inline val elementBackground get() = currentColorPalette.elementBackground
    inline val titlePanelColor get() = currentColorPalette.titlePanelColor
    inline val buttonColor get() = currentColorPalette.buttonColor
    inline val moduleButtonColor get() = currentColorPalette.moduleButtonColor
    inline val font get() = currentColorPalette.font


    const val TEXT_OFFSET = 9f
}