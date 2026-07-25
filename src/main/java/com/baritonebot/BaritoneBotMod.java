package com.baritonebot;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Mod entry point. The interesting work lives in {@link com.baritonebot.command}
 * (command registration) and {@link com.baritonebot.task} (the actual behaviours).
 * This mod is client-only: it drives the local player through Baritone plus a
 * handful of custom tasks (combat, crafting, smelting).
 */
@Mod(BaritoneBotMod.MODID)
public class BaritoneBotMod {
    public static final String MODID = "baritonebot";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BaritoneBotMod() {
        LOGGER.info("Baritone Bot initialised. Type '/bot help' in game once you are in a world.");
    }
}
