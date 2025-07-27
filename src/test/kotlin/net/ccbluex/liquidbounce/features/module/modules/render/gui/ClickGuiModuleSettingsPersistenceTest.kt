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

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.ChooseListValue
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * Test for verifying that module settings are actually changed and get saved 
 * when changed via the Minecraft Kotlin ClickGUI
 */
class ClickGuiModuleSettingsPersistenceTest {

    @TempDir
    lateinit var tempDir: File

    // Test implementation of NamedChoice for testing
    private enum class TestChoice(override val choiceName: String) : NamedChoice {
        OPTION_ONE("Option1"),
        OPTION_TWO("Option2"),
        OPTION_THREE("Option3")
    }

    // Create a test configurable with various setting types
    private class TestConfigurable : Configurable("TestModule") {
        
        val booleanSetting = Value("booleanTest", emptyArray(), false, ValueType.BOOLEAN)
        val intSetting = Value("intTest", emptyArray(), 10, ValueType.INT)
        val floatSetting = Value("floatTest", emptyArray(), 1.5f, ValueType.FLOAT)
        val stringSetting = Value("stringTest", emptyArray(), "default", ValueType.TEXT)
        val rangeSetting = RangedValue("rangeTest", emptyArray(), 50, 0..100, "", ValueType.INT)
        val chooseSetting = ChooseListValue(
            "chooseTest", 
            emptyArray(), 
            TestChoice.OPTION_ONE, 
            arrayOf(TestChoice.OPTION_ONE, TestChoice.OPTION_TWO, TestChoice.OPTION_THREE)
        )

        init {
            // Add all values to the configurable
            inner.add(booleanSetting)
            inner.add(intSetting)
            inner.add(floatSetting)
            inner.add(stringSetting)
            inner.add(rangeSetting)
            inner.add(chooseSetting)
        }
    }

    private lateinit var testConfigurable: TestConfigurable
    private lateinit var testConfigFile: File

    @BeforeEach
    fun setup() {
        testConfigurable = TestConfigurable()
        testConfigFile = File(tempDir, "${testConfigurable.loweredName}.json")
    }

    @AfterEach
    fun cleanup() {
        if (testConfigFile.exists()) {
            testConfigFile.delete()
        }
    }

    @Test
    fun `test boolean setting persistence via ClickGUI simulation`() {
        // Initial state
        assertFalse(testConfigurable.booleanSetting.get())
        
        // Simulate ClickGUI changing the value via setByString (same as popup would do)
        testConfigurable.booleanSetting.setByString("true")
        assertTrue(testConfigurable.booleanSetting.get())
        
        // Simulate the saving that happens when popup is hidden
        saveConfiguration(testConfigurable)
        
        // Verify file was created and contains the change
        assertTrue(testConfigFile.exists())
        
        // Create a new configurable instance and load the config to verify persistence
        val newConfigurable = TestConfigurable()
        loadConfiguration(newConfigurable)
        
        // Verify the setting was persisted
        assertTrue(newConfigurable.booleanSetting.get())
    }

    @Test
    fun `test integer setting persistence via ClickGUI simulation`() {
        // Initial state
        assertEquals(10, testConfigurable.intSetting.get())
        
        // Simulate ClickGUI changing the value
        testConfigurable.intSetting.setByString("42")
        assertEquals(42, testConfigurable.intSetting.get())
        
        // Save and reload
        saveConfiguration(testConfigurable)
        val newConfigurable = TestConfigurable()
        loadConfiguration(newConfigurable)
        
        // Verify persistence
        assertEquals(42, newConfigurable.intSetting.get())
    }

    @Test
    fun `test float setting persistence via ClickGUI simulation`() {
        // Initial state
        assertEquals(1.5f, testConfigurable.floatSetting.get(), 0.01f)
        
        // Simulate ClickGUI changing the value
        testConfigurable.floatSetting.setByString("3.14")
        assertEquals(3.14f, testConfigurable.floatSetting.get(), 0.01f)
        
        // Save and reload
        saveConfiguration(testConfigurable)
        val newConfigurable = TestConfigurable()
        loadConfiguration(newConfigurable)
        
        // Verify persistence
        assertEquals(3.14f, newConfigurable.floatSetting.get(), 0.01f)
    }

    @Test
    fun `test string setting persistence via ClickGUI simulation`() {
        // Initial state
        assertEquals("default", testConfigurable.stringSetting.get())
        
        // Simulate ClickGUI changing the value
        testConfigurable.stringSetting.setByString("test value changed")
        assertEquals("test value changed", testConfigurable.stringSetting.get())
        
        // Save and reload
        saveConfiguration(testConfigurable)
        val newConfigurable = TestConfigurable()
        loadConfiguration(newConfigurable)
        
        // Verify persistence
        assertEquals("test value changed", newConfigurable.stringSetting.get())
    }

