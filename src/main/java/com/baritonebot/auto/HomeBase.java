package com.baritonebot.auto;

import net.minecraft.core.BlockPos;

/**
 * Remembers the free-play bot's home base: where it is, and the positions of
 * its chest, furnace and bed. Session-scoped (reset when the game restarts).
 */
public final class HomeBase {

    private static BlockPos home;
    private static BlockPos chest;
    private static BlockPos furnace;
    private static BlockPos bed;
    private static boolean established;

    private HomeBase() {}

    public static boolean isEstablished() {
        return established;
    }

    public static void markEstablished(BlockPos where) {
        home = where;
        established = true;
    }

    public static BlockPos home() {
        return home;
    }

    public static BlockPos chest() {
        return chest;
    }

    public static BlockPos furnace() {
        return furnace;
    }

    public static BlockPos bed() {
        return bed;
    }

    public static void setChest(BlockPos p) {
        chest = p;
    }

    public static void setFurnace(BlockPos p) {
        furnace = p;
    }

    public static void setBed(BlockPos p) {
        bed = p;
    }

    public static void reset() {
        home = chest = furnace = bed = null;
        established = false;
    }
}
