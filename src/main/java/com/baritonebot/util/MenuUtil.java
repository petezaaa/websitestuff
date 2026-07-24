package com.baritonebot.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Helpers for manipulating the currently open container menu (crafting table,
 * furnace, chest, or the player inventory). All clicks go through
 * {@code MultiPlayerGameMode} so the server stays authoritative.
 */
public final class MenuUtil {

    private MenuUtil() {}

    /** Shift-click (quick-move) the given menu slot. */
    public static void quickMove(AbstractContainerMenu menu, int slotIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(menu.containerId, slotIndex, 0, ClickType.QUICK_MOVE, mc.player);
    }

    /** Drop the entire stack in the given menu slot onto the ground. */
    public static void throwSlot(AbstractContainerMenu menu, int slotIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return;
        // button 1 = drop whole stack.
        mc.gameMode.handleInventoryMouseClick(menu.containerId, slotIndex, 1, ClickType.THROW, mc.player);
    }

    /**
     * Find a menu slot backed by the player's inventory that holds {@code item}.
     * Returns the menu slot index, or -1. Useful for quick-moving a specific
     * item from the player's inventory into a furnace/crafting grid.
     */
    public static int findPlayerInventorySlot(AbstractContainerMenu menu, Item item) {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!(slot.container instanceof Inventory)) continue;
            ItemStack s = slot.getItem();
            if (!s.isEmpty() && s.is(item)) return i;
        }
        return -1;
    }

    /** Total quantity of an item currently visible in the menu's player-inventory slots. */
    public static int countInPlayerInventory(AbstractContainerMenu menu, Item item) {
        int total = 0;
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof Inventory)) continue;
            ItemStack s = slot.getItem();
            if (!s.isEmpty() && s.is(item)) total += s.getCount();
        }
        return total;
    }
}
