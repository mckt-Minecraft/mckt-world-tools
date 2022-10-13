package io.github.mcktminecraft.worldtools.format.anvil

import io.github.mcktminecraft.worldtools.WorldChunk
import io.github.mcktminecraft.worldtools.format.DirtiableRegion
import io.github.mcktminecraft.worldtools.util.ceilToInt
import net.kyori.adventure.nbt.BinaryTagIO
import org.slf4j.LoggerFactory
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.Channels

class AnvilRegion(val x: Int, val z: Int, val regionFile: File) : DirtiableRegion() {
    companion object {
        private val LOGGER = LoggerFactory.getLogger("ANVIL")
    }

    override fun save() {
        RandomAccessFile(regionFile, "rw").use { raf ->
            raf.setLength(0)
            val chos = Channels.newOutputStream(raf.channel)
            val count = dirtyChunks.cardinality()
            var written = 0
            var time = System.nanoTime()
            var currentAddr = 8192L
            dirtyChunks.stream().forEach { index ->
                val x = index shr 5
                val z = index and 31
                val chunk = chunks[index]
                if (chunk != null) {
                    raf.seek(currentAddr + 4)
                    raf.writeByte(2) // Zlib compression
                    BinaryTagIO.writer().write(chunk.toVanillaNbt(), chos, BinaryTagIO.Compression.ZLIB)
                    val length = raf.filePointer - currentAddr - 4
                    raf.seek(currentAddr)
                    raf.writeInt(length.toInt())
                    raf.seek((z shl 5 or x shl 2).toLong())
                    val sectorCount = (length / 4096.0).ceilToInt()
                    raf.writeInt((currentAddr ushr 4).toInt() or sectorCount)
                    currentAddr += sectorCount.toLong() shl 12
                }
                written++
                val currentTime = System.nanoTime()
                if (currentTime - time >= 1_000_000_000) {
                    LOGGER.info("Writing... {}%", written * 100 / count)
                    time = currentTime
                }
            }
        }
    }

    override fun load() {
        RandomAccessFile(regionFile, "r").use { raf ->
            val chis = Channels.newInputStream(raf.channel)
            var read = 0
            var time = System.nanoTime()
            repeat(32) { x ->
                repeat(32) inner@ { z ->
                    read++
                    raf.seek((z shl 5 or x shl 2).toLong())
                    val locationData = raf.readInt()
                    if (locationData == 0) return@inner
                    val dataOffset = (locationData ushr 8).toLong() shl 12
                    raf.seek(dataOffset + 4)
                    val compression = when (val cid = raf.readByte()) {
                        1.toByte() -> BinaryTagIO.Compression.GZIP
                        2.toByte() -> BinaryTagIO.Compression.ZLIB
                        3.toByte() -> BinaryTagIO.Compression.NONE
                        else -> throw IllegalArgumentException("Unknown compression type $cid")
                    }
                    dirtyChunks.clear(getChunkIndex(x, z))
                    setChunk(x, z, WorldChunk(this.x + x, this.z + z, x, z).also { it.readVanillaNbt(
                        BinaryTagIO.unlimitedReader().read(chis, compression)
                    ) })
                    val currentTime = System.nanoTime()
                    if (currentTime - time >= 1_000_000_000) {
                        LOGGER.info("Reading... {}%", (read / 10.24).toInt())
                        time = currentTime
                    }
                }
            }
        }
    }
}
