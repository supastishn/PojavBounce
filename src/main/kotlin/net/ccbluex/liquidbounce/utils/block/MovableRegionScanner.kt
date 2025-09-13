/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.block

<<<<<<< HEAD
import net.minecraft.util.math.BlockPos
import kotlin.math.max
import kotlin.math.min

class MovableRegionScanner {
    var currentRegion = Region.EMPTY
=======
import net.ccbluex.liquidbounce.utils.math.contains
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos

private val ORIGIN = BlockBox(BlockPos.ORIGIN)

class MovableRegionScanner {
    var currentRegion = ORIGIN
>>>>>>> upstream/nextgen
        private set

    /**
     * Moves the current region; returns regions that have been newly covered
     */
<<<<<<< HEAD
    fun moveTo(region: Region): Sequence<Region> {
=======
    fun moveTo(region: BlockBox): List<BlockBox> {
>>>>>>> upstream/nextgen
        val lastRegion = this.currentRegion

        this.currentRegion = region

        return when {
            // No new blocks where covered
<<<<<<< HEAD
            lastRegion == region || region in lastRegion -> emptySequence()
            // All blocks are new
            !lastRegion.intersects(region) -> sequenceOf(region)
            // Some of the blocks are new, we have to check...
            else -> overlaps(region, lastRegion).filter { !it.isEmpty() }
        }
    }

    private fun overlaps(region: Region, lastRegion: Region): Sequence<Region> {
        return sequenceOf(
            Region(
                BlockPos(min(region.to.x, lastRegion.to.x), region.from.y, region.from.z),
                BlockPos(max(region.to.x, lastRegion.to.x), region.to.y, region.to.z)
            ),
            Region(
                BlockPos(min(region.from.x, lastRegion.from.x), region.from.y, region.from.z),
                BlockPos(max(region.from.x, lastRegion.from.x), region.to.y, region.to.z)
            ),
            Region(
                BlockPos(region.from.x, min(region.to.y, lastRegion.to.y), region.from.z),
                BlockPos(region.to.x, max(region.to.y, lastRegion.to.y), region.to.z)
            ),
            Region(
                BlockPos(region.from.x, min(region.from.y, lastRegion.from.y), region.from.z),
                BlockPos(region.to.x, max(region.from.y, lastRegion.from.y), region.to.z)
            ),
            Region(
                BlockPos(region.from.x, region.from.y, min(region.to.z, lastRegion.to.z)),
                BlockPos(region.to.x, region.to.y, max(region.to.z, lastRegion.to.z))
            ),
            Region(
                BlockPos(region.from.x, region.from.y, min(region.from.z, lastRegion.from.z)),
                BlockPos(region.to.x, region.to.y, max(region.from.z, lastRegion.from.z))
            )
        )
    }

    fun clearRegion() {
        this.currentRegion = Region.EMPTY
=======
            lastRegion == region || lastRegion.contains(region) -> emptyList()
            // All blocks are new
            !lastRegion.intersects(region) -> listOf(region)
            // Some blocks are new, we have to check...
            else -> computeDifference(region, lastRegion)
        }
    }

    private fun computeDifference(region: BlockBox, lastRegion: BlockBox): List<BlockBox> {
        val result = ArrayList<BlockBox>(6)

        // Along +X
        if (region.maxX > lastRegion.maxX) {
            result += BlockBox(
                lastRegion.maxX + 1, region.minY, region.minZ,
                region.maxX, region.maxY, region.maxZ
            )
        }
        // Along -X
        if (region.minX < lastRegion.minX) {
            result += BlockBox(
                region.minX, region.minY, region.minZ,
                lastRegion.minX - 1, region.maxY, region.maxZ
            )
        }
        // Along +Y
        if (region.maxY > lastRegion.maxY) {
            result += BlockBox(
                region.minX, lastRegion.maxY + 1, region.minZ,
                region.maxX, region.maxY, region.maxZ
            )
        }
        // Along -Y
        if (region.minY < lastRegion.minY) {
            result += BlockBox(
                region.minX, region.minY, region.minZ,
                region.maxX, lastRegion.minY - 1, region.maxZ
            )
        }
        // Along +Z
        if (region.maxZ > lastRegion.maxZ) {
            result += BlockBox(
                region.minX, region.minY, lastRegion.maxZ + 1,
                region.maxX, region.maxY, region.maxZ
            )
        }
        // Along -Z
        if (region.minZ < lastRegion.minZ) {
            result += BlockBox(
                region.minX, region.minY, region.minZ,
                region.maxX, region.maxY, lastRegion.minZ - 1
            )
        }

        return result
    }

    fun clearRegion() {
        this.currentRegion = ORIGIN
>>>>>>> upstream/nextgen
    }

}
