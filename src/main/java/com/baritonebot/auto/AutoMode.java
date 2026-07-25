package com.baritonebot.auto;

/**
 * Global toggle for the background survival guardians (auto-respawn, auto-eat,
 * auto-defend). Durability protection is handled separately by Baritone's
 * itemSaver setting and stays on regardless.
 */
public final class AutoMode {

    private static boolean enabled = false;

    private AutoMode() {}

    public static void set(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