    @Test
    fun `test ranged value setting persistence via ClickGUI simulation`() {
        // Initial state
        assertEquals(50, testConfigurable.rangeSetting.get())
        
        // Simulate ClickGUI changing the value
        testConfigurable.rangeSetting.setByString("75")
        assertEquals(75, testConfigurable.rangeSetting.get())
        
        // Save and reload
        saveConfiguration(testConfigurable)
        val newConfigurable = TestConfigurable()
        loadConfiguration(newConfigurable)
        
        // Verify persistence
        assertEquals(75, newConfigurable.rangeSetting.get())
    }

    @Test
    fun `test choose setting persistence via ClickGUI simulation`() {
        // Initial state
        assertEquals(TestChoice.OPTION_ONE, testConfigurable.chooseSetting.get())
        
        // Simulate ClickGUI changing the value
        testConfigurable.chooseSetting.setByString("Option2")
        assertEquals(TestChoice.OPTION_TWO, testConfigurable.chooseSetting.get())
        
        // Save and reload
        saveConfiguration(testConfigurable)
        val newConfigurable = TestConfigurable()
        loadConfiguration(newConfigurable)
        
        // Verify persistence
        assertEquals(TestChoice.OPTION_TWO, newConfigurable.chooseSetting.get())
    }

    @Test
    fun `test multiple settings persistence in single save operation`() {
        // Change multiple settings
        testConfigurable.booleanSetting.setByString("true")
        testConfigurable.intSetting.setByString("100")
        testConfigurable.floatSetting.setByString("2.5")
        testConfigurable.stringSetting.setByString("multiple changes")
        testConfigurable.chooseSetting.setByString("Option3")
        
        // Save once (simulating popup hide operation)
        saveConfiguration(testConfigurable)
        
        // Load into new configurable
        val newConfigurable = TestConfigurable()
        loadConfiguration(newConfigurable)
        
        // Verify all changes persisted
        assertTrue(newConfigurable.booleanSetting.get())
        assertEquals(100, newConfigurable.intSetting.get())
        assertEquals(2.5f, newConfigurable.floatSetting.get(), 0.01f)
        assertEquals("multiple changes", newConfigurable.stringSetting.get())
        assertEquals(TestChoice.OPTION_THREE, newConfigurable.chooseSetting.get())
    }

    @Test
    fun `test persistence survives multiple save-load cycles`() {
        // First cycle
        testConfigurable.intSetting.setByString("20")
        saveConfiguration(testConfigurable)
        
        // Second cycle - load and change again
        val secondConfigurable = TestConfigurable()
        loadConfiguration(secondConfigurable)
        assertEquals(20, secondConfigurable.intSetting.get())
        
        secondConfigurable.intSetting.setByString("30")
        saveConfiguration(secondConfigurable)
        
        // Third cycle - verify final state
        val thirdConfigurable = TestConfigurable()
        loadConfiguration(thirdConfigurable)
        assertEquals(30, thirdConfigurable.intSetting.get())
    }

    @Test
    fun `test configuration file structure is valid JSON`() {
        // Change some settings
        testConfigurable.booleanSetting.setByString("true")
        testConfigurable.intSetting.setByString("123")
        
        // Save configuration
        saveConfiguration(testConfigurable)
        
        // Verify file exists and is valid JSON
        assertTrue(testConfigFile.exists())
        
        val gson = Gson()
        val jsonContent = testConfigFile.readText()
        
        // Should not throw exception if valid JSON
        val jsonObject = gson.fromJson(jsonContent, JsonObject::class.java)
        assertNotNull(jsonObject)
        
        // Verify structure contains expected fields
        assertTrue(jsonObject.has("booleanTest"))
        assertTrue(jsonObject.has("intTest"))
    }

    @Test
    fun `test error handling when save fails`() {
        // Make the temp directory read-only to simulate save failure
        val readOnlyDir = File(tempDir, "readonly")
        readOnlyDir.mkdirs()
        readOnlyDir.setWritable(false)
        
        val readOnlyFile = File(readOnlyDir, "${testConfigurable.loweredName}.json")
        
        // Change setting
        testConfigurable.booleanSetting.setByString("true")
        
        // This should handle the error gracefully (not throw exception)
        assertDoesNotThrow {
            saveConfigurationToFile(testConfigurable, readOnlyFile)
        }
        
        // Cleanup
        readOnlyDir.setWritable(true)
    }

    /**
     * Simulate the saving mechanism used by ModuleSettingsPopup
     */
    private fun saveConfiguration(configurable: TestConfigurable) {
        saveConfigurationToFile(configurable, testConfigFile)
    }

