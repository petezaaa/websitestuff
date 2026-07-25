package com.baritonebot.task;

import net.minecraft.client.Minecraft;

/** A task that immediately completes — used as a "nothing to do / skipped" step. */
public class NoOpTask extends Task {

    private final String message;

    public NoOpTask(String message) {
        this.message = message;
    }

    @Override
    public String name() {
        return message;
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        return TaskResult.success(message);
    }
}
