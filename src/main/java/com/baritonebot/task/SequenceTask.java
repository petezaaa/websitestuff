package com.baritonebot.task;

import com.baritonebot.util.ChatUtil;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Supplier;

/**
 * Runs a list of child tasks in order, hosting each child's lifecycle. Used to
 * compose higher-level behaviours (gather loops, progression, schematic
 * material acquisition) out of the existing mine/craft/smelt/deposit tasks.
 *
 * Steps are supplied lazily so each one is built against the world state at the
 * moment it runs, not up front.
 */
public class SequenceTask extends Task {

    private final String label;
    private final List<Supplier<Task>> steps;
    private final boolean stopOnFailure;

    private int index = -1;
    private Task current;

    public SequenceTask(String label, List<Supplier<Task>> steps, boolean stopOnFailure) {
        this.label = label;
        this.steps = steps;
        this.stopOnFailure = stopOnFailure;
    }

    @Override
    public String name() {
        return label + (current != null ? " (" + current.name() + ")" : "");
    }

    @Override
    public void onStart(Minecraft mc) {
        // First step starts on the first tick, so failures surface through tick().
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        if (current == null) {
            return advance(mc);
        }
        TaskResult r;
        try {
            r = current.tick(mc);
        } catch (Exception e) {
            r = TaskResult.failed(e.getMessage() != null ? e.getMessage() : "error");
        }
        if (!r.isDone()) return TaskResult.running();

        safeStop(mc, current);
        if (r.state == TaskResult.State.FAILED) {
            ChatUtil.warn("[" + label + "] " + current.name() + ": " + r.message);
            if (stopOnFailure) {
                String failed = current.name();
                current = null;
                return TaskResult.failed(label + " stopped at '" + failed + "'.");
            }
        } else {
            ChatUtil.ok("[" + label + "] " + (r.message != null ? r.message : current.name() + " done"));
        }
        current = null;
        return advance(mc);
    }

    private TaskResult advance(Minecraft mc) {
        index++;
        if (index >= steps.size()) {
            return TaskResult.success(label + " complete.");
        }
        try {
            current = steps.get(index).get();
            current.onStart(mc);
            ChatUtil.info("[" + label + "] step " + (index + 1) + "/" + steps.size() + ": " + current.name());
            return TaskResult.running();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "failed to start";
            ChatUtil.warn("[" + label + "] step " + (index + 1) + ": " + msg);
            Task failedTask = current;
            current = null;
            if (stopOnFailure) {
                return TaskResult.failed(label + " stopped: " + msg);
            }
            return advance(mc); // skip and continue
        }
    }

    private void safeStop(Minecraft mc, Task task) {
        try {
            task.onStop(mc);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onStop(Minecraft mc) {
        if (current != null) safeStop(mc, current);
    }
}
