package com.baritonebot.task;

import com.baritonebot.integration.BaritoneHelper;
import com.baritonebot.util.EntityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.player.LocalPlayer;

/**
 * Kill creatures. With no name it clears the nearest hostiles; with a name
 * (e.g. "zombie", "cow") it hunts that type. Uses Baritone to close distance,
 * then attacks directly with correct attack-cooldown timing.
 */
public class KillTask extends Task {

    private static final double REACH = 3.0;
    private static final double SEARCH_RADIUS = 48.0;
    private static final int REPATH_INTERVAL = 10;

    private final String targetName; // null => nearest hostile
    private final int count;

    private int killed = 0;
    private LivingEntity target;
    private int repathCooldown = 0;

    public KillTask(String targetName, int count) {
        this.targetName = targetName;
        this.count = Math.max(1, count);
    }

    private String label() {
        return targetName == null ? "hostiles" : targetName;
    }

    @Override
    public String name() {
        return "kill " + count + " " + label();
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) return TaskResult.failed("No player.");

        if (killed >= count) {
            return TaskResult.success("Killed " + killed + " " + label() + ".");
        }

        // Acquire or refresh the current target.
        if (target == null || !target.isAlive() || target.isRemoved()) {
            if (target != null) {
                killed++;
                target = null;
                BaritoneHelper.cancel();
                if (killed >= count) {
                    return TaskResult.success("Killed " + killed + " " + label() + ".");
                }
            }
            target = acquire();
            if (target == null) {
                return killed > 0
                    ? TaskResult.success("Killed " + killed + " " + label() + "; no more found.")
                    : TaskResult.failed("No " + label() + " within " + (int) SEARCH_RADIUS + " blocks.");
            }
            repathCooldown = 0;
        }

        double dist = player.distanceTo(target);
        if (dist <= REACH) {
            // In range: stop moving, face the target, and swing on full charge.
            if (BaritoneHelper.isPathing()) BaritoneHelper.cancel();
            player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            if (player.getAttackStrengthScale(0f) >= 0.9f) {
                mc.gameMode.attack(player, target);
                player.swing(InteractionHand.MAIN_HAND);
                player.resetAttackStrengthTicker();
            }
        } else if (repathCooldown <= 0) {
            // Out of range: (re)path toward the moving target.
            BaritoneHelper.gotoNear(target.blockPosition(), 2);
            repathCooldown = REPATH_INTERVAL;
        }

        repathCooldown--;
        return TaskResult.running();
    }

    private LivingEntity acquire() {
        return targetName == null
            ? EntityUtil.nearestHostile(SEARCH_RADIUS)
            : EntityUtil.nearestNamed(targetName, SEARCH_RADIUS);
    }

    @Override
    public void onStop(Minecraft mc) {
        BaritoneHelper.cancel();
    }
}
