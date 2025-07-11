package noobroutes.utils.render

import gg.essential.universal.UMatrixStack
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import noobroutes.Core.mc
import noobroutes.font.Font
import noobroutes.font.FontType
import noobroutes.ui.ColorPalette
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.util.shader.RoundedRect
import noobroutes.utils.coerceAlpha
import noobroutes.utils.minus
import noobroutes.utils.plus
import noobroutes.utils.render.RenderUtils.loadBufferedImage
import noobroutes.utils.times
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11

val matrix = UMatrixStack.Compat
val scaleFactor get() = ScaledResolution(mc).scaleFactor.toFloat()
private val arrowIcon = DynamicTexture(
    loadBufferedImage(
        "/assets/defnotstolen/clickgui/arrow.png"
    )
)

data class Box(var x: Number, var y: Number, var w: Number, var h: Number)
data class BoxWithClass<T : Number>(var x: T, var y: T, var w: T, var h: T)
fun Box.expand(factor: Number): Box = Box(this.x - factor, this.y - factor, this.w + factor * 2, this.h + factor * 2)
fun Box.isPointWithin(x: Number, y: Number): Boolean {
    return x.toDouble() >= this.x.toDouble() &&
            y.toDouble() >= this.y.toDouble() &&
            x.toDouble() <= (this.x.toDouble() + this.w.toDouble()) &&
            y.toDouble() <= (this.y.toDouble() + this.h.toDouble())
}

fun roundedRectangle(
    x: Number, y: Number, w: Number, h: Number,
    color: Color, borderColor: Color, shadowColor: Color,
    borderThickness: Number, topL: Number, topR: Number, botL: Number, botR: Number, edgeSoftness: Number,
    color2: Color = color, gradientDir: Int = 0, shadowSoftness: Float = 0f
) {
    matrix.runLegacyMethod(matrix.get()) {
        RoundedRect.drawRectangle(
            matrix.get(),
            x.toFloat(),
            y.toFloat(),
            w.toFloat(),
            h.toFloat(),
            color,
            borderColor,
            shadowColor,
            borderThickness.toFloat(),
            topL.toFloat(),
            topR.toFloat(),
            botL.toFloat(),
            botR.toFloat(),
            edgeSoftness.toFloat(),
            color2,
            gradientDir,
            shadowSoftness
        )
    }
}

fun roundedRectangle(x: Number, y: Number, w: Number, h: Number, color: Color, radius: Number = 0f, edgeSoftness: Number = 0.5f) =
    roundedRectangle(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, color, color,
        0f, radius.toFloat(), radius.toFloat(), radius.toFloat(), radius.toFloat(), edgeSoftness)

fun roundedRectangle(box: Box, color: Color, radius: Number = 0f, edgeSoftness: Number = .5f) =
    roundedRectangle(box.x, box.y, box.w, box.h, color, radius, edgeSoftness)

fun <T: Number> roundedRectangle(box: BoxWithClass<T>, color: Color, radius: Number = 0f, edgeSoftness: Number = .5f) =
    roundedRectangle(box.x, box.y, box.w, box.h, color, radius, edgeSoftness)


fun rectangleOutline(x: Number, y: Number, w: Number, h: Number, color: Color, radius: Number = 0f, thickness: Number, edgeSoftness: Number = 1f) {
    roundedRectangle(x, y, w, h, Color.TRANSPARENT, color, Color.TRANSPARENT, thickness, radius, radius, radius, radius, edgeSoftness)
}

fun gradientRect(x: Float, y: Float, w: Float, h: Float, color1: Color, color2: Color, radius: Float, direction: GradientDirection = GradientDirection.Right, borderColor: Color = Color.TRANSPARENT, borderThickness: Number = 0f) {
    if (color1.isTransparent && color2.isTransparent) return
    roundedRectangle(
        x, y, w, h, color1.coerceAlpha(.1f, 1f), borderColor, Color.TRANSPARENT, borderThickness, radius, radius, radius, radius, 3, color2.coerceAlpha(.1f, 1f), direction.ordinal
    )
}

