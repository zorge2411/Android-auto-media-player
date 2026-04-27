package com.pscholer.autoplayer.car.surface.gl

import android.opengl.GLES20
import android.util.Log

/**
 * GLES2 program that draws an OES external texture (SurfaceTexture) onto the full viewport.
 *
 * Owns the OES texture name so GLVideoPipeline can pass it directly to SurfaceTexture().
 *
 * Vertex shader: applies uMVPMatrix (aspect + rotation) and uTexMatrix (SurfaceTexture transform)
 * Fragment shader: samples samplerExternalOES — required for SurfaceTexture-backed textures.
 *
 * Usage:
 *   1. init()       — compile shaders, link program, gen OES texture, cache locations
 *   2. (set uniforms via public location fields, then call quad.draw)
 *   3. release()    — delete program + texture
 *
 * Source: Phase 3 RESEARCH.md Pattern 6.
 */
class OesTextureProgram {
    companion object {
        private const val TAG = "OesTextureProgram"
        const val GL_TEXTURE_EXTERNAL_OES = 0x8D65

        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uTexMatrix;
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
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

    var programId = 0
        private set
    var oesTextureId = 0
        private set
    var aPositionLoc = -1
        private set
    var aTextureCoordLoc = -1
        private set
    var uMvpMatrixLoc = -1
        private set
    var uTexMatrixLoc = -1
        private set
    var uTextureLoc = -1
        private set

    fun init() {
        check(programId == 0) { "init() called twice" }

        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        programId = GLES20.glCreateProgram()
        check(programId != 0) { "glCreateProgram failed" }
        GLES20.glAttachShader(programId, vs)
        GLES20.glAttachShader(programId, fs)
        GLES20.glLinkProgram(programId)
        val status = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) {
            val log = GLES20.glGetProgramInfoLog(programId)
            GLES20.glDeleteProgram(programId); programId = 0
            "glLinkProgram failed: $log"
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)

        aPositionLoc     = GLES20.glGetAttribLocation(programId, "aPosition")
        aTextureCoordLoc = GLES20.glGetAttribLocation(programId, "aTexCoord")
        uMvpMatrixLoc    = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uTexMatrixLoc    = GLES20.glGetUniformLocation(programId, "uTexMatrix")
        uTextureLoc      = GLES20.glGetUniformLocation(programId, "uTexture")

        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        oesTextureId = texIds[0]
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0)

        Log.i(TAG, "Program linked oesTexId=$oesTextureId " +
            "(aPos=$aPositionLoc aTC=$aTextureCoordLoc uMVP=$uMvpMatrixLoc uTex=$uTexMatrixLoc)")
    }

    fun release() {
        if (oesTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
            oesTextureId = 0
        }
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
            Log.d(TAG, "Program + texture deleted")
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
