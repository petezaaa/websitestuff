package com.baritonebot.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves human-friendly names ("oak log", "iron_ingot", "diamond ore") to
 * Minecraft registry objects, with a few friendly aliases for block groups.
 */
public final class Names {

    private Names() {}

    /** Alias groups so "wood"/"logs"/"stone" match every relevant variant. */
    private static final Map<String, String[]> BLOCK_ALIASES = Map.of(
        "wood", new String[]{"oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log"},
        "log", new String[]{"oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log"},
        "logs", new String[]{"oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log"},
        "stone", new String[]{"stone", "cobblestone", "deepslate", "cobbled_deepslate"},
        "coal_ore", new String[]{"coal_ore", "deepslate_coal_ore"},
        "iron_ore", new String[]{"iron_ore", "deepslate_iron_ore"},
        "gold_ore", new String[]{"gold_ore", "deepslate_gold_ore"},
        "diamond_ore", new String[]{"diamond_ore", "deepslate_diamond_ore"},
        "copper_ore", new String[]{"copper_ore", "deepslate_copper_ore"}
    );

    public static String normalize(String raw) {
        String s = raw.trim().toLowerCase();
        if (s.startsWith("minecraft:")) s = s.substring("minecraft:".length());
        return s.replace(' ', '_');
    }

    private static ResourceLocation id(String name) {
        String n = normalize(name);
        return n.contains(":") ? new ResourceLocation(n) : new ResourceLocation("minecraft", n);
    }

    /** Resolve one or more Blocks for a name (expanding aliases). Empty if unknown. */
    public static List<Block> blocks(String name) {
        Set<Block> out = new LinkedHashSet<>();
        String key = normalize(name);
        String[] group = BLOCK_ALIASES.get(key);
        String[] candidates = group != null ? group : new String[]{key};
        for (String c : candidates) {
            Optional<Block> b = BuiltInRegistries.BLOCK.getOptional(id(c));
            b.ifPresent(out::add);
        }
        return new ArrayList<>(out);
    }

    public static Optional<Block> block(String name) {
        return BuiltInRegistries.BLOCK.getOptional(id(name));
    }

    public static Optional<Item> item(String name) {
        return BuiltInRegistries.ITEM.getOptional(id(name));
    }

    public static String itemName(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    /** Up to five registry names that loosely resemble the query (for error hints). */
    public static List<String> suggestItems(String name) {
        String key = normalize(name);
        List<String> hits = new ArrayList<>();
        for (ResourceLocation rl : BuiltInRegistries.ITEM.keySet()) {
            String path = rl.getPath();
            if (path.contains(key) || key.contains(path)) {
                hits.add(path);
                if (hits.size() >= 5) break;
            }
        }
        return hits;
    }
}