fun drawHSBBox(x: Float, y: Float, w: Float, h: Float, color: Color) {
    matrix.runLegacyMethod(matrix.get()) {
        RoundedRect.drawHSBBox(
            matrix.get(),
            x,
            y,
            w,
            h,
            color,
        )
    }
    rectangleOutline(x-1, y-1, w+2, h+2, Color(38, 38, 38), 3f, 2f)
}

fun circle(x: Number, y: Number, radius: Number, color: Color, borderColor: Color = color, borderThickness: Number = 0f) {
    matrix.runLegacyMethod(matrix.get()) {
        RoundedRect.drawCircle(
            matrix.get(),
            x.toFloat(),
            y.toFloat(),
            radius.toFloat(),
            color,
            borderColor,
            borderThickness.toFloat()
        )
    }
}

fun text(text: String, x: Number, y: Number, color: Color, size: Number, type: Int = Font.REGULAR, align: TextAlign = TextAlign.Left, verticalAlign: TextPos = TextPos.Middle, shadow: Boolean = false, fontType: FontType = ColorPalette.font) {
    fontType.font.text(text, x.toFloat(), y.toFloat(), color, size.toFloat(), align, verticalAlign, shadow, type)
}

fun mcText(text: String, x: Number, y: Number, scale: Number, color: Color, shadow: Boolean = true, center: Boolean = true) {
    RenderUtils.drawText("$text§r", x.toFloat(), y.toFloat(), scale.toDouble(), color, shadow, center)
}

fun textAndWidth(text: String, x: Float, y: Float, color: Color, size: Float, type: Int = Font.REGULAR, align: TextAlign = TextAlign.Left, verticalAlign: TextPos = TextPos.Middle, shadow: Boolean = false, fontType: FontType = ColorPalette.font): Float {
    text(text, x, y, color, size, type, align, verticalAlign, shadow, fontType)
    return getTextWidth(text, size, fontType)
}

fun mcTextAndWidth(text: String, x: Number, y: Number, scale: Number, color: Color, shadow: Boolean = true, center: Boolean = true): Float {
    mcText(text, x, y, scale, color, shadow, center)
    return getMCTextWidth(text).toFloat()
}

fun getMCTextWidth(text: String) = mc.fontRendererObj.getStringWidth(text)

fun getTextWidth(text: String, size: Float, fontType: FontType = ColorPalette.font) = fontType.font.getTextWidth(text, size)

fun getMCTextHeight() = mc.fontRendererObj.FONT_HEIGHT

fun getTextHeight(text: String = "", size: Float, fontType: FontType = ColorPalette.font) = fontType.font.getTextHeight(text, size)

fun translate(x: Number, y: Number, z: Number = 1f) = GlStateManager.translate(x.toDouble(), y.toDouble(), z.toDouble())

fun rotate(degrees: Float, xPos: Float, yPos: Float, zPos: Float, xAxis: Float, yAxis: Float, zAxis: Float) {
    translate(xPos, yPos, zPos)
    GlStateManager.rotate(degrees, xAxis, yAxis, zAxis)
    translate(-xPos, -yPos, -zPos)
}

fun scale(x: Number, y: Number, z: Number = 1f) = GlStateManager.scale(x.toDouble(), y.toDouble(), z.toDouble())

fun dropShadow(x: Number, y: Number, w: Number, h: Number, shadowColor: Color, shadowSoftness: Number, topL: Number, topR: Number, botL: Number, botR: Number) {
    translate(0f, 0f, -100f)

    matrix.runLegacyMethod(matrix.get()) {
        RoundedRect.drawDropShadow(
            matrix.get(),
            (x - shadowSoftness * 0.5).toFloat(),
            (y - shadowSoftness * 0.5).toFloat(),
            (w + shadowSoftness).toFloat(),
            (h + shadowSoftness).toFloat(),
            shadowColor,
            topL.toFloat(),
            topR.toFloat(),
            botL.toFloat(),
            botR.toFloat(),
            shadowSoftness.toFloat()
        )
    }

    translate(0f, 0f, 100f)
}

