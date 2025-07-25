package noobroutes.ui.blockgui.blockselector

import net.minecraft.block.Block
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.ColorUtil.darker
import noobroutes.utils.ColorUtil.multiplyAlpha
import noobroutes.utils.render.roundedRectangle

abstract class BlockElement(var x: Int, var y: Int) {

    abstract var block: Block?
    abstract var displayName: String

    val isHovered: Boolean get() = isAreaHovered(
        x * 50f + BlockSelector.originX + 45f,
        y * 50f + BlockSelector.scrollOffset + BlockSelector.originY - 5,
        48f,
        48f
    )

    open fun draw() {
        if (isHovered) {
            roundedRectangle(
                x * 50f + BlockSelector.originX + 45f,
                y * 50f + BlockSelector.scrollOffset + BlockSelector.originY - 5,
                48f, 48f,
                buttonColor.darker(0.4f).multiplyAlpha(1.3f)
            )
        }
    }

    abstract fun mouseClicked()


}