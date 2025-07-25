package noobroutes.ui.blockgui.blockselector

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.ui.ColorPalette.titlePanelColor
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.ceil
import noobroutes.utils.render.*
import kotlin.math.floor
import kotlin.math.sign

object BlockSelector {

    var scrollOffset = 78f
    var originX = 100f
    var originY = 200f
    private val blockList = mutableListOf<BlockElement>()
    private const val WIDTH = 600f
    private const val HEIGHT = 600f
    private val blockBlackListRegex = listOf(
        "skull",
        "banner",
        "paint",
        "frame",
        "sign",
        "pot",
        "bed",
        "stand",
        "egg",
        "head",
        " door",
        "hook",
        "trapped",
        "lamp"
    )


    init {
        val items = mutableListOf<ItemStack>()
        CreativeTabs.tabBlock.displayAllReleventItems(items)
        CreativeTabs.tabDecorations.displayAllReleventItems(items)
        CreativeTabs.tabRedstone.displayAllReleventItems(items)
        for (item in items) {
            if (blockList.any {it.displayName == item.displayName} || blockBlackListRegex.any { item.displayName.contains(it, true) }) continue
            blockList.add(BlockItemStackElement(0, 0, item))
        }
        val cakeElement = BlockItemStackElement(0, 0, ItemStack(Items.cake))
        cakeElement.block = Blocks.cake
        blockList.add(cakeElement)


        blockList.add(BlockItemStackElement(0, 0, ItemStack(Blocks.redstone_lamp)))

        blockList.add(BlockStateElement(0, 0, Blocks.lit_redstone_lamp.defaultState, "Lit Redstone Lamp"))
        blockList.add(BlockItemStackElement(0, 0, ItemStack(Blocks.rail)))
        blockList.add(BlockItemStackElement(0, 0, ItemStack(Blocks.barrier)))
        blockList.add(BlockItemStackElement(0, 0, ItemStack(Blocks.beacon)))
        val lavaElement = BlockItemStackElement(0, 0, ItemStack(Items.lava_bucket))
        lavaElement.block = Blocks.lava
        blockList.add(lavaElement)
        val waterElement = BlockItemStackElement(0, 0, ItemStack(Items.water_bucket))
        waterElement.block = Blocks.water
        blockList.add(waterElement)
    }

    fun onScroll(amount: Int) {
        val actualAmount = amount.sign * 48
        scrollOffset += actualAmount
    }
    private var lastTime = System.currentTimeMillis()
    private fun smoothScrollOffset() {
        val deltaTime = (System.currentTimeMillis() - lastTime) * 0.005f
        lastTime = System.currentTimeMillis()
        val target = scrollOffset.coerceIn((blockList.size * 0.1f).ceil() * -32f, 78f)
        scrollOffset += (target - scrollOffset) * deltaTime
    }



    private var x2 = 0f
    private var y2 = 0f
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
            titlePanelColor,
            titlePanelColor,
            Color.TRANSPARENT,
            0, 20f, 20f, 0f, 0f, 0f
        )
        roundedRectangle(originX, originY, WIDTH, HEIGHT, buttonColor, radius = 20)
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