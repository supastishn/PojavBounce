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
package net.ccbluex.liquidbounce.features.module.modules.render.gui

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.*
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.registry.Registries

/**
 * Factory object for creating setting widgets for ClickGui panels
 */
object ClickGuiPanelWidgetFactory {
    private const val SETTING_HEIGHT = 18
    private const val SETTING_SPACING = 2

    fun initializeSettingsWidgets(
        module: ClientModule,
        panelX: Int,
        panelWidth: Int,
        existingWidgets: MutableMap<ClientModule, List<SettingWidget<*>>>,
        expandedSections: MutableMap<String, Boolean>
    ) {
        val widgets = mutableListOf<SettingWidget<*>>()
        val valueCreators = mutableListOf<Pair<Value<*>, Int>>()
        collectValues(module, valueCreators, 0, expandedSections)

        var currentY = 0 // y is set dynamically during render
        for ((value, indent) in valueCreators) {
            val widgetX = panelX + 10 + indent * 10
            val widgetWidth = panelWidth - 20 - indent * 10
            val widget = createWidgetForValue(value, widgetX, currentY, widgetWidth, module, expandedSections)
            if (widget != null) {
                widgets.add(widget)
                currentY += SETTING_HEIGHT + SETTING_SPACING
            }
        }
        existingWidgets[module] = widgets
    }

