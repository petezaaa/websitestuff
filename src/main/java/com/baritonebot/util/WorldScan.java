package com.baritonebot.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Set;

/** Small local block search around a position. */
public final class WorldScan {

    private WorldScan() {}

    /** Nearest block position matching any of {@code blocks} within {@code radius}, or null. */
    public static BlockPos findNearest(Level level, BlockPos origin, int radius, Set<Block> blocks) {
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!blocks.contains(level.getBlockState(cursor).getBlock())) continue;
                    double d = cursor.distSqr(origin);
                    if (d < bestSq) {
                        bestSq = d;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }
}
