package com.baritonebot.task;

import com.baritonebot.util.Names;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Automates the classic tool progression: wood → stone → iron → diamond,
 * ending with a full set of diamond tools. Built entirely by composing the
 * existing mine/craft/smelt tasks through a {@link SequenceTask}.
 *
 * Baritone handles the actual mining/pathing (and, with itemSaver on, protects
 * Mending tools). Enable auto mode (`/bot auto on`) first so the bot eats,
 * fights off mobs, and respawns while this long job runs.
 */
public class ProgressionTask extends Task {

    private SequenceTask inner;

    @Override
    public String name() {
        return "auto-progress to diamond tools";
    }

    @Override
    public void onStart(Minecraft mc) {
        List<Supplier<Task>> steps = new ArrayList<>();

        // Wood tier
        steps.add(mine("wood", 5));
        steps.add(craft("crafting_table", 1));
        steps.add(craft("wooden_pickaxe", 1));

        // Stone tier
        steps.add(mine("cobblestone", 24));
        steps.add(craft("stone_pickaxe", 1));
        steps.add(craft("furnace", 1));

        // Iron tier
        steps.add(mine("iron_ore", 6));
        steps.add(smelt("raw_iron", 6));
        steps.add(craft("iron_pickaxe", 1));

        // Diamond tier — pickaxe first so at least that is secured.
        steps.add(mine("diamond_ore", 3));
        steps.add(craft("diamond_pickaxe", 1));

        // Full diamond set (best-effort; stops here if diamonds run short).
        steps.add(mine("diamond_ore", 9));
        steps.add(craft("diamond_sword", 1));
        steps.add(craft("diamond_axe", 1));
        steps.add(craft("diamond_shovel", 1));
        steps.add(craft("diamond_hoe", 1));

        inner = new SequenceTask("progress", steps, true);
        inner.onStart(mc);
    }

    private Supplier<Task> mine(String block, int count) {
        return () -> new MineTask(block, count, Names.blocks(block));
    }

    private Supplier<Task> craft(String item, int count) {
        return () -> new CraftTask(item, count);
    }

    private Supplier<Task> smelt(String item, int count) {
        return () -> new SmeltTask(item, count, null);
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
