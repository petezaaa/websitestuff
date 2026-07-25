# schematics/

Put schematic files here, then build them in game with:

```
/bot build <name> [x y z]
```

`<name>` is the filename without extension. If you omit coordinates, the build
starts at the bot's current position.

## Supported formats

Baritone reads these (drop the file straight in this folder):

- **Sponge** `.schem` — e.g. WorldEdit `//schem save <name>`
- **MCEdit** `.schematic`
- **Litematica** `.litematic`

## At game time

This folder is a project template. At runtime the mod looks for schematics in
your Minecraft instance:

```
.minecraft/schematics/<name>.schem
```

So copy the files you want to build into `.minecraft/schematics/`.

## Related commands

- `/bot build <name> [x y z]` — build it (Baritone keeps redstone orientation).
- `/bot materials <name>` — gather/craft/smelt the materials it needs (best-effort).
- `/bot verify <name> [x y z]` — compare the built structure to the schematic,
  including redstone facing/delay/mode.

## Included samples

- `cozy_house.schem` — a 7×5×7 oak house with a **chest, furnace, crafting
  table, bed, glass windows and torches**. A ready-made base.
- `log_cabin.schem` — a bigger 9×6×7 spruce cabin with **two chests, two
  furnaces**, a crafting table and a bed.
- `starter_shack.schem` — a 5×4×5 hollow cobblestone hut with a doorway.
- `redstone_demo.schem` — a tiny piston + repeater + redstone-block line, for
  testing `/bot verify redstone_demo` and orientation handling.

## Build a house as your base

```
/bot materials cozy_house    # gather/craft the blocks it needs first
/bot base cozy_house         # build it and register its chest/furnace/bed as home
```

`/bot base` builds the house with Baritone (you need the blocks — glass = smelted
sand, bed = wool + planks, chest/furnace/table as usual), then scans the built
house and records its chest, furnace and bed so the bot stashes loot and sleeps
there. You can also just `/bot build cozy_house` if you don't want it registered
as home.

> Baritone builds each block from the schematic, so it needs the exact items in
> its inventory. Beds and wall-attached blocks are the least reliable to place;
> if a piece is missing after building, `/bot verify cozy_house` shows what.
