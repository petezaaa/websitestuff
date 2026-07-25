package com.baritonebot.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Equips the best available melee weapon (sword preferred, then axe) into the
 * main hand. Shared by combat tasks and the auto-defend guardian.
 */
public final class WeaponUtil {

    private WeaponUtil() {}

    private static final String[] WEAPONS = {
        "netherite_sword", "diamond_sword", "iron_sword", "stone_sword", "golden_sword", "wooden_sword",
        "netherite_axe", "diamond_axe", "iron_axe", "stone_axe", "golden_axe", "wooden_axe"
    };

    /** Hold the best melee weapon we own. Returns true if one is (already) held. */
    public static boolean equipBestMelee(LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            String heldName = BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();
            if (heldName.endsWith("_sword")) return true; // already best category
        }
        for (String w : WEAPONS) {
            var opt = Names.item(w);
            if (opt.isPresent() && InventoryUtil.count(player, opt.get()) > 0) {
                EquipUtil.holdItem(player, opt.get());
                return true;
            }
        }
        return false;
    }
}
