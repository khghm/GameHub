package com.gamehub.shared.graphics.vfx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush

/**
 * Shader Types
 */
enum class ShaderType {
    VERTEX,
    FRAGMENT,
    COMPUTE
}

/**
 * Shader Uniform Type
 */
sealed class UniformValue {
    data class FloatValue(val value: Float) : UniformValue()
    data class Float2Value(val x: Float, val y: Float) : UniformValue()
    data class ColorValue(val color: Color) : UniformValue()
}

/**
 * Shader Interface - multiplatform abstraction with default no‑op implementation
 */
open class Shader {
    open fun setUniform(name: String, value: UniformValue) {}
    open fun getBrush(): ShaderBrush? = null
}

/**
 * Shader Manager - loads and manages shaders with default no‑op implementation
 */
open class ShaderManager {
    open fun loadShader(
        name: String,
        vertexCode: String? = null,
        fragmentCode: String
    ): Shader = Shader()

    open fun getShader(name: String): Shader? = null

    open fun releaseShader(name: String) {}
}

/**
 * Built-in Shader Sources (common part)
 */
object BuiltinShaderSources {
    val BLUR_FRAGMENT = """
        // Fragment shader for Gaussian Blur
        uniform vec2 resolution;
        uniform sampler2D image;
        uniform float radius;
        
        vec4 blur(vec2 uv) {
            vec4 color = vec4(0.0);
            float total = 0.0;
            float offset = 1.0 / resolution.x;
            for (float x = -4.0; x <= 4.0; x++) {
                for (float y = -4.0; y <= 4.0; y++) {
                    float weight = exp(-(x*x + y*y)/(2.0*radius*radius));
                    color += texture2D(image, uv + vec2(x, y)*offset) * weight;
                    total += weight;
                }
            }
            return color / total;
        }
        
        void main() {
            gl_FragColor = blur(gl_FragCoord.xy/resolution);
        }
    """.trimIndent()

    val GLOW_FRAGMENT = """
        // Fragment shader for Glow effect (combines blur)
        uniform vec2 resolution;
        uniform sampler2D image;
        uniform float intensity;
        
        void main() {
            vec2 uv = gl_FragCoord.xy/resolution;
            vec4 color = texture2D(image, uv);
            vec4 blurred = color; // use blur pass
            gl_FragColor = mix(color, blurred, intensity);
        }
    """.trimIndent()
}
