package com.baritonebot.task;

import com.baritonebot.auto.HomeBase;
import com.baritonebot.util.PlacementUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Places whatever base furniture the bot is carrying (crafting table, chest,
 * furnace, bed) on free spots around it, records their positions in
 * {@link HomeBase}, and marks the base as established.
 */
public class PlaceBaseTask extends Task {

    private int step;
    private int wait;
    private final List<String> placed = new ArrayList<>();

    @Override
    public String name() {
        return "set up base";
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        if (wait > 0) { wait--; return TaskResult.running(); }
        LocalPlayer player = mc.player;

        switch (step++) {
            case 0:
                place(player, Items.CRAFTING_TABLE, null);
                return TaskResult.running();
            case 1:
                place(player, Items.CHEST, HomeBase::setChest);
                return TaskResult.running();
            case 2:
                place(player, Items.FURNACE, HomeBase::setFurnace);
                return TaskResult.running();
            case 3: {
                Item bed = findBed(player);
                if (bed != null) place(player, bed, HomeBase::setBed);
                return TaskResult.running();
            }
            default:
                HomeBase.markEstablished(player.blockPosition());
                return TaskResult.success(placed.isEmpty()
                    ? "Base marked here (nothing to place)."
                    : "Base set up with " + String.join(", ", placed) + ".");
        }
    }

    private interface PosSink {
        void accept(BlockPos p);
    }

    private void place(LocalPlayer player, Item item, PosSink sink) {
        BlockPos pos = PlacementUtil.placeBeside(player, item);
        if (pos != null) {
            if (sink != null) sink.accept(pos);
            placed.add(BuiltInRegistries.ITEM.getKey(item).getPath());
            wait = 6; // let the block register before the next placement
        }
    }

    private Item findBed(LocalPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().endsWith("_bed")) return s.getItem();
        }
        return null;
    }
}
