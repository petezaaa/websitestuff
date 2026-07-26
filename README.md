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
| `/bot build <name> [x y z]` | Build a schematic (keeps redstone orientation) | Baritone builder |
| `/bot base <house> [x y z]` | Build a house schematic and register it as home | Custom |
| `/bot materials <name>` | Gather/craft/smelt what a schematic needs | Custom planner |
| `/bot verify <name> [x y z]` | Check a build vs schematic, incl. redstone facing | Custom |
| `/bot gather <block> <n>` | Mine, then return to a chest to deposit, on a loop | Baritone + custom |
| `/bot deposit [item\|all\|resources]` | Deposit into nearest chest (logs contents) | Custom |
| `/bot withdraw <item> [n]` | Take items from nearest chest | Custom |
| `/bot chests` | List logged chest contents | Custom |
| `/bot tools` | Auto-progress wood → stone → iron → diamond tools | Composed |
| `/bot play [hours]` | Free-play: gather, craft, build random stuff, fight, explore | Composed |
| `/bot survive [hours]` | Lean survival loop: roam, kill, mine, hunt food, base to respawn/store | Composed |
| `/bot auto <on\|off>` | Eat, fight mobs, respawn, protect Mending tools | Guardians |
| `/bot drop <item\|all>` | Drop items | — |
| `/bot equip <item>` | Hold an item | — |
| `/bot equiparmor` | Equip the best armor you have | — |
| `/bot inv` | List your inventory | — |
| `/bot status` | Show the active task | — |
| `/bot stop` | Cancel everything | — |

## Autonomous features

**Auto / survival mode** — `/bot auto on` turns on background guardians that run
every tick regardless of the current task:
- **Auto-respawn** when the bot dies.
- **Auto-eat** when hunger drops (picks the best food, avoids poisonous ones).
- **Auto-defend**: when a hostile is near it **equips your best armor** and a
  sword, then melees anything that gets into reach. Because attacks are
  entity-targeted (not raycast), this works even while Baritone is moving —
  handy for not getting blown up or shot mid-task.
- **Mending/durability protection**: Baritone's `itemSaver` is set to **2**, so
  tools are dropped from use at 2 durability and never break (protects Mending
  gear). Always on; tune with the setting if you like.

`KillTask` (`/bot kill`) also equips your best sword before engaging, and
`/bot equiparmor` equips armor on demand.

**Mine → deposit loop** — `/bot gather <block> <n>` mines with Baritone, and
when the inventory fills it runs to the nearest chest, deposits *resources*
(keeping tools/armor/food), then resumes — repeating until it has `n`.

**Chest logging** — always on: whenever you (or a task) open a chest/barrel, its
position and contents are recorded to `<gameDir>/baritonebot/chestlog.txt`.
`/bot chests` prints the log. Deposit/withdraw also re-log after transferring.

**Tool progression** — `/bot tools` chains mine/craft/smelt steps to go from
nothing to a full set of diamond tools (wood → stone → iron → diamond). Turn on
`/bot auto on` first so it survives the trip.

**Free-play / "AI player" mode** — `/bot play [hours]` (default **24h**) turns the
bot loose to behave like a player: it keeps itself alive (auto mode is enabled
automatically) and, whenever it's idle, picks a human-like activity based on its
inventory plus randomness:
- gather wood when it's low,
- craft a pickaxe if it doesn't have one,
- **set up a home base** — crafts and places a crafting table, chest, furnace
  (and a bed if it has one) once it has a pickaxe and some wood,
- mine cobblestone/stone,
- **build a random structure** out of whatever block it has the most of,
- **stash excess loot** in the base chest when its pack fills up,
- **sleep at the base** through the night to stay safe from mobs,
- hunt nearby mobs,
- or wander and explore.

