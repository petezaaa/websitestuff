package com.baritonebot.integration;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;

import java.io.File;
import java.util.function.Predicate;

/**
 * Thin, single-responsibility wrapper around the Baritone API. Every Baritone
 * call the mod makes goes through here, so if a Baritone build exposes a
 * slightly different signature you only have one file to adjust.
 *
 * Baritone provides the heavy lifting we don't want to reimplement:
 * pathfinding, block finding + mining, schematic building, following and
 * exploring. The custom tasks (combat, crafting, smelting) sit on top.
 */
public final class BaritoneHelper {

    private BaritoneHelper() {}

    private static boolean configured = false;

    public static IBaritone baritone() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }

    /**
     * Apply our preferred Baritone settings once. Notably enables itemSaver so
     * tools are dropped before breaking — set to 2 durability, this protects
     * Mending tools by never using them below 2 points. Also lets Baritone pull
     * required items into the hotbar for mining/building.
     */
    public static void configureDefaults() {
        if (configured) return;
        try {
            Settings s = BaritoneAPI.getSettings();
            s.itemSaver.value = true;
            s.itemSaverThreshold.value = 2;
            s.allowInventory.value = true;
            configured = true;
        } catch (Throwable ignored) {
            // Baritone may be absent in a dev launch without it installed.
        }
    }

    /** Set the durability at which tools stop being used (Mending protection). */
    public static void setItemSaverThreshold(int durability) {
        try {
            Settings s = BaritoneAPI.getSettings();
            s.itemSaver.value = durability > 0;
            s.itemSaverThreshold.value = Math.max(0, durability);
        } catch (Throwable ignored) {
        }
    }

    // --- Movement ---------------------------------------------------------

    public static void gotoBlock(int x, int y, int z) {
        baritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(x, y, z));
    }

    public static void gotoNear(BlockPos pos, int range) {
        baritone().getCustomGoalProcess().setGoalAndPath(new GoalNear(pos, range));
    }

    public static void gotoXZ(int x, int z) {
        baritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(x, z));
    }

    /** Path until adjacent to a specific block (so it can be interacted with). */
    public static void gotoInteract(BlockPos pos) {
        baritone().getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(pos));
    }

    public static boolean isPathing() {
        return baritone().getPathingBehavior().isPathing()
            || baritone().getCustomGoalProcess().isActive();
    }

    // --- Mining -----------------------------------------------------------

    public static void mine(int quantity, Block... blocks) {
        baritone().getMineProcess().mine(quantity, blocks);
    }

    public static boolean isMining() {
        return baritone().getMineProcess().isActive();
    }

    // --- Building ---------------------------------------------------------

    public static boolean build(String name, File schematic, Vec3i origin) {
        return baritone().getBuilderProcess().build(name, schematic, origin);
    }

    public static boolean isBuilding() {
        return baritone().getBuilderProcess().isActive();
    }

    // --- Following / exploring -------------------------------------------

    public static void follow(Predicate<Entity> filter) {
        baritone().getFollowProcess().follow(filter);
    }

    public static void explore(int centerX, int centerZ) {
        baritone().getExploreProcess().explore(centerX, centerZ);
    }

    // --- Control ----------------------------------------------------------

    /** Cancel every active Baritone process and clear the current goal. */
    public static void cancel() {
        IBaritone b = baritone();
        b.getPathingBehavior().cancelEverything();
        b.getCustomGoalProcess().setGoal(null);
    }
}
