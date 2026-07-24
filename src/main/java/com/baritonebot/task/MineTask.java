package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Find and mine a number of blocks using Baritone's mining process. */
public class MineTask extends DelegatedTask {

    private final String label;
    private final int quantity;
    private final Block[] blocks;

    public MineTask(String label, int quantity, List<Block> blocks) {
        super(100);
        this.label = label;
        this.quantity = quantity;
        this.blocks = blocks.toArray(new Block[0]);
    }

    @Override
    public String name() {
        return "mine " + quantity + " " + label;
    }

    @Override
    protected void begin(Minecraft mc) {
        BaritoneHelper.mine(quantity, blocks);
    }

    @Override
    protected boolean active() {
        return BaritoneHelper.isMining();
    }

    @Override
    protected String doneMessage() {
        return "Finished mining " + label + ".";
    }
}
