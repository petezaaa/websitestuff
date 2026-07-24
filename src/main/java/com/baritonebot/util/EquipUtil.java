package com.baritonebot.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;

/**
 * Ensures a given item is held in the main hand, swapping it into the hotbar
 * from the main inventory if necessary.
 */
public final class EquipUtil {

    private EquipUtil() {}

    public static boolean holdItem(LocalPlayer player, Item item) {
        Inventory inv = player.getInventory();

        // Already in the hotbar? Just select it.
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(item)) {
                inv.selected = i;
                return true;
            }
        }

        // Otherwise find it in the main inventory (indices 9..35).
        int found = -1;
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i).is(item)) {
                found = i;
                break;
            }
        }
        if (found < 0) return false;

        // Swap it into the currently selected hotbar slot. In InventoryMenu the
        // main-inventory indices 9..35 map 1:1 to menu slot indices 9..35.
        Minecraft mc = Minecraft.getInstance();
        int hotbar = inv.selected;
        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, found, hotbar, ClickType.SWAP, player);
        inv.selected = hotbar;
        return true;
    }
}
