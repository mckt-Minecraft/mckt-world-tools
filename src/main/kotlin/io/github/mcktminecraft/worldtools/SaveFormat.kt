package io.github.mcktminecraft.worldtools

import io.github.mcktminecraft.worldtools.format.Region
import io.github.mcktminecraft.worldtools.format.legacy.LegacyRegion
import io.github.mcktminecraft.worldtools.format.standard.StandardRegion
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.io.path.copyTo

enum class SaveFormat(val friendlyName: String) {
    LEGACY("legacy") {
        override fun convertTo(format: SaveFormat, inFile: File, outFile: File) = when (format) {
            LEGACY -> {
                inFile.toPath().copyTo(outFile.toPath(), overwrite = true)
                true
            }
            STANDARD -> {
                val inRegion = LegacyRegion(0, 0, inFile)
                val outRegion = StandardRegion(0, 0, outFile)
                LOGGER.info("Loading chunks...")
                inRegion.load()
                LOGGER.info("Converting chunks...")
                repeat(32) { x ->
                    repeat(32) { z ->
                        val chunk = inRegion.getChunk(x, z)
                        outRegion.setChunk(x, z, chunk)
                        outRegion.dirtyChunks.set(Region.getChunkIndex(x, z))
                    }
                }
                LOGGER.info("Saving chunks...")
                outRegion.save()
                true
            }
        }
    },
    STANDARD("standard") {
        override fun convertTo(format: SaveFormat, inFile: File, outFile: File) = when (format) {
            LEGACY -> {
                val inRegion = StandardRegion(0, 0, inFile)
                val outRegion = LegacyRegion(0, 0, outFile)
                LOGGER.info("Loading chunks...")
                inRegion.load()
                LOGGER.info("Converting chunks...")
                repeat(32) { x ->
                    repeat(32) { z ->
                        val chunk = inRegion.getChunk(x, z)
                        outRegion.setChunk(x, z, chunk)
                    }
                }
                LOGGER.info("Saving chunks...")
                outRegion.save()
                true
            }
            STANDARD -> {
                inFile.toPath().copyTo(outFile.toPath(), overwrite = true)
                true
            }
        }
    }
    ;

    class ConversionFailedException(message: String) : RuntimeException(message)

    companion object {
        private val LOGGER = LoggerFactory.getLogger("FORMAT")
        private val BY_NAME = buildMap {
            SaveFormat.values().forEach { put(it.friendlyName.lowercase(), it) }
        }

        fun getByName(name: String) = BY_NAME[name.lowercase()]
    }

    abstract fun convertTo(format: SaveFormat, inFile: File, outFile: File): Boolean
}
