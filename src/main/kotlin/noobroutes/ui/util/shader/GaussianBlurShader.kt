package noobroutes.ui.util.shader

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20

object GaussianBlurShader {
    private val mc = Minecraft.getMinecraft()
    private var blurShader: Int = 0
    private var backgroundTexture: Int = 0

    // Simple blur shader that samples the background
    private val vertexShaderSource = """
        #version 120
        
        void main() {
            gl_Position = ftransform();
            gl_TexCoord[0] = gl_MultiTexCoord0;
        }
    """.trimIndent()

    private val fragmentShaderSource = """
        #version 120
        
        uniform sampler2D u_texture;
        uniform vec2 u_texelSize;
        uniform float u_radius;
        uniform vec2 u_resolution;
        
        void main() {
            vec2 texCoord = gl_TexCoord[0].xy;
            vec4 color = vec4(0.0);
            float totalWeight = 0.0;
            
            // Simple box blur for better performance
            float pixelRadius = u_radius;
            
            for (float x = -pixelRadius; x <= pixelRadius; x += 1.0) {
                for (float y = -pixelRadius; y <= pixelRadius; y += 1.0) {
                    vec2 offset = vec2(x, y) * u_texelSize;
                    float weight = 1.0;
                    color += texture2D(u_texture, texCoord + offset) * weight;
                    totalWeight += weight;
                }
            }
            
            gl_FragColor = color / totalWeight;
        }
    """.trimIndent()

    fun initShaders() {
        if (blurShader != 0) return

        val vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER)
        GL20.glShaderSource(vertexShader, vertexShaderSource)
        GL20.glCompileShader(vertexShader)

        val fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER)
        GL20.glShaderSource(fragmentShader, fragmentShaderSource)
        GL20.glCompileShader(fragmentShader)

        blurShader = GL20.glCreateProgram()
        GL20.glAttachShader(blurShader, vertexShader)
        GL20.glAttachShader(blurShader, fragmentShader)
        GL20.glLinkProgram(blurShader)

        GL20.glDeleteShader(vertexShader)
        GL20.glDeleteShader(fragmentShader)
    }

    /**
     * Captures the current screen content as a texture for blurring
     * Call this BEFORE drawing any UI elements
     */
    fun captureBackground() {
        initShaders()

        // Delete old texture if it exists
        if (backgroundTexture != 0) {
            GL11.glDeleteTextures(backgroundTexture)
        }

        backgroundTexture = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, backgroundTexture)
        GL11.glCopyTexImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB,
            0, 0, mc.displayWidth, mc.displayHeight, 0
        )

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP)

        // Unbind texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    }

    /**
     * Draws a blurred version of the captured background
     * This respects the current stencil state
     * Call this BETWEEN stencil() and popStencil()
     */
    fun drawBlur(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        if (backgroundTexture == 0 || radius <= 0) return

        // Save current state
        val prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)
        val prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)

        // Use blur shader
        GL20.glUseProgram(blurShader)

        // Set uniforms
        val textureUniform = GL20.glGetUniformLocation(blurShader, "u_texture")
        val texelSizeUniform = GL20.glGetUniformLocation(blurShader, "u_texelSize")
        val radiusUniform = GL20.glGetUniformLocation(blurShader, "u_radius")
        val resolutionUniform = GL20.glGetUniformLocation(blurShader, "u_resolution")

        GL20.glUniform1i(textureUniform, 0)
        GL20.glUniform2f(texelSizeUniform, 1f / mc.displayWidth, 1f / mc.displayHeight)
        GL20.glUniform1f(radiusUniform, radius)
        GL20.glUniform2f(resolutionUniform, mc.displayWidth.toFloat(), mc.displayHeight.toFloat())

        // Bind background texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, backgroundTexture)

        // Draw textured quad - this will be clipped by the active stencil
        GlStateManager.color(1f, 1f, 1f, 1f)
        drawTexturedRect(x, y, width, height)

        // Restore state
        GL20.glUseProgram(prevProgram)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture)
    }

    private fun drawTexturedRect(x: Float, y: Float, width: Float, height: Float) {
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        // Calculate texture coordinates for the specific screen region
        // Convert screen coordinates to normalized texture coordinates [0,1]
        val u1 = x / mc.displayWidth
        val v1 = 1.0f - (y + height) / mc.displayHeight  // Flip Y and use correct bounds
        val u2 = (x + width) / mc.displayWidth
        val v2 = 1.0f - y / mc.displayHeight  // Flip Y and use correct bounds

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0).tex(u1.toDouble(), v1.toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0).tex(u2.toDouble(), v1.toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), y.toDouble(), 0.0).tex(u2.toDouble(), v2.toDouble()).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), 0.0).tex(u1.toDouble(), v2.toDouble()).endVertex()
        tessellator.draw()
    }

    fun cleanup() {
        if (blurShader != 0) {
            GL20.glDeleteProgram(blurShader)
            blurShader = 0
        }
        if (backgroundTexture != 0) {
            GL11.glDeleteTextures(backgroundTexture)
            backgroundTexture = 0
        }
    }

    // Extension function for easier use with your stencil system
    fun blurredBackground(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        drawBlur(x, y, width, height, radius)
    }
}