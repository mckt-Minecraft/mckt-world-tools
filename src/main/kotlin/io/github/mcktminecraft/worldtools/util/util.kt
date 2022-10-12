package io.github.mcktminecraft.worldtools.util

import io.github.mcktminecraft.worldtools.format.Region
import io.github.mcktminecraft.worldtools.formattypes.BlockState
import io.github.mcktminecraft.worldtools.formattypes.Identifier
import org.intellij.lang.annotations.Language
import java.io.File
import kotlin.math.ceil

val AIR = Identifier("air")
val AIR_STATE = BlockState(AIR)

@Language("RegExp")
const val N = """(-?[0-9]+)"""

typealias RegionCreator = (x: Int, z: Int, regionFile: File) -> Region

fun Double.ceilToInt() = ceil(this).toInt()
