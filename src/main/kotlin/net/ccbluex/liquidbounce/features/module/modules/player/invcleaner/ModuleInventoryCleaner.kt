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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType

/**
 * InventoryCleaner module
 *
 * Automatically throws away useless items and sorts them.
 */
object ModuleInventoryCleaner : ClientModule("InventoryCleaner", Category.PLAYER,
    aliases = arrayOf("InventoryManager")
) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    private val maxBlocks by int("MaximumBlocks", 512, 0..2500)
    private val maxArrows by int("MaximumArrows", 128, 0..2500)
    private val maxThrowables by int("MaximumThrowables", 64, 0..600)
    private val maxFoods by int("MaximumFoodPoints", 200, 0..2000)

    private val isGreedy by boolean("Greedy", true)

    private val offHandItem by enumChoice("OffHandItem", ItemSortChoice.SHIELD)
    private val slotItem1 by enumChoice("SlotItem-1", ItemSortChoice.WEAPON)
    private val slotItem2 by enumChoice("SlotItem-2", ItemSortChoice.BOW)
    private val slotItem3 by enumChoice("SlotItem-3", ItemSortChoice.PICKAXE)
    private val slotItem4 by enumChoice("SlotItem-4", ItemSortChoice.AXE)
    private val slotItem5 by enumChoice("SlotItem-5", ItemSortChoice.NONE)
    private val slotItem6 by enumChoice("SlotItem-6", ItemSortChoice.POTION)
    private val slotItem7 by enumChoice("SlotItem-7", ItemSortChoice.FOOD)
    private val slotItem8 by enumChoice("SlotItem-8", ItemSortChoice.BLOCK)
    private val slotItem9 by enumChoice("SlotItem-9", ItemSortChoice.BLOCK)

    val cleanupTemplateFromSettings: CleanupPlanPlacementTemplate
        get() {
            val slotTargets = hashMapOf<ItemSlot, ItemSortChoice>(
                Pair(OffHandSlot, offHandItem),
                Pair(Slots.Hotbar[0], slotItem1),
                Pair(Slots.Hotbar[1], slotItem2),
                Pair(Slots.Hotbar[2], slotItem3),
                Pair(Slots.Hotbar[3], slotItem4),
                Pair(Slots.Hotbar[4], slotItem5),
                Pair(Slots.Hotbar[5], slotItem6),
                Pair(Slots.Hotbar[6], slotItem7),
                Pair(Slots.Hotbar[7], slotItem8),
                Pair(Slots.Hotbar[8], slotItem9),
            )

            val forbiddenSlots = slotTargets
                .filterValues { it == ItemSortChoice.IGNORE }
                .keys.toHashSet()

            // Disallow tampering with armor slots since auto armor already handles them
            forbiddenSlots += Slots.Armor

            if (ModuleOffhand.isOperating()) {
                // Disallow tampering with off-hand slot when AutoTotem is active
                forbiddenSlots.add(OffHandSlot)
            }

            val forbiddenSlotsToFill = setOfNotNull(
                // Disallow tampering with off-hand slot when AutoTotem is active
                if (ModuleOffhand.isOperating()) OffHandSlot else null
            )

            val constraintProvider = AmountItemAmountConstraintProvider(
                desiredItemsPerCategory = hashMapOf(
                    Pair(ItemSortChoice.BLOCK.category!!, maxBlocks),
                    Pair(ItemSortChoice.THROWABLES.category!!, maxThrowables),
                    Pair(ItemCategory(ItemType.ARROW, 0), maxArrows),
                ),
                desiredValuePerFunction = hashMapOf(
                    ItemFunction.FOOD to maxFoods,
                    ItemFunction.WEAPON_LIKE to 1,
                ),
                desiredItemsInSpecificGroups = hashMapOf(
                    listOf(Items.EGG) to 64,
                    listOf(Items.EGG, Items.SNOWBALL) to 32,
                )
            )

            return CleanupPlanPlacementTemplate(
                slotTargets,
                itemAmountConstraintProvider = constraintProvider,
                forbiddenSlots = forbiddenSlots,
                forbiddenSlotsToFill = forbiddenSlotsToFill,
                isGreedy = isGreedy,
            )
        }

    @Suppress("unused")
    private val handleInventorySchedule = handler<ScheduleInventoryActionEvent> { event ->
        val cleanupPlan = CleanupPlanGenerator(cleanupTemplateFromSettings, findNonEmptySlotsInInventory())
            .generatePlan()

        // Step 1: Move items to the correct slots
        for (hotbarSwap in cleanupPlan.swaps) {
            check(hotbarSwap.to is HotbarItemSlot) { "Cannot swap to non-hotbar-slot" }

            event.schedule(
                inventoryConstraints,
                ClickInventoryAction.performSwap(null, hotbarSwap.from, hotbarSwap.to)
            )

            // todo: run when successful or do not care?
            cleanupPlan.remapSlots(
                hashMapOf(
                    Pair(hotbarSwap.from, hotbarSwap.to),
                    Pair(hotbarSwap.to, hotbarSwap.from),
                )
            )
        }

        // Step 2: Merge stacks
        val stacksToMerge = ItemMerge.findStacksToMerge(cleanupPlan)
        for (slot in stacksToMerge) {
            event.schedule(
                inventoryConstraints,
                ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP),
                ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP_ALL),
                ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP),
            )
        }

        // It is important that we call findItemSlotsInInventory() here again, because the inventory has changed.
        val itemsToThrowOut = findItemsToThrowOut(cleanupPlan, findNonEmptySlotsInInventory())

        for (slot in itemsToThrowOut) {
            event.schedule(
                inventoryConstraints,
                ClickInventoryAction.performThrow(screen = null, slot),
                Priority.NOT_IMPORTANT
            )
        }
    }

    fun findItemsToThrowOut(
        cleanupPlan: InventoryCleanupPlan,
        itemsInInv: List<ItemSlot>,
    ) = itemsInInv.filter { it !in cleanupPlan.usefulItems }

    private class AmountItemAmountConstraintProvider(
        val desiredItemsPerCategory: Map<ItemCategory, Int>,
        val desiredValuePerFunction: Map<ItemFunction, Int>,
        /**
         * Contains information about specific item groups constraints like `[snowball, egg] -> 32`.
         * In that example, the inventory cleaner would not start throwing out items until at least 32 items of
         * snowballs or eggs are in the inventory.
         */
        desiredItemsInSpecificGroups: Map<List<Item>, Int>
    ) : ItemAmountConstraintProvider {
        /**
         * Contains all specific item groups in which an item is.
         *
         * For these rules: `[egg, snowball] -> 32, [egg, carrot] -> 64`, this list would look like this:
         * - `egg` -> `[0, 1]`
         * - `snowball` -> `[0]`
         * - `carrot` -> `[1]`
         */
        private val itemSpecificGroupMap: Map<Item, List<SpecificItemGroup>> = run {
            desiredItemsInSpecificGroups.entries
                .flatMapIndexed { idx, (items, desiredAmount) ->
                    val group = SpecificItemGroup(id = idx, desiredAmount = desiredAmount, priority = idx)

                    items.map { it to group }
                }
                .groupBy { it.first }
                .mapValues { list -> list.value.map { it.second } }
        }

        override fun getConstraints(facet: ItemFacet): ArrayList<ItemConstraintInfo> {
            val constraints = ArrayList<ItemConstraintInfo>()

            for (group in this.itemSpecificGroupMap.getOrDefault(facet.itemStack.item, emptyList())) {
                val info = ItemConstraintInfo(
                    group = SpecificItemGroupConstraintGroup(
                        acceptableRange = group.desiredAmount..Integer.MAX_VALUE,
                        priority = group.priority,
                        groupId = group.id
                    ),
                    amountAddedByItem = facet.itemStack.count,
                    default = false
                )

                constraints.add(info)
            }

            for ((function, amountAdded) in facet.providedItemFunctions) {
                val configuredDesiredAmount = desiredValuePerFunction[function]

                val (default, desiredAmount) = if (configuredDesiredAmount != null) {
                    false to configuredDesiredAmount
                } else {
                    true to 1
                }

                val info = ItemConstraintInfo(
                    group = ItemFunctionCategoryConstraintGroup(
                        desiredAmount..Integer.MAX_VALUE,
                        1000,
                        function
                    ),
                    amountAddedByItem = amountAdded,
                    default = default
                )

                constraints.add(info)
            }

            if (facet.providedItemFunctions.isEmpty()) {
                val defaultDesiredAmount = if (facet.category.type.oneIsSufficient) 1 else Integer.MAX_VALUE
                val configuredDesiredAmount = this.desiredItemsPerCategory[facet.category]

                val (default, desiredAmount) = if (configuredDesiredAmount != null) {
                    false to configuredDesiredAmount
                } else {
                    true to defaultDesiredAmount
                }

                val info = ItemConstraintInfo(
                    group = ItemCategoryConstraintGroup(
                        desiredAmount..Integer.MAX_VALUE,
                        1000,
                        facet.category
                    ),
                    amountAddedByItem = facet.itemStack.count,
                    default = default
                )

                constraints.add(info)
            }

            return constraints
        }

        private class SpecificItemGroup(val id: Int, val desiredAmount: Int, val priority: Int)
    }

}
