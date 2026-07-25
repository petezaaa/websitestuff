package com.baritonebot.task;

import com.baritonebot.schematic.SchematicData;
import com.baritonebot.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compares the world against a schematic and reports mismatches — including
 * redstone orientation (piston/repeater/comparator facing, axis, delay, mode),
 * so you can confirm the build's redstone is placed correctly. Runs
 * incrementally across ticks to avoid lag on large builds.
 */
public class VerifyTask extends Task {

    // Orientation-relevant properties we care about for redstone/directional blocks.
    private static final Property<?>[] ORIENTATION = {
        BlockStateProperties.FACING,
        BlockStateProperties.HORIZONTAL_FACING,
        BlockStateProperties.AXIS,
        BlockStateProperties.ROTATION_16,
        BlockStateProperties.FACE,
        BlockStateProperties.DELAY,
        BlockStateProperties.MODE_COMPARATOR,
    };

    private final String schemName;
    private final File file;
    private final BlockPos origin;

    private List<Map.Entry<BlockPos, BlockState>> entries;
    private int idx;
    private int correct;
    private int wrongBlock;
    private int wrongOrient;
    private int missing;
    private final List<String> notes = new ArrayList<>();

    public VerifyTask(String schemName, File file, BlockPos origin) {
        this.schemName = schemName;
        this.file = file;
        this.origin = origin;
    }

    @Override
    public String name() {
        return "verify " + schemName;
    }

    @Override
    public void onStart(Minecraft mc) {
        SchematicData data;
        try {
            data = SchematicData.load(file);
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't read schematic \"" + schemName + "\": " + e.getMessage());
        }
        entries = new ArrayList<>(data.blocks.entrySet());
        ChatUtil.info("Verifying " + data.width + "x" + data.height + "x" + data.length
            + " (" + entries.size() + " blocks) from " + origin.toShortString() + "...");
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        int budget = 1024;
        while (idx < entries.size() && budget-- > 0) {
            Map.Entry<BlockPos, BlockState> e = entries.get(idx++);
            BlockPos world = origin.offset(e.getKey());
            BlockState expected = e.getValue();
            BlockState actual = mc.level.getBlockState(world);

            if (actual.isAir()) {
                missing++;
                note(world, "missing " + name(expected));
            } else if (actual.getBlock() != expected.getBlock()) {
                wrongBlock++;
                note(world, "expected " + name(expected) + " but found " + name(actual));
            } else if (!orientationMatches(expected, actual)) {
                wrongOrient++;
                note(world, "orientation off: " + expected.toString());
            } else {
                correct++;
            }
        }

        if (idx < entries.size()) return TaskResult.running();

        for (String n : notes) ChatUtil.warn(n);
        String summary = "Verify " + schemName + ": " + correct + " ok, "
            + wrongBlock + " wrong block, " + wrongOrient + " wrong orientation, " + missing + " missing.";
        return TaskResult.success(summary);
    }

    private boolean orientationMatches(BlockState expected, BlockState actual) {
        for (Property<?> p : ORIENTATION) {
            if (!expected.hasProperty(p)) continue;
            if (!actual.hasProperty(p) || !expected.getValue(p).equals(actual.getValue(p))) {
                return false;
            }
        }
        return true;
    }

    private void note(BlockPos pos, String msg) {
        if (notes.size() < 25) notes.add(pos.toShortString() + ": " + msg);
    }

    private static String name(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
    }
}
