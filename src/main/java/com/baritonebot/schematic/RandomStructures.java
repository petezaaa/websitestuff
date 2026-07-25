package com.baritonebot.schematic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Procedurally generates small structures out of a single block type, so the
 * free-play mode has something to "design" and build. Each structure is a
 * relative position -> block map plus its bounding size.
 */
public final class RandomStructures {

    private RandomStructures() {}

    public static final class Structure {
        public final String name;
        public final int width;
        public final int height;
        public final int length;
        public final Map<BlockPos, Block> blocks;

        Structure(String name, int width, int height, int length, Map<BlockPos, Block> blocks) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.length = length;
            this.blocks = blocks;
        }
    }

    private interface Gen {
        Structure build(Random r, Block m);
    }

    private static final Gen[] GENERATORS = {
        RandomStructures::pillar,
        RandomStructures::wall,
        RandomStructures::platform,
        RandomStructures::hollowCube,
        RandomStructures::pyramid,
        RandomStructures::hut,
        RandomStructures::staircase,
        RandomStructures::blob,
    };

    public static Structure generate(Random r, Block material) {
        return GENERATORS[r.nextInt(GENERATORS.length)].build(r, material);
    }

    private static int range(Random r, int min, int max) {
        return min + r.nextInt(max - min + 1);
    }

    private static Structure pillar(Random r, Block m) {
        int h = range(r, 3, 9);
        Map<BlockPos, Block> b = new LinkedHashMap<>();
        for (int y = 0; y < h; y++) b.put(new BlockPos(0, y, 0), m);
        return new Structure("pillar", 1, h, 1, b);
    }

    private static Structure wall(Random r, Block m) {
        int w = range(r, 3, 9);
        int h = range(r, 2, 5);
        Map<BlockPos, Block> b = new LinkedHashMap<>();
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                b.put(new BlockPos(x, y, 0), m);
        return new Structure("wall", w, h, 1, b);
    }

    private static Structure platform(Random r, Block m) {
        int w = range(r, 3, 7);
        int l = range(r, 3, 7);
        Map<BlockPos, Block> b = new LinkedHashMap<>();
        for (int x = 0; x < w; x++)
            for (int z = 0; z < l; z++)
                b.put(new BlockPos(x, 0, z), m);
        return new Structure("platform", w, 1, l, b);
    }

    private static Structure hollowCube(Random r, Block m) {
        int s = range(r, 3, 6);
        Map<BlockPos, Block> b = new LinkedHashMap<>();
        for (int x = 0; x < s; x++)
            for (int y = 0; y < s; y++)
                for (int z = 0; z < s; z++) {
                    int edges = 0;
                    if (x == 0 || x == s - 1) edges++;
                    if (y == 0 || y == s - 1) edges++;
                    if (z == 0 || z == s - 1) edges++;
                    if (edges >= 2) b.put(new BlockPos(x, y, z), m); // frame only
                }
        return new Structure("cube frame", s, s, s, b);
    }

    private static Structure pyramid(Random r, Block m) {
        int base = range(r, 3, 4) * 2 + 1; // odd 5..9 -> keep small
        base = Math.min(base, 9);
        int h = (base + 1) / 2;
        Map<BlockPos, Block> b = new LinkedHashMap<>();
        for (int y = 0; y < h; y++) {
            int inset = y;
            for (int x = inset; x < base - inset; x++)
                for (int z = inset; z < base - inset; z++)
                    b.put(new BlockPos(x, y, z), m);
        }
        return new Structure("pyramid", base, h, base, b);
    }

    private static Structure hut(Random r, Block m) {
        int w = range(r, 4, 6);
        int l = range(r, 4, 6);
        int h = range(r, 3, 4);
        Map<BlockPos, Block> b = new LinkedHashMap<>();
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++)
                for (int z = 0; z < l; z++) {
                    boolean floor = y == 0;
                    boolean roof = y == h - 1;
                    boolean wall = x == 0 || x == w - 1 || z == 0 || z == l - 1;
                    boolean door = z == 0 && x == w / 2 && (y == 1 || y == 2);
                    if ((floor || roof || wall) && !door) b.put(new BlockPos(x, y, z), m);
                }
        return new Structure("hut", w, h, l, b);
    }

    private static Structure staircase(Random r, Block m) {
        int n = range(r, 3, 7);
        Map<BlockPos, Block> b = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            for (int y = 0; y <= i; y++) b.put(new BlockPos(i, y, 0), m); // solid steps
        }
        return new Structure("staircase", n, n, 1, b);
    }

    private static Structure blob(Random r, Block m) {
        int s = range(r, 3, 5);
        Map<BlockPos, Block> b = new LinkedHashMap<>();
        for (int x = 0; x < s; x++)
            for (int y = 0; y < s; y++)
                for (int z = 0; z < s; z++)
                    if (r.nextDouble() < 0.45) b.put(new BlockPos(x, y, z), m);
        if (b.isEmpty()) b.put(new BlockPos(0, 0, 0), m);
        return new Structure("sculpture", s, s, s, b);
    }
}
