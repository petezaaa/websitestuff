package com.baritonebot.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Prints bot feedback into the client chat. Safe to call from any task tick.
 */
public final class ChatUtil {

    private ChatUtil() {}

    public static void info(String message) {
        send("§b[Bot] §r" + message);
    }

    public static void ok(String message) {
        send("§a[Bot] §r" + message);
    }

    public static void warn(String message) {
        send("§e[Bot] §r" + message);
    }

    public static void err(String message) {
        send("§c[Bot] §r" + message);
    }

    private static void send(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), false);
        } else {
            System.out.println("[Bot] " + message);
        }
    }
}
