package io.github.mcktminecraft.worldtools.formattypes

data class Identifier(val namespace: String, val value: String) {
    companion object {
        val EMPTY = Identifier("", "")

        fun parse(s: String) = s.indexOf(':').let { colonIndex ->
            if (colonIndex == -1) Identifier(s)
            else Identifier(s.substring(0, colonIndex), s.substring(colonIndex + 1))
        }

        fun isBaseValid(c: Char) =
            (c in 'a'..'z') ||
            (c in '0'..'9') ||
            c == '.' || c == '-' || c == '_'
    }

    init {
        if (namespace != "minecraft") {
            for (c in namespace) {
                if (isBaseValid(c)) continue
                throw IllegalArgumentException("Invalid namespace $namespace")
            }
        }
        for (c in value) {
            if (isBaseValid(c) || c == '/') continue
            throw IllegalArgumentException("Invalid value $value")
        }
    }

    constructor(value: String) : this("minecraft", value)

    override fun toString() = "$namespace:$value"

    fun toShortString() = if (namespace == "minecraft") value else "$namespace:$value"
}
