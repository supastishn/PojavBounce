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
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import net.ccbluex.liquidbounce.config.types.SettingsTreeSerializer
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Simple tests for the settings tree functions
 */
class SettingsTreeFunctionsTestSimple {

    private lateinit var testModule: TestModule

    @BeforeEach
    fun setup() {
        testModule = TestModule()
    }

    @Test
    fun `FieldUpdateRequest should create correctly`() {
        val updateRequest = FieldUpdateRequest(
            module = testModule.name,
            fieldPath = "TestModule.testBoolean",
            value = false
        )

        assertNotNull(updateRequest.module)
        assertNotNull(updateRequest.fieldPath)
        assertNotNull(updateRequest.value)
        assertEquals("TestModule", updateRequest.module)
        assertEquals("TestModule.testBoolean", updateRequest.fieldPath)
        assertEquals(false, updateRequest.value)
    }

    @Test
    fun `should handle basic serialization without errors`() {
        // Test that the core functionality doesn't throw exceptions
        assertDoesNotThrow {
            SettingsTreeSerializer.serializeModule(testModule)
        }
    }

    /**
     * Test module with basic settings for testing
     */
    private class TestModule : ClientModule("TestModule", Category.MISC) {
        val testBoolean by boolean("testBoolean", true)
        val testInt by int("testInt", 5, 1..10)
    }
}