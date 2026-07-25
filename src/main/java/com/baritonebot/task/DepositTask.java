package com.baritonebot.task;

import com.baritonebot.chest.ChestLog;
import com.baritonebot.chest.ContainerHelper;
import com.baritonebot.integration.BaritoneHelper;
import com.baritonebot.util.MenuUtil;
import com.baritonebot.util.Names;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Travels to the nearest chest/barrel and deposits items. Mode is either a
 * specific item, everything, or "resources" (keeping tools/armor/food). Logs
 * the chest contents afterwards.
 */
public class DepositTask extends Task {

    public enum Mode { ALL, RESOURCES, ITEM }

    private enum Phase { GO, OPEN, TRANSFER }

    private final Mode mode;
    private final Item item; // for Mode.ITEM

    private Phase phase = Phase.GO;
    private int phaseTicks;
    private boolean sawPath;
    private int wait;
    private BlockPos chestPos;
    private int moved;
    private int moves;

    public DepositTask(Mode mode, Item item) {
        this.mode = mode;
        this.item = item;
    }

    public static DepositTask resources() {
        return new DepositTask(Mode.RESOURCES, null);
    }

    @Override
    public String name() {
        return "deposit " + (mode == Mode.ITEM ? Names.itemName(item) : mode.name().toLowerCase());
    }

    @Override
    public void onStart(Minecraft mc) {
        LocalPlayer player = mc.player;
        chestPos = ContainerHelper.findNearest(player.level(), player.blockPosition(), 24);
        if (chestPos == null) throw new IllegalStateException("No chest or barrel within 24 blocks.");
        BaritoneHelper.gotoInteract(chestPos);
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        phaseTicks++;
        LocalPlayer player = mc.player;
        switch (phase) {
            case GO:
                if (BaritoneHelper.isPathing()) { sawPath = true; return TaskResult.running(); }
                if (!sawPath && phaseTicks < 60) return TaskResult.running();
                if (!player.blockPosition().closerThan(chestPos, 5)) return TaskResult.failed("Couldn't reach the chest.");
                enter(Phase.OPEN);
                return TaskResult.running();
            case OPEN:
                if (mc.player.containerMenu instanceof ChestMenu) { enter(Phase.TRANSFER); return TaskResult.running(); }
                if (phaseTicks % 10 == 1) rightClick(mc, player);
                if (phaseTicks > 60) return TaskResult.failed("Couldn't open the chest.");
                return TaskResult.running();
            default:
                return transfer(mc);
        }
    }

    private TaskResult transfer(Minecraft mc) {
        if (wait > 0) { wait--; return TaskResult.running(); }
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (!(menu instanceof ChestMenu)) return TaskResult.failed("Chest closed unexpectedly.");

        int slot = nextDepositSlot(menu);
        if (slot >= 0 && moves < 300) {
            moved += menu.getSlot(slot).getItem().getCount();
            MenuUtil.quickMove(menu, slot);
            moves++;
            wait = 2;
            return TaskResult.running();
        }
        // Nothing left to move (or chest full) — record and finish.
        ChestLog.record(chestPos, menu);
        mc.player.closeContainer();
        return TaskResult.success("Deposited " + moved + " item(s). " + ChestLog.describe(chestPos));
    }

    private int nextDepositSlot(AbstractContainerMenu menu) {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (!(s.container instanceof Inventory)) continue;
            ItemStack stack = s.getItem();
            if (stack.isEmpty()) continue;
            if (depositable(stack)) return i;
        }
        return -1;
    }

    private boolean depositable(ItemStack stack) {
        switch (mode) {
            case ALL: return true;
            case ITEM: return stack.is(item);
            default: return !ContainerHelper.isEssential(stack.getItem());
        }
    }

    private void rightClick(Minecraft mc, LocalPlayer player) {
        Vec3 hit = new Vec3(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, hit);
        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, chestPos, false));
    }

    private void enter(Phase p) {
        phase = p;
        phaseTicks = 0;
    }

    @Override
    public void onStop(Minecraft mc) {
        if (mc.player != null && mc.player.containerMenu instanceof ChestMenu) mc.player.closeContainer();
        try {
            BaritoneHelper.cancel();
        } catch (Exception ignored) {
        }
    }
}
