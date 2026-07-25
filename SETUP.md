# Getting the bot running on Minecraft (Forge 1.20.1)

This walks you through building the mod and installing it. Windows-first (you're
on a gaming PC), with Mac/Linux notes. There are helper scripts —
[`setup.ps1`](setup.ps1) (Windows) and [`setup.sh`](setup.sh) (Mac/Linux) — that
automate the build + install once the prerequisites are in place.

> The one manual step nobody can automate for you is getting the **Baritone**
> jar (licensing / where it's hosted). See step 3.

---

## 1. Prerequisites

| Need | How |
|------|-----|
| **Minecraft Java Edition** | You have it. |
| **Forge 1.20.1** | Download the installer from <https://files.minecraftforge.net/> → Minecraft 1.20.1 → *Installer*. Run it, choose **Install client**. This creates a "forge" profile in the launcher. Launch it once so the `mods` folder is created. |
| **JDK 17** | Needed to *build* the mod. Get Temurin 17 from <https://adoptium.net/temurin/releases/?version=17>. During install, tick **"Add to PATH"** and **"Set JAVA_HOME"**. Verify: open a new terminal and run `java -version` → it should say `17`. |
| **Git** (optional) | <https://git-scm.com/download/win> — or just download the repo as a ZIP. |

## 2. Get the code

```powershell
git clone -b claude/minecraft-baritone-bot-z4dyun https://github.com/petezaaa/websitestuff.git
cd websitestuff
```
(Or download the ZIP from GitHub, extract it, and open a terminal in that folder.)

## 3. Get Baritone (the fiddly bit)

This mod is built **on top of** Baritone and doesn't bundle it. You need the
**Baritone Forge build for Minecraft 1.20.1** — two jars from the same build:

- `baritone-api-forge-<version>.jar`  → put in this repo's **`libs/`** folder (needed to compile)
- `baritone-standalone-forge-<version>.jar` → goes in your **`.minecraft/mods/`** (needed to run)

Where to get them:

- **Easiest:** download a prebuilt Baritone 1.20.1 Forge release from a source
  you trust (the Baritone project's releases, or a reputable mirror). The 1.20.1
  line is Baritone `1.10.x`.
- **Most reliable:** build Baritone yourself from the official source —
  <https://github.com/cabaletta/baritone> — check out the 1.20.1 branch and run
  its own `./gradlew build`; the jars land in `dist/`.

Then make sure **`baritone_version`** in [`gradle.properties`](gradle.properties)
matches your jar's version (default is `1.10.2`; change it if yours differs).

## 4. Build the mod

**Windows** (in the repo folder):
```powershell
.\setup.ps1          # checks Java, builds, offers to copy into .minecraft/mods
```
or manually:
```powershell
.\gradlew.bat build
```

**Mac/Linux:**
```bash
./setup.sh
# or: ./gradlew build
```

The finished mod is at **`build/libs/baritonebot-1.0.0.jar`**.

## 5. Install

Copy **both** jars into your mods folder:

- Windows: `%APPDATA%\.minecraft\mods\`
- Mac: `~/Library/Application Support/minecraft/mods/`
- Linux: `~/.minecraft/mods/`

Put in there:
1. `build/libs/baritonebot-1.0.0.jar`  (this mod — `setup.ps1` can copy it for you)
2. `baritone-standalone-forge-<version>.jar`  (Baritone — you copy this one)

## 6. Play

1. In the Minecraft launcher, pick the **Forge 1.20.1** profile and hit Play.
2. Load a single-player world (try Creative first to test safely).
3. Type **`/bot help`** in chat.

Try these:
```
/bot goto ~ ~ ~          (won't do much; just proves commands work)
/bot mine wood 4
/bot kill 3
/bot craft wooden_pickaxe
/bot auto on
/bot play 1              (1-hour free-play demo — gathers, builds, bases up)
/bot base cozy_house     (after /bot materials cozy_house)
```

---

## Troubleshooting

- **`gradlew` fails with a Java version error** — you're not on JDK 17. Check
  `java -version`; install Temurin 17 and reopen the terminal.
- **Build can't find `baritone-api-forge`** — the API jar isn't in `libs/`, or
  `baritone_version` doesn't match the jar's filename.
- **Game crashes on launch / "missing Baritone"** — the standalone Baritone jar
  isn't in `mods/`, or its Minecraft version isn't 1.20.1.
- **`/bot` command doesn't exist** — the mod didn't load; check the launcher's
  "Mods" list shows *Baritone Bot*, and that you launched the Forge 1.20.1
  profile (not vanilla).
- **It fights but won't path / mine** — Baritone isn't loaded; confirm both jars
  are in `mods/` and versions match.

The RL/learning agent in [`learning/`](learning/README.md) is a **completely
separate** thing (Python, your GPU, Minecraft 1.16 via MineRL). This guide is
only for the Forge mod.
