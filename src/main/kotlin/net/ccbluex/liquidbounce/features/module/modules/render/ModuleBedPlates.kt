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
package net.ccbluex.liquidbounce.features.module.modules.render

<<<<<<< HEAD
import it.unimi.dsi.fastutil.doubles.DoubleObjectPair
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
=======
>>>>>>> upstream/nextgen
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.type.Color4b
<<<<<<< HEAD
import net.ccbluex.liquidbounce.render.newDrawContext
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.*
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.block.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*
=======
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.block.bed.BedBlockTracker
import net.ccbluex.liquidbounce.utils.block.bed.BedState
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.removeRange
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.util.math.Vec3d
>>>>>>> upstream/nextgen
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val ITEM_SIZE: Int = 16
private const val BACKGROUND_PADDING: Int = 2

<<<<<<< HEAD
object ModuleBedPlates : ClientModule("BedPlates", Category.RENDER) {
    private val ROMAN_NUMERALS = arrayOf("", "I", "II", "III", "IV", "V")

    private val maxLayers by int("MaxLayers", 5, 1..5)
    private val scale by float("Scale", 1.5f, 0.5f..3.0f)
    private val renderY by float("RenderY", 0.0F, -2.0F..2.0F)
=======
object ModuleBedPlates : ClientModule("BedPlates", Category.RENDER), BedBlockTracker.Subscriber {
    private val ROMAN_NUMERALS = arrayOf("", "I", "II", "III", "IV", "V")

    private val backgroundColor by color("BackgroundColor", Color4b(Int.MIN_VALUE, hasAlpha = true))

    override val maxLayers by int("MaxLayers", 5, 1..5).onChanged {
        BedBlockTracker.triggerRescan()
    }
    private val scale by float("Scale", 1.5f, 0.5f..3.0f)
    private val renderOffset by vec3d("RenderOffset", Vec3d.ZERO)
>>>>>>> upstream/nextgen
    private val maxDistance by float("MaxDistance", 256.0f, 128.0f..1280.0f)
    private val maxCount by int("MaxCount", 8, 1..64)
    private val highlightUnbreakable by boolean("HighlightUnbreakable", true)
    private val compact by boolean("Compact", true)

    private val fontRenderer
        get() = FontManager.FONT_RENDERER

<<<<<<< HEAD
    private val WHITELIST_NON_SOLID = setOf(
        Blocks.LADDER,

        Blocks.GLASS,
        Blocks.WHITE_STAINED_GLASS,
        Blocks.ORANGE_STAINED_GLASS,
        Blocks.MAGENTA_STAINED_GLASS,
        Blocks.LIGHT_BLUE_STAINED_GLASS,
        Blocks.YELLOW_STAINED_GLASS,
        Blocks.LIME_STAINED_GLASS,
        Blocks.PINK_STAINED_GLASS,
        Blocks.GRAY_STAINED_GLASS,
        Blocks.LIGHT_GRAY_STAINED_GLASS,
        Blocks.CYAN_STAINED_GLASS,
        Blocks.PURPLE_STAINED_GLASS,
        Blocks.BLUE_STAINED_GLASS,
        Blocks.BROWN_STAINED_GLASS,
        Blocks.GREEN_STAINED_GLASS,
        Blocks.RED_STAINED_GLASS,
        Blocks.BLACK_STAINED_GLASS,
    )

    private val bedStatesWithSquaredDistance by computedOn<GameTickEvent, MutableList<DoubleObjectPair<BedState>>>(
        initialValue = mutableListOf()
    ) { _, list ->
        val playerPos = player.blockPos
=======
    private class BedStateAndDistance(@JvmField val bedState: BedState, @JvmField val distanceSq: Double)

    private val bedStatesWithSquaredDistance by computedOn<GameTickEvent, MutableList<BedStateAndDistance>>(
        initialValue = mutableListOf()
    ) { _, list ->
        val cameraPos = (mc.cameraEntity ?: player).blockPos
>>>>>>> upstream/nextgen
        val maxDistanceSquared = maxDistance.sq()
        list.clear()

        BedBlockTracker.iterate().mapTo(list) { (pos, bedState) ->
<<<<<<< HEAD
            DoubleObjectPair.of(pos.getSquaredDistance(playerPos), bedState)
        }

        list.removeIf { it.firstDouble() > maxDistanceSquared } // filter items out of range
        list.sortBy { it.firstDouble() } // order by distance asc
=======
            BedStateAndDistance(bedState, pos.getSquaredDistance(cameraPos))
        }

        list.removeIf { it.distanceSq > maxDistanceSquared } // filter items out of range
        list.sortBy { it.distanceSq } // order by distance asc
>>>>>>> upstream/nextgen
        if (list.size > maxCount) {
            list.removeRange(fromInclusive = maxCount)
        }
        list
    }

