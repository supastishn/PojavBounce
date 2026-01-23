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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isSword
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.MaceItem
import net.minecraft.world.item.TridentItem
import net.minecraft.world.item.enchantment.Enchantments

/**
 * AttributeSwap module
 *
 * Temporarily switches to different items before attacks to exploit
 * Minecraft's attribute application delay, then immediately reverts back.
 *
 * Supports manual slot selection and automatic best attribute detection
 * with priority queue system for customizable attribute prioritization.
 */
object ModuleAttributeSwap : ClientModule("AttributeSwap", ModuleCategories.COMBAT) {

    private val swapMode by enumChoice("Mode", SwapMode.AUTOMATIC)
    private val attributeTarget by enumChoice("Target", AttributeTarget.DAMAGE)

    // Manual mode - multi-select slots
    private val manualSlots by intRange("ManualSlots", 0..8, 0..8)
        .doNotIncludeWhen { swapMode != SwapMode.MANUAL }

    // Priority queue for PRIORITY_QUEUE mode
    private val priorityList by multiEnumChoice(
        "PriorityList",
        enumSetOf(
            EnchantmentPriority.BREACH,
            EnchantmentPriority.FIRE_ASPECT,
            EnchantmentPriority.KNOCKBACK,
            EnchantmentPriority.SHARPNESS,
            EnchantmentPriority.DAMAGE,
            EnchantmentPriority.SPEED,
            EnchantmentPriority.REACH
        )
    ).doNotIncludeWhen { attributeTarget != AttributeTarget.PRIORITY_QUEUE }

    // Durability save mode settings
    private val saveSlot by int("DurabilitySaveSlot", 0, 0..8, "slot")
        .doNotIncludeWhen { attributeTarget != AttributeTarget.DURABILITY_SAVE }
    private val saveUseEmpty by boolean("SaveUseEmpty", true)
        .doNotIncludeWhen { attributeTarget != AttributeTarget.DURABILITY_SAVE }

    // Timing
    private val switchBackDelay by int("SwitchBackDelay", 0, 0..5, "ticks")

    enum class EnchantmentPriority(override val choiceName: String) : NamedChoice {
        BREACH("Breach"),
        FIRE_ASPECT("FireAspect"),
        KNOCKBACK("Knockback"),
        SHARPNESS("Sharpness"),
        DAMAGE("Damage"),
        SPEED("Speed"),
        REACH("Reach")
    }

    enum class SwapMode(override val choiceName: String) : NamedChoice {
        MANUAL("Manual"),           // User specifies slots
        AUTOMATIC("Automatic")      // Auto-detect best attributes
    }

    enum class AttributeTarget(override val choiceName: String) : NamedChoice {
        DAMAGE("Damage"),          // Highest attack damage
        SPEED("Speed"),           // Highest attack speed
        KNOCKBACK("Knockback"),       // Highest knockback
        BREACH("Breach"),          // Mace with Breach enchantment
        REACH("Reach"),           // Extended reach (Spear/Trident)
        FIRE_ASPECT("FireAspect"),     // Fire aspect enchantment
        DURABILITY_SAVE("DurabilitySave"), // Swap to empty hand/non-tool to save durability
        PRIORITY_QUEUE("PriorityQueue")   // Customizable priority list
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent>(
        priority = FIRST_PRIORITY
    ) { event ->
        if (!enabled) return@handler

        val targetSlot = determineSwapSlot() ?: return@handler
        val currentSlot = player.inventory.selectedSlot

        if (targetSlot == currentSlot) return@handler

        // Swap to target item
        SilentHotbar.selectSlotSilently(
            this,
            targetSlot,
            switchBackDelay
        )

        // Ensure the carried item packet is sent with new slot
        interaction.ensureHasSentCarriedItem()
    }

    private fun determineSwapSlot(): Int? {
        return when (swapMode) {
            SwapMode.MANUAL -> determineManualSlot()
            SwapMode.AUTOMATIC -> determineAutomaticSlot()
        }
    }

    private fun determineManualSlot(): Int? {
        // Find first non-empty slot in manual slot range
        for (slot in manualSlots.first..manualSlots.last) {
            if (!Slots.Hotbar[slot].itemStack.isEmpty) {
                return slot
            }
        }
        return null
    }

    private fun determineAutomaticSlot(): Int? {
        return when (attributeTarget) {
            AttributeTarget.DAMAGE -> getBestDamageSlot()
            AttributeTarget.SPEED -> getBestSpeedSlot()
            AttributeTarget.KNOCKBACK -> getBestKnockbackSlot()
            AttributeTarget.BREACH -> getBestBreachSlot()
            AttributeTarget.REACH -> getBestReachSlot()
            AttributeTarget.FIRE_ASPECT -> getBestFireAspectSlot()
            AttributeTarget.DURABILITY_SAVE -> getDurabilitySaveSlot()
            AttributeTarget.PRIORITY_QUEUE -> getPriorityQueueSlot()
        }
    }

