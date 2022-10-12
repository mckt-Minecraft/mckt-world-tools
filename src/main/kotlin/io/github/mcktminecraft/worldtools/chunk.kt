package io.github.mcktminecraft.worldtools

import io.github.mcktminecraft.worldtools.formattypes.*
import io.github.mcktminecraft.worldtools.util.AIR
import io.github.mcktminecraft.worldtools.util.AIR_STATE
import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max

private val LOGGER = LoggerFactory.getLogger("CHUNK")

class WorldChunk(val absX: Int, val absZ: Int, val xInRegion: Int, val zInRegion: Int) {
    val sections = arrayOfNulls<ChunkSection>(254)
    val blockEntities = mutableMapOf<BlockPosition, CompoundBinaryTag>()

    fun toMcktNbt() = CompoundBinaryTag.builder().apply {
        val sectionsPresent = BitSet(sections.size)
        put("Sections", ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
            sections.forEachIndexed { i, section ->
                if (section != null && section.blockCount > 0) {
                    sectionsPresent.set(i)
                    add(section.toMcktNbt())
                }
            }
        }.build())
        putLongArray("SectionsPresent", sectionsPresent.toLongArray())
        put("BlockEntities", ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
            blockEntities.values.forEach { add(it) }
        }.build())
    }.build()

    fun toVanillaNbt() = CompoundBinaryTag.builder().apply {
        putInt("DataVersion", 3120)
        putInt("xPos", absX)
        putInt("zPos", absZ)
        putInt("yPos", -127)
        putString("Status", "full")
        putLong("LastUpdate", 0L)
        put("sections", ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
            sections.forEachIndexed { index, section ->
                if (section != null) {
                    add(section.toVanillaNbt())
                    return@forEachIndexed
                }
                add(CompoundBinaryTag.builder().apply {
                    putByte("Y", (index - 127).toByte())
                    put("block_states", CompoundBinaryTag.builder().apply {
                        put("palette", ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
                            add(CompoundBinaryTag.builder().apply {
                                putString("Name", "minecraft:air")
                                put("Properties", CompoundBinaryTag.empty())
                            }.build())
                        }.build())
                    }.build())
                    put("biomes", CompoundBinaryTag.builder().apply {
                        put("palette", ListBinaryTag.builder(BinaryTagTypes.STRING).apply {
                            add(StringBinaryTag.of("minecraft:plains"))
                        }.build())
                    }.build())
                    putByteArray("BlockLight", ByteArray(2048))
                    putByteArray("SkyLight", ByteArray(2048))
                }.build())
            }
        }.build())
        put("block_entities", ListBinaryTag.from(blockEntities.values))
        put("Heightmaps", CompoundBinaryTag.builder().apply {
            putLongArray("MOTION_BLOCKING", LongArray(52))
        }.build())
        put("fluid_ticks", ListBinaryTag.of(BinaryTagTypes.COMPOUND, listOf()))
        put("block_ticks", ListBinaryTag.of(BinaryTagTypes.COMPOUND, listOf()))
        putLong("InhabitedTime", 0L)
        put("structures", CompoundBinaryTag.builder().apply {
            put("References", CompoundBinaryTag.empty())
            put("starts", CompoundBinaryTag.empty())
        }.build())
    }.build()

    fun readMcktNbt(nbt: CompoundBinaryTag) {
        val sectionsPresent = BitSet.valueOf(nbt.getLongArray("SectionsPresent"))
        val sectionsNbt = nbt.getList("Sections")
        var dataIndex = 0
        repeat(254) { i ->
            if (sectionsPresent[i]) {
                sections[i] = ChunkSection(this, i - 127).also { it.readMcktNbt(sectionsNbt.getCompound(dataIndex++)) }
            } else {
                sections[i] = null
            }
        }
        blockEntities.clear()
        val chunkOrigin = BlockPosition(absX shl 4, 0, absZ shl 4)
        nbt.getList("BlockEntities").forEach {
            it as CompoundBinaryTag
            val location = BlockPosition(it.getInt("x"), it.getInt("y"), it.getInt("z"))
            blockEntities[location - chunkOrigin] = it
        }
    }

    fun readVanillaNbt(nbt: CompoundBinaryTag) {
        sections.fill(null)
        for (data in nbt.getList("sections", BinaryTagTypes.COMPOUND)) {
            data as CompoundBinaryTag
            val y = data.getByte("y").toInt()
            val section = ChunkSection(this, y)
            section.readVanillaNbt(data)
            sections[y - 127] = section
        }
        blockEntities.clear()
        val chunkOrigin = BlockPosition(absX shl 4, 0, absZ shl 4)
        nbt.getList("block_entities").forEach {
            it as CompoundBinaryTag
            val location = BlockPosition(it.getInt("x"), it.getInt("y"), it.getInt("z"))
            blockEntities[location - chunkOrigin] = it
        }
    }
}

