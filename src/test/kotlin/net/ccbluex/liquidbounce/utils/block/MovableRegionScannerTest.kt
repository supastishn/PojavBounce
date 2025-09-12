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

import net.minecraft.util.math.BlockPos
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MovableRegionScannerTest {

    private lateinit var scanner: MovableRegionScanner

    @BeforeEach
    fun setup() {
        scanner = MovableRegionScanner()
    }

    @Test
    fun `same region returns empty`() {
        val region = Region(BlockPos(0, 0, 0), BlockPos(2, 2, 2))

        scanner.moveTo(region) // set first region
        val result = scanner.moveTo(region)

        assertTrue(result.none(), "Moving to same region should yield no new boxes")
    }

    @Test
    fun `new region inside old returns empty`() {
        val oldRegion = Region(BlockPos(0, 0, 0), BlockPos(4, 4, 4))
        val innerRegion = Region(BlockPos(1, 1, 1), BlockPos(3, 3, 3))

        scanner.moveTo(oldRegion)
        val result = scanner.moveTo(innerRegion)

        assertTrue(result.none(), "New region inside old region should yield no new boxes")
    }

    @Test
    fun `non overlapping returns whole new region`() {
        val region1 = Region(BlockPos(0, 0, 0), BlockPos(2, 2, 2))
        val region2 = Region(BlockPos(10, 10, 10), BlockPos(12, 12, 12))

        scanner.moveTo(region1)
        val result = scanner.moveTo(region2)

        assertEquals(1, result.count())
        assertEquals(region2, result.first())
    }

    @Test
    fun `expansion on positive X`() {
        val region1 = Region(BlockPos(0, 0, 0), BlockPos(2, 2, 2))
        val region2 = Region(BlockPos(0, 0, 0), BlockPos(4, 2, 2))

        scanner.moveTo(region1)
        val result = scanner.moveTo(region2)

        assertEquals(1, result.count())
        val newBox = result.first()
        assertEquals(Region(BlockPos(3, 0, 0), BlockPos(4, 2, 2)), newBox)
    }

    @Test
    fun `expansion on negative Z`() {
        val region1 = Region(BlockPos(0, 0, 0), BlockPos(2, 2, 2))
        val region2 = Region(BlockPos(0, 0, -2), BlockPos(2, 2, 2))

        scanner.moveTo(region1)
        val result = scanner.moveTo(region2)

        assertEquals(1, result.count())
        val newBox = result.first()
        assertEquals(Region(BlockPos(0, 0, -2), BlockPos(2, 2, -1)), newBox)
    }

    @Test
    fun `expansion in multiple directions`() {
        val region1 = Region(BlockPos(0, 0, 0), BlockPos(2, 2, 2))
        val region2 = Region(BlockPos(-1, -1, -1), BlockPos(3, 3, 3))

        scanner.moveTo(region1)
        val result = scanner.moveTo(region2)

        // Expect expansion in -X, +X, -Y, +Y, -Z, +Z
        assertEquals(6, result.count())
    }
}
