package noobroutes.font

import noobroutes.font.fonts.ManropeFont
import noobroutes.font.fonts.MinecraftFont
import noobroutes.font.fonts.NunitoFont
import noobroutes.font.fonts.OdinFont

enum class FontType(val font: Font) {
    MINECRAFT(MinecraftFont),
    ODIN(OdinFont),
    //MANROPE(ManropeFont),
    //NUNITO(NunitoFont)
}