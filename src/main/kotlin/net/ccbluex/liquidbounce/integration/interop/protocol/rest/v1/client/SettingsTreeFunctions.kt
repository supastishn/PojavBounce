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
 *
 */
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import net.ccbluex.liquidbounce.config.types.*
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.ModuleManager.modulesConfigurable
import net.ccbluex.liquidbounce.integration.interop.*
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * REST endpoints for the new JSON settings tree API
 */

// GET /api/v1/client/modules/settingsTree
fun getModuleSettingsTree(requestObject: RequestObject): FullHttpResponse {
    val moduleName = requestObject.queryParams["name"] ?: return httpBadRequest("Module name required")
    val module = ModuleManager[moduleName] ?: return httpNotFound("Module not found")
    
    return try {
        val settingsTree = SettingsTreeSerializer.serializeModule(module)
        httpOk(interopGson.toJsonTree(settingsTree))
    } catch (e: Exception) {
        logger.error("Failed to serialize settings tree for module $moduleName", e)
        httpInternalServerError("Failed to serialize settings tree")
    }
}

// GET /api/v1/client/modules/settings/field
fun getModuleSettingsField(requestObject: RequestObject): FullHttpResponse {
    val fieldPath = requestObject.queryParams["field"] ?: return httpBadRequest("Field path required")
    val moduleName = requestObject.queryParams["module"] ?: return httpBadRequest("Module name required")
    
    val module = ModuleManager[moduleName] ?: return httpNotFound("Module not found")
    
    return try {
        val value = findValueByPath(module, fieldPath)
            ?: return httpNotFound("Field not found at path: $fieldPath")
        
        val response = JsonObject().apply {
            add("fieldId", JsonPrimitive(fieldPath))
            add("currentValue", interopGson.toJsonTree(value.get()))
            add("defaultValue", null) // TODO: Access defaultValue when public API available
            add("fieldType", JsonPrimitive(mapValueTypeToString(value.valueType)))
        }
        
        httpOk(response)
    } catch (e: Exception) {
        logger.error("Failed to get field $fieldPath for module $moduleName", e)
        httpInternalServerError("Failed to get field value")
    }
}

// PUT /api/v1/client/modules/settings/field
fun setModuleSettingsField(requestObject: RequestObject): FullHttpResponse {
    return requestObject.asJson<FieldUpdateRequest>().acceptFieldUpdate()
}

// POST /api/v1/client/modules/settings/field
fun updateModuleSettingsField(requestObject: RequestObject): FullHttpResponse {
    return requestObject.asJson<FieldUpdateRequest>().acceptFieldUpdate()
}

/**
 * Request data for field updates
 */
data class FieldUpdateRequest(
    val module: String,
    val fieldPath: String,
    val value: Any
) {
    fun acceptFieldUpdate(): FullHttpResponse {
        val module = ModuleManager[module] ?: return httpNotFound("Module not found")
        
        return try {
            val field = findValueByPath(module, fieldPath)
                ?: return httpNotFound("Field not found at path: $fieldPath")
            
            RenderSystem.recordRenderCall {
                runCatching {
                    updateFieldValue(field, value)
                    ConfigSystem.storeConfigurable(modulesConfigurable)
                }.onFailure {
                    logger.error("Failed to update field $fieldPath for module ${this.module}", it)
                }
            }
            
            httpOk(emptyJsonObject())
        } catch (e: Exception) {
            logger.error("Failed to update field $fieldPath for module $module", e)
            httpInternalServerError("Failed to update field value")
        }
    }
}

/**
 * Find a value by path within a configurable
 */
private fun findValueByPath(configurable: Configurable, path: String): Value<*>? {
    val pathParts = path.split(".").drop(1) // Remove module name
    return findValueByPathParts(configurable, pathParts)
}

/**
 * Recursively find a value by path parts
 */
private fun findValueByPathParts(configurable: Configurable, pathParts: List<String>): Value<*>? {
    if (pathParts.isEmpty()) return null
    
    val currentPart = pathParts.first()
    val remainingParts = pathParts.drop(1)
    
    for (value in configurable.inner) {
        if (value.name == currentPart) {
            return processMatchedValue(value, remainingParts)
        }
    }
    
    return null
}

/**
 * Process a matched value and handle remaining path parts
 */
private fun processMatchedValue(value: Value<*>, remainingParts: List<String>): Value<*>? {
    if (remainingParts.isEmpty()) {
        return value
    }
    
    return when (value) {
        is ChoiceConfigurable<*> -> handleChoiceConfigurable(value, remainingParts)
        is ToggleableConfigurable -> handleToggleableConfigurable(value, remainingParts)
        is Configurable -> findValueByPathParts(value, remainingParts)
        else -> null
    }
}

/**
 * Handle choice configurable path navigation
 */
