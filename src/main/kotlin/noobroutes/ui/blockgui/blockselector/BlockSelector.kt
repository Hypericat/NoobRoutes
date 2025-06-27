package noobroutes.ui.blockgui.blockselector

import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.equalsOneOf
import noobroutes.utils.render.Color
import noobroutes.utils.render.resetScissor
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.scissor
import noobroutes.utils.render.text
import noobroutes.utils.skyblock.devMessage
import kotlin.math.floor
import kotlin.math.sign

object BlockSelector {

    var scrollOffset = 78f
    var originX = 100f
    var originY = 200f
    val blockList = mutableListOf<BlockElement>()
    const val WIDTH = 600f
    const val HEIGHT = 600f
    init {
        for (block in Block.blockRegistry) {
            if (block.registryName.equalsOneOf("minecraft:farmland", "minecraft:lit_furnace")) continue
            val item = ItemStack(Item.getItemFromBlock(block) ?: continue)
            blockList.add(BlockElement(0, 0, item, block))
        }
    }

    fun onScroll(amount: Int) {
        val actualAmount = amount.sign * 16
        scrollOffset += actualAmount
    }
    var lastTime = System.currentTimeMillis()
    fun smoothScrollOffset() {
        val deltaTime = (System.currentTimeMillis() - lastTime) * 0.005f
        lastTime = System.currentTimeMillis()
        val target = scrollOffset.coerceIn(-167f, 78f)
        scrollOffset += (target - scrollOffset) * deltaTime
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

        if (isAreaHovered(originX, originY + 70f, WIDTH, HEIGHT)) {
            blockList.forEach { it.mouseClicked() }
        }
    }
    fun draw() {
        smoothScrollOffset()
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
        text("Block Selector", originX + 20, originY + 37.5, Color.WHITE, size = 30)
        val s = scissor(originX, originY + 70, WIDTH, HEIGHT - 100)
        for (block in blockList) {
            if (currentX >= 10) {
                currentY++
                currentX = 0
            }
            block.x = currentX
            block.y = currentY
            block.draw()
            currentX++
        }
        resetScissor(s)
    }

}