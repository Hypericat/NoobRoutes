package noobroutes.features.render

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.features.Module
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.SelectorSetting
import noobroutes.font.FontType
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.elements.TextBoxElement
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color

object TemporaryColorPalletModule : Module(
    "ColorPallet",
    description = "Temporary color pallet module, if this is still in during a non-beta release, spam wadey."
) {

    val text by ColorSetting("Text", Color(205, 214, 244), description = "")
    val subText by ColorSetting("subText", Color(166, 173, 200), description = "")
    val elementPrimary by ColorSetting("elementPrimary", Color(75, 75, 204), description = "")
    val elementSecondary by ColorSetting("elementSecondary", Color(95, 95, 222), description = "")
    val backgroundPrimary by ColorSetting("backgroundPrimary", Color(51, 51, 95), description = "")
    val backgroundSecondary by ColorSetting("backgroundSecondary", Color(58, 58, 107), description = "")
    val font by SelectorSetting("Font", "NUNITO", FontType.entries.map { it.name }.toCollection(ArrayList()), description = "")

    @SubscribeEvent
    fun onTick(tickEvent: TickEvent.ClientTickEvent) {
        if (tickEvent.isEnd) return

    }


}