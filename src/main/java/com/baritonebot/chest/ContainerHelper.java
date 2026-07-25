package com.baritonebot.chest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import com.baritonebot.util.WorldScan;

import java.util.Set;

/** Shared helpers for chest/barrel tasks. */
public final class ContainerHelper {

    private ContainerHelper() {}

    public static final Set<net.minecraft.world.level.block.Block> CONTAINERS =
        Set.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL);

    public static BlockPos findNearest(Level level, BlockPos origin, int radius) {
        return WorldScan.findNearest(level, origin, radius, CONTAINERS);
    }

    /** Items to keep on the bot when depositing "resources" (tools, armor, food, etc.). */
    public static boolean isEssential(Item item) {
        return item instanceof TieredItem       // pickaxes, axes, swords, shovels, hoes
            || item instanceof ArmorItem
            || item instanceof ShieldItem
            || item instanceof BucketItem
            || item instanceof FlintAndSteelItem
            || item.isEdible();
    }
}
