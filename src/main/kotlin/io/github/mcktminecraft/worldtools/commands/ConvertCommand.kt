package io.github.mcktminecraft.worldtools.commands

import io.github.mcktminecraft.worldtools.SaveFormat
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

object ConvertCommand : Command(
    "convert",
    "Convert from one save format to another",
    "<in-format> <in-file> <out-format> <out-file>"
) {
    private val LOGGER = LoggerFactory.getLogger("CONVERT")

    override fun run(args: Array<String>, commands: Map<String, Command>): Boolean {
        if (args.size < 4) return false
        val inFormat = getSaveFormat(args[0])
        val inFile = File(args[1])
        val outFormat = getSaveFormat(args[2])
        val outFile = File(args[3])
        LOGGER.info("Converting ${inFile.name} from ${inFormat.friendlyName} to ${outFormat.friendlyName}...")
        val startTime = System.nanoTime()
        try {
            if (!inFormat.convertTo(outFormat, inFile, outFile)) {
                throw SaveFormat.ConversionFailedException("Unsupported conversion")
            }
        } catch (e: Exception) {
            val baseMessage = "Conversion failed in ${getDuration(startTime)}"
            if (e is SaveFormat.ConversionFailedException) {
                LOGGER.error("{}: {}", baseMessage, e.message)
            } else {
                LOGGER.error(baseMessage, e)
            }
            exitProcess(1)
        }
        LOGGER.info("Conversion succeeded in ${getDuration(startTime)}")
        return true
    }

    private fun getDuration(startTime: Long): String {
        val duration = (System.nanoTime() - startTime).nanoseconds
        return if (duration.inWholeMilliseconds <= 1500) {
            "${duration.toDouble(DurationUnit.MILLISECONDS)}ms"
        } else if (duration.inWholeSeconds <= 90) {
            "${duration.toDouble(DurationUnit.SECONDS)} seconds"
        } else if (duration.inWholeMinutes < 60) {
            "${duration.inWholeMinutes}:${duration.toDouble(DurationUnit.SECONDS) % 60} minutes"
        } else {
            "${duration.inWholeHours}:${duration.toDouble(DurationUnit.MINUTES) % 60}:" +
                "${duration.toDouble(DurationUnit.SECONDS) % 3600} hours"
        }
    }

    private fun getSaveFormat(name: String) = SaveFormat.getByName(name)
        ?: run {
            LOGGER.error("Unknown save format \"{}\"", name)
            exitProcess(1)
        }
}
