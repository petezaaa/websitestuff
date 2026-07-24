package com.baritonebot.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Helpers for locating entities the bot should target.
 */
public final class EntityUtil {

    private EntityUtil() {}

    public static String typePath(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    public static boolean isHostile(Entity e) {
        return e instanceof Enemy;
    }

    /** Nearest living hostile within {@code radius} blocks, or null. */
    public static LivingEntity nearestHostile(double radius) {
        return nearest(radius, e -> e instanceof LivingEntity && isHostile(e) && e.isAlive());
    }

    /** Nearest living entity whose type path (e.g. "cow") or player name matches. */
    public static LivingEntity nearestNamed(String name, double radius) {
        String want = Names.normalize(name);
        return nearest(radius, e -> {
            if (!(e instanceof LivingEntity) || !e.isAlive()) return false;
            if (e instanceof Player p) {
                return p.getGameProfile().getName().equalsIgnoreCase(name);
            }
            return typePath(e).equals(want);
        });
    }

    /** Nearest other player (not the bot itself), or null. */
    public static Player nearestOtherPlayer(double radius) {
        Minecraft mc = Minecraft.getInstance();
        Player self = mc.player;
        if (self == null || mc.level == null) return null;
        Player best = null;
        double bestSq = radius * radius;
        for (Player p : mc.level.players()) {
            if (p == self || !p.isAlive()) continue;
            double d = p.distanceToSqr(self);
            if (d < bestSq) {
                bestSq = d;
                best = p;
            }
        }
        return best;
    }

    private interface EntityFilter {
        boolean test(Entity e);
    }

    private static LivingEntity nearest(double radius, EntityFilter filter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        LivingEntity best = null;
        double bestSq = radius * radius;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || !filter.test(e)) continue;
            double d = e.distanceToSqr(mc.player);
            if (d < bestSq) {
                bestSq = d;
                best = (LivingEntity) e;
            }
        }
        return best;
    }
}
