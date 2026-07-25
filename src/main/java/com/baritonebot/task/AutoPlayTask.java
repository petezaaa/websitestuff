package com.baritonebot.task;

import com.baritonebot.auto.AutoMode;
import com.baritonebot.auto.HomeBase;
import com.baritonebot.util.ChatUtil;
import com.baritonebot.util.Names;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Free-play "AI player" mode: for a set duration (default 24h) the bot keeps
 * itself alive (auto mode) and, whenever idle, picks a human-like activity —
 * gather wood, craft a pickaxe, set up a home base, mine, build a random
 * structure, hunt mobs, or explore — based on its current inventory plus some
 * randomness. It runs child tasks one at a time and chooses the next when each
 * finishes.
 *
 * Once it has a pickaxe and some wood it establishes a base (crafting table,
 * chest, furnace, and a bed if it has one), then periodically stashes excess
 * loot in the base chest and sleeps there at night to stay safe from mobs.
 *
 * Great for leaving it running to see what it "decides" to make over time.
 */
public class AutoPlayTask extends Task {

    private final long durationTicks;
    private final Random rng = new Random();

    private long elapsed;
    private Task child;
    private int idleCooldown;
    private long noSleepUntil;

    public AutoPlayTask(double hours) {
        this.durationTicks = (long) (Math.max(0.01, hours) * 3600 * 20);
    }

    @Override
    public String name() {
        return "free-play" + (child != null ? " (" + child.name() + ")" : "");
    }

    @Override
    public void onStart(Minecraft mc) {
        AutoMode.set(true); // eat, fight, respawn, protect tools while it plays
        ChatUtil.info("Free-play mode ON. I'll gather, craft, build, fight and explore. '/bot stop' to end.");
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        elapsed++;
        if (elapsed >= durationTicks) {
            return TaskResult.success("Free-play complete after " + hours() + ".");
        }

        if (child == null) {
            if (idleCooldown > 0) { idleCooldown--; return TaskResult.running(); }
            child = pick(mc);
            try {
                child.onStart(mc);
                ChatUtil.info("[Free-play] " + child.name());
            } catch (Exception e) {
                ChatUtil.warn("[Free-play] skipped: " + (e.getMessage() != null ? e.getMessage() : "couldn't start"));
                child = null;
                idleCooldown = 20;
            }
            return TaskResult.running();
        }

        TaskResult r;
        try {
            r = child.tick(mc);
        } catch (Exception e) {
            r = TaskResult.failed(e.getMessage());
        }
        if (!r.isDone()) return TaskResult.running();

        try {
            child.onStop(mc);
        } catch (Exception ignored) {
        }
        if (r.state == TaskResult.State.FAILED) {
            ChatUtil.warn("[Free-play] " + child.name() + ": " + r.message);
        }
        child = null;
        idleCooldown = 10; // brief pause between activities
        return TaskResult.running();
    }

    /** Choose the next activity from the current state, with some randomness. */
    private Task pick(Minecraft mc) {
        LocalPlayer player = mc.player;

        int logs = countSuffix(player, "_log");
        if (logs < 2) return new MineTask("wood", 4, Names.blocks("wood"));

        boolean hasPick = hasSuffix(player, "_pickaxe");
        if (!hasPick && (countSuffix(player, "_planks") >= 2 || logs >= 1)) {
            return new CraftTask("wooden_pickaxe", 1);
        }

        // Set up a home base once we have a pickaxe and a little wood.
        if (hasPick && !HomeBase.isEstablished() && logs >= 3) {
            return buildBase();
        }

        // Sleep through the night at the base bed to stay safe from mobs.
        if (HomeBase.bed() != null && SleepTask.isNight(mc.level) && elapsed >= noSleepUntil) {
            noSleepUntil = elapsed + 600;
            return new SleepTask();
        }

        // Stash excess loot in the base chest when the pack is full.
        if (HomeBase.isEstablished() && isInventoryFull(player)) {
            return stashAtBase();
        }

        int buildBlocks = countPlaceable(player);
        int roll = rng.nextInt(100);

        if (hasPick && buildBlocks >= 20 && roll < 45) {
            return new RandomBuildTask();
        }
        if (hasPick && roll < 70) {
            // Mining stone drops cobblestone — the bot's main building material.
            return new MineTask("stone", 24, Names.blocks("stone"));
        }
        if (roll < 84) {
            return new ExploreTask((int) player.getX(), (int) player.getZ());
        }
        if (roll < 94) {
            return new KillTask(null, 3);
        }
        return new MineTask("wood", 6, Names.blocks("wood"));
    }

    /** Craft and place a crafting table, chest, furnace (and a bed if we have one). */
    private Task buildBase() {
        List<Supplier<Task>> steps = new ArrayList<>();
        steps.add(() -> new CraftTask("crafting_table", 1));
        steps.add(() -> new CraftTask("chest", 1));
        steps.add(() -> new CraftTask("furnace", 1));
        steps.add(PlaceBaseTask::new);
        return new SequenceTask("build base", steps, false);
    }

    /** Walk home and deposit resources into the base chest. */
    private Task stashAtBase() {
        BlockPos home = HomeBase.home();
        List<Supplier<Task>> steps = new ArrayList<>();
        if (home != null) steps.add(() -> new GotoTask(home.getX(), home.getY(), home.getZ()));
        steps.add(DepositTask::resources);
        return new SequenceTask("stash at base", steps, false);
    }

    private boolean isInventoryFull(LocalPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    // --- inventory helpers ------------------------------------------------

    private int countSuffix(LocalPlayer player, String suffix) {
        Inventory inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().endsWith(suffix)) total += s.getCount();
        }
        return total;
    }

    private boolean hasSuffix(LocalPlayer player, String suffix) {
        return countSuffix(player, suffix) > 0;
    }

    private int countPlaceable(LocalPlayer player) {
        Inventory inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem) total += s.getCount();
        }
        return total;
    }

    private String hours() {
        double h = durationTicks / (3600.0 * 20.0);
        return String.format("%.1fh", h);
    }

    @Override
    public void onStop(Minecraft mc) {
        if (child != null) {
            try {
                child.onStop(mc);
            } catch (Exception ignored) {
            }
        }
    }
}
