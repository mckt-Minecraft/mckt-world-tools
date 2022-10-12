package io.github.mcktminecraft.worldtools

import io.github.mcktminecraft.worldtools.formattypes.*
import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag
import org.slf4j.LoggerFactory
import java.util.*

private val LOGGER = LoggerFactory.getLogger("CHUNK")

class WorldChunk(val absX: Int, val absZ: Int, val xInRegion: Int, val zInRegion: Int) {
    val sections = arrayOfNulls<ChunkSection>(254)
    val blockEntities = mutableMapOf<BlockPosition, CompoundBinaryTag>()

    fun toNbt() = CompoundBinaryTag.builder().apply {
        val sectionsPresent = BitSet(sections.size)
        put("Sections", ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
            sections.forEachIndexed { i, section ->
                if (section != null && section.blockCount > 0) {
                    sectionsPresent.set(i)
                    add(section.toNbt())
                }
            }
        }.build())
        putLongArray("SectionsPresent", sectionsPresent.toLongArray())
        put("BlockEntities", ListBinaryTag.builder(BinaryTagTypes.COMPOUND).apply {
            blockEntities.values.forEach { add(it) }
        }.build())
    }.build()

    fun readNbt(nbt: CompoundBinaryTag) {
        val sectionsPresent = BitSet.valueOf(nbt.getLongArray("SectionsPresent"))
        val sectionsNbt = nbt.getList("Sections")
        var dataIndex = 0
        repeat(254) { i ->
            if (sectionsPresent[i]) {
                sections[i] = ChunkSection(this, i - 127).also { it.readNbt(sectionsNbt.getCompound(dataIndex++)) }
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
}

class ChunkSection(val chunk: WorldChunk, val y: Int) {
    val xInRegion get() = chunk.xInRegion
    val zInRegion get() = chunk.zInRegion

    val data = PalettedStorage(4096, BlockState(Identifier("air")))

    var blockCount = 0
        private set

    fun toNbt() = CompoundBinaryTag.builder().apply {
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

    fun readNbt(nbt: CompoundBinaryTag) {
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
}
