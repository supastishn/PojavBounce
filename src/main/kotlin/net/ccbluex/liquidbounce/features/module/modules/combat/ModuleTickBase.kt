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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.util.math.Vec3d

/**
 * TickBase
 *
 * Calls tick function to speed up, when needed
 */
internal object ModuleTickBase : ClientModule("TickBase", Category.COMBAT) {

    private val mode by enumChoice("Mode", TickBaseMode.PAST)
        .apply { tagBy(this) }
    private val call by enumChoice("Call", TickBaseCall.GAME)

    // Range to tick-base into
    private val inRange by floatRange("In-Range", 2.5f..3f, 0f..5f)
    // Maximum ticks we can use for tick-base
    private val movingTicks by int("Moving", 4, 1..20, "ticks")
    // Ticks we throw away after the tick-base (This should usually be 0)
    private val additionalTicks by int("Additional", 0, 0..20, "ticks")

    // Ticks we want to wait until we can tick-base again
    private val cooldown by intRange("Cooldown", 0..0, 0..100, "ticks")

    // Requirements for a tick to be valid
    private val requires by multiEnumChoice("Requires", TickBaseRequirements.KILLAURA)

    // The walk line color
    private val lineColor by color("Line", Color4b.WHITE)
        .doNotIncludeAlways()

    private var ticksToSkip = 0
    private val tickBuffer = mutableListOf<TickData>()

    override fun disable() {
        tickBuffer.clear()
    }

    @Suppress("unused")
    private val playerTickHandler = handler<PlayerTickEvent> { event ->
        // We do not want this module to conflict with blink
        if (player.vehicle != null || ModuleBlink.running) {
            return@handler
        }

        if (ticksToSkip-- > 0) {
            event.cancelEvent()
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        // We do not want this module to conflict with blink
        if (player.vehicle != null || ModuleBlink.running || tickBuffer.isEmpty()) {
            return@tickHandler
        }

        val target = ModuleKillAura.targetTracker.target.takeIf { ModuleKillAura.running }
            ?: world.findEnemy(0f..10f)
            ?: return@tickHandler
        this@ModuleTickBase.debugParameter("Target") { target.nameForScoreboard }

        var distanceSq = player.pos.squaredDistanceTo(target.pos)
        val rangeSq = inRange.start.sq()..inRange.endInclusive.sq()

        var ticks = tickBuffer
            .withIndex()
            .filter { (index, tick) ->
                val distSq = tick.position.squaredDistanceTo(target.pos)

                if (distSq < distanceSq && distSq in rangeSq && requires.any { it.meets(index) }) {
                    distanceSq = distSq
                    true
                } else {
                    false
                }
            }
        this@ModuleTickBase.debugGeometry("Ticks") {
            ModuleDebug.DebugCollection(ticks.map { (_, tick) ->
                ModuleDebug.DebuggedPoint(tick.position, Color4b.BLUE, 0.05)
            })
        }

        // We want to prefer ticks that let us do a critical hit
        val criticalTick = ticks.firstOrNull { (_, tick) ->
            tick.fallDistance > 0.0f
        }

        val (tick, _) = criticalTick ?: ticks.firstOrNull() ?: return@tickHandler
        this@ModuleTickBase.debugParameter("Tick") { tick }
        this@ModuleTickBase.debugParameter("Critical Tick") { criticalTick?.index ?: -1 }

        if (tick == 0) {
            return@tickHandler
        }

        when (mode) {
            TickBaseMode.PAST -> {
                ticksToSkip = tick + additionalTicks
                waitTicks(ticksToSkip)

                repeat(tick) {
                    call.tick()
                }

                ModuleDebug.debugParameter(this, "Recommended Skip", tick)
                ticksToSkip = 0
            }

            TickBaseMode.FUTURE -> {
                var totalSkipped = 0

                for (i in 0 until tick) {
                    call.tick()
                    totalSkipped++

                    if (requires.none { it.meets(0) }) {
                        break
                    }
                }

                ModuleDebug.debugParameter(this, "Total Skipped", totalSkipped)
                ModuleDebug.debugParameter(this, "Recommended Skip", tick)

                ticksToSkip = totalSkipped + additionalTicks
                waitTicks(ticksToSkip)
                ticksToSkip = 0
            }
        }

        waitTicks(cooldown.random())
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent> { event ->
        // We do not want this module to conflict with blink
        if (player.vehicle != null || ModuleBlink.running) {
            return@handler
        }

        tickBuffer.clear()

        val simulatedPlayer = PlayerSimulationCache.getSimulationForLocalPlayer()
        val snapshots = simulatedPlayer.getSnapshotsBetween(0 until movingTicks)

        snapshots.mapTo(tickBuffer) { snapshot ->
            TickData(
                snapshot.pos,
                snapshot.fallDistance,
                snapshot.velocity,
                snapshot.onGround
            )
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (lineColor.a <= 0) {
            return@handler
        }

        renderEnvironmentForWorld(event.matrixStack) {
            withColor(lineColor) {
                drawLineStrip(positions = tickBuffer.mapArray { tick ->
                    relativeToCamera(tick.position).toVec3()
                })
            }
        }
    }

    @JvmRecord
    private data class TickData(
        val position: Vec3d,
        val fallDistance: Float,
        val velocity: Vec3d,
        val onGround: Boolean
    )

    private enum class TickBaseMode(override val choiceName: String) : NamedChoice {
        PAST("Past"),
        FUTURE("Future")
    }

    @Suppress("unused")
    private enum class TickBaseCall(
        override val choiceName: String,
        val tick: () -> Unit
    ) : NamedChoice {

        /**
         * Runs a full game tick.
         *
         * TODO: Cancel full game ticks after this,
         *   not just the player ticks.
         */
        GAME("Game", { mc.tick() }),

        /**
         * This will NOT update the game tick,
         * but only the player tick - that means
         * e.g. Rotation Manager will not update either.
         *
         * This was the previous default behavior of the TickBase,
         * so it is kept for compatibility reasons.
         */
        PLAYER("Player", { player.tick() })
    }

    @Suppress("unused")
    enum class TickBaseRequirements(
        override val choiceName: String,
        val meets: (Int) -> Boolean
    ) : NamedChoice {
        /**
         * This will check if the tick can be used for Kill Aura.
         */
        KILLAURA("KillAura", { tick ->
            ModuleKillAura.running && ModuleKillAura.targetTracker.target != null &&
                ModuleKillAura.clicker.willClickAt(tick)
        }),
        /**
         * This will check if the tick can be used for Auto Clicker.
         */
        AUTO_CLICKER("AutoClicker", {
            ModuleAutoClicker.running && ModuleAutoClicker.AttackButton.running &&
                ModuleAutoClicker.AttackButton.clicker.willClickAt(it)
        });
    }

}
