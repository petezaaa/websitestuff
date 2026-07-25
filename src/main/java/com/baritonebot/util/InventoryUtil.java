package com.baritonebot.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only inventory helpers (counting / summarising). Item movement lives in
 * {@link MenuUtil} because it must go through the open container menu.
 */
public final class InventoryUtil {

    private InventoryUtil() {}

    public static int count(Player player, Item item) {
        int total = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(item)) total += s.getCount();
        }
        return total;
    }

    public static boolean has(Player player, Item item, int min) {
        return count(player, item) >= min;
    }

    public static String summary(Player player) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            String name = Names.itemName(s.getItem());
            counts.merge(name, s.getCount(), Integer::sum);
        }
        if (counts.isEmpty()) return "Inventory is empty.";
        StringBuilder sb = new StringBuilder("Inventory: ");
        counts.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .forEach(e -> sb.append(e.getValue()).append("x ").append(e.getKey()).append(", "));
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    /** First hotbar/inventory slot index (0..35) holding the item, or -1. */
    public static int findSlot(Player player, Item item) {
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(item)) return i;
        }
        return -1;
    }
}
