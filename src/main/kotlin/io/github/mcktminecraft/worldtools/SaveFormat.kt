package io.github.mcktminecraft.worldtools

import io.github.mcktminecraft.worldtools.format.anvil.AnvilRegion
import io.github.mcktminecraft.worldtools.format.legacy.LegacyRegion
import io.github.mcktminecraft.worldtools.format.standard.StandardRegion
import io.github.mcktminecraft.worldtools.util.RegionCreator
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.io.path.copyTo

enum class SaveFormat(val friendlyName: String, val regionCreator: RegionCreator) {
    LEGACY("legacy", ::LegacyRegion),
    STANDARD("standard", ::StandardRegion),
    ANVIL("anvil", ::AnvilRegion);

    class ConversionFailedException(message: String) : RuntimeException(message)

    companion object {
        private val LOGGER = LoggerFactory.getLogger("FORMAT")
        private val BY_NAME = buildMap {
            SaveFormat.values().forEach { put(it.friendlyName.lowercase(), it) }
        }

        fun getByName(name: String) = BY_NAME[name.lowercase()]

        fun getRegionLocation(file: File): Pair<Int, Int>? {
            val name = file.nameWithoutExtension
            var i = 0
            for (c in name) {
                if (c !in 'a'..'z' && c !in 'A'..'Z') {
                    break
                }
                i++
            }
            if (i == name.length) return null
            val sep = name[i]
            return if (name.count { it == sep } == 2) {
                val parts = name.split(sep)
                try {
                    Pair(parts[1].toInt(), parts[2].toInt())
                } catch (_: NumberFormatException) {
                    null
                }
            } else null
        }
    }

    fun convertTo(toFormat: SaveFormat, inFile: File, outFile: File) {
        if (toFormat == this) {
            inFile.toPath().copyTo(outFile.toPath(), overwrite = true)
            return
        }
        var location = getRegionLocation(inFile)
        if (location == null) {
            LOGGER.warn("Could not infer region location from filename")
            location = Pair(0, 0)
        }
        val inRegion = this.regionCreator(location.first, location.second, inFile)
        val outRegion = toFormat.regionCreator(location.first, location.second, outFile)
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
    }
}
