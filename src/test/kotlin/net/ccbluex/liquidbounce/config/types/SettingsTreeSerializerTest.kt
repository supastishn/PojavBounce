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

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Unit tests for SettingsTreeSerializer
 */
class SettingsTreeSerializerTest {

    private lateinit var testModule: TestModule

    @BeforeEach
    fun setup() {
        testModule = TestModule()
    }

    @Test
    fun `should serialize basic module settings`() {
        val settingsTree = SettingsTreeSerializer.serializeModule(testModule)

        assertNotNull(settingsTree)
        assertEquals("TestModule", settingsTree.moduleId)
        assertEquals("TestModule", settingsTree.moduleName)
        assertTrue(settingsTree.groups.isNotEmpty())
        assertNotNull(settingsTree.stableId)
    }

    @Test
    fun `should generate consistent stable IDs`() {
        val tree1 = SettingsTreeSerializer.serializeModule(testModule)
        val tree2 = SettingsTreeSerializer.serializeModule(testModule)

        assertEquals(tree1.stableId, tree2.stableId)
        assertEquals(tree1.groups.size, tree2.groups.size)
        
        for (i in tree1.groups.indices) {
            assertEquals(tree1.groups[i].stableId, tree2.groups[i].stableId)
        }
    }

    @Test
    fun `should serialize boolean fields correctly`() {
        val settingsTree = SettingsTreeSerializer.serializeModule(testModule)
        val booleanField = findFieldByName(settingsTree, "testBoolean")

        assertNotNull(booleanField)
        assertEquals(SettingsFieldType.BOOLEAN, booleanField!!.fieldType)
        assertEquals(true, booleanField.currentValue)
        assertEquals(false, booleanField.defaultValue)
        assertTrue(booleanField.visible)
        assertTrue(booleanField.enabled)
    }

    @Test
    fun `should serialize number fields correctly`() {
        val settingsTree = SettingsTreeSerializer.serializeModule(testModule)
        val intField = findFieldByName(settingsTree, "testInt")
        val floatField = findFieldByName(settingsTree, "testFloat")

        assertNotNull(intField)
        assertEquals(SettingsFieldType.NUMBER, intField!!.fieldType)
        assertEquals(5, intField.currentValue)
        assertTrue(intField.metadata.containsKey("min"))
        assertTrue(intField.metadata.containsKey("max"))

        assertNotNull(floatField)
        assertEquals(SettingsFieldType.NUMBER, floatField!!.fieldType)
        assertEquals(1.5f, floatField.currentValue)
    }

    @Test
    fun `should serialize range fields correctly`() {
        val settingsTree = SettingsTreeSerializer.serializeModule(testModule)
        val rangeField = findFieldByName(settingsTree, "testRange")

        assertNotNull(rangeField)
        assertEquals(SettingsFieldType.RANGE, rangeField!!.fieldType)
        assertTrue(rangeField.metadata.containsKey("min"))
        assertTrue(rangeField.metadata.containsKey("max"))
    }

    @Test
    fun `should serialize toggleable configurables correctly`() {
        val settingsTree = SettingsTreeSerializer.serializeModule(testModule)
        val toggleGroup = settingsTree.groups.find { it.groupType == SettingsGroupType.TOGGLEABLE }

        assertNotNull(toggleGroup)
        assertEquals(SettingsGroupType.TOGGLEABLE, toggleGroup!!.groupType)
        assertTrue(toggleGroup.fields.isNotEmpty())
        
        val enabledField = toggleGroup.fields.find { it.fieldName == "Enabled" }
        assertNotNull(enabledField)
        assertEquals(SettingsFieldType.BOOLEAN, enabledField!!.fieldType)
    }

    @Test
    fun `should include proper endpoints for fields`() {
        val settingsTree = SettingsTreeSerializer.serializeModule(testModule)
        val field = settingsTree.groups.flatMap { it.fields }.first()

        assertNotNull(field.endpoint)
        assertTrue(field.endpoint.get.contains("/api/v1/client/modules/settings/field"))
        assertTrue(field.endpoint.set.contains("/api/v1/client/modules/settings/field"))
    }

    @Test
    fun `should handle nested configurables`() {
        val settingsTree = SettingsTreeSerializer.serializeModule(testModule)
        val nestedGroup = settingsTree.groups.find { it.groupName.contains("TestNested") }

        assertNotNull(nestedGroup)
        assertTrue(nestedGroup!!.fields.isNotEmpty() || nestedGroup.subGroups.isNotEmpty())
    }

    @Test
    fun `should filter out internal values`() {
        val settingsTree = SettingsTreeSerializer.serializeModule(testModule)
        val enabledFields = settingsTree.groups.flatMap { it.fields }
            .filter { it.fieldName.equals("Enabled", true) }

        // Should only have enabled fields for toggleable configurables, not the main module
        assertTrue(enabledFields.isEmpty() || enabledFields.all { !it.fieldId.endsWith("TestModule.Enabled") })
    }

    @Test
    fun `serialization should not throw exceptions`() {
        assertDoesNotThrow {
            SettingsTreeSerializer.serializeModule(testModule)
        }
    }

    @Test
    fun `should handle empty configurables gracefully`() {
        val emptyModule = object : ClientModule("EmptyModule", Category.MISC) {}
        
        assertDoesNotThrow {
            val settingsTree = SettingsTreeSerializer.serializeModule(emptyModule)
            assertNotNull(settingsTree)
            assertEquals("EmptyModule", settingsTree.moduleId)
        }
    }

    private fun findFieldByName(tree: SettingsTree, name: String): SettingsField? {
        return tree.groups.flatMap { group ->
            group.fields + group.subGroups.flatMap { it.fields }
        }.find { it.fieldName == name }
    }

    /**
     * Test module with various setting types for testing
     */
    private class TestModule : ClientModule("TestModule", Category.MISC) {
        
        @Suppress("UnusedPrivateProperty")
        private val testBoolean by boolean("testBoolean", true)
        @Suppress("UnusedPrivateProperty") 
        private val testInt by int("testInt", 5, 1..10)
        @Suppress("UnusedPrivateProperty")
        private val testFloat by float("testFloat", 1.5f, 0f..5f)
        @Suppress("UnusedPrivateProperty")
        private val testString by text("testString", "default")
        @Suppress("UnusedPrivateProperty")
        private val testRange by intRange("testRange", 1..5, 0..10)
        
        // Toggleable configurable  
        @Suppress("UnusedPrivateProperty")
        private val testToggleable = tree(object : ToggleableConfigurable(null, "TestToggleable", true) {
            val nestedValue by boolean("nestedBoolean", false)
        })
        
        // Nested configurable
        @Suppress("UnusedPrivateProperty")
        private val testNested = tree(object : Configurable("TestNested") {
            val deepValue by text("deepValue", "nested")
        })
    }
}
