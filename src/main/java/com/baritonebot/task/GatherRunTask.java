package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import com.baritonebot.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Mines a target block with Baritone (which stops once it has the requested
 * amount), but watches the inventory: when it fills up, it pauses mining, runs
 * to the nearest chest to deposit resources, then resumes — the classic
 * mine → return → deposit → repeat loop.
 */
public class GatherRunTask extends Task {

    private enum Phase { MINE, DEPOSIT }

    private final String label;
    private final int total;
    private final Block[] blocks;

    private Phase phase;
    private int phaseTicks;
    private boolean sawMining;
    private DepositTask deposit;

    public GatherRunTask(String label, int total, List<Block> blocks) {
        this.label = label;
        this.total = Math.max(1, total);
        this.blocks = blocks.toArray(new Block[0]);
    }

    @Override
    public String name() {
        return "gather " + total + " " + label;
    }

    @Override
    public void onStart(Minecraft mc) {
        startMining();
    }

    private void startMining() {
        BaritoneHelper.mine(total, blocks);
        phase = Phase.MINE;
        phaseTicks = 0;
        sawMining = false;
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        phaseTicks++;
        LocalPlayer player = mc.player;
        if (phase == Phase.MINE) {
            boolean mining = safeMining();
            if (mining) sawMining = true;

            if (!mining && (sawMining || phaseTicks > 60)) {
                return TaskResult.success("Gathered " + label + " (mining finished).");
            }

            if (inventoryFull(player)) {
                BaritoneHelper.cancel();
                try {
                    deposit = DepositTask.resources();
                    deposit.onStart(mc);
                    phase = Phase.DEPOSIT;
                } catch (Exception e) {
                    ChatUtil.warn("Inventory full but no chest to deposit into — continuing.");
                    startMining();
                }
            }
            return TaskResult.running();
        }

        // DEPOSIT phase
        TaskResult r;
        try {
            r = deposit.tick(mc);
        } catch (Exception e) {
            r = TaskResult.failed(e.getMessage());
        }
        if (!r.isDone()) return TaskResult.running();
        deposit.onStop(mc);
        deposit = null;
        startMining();
        return TaskResult.running();
    }

    private boolean inventoryFull(LocalPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    private boolean safeMining() {
        try {
            return BaritoneHelper.isMining();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onStop(Minecraft mc) {
        if (deposit != null) {
            try {
                deposit.onStop(mc);
            } catch (Exception ignored) {
            }
        }
        try {
            BaritoneHelper.cancel();
        } catch (Exception ignored) {
        }
    }
}
