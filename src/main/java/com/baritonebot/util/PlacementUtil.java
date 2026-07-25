package com.baritonebot.util;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Places a block from inventory onto a free spot next to the player — used to
 * drop a crafting table or furnace when none is nearby.
 */
public final class PlacementUtil {

    private PlacementUtil() {}

    private static final Direction[] HORIZONTAL = {
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    /** Place {@code item} on the ground beside the player. Returns the placed position, or null. */
    public static BlockPos placeBeside(LocalPlayer player, Item item) {
        Minecraft mc = Minecraft.getInstance();
        Level level = player.level();
        if (!EquipUtil.holdItem(player, item)) return null;

        BlockPos below = player.blockPosition().below();
        for (Direction dir : HORIZONTAL) {
            BlockPos ref = below.relative(dir);       // floor block beside the player's feet
            BlockPos target = ref.above();            // the empty cell beside the player
            boolean solidTop = level.getBlockState(ref).isFaceSturdy(level, ref, Direction.UP);
            boolean targetAir = level.getBlockState(target).isAir();
            if (solidTop && targetAir) {
                Vec3 hit = new Vec3(ref.getX() + 0.5, ref.getY() + 1.0, ref.getZ() + 0.5);
                player.lookAt(EntityAnchorArgument.Anchor.EYES, hit);
                BlockHitResult result = new BlockHitResult(hit, Direction.UP, ref, false);
                mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, result);
                player.swing(InteractionHand.MAIN_HAND);
                return target;
            }
        }
        return null;
    }
}