private fun handleChoiceConfigurable(choice: ChoiceConfigurable<*>, remainingParts: List<String>): Value<*>? {
    if (remainingParts.size == 1 && remainingParts[0] == "active") {
        return createVirtualChoiceValue(choice)
    }
    return findValueByPathParts(choice.activeChoice, remainingParts)
}

/**
 * Handle toggleable configurable path navigation
 */
private fun handleToggleableConfigurable(toggleable: ToggleableConfigurable, remainingParts: List<String>): Value<*>? {
    if (remainingParts.size == 1 && remainingParts[0] == "enabled") {
        return toggleable.inner.find { it.name == "Enabled" }
    }
    return findValueByPathParts(toggleable, remainingParts)
}

/**
 * Create a virtual value for choice selection
 */
private fun createVirtualChoiceValue(choiceConfigurable: ChoiceConfigurable<*>): Value<*> {
    // Create a simple proxy that handles choice selection
    val choiceValue = Value<String>(
        name = "active",
        aliases = emptyArray(),
        defaultValue = choiceConfigurable.choices.firstOrNull()?.name ?: "",
        valueType = ValueType.CHOICE
    )
    
    // Set the current value to match the active choice
    choiceValue.set(choiceConfigurable.activeChoice.name)
    
    return choiceValue
}

/**
 * Update a field value with proper type conversion
 */
private fun updateFieldValue(field: Value<*>, newValue: Any) {
    when (field.valueType) {
        ValueType.BOOLEAN -> {
            @Suppress("UNCHECKED_CAST")
            (field as Value<Boolean>).set(convertToBoolean(newValue))
        }
        ValueType.INT -> {
            @Suppress("UNCHECKED_CAST")
            (field as Value<Int>).set(convertToInt(newValue))
        }
        ValueType.FLOAT -> {
            @Suppress("UNCHECKED_CAST")
            (field as Value<Float>).set(convertToFloat(newValue))
        }
        ValueType.TEXT -> {
            @Suppress("UNCHECKED_CAST")
            (field as Value<String>).set(newValue.toString())
        }
        ValueType.CHOICE -> {
            if (field is ChooseListValue<*>) {
                field.setByString(newValue.toString())
            } else {
                // Virtual choice value - use setByString for type safety
                field.setByString(newValue.toString())
            }
        }
        ValueType.INT_RANGE -> {
            @Suppress("UNCHECKED_CAST")
            (field as Value<IntRange>).set(convertToIntRange(newValue))
        }
        ValueType.FLOAT_RANGE -> {
            @Suppress("UNCHECKED_CAST")
            (field as Value<ClosedFloatingPointRange<Float>>).set(convertToFloatRange(newValue))
        }
        else -> {
            // Generic approach - try to set as string
            field.setByString(newValue.toString())
        }
    }
}

/**
 * Type conversion utilities
 */
private fun convertToBoolean(value: Any): Boolean {
    return when (value) {
        is Boolean -> value
        is String -> value.toBoolean()
        is Number -> value.toDouble() != 0.0
        else -> false
    }
}

private fun convertToInt(value: Any): Int {
    return when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: 0
        else -> 0
    }
}

private fun convertToFloat(value: Any): Float {
    return when (value) {
        is Number -> value.toFloat()
        is String -> value.toFloatOrNull() ?: 0f
        else -> 0f
    }
}

private fun convertToIntRange(value: Any): IntRange {
    return when (value) {
        is Map<*, *> -> {
            val start = convertToInt(value["start"] ?: 0)
            val end = convertToInt(value["end"] ?: 0)
            start..end
        }
        is List<*> -> {
            val start = convertToInt(value.getOrNull(0) ?: 0)
            val end = convertToInt(value.getOrNull(1) ?: 0)
            start..end
        }
        else -> 0..0
    }
}

private fun convertToFloatRange(value: Any): ClosedFloatingPointRange<Float> {
    return when (value) {
        is Map<*, *> -> {
            val start = convertToFloat(value["start"] ?: 0f)
            val end = convertToFloat(value["end"] ?: 0f)
            start..end
        }
        is List<*> -> {
            val start = convertToFloat(value.getOrNull(0) ?: 0f)
            val end = convertToFloat(value.getOrNull(1) ?: 0f)
            start..end
        }
        else -> 0f..0f
    }
}

/**
 * Map ValueType to string representation
 */
private fun mapValueTypeToString(valueType: ValueType): String {
    return when (valueType) {
        ValueType.BOOLEAN -> "boolean"
        ValueType.INT -> "number"
        ValueType.FLOAT -> "number"
        ValueType.INT_RANGE -> "range"
        ValueType.FLOAT_RANGE -> "range"
        ValueType.TEXT -> "string"
        ValueType.CHOICE -> "choice"
        ValueType.COLOR -> "color"
        ValueType.KEY -> "key"
        ValueType.BIND -> "bind"
        else -> "string"
    }
}
