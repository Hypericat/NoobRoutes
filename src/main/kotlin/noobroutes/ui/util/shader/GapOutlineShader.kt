package noobroutes.ui.util.shader

import gg.essential.elementa.components.UIBlock
import gg.essential.universal.UMatrixStack
import net.minecraft.client.renderer.GlStateManager
import noobroutes.utils.Utils.COLOR_NORMALIZER
import noobroutes.utils.render.Color
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20

object GapOutlineShader {
    private var program = 0
    private var vertexShader = 0
    private var fragmentShader = 0
    private var isInitialized = false

    private lateinit var shaderCenterUniform: ShaderUniform2f
    private lateinit var shaderSizeUniform: ShaderUniform2f
    private lateinit var shaderRadiusUniform: ShaderUniform1f
    private lateinit var shaderThicknessUniform: ShaderUniform1f
    private lateinit var shaderColorUniform: ShaderUniform4f
    private lateinit var shaderGapCenterUniform: ShaderUniform2f
    private lateinit var shaderGapSizeUniform: ShaderUniform2f
    private lateinit var shaderGapRadiusUniform: ShaderUniform1f

    private val vertexShaderSource = """
        #version 120
        
        varying vec2 f_Position;
        
        void main() {
            gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
            f_Position = gl_Vertex.xy;
        }
    """.trimIndent()

    private val fragmentShaderSource = """
        #version 120
        
        uniform vec2 u_center;
        uniform vec2 u_size;
        uniform float u_radius;
        uniform float u_thickness;
        uniform vec4 u_color;
        uniform vec2 u_gapCenter;
        uniform vec2 u_gapSize;
        uniform float u_gapRadius;
        
        varying vec2 f_Position;
        
        float roundedRectSDF(vec2 centerPos, vec2 size, float radius) {
            vec2 halfSize = size * 0.5;
            vec2 q = abs(centerPos) - halfSize + radius;
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
        }
        
        void main() {
            // Get position relative to rectangle center
            vec2 rectPos = f_Position - u_center;
            
            // Calculate main rectangle SDF
            float rectSDF = roundedRectSDF(rectPos, u_size, u_radius);
            
            // Calculate gap SDF (relative to gap center)
            vec2 gapPos = f_Position - u_gapCenter;
            float gapSDF = roundedRectSDF(gapPos, u_gapSize, u_gapRadius);
            
            // Create border mask (area between outer and inner rectangle)
            float outerRect = rectSDF;
            float innerRect = rectSDF + u_thickness;
            
            // Smooth border mask
            float borderMask = smoothstep(0.5, -0.5, outerRect) * smoothstep(-0.5, 0.5, innerRect);
            
            // Gap mask (removes area where gap should be)
            float gapMask = smoothstep(-0.5, 0.5, gapSDF);
            
            // Final alpha combines border with gap cutout
            float alpha = borderMask * gapMask;
            
            gl_FragColor = vec4(u_color.rgb, u_color.a * alpha);
        }
    """.trimIndent()

    fun initShader() {
        if (isInitialized) return

        vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER)
        GL20.glShaderSource(vertexShader, vertexShaderSource)
        GL20.glCompileShader(vertexShader)

        fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER)
        GL20.glShaderSource(fragmentShader, fragmentShaderSource)
        GL20.glCompileShader(fragmentShader)

        program = GL20.glCreateProgram()
        GL20.glAttachShader(program, vertexShader)
        GL20.glAttachShader(program, fragmentShader)
        GL20.glLinkProgram(program)

        shaderCenterUniform = ShaderUniform2f(program, "u_center")
        shaderSizeUniform = ShaderUniform2f(program, "u_size")
        shaderRadiusUniform = ShaderUniform1f(program, "u_radius")
        shaderThicknessUniform = ShaderUniform1f(program, "u_thickness")
        shaderColorUniform = ShaderUniform4f(program, "u_color")
        shaderGapCenterUniform = ShaderUniform2f(program, "u_gapCenter")
        shaderGapSizeUniform = ShaderUniform2f(program, "u_gapSize")
        shaderGapRadiusUniform = ShaderUniform1f(program, "u_gapRadius")

        isInitialized = true
    }

    fun drawGapOutline(
        matrixStack: UMatrixStack,
        x: Float, y: Float,
        width: Float, height: Float,
        radius: Float, thickness: Float,
        color: Color,
        gapCenterX: Float, gapCenterY: Float,
        gapWidth: Float, gapHeight: Float,
        gapRadius: Float = 0f
    ) {
        if (!isInitialized) {
            initShader()
            if (!isInitialized) return
        }

        bind()

        shaderCenterUniform.setValue(x + width * 0.5f, y + height * 0.5f)
        shaderSizeUniform.setValue(width, height)
        shaderRadiusUniform.setValue(radius)
        shaderThicknessUniform.setValue(thickness)
        shaderColorUniform.setValue(
            color.r * COLOR_NORMALIZER,
            color.g * COLOR_NORMALIZER,
            color.b * COLOR_NORMALIZER,
            color.alpha
        )

        shaderGapCenterUniform.setValue(gapCenterX, gapCenterY)
        shaderGapSizeUniform.setValue(gapWidth, gapHeight)
        shaderGapRadiusUniform.setValue(gapRadius)

        UIBlock.drawBlockWithActiveShader(
            matrixStack,
            color.javaColor,
            x.toDouble(),
            y.toDouble(),
            x.toDouble() + width,
            y.toDouble() + height
        )

        unbind()
    }

    private fun bind() {
        GL20.glUseProgram(program)
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    }

    private fun unbind() {
        GL20.glUseProgram(0)
    }

    fun cleanup() {
        if (program != 0) {
            GL20.glDeleteProgram(program)
            GL20.glDeleteShader(vertexShader)
            GL20.glDeleteShader(fragmentShader)
            program = 0
            isInitialized = false
        }
    }
}

class ShaderUniform1f(program: Int, name: String) {
    private val location = GL20.glGetUniformLocation(program, name)
    fun setValue(value: Float) = GL20.glUniform1f(location, value)
}

class ShaderUniform2f(program: Int, name: String) {
    private val location = GL20.glGetUniformLocation(program, name)
    fun setValue(x: Float, y: Float) = GL20.glUniform2f(location, x, y)
}

class ShaderUniform4f(program: Int, name: String) {
    private val location = GL20.glGetUniformLocation(program, name)
    fun setValue(x: Float, y: Float, z: Float, w: Float) = GL20.glUniform4f(location, x, y, z, w)
}