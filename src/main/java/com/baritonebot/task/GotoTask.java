package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import net.minecraft.client.Minecraft;

/** Path to an absolute coordinate using Baritone. */
public class GotoTask extends DelegatedTask {

    private final int x;
    private final int y;
    private final int z;

    public GotoTask(int x, int y, int z) {
        super(100);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String name() {
        return "goto " + x + " " + y + " " + z;
    }

    @Override
    protected void begin(Minecraft mc) {
        BaritoneHelper.gotoBlock(x, y, z);
    }

    @Override
    protected boolean active() {
        return BaritoneHelper.isPathing();
    }

    @Override
    protected String doneMessage() {
        return "Arrived at " + x + ", " + y + ", " + z + ".";
    }
}
