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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertFalse

/**
 * Test for blur effect being disabled
 */
class BlurEffectTest {
    
    @Test
    fun testBlurEffectIsCompletelyDisabled() {
        // The blur effect should always be disabled now
        assertFalse(ModuleHud.isBlurEffectActive, "Blur effect should be completely disabled")
        
        println("✓ Blur effect is correctly disabled")
    }
    
    @Test
    fun testBlurEffectStaysDisabledRegardlessOfSettings() {
        // Even if the internal blur setting is changed, the effect should remain disabled
        val module = ModuleHud
        
        // Try to enable the module
        module.enabled = true
        
        // Blur should still be disabled
        assertFalse(module.isBlurEffectActive, "Blur effect should remain disabled even when module is enabled")
        
        // Disable the module again
        module.enabled = false
        
        println("✓ Blur effect remains disabled regardless of module state")
    }
}
