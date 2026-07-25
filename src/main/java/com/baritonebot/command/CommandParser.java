package com.baritonebot.command;

import com.baritonebot.task.BuildTask;
import com.baritonebot.task.CraftTask;
import com.baritonebot.task.ExploreTask;
import com.baritonebot.task.FollowTask;
import com.baritonebot.task.GotoTask;
import com.baritonebot.task.KillTask;
import com.baritonebot.task.MineTask;
import com.baritonebot.task.SmeltTask;
import com.baritonebot.task.Task;
import com.baritonebot.task.TaskManager;
import com.baritonebot.task.DepositTask;
import com.baritonebot.task.GatherRunTask;
import com.baritonebot.task.MaterialsTask;
import com.baritonebot.task.ProgressionTask;
import com.baritonebot.task.VerifyTask;
import com.baritonebot.task.WithdrawTask;
import com.baritonebot.auto.AutoMode;
import com.baritonebot.chest.ChestLog;
import com.baritonebot.util.ActionUtil;
import com.baritonebot.util.ChatUtil;
import com.baritonebot.util.EntityUtil;
import com.baritonebot.util.InventoryUtil;
import com.baritonebot.util.Names;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.io.File;
import java.util.List;

/**
 * Parses the free-form argument string from {@code /bot ...} and turns it into
 * either a queued {@link Task} or a one-shot action.
 */
public final class CommandParser {

    private CommandParser() {}

