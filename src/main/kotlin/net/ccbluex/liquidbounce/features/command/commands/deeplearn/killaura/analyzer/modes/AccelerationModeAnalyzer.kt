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

package net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.modes

import net.ccbluex.liquidbounce.deeplearn.data.CombatSample
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.AnalysisResult
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.AutoConfigApplier
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.KillAuraAnalyzer
import net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer.SettingChange
import net.ccbluex.liquidbounce.utils.client.chat
import kotlin.math.abs

object AccelerationModeAnalyzer : KillAuraAnalyzer {

    override fun analyze(samples: List<CombatSample>): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult("AccelerationMode", emptyMap(), emptyMap(), 0f)
        }

        val yawDeltas = samples.map { it.totalDelta.deltaYaw.toDouble() }
        val pitchDeltas = samples.map { it.totalDelta.deltaPitch.toDouble() }
        val distances = samples.map { it.distance.toDouble() }

        // Calculate accelerations (1st derivative)
        val yawAccelerations = mutableListOf<Double>()
        val pitchAccelerations = mutableListOf<Double>()

        for (i in 1 until yawDeltas.size) {
            yawAccelerations.add(yawDeltas[i] - yawDeltas[i - 1])
            pitchAccelerations.add(pitchDeltas[i] - pitchDeltas[i - 1])
        }

        val avgYawAccel = yawAccelerations.average()
        val avgPitchAccel = pitchAccelerations.average()
        val maxYawAccel = yawAccelerations.map { abs(it) }.maxOrNull() ?: 0.0
        val maxPitchAccel = pitchAccelerations.map { abs(it) }.maxOrNull() ?: 0.0

        // Jerk: 2nd derivative (rate of acceleration change)
        val jerks = mutableListOf<Double>()
        for (i in 1 until yawAccelerations.size) {
            val yawJerk = abs((yawAccelerations[i] - yawAccelerations[i - 1]))
            val pitchJerk = abs((pitchAccelerations[i] - pitchAccelerations[i - 1]))
            jerks.add((yawJerk + pitchJerk) / 2)
        }

        val avgJerk = if (jerks.isNotEmpty()) jerks.average() else 0.0

        // Recommend parameters based on acceleration patterns
        // AccelerationAngleSmooth uses floatRange for YawAcceleration/PitchAcceleration (1-180)

        // Use percentiles for acceleration ranges
        val sortedYawAccel = yawAccelerations.map { abs(it) }.sorted()
        val sortedPitchAccel = pitchAccelerations.map { abs(it) }.sorted()

        val yawP50 = if (sortedYawAccel.isNotEmpty())
            sortedYawAccel[(sortedYawAccel.size * 0.50).toInt().coerceIn(0, sortedYawAccel.size - 1)]
            else 20.0
        val yawP85 = if (sortedYawAccel.isNotEmpty())
            sortedYawAccel[(sortedYawAccel.size * 0.85).toInt().coerceIn(0, sortedYawAccel.size - 1)]
            else 25.0
        val pitchP50 = if (sortedPitchAccel.isNotEmpty())
            sortedPitchAccel[(sortedPitchAccel.size * 0.50).toInt().coerceIn(0, sortedPitchAccel.size - 1)]
            else 18.0
        val pitchP85 = if (sortedPitchAccel.isNotEmpty())
            sortedPitchAccel[(sortedPitchAccel.size * 0.85).toInt().coerceIn(0, sortedPitchAccel.size - 1)]
            else 23.0

        val recommendedYawAccelStart = yawP50.coerceIn(1.0, 180.0).toFloat()
        val recommendedYawAccelEnd = yawP85.coerceIn(recommendedYawAccelStart.toDouble() + 3.0, 180.0).toFloat()
        val recommendedYawAccel = recommendedYawAccelStart..recommendedYawAccelEnd

        val recommendedPitchAccelStart = pitchP50.coerceIn(1.0, 180.0).toFloat()
        val recommendedPitchAccelEnd = pitchP85.coerceIn(recommendedPitchAccelStart.toDouble() + 3.0, 180.0).toFloat()
        val recommendedPitchAccel = recommendedPitchAccelStart..recommendedPitchAccelEnd

        // Error settings: continuous mapping based on jerk
        // Map avgJerk to accel error: 0 jerk -> 0.08, 3+ jerk -> 0.18
        val jerkNormalized = (avgJerk / 3.0).coerceIn(0.0, 1.0)
        val recommendedAccelError = (0.08 + (jerkNormalized * 0.10)).coerceIn(0.05, 0.20).toFloat()

        // Map avgJerk to constant error: 0 jerk -> 0.06, 3+ jerk -> 0.14
        val recommendedConstantError = (0.06 + (jerkNormalized * 0.08)).coerceIn(0.05, 0.20).toFloat()

        // ===== DynamicAccel Analysis =====
        // Analyze distance-based patterns to determine if DynamicAccel should be enabled
        val avgDistance = distances.average()
        val distanceVariance = distances.map { (it - avgDistance) * (it - avgDistance) }.average()
        val distanceStdDev = kotlin.math.sqrt(distanceVariance)

        // Enable DynamicAccel if distance varies significantly (player moves around target)
        val enableDynamicAccel = distanceStdDev > 0.5 || avgDistance > 3.0

        // CoefDistance: negative = slower rotation when far, positive = faster when far
        // Based on correlation between distance and rotation delta magnitude
        val distanceRotationCorrelation = if (samples.size > 10) {
            val rotationMagnitudes = samples.map {
                kotlin.math.sqrt(
                    it.totalDelta.deltaYaw * it.totalDelta.deltaYaw +
                    it.totalDelta.deltaPitch * it.totalDelta.deltaPitch
                ).toDouble()
            }
            calculateCorrelation(distances, rotationMagnitudes)
        } else 0.0

        // Map correlation to CoefDistance range (-2 to 2)
        val recommendedCoefDistance = (distanceRotationCorrelation * -1.5).coerceIn(-2.0, 2.0).toFloat()

        // NOTE: YawCrosshairAccel and PitchCrosshairAccel are NOT configured by autoconfig
        // because CombatSample data does not include "crosshair on target" state information.
        // These settings require knowing when the player is already aiming at the target,
        // which is not captured in the training data.

        // ===== SigmoidDeceleration Analysis =====
        // Enable if there's high variance in rotation speeds (needs smooth deceleration)
        val rotationVariance = kotlin.math.sqrt(
            yawDeltas.map { (it - yawDeltas.average()) * (it - yawDeltas.average()) }.average() +
            pitchDeltas.map { (it - pitchDeltas.average()) * (it - pitchDeltas.average()) }.average()
        )

        val enableSigmoidDecel = rotationVariance > 8.0 || avgJerk > 1.5

        // Steepness: continuous mapping based on rotation variance (0-20 range)
        // Map rotationVariance to steepness: 0 variance -> 6, 20 variance -> 16
        val varianceNormalized = (rotationVariance / 20.0).coerceIn(0.0, 1.0)
        val recommendedSigmoidSteepness = (6.0 + (varianceNormalized * 10.0)).coerceIn(0.0, 20.0).toFloat()

        // Midpoint: continuous mapping based on avg rotation magnitude (0-1 range)
        // Map avgRotationMagnitude to midpoint: 0° -> 0.25, 40° -> 0.45
        val avgRotationMagnitude = kotlin.math.sqrt(
            yawDeltas.average() * yawDeltas.average() +
            pitchDeltas.average() * pitchDeltas.average()
        )
        val magnitudeNormalized = (avgRotationMagnitude / 40.0).coerceIn(0.0, 1.0)
        val recommendedSigmoidMidpoint = (0.25 + (magnitudeNormalized * 0.20)).coerceIn(0.0, 1.0).toFloat()

        val changes = mutableMapOf<String, SettingChange>()

        changes["yawAcceleration"] = SettingChange(
            "YawAcceleration",
            "Current",
            "${recommendedYawAccel.start}..${recommendedYawAccel.endInclusive}",
            "From P50-P85 yaw accel: ${"%.2f".format(yawP50)}-${"%.2f".format(yawP85)}°/tick²"
        )

        changes["pitchAcceleration"] = SettingChange(
            "PitchAcceleration",
            "Current",
            "${recommendedPitchAccel.start}..${recommendedPitchAccel.endInclusive}",
            "From P50-P85 pitch accel: ${"%.2f".format(pitchP50)}-${"%.2f".format(pitchP85)}°/tick²"
        )

        changes["accelerationError"] = SettingChange(
            "AccelerationError",
            "Current",
            "%.2f".format(recommendedAccelError),
            "Avg jerk: ${"%.3f".format(avgJerk)} → continuous error"
        )

        changes["constantError"] = SettingChange(
            "ConstantError",
            "Current",
            "%.2f".format(recommendedConstantError),
            "Jerk-based continuous error compensation"
        )

        changes["dynamicAccel"] = SettingChange(
            "DynamicAccel",
            "Current",
            enableDynamicAccel,
            "Distance stdDev: ${"%.2f".format(distanceStdDev)}"
        )

        changes["coefDistance"] = SettingChange(
            "CoefDistance",
            "Current",
            "%.3f".format(recommendedCoefDistance),
            "Distance-rotation correlation: ${"%.2f".format(distanceRotationCorrelation)}"
        )

        changes["sigmoidDeceleration"] = SettingChange(
            "SigmoidDeceleration",
            "Current",
            enableSigmoidDecel,
            "Rotation variance: ${"%.2f".format(rotationVariance)}"
        )

        changes["sigmoidSteepness"] = SettingChange(
            "Steepness",
            "Current",
            "%.1f".format(recommendedSigmoidSteepness),
            "Based on rotation variance"
        )

        changes["sigmoidMidpoint"] = SettingChange(
            "Midpoint",
            "Current",
            "%.2f".format(recommendedSigmoidMidpoint),
            "Based on avg rotation magnitude"
        )

        val stats = mapOf(
            "avgYawAccel" to avgYawAccel,
            "avgPitchAccel" to avgPitchAccel,
            "maxYawAccel" to maxYawAccel,
            "maxPitchAccel" to maxPitchAccel,
            "avgJerk" to avgJerk,
            "yawP50" to yawP50,
            "yawP85" to yawP85,
            "pitchP50" to pitchP50,
            "pitchP85" to pitchP85,
            "yawAccelStart" to recommendedYawAccel.start.toDouble(),
            "yawAccelEnd" to recommendedYawAccel.endInclusive.toDouble(),
            "pitchAccelStart" to recommendedPitchAccel.start.toDouble(),
            "pitchAccelEnd" to recommendedPitchAccel.endInclusive.toDouble(),
            "recommendedAccelError" to recommendedAccelError.toDouble(),
            "recommendedConstantError" to recommendedConstantError.toDouble(),
            // DynamicAccel stats
            "enableDynamicAccel" to if (enableDynamicAccel) 1.0 else 0.0,
            "coefDistance" to recommendedCoefDistance.toDouble(),
            // NOTE: YawCrosshairAccel/PitchCrosshairAccel not included - no crosshair-on-target data
            // SigmoidDeceleration stats
            "enableSigmoidDecel" to if (enableSigmoidDecel) 1.0 else 0.0,
            "sigmoidSteepness" to recommendedSigmoidSteepness.toDouble(),
            "sigmoidMidpoint" to recommendedSigmoidMidpoint.toDouble(),
            "rotationVariance" to rotationVariance,
            "distanceStdDev" to distanceStdDev,
            "avgRotationMagnitude" to avgRotationMagnitude
        )

        return AnalysisResult("AccelerationMode", changes, stats, 0.90f)
    }

    /**
     * Calculate Pearson correlation coefficient between two lists
     */
    private fun calculateCorrelation(x: List<Double>, y: List<Double>): Double {
        if (x.size != y.size || x.size < 2) return 0.0

        val n = x.size
        val avgX = x.average()
        val avgY = y.average()

        var numerator = 0.0
        var denomX = 0.0
        var denomY = 0.0

        for (i in 0 until n) {
            val dx = x[i] - avgX
            val dy = y[i] - avgY
            numerator += dx * dy
            denomX += dx * dx
            denomY += dy * dy
        }

        val denominator = kotlin.math.sqrt(denomX * denomY)
        return if (denominator == 0.0) 0.0 else numerator / denominator
    }

    override fun apply(result: AnalysisResult) {
        if (result.stats.isEmpty()) return

        // Set rotation mode to Acceleration
        if (!AutoConfigApplier.setRotationMode("Acceleration")) {
            chat("§c✗ Failed to set Acceleration mode")
            return
        }

        // Apply yaw acceleration
        val yawStart = result.stats["yawAccelStart"]?.toFloat() ?: 20f
        val yawEnd = result.stats["yawAccelEnd"]?.toFloat() ?: 25f
        if (AutoConfigApplier.setFloatRangeValue("YawAcceleration", yawStart..yawEnd)) {
            chat("§a  ✓ YawAcceleration: ${"%.1f".format(yawStart)}..${"%.1f".format(yawEnd)}")
        }

        // Apply pitch acceleration
        val pitchStart = result.stats["pitchAccelStart"]?.toFloat() ?: 20f
        val pitchEnd = result.stats["pitchAccelEnd"]?.toFloat() ?: 25f
        if (AutoConfigApplier.setFloatRangeValue("PitchAcceleration", pitchStart..pitchEnd)) {
            chat("§a  ✓ PitchAcceleration: ${"%.1f".format(pitchStart)}..${"%.1f".format(pitchEnd)}")
        }

        // Enable and configure AccelerationError
        AutoConfigApplier.setToggleableEnabled("AccelerationError", true)
        val accelError = result.stats["recommendedAccelError"]?.toFloat() ?: 0.1f
        if (AutoConfigApplier.setNestedFloatValue("AccelerationError", "YawAccelError", accelError)) {
            chat("§a  ✓ YawAccelError: ${"%.2f".format(accelError)}")
        }
        if (AutoConfigApplier.setNestedFloatValue("AccelerationError", "PitchAccelError", accelError)) {
            chat("§a  ✓ PitchAccelError: ${"%.2f".format(accelError)}")
        }

        // Enable and configure ConstantError
        AutoConfigApplier.setToggleableEnabled("ConstantError", true)
        val constantError = result.stats["recommendedConstantError"]?.toFloat() ?: 0.1f
        if (AutoConfigApplier.setNestedFloatValue("ConstantError", "YawConstantError", constantError)) {
            chat("§a  ✓ YawConstantError: ${"%.2f".format(constantError)}")
        }
        if (AutoConfigApplier.setNestedFloatValue("ConstantError", "PitchConstantError", constantError)) {
            chat("§a  ✓ PitchConstantError: ${"%.2f".format(constantError)}")
        }

        // ===== DynamicAccel =====
        val enableDynamicAccel = result.stats["enableDynamicAccel"]?.toInt() == 1
        AutoConfigApplier.setToggleableEnabled("DynamicAccel", enableDynamicAccel)
        if (enableDynamicAccel) {
            chat("§a  ✓ DynamicAccel: enabled")

            val coefDistance = result.stats["coefDistance"]?.toFloat() ?: -1.393f
            if (AutoConfigApplier.setNestedFloatValue("DynamicAccel", "CoefDistance", coefDistance)) {
                chat("§a    ✓ CoefDistance: ${"%.3f".format(coefDistance)}")
            }

            // NOTE: YawCrosshairAccel and PitchCrosshairAccel are NOT modified
            // because CombatSample data lacks "crosshair on target" state.
            // User must configure these manually if needed.
            chat("§7    ○ YawCrosshairAccel: not modified (no crosshair data)")
            chat("§7    ○ PitchCrosshairAccel: not modified (no crosshair data)")
        } else {
            chat("§7  ○ DynamicAccel: disabled (low distance variance)")
        }

        // ===== SigmoidDeceleration =====
        val enableSigmoidDecel = result.stats["enableSigmoidDecel"]?.toInt() == 1
        AutoConfigApplier.setToggleableEnabled("SigmoidDeceleration", enableSigmoidDecel)
        if (enableSigmoidDecel) {
            chat("§a  ✓ SigmoidDeceleration: enabled")

            val steepness = result.stats["sigmoidSteepness"]?.toFloat() ?: 10f
            if (AutoConfigApplier.setNestedFloatValue("SigmoidDeceleration", "Steepness", steepness)) {
                chat("§a    ✓ Steepness: ${"%.1f".format(steepness)}")
            }

            val midpoint = result.stats["sigmoidMidpoint"]?.toFloat() ?: 0.3f
            if (AutoConfigApplier.setNestedFloatValue("SigmoidDeceleration", "Midpoint", midpoint)) {
                chat("§a    ✓ Midpoint: ${"%.2f".format(midpoint)}")
            }
        } else {
            chat("§7  ○ SigmoidDeceleration: disabled (low rotation variance)")
        }
    }

    override fun report(result: AnalysisResult): String {
        if (result.changes.isEmpty()) {
            return "✗ Acceleration Mode: Insufficient data"
        }

        val yawAccel = result.changes["yawAcceleration"]
        val pitchAccel = result.changes["pitchAcceleration"]
        val accelErr = result.changes["accelerationError"]
        val constErr = result.changes["constantError"]
        val dynamicAccel = result.changes["dynamicAccel"]
        val coefDist = result.changes["coefDistance"]
        val sigmoidDecel = result.changes["sigmoidDeceleration"]
        val sigmoidSteep = result.changes["sigmoidSteepness"]
        val sigmoidMid = result.changes["sigmoidMidpoint"]
        val maxYaw = result.stats["maxYawAccel"]?.let { "%.2f".format(it) } ?: "?"
        val maxPitch = result.stats["maxPitchAccel"]?.let { "%.2f".format(it) } ?: "?"
        val avgJ = result.stats["avgJerk"]?.let { "%.3f".format(it) } ?: "?"
        val rotVar = result.stats["rotationVariance"]?.let { "%.2f".format(it) } ?: "?"
        val distStd = result.stats["distanceStdDev"]?.let { "%.2f".format(it) } ?: "?"

        return buildString {
            append("§1╔ Acceleration Mode Configuration\n")
            if (yawAccel != null) {
                append("§1║ YawAcceleration: §7${yawAccel.newValue}\n")
            }
            if (pitchAccel != null) {
                append("§1║ PitchAcceleration: §7${pitchAccel.newValue}\n")
            }
            if (accelErr != null) {
                append("§1║ AccelerationError: §7${accelErr.newValue}\n")
            }
            if (constErr != null) {
                append("§1║ ConstantError: §7${constErr.newValue}\n")
            }
            append("§1╠═══ DynamicAccel ═══\n")
            if (dynamicAccel != null) {
                append("§1║ Enabled: §7${dynamicAccel.newValue}\n")
            }
            if (coefDist != null) {
                append("§1║ CoefDistance: §7${coefDist.newValue}\n")
            }
            append("§1║ §8YawCrosshairAccel: §7(not modified - no data)\n")
            append("§1║ §8PitchCrosshairAccel: §7(not modified - no data)\n")
            append("§1╠═══ SigmoidDeceleration ═══\n")
            if (sigmoidDecel != null) {
                append("§1║ Enabled: §7${sigmoidDecel.newValue}\n")
            }
            if (sigmoidSteep != null) {
                append("§1║ Steepness: §7${sigmoidSteep.newValue}\n")
            }
            if (sigmoidMid != null) {
                append("§1║ Midpoint: §7${sigmoidMid.newValue}\n")
            }
            append("§1╠═══ Stats ═══\n")
            append("§1║ Max Yaw Accel: §7${maxYaw}°/tick² | Max Pitch Accel: §7${maxPitch}°/tick²\n")
            append("§1║ Avg Jerk: §7${avgJ} | Rotation Var: §7${rotVar}°\n")
            append("§1║ Distance StdDev: §7${distStd}\n")
            append("§1╚ Acceleration-based smoothing - handles high variance")
        }
    }
}
