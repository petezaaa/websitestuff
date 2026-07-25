package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import com.baritonebot.util.EquipUtil;
import com.baritonebot.util.InventoryUtil;
import com.baritonebot.util.MenuUtil;
import com.baritonebot.util.Names;
import com.baritonebot.util.PlacementUtil;
import com.baritonebot.util.WorldScan;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Crafts an item. Recipes that fit a 2x2 grid are made directly in the
 * inventory; larger recipes locate a crafting table nearby (or place one from
 * inventory), open it, and use the recipe-book autofill to craft repeatedly
 * until the requested count is reached or ingredients run out.
 *
 * This is the most environment-sensitive task (it drives container menus), so
 * behaviour is deliberately defensive and progress-based.
 */
public class CraftTask extends Task {

    private enum Phase { GO_TABLE, PLACE_TABLE, OPEN, CRAFT }

    private final String itemName;
    private final int wanted;

    private Item target;
    private CraftingRecipe recipe;
    private boolean needsTable;
    private int initialCount;

    private Phase phase;
    private int phaseTicks;
    private boolean sawPath;
    private BlockPos tablePos;

    // Craft-loop bookkeeping
    private int craftStep;
    private int craftWait;
    private int lastBefore;
    private int noProgress;
    private int cycles;

    public CraftTask(String itemName, int count) {
        this.itemName = itemName;
        this.wanted = Math.max(1, count);
    }

    @Override
    public String name() {
        return "craft " + wanted + " " + itemName;
    }

    @Override
    public void onStart(Minecraft mc) {
        LocalPlayer player = mc.player;
        target = Names.item(itemName).orElseThrow(() -> {
            var hints = Names.suggestItems(itemName);
            return new IllegalStateException("Unknown item \"" + itemName + "\"."
                + (hints.isEmpty() ? "" : " Did you mean: " + String.join(", ", hints) + "?"));
        });

        var ra = mc.level.registryAccess();
        CraftingRecipe found = null;
        for (CraftingRecipe r : mc.level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack res = r.getResultItem(ra);
            if (res.isEmpty() || !res.is(target)) continue;
            if (r.canCraftInDimensions(2, 2)) { found = r; break; } // prefer table-free
            if (found == null) found = r;
        }
        if (found == null) throw new IllegalStateException("No crafting recipe for \"" + itemName + "\".");

        recipe = found;
        needsTable = !found.canCraftInDimensions(2, 2);
        initialCount = InventoryUtil.count(player, target);

        if (!needsTable) {
            enterCraft();
            return;
        }

        // Needs a 3x3 table.
        BlockPos near = WorldScan.findNearest(player.level(), player.blockPosition(), 12, Set.of(Blocks.CRAFTING_TABLE));
        if (near != null) {
            tablePos = near;
            BaritoneHelper.gotoInteract(near);
            enter(Phase.GO_TABLE);
        } else if (InventoryUtil.count(player, Items.CRAFTING_TABLE) > 0) {
            enter(Phase.PLACE_TABLE);
        } else {
            throw new IllegalStateException("Need a crafting table: place one within 12 blocks or keep a crafting_table item.");
        }
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        phaseTicks++;
        switch (phase) {
            case GO_TABLE: return tickGoTable(player);
            case PLACE_TABLE: return tickPlaceTable(player);
            case OPEN: return tickOpen(mc, player);
            case CRAFT: return tickCraft(mc, player);
            default: return TaskResult.failed("Bad crafting state.");
        }
    }

    private TaskResult tickGoTable(LocalPlayer player) {
        if (BaritoneHelper.isPathing()) { sawPath = true; return TaskResult.running(); }
        if (!sawPath && phaseTicks < 60) return TaskResult.running();
        if (player.blockPosition().closerThan(tablePos, 5)) { enter(Phase.OPEN); return TaskResult.running(); }
        return TaskResult.failed("Couldn't reach the crafting table.");
    }

    private TaskResult tickPlaceTable(LocalPlayer player) {
        BlockPos pos = PlacementUtil.placeBeside(player, Items.CRAFTING_TABLE);
        if (pos == null) return TaskResult.failed("No room to place a crafting table.");
        tablePos = pos;
        enter(Phase.OPEN);
        return TaskResult.running();
    }

    private TaskResult tickOpen(Minecraft mc, LocalPlayer player) {
        if (mc.player.containerMenu instanceof CraftingMenu) { enterCraft(); return TaskResult.running(); }
        if (phaseTicks % 10 == 1) {
            Vec3 hit = new Vec3(tablePos.getX() + 0.5, tablePos.getY() + 1.0, tablePos.getZ() + 0.5);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, hit);
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, tablePos, false));
        }
        if (phaseTicks > 60) return TaskResult.failed("Couldn't open the crafting table.");
        return TaskResult.running();
    }

    private TaskResult tickCraft(Minecraft mc, LocalPlayer player) {
        if (++cycles > 400) return finish(player, InventoryUtil.count(player, target) - initialCount, true);
        if (craftWait > 0) { craftWait--; return TaskResult.running(); }

        AbstractContainerMenu menu = mc.player.containerMenu;
        switch (craftStep) {
            case 0: // place ingredients into the grid
                lastBefore = InventoryUtil.count(player, target);
                mc.gameMode.handlePlaceRecipe(menu.containerId, recipe, true);
                craftWait = 3; craftStep = 1;
                return TaskResult.running();
            case 1: // collect the result
                if (!menu.getSlot(0).getItem().isEmpty()) {
                    MenuUtil.quickMove(menu, 0);
                    craftWait = 3;
                }
                craftStep = 2;
                return TaskResult.running();
            default: { // evaluate progress
                int made = InventoryUtil.count(player, target) - initialCount;
                if (made >= wanted) return finish(player, made, false);
                if (InventoryUtil.count(player, target) <= lastBefore) {
                    if (++noProgress >= 2) return finish(player, made, true);
                } else {
                    noProgress = 0;
                }
                craftStep = 0;
                return TaskResult.running();
            }
        }
    }

    private TaskResult finish(LocalPlayer player, int made, boolean ranOut) {
        if (made <= 0) {
            return TaskResult.failed("Couldn't craft " + itemName + " — missing ingredients.");
        }
        String suffix = ranOut ? " (ran out of materials)" : "";
        return TaskResult.success("Crafted " + made + "x " + itemName + suffix + ".");
    }

    private void enter(Phase p) {
        phase = p;
        phaseTicks = 0;
        sawPath = false;
    }

    private void enterCraft() {
        enter(Phase.CRAFT);
        craftStep = 0;
        craftWait = 0;
        noProgress = 0;
        cycles = 0;
    }

    @Override
    public void onStop(Minecraft mc) {
        // Return any leftover ingredients from a table grid, then close it.
        if (mc.player != null && mc.player.containerMenu instanceof CraftingMenu menu) {
            for (int i = 1; i <= 9; i++) {
                if (!menu.getSlot(i).getItem().isEmpty()) MenuUtil.quickMove(menu, i);
            }
            mc.player.closeContainer();
        }
        try {
            BaritoneHelper.cancel();
        } catch (Exception ignored) {
        }
    }
}
