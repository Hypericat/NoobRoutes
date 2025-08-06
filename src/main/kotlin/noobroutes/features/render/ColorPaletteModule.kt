package noobroutes.features.render

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.features.Module
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.SelectorSetting
import noobroutes.font.FontType
import noobroutes.ui.ColorPalette
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color


object ColorPaletteModule : Module(
    "Color Palette",
    description = "why are you looking at this, this shouldn't be available for the end user"
) {

    @DevOnly
    val text by ColorSetting("Text", Color(205, 214, 244), description = "")
    val subText by ColorSetting("subText", Color(166, 173, 200), description = "", allowAlpha = true)
    val elementPrimary by ColorSetting("elementPrimary", Color(75, 75, 204), description = "", allowAlpha = true)
    val elementSecondary by ColorSetting("elementSecondary", Color(95, 95, 222), description = "", allowAlpha = true)
    val backgroundPrimary by ColorSetting("backgroundPrimary", Color(51, 51, 95), description = "", allowAlpha = true)
    val backgroundSecondary by ColorSetting("backgroundSecondary", Color(58, 58, 107), description = "", allowAlpha = true)



}