The random structures are generated procedurally (pillars, walls, platforms,
cube frames, pyramids, huts, staircases, block "sculptures"), written to a
temporary schematic in `<gameDir>/baritonebot/generated/`, and built by
Baritone. Each decision is announced in chat (`[Free-play] build a random hut`),
so you can leave it running and watch what it decides to make over time. Stop any
time with `/bot stop`.

**Survival loop** — `/bot survive [hours]` is a leaner, more reactive version:
no random building or diamond-chasing, just **roam, kill hostiles, mine, hunt
animals for food, and keep a base to respawn at and store loot in**. It reacts to
danger and hunger first, then cycles through mining/hunting/exploring, stashing
at the base when full and sleeping at night if it has a bed. Best paired with the
`keepInventory` gamerule on (`/gamerule keepInventory true`) so death just sends
it home to keep going.

## Redstone & schematics

Baritone's builder places blocks using the **full block state** stored in the
schematic, so pistons, repeaters, comparators, observers, etc. come out with the
correct **facing / delay / mode** — as long as the schematic contains those
states (WorldEdit/Litematica saves do).

- `/bot materials <name>` reads a schematic, tallies the blocks it needs,
  subtracts your inventory, then **mines raw blocks and crafts the rest**
  (pistons, slabs, redstone components, ...). It's best-effort — see the note in
  the schematics section — and reports anything it couldn't obtain.
- `/bot verify <name> [x y z]` compares the built structure against the
  schematic and reports **wrong block / wrong orientation / missing**, so you can
  confirm the redstone is oriented correctly.
- `/bot base <house>` builds a **ready-made house** schematic (with chest,
  furnace, crafting table, bed) and registers its containers/bed as the bot's
  **home base**, so it then stashes loot and sleeps there. Two houses ship in
  `schematics/`: `cozy_house` and `log_cabin`. Run `/bot materials <house>`
  first to gather the blocks.

Included schematics: `cozy_house`, `log_cabin`, `starter_shack`, `redstone_demo`.

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
│   ├── SequenceTask      runs child tasks in order (composes the rest)
│   ├── DelegatedTask     base for "hand off to Baritone, watch for done"
│   ├── GotoTask / MineTask / BuildTask     Baritone-backed
│   ├── FollowTask / ExploreTask            Baritone-backed (open-ended)
│   ├── KillTask          chase + attack with correct swing timing
│   ├── CraftTask         table detect/place + recipe-book autofill
│   ├── SmeltTask         furnace detect/place + load fuel/input + collect
│   ├── Deposit/WithdrawTask   chest transfer + logging
│   ├── GatherRunTask     mine → return → deposit loop
│   ├── Materials/VerifyTask   schematic materials + build verification
│   └── ProgressionTask   wood → stone → iron → diamond tools
├── auto                  AutoMode + Guardians (respawn/eat/defend)
├── chest                 ChestLog + container helpers
├── schematic             SchematicData (Sponge .schem parser)
├── integration
│   └── BaritoneHelper    the ONLY file that touches the Baritone API
└── util                  names, entities, inventory, menus, placement, chat
```

If a Baritone build exposes a slightly different signature, `BaritoneHelper`
is the single place you'd adjust.

---

## Building it

> **Just want to get it running?** Follow [`SETUP.md`](SETUP.md) — a Windows-first,
> step-by-step guide with helper scripts (`setup.ps1` / `setup.sh`) that check
> Java, build the mod, and copy it into your `mods/` folder.

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

## Learning / RL agent (separate, GPU)

There's also a **from-scratch reinforcement-learning agent** in
[`learning/`](learning/README.md) — a PyTorch/CUDA scaffold that *learns* to play
Minecraft from the screen using **PPO + curiosity (RND)** on your GPU. It's a
different paradigm from this mod (it learns behaviour instead of running scripted
behaviour, and targets MineRL/Minecraft 1.16), so it lives in its own folder with
its own README and honest expectations. Use whichever fits: scripted-and-capable
(this mod) or learning-and-curious (`learning/`).

## License

MIT — see [LICENSE](LICENSE).
