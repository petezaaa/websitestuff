# Baritone Bot

A command-driven autonomous Minecraft bot for **Forge 1.20.1**, built on top of
[Baritone](https://github.com/cabaletta/baritone). Baritone provides the
pathfinding, block-finding/mining and schematic-building foundation; this mod
adds the things Baritone doesn't do — **combat, crafting and smelting** — and a
single `/bot` command that ties everything together.

> **Tell it what to do, in chat:**
> ```
> /bot mine diamond_ore 3
> /bot kill zombie 5
> /bot craft iron_pickaxe
> /bot smelt raw_iron 8 coal
> /bot build starter_shack
> ```

---

## What it can do

| Command | What happens | Powered by |
|---------|--------------|------------|
| `/bot goto <x> <y> <z>` | Path to coordinates | Baritone pathfinding |
| `/bot come` | Walk to the nearest other player | Baritone |
| `/bot follow <player>` | Continuously follow a player | Baritone follow |
| `/bot explore [x z]` | Explore outward to reveal terrain | Baritone explore |
| `/bot mine <block> [n]` | Find and mine blocks (aliases: `wood`, `stone`, `*_ore`) | Baritone mining |
| `/bot kill [target] [n]` | Kill nearest hostiles, or *n* of a named mob | Custom combat |
| `/bot craft <item> [n]` | Craft items — auto-finds/places a crafting table for 3×3 recipes | Custom crafting |
| `/bot smelt <item> [n] [fuel]` | Smelt in a furnace — auto-finds/places one, auto-fuels | Custom smelting |
| `/bot build <name> [x y z]` | Build a schematic from `./schematics` | Baritone builder |
| `/bot drop <item\|all>` | Drop items | — |
| `/bot equip <item>` | Hold an item | — |
| `/bot inv` | List your inventory | — |
| `/bot status` | Show the active task | — |
| `/bot stop` | Cancel everything | — |

Only **one task runs at a time**. Issuing a new command (or `/bot stop`) cancels
the current one and halts Baritone cleanly.

---

## Architecture

Everything is a small, tick-driven **Task** so nothing ever blocks the game
thread — long jobs advance a state machine one client tick at a time.

```
com.baritonebot
├── BaritoneBotMod        @Mod entry point
├── ClientEvents          registers /bot, pumps the TaskManager each tick
├── command
│   ├── BotCommands       Brigadier registration of /bot
│   └── CommandParser     tokenizes args -> a Task or one-shot action
├── task
│   ├── Task / TaskResult / TaskManager     one active task, tick loop
│   ├── DelegatedTask     base for "hand off to Baritone, watch for done"
│   ├── GotoTask / MineTask / BuildTask     Baritone-backed
│   ├── FollowTask / ExploreTask            Baritone-backed (open-ended)
│   ├── KillTask          chase + attack with correct swing timing
│   ├── CraftTask         table detect/place + recipe-book autofill
│   └── SmeltTask         furnace detect/place + load fuel/input + collect
├── integration
│   └── BaritoneHelper    the ONLY file that touches the Baritone API
└── util                  names, entities, inventory, menus, placement, chat
```

If a Baritone build exposes a slightly different signature, `BaritoneHelper`
is the single place you'd adjust.

---

## Building it

### Requirements
- **JDK 17** (Forge 1.20.1 targets Java 17)
- The Baritone **Forge 1.20.1** jars (see below)

### Steps
1. Download Baritone's Forge 1.20.1 release. Put the **API** jar in
   [`libs/`](libs/README.md) so the mod can compile, and keep the **standalone**
   jar for your `mods/` folder so Baritone actually runs.
   - Make sure `baritone_version` in `gradle.properties` matches the
     `baritone-api-forge-<version>.jar` you downloaded.
2. Build:
   ```bash
   ./gradlew build
   ```
3. Grab `build/libs/baritonebot-1.0.0.jar`.

### Installing
Drop **both** jars into your Forge 1.20.1 `.minecraft/mods/` folder:
- `baritonebot-1.0.0.jar` (this mod)
- `baritone-standalone-forge-<version>.jar` (Baritone)

Launch, load a world, and type `/bot help`.

> **Heads up on the sandbox this was authored in:** a full ForgeGradle build
> decompiles Minecraft and pulls multi-GB dependencies, which the authoring
> environment couldn't run. The source targets the standard Forge 1.20.1 MDK
> and Mojang official mappings; you build it in a normal modding environment
> (JDK 17). The Baritone dependency is deliberately isolated in one file
> (`BaritoneHelper`) in case your Baritone build's API differs by a method name.

---

## Development notes

- **Client-only.** The bot drives *your* player, so it loads on the client side
  and requires Baritone (also client-side).
- **Crafting & smelting** are the most environment-sensitive tasks because they
  drive container menus (crafting table / furnace) through the server-authoritative
  click path. They're written defensively and are progress-based (they stop when
  they stop making progress), but they're the first place to look if a recipe
  behaves oddly on a heavily-modded server.
- **Schematics**: see [`schematics/README.md`](schematics/README.md). A valid
  sample (`starter_shack.schem`) is included.

## License

MIT — see [LICENSE](LICENSE).
