package noobroutes.ui.newclickgui.elements

import noobroutes.features.Category
import noobroutes.ui.util.UiElement
import noobroutes.utils.capitalizeFirst

class ClickGUIBase : UiElement(0f, 0f) {

    init {
        for (category in Category.entries) {
            val name = if (category == Category.FLOOR7) "Floor 7" else category.name.lowercase().capitalizeFirst()
            addChild(Panel(name, category))
        }
    }

}