    @Suppress("unused")
<<<<<<< HEAD
    private val renderHandler = handler<OverlayRenderEvent> {
        renderEnvironmentForGUI {
            fontRenderer.withBuffers { buf ->
                bedStatesWithSquaredDistance.forEachWithSelf { (distSq, bedState), i, self ->
                    val screenPos = WorldToScreen.calculateScreenPos(bedState.pos.add(0.0, renderY.toDouble(), 0.0))
                        ?: return@forEachWithSelf
                    val distance = sqrt(distSq)
                    val surrounding = bedState.surroundingBlocks

                    val z = 1000.0F * (self.size - i - 1) / self.size
=======
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        renderEnvironmentForGUI {
            fontRenderer.withBuffers { buf ->
                bedStatesWithSquaredDistance.forEach {
                    val bedState = it.bedState
                    val screenPos = WorldToScreen.calculateScreenPos(bedState.pos.add(renderOffset))
                        ?: return@forEach
                    val distance = sqrt(it.distanceSq)
                    val surrounding = if (compact) bedState.compactSurroundingBlocks else bedState.surroundingBlocks
>>>>>>> upstream/nextgen

                    // without padding
                    val rectWidth = ITEM_SIZE * (1 + surrounding.size)
                    val rectHeight = ITEM_SIZE

                    // draw items and background
<<<<<<< HEAD
                    with(newDrawContext()) {
                        with(matrices) {
                            translate(screenPos.x, screenPos.y, z)
=======
                    with(event.context) {
                        with(matrices) {
                            push()
                            translate(screenPos.x, screenPos.y, screenPos.z)
>>>>>>> upstream/nextgen
                            scale(scale, scale, 1.0F)
                            translate(-0.5F * rectWidth, -0.5F * rectHeight, -1F)

                            fill(
                                -BACKGROUND_PADDING,
                                -BACKGROUND_PADDING,
                                rectWidth + BACKGROUND_PADDING,
                                rectHeight + BACKGROUND_PADDING,
<<<<<<< HEAD
                                Color4b(0, 0, 0, 128).toARGB()
=======
                                backgroundColor.toARGB()
>>>>>>> upstream/nextgen
                            )

                            var itemX = 0
                            drawItem(bedState.block.asItem().defaultStack, itemX, 0)
<<<<<<< HEAD
                            surrounding.forEach {
                                itemX += ITEM_SIZE
                                drawItem(it.block.asItem().defaultStack, itemX, 0)
                            }
=======
                            surrounding.forEach { surrounding ->
                                itemX += ITEM_SIZE
                                drawItem(surrounding.block.asItem().defaultStack, itemX, 0)
                            }
                            pop()
>>>>>>> upstream/nextgen
                        }
                    }

                    // draw texts
                    withMatrixStack {
<<<<<<< HEAD
                        translate(screenPos.x, screenPos.y, z)
=======
                        translate(screenPos.x, screenPos.y, screenPos.z)
>>>>>>> upstream/nextgen
                        scale(scale, scale, 1.0F)
                        translate(-0.5F * rectWidth, -0.5F * rectHeight, 150F + 20F)

                        val fontScale = 1.0F / (size * 0.15F)
                        val heightScaled = fontScale * height

                        var topLeftX = 0
                        draw(
                            process("${distance.roundToInt()}m"),
                            0F,
                            rectHeight - heightScaled,
                            shadow = true,
                            scale = fontScale,
                        )
                        commit(buf)
<<<<<<< HEAD
                        surrounding.forEach {
                            topLeftX += ITEM_SIZE

                            val defaultState = it.block.defaultState
=======
                        surrounding.forEach { surrounding ->
                            topLeftX += ITEM_SIZE

                            val defaultState = surrounding.block.defaultState
>>>>>>> upstream/nextgen
                            val color =
                                if (highlightUnbreakable && defaultState.isToolRequired
                                    && Slots.Hotbar.findSlot { s -> s.isSuitableFor(defaultState) } == null
                                ) {
                                    Color4b.RED
                                } else {
                                    Color4b.WHITE
                                }

                            // count
<<<<<<< HEAD
                            val countText = process(it.count.toString(), color)
=======
                            val countText = process(surrounding.count.toString(), color)
>>>>>>> upstream/nextgen
                            draw(
                                countText,
                                topLeftX + ITEM_SIZE - countText.widthWithShadow * fontScale,
                                rectHeight - heightScaled,
                                shadow = true,
                                scale = fontScale,
                            )
                            commit(buf)

                            if (!compact) {
                                // layer
<<<<<<< HEAD
                                val layerText = process(ROMAN_NUMERALS[it.layer], color)
=======
                                val layerText = process(ROMAN_NUMERALS[surrounding.layer], color)
>>>>>>> upstream/nextgen
                                draw(
                                    layerText,
                                    topLeftX.toFloat(),
                                    0F,
                                    shadow = true,
                                    scale = fontScale,
                                )
                                commit(buf)
                            }
                        }
                    }
                }
            }
        }
    }

<<<<<<< HEAD
    @JvmRecord
    private data class SurroundingBlock(
        val block: Block,
        val count: Int,
        val layer: Int,
    ) : Comparable<SurroundingBlock> {
        override fun compareTo(other: SurroundingBlock): Int = compareValuesBy(
            this, other,
            { it.layer }, { -it.count }, { -it.block.hardness }, { it.block.translationKey })
    }

    @JvmRecord
    private data class BedState(val block: Block, val pos: Vec3d, val surroundingBlocks: Set<SurroundingBlock>)

    private fun BlockPos.getBedSurroundingBlocks(blockState: BlockState): Set<SurroundingBlock> {
        val layers = Array<Object2IntOpenHashMap<Block>>(maxLayers, ::Object2IntOpenHashMap)

        searchBedLayer(blockState, maxLayers)
            .forEach { (layer, pos) ->
                val state = pos.getState()

                // Ignore empty positions and fluid
                if (state == null || state.isAir) {
                    return@forEach
                }

                val block = state.block
                if (state.isSolidBlock(world, pos) || block in WHITELIST_NON_SOLID) {
                    // Count blocks (getInt default = 0)
                    with(layers[layer - 1]) {
                        put(block, getInt(block) + 1)
                    }
                }
            }

        val result = TreeSet<SurroundingBlock>()

        layers.forEachIndexed { i, map ->
            map.object2IntEntrySet().forEach { (block, count) ->
                result += SurroundingBlock(block, count, i + 1)
            }
        }

        return if (compact) {
            result.groupBy { surrounding ->
                surrounding.block
            }.map { (block, group) ->
                group.reduce { acc, item ->
                    SurroundingBlock(
                        block = block,
                        count = acc.count + item.count,
                        layer = minOf(acc.layer, item.layer)
                    )
                }
            }.toSet()
        } else {
            result
        }
    }

    private fun BlockPos.getBedPlates(headState: BlockState): BedState {
        val bedDirection = headState.get(BedBlock.FACING)

        val bedBlock = headState.block
        val renderPos = Vec3d(
            x - (bedDirection.offsetX * 0.5) + 0.5,
            y + 1.0,
            z - (bedDirection.offsetZ * 0.5) + 0.5,
        )

        return BedState(bedBlock, renderPos, getBedSurroundingBlocks(headState))
    }

    override fun enable() {
        ChunkScanner.subscribe(BedBlockTracker)
    }

    override fun disable() {
        ChunkScanner.unsubscribe(BedBlockTracker)
        bedStatesWithSquaredDistance.clear()
    }

    private object BedBlockTracker : AbstractBlockLocationTracker.BlockPos2State<BedState>() {
        @Suppress("detekt:CognitiveComplexMethod")
        override fun getStateFor(pos: BlockPos, state: BlockState): BedState? {
            return if (state.isBed) {
                val part = BedBlock.getBedPart(state)
                // Only track the first part (head) of the bed
                if (part == DoubleBlockProperties.Type.FIRST) {
                    pos.getBedPlates(state)
                } else {
                    null
                }
            } else {
                // A non-bed block was updated, we need to update the bed blocks around it
                val distance = maxLayers

                allPositions().forEach { bedPos ->
                    // Update if the block is close to a bed
                    if (bedPos.getManhattanDistance(pos) > distance) {
                        return@forEach
                    }

                    val bedState = bedPos.getState()
                    if (bedState == null || !bedState.isBed) {
                        // The tracked block is not a bed anymore, remove it
                        untrack(bedPos)
                    } else {
                        track(bedPos, bedPos.getBedPlates(bedState))
                    }
                }

                null
            }
        }
    }
=======
    override fun onEnabled() {
        BedBlockTracker.subscribe(this)
    }

    override fun onDisabled() {
        BedBlockTracker.unsubscribe(this)
        bedStatesWithSquaredDistance.clear()
    }
>>>>>>> upstream/nextgen
}
