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

## Included sample

- `starter_shack.schem` — a 5×4×5 hollow cobblestone hut with a doorway, a
  valid Sponge v2 file you can use to test `/bot build starter_shack`.
  (You'll need cobblestone in your inventory for Baritone to place.)
