package com.baritonebot.task;

import com.baritonebot.util.ArmorUtil;
import net.minecraft.client.Minecraft;

/** Equips the best armor from the inventory into empty armor slots. */
public class EquipArmorTask extends Task {

    @Override
    public String name() {
        return "equip armor";
    }

    @Override
    public TaskResult tick(Minecraft mc) {
        int n = ArmorUtil.equipBest(mc.player);
        return TaskResult.success(n > 0 ? "Equipped " + n + " armor piece(s)." : "No armor to equip (or already worn).");
    }
}
