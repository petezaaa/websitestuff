package com.baritonebot.chest;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers what has been seen inside chests (position -> item counts) and
 * mirrors it to <gameDir>/baritonebot/chestlog.txt so it survives restarts.
 */
public final class ChestLog {

    private ChestLog() {}

    private static final Map<BlockPos, Map<String, Integer>> CONTENTS = new LinkedHashMap<>();

    /** Snapshot the non-player slots of an open container at {@code pos}. */
    public static void record(BlockPos pos, AbstractContainerMenu menu) {
        Map<String, Integer> items = new LinkedHashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) continue; // player side
            ItemStack s = slot.getItem();
            if (s.isEmpty()) continue;
            String name = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
            items.merge(name, s.getCount(), Integer::sum);
        }
        CONTENTS.put(pos.immutable(), items);
        persist();
    }

    public static String describe(BlockPos pos) {
        Map<String, Integer> items = CONTENTS.get(pos);
        if (items == null) return "chest at " + pretty(pos) + ": (not logged yet)";
        if (items.isEmpty()) return "chest at " + pretty(pos) + ": empty";
        return "chest at " + pretty(pos) + ": " + join(items);
    }

    public static List<String> summary() {
        List<String> out = new ArrayList<>();
        if (CONTENTS.isEmpty()) {
            out.add("No chests logged yet. Use /bot deposit, /bot withdraw, or open one via a task.");
            return out;
        }
        for (var e : CONTENTS.entrySet()) {
            out.add(pretty(e.getKey()) + " -> " + (e.getValue().isEmpty() ? "empty" : join(e.getValue())));
        }
        return out;
    }

    private static String join(Map<String, Integer> items) {
        StringBuilder sb = new StringBuilder();
        items.forEach((k, v) -> sb.append(v).append("x ").append(k).append(", "));
        if (sb.length() >= 2) sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private static String pretty(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static void persist() {
        try {
            File dir = new File(Minecraft.getInstance().gameDirectory, "baritonebot");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            List<String> lines = new ArrayList<>();
            for (var e : CONTENTS.entrySet()) {
                lines.add(pretty(e.getKey()) + " | " + (e.getValue().isEmpty() ? "empty" : join(e.getValue())));
            }
            Files.write(new File(dir, "chestlog.txt").toPath(), lines);
        } catch (IOException ignored) {
        }
    }
}
