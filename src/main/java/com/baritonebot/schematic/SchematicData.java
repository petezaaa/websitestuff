package com.baritonebot.schematic;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import java.io.File;

/**
 * Parses a Sponge {@code .schem} file (as WorldEdit / Baritone use) into block
 * states and a material tally, using only vanilla NBT + BlockStateParser so the
 * full block state (including piston/repeater/comparator facing) is preserved.
 */
public final class SchematicData {

    public final int width;
    public final int height;
    public final int length;
    /** Relative position -> block state (air omitted). */
    public final Map<BlockPos, BlockState> blocks;
    /** Item -> count required to place every non-air block. */
    public final Map<Item, Integer> materials;

    private SchematicData(int w, int h, int l, Map<BlockPos, BlockState> blocks, Map<Item, Integer> materials) {
        this.width = w;
        this.height = h;
        this.length = l;
        this.blocks = blocks;
        this.materials = materials;
    }

    public static SchematicData load(File file) throws IOException {
        CompoundTag root;
        try (InputStream in = new FileInputStream(file)) {
            root = NbtIo.readCompressed(in);
        }
        CompoundTag schem = root.contains("Schematic", 10) ? root.getCompound("Schematic") : root;

        int w = schem.getShort("Width");
        int h = schem.getShort("Height");
        int l = schem.getShort("Length");

        // Build palette-index -> BlockState.
        HolderLookup<net.minecraft.world.level.block.Block> lookup = BuiltInRegistries.BLOCK.asLookup();
        CompoundTag palette = schem.getCompound("Palette");
        Map<Integer, BlockState> byId = new HashMap<>();
        for (String key : palette.getAllKeys()) {
            int id = palette.getInt(key);
            BlockState state;
            try {
                state = BlockStateParser.parseForBlock(lookup, key, false).blockState();
            } catch (Exception e) {
                state = Blocks.AIR.defaultBlockState();
            }
            byId.put(id, state);
        }

        byte[] data = schem.getByteArray("BlockData");
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        Map<Item, Integer> materials = new LinkedHashMap<>();

        int cursor = 0;
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < l; z++) {
                for (int x = 0; x < w; x++) {
                    // varint decode
                    int value = 0;
                    int shift = 0;
                    byte b;
                    do {
                        b = data[cursor++];
                        value |= (b & 0x7F) << shift;
                        shift += 7;
                    } while ((b & 0x80) != 0);

                    BlockState state = byId.getOrDefault(value, Blocks.AIR.defaultBlockState());
                    if (state.isAir()) continue;
                    blocks.put(new BlockPos(x, y, z), state);
                    Item item = state.getBlock().asItem();
                    if (item != Items.AIR) materials.merge(item, 1, Integer::sum);
                }
            }
        }
        return new SchematicData(w, h, l, blocks, materials);
    }
}
