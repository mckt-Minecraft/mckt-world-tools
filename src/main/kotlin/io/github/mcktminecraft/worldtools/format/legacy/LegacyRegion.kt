package io.github.mcktminecraft.worldtools.format.legacy

import io.github.mcktminecraft.worldtools.WorldChunk
import io.github.mcktminecraft.worldtools.format.Region
import net.kyori.adventure.nbt.BinaryTagIO
import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

class LegacyRegion(val x: Int, val z: Int, val regionFile: File) : Region() {
    companion object {
        private val LOGGER = LoggerFactory.getLogger("LEGACY")
    }

    fun save() {
        BinaryTagIO.writer().write(toNbt().also {
            LOGGER.info("Writing...")
        }, regionFile.toPath(), BinaryTagIO.Compression.GZIP)
    }

    fun load() {
        readNbt(BinaryTagIO.unlimitedReader().read(regionFile.toPath(), BinaryTagIO.Compression.GZIP))
    }

    fun toNbt() = CompoundBinaryTag.builder().apply {
        val chunksPresent = BitSet(chunks.size)
        put("Chunks", ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
            var time = System.nanoTime()
            chunks.toList().forEachIndexed { i, chunk ->
                if (chunk != null) {
                    chunksPresent.set(i)
                    add(chunk.toNbt())
                }
                val currentTime = System.nanoTime()
                if (currentTime - time >= 1_000_000_000) {
                    LOGGER.info("Serializing... {}%", ((i + 1) / 10.24).toInt())
                    time = currentTime
                }
            }
        }.build())
        putLongArray("ChunksPresent", chunksPresent.toLongArray())
    }.build()

    fun readNbt(nbt: CompoundBinaryTag) {
        val chunksPresent = BitSet.valueOf(nbt.getLongArray("ChunksPresent"))
        val chunksNbt = nbt.getList("Chunks")
        var dataIndex = 0
        val baseX = x shl 5
        val baseZ = z shl 5
        var read = 0
        var time = System.nanoTime()
        repeat(32) { x ->
            repeat(32) { z ->
                val memIndex = (x shl 5) + z
                if (chunksPresent[memIndex]) {
                    chunks[memIndex] = WorldChunk(baseX + x, baseZ + z, x, z).also {
                        it.readNbt(chunksNbt.getCompound(dataIndex++))
                    }
                } else {
                    chunks[memIndex] = null
                }
                read++
                val currentTime = System.nanoTime()
                if (currentTime - time >= 1_000_000_000) {
                    LOGGER.info("Deserializing... {}%", (read / 10.24).toInt())
                    time = currentTime
                }
            }
        }
    }
}
