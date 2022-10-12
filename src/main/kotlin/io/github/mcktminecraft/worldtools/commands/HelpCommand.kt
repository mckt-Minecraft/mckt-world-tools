package io.github.mcktminecraft.worldtools.commands

import io.github.mcktminecraft.worldtools.BASE_USAGE
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

object HelpCommand : Command(
    "help",
    "Get help on commands",
    "[command]",
    "Specify a subcommand to view specific help."
) {
    private val LOGGER = LoggerFactory.getLogger("HELP")

    override fun run(args: Array<String>, commands: Map<String, Command>): Boolean {
        if (args.isEmpty()) {
            LOGGER.info("Summary of commands:")
            val maxWidth = commands.keys.maxOf { it.length }
            for (command in commands.values) {
                LOGGER.info("  + {} -- {}", command.name.padEnd(maxWidth), command.shortHelp)
            }
            return true
        }
        val command = commands[args[0].lowercase()]
        if (command == null) {
            LOGGER.error("Unknown command \"{}\"", args[0])
            exitProcess(1)
        }
        printHelp(command, LOGGER::info)
        return true
    }

    fun printHelp(command: Command, log: (String) -> Unit) {
        log("$BASE_USAGE ${command.name} ${command.usage}")
        log("")
        log(if (command.longHelp == null) {
            command.shortHelp
        } else {
            command.shortHelp.removeSuffix(".") + ". " + command.longHelp
        })
    }
}
