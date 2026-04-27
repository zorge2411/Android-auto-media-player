package com.pscholer.autoplayer.car.surface.gl

import android.opengl.GLES20
import android.util.Log

/**
 * GLES2 program that draws an OES external texture (SurfaceTexture) onto the full viewport.
 *
 * Vertex shader: receives aPosition (NDC quad corners) and aTexCoord, multiplies by uMVPMatrix
 * to apply aspect-ratio + rotation transform, passes vTexCoord to fragment stage.
 *
 * Fragment shader: samples samplerExternalOES using the OES_EGL_image_external extension —
 * required for SurfaceTexture-backed textures produced by ExoPlayer.
 *
 * Usage:
 *   1. init()       — compile shaders, link program, cache attribute/uniform locations
 *   2. draw(mvp)    — bind program, upload matrix, draw quad via glDrawArrays
 *   3. release()    — delete program (not the texture — caller owns it)
 *
 * Source: Phase 3 RESEARCH.md Pattern 6.
 */
class OesTextureProgram {
    companion object {
        private const val TAG = "OesTextureProgram"

        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            varying vec2 vTexCoord;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }

    private var program = 0
    private var aPosition = -1
    private var aTexCoord = -1
    private var uMVPMatrix = -1
    private var uTexture = -1

    fun init() {
        check(program == 0) { "init() called twice" }
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES20.glCreateProgram()
        check(program != 0) { "glCreateProgram failed" }
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program); program = 0
            "glLinkProgram failed: $log"
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)

        aPosition  = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoord  = GLES20.glGetAttribLocation(program, "aTexCoord")
        uMVPMatrix = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uTexture   = GLES20.glGetUniformLocation(program, "uTexture")
        Log.i(TAG, "Program linked (aPos=$aPosition aTC=$aTexCoord uMVP=$uMVPMatrix uTex=$uTexture)")
    }

    /**
     * Draw the quad. Caller must have already called makeCurrent() and bound the OES texture.
     *
     * @param mvpMatrix 16-float column-major MVP matrix (from AspectRatioCalculator.computeVertexTransform)
     * @param quad      GeometryQuad providing vertex + texcoord buffers
     * @param textureId OES texture name produced by SurfaceTexture
     */
    fun draw(mvpMatrix: FloatArray, quad: GeometryQuad, textureId: Int) {
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(0x8D65 /* GL_TEXTURE_EXTERNAL_OES */, textureId)
        GLES20.glUniform1i(uTexture, 0)

        GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0)

        quad.bindPositions(aPosition)
        quad.bindTexCoords(aTexCoord)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)
    }

    fun release() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
            Log.d(TAG, "Program deleted")
        }
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        check(shader != 0) { "glCreateShader($type) failed" }
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            "glCompileShader(${if (type == GLES20.GL_VERTEX_SHADER) "VS" else "FS"}) failed: $log"
        }
        return shader
    }
}
