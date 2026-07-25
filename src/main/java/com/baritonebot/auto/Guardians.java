package com.baritonebot.auto;

import com.baritonebot.chest.ChestLog;
import com.baritonebot.chest.ContainerHelper;
import com.baritonebot.integration.BaritoneHelper;
import com.baritonebot.util.ArmorUtil;
import com.baritonebot.util.EntityUtil;
import com.baritonebot.util.EquipUtil;
import com.baritonebot.util.WeaponUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Background survival behaviours that run every client tick while auto mode is
 * on, independent of whatever task is active:
 *  - auto-respawn on death
 *  - auto-eat when hunger drops
 *  - auto-defend: melee any hostile that gets into reach (works alongside
 *    Baritone since attacks are entity-targeted, not raycast-based)
 */
public final class Guardians {

    private Guardians() {}

    // Foods we avoid eating unless nothing else is around.
    private static final Set<String> BAD_FOOD = Set.of(
        "spider_eye", "pufferfish", "poisonous_potato", "rotten_flesh", "suspicious_stew", "chicken");

    private static boolean eating = false;
    private static int eatTimeout = 0;
    private static int armorCooldown = 0;
    private static int lastChestContainerId = -1;

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            if (eating) stopEat(mc);
            return;
        }
        // Chest logging is always on, regardless of auto mode.
        watchChests(mc);

        if (!AutoMode.isEnabled()) {
            if (eating) stopEat(mc);
            return;
        }
        respawn(mc);
        eat(mc);
        defend(mc);
    }

    // --- Passive chest logging -------------------------------------------

    private static void watchChests(Minecraft mc) {
        if (mc.player.containerMenu instanceof ChestMenu menu) {
            if (menu.containerId != lastChestContainerId) {
                lastChestContainerId = menu.containerId;
                BlockPos pos = ContainerHelper.findNearest(mc.level, mc.player.blockPosition(), 6);
                if (pos != null) ChestLog.record(pos, menu);
            }
        } else {
            lastChestContainerId = -1;
        }
    }

    // --- Respawn ----------------------------------------------------------

    private static void respawn(Minecraft mc) {
        if (mc.screen instanceof DeathScreen && mc.player != null) {
            mc.player.respawn();
            mc.setScreen(null);
        }
    }

    // --- Eat --------------------------------------------------------------

    private static void eat(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (eating) {
            mc.options.keyUse.setDown(true);
            eatTimeout--;
            if (eatTimeout <= 0 || !player.isUsingItem() && eatTimeout < 30) {
                stopEat(mc);
            }
            return;
        }
        // Don't fight Baritone's own right-click use while it's building.
        if (BaritoneHelper.isBuilding()) return;
        if (player.getFoodData().getFoodLevel() > 16 || player.isUsingItem()) return;

        Item food = findFood(player);
        if (food == null) return;
        if (!EquipUtil.holdItem(player, food)) return;
        eating = true;
        eatTimeout = 40;
        mc.options.keyUse.setDown(true);
    }

    private static void stopEat(Minecraft mc) {
        eating = false;
        eatTimeout = 0;
        if (mc.options != null) mc.options.keyUse.setDown(false);
    }

    private static Item findFood(LocalPlayer player) {
        var inv = player.getInventory();
        Item fallback = null;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || !s.getItem().isEdible()) continue;
            String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
            if (BAD_FOOD.contains(name)) {
                fallback = fallback == null ? s.getItem() : fallback;
                continue;
            }
            return s.getItem();
        }
        return fallback; // eat something rather than starve
    }

    // --- Defend -----------------------------------------------------------

    private static void defend(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (armorCooldown > 0) armorCooldown--;
        if (eating) return;
        LivingEntity threat = EntityUtil.nearestHostile(10);
        if (threat == null) return;

        // A threat is near — make sure we're wearing our armor.
        if (armorCooldown == 0) {
            ArmorUtil.equipBest(player);
            armorCooldown = 200;
        }

        double dist = player.distanceTo(threat);
        if (dist > 3.2) return; // in melee range only; Baritone controls movement

        WeaponUtil.equipBestMelee(player);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, threat.getEyePosition());
        if (player.getAttackStrengthScale(0f) >= 0.9f) {
            mc.gameMode.attack(player, threat);
            player.swing(InteractionHand.MAIN_HAND);
            player.resetAttackStrengthTicker();
        }
    }
}
