package com.baritonebot;

import com.baritonebot.command.BotCommands;
import com.baritonebot.task.TaskManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side Forge event wiring: registers the {@code /bot} command and pumps
 * the {@link TaskManager} once per client tick so tasks advance their state
 * machines without blocking the game thread.
 */
@Mod.EventBusSubscriber(modid = BaritoneBotMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        BotCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TaskManager.tick();
        }
    }
}