    private fun collectValues(
        configurable: Configurable,
        list: MutableList<Pair<Value<*>, Int>>,
        indent: Int,
        expandedSections: Map<String, Boolean>
    ) {
        for (value in configurable.inner) {
            if (value.notAnOption) continue

            if (value is Configurable) {
                list.add(Pair(value, indent)) // Add section header

                val isSectionExpanded = expandedSections.getOrDefault(value.name, true)
                if (isSectionExpanded) {
                    if (value is net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable<*>) {
                        collectValues(value.activeChoice, list, indent + 1, expandedSections)
                    } else {
                        collectValues(value, list, indent + 1, expandedSections)
                    }
                }
            } else {
                list.add(Pair(value, indent)) // Add normal value
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createWidgetForValue(
        value: Value<*>,
        widgetX: Int,
        widgetY: Int,
        widgetWidth: Int,
        module: ClientModule,
        expandedSections: Map<String, Boolean>
    ): SettingWidget<*>? {
        return when (value.valueType) {
            ValueType.BOOLEAN, ValueType.TOGGLEABLE -> createBooleanWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.FLOAT -> createFloatWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.FLOAT_RANGE -> createFloatRangeAsTextWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.INT -> createIntWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.INT_RANGE -> createIntRangeAsTextWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.CHOOSE, ValueType.CHOICE -> createEnumWidget(value, widgetX, widgetY, widgetWidth, module)
            ValueType.TEXT, ValueType.BIND, ValueType.LIST, ValueType.BLOCK, ValueType.COLOR, ValueType.MULTI_CHOOSE ->
                createTextWidget(value, widgetX, widgetY, widgetWidth, module)
            else -> if (value is Configurable) {
                createSectionHeaderWidget(value, widgetX, widgetY, widgetWidth, expandedSections)
            } else {
                null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createBooleanWidget(
        value: Value<*>, 
        widgetX: Int, 
        widgetY: Int, 
        widgetWidth: Int, 
        module: ClientModule
    ): BooleanSettingWidget {
        val typedValue = value as Value<Boolean>
        return BooleanSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                if (value is ToggleableConfigurable) {
                    value.enabled = newValue
                } else {
                    typedValue.set(newValue)
                }
                try {
                    ConfigSystem.storeConfigurable(module)
                } catch (e: Exception) {
                    println("Error saving configuration for module ${module.name}: ${e.message}")
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createFloatWidget(
        value: Value<*>, 
        widgetX: Int, 
        widgetY: Int, 
        widgetWidth: Int, 
        module: ClientModule
    ): FloatSettingWidget {
        val typedValue = value as Value<Float>
        val (min, max) = getRangeForValue(value, 0.0f, 10.0f)
        return FloatSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = RangeWidgetConfig(
                x = widgetX, y = widgetY, min = min, max = max, width = widgetWidth, height = SETTING_HEIGHT
            ),
            onValueChanged = { newValue ->
                typedValue.set(newValue)
                try {
                    ConfigSystem.storeConfigurable(module)
                } catch (e: Exception) {
                    println("Error saving configuration for module ${module.name}: ${e.message}")
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createIntWidget(
        value: Value<*>, 
        widgetX: Int, 
        widgetY: Int, 
        widgetWidth: Int, 
        module: ClientModule
    ): IntSettingWidget {
        val typedValue = value as Value<Int>
        val (min, max) = getRangeForValue(value, 0, 1000)
        return IntSettingWidget(
            name = value.name,
            value = typedValue.get(),
            config = IntRangeWidgetConfig(
                x = widgetX, y = widgetY, min = min, max = max, width = widgetWidth, height = SETTING_HEIGHT
            ),
            onValueChanged = { newValue ->
                typedValue.set(newValue)
                try {
                    ConfigSystem.storeConfigurable(module)
                } catch (e: Exception) {
                    println("Error saving configuration for module ${module.name}: ${e.message}")
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Comparable<T>> getRangeForValue(value: Value<*>, defaultMin: T, defaultMax: T): Pair<T, T> {
        return try {
            if (value is RangedValue<*>) {
                val range = value.range
                when (range) {
                    is ClosedFloatingPointRange<*> -> Pair(range.start as T, range.endInclusive as T)
                    is IntRange -> Pair(range.first as T, range.last as T)
                    else -> Pair(defaultMin, defaultMax)
                }
            } else {
                Pair(defaultMin, defaultMax)
            }
        } catch (e: Exception) {
            println("Warning: Invalid range configuration for value ${value.name}: ${e.message}")
            Pair(defaultMin, defaultMax)
        }
    }



    @Suppress("UNCHECKED_CAST")
    private fun createTextWidget(
        value: Value<*>, 
        widgetX: Int, 
        widgetY: Int, 
        widgetWidth: Int, 
        module: ClientModule
    ): TextSettingWidget {
        val displayValue = when (value.valueType) {
            ValueType.TEXT -> {
                val typedValue = value as Value<String>
                typedValue.get()
            }
            ValueType.BIND -> {
                val typedValue = value as Value<InputBind>
                typedValue.get().boundKey.translationKey
            }
            ValueType.COLOR -> {
                val typedValue = value as Value<Color4b>
                "#" + typedValue.get().toARGB().toUInt().toString(16)
            }
            ValueType.MULTI_CHOOSE -> {
                val typedValue = value as MultiChooseListValue<*>
                typedValue.get().joinToString(", ") { if (it is Enum<*>) it.name else it.toString() }
            }
            ValueType.LIST, ValueType.BLOCK -> {
                val typedValue = value as ListValue<*, *>
                if (typedValue is RegistryListValue<*, *>) {
                    formatRegistryListValue(typedValue)
                } else {
                    typedValue.get().joinToString(", ")
                }
            }
            else -> value.toString()
        }
        
        return TextSettingWidget(
            name = value.name,
            value = displayValue,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    try {
                        ConfigSystem.storeConfigurable(module)
                    } catch (e: Exception) {
                        println("Error saving configuration for module ${module.name}: ${e.message}")
                    }
                } catch (e: Exception) { 
                    println("Parse error: ${e.message}") 
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun formatRegistryListValue(value: RegistryListValue<*, *>): String {
        val collection = value.get() as Collection<Any>
        return try {
            when (value.innerType) {
                Block::class.java -> collection.joinToString(", ") { 
                    Registries.BLOCK.getId(it as Block).toString() 
                }
                Item::class.java -> collection.joinToString(", ") { 
                    Registries.ITEM.getId(it as Item).toString() 
                }
                else -> collection.joinToString(", ")
            }
        } catch (e: Exception) {
            println("Registry access error for ${value.name}: ${e.message}")
            collection.joinToString(", ")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createEnumWidget(
        value: Value<*>, 
        widgetX: Int, 
        widgetY: Int, 
        widgetWidth: Int, 
        module: ClientModule
    ): EnumSettingWidget {
        val (currentChoiceName, choiceNames) = when (value.valueType) {
            ValueType.CHOOSE -> {
                val chooseValue = value as ChooseListValue<*>
                val currentChoice = chooseValue.get() as NamedChoice
                val choices = chooseValue.choices.map { it.choiceName }.toTypedArray()
                Pair(currentChoice.choiceName, choices)
            }
            ValueType.CHOICE -> {
                val choiceConfigurable = value as net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable<*>
                val currentChoice = choiceConfigurable.activeChoice
                val choices = choiceConfigurable.choices.map { it.choiceName }.toTypedArray()
                Pair(currentChoice.choiceName, choices)
            }
            else -> Pair("", emptyArray<String>())
        }

        return EnumSettingWidget(
            name = value.name,
            value = currentChoiceName,
            choices = choiceNames,
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { choiceName ->
                value.setByString(choiceName)
                try {
                    ConfigSystem.storeConfigurable(module)
                } catch (e: Exception) {
                    println("Error saving configuration for module ${module.name}: ${e.message}")
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createIntRangeAsTextWidget(
        value: Value<*>,
        widgetX: Int,
        widgetY: Int,
        widgetWidth: Int,
        module: ClientModule
    ): TextSettingWidget {
        val typedValue = value as Value<IntRange>
        return TextSettingWidget(
            name = value.name,
            value = typedValue.get().let { "${it.first}..${it.last}" },
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    try {
                        ConfigSystem.storeConfigurable(module)
                    } catch (e: Exception) {
                        println("Error saving configuration for module ${module.name}: ${e.message}")
                    }
                } catch (e: Exception) {
                    println("Parse error: ${e.message}")
                }
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createFloatRangeAsTextWidget(
        value: Value<*>,
        widgetX: Int,
        widgetY: Int,
        widgetWidth: Int,
        module: ClientModule
    ): TextSettingWidget {
        val typedValue = value as Value<ClosedFloatingPointRange<Float>>
        return TextSettingWidget(
            name = value.name,
            value = typedValue.get().let { "${it.start}..${it.endInclusive}" },
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT),
            onValueChanged = { newValue ->
                try {
                    value.setByString(newValue)
                    try {
                        ConfigSystem.storeConfigurable(module)
                    } catch (e: Exception) {
                        println("Error saving configuration for module ${module.name}: ${e.message}")
                    }
                } catch (e: Exception) {
                    println("Parse error: ${e.message}")
                }
            }
        )
    }

    private fun createSectionHeaderWidget(
        value: Configurable,
        widgetX: Int,
        widgetY: Int,
        widgetWidth: Int,
        expandedSections: Map<String, Boolean>
    ): SectionHeaderWidget {
        return SectionHeaderWidget(
            name = value.name,
            isExpanded = expandedSections.getOrDefault(value.name, true),
            config = WidgetConfig(x = widgetX, y = widgetY, width = widgetWidth, height = SETTING_HEIGHT)
        )
    }
}
