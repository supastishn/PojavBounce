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

package net.ccbluex.liquidbounce.features.command.commands.deeplearn.killaura.analyzer

import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * Helper object for applying autoconfig settings to KillAura module
 */
object AutoConfigApplier {

    /**
     * Sets the rotation mode (AngleSmooth) for KillAura
     * @param modeName The name of the mode to set (Linear, Sigmoid, Interpolation, Acceleration)
     * @return true if successful, false otherwise
     */
    fun setRotationMode(modeName: String): Boolean {
        return try {
            ModuleKillAura.rotations.angleSmooth.setByString(modeName)
            chat("§a✓ Set rotation mode to: $modeName")
            true
        } catch (e: Exception) {
            logger.error("Failed to set rotation mode to $modeName", e)
            chat("§c✗ Failed to set rotation mode: ${e.message}")
            false
        }
    }

    /**
     * Gets the active rotation mode choice
     */
    fun getActiveRotationMode() = ModuleKillAura.rotations.angleSmooth.activeChoice

    /**
     * Finds a value in a configurable by name (case insensitive)
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> findValue(configurable: Configurable, name: String): Value<T>? {
        return configurable.containedValues.find {
            it.name.equals(name, ignoreCase = true)
        } as? Value<T>
    }

    /**
     * Finds a nested configurable by name
     */
    fun findConfigurable(parent: Configurable, name: String): Configurable? {
        return parent.containedValues.filterIsInstance<Configurable>().find {
            it.name.equals(name, ignoreCase = true)
        }
    }

    /**
     * Sets a float value in the active rotation mode
     */
    fun setFloatValue(name: String, value: Float): Boolean {
        val activeChoice = getActiveRotationMode()
        val floatValue = findValue<Float>(activeChoice, name)

        return if (floatValue != null) {
            try {
                floatValue.set(value)
                true
            } catch (e: Exception) {
                logger.error("Failed to set $name to $value", e)
                false
            }
        } else {
            logger.warn("Float value $name not found in ${activeChoice.name}")
            false
        }
    }

    /**
     * Sets a float range value in the active rotation mode
     */
    fun setFloatRangeValue(name: String, range: ClosedFloatingPointRange<Float>): Boolean {
        val activeChoice = getActiveRotationMode()
        val rangeValue = findValue<ClosedFloatingPointRange<Float>>(activeChoice, name)

        return if (rangeValue != null) {
            try {
                rangeValue.set(range)
                true
            } catch (e: Exception) {
                logger.error("Failed to set $name to $range", e)
                false
            }
        } else {
            logger.warn("Float range value $name not found in ${activeChoice.name}")
            false
        }
    }

    /**
     * Sets an int range value in the active rotation mode
     */
    fun setIntRangeValue(name: String, range: IntRange): Boolean {
        val activeChoice = getActiveRotationMode()
        val rangeValue = findValue<IntRange>(activeChoice, name)

        return if (rangeValue != null) {
            try {
                rangeValue.set(range)
                true
            } catch (e: Exception) {
                logger.error("Failed to set $name to $range", e)
                false
            }
        } else {
            logger.warn("Int range value $name not found in ${activeChoice.name}")
            false
        }
    }

    /**
     * Enables or disables a toggleable configurable within the active rotation mode
     */
    fun setToggleableEnabled(name: String, enabled: Boolean): Boolean {
        val activeChoice = getActiveRotationMode()
        val toggleable = activeChoice.containedValues
            .filterIsInstance<ToggleableConfigurable>()
            .find { it.name.equals(name, ignoreCase = true) }

        return if (toggleable != null) {
            try {
                toggleable.enabled = enabled
                true
            } catch (e: Exception) {
                logger.error("Failed to set $name enabled to $enabled", e)
                false
            }
        } else {
            logger.warn("Toggleable $name not found in ${activeChoice.name}")
            false
        }
    }

    /**
     * Sets a float value within a nested toggleable configurable
     */
    fun setNestedFloatValue(toggleableName: String, valueName: String, value: Float): Boolean {
        val activeChoice = getActiveRotationMode()
        val toggleable = activeChoice.containedValues
            .filterIsInstance<ToggleableConfigurable>()
            .find { it.name.equals(toggleableName, ignoreCase = true) }

        if (toggleable == null) {
            logger.warn("Toggleable $toggleableName not found in ${activeChoice.name}")
            return false
        }

        val floatValue = findValue<Float>(toggleable, valueName)
        return if (floatValue != null) {
            try {
                floatValue.set(value)
                true
            } catch (e: Exception) {
                logger.error("Failed to set $toggleableName.$valueName to $value", e)
                false
            }
        } else {
            logger.warn("Float value $valueName not found in $toggleableName")
            false
        }
    }

    /**
     * Sets a float range value within a nested toggleable configurable
     */
    fun setNestedFloatRangeValue(
        toggleableName: String,
        valueName: String,
        range: ClosedFloatingPointRange<Float>
    ): Boolean {
        val activeChoice = getActiveRotationMode()
        val toggleable = activeChoice.containedValues
            .filterIsInstance<ToggleableConfigurable>()
            .find { it.name.equals(toggleableName, ignoreCase = true) }

        if (toggleable == null) {
            logger.warn("Toggleable $toggleableName not found in ${activeChoice.name}")
            return false
        }

        val rangeValue = findValue<ClosedFloatingPointRange<Float>>(toggleable, valueName)
        return if (rangeValue != null) {
            try {
                rangeValue.set(range)
                true
            } catch (e: Exception) {
                logger.error("Failed to set $toggleableName.$valueName to $range", e)
                false
            }
        } else {
            logger.warn("Float range value $valueName not found in $toggleableName")
            false
        }
    }

    /**
     * Logs all available settings in the active rotation mode (for debugging)
     */
    fun logAvailableSettings() {
        val activeChoice = getActiveRotationMode()
        chat("§7Available settings in ${activeChoice.name}:")

        activeChoice.containedValues.forEach { value ->
            when (value) {
                is ToggleableConfigurable -> {
                    chat("§7  [Toggle] ${value.name} (enabled: ${value.enabled})")
                    value.containedValues.forEach { nested ->
                        chat("§8    - ${nested.name}: ${nested.get()}")
                    }
                }
                is Configurable -> {
                    chat("§7  [Config] ${value.name}")
                    value.containedValues.forEach { nested ->
                        chat("§8    - ${nested.name}: ${nested.get()}")
                    }
                }
                else -> {
                    chat("§7  ${value.name}: ${value.get()}")
                }
            }
        }
    }
}
