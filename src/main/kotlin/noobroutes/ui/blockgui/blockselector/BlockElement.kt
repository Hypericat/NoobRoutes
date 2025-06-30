package noobroutes.ui.blockgui.blockselector

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import noobroutes.features.dungeon.Brush
import noobroutes.ui.blockgui.blockeditor.BlockEditor
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.clickgui.util.ColorUtil.darker
import noobroutes.ui.clickgui.util.ColorUtil.multiplyAlpha
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.render.RenderUtils.drawItem
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
                ColorUtil.buttonColor.darker(0.4f).multiplyAlpha(1.3f)
            )
        }
    }

    abstract fun mouseClicked()


}