package io.github.mcktminecraft.worldtools.commands

sealed class Command(
    val name: String,
    val shortHelp: String,
    val usage: String = "",
    val longHelp: String? = null
) {
    abstract fun run(args: Array<String>, commands: Map<String, Command>): Boolean

    override fun toString() = "[Command $name]"
}
