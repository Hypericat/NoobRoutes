package noobroutes.ui.blockgui.blockselector

import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import noobroutes.utils.render.RenderUtils.drawItem

class BlockElement(val block: ItemStack) {

    fun draw(x: Int, y: Int) {
        block.drawItem(x * 36f, y * 36f + BlockSelector.scrollOffset)
    }

    fun mouseClicked() {

    }


}