    public static void run(String raw) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            ChatUtil.err("Not in a world.");
            return;
        }

        String[] t = raw.trim().split("\\s+");
        if (t.length == 0 || t[0].isEmpty()) {
            help();
            return;
        }
        String verb = t[0].toLowerCase();

        try {
            switch (verb) {
                case "goto":     handleGoto(t); break;
                case "come":     handleCome(player); break;
                case "follow":   handleFollow(t); break;
                case "explore":  handleExplore(t, player); break;
                case "mine":     handleMine(t); break;
                case "kill":     handleKill(t); break;
                case "craft":    handleCraft(t); break;
                case "smelt":    handleSmelt(t); break;
                case "build":    handleBuild(t, player); break;
                case "materials": handleMaterials(t); break;
                case "verify":   handleVerify(t, player); break;
                case "gather":   handleGather(t); break;
                case "deposit":  handleDeposit(t); break;
                case "withdraw": handleWithdraw(t); break;
                case "chests":
                case "chestlog": handleChests(); break;
                case "tools":
                case "progress": TaskManager.start(new ProgressionTask()); break;
                case "auto":     handleAuto(t); break;
                case "drop":     handleDrop(t, player); break;
                case "equip":    handleEquip(t, player); break;
                case "inv":
                case "inventory": ChatUtil.info(InventoryUtil.summary(player)); break;
                case "status":   ChatUtil.info("Current task: " + TaskManager.currentName()); break;
                case "stop":     TaskManager.stop("Stopped."); break;
                case "help":     help(); break;
                default:         ChatUtil.err("Unknown command \"" + verb + "\". Try '/bot help'.");
            }
        } catch (NumberFormatException e) {
            ChatUtil.err("Expected a number but got something else. Check '/bot help'.");
        } catch (Exception e) {
            ChatUtil.err(e.getMessage() != null ? e.getMessage() : "Command failed.");
        }
    }

    // --- Handlers ---------------------------------------------------------

    private static void handleGoto(String[] t) {
        if (t.length < 4) throw new IllegalArgumentException("Usage: /bot goto <x> <y> <z>");
        TaskManager.start(new GotoTask(Integer.parseInt(t[1]), Integer.parseInt(t[2]), Integer.parseInt(t[3])));
    }

    private static void handleCome(LocalPlayer player) {
        Player p = EntityUtil.nearestOtherPlayer(256);
        if (p == null) throw new IllegalStateException("No other player nearby to come to.");
        BlockPos pos = p.blockPosition();
        TaskManager.start(new GotoTask(pos.getX(), pos.getY(), pos.getZ()));
    }

    private static void handleFollow(String[] t) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot follow <player>");
        TaskManager.start(new FollowTask(t[1]));
    }

    private static void handleExplore(String[] t, LocalPlayer player) {
        int x = t.length >= 3 ? Integer.parseInt(t[1]) : player.blockPosition().getX();
        int z = t.length >= 3 ? Integer.parseInt(t[2]) : player.blockPosition().getZ();
        TaskManager.start(new ExploreTask(x, z));
    }

    private static void handleMine(String[] t) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot mine <block> [count]");
        String blockName = t[1];
        int count = t.length >= 3 ? Integer.parseInt(t[2]) : 1;
        List<Block> blocks = Names.blocks(blockName);
        if (blocks.isEmpty()) throw new IllegalStateException("Unknown block \"" + blockName + "\".");
        TaskManager.start(new MineTask(blockName, count, blocks));
    }

    private static void handleKill(String[] t) {
        String target = null;
        int count = 1;
        if (t.length == 2) {
            if (isInt(t[1])) count = Integer.parseInt(t[1]);
            else target = t[1];
        } else if (t.length >= 3) {
            target = t[1];
            count = Integer.parseInt(t[2]);
        }
        TaskManager.start(new KillTask(target, count));
    }

    private static void handleCraft(String[] t) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot craft <item> [count]");
        int count = t.length >= 3 ? Integer.parseInt(t[2]) : 1;
        TaskManager.start(new CraftTask(t[1], count));
    }

    private static void handleSmelt(String[] t) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot smelt <item> [count] [fuel]");
        int count = t.length >= 3 ? Integer.parseInt(t[2]) : 1;
        String fuel = t.length >= 4 ? t[3] : null;
        TaskManager.start(new SmeltTask(t[1], count, fuel));
    }

    private static void handleBuild(String[] t, LocalPlayer player) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot build <schematic> [x y z]");
        String schemName = t[1];
        File file = resolveSchematic(schemName);
        if (file == null) {
            throw new IllegalStateException("Schematic \"" + schemName + "\" not found in the 'schematics' folder.");
        }
        BlockPos origin = player.blockPosition();
        if (t.length >= 5) origin = new BlockPos(Integer.parseInt(t[2]), Integer.parseInt(t[3]), Integer.parseInt(t[4]));
        TaskManager.start(new BuildTask(schemName, file, origin));
    }

    private static void handleDrop(String[] t, LocalPlayer player) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot drop <item|all>");
        if (t[1].equalsIgnoreCase("all")) {
            int n = ActionUtil.drop(player, null);
            ChatUtil.ok("Dropped " + n + " item(s).");
            return;
        }
        Item item = Names.item(t[1]).orElseThrow(() -> new IllegalStateException("Unknown item \"" + t[1] + "\"."));
        int n = ActionUtil.drop(player, item);
        ChatUtil.ok("Dropped " + n + "x " + t[1] + ".");
    }

    private static void handleEquip(String[] t, LocalPlayer player) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot equip <item>");
        Item item = Names.item(t[1]).orElseThrow(() -> new IllegalStateException("Unknown item \"" + t[1] + "\"."));
        if (ActionUtil.equip(player, item)) ChatUtil.ok("Equipped " + t[1] + ".");
        else ChatUtil.err("I don't have \"" + t[1] + "\".");
    }

    private static void handleMaterials(String[] t) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot materials <schematic>");
        File file = resolveSchematic(t[1]);
        if (file == null) throw new IllegalStateException("Schematic \"" + t[1] + "\" not found.");
        TaskManager.start(new MaterialsTask(t[1], file));
    }

    private static void handleVerify(String[] t, LocalPlayer player) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot verify <schematic> [x y z]");
        File file = resolveSchematic(t[1]);
        if (file == null) throw new IllegalStateException("Schematic \"" + t[1] + "\" not found.");
        BlockPos origin = player.blockPosition();
        if (t.length >= 5) origin = new BlockPos(Integer.parseInt(t[2]), Integer.parseInt(t[3]), Integer.parseInt(t[4]));
        TaskManager.start(new VerifyTask(t[1], file, origin));
    }

    private static void handleGather(String[] t) {
        if (t.length < 3) throw new IllegalArgumentException("Usage: /bot gather <block> <count>");
        String block = t[1];
        int count = Integer.parseInt(t[2]);
        List<Block> blocks = Names.blocks(block);
        if (blocks.isEmpty()) throw new IllegalStateException("Unknown block \"" + block + "\".");
        TaskManager.start(new GatherRunTask(block, count, blocks));
    }

    private static void handleDeposit(String[] t) {
        if (t.length < 2 || t[1].equalsIgnoreCase("resources")) {
            TaskManager.start(DepositTask.resources());
            return;
        }
        if (t[1].equalsIgnoreCase("all")) {
            TaskManager.start(new DepositTask(DepositTask.Mode.ALL, null));
            return;
        }
        Item item = Names.item(t[1]).orElseThrow(() -> new IllegalStateException("Unknown item \"" + t[1] + "\"."));
        TaskManager.start(new DepositTask(DepositTask.Mode.ITEM, item));
    }

    private static void handleWithdraw(String[] t) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot withdraw <item> [count]");
        Item item = Names.item(t[1]).orElseThrow(() -> new IllegalStateException("Unknown item \"" + t[1] + "\"."));
        int count = t.length >= 3 ? Integer.parseInt(t[2]) : 1;
        TaskManager.start(new WithdrawTask(item, count));
    }

    private static void handleChests() {
        for (String s : ChestLog.summary()) ChatUtil.info(s);
    }

    private static void handleAuto(String[] t) {
        if (t.length < 2) throw new IllegalArgumentException("Usage: /bot auto <on|off>");
        boolean on = t[1].equalsIgnoreCase("on") || t[1].equalsIgnoreCase("true") || t[1].equalsIgnoreCase("enable");
        AutoMode.set(on);
        if (on) ChatUtil.ok("Auto mode ON — eating, fighting mobs, respawning; Mending tools protected at 2 durability.");
        else ChatUtil.ok("Auto mode OFF.");
    }

    // --- Helpers ----------------------------------------------------------

    /** Look for <name>.schem / .schematic / .litematic under ./schematics. */
    private static File resolveSchematic(String name) {
        File dir = new File(Minecraft.getInstance().gameDirectory, "schematics");
        String[] exts = {"", ".schem", ".schematic", ".litematic", ".nbt"};
        for (String ext : exts) {
            File f = new File(dir, name + ext);
            if (f.isFile()) return f;
        }
        return null;
    }

    private static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void help() {
        String[] lines = {
            "§6=== Baritone Bot commands ===",
            "§e/bot goto <x> <y> <z> §7- walk to coordinates",
            "§e/bot come §7- walk to the nearest other player",
            "§e/bot follow <player> §7- follow a player",
            "§e/bot explore [x z] §7- explore outward",
            "§e/bot mine <block> [n] §7- find & mine blocks (aliases: wood, stone, *_ore)",
            "§e/bot kill [target] [n] §7- kill hostiles, or n of a named mob",
            "§e/bot craft <item> [n] §7- craft items (auto-uses a crafting table)",
            "§e/bot smelt <item> [n] [fuel] §7- smelt in a furnace",
            "§e/bot build <schematic> [x y z] §7- build a schematic (keeps redstone orientation)",
            "§e/bot materials <schematic> §7- gather/craft/smelt what a schematic needs",
            "§e/bot verify <schematic> [x y z] §7- check a build vs schematic (redstone facing)",
            "§e/bot gather <block> <n> §7- mine + auto-deposit into chests in a loop",
            "§e/bot deposit [item|all|resources] §7- deposit into nearest chest (logs contents)",
            "§e/bot withdraw <item> [n] §7- take items from nearest chest",
            "§e/bot chests §7- list logged chest contents",
            "§e/bot tools §7- auto-progress wood → stone → iron → diamond tools",
            "§e/bot auto <on|off> §7- eat, fight mobs, respawn, protect Mending tools",
            "§e/bot drop <item|all> §7- drop items",
            "§e/bot equip <item> §7- hold an item",
            "§e/bot inv §7- list inventory",
            "§e/bot status §7- show the current task",
            "§e/bot stop §7- cancel everything",
        };
        for (String l : lines) ChatUtil.info(l);
    }
}
