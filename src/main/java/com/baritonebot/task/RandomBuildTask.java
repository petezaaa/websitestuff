package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import com.baritonebot.schematic.RandomStructures;
import com.baritonebot.schematic.SchematicWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Picks the most plentiful building block in the inventory, generates a random
 * small structure from it, writes it to a temporary schematic and builds it
 * with Baritone. This is what makes the free-play mode "design and build" things.
 */
public class RandomBuildTask extends DelegatedTask {

    private final Random rng = new Random();
    private String label = "structure";

    public RandomBuildTask() {
        super(120);
    }

    @Override
    public String name() {
        return "build a random " + label;
    }

    @Override
    protected void begin(Minecraft mc) {
        LocalPlayer player = mc.player;
        Block material = bestBuildMaterial(player);
        if (material == null) {
            throw new IllegalStateException("No building blocks to build with.");
        }

        RandomStructures.Structure structure = RandomStructures.generate(rng, material);
        label = structure.name;

        File file;
        try {
            file = SchematicWriter.write("auto_" + System.currentTimeMillis(), structure);
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't write the generated schematic: " + e.getMessage());
        }

        // Build a couple of blocks away so it isn't placed on top of the bot.
        BlockPos origin = player.blockPosition().offset(2, 0, 2);
        if (!BaritoneHelper.build("autobuild", file, origin)) {
            throw new IllegalStateException("Baritone wouldn't start the build.");
        }
    }

    @Override
    protected boolean active() {
        return BaritoneHelper.isBuilding();
    }

    @Override
    protected String doneMessage() {
        return "Built a " + label + ".";
    }

    /** The block the bot has the most of (that is a placeable full-ish block). */
    private Block bestBuildMaterial(LocalPlayer player) {
        Inventory inv = player.getInventory();
        Map<Block, Integer> counts = new HashMap<>();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !(s.getItem() instanceof BlockItem bi)) continue;
            Block b = bi.getBlock();
            if (b == Blocks.AIR) continue;
            counts.merge(b, s.getCount(), Integer::sum);
        }
        Block best = null;
        int bestCount = 7; // need a small stash before it's worth building
        for (var e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }
}
