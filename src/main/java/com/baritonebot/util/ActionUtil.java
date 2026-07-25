package com.baritonebot.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

/**
 * One-shot inventory actions that don't need a task (dropping, equipping).
 */
public final class ActionUtil {

    private ActionUtil() {}

    /** Drop every stack of {@code item}, or the whole inventory if item is null. */
    public static int drop(LocalPlayer player, Item item) {
        var menu = player.inventoryMenu;
        int dropped = 0;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!(slot.container instanceof Inventory)) continue;
            var stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (item == null || stack.is(item)) {
                dropped += stack.getCount();
                MenuUtil.throwSlot(menu, i);
            }
        }
        return dropped;
    }

    public static boolean equip(LocalPlayer player, Item item) {
        return EquipUtil.holdItem(player, item);
    }
}
