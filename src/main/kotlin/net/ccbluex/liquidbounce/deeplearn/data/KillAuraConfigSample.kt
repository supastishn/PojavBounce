/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.deeplearn.data

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.config.gson.publicGson
import java.io.File

/**
 * Comprehensive combat sample that includes all data needed for full KillAura autoconfig
 */
data class KillAuraConfigSample(
    // Base rotation data
    @SerializedName("combat")
    val combatData: CombatSample,

    // Click timing data
    @SerializedName("click_ts")
    val clickTimestamp: Long,
    @SerializedName("time_since_last_click")
    val timeSinceLastClick: Long,
    @SerializedName("cps")
    val currentCPS: Float,

    // Range/Raycast data
    @SerializedName("wall_between")
    val hasWallBetween: Boolean,
    @SerializedName("raycast_hit")
    val raycastHit: Boolean,
    @SerializedName("actual_range")
    val actualRange: Float,

    // AutoBlock data
    @SerializedName("blocking")
    val wasBlocking: Boolean,
    @SerializedName("block_duration")
    val blockDuration: Long,

    // Hit success tracking
    @SerializedName("attack_attempted")
    val attackAttempted: Boolean,
    @SerializedName("attack_succeeded")
    val attackSucceeded: Boolean,

    // Target data
    @SerializedName("available_targets")
    val availableTargets: Int,
    @SerializedName("target_health")
    val targetHealth: Float,
    @SerializedName("target_armor")
    val targetArmorValue: Float,

    // === NEW FIELDS FOR COMPREHENSIVE CONFIG ===

    // Criticals data
    @SerializedName("falling")
    val wasFalling: Boolean = false,
    @SerializedName("fall_distance")
    val fallDistance: Float = 0f,
    @SerializedName("on_ground")
    val onGround: Boolean = true,
    @SerializedName("was_crit")
    val wasCriticalHit: Boolean = false,

    // Sprint data
    @SerializedName("sprinting")
    val wasSprinting: Boolean = false,
    @SerializedName("sprint_after_hit")
    val sprintingAfterHit: Boolean = false,

    // Scan/Target selection data
    @SerializedName("scan_range")
    val scanRange: Float = 0f,
    @SerializedName("closest_target_dist")
    val closestTargetDistance: Float = 0f,
    @SerializedName("target_in_fov")
    val targetInFOV: Boolean = true,

    // Miss/FailSwing data
    @SerializedName("swing_at_air")
    val swingAtAir: Boolean = false,
    @SerializedName("miss_distance")
    val missDistance: Float = 0f
) {
    companion object {
        /**
         * Parse KillAuraConfigSample files from the debug-recorder/KillAuraConfig folder
         */
        fun parse(folder: File): List<KillAuraConfigSample> {
            if (!folder.exists()) return emptyList()

            return folder.listFiles()
                ?.filter { it.extension == "json" }
                ?.flatMap { file ->
                    try {
                        file.bufferedReader().use { reader ->
                            publicGson.fromJson(
                                reader,
                                Array<KillAuraConfigSample>::class.java
                            ).toList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
        }
    }
}
