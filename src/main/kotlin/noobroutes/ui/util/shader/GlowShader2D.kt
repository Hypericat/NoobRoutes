package noobroutes.ui.util.shader

import noobroutes.Core.mc
import org.lwjgl.opengl.GL20

object GlowShader2D : FramebufferShader("source/glowShader2D") {
    override fun setupUniforms() {
        setupUniform("u_texture")
        setupUniform("u_resolution")
    }

    override fun updateUniforms() {
        GL20.glUniform1i(getUniform("u_texture"), 0)
        GL20.glUniform2f(getUniform("u_resolution"), mc.displayWidth.toFloat(), mc.displayHeight.toFloat())
    }


}