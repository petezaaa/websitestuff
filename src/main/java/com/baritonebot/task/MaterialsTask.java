package com.baritonebot.task;

import com.baritonebot.schematic.SchematicData;
import com.baritonebot.util.ChatUtil;
import com.baritonebot.util.InventoryUtil;
import com.baritonebot.util.Names;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Gathers/crafts/smelts the materials needed to build a schematic. Tallies the
 * schematic's blocks, subtracts what's already in the inventory, and runs a
 * best-effort acquisition plan: mine raw blocks first, then craft the rest
 * (pistons, slabs, redstone components, ...). Craftable items reuse
 * {@link CraftTask}, which pulls a crafting table and provisions simple
 * intermediates automatically.
 *
 * This is best-effort: it does not resolve arbitrarily deep dependency trees
 * (e.g. it won't go mine redstone ore to satisfy a repeater craft). Anything it
 * can't obtain is reported so you can top it up manually.
 */
public class MaterialsTask extends Task {

    private final String schemName;
    private final File file;
    private SequenceTask inner;

    public MaterialsTask(String schemName, File file) {
        this.schemName = schemName;
        this.file = file;
    }

    @Override
    public String name() {
        return "materials " + schemName;
    }

    @Override
    public void onStart(Minecraft mc) {
        SchematicData data;
        try {
            data = SchematicData.load(file);
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't read schematic \"" + schemName + "\": " + e.getMessage());
        }

        var ra = mc.level.registryAccess();
        var recipes = mc.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);

        List<Supplier<Task>> raw = new ArrayList<>();
        List<Supplier<Task>> craft = new ArrayList<>();
        List<Supplier<Task>> skip = new ArrayList<>();

        for (var entry : data.materials.entrySet()) {
            final Item item = entry.getKey();
            final int need = entry.getValue();
            final String iname = Names.itemName(item);

            boolean craftable = recipes.stream().anyMatch(r -> {
                var rs = r.getResultItem(ra);
                return !rs.isEmpty() && rs.is(item);
            });
            var blockOpt = Names.block(iname);
            boolean mineable = blockOpt.isPresent() && blockOpt.get() != Blocks.AIR && blockOpt.get().asItem() == item;

            if (mineable && !craftable) {
                raw.add(() -> mineStep(item, iname, need));
            } else if (craftable) {
                craft.add(() -> craftStep(item, iname, need));
            } else if (mineable) {
                raw.add(() -> mineStep(item, iname, need));
            } else {
                skip.add(() -> new NoOpTask("can't auto-acquire " + need + "x " + iname));
            }
        }

        ChatUtil.info("Materials for " + schemName + ": " + data.materials.size()
            + " types (" + raw.size() + " to mine, " + craft.size() + " to craft).");

        List<Supplier<Task>> all = new ArrayList<>();
        all.addAll(raw);
        all.addAll(craft);
        all.addAll(skip);

        inner = new SequenceTask("materials " + schemName, all, false);
        inner.onStart(mc);
    }

    private Task mineStep(Item item, String iname, int need) {
        int have = InventoryUtil.count(Minecraft.getInstance().player, item);
        if (have >= need) return new NoOpTask(iname + " already stocked");
        List<Block> blocks = Names.blocks(iname);
        if (blocks.isEmpty()) return new NoOpTask("skip " + iname);
        return new MineTask(iname, need - have, blocks);
    }

    private Task craftStep(Item item, String iname, int need) {
        int have = InventoryUtil.count(Minecraft.getInstance().player, item);
        if (have >= need) return new NoOpTask(iname + " already stocked");
        return new CraftTask(iname, need - have);
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        return inner.tick(mc);
    }

    @Override
    public void onStop(Minecraft mc) {
        if (inner != null) inner.onStop(mc);
    }
}
