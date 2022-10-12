package io.github.mcktminecraft.worldtools

import io.github.mcktminecraft.worldtools.commands.Command
import io.github.mcktminecraft.worldtools.commands.HelpCommand
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

const val BASE_USAGE = "Usage: java -jar mckt-world-tools.jar"

private val LOGGER = LoggerFactory.getLogger("MAIN")

fun main(args: Array<String>) {
    val commands = buildMap {
        for (commandClass in Command::class.sealedSubclasses) {
            val instance = commandClass.objectInstance ?: throw Error("Command subclass must be object")
            put(instance.name.lowercase(), instance)
        }
    }
    val command = commands[args.getOrNull(0)?.lowercase() ?: "help"]
    if (args.isEmpty()) {
        LOGGER.info("$BASE_USAGE <command> [args] ...")
        LOGGER.info("")
    }
    if (command == null) {
        LOGGER.error("Unknown command \"{}\"", args[0])
        exitProcess(1)
    }
    if (!command.run(if (args.isEmpty()) args else args.copyOfRange(1, args.size), commands)) {
        HelpCommand.printHelp(command, LOGGER::error)
        exitProcess(1)
    }
}
