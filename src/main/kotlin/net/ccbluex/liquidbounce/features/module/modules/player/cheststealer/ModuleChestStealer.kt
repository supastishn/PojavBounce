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
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.cheststealer

import net.ccbluex.liquidbounce.config.types.NamedChoice
<<<<<<< HEAD
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.text.Text
=======
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.isMergeable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text
import java.util.EnumSet
>>>>>>> upstream/nextgen
import kotlin.math.ceil

/**
 * ChestStealer module
 *
 * Automatically steals all items from a chest.
 */

object ModuleChestStealer : ClientModule("ChestStealer", Category.PLAYER) {

    private val inventoryConstrains = tree(InventoryConstraints())
    private val autoClose by boolean("AutoClose", true)

    private val selectionMode by enumChoice("SelectionMode", SelectionMode.DISTANCE)
    private val itemMoveMode by enumChoice("MoveMode", ItemMoveMode.QUICK_MOVE)
    private val quickSwaps by boolean("QuickSwaps", true)

<<<<<<< HEAD
    private val checkTitle by boolean("CheckTitle", true)

    init {
        tree(FeatureChestAura)
    }

    override fun disable() {
        FeatureChestAura.interactedBlocksSet.clear()
        super.disable()
    }

    val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
=======
    private val onFull by enumChoice("OnFull", OnFull.THROW)

    private enum class OnFull(override val choiceName: String) : NamedChoice {
        NONE("None"),
        THROW("Throw"),
//        PUT_BACK("PutBack"), TODO: Fix this
    }

    private object CheckScreenHandlerType : ToggleableConfigurable(this, "CheckScreenHandlerType", enabled = true) {
        private val types by registryList(
            "Types",
            hashSetOf(
                ScreenHandlerType.GENERIC_9X3, ScreenHandlerType.GENERIC_9X6, ScreenHandlerType.SHULKER_BOX,
            ),
            ValueType.SCREEN_HANDLER
        )
        private val filter by enumChoice("Filter", Filter.WHITELIST)

        fun canSteal(screen: HandledScreen<*>): Boolean {
            return !enabled || filter(runCatching { screen.screenHandler.type }.getOrNull(), types)
        }
    }

    init {
        tree(CheckScreenHandlerType)
    }

    private object CheckTitle : ToggleableConfigurable(this, "CheckTitle", enabled = true) {
        private val titles by multiEnumChoice(
            "Titles",
            EnumSet.of(
                ContainerTitle.CHEST, ContainerTitle.LARGE_CHEST,
                ContainerTitle.SHULKER_BOX, ContainerTitle.BARREL,
            ),
        )
        private val filter by enumChoice("Filter", Filter.WHITELIST)

        fun canSteal(screen: Screen): Boolean {
            if (!enabled) return true

            val titleString = screen.title.string

            return when (filter) {
                Filter.WHITELIST -> titles.any {
                    Text.translatable(it.translatableKey).string == titleString
                }
                Filter.BLACKLIST -> titles.none {
                    Text.translatable(it.translatableKey).string == titleString
                }
            }
        }
    }

    init {
        tree(CheckTitle)
    }

    @Suppress("unused")
    private enum class ContainerTitle(override val choiceName: String, val translatableKey: String) : NamedChoice {
        BARREL("Barrel", "container.barrel"),
        BEACON("Beacon", "container.beacon"),
        BLAST_FURNACE("BlastFurnace", "container.blast_furnace"),
        BREWING_STAND("BrewingStand", "container.brewing"),
        CHEST("Chest", "container.chest"),
        LARGE_CHEST("LargeChest", "container.chestDouble"),
        DISPENSER("Dispenser", "container.dispenser"),
        DROPPER("Dropper", "container.dropper"),
        ENDER_CHEST("EnderChest", "container.enderchest"),
        FURNACE("Furnace", "container.furnace"),
        HOPPER("Hopper", "container.hopper"),
        SHULKER_BOX("ShulkerBox", "container.shulkerBox"),
        SMOKER("Smoker", "container.smoker"),
    }

    init {
        tree(FeatureChestAura)
        tree(FeatureSilentScreen)
    }

    private val mainInventory = Slots.Inventory + Slots.Hotbar

    @Suppress("unused")
    private val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
>>>>>>> upstream/nextgen
        // Check if we are in a chest screen
        val screen = getChestScreen() ?: return@handler

        val cleanupPlan = createCleanupPlan(screen)
        val itemsToCollect = cleanupPlan.usefulItems.filterIsInstance<ContainerItemSlot>()

