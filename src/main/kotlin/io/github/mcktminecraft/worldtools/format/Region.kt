package io.github.mcktminecraft.worldtools.format

import io.github.mcktminecraft.worldtools.WorldChunk

abstract class Region {
    companion object {
        fun getChunkIndex(x: Int, z: Int) = (x shl 5) + z
    }

    val chunks = arrayOfNulls<WorldChunk>(32 * 32)

    fun getChunk(x: Int, z: Int) = chunks[getChunkIndex(x, z)]

    fun setChunk(x: Int, z: Int, chunk: WorldChunk?) {
        chunks[getChunkIndex(x, z)] = chunk
    }
}