class ChunkSection(val chunk: WorldChunk, val y: Int) {
    val xInRegion get() = chunk.xInRegion
    val zInRegion get() = chunk.zInRegion

    val data = PalettedStorage(4096, AIR_STATE)

    var blockCount = 0
        private set

    fun toMcktNbt() = CompoundBinaryTag.builder().apply {
        data.compact()
        putInt("BlockCount", blockCount)
        put("Palette", ListBinaryTag.builder(BinaryTagTypes.STRING).apply {
            for (state in data.paletteItems) {
                add(StringBinaryTag.of(state.toString()))
            }
        }.build())
        put("Blocks", CompoundBinaryTag.builder().apply {
            putInt("bits", data.storage.bits)
            putLongArray("data", data.storage.data)
        }.build())
    }.build()

    fun toVanillaNbt() = CompoundBinaryTag.builder().apply {
        data.compact()
        putByte("Y", y.toByte())
        put("block_states", CompoundBinaryTag.builder().apply {
            put("palette", ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
                for (state in data.paletteItems) {
                    add(CompoundBinaryTag.builder().apply {
                        putString("Name", state.blockId.toString())
                        put("Properties", CompoundBinaryTag.builder().apply {
                            for ((key, value) in state.properties) {
                                putString(key, value)
                            }
                        }.build())
                    }.build())
                }
            }.build())
            if (data.paletteSize > 1) {
                putLongArray("data", data.storage.data)
            }
        }.build())
        put("biomes", CompoundBinaryTag.builder().apply {
            put("palette", ListBinaryTag.builder(BinaryTagTypes.STRING).apply {
                add(StringBinaryTag.of("minecraft:plains"))
            }.build())
        }.build())
        putByteArray("BlockLight", ByteArray(2048))
        putByteArray("SkyLight", ByteArray(2048))
    }.build()

    fun readMcktNbt(nbt: CompoundBinaryTag) {
        blockCount = nbt.getInt("BlockCount")
        data.setPaletteItems(nbt.getList("Palette").map {
            if (it is StringBinaryTag) {
                BlockState.parse(it.value())
            } else {
                it as CompoundBinaryTag
                BlockState.fromMap(it.associate { (key, value) ->
                    value as StringBinaryTag
                    key to value.value()
                })
            }
        })
        val blocks = nbt.getCompound("Blocks")
        data.storage = SimpleBitStorage(blocks.getInt("bits"), 4096, blocks.getLongArray("data"))
    }

    fun readVanillaNbt(nbt: CompoundBinaryTag) {
        val states = nbt.getCompound("block_states")
        data.setPaletteItems(states.getList("palette", BinaryTagTypes.COMPOUND).map {
            it as CompoundBinaryTag
            BlockState(
                Identifier.parse(it.getString("Name")),
                it.getCompound("Properties").associate { (key, value) ->
                    key to (value as StringBinaryTag).value()
                }
            )
        })
        val storage = states.getLongArray("data", null)
        if (storage != null) {
            data.storage = SimpleBitStorage(max(4, 32 - data.paletteSize.countLeadingZeroBits()), 4096, storage)
        }
        blockCount = 0
        repeat(4096) { i ->
            if (data[i].blockId != AIR) {
                blockCount++
            }
        }
    }
}
