package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.DynamicListSetting
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.SettingElement

class SettingElementDynamicList(setting: DynamicListSetting) :
    SettingElement<DynamicListSetting>(setting, ElementType.DYNAMIC_LIST) {

    override fun draw() {
        GlStateManager.pushMatrix()

        GlStateManager.popMatrix()
    }

}