package com.baritonebot.task;

import com.baritonebot.auto.HomeBase;
import com.baritonebot.util.WorldScan;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds a house schematic with Baritone and then registers its chest, furnace
 * and bed as the {@link HomeBase}, so the free-play systems (stashing loot,
 * sleeping) will use the freshly-built house. You supply the schematic (a couple
 * of ready-made houses ship in the schematics/ folder).
 */
public class BuildBaseTask extends Task {

    private final String schemName;
    private final File file;
    private final BlockPos origin;

    private BuildTask child;
    private boolean built;

    public BuildBaseTask(String schemName, File file, BlockPos origin) {
        this.schemName = schemName;
        this.file = file;
        this.origin = origin;
    }

    @Override
    public String name() {
        return "build base " + schemName;
    }

    @Override
    public void onStart(Minecraft mc) {
        child = new BuildTask(schemName, file, origin);
        child.onStart(mc); // may throw if Baritone rejects the schematic
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        if (!built) {
            TaskResult r;
            try {
                r = child.tick(mc);
            } catch (Exception e) {
                r = TaskResult.failed(e.getMessage());
            }
            if (!r.isDone()) return TaskResult.running();
            child.onStop(mc);
            if (r.state == TaskResult.State.FAILED) return r;
            built = true;
            return TaskResult.running(); // register on the next tick
        }

        Level level = mc.level;
        BlockPos chest = WorldScan.findNearest(level, origin, 20,
            Set.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL));
        BlockPos furnace = WorldScan.findNearest(level, origin, 20, Set.of(Blocks.FURNACE));
        BlockPos bed = WorldScan.findNearest(level, origin, 20, Set.of(Blocks.RED_BED));

        List<String> parts = new ArrayList<>();
        if (chest != null) { HomeBase.setChest(chest); parts.add("chest"); }
        if (furnace != null) { HomeBase.setFurnace(furnace); parts.add("furnace"); }
        if (bed != null) { HomeBase.setBed(bed); parts.add("bed"); }
        HomeBase.markEstablished(origin);

        return TaskResult.success("Base \"" + schemName + "\" built and registered"
            + (parts.isEmpty() ? " (no furniture found — was it fully built?)." : " (" + String.join(", ", parts) + ")."));
    }

    @Override
    public void onStop(Minecraft mc) {
        if (!built && child != null) {
            try {
                child.onStop(mc);
            } catch (Exception ignored) {
            }
        }
    }
}
