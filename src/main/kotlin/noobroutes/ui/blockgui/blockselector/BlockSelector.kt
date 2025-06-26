package noobroutes.ui.blockgui.blockselector

import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import noobroutes.ui.blockgui.BlockGui.isResetHovered

import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.render.Color
import noobroutes.utils.render.resetScissor
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.scissor
import kotlin.math.floor

object BlockSelector {

    var scrollOffset = 0
    var originX = 100f
    var originY = 200f
    val blockList = mutableListOf<BlockElement>()
    const val WIDTH = 600f
    const val HEIGHT = 600f
    init {
        for (block in Block.blockRegistry) {
            val item = ItemStack(Item.getItemFromBlock(block) ?: continue)


            blockList.add(BlockElement(item))
        }
    }

    var x2 = 0f
    var y2 = 0f
    var dragging = false
    fun mouseClicked() {
        if (isAreaHovered(originX, originY, WIDTH, 70f)) {
            x2 = originX - MouseUtils.mouseX
            y2 = originY - MouseUtils.mouseY
            dragging = true
        }

        if (isAreaHovered(originX, originY, WIDTH, HEIGHT)) {
            blockList.forEach { it.mouseClicked() }
        }
    }
    fun draw() {
        if (dragging) {
            originX = floor(x2 + MouseUtils.mouseX)
            originY = floor(y2 + MouseUtils.mouseY)
        }
        var currentX = 0
        var currentY = 0
        roundedRectangle(
            originX,
            originY,
            HEIGHT,
            70,
            ColorUtil.titlePanelColor,
            ColorUtil.titlePanelColor,
            Color.TRANSPARENT,
            0, 20f, 20f, 0f, 0f, 0f
        )
        roundedRectangle(originX, originY, WIDTH, HEIGHT, ColorUtil.buttonColor, radius = 20)
        val s = scissor(originX, originY + 50, WIDTH, HEIGHT - 100)
        for (block in blockList) {
            currentX++
            if (currentX >= 10) {
                currentY++
                currentX = 0
            }
            block.draw(currentX, currentY)
        }
        resetScissor(s)
    }

}