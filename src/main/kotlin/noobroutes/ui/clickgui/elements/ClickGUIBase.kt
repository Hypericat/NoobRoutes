package noobroutes.ui.clickgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.Category
import noobroutes.features.render.ClickGUIModule
import noobroutes.ui.util.UiElement
import noobroutes.utils.capitalizeFirst

class ClickGUIBase : UiElement(0f, 0f) {



    init {
        addChild(SearchBar)
        for (category in Category.entries) {
            val name = if (category == Category.FLOOR7) "Floor 7" else category.name.lowercase().capitalizeFirst()
            addChild(Panel(name, category))
        }
    }

    fun removePanel(panel: Panel) {
        uiChildren.remove(panel)
    }

    override fun doDrawChildren() {
        for (i in uiChildren.indices) {
            uiChildren[i].doHandleDraw()
            GlStateManager.translate(0f, 0f, -8f)
        }
    }

    fun onGuiInit(){
        for (i in uiChildren.indices) {
            val panel = uiChildren[i] as? Panel ?: continue
            panel.updatePosition(ClickGUIModule.panelX[panel.category]!!.value, ClickGUIModule.panelY[panel.category]!!.value)
            panel.extended = ClickGUIModule.panelExtended[panel.category]!!.enabled
            panel.updatingModuleButtons()
        }
        SearchBar.updatePosition(ClickGUIModule.searchBarX.value, ClickGUIModule.searchBarY.value)
    }

}