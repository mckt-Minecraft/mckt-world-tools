package io.github.mcktminecraft.worldtools.formattypes

import kotlin.collections.component1
import kotlin.collections.component2

class BlockState(
    val blockId: Identifier = Identifier.EMPTY,
    val properties: Map<String, String> = mapOf(),
) {
    companion object {
        fun fromMap(map: Map<String, String>) = BlockState(
            blockId = Identifier.parse(
                map["blockId"] ?: throw IllegalArgumentException(
                    "Missing blockId field from block state data"
                )
            ),
            properties = map.toMutableMap().apply { remove("blockId") }
        )

        fun parse(state: String): BlockState {
            val bracketIndex = state.indexOf('[')
            val blockId = Identifier.parse(
                if (bracketIndex == -1) {
                    state
                } else {
                    state.substring(0, bracketIndex)
                }
            )
            val properties = if (bracketIndex != -1) {
                if (state[state.length - 1] != ']') {
                    throw IllegalArgumentException("Mismatched [")
                }
                state.substring(bracketIndex + 1, state.length - 1).split(",").associate { prop ->
                    val info = prop.split("=", limit = 2)
                    if (info.size < 2) {
                        throw IllegalArgumentException("Missing \"=\": $prop")
                    }
                    info[0] to info[1]
                }
            } else {
                mapOf()
            }
            return BlockState(blockId, properties)
        }
    }

    private val hash = blockId.hashCode() * 31 + properties.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockState

        if (blockId != other.blockId) return false
        if (properties != other.properties) return false

        return true
    }

    override fun hashCode() = hash

    override fun toString() = buildString {
        append(blockId)
        if (properties.isNotEmpty()) {
            append('[')
            properties.entries.forEachIndexed { index, (name, value) ->
                if (index > 0) {
                    append(',')
                }
                append(name)
                append('=')
                append(value)
            }
            append(']')
        }
    }

    operator fun get(property: String) =
        properties[property]
            ?: throw IllegalArgumentException("Unknown property $property for block $blockId")
}
