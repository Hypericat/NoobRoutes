package noobroutes.ui.blockgui.blockeditor

import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.properties.PropertyInteger
import net.minecraft.block.state.IBlockState
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import noobroutes.features.dungeon.Brush
import noobroutes.ui.blockgui.BlockDisplay
import noobroutes.ui.blockgui.blockeditor.elements.ElementSelector
import noobroutes.ui.blockgui.blockeditor.elements.ElementSlider
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.util.MouseUtils
import noobroutes.utils.IBlockStateUtils
import noobroutes.utils.capitalizeFirst
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils.drawItem
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text
import kotlin.math.floor

object BlockEditor {
    var originX = 500f
    var originY = 200f

    const val WIDTH = 600f
    const val HEIGHT = 600f
    val elements = mutableListOf<Element>()

    fun keyTyped(typedChar: Char, keyCode: Int) {
        elements.forEach { it.keyTyped(typedChar, keyCode) }
    }
    fun mouseReleased(){
        dragging = false
        elements.forEach { it.mouseReleased() }
    }


    var x2 = 0f
    var y2 = 0f
    var dragging = false
    fun mouseClicked(mouseButton: Int): Boolean {
        elements.forEach { it.mouseClickedAnywhere(mouseButton) }
        if (MouseUtils.isAreaHovered(originX, originY, WIDTH, 70f)) {
            x2 = originX - MouseUtils.mouseX
            y2 = originY - MouseUtils.mouseY
            dragging = true
            return true
        }
        elements.forEach { it.mouseClicked() }
        return false
    }
    var lastBlockState: IBlockState = IBlockStateUtils.airIBlockState
    fun draw() {
        if (lastBlockState != Brush.selectedBlockState) {
            elements.clear()
            Brush.selectedBlockState.propertyNames.forEach {
                when (it) {
                    is PropertyEnum<*> -> {
                        elements.add(ElementSelector(it, Brush.selectedBlockState))
                    }
                    is PropertyInteger -> {
                        elements.add(ElementSlider(it.name, it, Brush.selectedBlockState))
                    }
                }
            }
            lastBlockState = Brush.selectedBlockState
        }
        if (dragging) {
            originX = floor(x2 + MouseUtils.mouseX)
            originY = floor(y2 + MouseUtils.mouseY)
        }

        roundedRectangle(
            originX,
            originY,
            600,
            70,
            ColorUtil.titlePanelColor,
            ColorUtil.titlePanelColor,
            Color.Companion.TRANSPARENT,
            0,
            20f,
            20f,
            0f,
            0f,
            0f
        )
        roundedRectangle(originX, originY, 600, HEIGHT, ColorUtil.buttonColor, radius = 20)
        text(
            Brush.selectedBlockState.block.registryName.removePrefix("minecraft:").capitalizeFirst().replace("_", " "),
            BlockDisplay.originX + 20,
            BlockDisplay.originY + 37.5,
            Color.Companion.WHITE,
            size = 30
        )



        var currentY = 70f
        elements.forEach {
            it.x = 30f
            it.y = currentY
            it.draw()
            currentY += it.getElementHeight()
        }

    }

}