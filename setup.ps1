# Baritone Bot — Windows build & install helper.
# Run from the repo folder:  .\setup.ps1
# See SETUP.md for the full walkthrough (including where to get Baritone).

$ErrorActionPreference = "Stop"

function Section($t) { Write-Host "`n=== $t ===" -ForegroundColor Cyan }

Section "1/4  Checking Java (need JDK 17)"
try {
    $verLine = (& java -version 2>&1)[0]
    Write-Host $verLine
    if ($verLine -notmatch '"17') {
        Write-Host "WARNING: Forge 1.20.1 builds on JDK 17. If the build fails, install Temurin 17" -ForegroundColor Yellow
        Write-Host "         from https://adoptium.net and reopen this terminal." -ForegroundColor Yellow
    }
} catch {
    Write-Host "Java not found on PATH. Install Temurin JDK 17: https://adoptium.net" -ForegroundColor Red
    exit 1
}

Section "2/4  Checking for the Baritone API jar in libs\"
$api = Get-ChildItem -Path ".\libs" -Filter "baritone-api-forge-*.jar" -ErrorAction SilentlyContinue
if (-not $api) {
    Write-Host "No baritone-api-forge-*.jar found in libs\." -ForegroundColor Red
    Write-Host "Download the Baritone 1.20.1 Forge build and put its API jar in libs\ (see SETUP.md step 3)." -ForegroundColor Yellow
    Write-Host "Also make sure baritone_version in gradle.properties matches that jar." -ForegroundColor Yellow
    exit 1
}
Write-Host "Found: $($api.Name)"

Section "3/4  Building the mod"
& .\gradlew.bat build
if ($LASTEXITCODE -ne 0) { Write-Host "Build failed. See the error above and SETUP.md troubleshooting." -ForegroundColor Red; exit 1 }

$jar = Get-ChildItem -Path ".\build\libs" -Filter "baritonebot-*.jar" |
    Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -First 1
if (-not $jar) { Write-Host "Build succeeded but no mod jar found in build\libs." -ForegroundColor Red; exit 1 }
Write-Host "Built: $($jar.FullName)" -ForegroundColor Green

Section "4/4  Install into .minecraft\mods"
$mods = Join-Path $env:APPDATA ".minecraft\mods"
if (-not (Test-Path $mods)) {
    Write-Host "No mods folder at $mods." -ForegroundColor Yellow
    Write-Host "Install & launch Forge 1.20.1 once to create it, then copy the jar there yourself." -ForegroundColor Yellow
} else {
    $answer = Read-Host "Copy $($jar.Name) to $mods ? (y/n)"
    if ($answer -eq "y") {
        Copy-Item $jar.FullName $mods -Force
        Write-Host "Copied." -ForegroundColor Green
    }
}

Write-Host "`nDONE. Remaining manual step:" -ForegroundColor Cyan
Write-Host "  - Copy baritone-standalone-forge-<version>.jar into $mods"
Write-Host "  - Launch the Forge 1.20.1 profile, load a world, type: /bot help"