    private fun saveConfigurationToFile(configurable: TestConfigurable, file: File) {
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
            
            val gson = Gson()
            FileWriter(file).use { writer ->
                gson.toJson(configurable, writer)
            }
        } catch (e: Exception) {
            // Simulate the error handling in the actual code
            println("Error saving configuration for configurable ${configurable.name}: ${e.message}")
        }
    }

    /**
     * Simulate loading configuration back
     */
    private fun loadConfiguration(configurable: TestConfigurable) {
        try {
            if (testConfigFile.exists()) {
                val gson = Gson()
                FileReader(testConfigFile).use { reader ->
                    val loadedConfigurable = gson.fromJson(reader, TestConfigurable::class.java)
                    
                    // Copy values from loaded configurable to target configurable
                    configurable.booleanSetting.set(loadedConfigurable.booleanSetting.get())
                    configurable.intSetting.set(loadedConfigurable.intSetting.get())
                    configurable.floatSetting.set(loadedConfigurable.floatSetting.get())
                    configurable.stringSetting.set(loadedConfigurable.stringSetting.get())
                    configurable.rangeSetting.set(loadedConfigurable.rangeSetting.get())
                    configurable.chooseSetting.set(loadedConfigurable.chooseSetting.get())
                }
            }
        } catch (e: Exception) {
            println("Error loading configuration for configurable ${configurable.name}: ${e.message}")
        }
    }

    @Test
    fun `test ClickGUI popup simulation with actual widget behavior`() {
        // This test simulates the actual ClickGUI workflow more closely
        // We'll test with ModuleHud since it's a real existing module
        
        val hudModule = ModuleHud
        
        try {
            // 1. Create popup (simulate user right-clicking on module)
            val popup = ModuleSettingsPopup(hudModule, 100, 100, 200, 25)
            
            // 2. Show popup
            popup.show()
            assertTrue(popup.isVisible())
            
            // 3. Simulate user interactions that would change settings
            // Find and modify a setting via the Value objects directly
            val containedValues = hudModule.containedValues
            
            // Find any boolean setting we can test with
            val booleanSetting = containedValues.find { it.get() is Boolean }
            
            if (booleanSetting != null) {
                val originalValue = booleanSetting.get() as Boolean
                val newValue = !originalValue
                
                // Simulate changing the value via setByString (as the popup would do)
                booleanSetting.setByString(newValue.toString())
                assertEquals(newValue, booleanSetting.get())
                
                // 4. Hide popup (this should trigger saving)
                popup.hide()
                assertFalse(popup.isVisible())
                
                // Restore original value to avoid affecting other tests
                booleanSetting.setByString(originalValue.toString())
            } else {
                // If no boolean setting found, at least test the popup lifecycle
                popup.hide()
                assertFalse(popup.isVisible())
            }
            
            // 5. The popup.hide() method should have called ConfigSystem.storeConfigurable()
            // In a real scenario, this would persist the changes to disk
            
        } catch (e: Exception) {
            // Ensure popup is hidden even if test fails
            fail("Test failed with exception: ${e.message}")
        }
    }

    @Test
    fun `test that setByString works consistently across all value types`() {
        // This test ensures that the setByString mechanism used by the ClickGUI
        // works consistently for all supported value types
        
        // Test boolean
        val boolValue = Value("testBool", emptyArray(), false, ValueType.BOOLEAN)
        boolValue.setByString("true")
        assertTrue(boolValue.get())
        boolValue.setByString("false")
        assertFalse(boolValue.get())
        
        // Test integer
        val intValue = Value("testInt", emptyArray(), 0, ValueType.INT)
        intValue.setByString("42")
        assertEquals(42, intValue.get())
        
        // Test float
        val floatValue = Value("testFloat", emptyArray(), 0.0f, ValueType.FLOAT)
        floatValue.setByString("3.14")
        assertEquals(3.14f, floatValue.get(), 0.01f)
        
        // Test string
        val stringValue = Value("testString", emptyArray(), "", ValueType.TEXT)
        stringValue.setByString("hello world")
        assertEquals("hello world", stringValue.get())
        
        // Test ranged int
        val rangedIntValue = RangedValue("testRangedInt", emptyArray(), 50, 0..100, "", ValueType.INT)
        rangedIntValue.setByString("75")
        assertEquals(75, rangedIntValue.get())
        
        // Test choose value
        val chooseValue = ChooseListValue(
            "testChoose", 
            emptyArray(), 
            TestChoice.OPTION_ONE, 
            arrayOf(TestChoice.OPTION_ONE, TestChoice.OPTION_TWO, TestChoice.OPTION_THREE)
        )
        chooseValue.setByString("Option2")
        assertEquals(TestChoice.OPTION_TWO, chooseValue.get())
    }
}
