package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import net.minecraft.client.Minecraft;

/**
 * Base class for tasks that hand off to a Baritone process (pathing, mining,
 * building) and simply watch for it to finish. Completion is detected by
 * observing the process become active and then inactive again; a startup grace
 * period covers the case where the goal is already satisfied.
 */
public abstract class DelegatedTask extends Task {

    private final int graceTicks;
    private boolean sawActive = false;
    private int ticks = 0;

    protected DelegatedTask(int graceTicks) {
        this.graceTicks = graceTicks;
    }

    /** Issue the Baritone command. May throw to fail the task immediately. */
    protected abstract void begin(Minecraft mc);

    /** Whether the delegated Baritone process is still running. */
    protected abstract boolean active();

    /** Message shown on success. */
    protected abstract String doneMessage();

    @Override
    public void onStart(Minecraft mc) {
        begin(mc);
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        ticks++;
        boolean isActive = safeActive();
        if (!sawActive) {
            if (isActive) {
                sawActive = true;
            } else if (ticks > graceTicks) {
                // Never became active — assume the goal was already met.
                return TaskResult.success(doneMessage());
            }
            return TaskResult.running();
        }
        return isActive ? TaskResult.running() : TaskResult.success(doneMessage());
    }

    @Override
    public void onStop(Minecraft mc) {
        try {
            BaritoneHelper.cancel();
        } catch (Exception ignored) {
        }
    }

    private boolean safeActive() {
        try {
            return active();
        } catch (Exception e) {
            return false;
        }
    }
}
