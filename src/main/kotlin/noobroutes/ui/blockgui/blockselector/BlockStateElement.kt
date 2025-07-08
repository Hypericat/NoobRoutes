package noobroutes.ui.blockgui.blockselector


import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import noobroutes.features.dungeon.brush.BrushModule
import noobroutes.ui.blockgui.blockeditor.BlockEditor
import noobroutes.utils.render.RenderUtils.drawBlockTexture

class BlockStateElement(x: Int, y: Int, val state: IBlockState, val name: String) : BlockElement(x, y) {
    override var block: Block? = state.block
    override var displayName = name

    override fun draw() {
        super.draw()
        state.drawBlockTexture(x * 50f + BlockSelector.originX + 50f, y * 50f + BlockSelector.scrollOffset + BlockSelector.originY, 1f, 2.3f)
    }

    override fun mouseClicked() {
        if (isHovered) {
            BrushModule.selectedBlockState = state
            BlockEditor.currentBlockName = displayName
        }
    }

}