        // Quick swap items in hotbar (i.e. swords), some servers hate them
        if (quickSwaps && performQuickSwaps(event, cleanupPlan, screen) != null) {
            return@handler
        }

        val stillRequiredSpace = getStillRequiredSpace(cleanupPlan, itemsToCollect.size)
        val sortedItemsToCollect = selectionMode.processor(itemsToCollect)

<<<<<<< HEAD
        for (slot in sortedItemsToCollect) {
            if (!hasInventorySpace() && stillRequiredSpace > 0) {
                event.schedule(inventoryConstrains, throwItem(cleanupPlan, screen) ?: break)
            }

            val emptySlot = findEmptyStorageSlotsInInventory().firstOrNull() ?: break

            val actions = getActionsForMove(screen, from = slot, to = emptySlot)

            event.schedule(inventoryConstrains, actions,
                /**
                 * we prioritize item based on how important it is
                 * for example we should prioritize armor over apples
                 */
                ItemCategorization(listOf()).getItemFacets(slot).maxOf { it.category.type.allocationPriority }
            )
=======
        val targetBlacklist = hashSetOf<ItemSlot>()

        for (slot in sortedItemsToCollect) {
            val moveActions = mainInventory.findPossiblePickActions(screen, slot, targetBlacklist)

            if (moveActions != null) {
                event.schedule(
                    inventoryConstrains, moveActions,
                    /**
                     * we prioritize item based on how important it is
                     * for example we should prioritize armor over apples
                     */
                    ItemCategorization(emptyList()).getItemFacets(slot).maxOf { it.category.type.allocationPriority }
                )
            } else if (stillRequiredSpace > 0) {
                // Throw useless items
                event.schedule(
                    inventoryConstrains,
                    throwItem(cleanupPlan, screen, targetBlacklist) ?: break
                )
            }
>>>>>>> upstream/nextgen
        }

        // Check if stealing the chest was completed
        if (autoClose && sortedItemsToCollect.isEmpty()) {
<<<<<<< HEAD
            event.schedule(inventoryConstrains, CloseContainerAction(screen))
=======
            event.schedule(inventoryConstrains, InventoryAction.CloseScreen(screen))
>>>>>>> upstream/nextgen
        }
    }

