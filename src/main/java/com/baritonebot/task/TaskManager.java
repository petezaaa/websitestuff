package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import com.baritonebot.util.ChatUtil;
import net.minecraft.client.Minecraft;

/**
 * Runs a single active task at a time, driven from the client tick. Starting a
 * new task cancels the current one (and any Baritone process it delegated to).
 */
public final class TaskManager {

    private static Task current;

    private TaskManager() {}

    public static synchronized void start(Task task) {
        stop("");
        current = task;
        Minecraft mc = Minecraft.getInstance();
        try {
            task.onStart(mc);
            ChatUtil.info("Started: " + task.name());
        } catch (Exception e) {
            ChatUtil.err("Failed to start " + task.name() + ": " + describe(e));
            safeStop(mc, task);
            current = null;
        }
    }

    public static synchronized void tick() {
        if (current == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        TaskResult result;
        try {
            result = current.tick(mc);
        } catch (Exception e) {
            result = TaskResult.failed("error: " + describe(e));
        }

        if (result.isDone()) {
            Task finished = current;
            current = null;
            safeStop(mc, finished);
            if (result.state == TaskResult.State.SUCCESS) {
                ChatUtil.ok(result.message != null ? result.message : finished.name() + " done.");
            } else {
                ChatUtil.err(result.message != null ? result.message : finished.name() + " failed.");
            }
        }
    }

    public static synchronized void stop(String reason) {
        Minecraft mc = Minecraft.getInstance();
        Task old = current;
        current = null;
        if (old != null) {
            safeStop(mc, old);
            if (!reason.isEmpty()) ChatUtil.warn(reason);
        }
        // Always make sure Baritone is halted too.
        try {
            BaritoneHelper.cancel();
        } catch (Exception ignored) {
            // Baritone may not be present in a dev launch without it.
        }
    }

    public static boolean isBusy() {
        return current != null;
    }

    public static String currentName() {
        return current == null ? "idle" : current.name();
    }

    private static void safeStop(Minecraft mc, Task task) {
        try {
            task.onStop(mc);
        } catch (Exception ignored) {
        }
    }

    private static String describe(Exception e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
