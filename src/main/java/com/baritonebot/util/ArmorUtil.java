package com.baritonebot.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * Equips armor from the inventory into empty armor slots using the vanilla
 * shift-click behaviour (which routes each piece to its correct slot). Prefers
 * the highest-defense piece available for each empty slot.
 */
public final class ArmorUtil {

    private ArmorUtil() {}

    private static int armorMenuSlot(EquipmentSlot slot) {
        switch (slot) {
            case HEAD: return 5;
            case CHEST: return 6;
            case LEGS: return 7;
            case FEET: return 8;
            default: return -1;
        }
    }

    /** Equip the best available piece into each empty armor slot. Returns count equipped. */
    public static int equipBest(LocalPlayer player) {
        AbstractContainerMenu menu = player.inventoryMenu;
        int equipped = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            int dest = armorMenuSlot(slot);
            if (dest < 0 || !menu.getSlot(dest).getItem().isEmpty()) continue; // already worn
            int src = bestSourceSlot(menu, slot);
            if (src >= 0) {
                MenuUtil.quickMove(menu, src);
                equipped++;
            }
        }
        return equipped;
    }

    private static int bestSourceSlot(AbstractContainerMenu menu, EquipmentSlot slot) {
        int best = -1;
        int bestDefense = -1;
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (!(s.container instanceof Inventory)) continue;
            ItemStack stack = s.getItem();
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armor)) continue;
            if (armor.getEquipmentSlot() != slot) continue;
            if (armor.getDefense() > bestDefense) {
                bestDefense = armor.getDefense();
                best = i;
            }
        }
        return best;
    }
}
