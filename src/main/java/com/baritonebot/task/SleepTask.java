package com.baritonebot.task;

import com.baritonebot.auto.HomeBase;
import com.baritonebot.integration.BaritoneHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Walks to the base bed and sleeps to skip the night (and reset spawn). */
public class SleepTask extends Task {

    private enum Phase { GO, SLEEP }

    private BlockPos bedPos;
    private Phase phase = Phase.GO;
    private int phaseTicks;
    private boolean sawPath;
    private boolean slept;

    /** Roughly the window during which a bed can be used. */
    public static boolean isNight(Level level) {
        long t = level.getDayTime() % 24000;
        return t >= 13000 && t < 23000;
    }

    @Override
    public String name() {
        return "sleep at base";
    }

    @Override
    public void onStart(Minecraft mc) {
        bedPos = HomeBase.bed();
        if (bedPos == null) throw new IllegalStateException("No bed at the base.");
        BaritoneHelper.gotoInteract(bedPos);
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        phaseTicks++;
        LocalPlayer player = mc.player;

        if (phase == Phase.GO) {
            if (BaritoneHelper.isPathing()) { sawPath = true; return TaskResult.running(); }
            if (!sawPath && phaseTicks < 60) return TaskResult.running();
            if (!player.blockPosition().closerThan(bedPos, 4)) return TaskResult.failed("Couldn't reach the bed.");
            phase = Phase.SLEEP;
            phaseTicks = 0;
            return TaskResult.running();
        }

        // SLEEP
        if (player.isSleeping()) {
            slept = true;
            return TaskResult.running(); // stay until we wake (time skips to day)
        }
        if (slept) {
            return TaskResult.success("Slept through the night.");
        }
        if (!isNight(mc.level)) {
            return TaskResult.success("Not night — no need to sleep.");
        }
        if (phaseTicks % 12 == 1) {
            Vec3 hit = new Vec3(bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5);
            player.lookAt(EntityAnchorArgument.Anchor.EYES, hit);
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, bedPos, false));
        }
        if (phaseTicks > 200) return TaskResult.failed("Couldn't sleep (mobs nearby or not night).");
        return TaskResult.running();
    }

    @Override
    public void onStop(Minecraft mc) {
        try {
            BaritoneHelper.cancel();
        } catch (Exception ignored) {
        }
    }
}