fun dropShadow(x: Number, y: Number, w: Number, h: Number,  radius: Number, shadowSoftness: Number = 1f, shadowColor: Color = ColorUtil.moduleButtonColor) {
    dropShadow(x, y, w, h, shadowColor, shadowSoftness, radius, radius, radius, radius)
}

fun dropShadow(box: Box, radius: Number, shadowSoftness: Number = 1f, shadowColor: Color = ColorUtil.moduleButtonColor) =
    dropShadow(box.x, box.y, box.w, box.h, radius, shadowSoftness, shadowColor)

data class Scissor(val x: Number, val y: Number, val w: Number, val h: Number, val context: Int)
private val scissorList = mutableListOf(Scissor(0, 0, 16000, 16000, 0))

fun scissor(x: Number, y: Number, w: Number, h: Number): Scissor {
    GL11.glEnable(GL11.GL_SCISSOR_TEST)
    GL11.glScissor(x.toInt(), Display.getHeight() - y.toInt() - h.toInt(), w.toInt(), h.toInt())
    val scissor = Scissor(x, y, w, h, scissorList.size)
    scissorList.add(scissor)
    return scissor
}

fun resetScissor(scissor: Scissor) {
    val nextScissor = scissorList[scissor.context - 1]
    GL11.glScissor(nextScissor.x.toInt(), nextScissor.y.toInt(), nextScissor.w.toInt(), nextScissor.h.toInt())
    GL11.glDisable(GL11.GL_SCISSOR_TEST)
    scissorList.removeLast()
}

fun drawArrow(xpos: Float, ypos: Float, rotation: Float = 90f, scale: Float = 1f, color: Color = Color.WHITE) {
    GlStateManager.pushMatrix()
    GlStateManager.translate(xpos, ypos, 0f)
    GlStateManager.rotate(rotation, 0f, 0f, 1f)
    GlStateManager.scale(scale, scale, 1f)
    GlStateManager.translate(-xpos, -ypos, 0f)
    GlStateManager.color(color.redFloat, color.greenFloat, color.blueFloat, color.alphaFloat)
    drawDynamicTexture(arrowIcon, xpos - 25 * 0.5 * scale, ypos - 25 * 0.5 * scale, 25 * scale, 25 * scale)
    GlStateManager.popMatrix()
}

fun drawDynamicTexture(dynamicTexture: DynamicTexture, x: Number, y: Number, w: Number, h: Number) {

    val isBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND)
    val isAlphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST)
    GlStateManager.pushMatrix()

    if (!isBlendEnabled) GlStateManager.enableBlend()
    if (!isAlphaEnabled) GlStateManager.enableAlpha()

    dynamicTexture.updateDynamicTexture()
    GlStateManager.bindTexture(dynamicTexture.glTextureId)
    RenderUtils.drawTexturedModalRect(
        x.toInt(),
        y.toInt(),
        w.toInt(),
        h.toInt()
    )

    if (!isBlendEnabled) GlStateManager.disableBlend()
    if (!isAlphaEnabled) GlStateManager.disableAlpha()

    GlStateManager.popMatrix()
}

fun wrappedText(text: String, x: Float, y: Float, w: Float, color: Color, size: Float, type: Int = Font.REGULAR, shadow: Boolean = false, fontType: FontType = ColorPalette.font) {
    fontType.font.wrappedText(text, x, y, w, color, size, type, shadow = shadow)
}

fun wrappedTextBounds(text: String, width: Float, size: Float, fontType: FontType = ColorPalette.font): Pair<Float, Float> {
    return fontType.font.wrappedTextBounds(text, width, size)
}

enum class TextAlign {
    Left, Middle, Right
}
enum class TextPos {
    Top, Bottom, Middle
}

enum class GradientDirection {
    Right, Down, Left, Up
}
