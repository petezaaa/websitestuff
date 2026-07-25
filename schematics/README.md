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

- `starter_shack.schem` — a 5×4×5 hollow cobblestone hut with a doorway.
  Test with `/bot build starter_shack` (needs cobblestone in inventory).
- `redstone_demo.schem` — a tiny piston + repeater + redstone-block line, handy
  for testing `/bot verify redstone_demo` and orientation handling.
