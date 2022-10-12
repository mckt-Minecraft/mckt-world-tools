package io.github.mcktminecraft.worldtools.format

import io.github.mcktminecraft.worldtools.WorldChunk
import java.util.*

abstract class DirtiableRegion : Region() {
    val dirtyChunks = BitSet(32 * 32)

    override fun setChunk(x: Int, z: Int, chunk: WorldChunk?) {
        super.setChunk(x, z, chunk)
        dirtyChunks.set(getChunkIndex(x, z))
    }
}