    private fun getBestDamageSlot(): Int? {
        return Slots.Hotbar.indices
            .map { slot -> slot to Slots.Hotbar[slot].itemStack }
            .filter { (_, stack) -> !stack.isEmpty }
            .maxByOrNull { (_, stack) -> stack.attackDamage }
            ?.first
    }

    private fun getBestSpeedSlot(): Int? {
        return Slots.Hotbar.indices
            .map { slot -> slot to Slots.Hotbar[slot].itemStack }
            .filter { (_, stack) -> !stack.isEmpty }
            .maxByOrNull { (_, stack) -> stack.attackSpeed }
            ?.first
    }

    private fun getBestKnockbackSlot(): Int? {
        return Slots.Hotbar.indices
            .map { slot -> slot to Slots.Hotbar[slot].itemStack }
            .filter { (_, stack) -> !stack.isEmpty }
            .maxByOrNull { (_, stack) -> getKnockbackValue(stack) }
            ?.first
    }

    private fun getBestBreachSlot(): Int? {
        return Slots.Hotbar.indices
            .map { slot -> slot to Slots.Hotbar[slot].itemStack }
            .filter { (_, stack) -> !stack.isEmpty }
            .maxByOrNull { (_, stack) -> getBreachValue(stack) }
            ?.first
    }

    private fun getBestReachSlot(): Int? {
        return Slots.Hotbar.indices
            .map { slot -> slot to Slots.Hotbar[slot].itemStack }
            .filter { (_, stack) -> !stack.isEmpty }
            .maxByOrNull { (_, stack) -> getReachValue(stack) }
            ?.first
    }

    private fun getBestFireAspectSlot(): Int? {
        return Slots.Hotbar.indices
            .map { slot -> slot to Slots.Hotbar[slot].itemStack }
            .filter { (_, stack) -> !stack.isEmpty }
            .maxByOrNull { (_, stack) -> getFireAspectValue(stack) }
            ?.first
    }

    private fun getKnockbackValue(stack: ItemStack): Double {
        return stack.getEnchantment(Enchantments.KNOCKBACK).toDouble()
    }

    private fun getBreachValue(stack: ItemStack): Double {
        // Prioritize Mace with Breach enchantment
        val isMace = stack.item is MaceItem
        val breachLevel = stack.getEnchantment(Enchantments.BREACH).toDouble()
        return if (isMace && breachLevel > 0) 100.0 + breachLevel else 0.0
    }

    private fun getReachValue(stack: ItemStack): Double {
        // Tridents and specific items with reach
        return when {
            stack.item is TridentItem -> 10.0
            else -> 0.0
        }
    }

    private fun getFireAspectValue(stack: ItemStack): Double {
        return stack.getEnchantment(Enchantments.FIRE_ASPECT).toDouble()
    }

    private fun getDurabilitySaveSlot(): Int? {
        return if (saveUseEmpty) {
            // Find first empty slot
            Slots.Hotbar.indices.firstOrNull { slot ->
                Slots.Hotbar[slot].itemStack.isEmpty
            }
        } else {
            // Use specified save slot if it's empty or non-tool
            val stack = Slots.Hotbar[saveSlot].itemStack
            if (stack.isEmpty || !isToolOrWeapon(stack)) saveSlot else null
        }
    }

    private fun isToolOrWeapon(stack: ItemStack): Boolean {
        return stack.isSword || stack.isAxe || stack.item is TridentItem
    }

    private fun getPriorityQueueSlot(): Int? {
        // Iterate through priority list and find first matching item
        for (priority in priorityList) {
            val slot = Slots.Hotbar.indices
                .map { slot -> slot to Slots.Hotbar[slot].itemStack }
                .filter { (_, stack) -> !stack.isEmpty }
                .firstOrNull { (_, stack) ->
                    when (priority) {
                        EnchantmentPriority.BREACH ->
                            stack.item is MaceItem && stack.getEnchantment(Enchantments.BREACH) > 0
                        EnchantmentPriority.FIRE_ASPECT ->
                            stack.getEnchantment(Enchantments.FIRE_ASPECT) > 0
                        EnchantmentPriority.KNOCKBACK ->
                            stack.getEnchantment(Enchantments.KNOCKBACK) > 0
                        EnchantmentPriority.SHARPNESS ->
                            stack.getEnchantment(Enchantments.SHARPNESS) > 0
                        EnchantmentPriority.DAMAGE ->
                            stack.attackDamage > 1.0
                        EnchantmentPriority.SPEED ->
                            stack.attackSpeed > 0.0
                        EnchantmentPriority.REACH ->
                            stack.item is TridentItem
                    }
                }?.first

            if (slot != null) {
                return slot
            }
        }
        return null
    }
}
