package noobroutes.ui.blockgui.blockeditor

import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.properties.PropertyInteger
import net.minecraft.block.state.IBlockState
import noobroutes.features.dungeon.brush.BrushModule
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.ui.ColorPalette.titlePanelColor
import noobroutes.ui.blockgui.blockeditor.elements.ElementSelector
import noobroutes.ui.blockgui.blockeditor.elements.ElementSlider
import noobroutes.ui.util.MouseUtils
import noobroutes.utils.IBlockStateUtils
import noobroutes.utils.render.Color
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text
import kotlin.math.floor

object BlockEditor {
    var originX = 500f
    var originY = 200f

    private const val WIDTH = 600f
    var currentBlockName: String = "None"
    val elements = mutableListOf<Element>()

    fun keyTyped(typedChar: Char, keyCode: Int) {
        elements.forEach { it.keyTyped(typedChar, keyCode) }
    }
    fun mouseReleased() {
        dragging = false
        elements.forEach { it.mouseReleased() }
    }
    private val blackListedPropertyRegexs = listOf(
        "color",
        "variant"
    )
    private var x2 = 0f
    private var y2 = 0f
    private var dragging = false
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
    private var lastBlockState: IBlockState = IBlockStateUtils.airIBlockState
    fun draw() {
        if (lastBlockState != BrushModule.selectedBlockState) {
            handleNewBlockState()
        }
        if (dragging) {
            originX = floor(x2 + MouseUtils.mouseX)
            originY = floor(y2 + MouseUtils.mouseY)
        }
        drawTop()
        var currentY = 70f + originY
        elements.forEach {
            roundedRectangle(originX, currentY, WIDTH, it.getElementHeight(), buttonColor)
            it.x = 30f
            it.y = currentY
            it.draw()
            currentY += it.getElementHeight()
        }
        roundedRectangle(
            originX,
            currentY,
            WIDTH,
            30f,
            buttonColor,
            buttonColor,
            Color.TRANSPARENT,
            0,
            0,
            0,
            20f,
            20f,
            0f
        )
    }

    private fun drawTop(){
        roundedRectangle(
            originX,
            originY,
            600,
            70,
            titlePanelColor,
            titlePanelColor,
            Color.TRANSPARENT,
            0,
            20f,
            20f,
            0f,
            0f,
            0f
        )
        roundedRectangle(
            originX,
            originY,
            600,
            70,
            titlePanelColor,
            titlePanelColor,
            Color.TRANSPARENT,
            0,
            20f,
            20f,
            0f,
            0f,
            0f
        )
        text(
            currentBlockName,
            originX + 20,
            originY + 37.5,
            Color.WHITE,
            size = 30
        )
    }
    private fun handleNewBlockState(){
        elements.clear()
        BrushModule.selectedBlockState.propertyNames.forEach { property ->
            if (blackListedPropertyRegexs.any { it.contains(property.name.toString(), true) }) return@forEach
            when (property) {
                is PropertyEnum<*> -> {
                    elements.add(ElementSelector(property, BrushModule.selectedBlockState))
                }
                is PropertyInteger -> {
                    elements.add(ElementSlider(property.name, property, BrushModule.selectedBlockState))
                }
            }
        }
        lastBlockState = BrushModule.selectedBlockState
    }

}