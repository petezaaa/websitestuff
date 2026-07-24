package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import net.minecraft.client.Minecraft;

/** Explore outward from a center point. Open-ended; runs until stopped. */
public class ExploreTask extends Task {

    private final int centerX;
    private final int centerZ;

    public ExploreTask(int centerX, int centerZ) {
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    @Override
    public String name() {
        return "explore around " + centerX + ", " + centerZ;
    }

    @Override
    public void onStart(Minecraft mc) {
        BaritoneHelper.explore(centerX, centerZ);
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        return TaskResult.running();
    }

    @Override
    public void onStop(Minecraft mc) {
        BaritoneHelper.cancel();
    }
}
