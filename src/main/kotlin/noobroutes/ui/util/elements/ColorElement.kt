package noobroutes.ui.util.elements

import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.utils.render.Color


//Make it so you can use hex code

class ColorElement(
    name: String,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    override var elementValue: Color
) : UiElement(name, x, y, w, h), ElementValue<Color> {
    override val elementValueChangeListeners = mutableListOf<(Color) -> Unit>()

    companion object {
        8
    }



    override fun draw() {
        TODO("Not yet implemented")
    }


}