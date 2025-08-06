package noobroutes.ui.blockgui

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import noobroutes.features.dungeon.brush.BrushModule
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.utils.render.RenderUtils.drawItem
import noobroutes.utils.render.roundedRectangle

object BlockDisplay {
    var originX = 0f
    var originY = 0f


    fun draw() {

        roundedRectangle(originX, originY, 300, 370, buttonColor, radius = 5)
        val item = ItemStack(Item.getItemFromBlock(BrushModule.selectedBlockState.block) ?: return)
        item.drawItem(18.75f + originX, 88.75f + originY, scale = 14f)
    }




}