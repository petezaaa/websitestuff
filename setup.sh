#!/usr/bin/env bash
# Baritone Bot — Mac/Linux build & install helper.
# Run from the repo folder:  ./setup.sh
# See SETUP.md for the full walkthrough (including where to get Baritone).
set -e

section() { printf '\n=== %s ===\n' "$1"; }

section "1/4  Checking Java (need JDK 17)"
if ! command -v java >/dev/null 2>&1; then
    echo "Java not found on PATH. Install Temurin JDK 17: https://adoptium.net"
    exit 1
fi
java -version
if ! java -version 2>&1 | grep -q '"17'; then
    echo "WARNING: Forge 1.20.1 builds on JDK 17. If the build fails, install Temurin 17."
fi

section "2/4  Checking for the Baritone API jar in libs/"
if ! ls libs/baritone-api-forge-*.jar >/dev/null 2>&1; then
    echo "No baritone-api-forge-*.jar in libs/."
    echo "Download the Baritone 1.20.1 Forge build and put its API jar in libs/ (see SETUP.md step 3)."
    echo "Also make sure baritone_version in gradle.properties matches that jar."
    exit 1
fi
echo "Found: $(ls libs/baritone-api-forge-*.jar)"

section "3/4  Building the mod"
chmod +x ./gradlew
./gradlew build

jar=$(ls build/libs/baritonebot-*.jar 2>/dev/null | grep -vE 'sources|javadoc' | head -n1 || true)
if [ -z "$jar" ]; then echo "Build succeeded but no mod jar found in build/libs."; exit 1; fi
echo "Built: $jar"

section "4/4  Install into .minecraft/mods"
case "$(uname -s)" in
    Darwin) mods="$HOME/Library/Application Support/minecraft/mods" ;;
    *)      mods="$HOME/.minecraft/mods" ;;
esac
if [ ! -d "$mods" ]; then
    echo "No mods folder at: $mods"
    echo "Install & launch Forge 1.20.1 once to create it, then copy the jar there yourself."
else
    read -r -p "Copy $(basename "$jar") to $mods ? (y/n) " ans
    if [ "$ans" = "y" ]; then cp -f "$jar" "$mods/"; echo "Copied."; fi
fi

echo
echo "DONE. Remaining manual step:"
echo "  - Copy baritone-standalone-forge-<version>.jar into: $mods"
echo "  - Launch the Forge 1.20.1 profile, load a world, type: /bot help"
