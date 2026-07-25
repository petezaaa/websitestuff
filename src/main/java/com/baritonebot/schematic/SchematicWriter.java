package com.baritonebot.schematic;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Block;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes a {@link RandomStructures.Structure} to a Sponge v2 {@code .schem}
 * file that Baritone can build. Structures use plain full blocks, so the
 * palette keys are just registry names (no block states to serialise).
 */
public final class SchematicWriter {

    private SchematicWriter() {}

    private static final int DATA_VERSION = 3465; // Minecraft 1.20.1

    public static File write(String name, RandomStructures.Structure s) throws IOException {
        File dir = new File(Minecraft.getInstance().gameDirectory, "baritonebot/generated");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File file = new File(dir, name + ".schem");

        // Palette: air is 0, then each distinct block.
        CompoundTag palette = new CompoundTag();
        palette.putInt("minecraft:air", 0);
        Map<Block, Integer> ids = new LinkedHashMap<>();
        int next = 1;
        for (Block b : s.blocks.values()) {
            if (!ids.containsKey(b)) {
                ids.put(b, next);
                palette.putInt(BuiltInRegistries.BLOCK.getKey(b).toString(), next);
                next++;
            }
        }

        // BlockData: varint palette indices in Y, Z, X order.
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        for (int y = 0; y < s.height; y++)
            for (int z = 0; z < s.length; z++)
                for (int x = 0; x < s.width; x++) {
                    Block b = s.blocks.get(new BlockPos(x, y, z));
                    writeVarInt(data, b == null ? 0 : ids.get(b));
                }

        CompoundTag root = new CompoundTag();
        root.putInt("Version", 2);
        root.putInt("DataVersion", DATA_VERSION);
        root.putShort("Width", (short) s.width);
        root.putShort("Height", (short) s.height);
        root.putShort("Length", (short) s.length);
        root.putIntArray("Offset", new int[]{0, 0, 0});
        root.putInt("PaletteMax", next);
        root.put("Palette", palette);
        root.putByteArray("BlockData", data.toByteArray());
        root.put("BlockEntities", new ListTag());
        CompoundTag meta = new CompoundTag();
        meta.putString("Name", s.name);
        root.put("Metadata", meta);

        try (OutputStream out = new FileOutputStream(file)) {
            NbtIo.writeCompressed(root, out);
        }
        return file;
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }
}