    /**
<<<<<<< HEAD
     * Create a list of actions that will move the item in the slot [from] to the slot [to].
     */
    private fun getActionsForMove(
        screen: GenericContainerScreen,
        from: ContainerItemSlot,
        to: ItemSlot
    ): List<ClickInventoryAction> {
        return when (itemMoveMode) {
            ItemMoveMode.QUICK_MOVE -> listOf(ClickInventoryAction.performQuickMove(screen, from))
            ItemMoveMode.DRAG_AND_DROP -> listOf(
                ClickInventoryAction.performPickup(screen, from),
                ClickInventoryAction.performPickup(screen, to),
            )
=======
     * Calculates the mergeable count.
     */
    private fun Iterable<ItemSlot>.mergeableCountFor(itemStack: ItemStack, blacklist: Set<ItemSlot>?): Int =
        sumOf {
            val targetStack = it.itemStack
            when {
                blacklist != null && it in blacklist -> 0
                targetStack.isEmpty -> itemStack.maxCount
                targetStack.isMergeable(itemStack) -> targetStack.maxCount - targetStack.count
                else -> 0
            }
        }

    /**
     * Gets the clicks from mergeable or empty slots, or null if impossible to pick
     */
    @Suppress("CognitiveComplexMethod")
    private fun Iterable<ItemSlot>.findPossiblePickActions(
        screen: HandledScreen<*>,
        from: ItemSlot,
        targetBlacklist: MutableSet<ItemSlot>? = null,
    ): List<InventoryAction.Click>? {
        val fromStack = from.itemStack
        val remaining = mergeableCountFor(fromStack, blacklist = targetBlacklist)

        // Impossible to pick any item into inventory
        if (remaining == 0) return null

        targetBlacklist?.add(from)
        return when (itemMoveMode) {
            ItemMoveMode.QUICK_MOVE -> listOf(InventoryAction.Click.performQuickMove(screen, from))

            ItemMoveMode.DRAG_AND_DROP -> {
                // Never empty
                val targets = filterTo(ArrayDeque()) {
                    (targetBlacklist == null || it !in targetBlacklist) &&
                        (it.itemStack.isEmpty || it.itemStack.isMergeable(fromStack))
                }

                /* The remaining count after merged with [fromStack]. Negative -> fromStack has remaining */
                fun mergedRemaining(target: ItemStack) = fromStack.maxCount - fromStack.count - target.count

                buildList {
                    // Pick up
                    this += InventoryAction.Click.performPickup(screen, from)

                    val possibleSinglePut = targets.firstOrNull { mergedRemaining(it.itemStack) >= 0 }
                    if (possibleSinglePut != null) {
                        this += InventoryAction.Click.performPickup(screen, possibleSinglePut)
                        targetBlacklist?.add(possibleSinglePut)
                    } else {
                        // Now all `mergedRemaining` result of [targets] are negative
                        // Minimize click count
                        targets.sortBy { mergedRemaining(it.itemStack) }
                        var count = fromStack.count
                        while (count >= 0) {
                            val target = targets.removeFirstOrNull() ?: break
                            count += mergedRemaining(target.itemStack)
                            this += InventoryAction.Click.performPickup(screen, target)
                            targetBlacklist?.add(target)
                        }
                    }

                    if (remaining < fromStack.count) {
                        // Unable to take all, put remaining items back
                        this += InventoryAction.Click.performPickup(screen, from)
                    }
                }
            }
>>>>>>> upstream/nextgen
        }
    }

    /**
     * @return if we should wait
     */
    private fun throwItem(
        cleanupPlan: InventoryCleanupPlan,
<<<<<<< HEAD
        screen: GenericContainerScreen
    ): InventoryAction? {
        val itemsInInv = findNonEmptySlotsInInventory()
        val itemToThrowOut = ModuleInventoryCleaner.findItemsToThrowOut(cleanupPlan, itemsInInv)
            .firstOrNull { it.getIdForServer(screen) != null } ?: return null

        return ClickInventoryAction.performThrow(screen, itemToThrowOut)
=======
        screen: HandledScreen<*>,
        targetBlacklist: MutableSet<ItemSlot>,
    ): List<InventoryAction>? {
        val itemsInInv = findNonEmptySlotsInInventory()
        val itemToThrowOut = cleanupPlan.findItemsToThrowOut(itemsInInv)
            .firstOrNull { it.getIdForServer(screen) != null } ?: return null

        return when (onFull) {
            OnFull.NONE -> null
//            OnFull.PUT_BACK -> screen.getSlotsInContainer()
//                .findPossiblePickActions(screen, itemToThrowOut, targetBlacklist)
            OnFull.THROW -> {
                targetBlacklist.add(itemToThrowOut)
                listOf(InventoryAction.Click.performThrow(screen, itemToThrowOut))
            }
        }
>>>>>>> upstream/nextgen
    }

    /**
     * @param slotsToCollect amount of items we need to take
     */
    private fun getStillRequiredSpace(
        cleanupPlan: InventoryCleanupPlan,
        slotsToCollect: Int,
    ): Int {
<<<<<<< HEAD
        val freeSlotsInInv = (Slots.Inventory + Slots.Hotbar).count { it.itemStack.isEmpty }
=======
        val freeSlotsInInv = mainInventory.count { it.itemStack.isEmpty }
>>>>>>> upstream/nextgen

        val spaceGainedThroughMerge = cleanupPlan.mergeableItems.entries.sumOf { (id, slots) ->
            val slotsInChest = slots.count { it.slotType == ItemSlotType.CONTAINER }
            val totalCount = slots.sumOf { it.itemStack.count }

            val mergedStackCount = ceil(totalCount.toDouble() / id.item.maxCount.toDouble()).toInt()

            (slots.size - mergedStackCount).coerceAtMost(slotsInChest)
        }

        return (slotsToCollect - freeSlotsInInv - spaceGainedThroughMerge).coerceAtLeast(0)
    }

<<<<<<< HEAD
    private fun isScreenTitleChest(screen: GenericContainerScreen): Boolean {
        val titleString = screen.title.string

        return arrayOf(
            "container.chest", "container.chestDouble", "container.enderchest", "container.shulkerBox",
            "container.barrel"
        ).any { Text.translatable(it).string == titleString }
    }


=======
>>>>>>> upstream/nextgen
    /**
     * WARNING: Due to the remap the hotbar swaps are not valid anymore after this function.
     *
     * @return true if the chest stealer should wait for the next tick to continue. null if we didn't do anything
     */
    private fun performQuickSwaps(
        event: ScheduleInventoryActionEvent,
        cleanupPlan: InventoryCleanupPlan,
<<<<<<< HEAD
        screen: GenericContainerScreen
=======
        screen: HandledScreen<*>
>>>>>>> upstream/nextgen
    ): Boolean? {
        for (hotbarSwap in cleanupPlan.swaps) {
            // We only care about swaps from the chest to the hotbar
            if (hotbarSwap.from.slotType != ItemSlotType.CONTAINER) {
                continue
            }

            if (hotbarSwap.to !is HotbarItemSlot) {
                continue
            }

            event.schedule(
                inventoryConstrains,
<<<<<<< HEAD
                ClickInventoryAction.performSwap(screen, hotbarSwap.from, hotbarSwap.to),
=======
                InventoryAction.Click.performSwap(screen, hotbarSwap.from, hotbarSwap.to),
>>>>>>> upstream/nextgen
                /**
                 * we prioritize item based on how important it is
                 * for example we should prioritize armor over apples
                 */
                hotbarSwap.priority
            )

            // todo: hook to schedule and check if swap was successful
            cleanupPlan.remapSlots(
                hashMapOf(
                    Pair(hotbarSwap.from, hotbarSwap.to), Pair(hotbarSwap.to, hotbarSwap.from)
                )
            )

        }

        return null
    }

    /**
     * Either asks [ModuleInventoryCleaner] what to do or just takes everything.
     */
<<<<<<< HEAD
    private fun createCleanupPlan(screen: GenericContainerScreen): InventoryCleanupPlan {
        val cleanupPlan = if (!ModuleInventoryCleaner.running) {
            val usefulItems = findItemsInContainer(screen)

            InventoryCleanupPlan(usefulItems.toMutableSet(), mutableListOf(), hashMapOf())
        } else {
            val availableItems = findNonEmptySlotsInInventory() + findItemsInContainer(screen)
=======
    private fun createCleanupPlan(screen: HandledScreen<*>): InventoryCleanupPlan {
        val cleanupPlan = if (!ModuleInventoryCleaner.running) {
            val usefulItems = screen.findItemsInContainer()

            InventoryCleanupPlan(usefulItems.toHashSet(), mutableListOf(), hashMapOf())
        } else {
            val availableItems = findNonEmptySlotsInInventory() + screen.findItemsInContainer()
>>>>>>> upstream/nextgen

            CleanupPlanGenerator(ModuleInventoryCleaner.cleanupTemplateFromSettings, availableItems).generatePlan()
        }

        return cleanupPlan
    }

    @Suppress("unused")
    private enum class SelectionMode(
        override val choiceName: String,
        val processor: (List<ContainerItemSlot>) -> List<ContainerItemSlot>
    ) : NamedChoice {
<<<<<<< HEAD
        DISTANCE("Distance", {
            it.sortedBy { slot ->
                val slotId = slot.slotInContainer

                val rowA = slotId / 9
                val colA = slotId % 9

                val rowB = InventoryManager.lastClickedSlot / 9
                val colB = InventoryManager.lastClickedSlot % 9

                (colA - colB) * (colA - colB) + (rowA - rowB) * (rowA - rowB)
            }
        }),
=======
        DISTANCE("Distance", { it.sortedWith(Comparator(ContainerItemSlot::distance)) }),
>>>>>>> upstream/nextgen
        INDEX("Index", { list -> list.sortedBy { it.slotInContainer } }),
        RANDOM("Random", List<ContainerItemSlot>::shuffled),
    }

    /**
     * @return the chest screen if it is open and the title matches the chest title
     */
<<<<<<< HEAD
    private fun getChestScreen(): GenericContainerScreen? {
        val screen = mc.currentScreen

        return if (screen is GenericContainerScreen && (!checkTitle || isScreenTitleChest(screen))) {
            screen
        } else {
            null
        }
=======
    private fun getChestScreen(): HandledScreen<*>? {
        return mc.currentScreen?.takeIf { it.canBeStolen() } as HandledScreen<*>?
    }

    fun Screen.canBeStolen(): Boolean {
        return running && this is HandledScreen<*> && CheckScreenHandlerType.canSteal(this) && CheckTitle.canSteal(this)
>>>>>>> upstream/nextgen
    }

    private enum class ItemMoveMode(override val choiceName: String) : NamedChoice {
        QUICK_MOVE("QuickMove"),
        DRAG_AND_DROP("DragAndDrop"),
    }
<<<<<<< HEAD
    
    // Stub functions for missing functionality 
    val canBeStolen: Boolean = true  // Simplified - allow all items to be stolen
    
}

// Extension function for block utilities that was missing
val net.minecraft.block.BlockState.anotherChestPartDirection: net.minecraft.util.math.Direction?
    get() = null  // Stub - simplified chest detection
=======

}
>>>>>>> upstream/nextgen
