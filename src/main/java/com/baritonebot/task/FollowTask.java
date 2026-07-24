package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import com.baritonebot.util.EntityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Continuously follow a named player. Runs until stopped or another command is
 * issued. If the target can't be seen at start, the task fails.
 */
public class FollowTask extends Task {

    private final String targetName;

    public FollowTask(String targetName) {
        this.targetName = targetName;
    }

    @Override
    public String name() {
        return "follow " + targetName;
    }

    @Override
    public void onStart(Minecraft mc) {
        if (EntityUtil.nearestNamed(targetName, 256) == null) {
            throw new IllegalStateException("I can't see \"" + targetName + "\" to follow.");
        }
        BaritoneHelper.follow(this::matches);
    }

    private boolean matches(Entity e) {
        return e instanceof Player p && p.getGameProfile().getName().equalsIgnoreCase(targetName);
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        // Follow is open-ended; it keeps running until cancelled by the user.
        return TaskResult.running();
    }

    @Override
    public void onStop(Minecraft mc) {
        BaritoneHelper.cancel();
    }
}
