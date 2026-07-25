package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
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
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Smelts items in a furnace: finds or places one, loads the input and fuel,
 * waits for results, and collects the output. Fuel is chosen from inventory
 * (or given explicitly) and topped up if it runs out mid-smelt.
 */
public class SmeltTask extends Task {

    private enum Phase { GO_FURNACE, PLACE_FURNACE, OPEN, LOAD, WAIT }

    private static final String[] FUEL_PRIORITY = {
        "coal", "charcoal", "coal_block", "blaze_rod",
        "oak_planks", "spruce_planks", "birch_planks", "jungle_planks",
        "acacia_planks", "dark_oak_planks",
        "oak_log", "spruce_log", "birch_log", "bamboo"
    };

    private final String inputName;
    private final int wanted;
    private final String fuelName; // may be null

    private Item input;
    private Item fuel;
    private int target;
    private int collected;

    private Phase phase;
    private int phaseTicks;
    private boolean sawPath;
    private BlockPos furnacePos;
    private int loadStep;
    private int loadWait;
    private int idleChecks;
    private int deadline;

    public SmeltTask(String inputName, int count, String fuelName) {
        this.inputName = inputName;
        this.wanted = Math.max(1, count);
        this.fuelName = fuelName;
    }

    @Override
    public String name() {
        return "smelt " + wanted + " " + inputName;
    }

    @Override
    public void onStart(Minecraft mc) {
        LocalPlayer player = mc.player;
        input = Names.item(inputName).orElseThrow(() ->
            new IllegalStateException("Unknown item \"" + inputName + "\"."));
        int have = InventoryUtil.count(player, input);
        if (have == 0) throw new IllegalStateException("I don't have any \"" + inputName + "\" to smelt.");
        target = Math.min(wanted, have);

        fuel = chooseFuel(player);
        if (fuel == null) throw new IllegalStateException("No fuel available (coal, charcoal, planks, ...).");

        deadline = target * 220 + 200;

        BlockPos near = WorldScan.findNearest(player.level(), player.blockPosition(), 12, Set.of(Blocks.FURNACE));
        if (near != null) {
            furnacePos = near;
            BaritoneHelper.gotoInteract(near);
            enter(Phase.GO_FURNACE);
        } else if (InventoryUtil.count(player, Items.FURNACE) > 0) {
            enter(Phase.PLACE_FURNACE);
        } else {
            throw new IllegalStateException("Need a furnace: place one within 12 blocks or keep a furnace item.");
        }
    }

    private Item chooseFuel(LocalPlayer player) {
        if (fuelName != null) {
            Item it = Names.item(fuelName).orElseThrow(() ->
                new IllegalStateException("Unknown fuel \"" + fuelName + "\"."));
            if (InventoryUtil.count(player, it) == 0) {
                throw new IllegalStateException("I don't have \"" + fuelName + "\" for fuel.");
            }
            return it;
        }
        for (String n : FUEL_PRIORITY) {
            var opt = Names.item(n);
            if (opt.isPresent() && InventoryUtil.count(player, opt.get()) > 0) return opt.get();
        }
        return null;
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        phaseTicks++;
        switch (phase) {
            case GO_FURNACE: return tickGoFurnace(player);
            case PLACE_FURNACE: return tickPlaceFurnace(player);
            case OPEN: return tickOpen(mc, player);
            case LOAD: return tickLoad(mc);
            case WAIT: return tickWait(mc);
            default: return TaskResult.failed("Bad smelting state.");
        }
    }

    private TaskResult tickGoFurnace(LocalPlayer player) {
        if (BaritoneHelper.isPathing()) { sawPath = true; return TaskResult.running(); }
        if (!sawPath && phaseTicks < 60) return TaskResult.running();
        if (player.blockPosition().closerThan(furnacePos, 5)) { enter(Phase.OPEN); return TaskResult.running(); }
        return TaskResult.failed("Couldn't reach the furnace.");
    }

    private TaskResult tickPlaceFurnace(LocalPlayer player) {
        BlockPos pos = PlacementUtil.placeBeside(player, Items.FURNACE);
        if (pos == null) return TaskResult.failed("No room to place a furnace.");
        furnacePos = pos;
        enter(Phase.OPEN);
        return TaskResult.running();
    }

    private TaskResult tickOpen(Minecraft mc, LocalPlayer player) {
        if (mc.player.containerMenu instanceof AbstractFurnaceMenu) {
            enter(Phase.LOAD);
            loadStep = 0;
            loadWait = 0;
            return TaskResult.running();
        }
        if (phaseTicks % 10 == 1) {
            Vec3 hit = new Vec3(furnacePos.getX() + 0.5, furnacePos.getY() + 1.0, furnacePos.getZ() + 0.5);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, hit);
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, furnacePos, false));
        }
        if (phaseTicks > 60) return TaskResult.failed("Couldn't open the furnace.");
        return TaskResult.running();
    }

    private TaskResult tickLoad(Minecraft mc) {
        if (loadWait > 0) { loadWait--; return TaskResult.running(); }
        AbstractContainerMenu menu = mc.player.containerMenu;
        switch (loadStep) {
            case 0: { // input -> smelt slot
                int slot = MenuUtil.findPlayerInventorySlot(menu, input);
                if (slot >= 0) MenuUtil.quickMove(menu, slot);
                loadWait = 3; loadStep = 1;
                return TaskResult.running();
            }
            case 1: { // fuel -> fuel slot
                int slot = MenuUtil.findPlayerInventorySlot(menu, fuel);
                if (slot >= 0) MenuUtil.quickMove(menu, slot);
                loadWait = 3; loadStep = 2;
                return TaskResult.running();
            }
            default:
                enter(Phase.WAIT);
                return TaskResult.running();
        }
    }

    private TaskResult tickWait(Minecraft mc) {
        if (phaseTicks % 10 != 0) return TaskResult.running();
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (!(menu instanceof AbstractFurnaceMenu)) {
            return collected > 0 ? done() : TaskResult.failed("Furnace closed unexpectedly.");
        }

        // Collect finished output.
        var out = menu.getSlot(2).getItem();
        if (!out.isEmpty()) {
            collected += out.getCount();
            MenuUtil.quickMove(menu, 2);
        }
        if (collected >= target) return done();

        // Top up fuel if it ran out with input still waiting.
        if (menu.getSlot(1).getItem().isEmpty() && !menu.getSlot(0).getItem().isEmpty()) {
            int slot = MenuUtil.findPlayerInventorySlot(menu, fuel);
            if (slot >= 0) MenuUtil.quickMove(menu, slot);
        }

        // Nothing cooking and nothing queued for a while -> finish.
        boolean idle = menu.getSlot(0).getItem().isEmpty() && menu.getSlot(2).getItem().isEmpty();
        idleChecks = idle ? idleChecks + 1 : 0;
        if (idleChecks > 10 || phaseTicks > deadline) return done();
        return TaskResult.running();
    }

    private TaskResult done() {
        return collected > 0
            ? TaskResult.success("Smelted " + collected + " of " + inputName + ".")
            : TaskResult.failed("Nothing was smelted (check fuel and input).");
    }

    private void enter(Phase p) {
        phase = p;
        phaseTicks = 0;
        sawPath = false;
        idleChecks = 0;
    }

    @Override
    public void onStop(Minecraft mc) {
        // Recover input/fuel/output and close the furnace.
        if (mc.player != null && mc.player.containerMenu instanceof AbstractFurnaceMenu menu) {
            for (int i = 0; i <= 2; i++) {
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
