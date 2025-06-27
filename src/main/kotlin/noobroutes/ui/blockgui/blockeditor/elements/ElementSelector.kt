package noobroutes.ui.blockgui.blockeditor.elements

import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.IBlockState
import noobroutes.ui.blockgui.blockeditor.BlockEditor
import noobroutes.ui.blockgui.blockeditor.Element
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.utils.render.roundedRectangle

class ElementSelector(val property: PropertyEnum<*>, val block: IBlockState) : Element(0f, 0f) {

    override fun draw() {
        roundedRectangle(BlockEditor.originX + x, BlockEditor.originY + y, 100f, 50f, ColorUtil.buttonColor, 10f)
    }

    override fun getElementHeight(): Float {
        return 104f
    }
}