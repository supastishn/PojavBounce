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
package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.ClientModule
import java.util.*

/**
 * Serializes a module's nested Configurable/Choice tree into a JSON 'settings tree'
 * that includes group/choice names, field types, current values, enabled/visibility 
 * predicates, stable IDs, and endpoints.
 */
object SettingsTreeSerializer {

    /**
     * Serialize a module's settings tree to JSON
     */
    fun serializeModule(module: ClientModule): SettingsTree {
        return SettingsTree(
            moduleId = module.name,
            moduleName = module.name,
            moduleDescription = module.description.get() ?: "",
            groups = serializeConfigurable(module, ""),
            stableId = generateStableId(module.name)
        )
    }

    /**
     * Serialize a configurable into a list of setting groups
     */
    private fun serializeConfigurable(configurable: Configurable, pathPrefix: String): List<SettingsGroup> {
        val groups = mutableListOf<SettingsGroup>()
        val currentPath = if (pathPrefix.isEmpty()) configurable.name else "$pathPrefix.${configurable.name}"
        
        // Group settings by type and organize them
        val fields = mutableListOf<SettingsField>()
        val subGroups = mutableListOf<SettingsGroup>()
        
        for (value in configurable.inner) {
            processConfigurableValue(value, currentPath, fields, subGroups)
        }
        
        // Create main group if we have direct fields
        if (fields.isNotEmpty()) {
            groups.add(createMainGroup(configurable, currentPath, fields))
        }
        
        groups.addAll(subGroups)
        return groups
    }
    
    /**
     * Process a single configurable value and add to appropriate collection
     */
    private fun processConfigurableValue(
        value: Value<*>, 
        currentPath: String, 
        fields: MutableList<SettingsField>, 
        subGroups: MutableList<SettingsGroup>
    ) {
        when (value) {
            is ChoiceConfigurable<*> -> {
                subGroups.add(createChoiceGroup(value, currentPath))
            }
            is ToggleableConfigurable -> {
                subGroups.add(createToggleableGroup(value, currentPath))
            }
            is Configurable -> {
                // Recursively serialize nested configurables
                subGroups.addAll(serializeConfigurable(value, currentPath))
            }
            else -> {
                // Regular value field
                createSettingsField(value, currentPath)?.let { field ->
                    fields.add(field)
                }
            }
        }
    }
    
    /**
     * Create a choice group
     */
    private fun createChoiceGroup(choiceConfigurable: ChoiceConfigurable<*>, currentPath: String): SettingsGroup {
        return SettingsGroup(
            groupId = "${currentPath}.${choiceConfigurable.name}",
            groupName = choiceConfigurable.name,
            groupType = SettingsGroupType.CHOICE,
            expanded = false,
            visible = isValueVisible(choiceConfigurable),
            enabled = isValueEnabled(choiceConfigurable),
            fields = listOf(createChoiceField(choiceConfigurable, currentPath)),
            subGroups = choiceConfigurable.choices.map { choice ->
                serializeChoice(choice, "${currentPath}.${choiceConfigurable.name}")
            }.flatten(),
            stableId = generateStableId("${currentPath}.${choiceConfigurable.name}")
        )
    }
    
    /**
     * Create a toggleable group
     */
    private fun createToggleableGroup(toggleable: ToggleableConfigurable, currentPath: String): SettingsGroup {
        return SettingsGroup(
            groupId = "${currentPath}.${toggleable.name}",
            groupName = toggleable.name,
            groupType = SettingsGroupType.TOGGLEABLE,
            expanded = false,
            visible = isValueVisible(toggleable),
            enabled = isValueEnabled(toggleable),
            fields = listOf(createToggleField(toggleable, currentPath)) + 
                     toggleable.inner.mapNotNull { createSettingsField(it, "${currentPath}.${toggleable.name}") },
            subGroups = emptyList(),
            stableId = generateStableId("${currentPath}.${toggleable.name}")
        )
    }
    
    /**
     * Create the main group for direct fields
     */
    private fun createMainGroup(
        configurable: Configurable, 
        currentPath: String, 
        fields: List<SettingsField>
    ): SettingsGroup {
        return SettingsGroup(
            groupId = currentPath,
            groupName = configurable.name,
            groupType = SettingsGroupType.NORMAL,
            expanded = false,
            visible = true,
            enabled = true,
            fields = fields,
            subGroups = emptyList(),
            stableId = generateStableId(currentPath)
        )
    }
    
    /**
     * Serialize a choice into setting groups
     */
    private fun serializeChoice(choice: Choice, pathPrefix: String): List<SettingsGroup> {
        return serializeConfigurable(choice, pathPrefix).map { group ->
            group.copy(
                visible = group.visible && choice.isSelected,
                groupName = "${choice.name} - ${group.groupName}"
            )
        }
    }
    
    /**
     * Create a settings field from a value
     */
    private fun createSettingsField(value: Value<*>, pathPrefix: String): SettingsField? {
        // Skip internal values that shouldn't be shown in UI
        if (value.name.equals("Enabled", true) && value.doNotInclude()) {
            return null
        }
        
        val fieldPath = "$pathPrefix.${value.name}"
        
        return SettingsField(
            fieldId = fieldPath,
            fieldName = value.name,
            fieldType = mapValueType(value.valueType),
            currentValue = value.get(),
            defaultValue = null, // TODO: Access defaultValue when public API available
            visible = isValueVisible(value),
            enabled = isValueEnabled(value),
            metadata = createFieldMetadata(value),
            stableId = generateStableId(fieldPath),
            endpoint = SettingsEndpoint(
                get = "/api/v1/client/modules/settings/field?field=$fieldPath",
                set = "/api/v1/client/modules/settings/field"
            )
        )
    }
    
