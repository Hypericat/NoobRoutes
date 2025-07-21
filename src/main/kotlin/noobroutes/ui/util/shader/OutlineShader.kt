package noobroutes.ui.util.shader

import noobroutes.Core.mc
import noobroutes.utils.Utils.COLOR_NORMALIZER
import noobroutes.utils.render.Color
import org.lwjgl.opengl.GL20

object OutlineShader : FramebufferShader("source/entity/outlineEntity.fsh") {
    override fun setupUniforms() {
        setupUniform("texture")
        setupUniform("texelSize")
        setupUniform("color")
        setupUniform("divider")
        setupUniform("radius")
        setupUniform("maxSample")
    }

    override fun updateUniforms() {
        GL20.glUniform1i(getUniform("texture"), 0)
        GL20.glUniform2f(
            getUniform("texelSize"),
            1f / mc.displayWidth * (radius * quality),
            1f / mc.displayHeight * (radius * quality)
        )
        updateColor(this.color)
        updateThickness(radius)
    }

    private fun updateColor(color: Color) {
        GL20.glUniform4f(getUniform("color"), color.r * COLOR_NORMALIZER, color.g * COLOR_NORMALIZER, color.b * COLOR_NORMALIZER, color.alpha)
    }

    private fun updateThickness(thickness: Float) {
        GL20.glUniform1f(getUniform("radius"), thickness)
    }
}