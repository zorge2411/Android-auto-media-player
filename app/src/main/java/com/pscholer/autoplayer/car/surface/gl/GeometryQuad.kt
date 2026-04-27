package com.pscholer.autoplayer.car.surface.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Unit quad covering NDC [-1, 1]² as a TRIANGLE_STRIP (4 vertices).
 *
 * Vertex layout (positions in NDC, tex coords in [0,1]):
 *   v0 (-1, +1)  v1 (+1, +1)
 *   v2 (-1, -1)  v3 (+1, -1)
 *
 * Texture coords follow standard top-left origin for OES external textures:
 *   (0,1) (1,1)
 *   (0,0) (1,0)
 *
 * Usage:
 *   1. allocate()  — fills native FloatBuffers (call once on GL thread)
 *   2. bindPositions(loc) / bindTexCoords(loc) — called per-frame by OesTextureProgram.draw()
 *   3. No release() needed — FloatBuffers are Java heap; GC collects them.
 */
class GeometryQuad {
    companion object {
        private const val COORDS_PER_POSITION = 2
        private const val COORDS_PER_TEXCOORD = 2
        private const val VERTEX_COUNT = 4
        private const val BYTES_PER_FLOAT = 4
    }

    private var positionBuffer: FloatBuffer? = null
    private var texCoordBuffer: FloatBuffer? = null

    fun allocate() {
        check(positionBuffer == null) { "allocate() called twice" }

        positionBuffer = floatBufferOf(
            -1f,  1f,   // v0 top-left
             1f,  1f,   // v1 top-right
            -1f, -1f,   // v2 bottom-left
             1f, -1f    // v3 bottom-right
        )

        texCoordBuffer = floatBufferOf(
            0f, 1f,     // v0 top-left
            1f, 1f,     // v1 top-right
            0f, 0f,     // v2 bottom-left
            1f, 0f      // v3 bottom-right
        )
    }

    fun bindPositions(attribLocation: Int) {
        val buf = checkNotNull(positionBuffer) { "allocate() not called" }
        buf.position(0)
        GLES20.glEnableVertexAttribArray(attribLocation)
        GLES20.glVertexAttribPointer(
            attribLocation, COORDS_PER_POSITION, GLES20.GL_FLOAT, false,
            COORDS_PER_POSITION * BYTES_PER_FLOAT, buf
        )
    }

    fun bindTexCoords(attribLocation: Int) {
        val buf = checkNotNull(texCoordBuffer) { "allocate() not called" }
        buf.position(0)
        GLES20.glEnableVertexAttribArray(attribLocation)
        GLES20.glVertexAttribPointer(
            attribLocation, COORDS_PER_TEXCOORD, GLES20.GL_FLOAT, false,
            COORDS_PER_TEXCOORD * BYTES_PER_FLOAT, buf
        )
    }

    private fun floatBufferOf(vararg values: Float): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(values); position(0) }
}
