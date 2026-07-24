package com.baritonebot.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Registers the single {@code /bot <args...>} client command. Argument parsing
 * is intentionally handled by {@link CommandParser} (a plain tokenizer) rather
 * than a deep Brigadier tree — it keeps the many subcommands easy to read.
 */
public final class BotCommands {

    private BotCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bot")
                .executes(ctx -> {
                    CommandParser.run("help");
                    return 1;
                })
                .then(Commands.argument("args", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        CommandParser.run(StringArgumentType.getString(ctx, "args"));
                        return 1;
                    }))
        );
    }
}
