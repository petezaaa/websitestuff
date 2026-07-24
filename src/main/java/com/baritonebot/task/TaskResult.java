package com.baritonebot.task;

/**
 * Result of a single task tick: keep running, finished successfully, or failed.
 */
public final class TaskResult {

    public enum State { RUNNING, SUCCESS, FAILED }

    private static final TaskResult RUNNING = new TaskResult(State.RUNNING, null);

    public final State state;
    public final String message;

    private TaskResult(State state, String message) {
        this.state = state;
        this.message = message;
    }

    public static TaskResult running() {
        return RUNNING;
    }

    public static TaskResult success(String message) {
        return new TaskResult(State.SUCCESS, message);
    }

    public static TaskResult failed(String message) {
        return new TaskResult(State.FAILED, message);
    }

    public boolean isDone() {
        return state != State.RUNNING;
    }
}
