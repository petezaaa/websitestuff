package com.baritonebot.task;

import net.minecraft.client.Minecraft;

/**
 * A unit of bot work that advances a little each client tick. Tasks must never
 * block the game thread — long operations are expressed as state machines that
 * make progress across many {@link #tick(Minecraft)} calls.
 */
public abstract class Task {

    /** A short human-readable label, shown by the status command. */
    public abstract String name();

    /** Called once when the task becomes active. */
    public void onStart(Minecraft mc) {}

    /** Called every client tick while active. Return RUNNING to continue. */
    public abstract TaskResult tick(Minecraft mc);

    /** Called once when the task ends (success, failure, or cancellation). */
    public void onStop(Minecraft mc) {}
}