    /**
     * Create a choice field
     */
    private fun createChoiceField(choiceConfigurable: ChoiceConfigurable<*>, pathPrefix: String): SettingsField {
        val fieldPath = "$pathPrefix.${choiceConfigurable.name}"
        
        return SettingsField(
            fieldId = fieldPath,
            fieldName = choiceConfigurable.name,
            fieldType = SettingsFieldType.CHOICE,
            currentValue = choiceConfigurable.activeChoice.name,
            defaultValue = choiceConfigurable.choices.firstOrNull()?.name ?: "",
            visible = isValueVisible(choiceConfigurable),
            enabled = isValueEnabled(choiceConfigurable),
            metadata = mapOf(
                "choices" to choiceConfigurable.choices.map { it.name }
            ),
            stableId = generateStableId(fieldPath),
            endpoint = SettingsEndpoint(
                get = "/api/v1/client/modules/settings/field?field=$fieldPath",
                set = "/api/v1/client/modules/settings/field"
            )
        )
    }
    
    /**
     * Create a toggle field
     */
    private fun createToggleField(toggleable: ToggleableConfigurable, pathPrefix: String): SettingsField {
        val fieldPath = "$pathPrefix.${toggleable.name}.enabled"
        val enabledValue = toggleable.inner.find { it.name == "Enabled" } as? Value<Boolean>
        
        return SettingsField(
            fieldId = fieldPath,
            fieldName = "Enabled",
            fieldType = SettingsFieldType.BOOLEAN,
            currentValue = toggleable.enabled,
            defaultValue = null, // TODO: Access defaultValue when public API available
            visible = enabledValue?.let { isValueVisible(it) } ?: true,
            enabled = enabledValue?.let { isValueEnabled(it) } ?: true,
            metadata = emptyMap(),
            stableId = generateStableId(fieldPath),
            endpoint = SettingsEndpoint(
                get = "/api/v1/client/modules/settings/field?field=$fieldPath",
                set = "/api/v1/client/modules/settings/field"
            )
        )
    }
    
    /**
     * Map ValueType to SettingsFieldType
     */
    private fun mapValueType(valueType: ValueType): SettingsFieldType {
        return when (valueType) {
            ValueType.BOOLEAN -> SettingsFieldType.BOOLEAN
            ValueType.INT -> SettingsFieldType.NUMBER
            ValueType.FLOAT -> SettingsFieldType.NUMBER
            ValueType.INT_RANGE -> SettingsFieldType.RANGE
            ValueType.FLOAT_RANGE -> SettingsFieldType.RANGE
            ValueType.TEXT -> SettingsFieldType.STRING
            ValueType.CHOICE -> SettingsFieldType.CHOICE
            ValueType.COLOR -> SettingsFieldType.COLOR
            ValueType.KEY -> SettingsFieldType.KEY
            ValueType.BIND -> SettingsFieldType.BIND
            else -> SettingsFieldType.STRING
        }
    }
    
    /**
     * Create field metadata based on value type
     */
    private fun createFieldMetadata(value: Value<*>): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()
        
        when (value) {
            is RangedValue<*> -> {
                metadata["min"] = value.range.start
                metadata["max"] = value.range.endInclusive
                metadata["suffix"] = value.suffix
            }
            is ChooseListValue<*> -> {
                metadata["choices"] = value.choices.map { it.choiceName }
            }
            is MultiChooseEnumListValue<*> -> {
                // TODO: Add proper handling for MultiChooseEnumListValue  
                metadata["choices"] = emptyList<String>()
                metadata["canBeNone"] = true
            }
        }
        
        return metadata
    }
    
    /**
     * Check if a value is visible based on doNotIncludeWhen predicates
     */
    private fun isValueVisible(value: Value<*>): Boolean {
        return !value.doNotInclude()
    }
    
    /**
     * Check if a value is enabled (can be modified)
     */
    private fun isValueEnabled(value: Value<*>): Boolean {
        // For now, assume all visible values are enabled
        // This could be extended to check parent configurables
        return isValueVisible(value)
    }
    
    /**
     * Generate a stable ID for a setting path
     */
    private fun generateStableId(path: String): String {
        return UUID.nameUUIDFromBytes(path.toByteArray()).toString()
    }
}

/**
 * Represents the complete settings tree for a module
 */
data class SettingsTree(
    val moduleId: String,
    val moduleName: String,
    val moduleDescription: String,
    val groups: List<SettingsGroup>,
    val stableId: String
)

/**
 * Represents a group of related settings
 */
data class SettingsGroup(
    val groupId: String,
    val groupName: String,
    val groupType: SettingsGroupType,
    val expanded: Boolean,
    val visible: Boolean,
    val enabled: Boolean,
    val fields: List<SettingsField>,
    val subGroups: List<SettingsGroup>,
    val stableId: String
)

/**
 * Type of settings group
 */
enum class SettingsGroupType {
    NORMAL,
    CHOICE,
    TOGGLEABLE
}

/**
 * Represents an individual setting field
 */
data class SettingsField(
    val fieldId: String,
    val fieldName: String,
    val fieldType: SettingsFieldType,
    val currentValue: Any?,
    val defaultValue: Any?,
    val visible: Boolean,
    val enabled: Boolean,
    val metadata: Map<String, Any>,
    val stableId: String,
    val endpoint: SettingsEndpoint
)

/**
 * Type of settings field
 */
enum class SettingsFieldType {
    BOOLEAN,
    NUMBER,
    STRING,
    CHOICE,
    RANGE,
    COLOR,
    KEY,
    BIND
}

/**
 * REST endpoint information for a field
 */
data class SettingsEndpoint(
    val get: String,
    val set: String
)
