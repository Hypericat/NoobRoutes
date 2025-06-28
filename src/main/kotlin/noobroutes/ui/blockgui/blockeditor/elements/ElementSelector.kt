package noobroutes.ui.blockgui.blockeditor.elements

import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.IBlockState
import noobroutes.ui.blockgui.blockeditor.BlockEditor
import noobroutes.ui.blockgui.blockeditor.Element
import noobroutes.ui.clickgui.ClickGUI.TEXTOFFSET
import noobroutes.ui.clickgui.animations.impl.EaseInOut
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.capitalizeFirst
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text

class ElementSelector(private val property: PropertyEnum<*>, val block: IBlockState) : Element(0f, 0f) {
    private val name = property.name.toString()
    private val options = mutableListOf<String>()

    private val posAnim = EaseInOut(250)
    private val optionsHovered: MutableList<() -> Boolean> = mutableListOf()
    companion object {
        const val WIDTH = 533f
        const val HEIGHT = 25f
        const val SCALE = 15f
    }
    private inline val xLeftBound get() = BlockEditor.originX + x + TEXTOFFSET
    private inline val yBound get() = BlockEditor.originY + y + 30f

    init {
        property.allowedValues.forEach {
            options.add(it.name)
        }
        options.forEachIndexed { index, _ ->
            optionsHovered.add {
                isAreaHovered(
                    xLeftBound,
                    yBound + index * (HEIGHT * 1.1f) + HEIGHT * 0.5f,
                    WIDTH,
                    HEIGHT,
                )
            }
        }
    }

    override fun draw() {
        text(name.capitalizeFirst(), xLeftBound, y + BlockEditor.originY + 17.75f, ColorUtil.textColor, 20f)
        options.forEachIndexed { index, name ->
            roundedRectangle(
                xLeftBound,
                yBound + index * (HEIGHT * 1.1f),
                WIDTH,
                HEIGHT,
                ColorUtil.buttonColor, radius = 10
            )
            text(
                name,
                xLeftBound + (WIDTH * 0.5),
                HEIGHT * 0.5 + yBound + index * (HEIGHT * 1.1f),
                ColorUtil.textColor.darkerIf(optionsHovered[index].invoke()),
                SCALE,
                align = TextAlign.Middle
            )
        }



    }

    override fun getElementHeight(): Float {
        return 104f + (options.size - 1) * (HEIGHT * 1.1f)
    }
}