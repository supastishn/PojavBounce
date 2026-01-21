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
    val targetArmorValue: Float
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
