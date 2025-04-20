package me.modcore.ui.clickgui.util

import me.modcore.features.impl.render.ClickGUIModule
import me.modcore.utils.render.Color

object ColorUtil {

    inline val clickGUIColor: Color
        get() = ClickGUIModule.color
    val buttonColorAlpha = 0.35f
    val moduleButtonColorAlpha = 0.42f
    val elementBackgroundAlpha = 0.43f
    val titlePanelColorAlpha = 0.6f


    val buttonColor = Color(28, 28, 28, buttonColorAlpha)
    val moduleButtonColor = Color(26, 26, 26, moduleButtonColorAlpha)
    val elementBackground = Color(0,0,0, elementBackgroundAlpha)
    val titlePanelColor: Color = Color(12, 12, 12, titlePanelColorAlpha)

    val textColor = Color(239, 239, 239)

    /**
     * Changes or creates a new color with the given alpha. (There is no checks if alpha is in valid range for now.)
     */
    fun Color.withAlpha(alpha: Float, newInstance: Boolean = true): Color {
        if (!newInstance) {
            this.alpha = alpha
            return this
        }
        return Color(r, g, b, alpha)
    }

    fun Color.multiplyAlpha(factor: Float): Color {
        return Color(r, g, b, (alpha * factor).coerceIn(0f, 1f))
    }

    fun Color.brighter(factor: Float = 1.3f): Color {
        return Color(hue, saturation, (brightness * factor.coerceAtLeast(1f)).coerceAtMost(1f), alpha)
    }

    fun Color.brighterIf(condition: Boolean, factor: Float = 1.3f): Color {
        return if (condition) brighter(factor) else this
    }


    fun Color.darker(factor: Float = 0.7f): Color {
        return Color(hue, saturation, brightness * factor, alpha)
    }
    fun Color.setBrightness(brightness: Float = 1f): Color {
        return Color(hue, saturation, brightness, alpha)
    }

    fun Color.darkerIf(condition: Boolean, factor: Float = 0.7f): Color {
        return if (condition) darker(factor) else this
    }
    fun Color.saturationIf(condition: Boolean, factor: Float = 1f): Color {
        return if (condition) saturation(factor) else this
    }
    fun Color.saturation(factor: Float = 1f): Color {
        return Color(hue, factor, brightness, alpha)
    }

    fun Color.hsbMax(): Color {
        return Color(hue, 1f, 1f)
    }
}