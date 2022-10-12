package io.github.mcktminecraft.worldtools.format.standard

import io.github.mcktminecraft.worldtools.WorldChunk
import io.github.mcktminecraft.worldtools.format.DirtiableRegion
import net.kyori.adventure.nbt.BinaryTagIO
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

class StandardRegion(val x: Int, val z: Int, val regionFile: File) : DirtiableRegion() {
    companion object {
        private val LOGGER = LoggerFactory.getLogger("STANDARD")
    }

    override fun save() {
        openFs().use { fs ->
            val count = dirtyChunks.cardinality()
            var written = 0
            var time = System.nanoTime()
            dirtyChunks.stream().forEach { index ->
                val x = index shr 5
                val z = index and 31
                val path = fs.getPath(filename(x, z))
                val chunk = chunks[index]
                if (chunk == null) {
                    path.deleteIfExists()
                } else {
                    BinaryTagIO.writer().write(chunk.toMcktNbt(), path)
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
        openFs().use { fs ->
            val entries = fs.rootDirectories.first().listDirectoryEntries()
            var read = 0
            var time = System.nanoTime()
            entries.forEach { path ->
                read++
                if (path.nameCount != 1) return@forEach
                val (x, z) = location(path.name) ?: return@forEach
                dirtyChunks.clear(getChunkIndex(x, z))
                setChunk(x, z, WorldChunk(this.x + x, this.z + z, x, z).also { it.readMcktNbt(
                    BinaryTagIO.unlimitedReader().read(path)
                ) })
                val currentTime = System.nanoTime()
                if (currentTime - time >= 1_000_000_000) {
                    LOGGER.info("Reading... {}%", read * 100 / entries.size)
                    time = currentTime
                }
            }
        }
    }

    private fun filename(x: Int, z: Int) = x.toString(32) + z.toString(32)

    private fun location(filename: String): Pair<Int, Int>? {
        if (filename.length != 2) return null
        val x = filename[0].digitToIntOrNull(32) ?: return null
        val z = filename[1].digitToIntOrNull(32) ?: return null
        return Pair(x, z)
    }

    private fun openFs() = FileSystems.newFileSystem(URI("jar:" + regionFile.toURI()), mapOf("create" to "true"))
}
