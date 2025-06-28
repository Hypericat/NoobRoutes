package noobroutes.ui.blockgui.blockselector

import net.minecraft.block.Block
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

class BlockElement(var x: Int, var y: Int, val blockItem: ItemStack) {

    var block: Block? = Block.getBlockFromItem(blockItem.item)
    var displayName = blockItem.displayName

    val isHovered: Boolean get() = isAreaHovered(
        x * 50f + BlockSelector.originX + 45f,
        y * 50f + BlockSelector.scrollOffset + BlockSelector.originY - 5,
        48f,
        48f
        )

    fun draw() {
        if (isHovered) {
            roundedRectangle(
                x * 50f + BlockSelector.originX + 45f,
                y * 50f + BlockSelector.scrollOffset + BlockSelector.originY - 5,
                48f, 48f,
                ColorUtil.buttonColor.darker(0.4f).multiplyAlpha(1.3f)
            )
        }
        blockItem.drawItem(x * 50f + BlockSelector.originX + 50f, y * 50f + BlockSelector.scrollOffset + BlockSelector.originY, scale = 2.3f)
    }

    fun mouseClicked() {
        if (isHovered) {
            Brush.selectedBlockState = block?.getStateFromMeta(blockItem.metadata) ?: return
            BlockEditor.currentBlockName = displayName
        }
    }


}