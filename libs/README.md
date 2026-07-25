# libs/ — drop the Baritone API jar here

This mod **compiles against** the Baritone API but does not bundle Baritone.
You need the Baritone **Forge** build for Minecraft **1.20.1**.

## What to download

Get a Baritone 1.20.1 **Forge** release (from Baritone's official releases or a
trusted mirror). You typically get two jars:

| Jar | Where it goes | Why |
|-----|---------------|-----|
| `baritone-api-forge-<version>.jar`        | `libs/` (this folder) | so this mod **compiles** |
| `baritone-standalone-forge-<version>.jar` | your `.minecraft/mods/` folder | so Baritone **runs** in game |

> The `baritone_version` in `gradle.properties` (default `1.10.2`) must match the
> `baritone-api-forge-<version>.jar` filename you place here. Adjust it if your
> download differs.

## Then build

```bash
./gradlew build
```

The finished mod jar appears in `build/libs/`. Put **that** jar *and* the
Baritone standalone jar into your `.minecraft/mods/` folder (Forge 1.20.1).

## Note

`libs/*.jar` is git-ignored on purpose — redistributing Baritone's jar here is
not our call. Everyone supplies their own copy.
